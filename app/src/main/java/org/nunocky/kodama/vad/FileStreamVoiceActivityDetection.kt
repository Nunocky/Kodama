package org.nunocky.kodama.vad

import com.konovalov.vad.webrtc.VadWebRTC
import com.konovalov.vad.webrtc.config.FrameSize
import com.konovalov.vad.webrtc.config.Mode
import com.konovalov.vad.webrtc.config.SampleRate
import org.nunocky.kodama.audio.parseWavHeader
import java.io.InputStream

class FileStreamVoiceActivityDetection(private val iStream: InputStream) :
    VoiceActivityDetectionInterface() {

    override fun start() {
        val vad = VadWebRTC(
            sampleRate = SampleRate.SAMPLE_RATE_16K,
            frameSize = FrameSize.FRAME_SIZE_320,
            mode = Mode.VERY_AGGRESSIVE,
            silenceDurationMs = 1000,
            speechDurationMs = 50,
        )

        try {
            iStream.use { fis ->
                val audioHeader = ByteArray(44).apply { fis.read(this) }
                val wavInfo = parseWavHeader(audioHeader)
                // val chunkSize = vad.frameSize.value * 2 // 16bitサンプリングを想定
                val chunkSize = vad.frameSize.value * wavInfo.bytesPerSample * wavInfo.channels

                while (fis.available() > 0) {
                    val frameChunk = ByteArray(chunkSize).apply { fis.read(this) }

                    val isSpeech = vad.isSpeech(frameChunk)

                    for (l in listener) {
                        l.invoke(frameChunk, isSpeech)
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