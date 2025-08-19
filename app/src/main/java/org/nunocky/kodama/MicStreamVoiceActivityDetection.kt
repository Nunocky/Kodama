package org.nunocky.kodama

import android.media.AudioRecord
import android.media.MediaRecorder
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.content.Context
import com.konovalov.vad.webrtc.VadWebRTC
import com.konovalov.vad.webrtc.config.FrameSize
import com.konovalov.vad.webrtc.config.Mode
import com.konovalov.vad.webrtc.config.SampleRate
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MicStreamVoiceActivityDetection(
    private val scope: CoroutineScope,
    private val onFrame: (ByteArray, Boolean) -> Unit,
    private val context: Context // 追加
) {
    private var audioRecord: AudioRecord? = null
    private var recordingJob: kotlinx.coroutines.Job? = null

    fun start() {
        // 権限チェック
        val permission = android.Manifest.permission.RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            // 権限がない場合は録音開始しない
            return
        }

        val sampleRate = 16000
        val channelConfig = android.media.AudioFormat.CHANNEL_IN_MONO
        val audioFormat = android.media.AudioFormat.ENCODING_PCM_16BIT
        val frameSize = FrameSize.FRAME_SIZE_320.value // 320 samples
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat).coerceAtLeast(frameSize * 2)

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
                        onFrame(buf.copyOf(read), isSpeech)
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
