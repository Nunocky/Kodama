package org.nunocky.kodama.vad

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject


class MicStreamVoiceActivityDetector @Inject constructor(
    @param:ApplicationContext private val context: Context
) : VoiceActivityDetector() {
    companion object {
        var COMPENSATION_FRAME_COUNT: Int = 5 // バッファするフレーム数（変更可能）
    }

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val frameBuffer = ArrayDeque<ByteArray>()

    override fun start(scope: CoroutineScope) {
        // 権限チェック
        val permission = Manifest.permission.RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(
                context,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 権限がない場合は録音開始しない
            return
        }

        // Silero VAD
        val frameSize = com.konovalov.vad.silero.config.FrameSize.FRAME_SIZE_512
        val vad = com.konovalov.vad.silero.Vad.builder()
            .setContext(context)
            .setSampleRate(com.konovalov.vad.silero.config.SampleRate.SAMPLE_RATE_16K)
            .setFrameSize(frameSize)
            .setMode(com.konovalov.vad.silero.config.Mode.NORMAL)
            // .setSilenceDurationMs(1000)
            // .setSpeechDurationMs(50)
            .build()

        // WebRTC VAD
//        val frameSize = com.konovalov.vad.webrtc.config.FrameSize.FRAME_SIZE_320
//        val vad = com.konovalov.vad.webrtc.Vad.builder()
//            .setSampleRate(com.konovalov.vad.webrtc.config.SampleRate.SAMPLE_RATE_16K)
//            .setFrameSize(frameSize)
//            .setMode(com.konovalov.vad.webrtc.config.Mode.VERY_AGGRESSIVE)
//            // .setSilenceDurationMs(1000)
//            // .setSpeechDurationMs(50)
//            .build()

        val sampleRate = 16000
        val minBufferSize = maxOf(
            AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ),
            2 * frameSize.value
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBufferSize
        )

        audioRecord?.startRecording()
        recordingJob = scope.launch(Dispatchers.IO) {
            try {
                val buf = ByteArray(frameSize.value * 2) // 16bit PCMなので2倍
                while (isActive) {
                    val read = audioRecord?.read(buf, 0, buf.size) ?: 0
                    if (read <= 0) {
                        // 録音終了
                        break
                    }

                    // バッファに追加
                    frameBuffer.addLast(buf.copyOf(read))
                    if (frameBuffer.size > COMPENSATION_FRAME_COUNT) {
                        frameBuffer.removeFirst()
                    }

                    val isSpeech = vad.isSpeech(buf)

                    when (state) {
                        State.UNVOICED -> {
                            if (isSpeech) {
                                listener?.onVoiceStart?.invoke()
                                // 発話検出時、バッファ全体をコールバック
                                listener?.onVoiceFrame?.invoke(frameBuffer.toList())
                                state = State.VOICED
                            }
                        }

                        State.VOICED -> {
                            if (!isSpeech) {
                                listener?.onVoiceEnd?.invoke()
                                state = State.UNVOICED
                            } else {
                                // 発話中も最新フレームのみコールバック
                                listener?.onVoiceFrame?.invoke(listOf(buf.copyOf(read)))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                when (state) {
                    State.VOICED -> {
                        listener?.onVoiceEnd?.invoke()
                        state = State.UNVOICED
                    }

                    else -> {}
                }
                vad.close()
                frameBuffer.clear()
            }
        }
    }

    override fun stop() {
        recordingJob?.cancel()
        recordingJob = null

        // TODO アプリ終了時ここでクラッシュする
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    override fun isRunning(): Boolean {
        return recordingJob?.isActive == true
    }
}
