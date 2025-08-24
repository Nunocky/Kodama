package org.nunocky.kodama.vad

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.konovalov.vad.webrtc.VadWebRTC
import com.konovalov.vad.webrtc.config.FrameSize
import com.konovalov.vad.webrtc.config.Mode
import com.konovalov.vad.webrtc.config.SampleRate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MicStreamVoiceActivityDetection(
    private val scope: CoroutineScope,
    private val context: Context // 追加
) : VoiceActivityDetectionInterface() {
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    override fun start() {
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

        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val frameSize = FrameSize.FRAME_SIZE_320.value // 320 samples
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            .coerceAtLeast(frameSize * 2)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        val vad = VadWebRTC(
            sampleRate = SampleRate.SAMPLE_RATE_16K,
            frameSize = FrameSize.FRAME_SIZE_320,
            mode = Mode.VERY_AGGRESSIVE,
            silenceDurationMs = 1000,
            speechDurationMs = 50,
        )

        audioRecord?.startRecording()
        recordingJob = scope.launch {
            try {
                val buf = ByteArray(frameSize * 2) // 16bit PCMなので2倍
                while (isActive) {
                    val read = audioRecord?.read(buf, 0, buf.size) ?: 0
                    if (read > 0) {
                        val isSpeech = vad.isSpeech(buf)
                        for (l in listener) {
                            l.invoke(buf.copyOf(read), isSpeech)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                vad.close()
            }
        }
    }

    fun stop() {
        recordingJob?.cancel()
        recordingJob = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
}
