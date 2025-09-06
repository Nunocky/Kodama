package org.nunocky.kodama.vad

import com.konovalov.vad.webrtc.VadWebRTC
import com.konovalov.vad.webrtc.config.FrameSize
import com.konovalov.vad.webrtc.config.Mode
import com.konovalov.vad.webrtc.config.SampleRate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject

class FileStreamVoiceActivityDetector @Inject constructor(
    private val file: File
) : VoiceActivityDetector() {

    private var job: Job? = null

    override fun start(scope: CoroutineScope) {
        if (job?.isActive == true) {
            return
        }

        job = scope.launch {
            val frameSize = 320 * 2 // 16bit PCM, 320 samples
            val vad = VadWebRTC(
                sampleRate = SampleRate.SAMPLE_RATE_16K,
                frameSize = FrameSize.FRAME_SIZE_320,
                mode = Mode.VERY_AGGRESSIVE,
                silenceDurationMs = 1000,
                speechDurationMs = 50,
            )
            FileInputStream(file).use { fis ->
                val buf = ByteArray(frameSize)

                while (true) {
                    val read = fis.read(buf)
                    if (read <= 0) {
                        break
                    }

                    val isSpeech = vad.isSpeech(buf.copyOf(read))

                    state = when (state) {
                        State.UNVOICED -> {
                            if (isSpeech) {
                                listener?.onVoiceStart?.invoke()
                                listener?.onVoiceFrame?.invoke(listOf(buf.copyOf(read)))
                                State.VOICED
                            } else {
                                State.UNVOICED
                            }
                        }

                        State.VOICED -> {
                            if (!isSpeech) {
                                listener?.onVoiceEnd?.invoke()
                                State.UNVOICED
                            } else {
                                listener?.onVoiceFrame?.invoke(listOf(buf.copyOf(read)))
                                State.VOICED
                            }
                        }
                    }
                }
            }
            vad.close()
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
    }

    override fun isRunning(): Boolean = job?.isActive == true
}

