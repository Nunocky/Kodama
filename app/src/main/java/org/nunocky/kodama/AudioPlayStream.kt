package org.nunocky.kodama

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log

interface AudioPlayStream {
    fun initialize(sampleRate: Int, channelConfig: Int, audioFormat: Int): Boolean
    fun start()
    fun write(buffer: ByteArray, offset: Int, size: Int): Int
    fun stop()
    fun release()
    val isPlaying: Boolean
}

class AudioPlayStreamImpl : AudioPlayStream {
    private var audioTrack: AudioTrack? = null
    private var _isPlaying = false

    override val isPlaying: Boolean
        get() = _isPlaying

    override fun initialize(sampleRate: Int, channelConfig: Int, audioFormat: Int): Boolean {
        return try {
            val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            
            if (bufferSize == AudioTrack.ERROR || bufferSize == AudioTrack.ERROR_BAD_VALUE) {
                Log.e("AudioPlayStream", "Invalid buffer size: $bufferSize")
                return false
            }

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(audioFormat)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .build()

            Log.d("AudioPlayStream", "AudioTrack initialized successfully")
            true
        } catch (e: Exception) {
            Log.e("AudioPlayStream", "Failed to initialize AudioTrack", e)
            false
        }
    }

    override fun start() {
        audioTrack?.let { track ->
            try {
                track.play()
                _isPlaying = true
                Log.d("AudioPlayStream", "AudioTrack started")
            } catch (e: Exception) {
                Log.e("AudioPlayStream", "Failed to start AudioTrack", e)
            }
        } ?: run {
            Log.w("AudioPlayStream", "AudioTrack not initialized")
        }
    }

    override fun write(buffer: ByteArray, offset: Int, size: Int): Int {
        return audioTrack?.let { track ->
            try {
                track.write(buffer, offset, size)
            } catch (e: Exception) {
                Log.e("AudioPlayStream", "Failed to write to AudioTrack", e)
                AudioTrack.ERROR
            }
        } ?: AudioTrack.ERROR
    }

    override fun stop() {
        audioTrack?.let { track ->
            try {
                track.stop()
                _isPlaying = false
                Log.d("AudioPlayStream", "AudioTrack stopped")
            } catch (e: Exception) {
                Log.e("AudioPlayStream", "Failed to stop AudioTrack", e)
            }
        }
    }

    override fun release() {
        audioTrack?.let { track ->
            try {
                if (_isPlaying) {
                    track.stop()
                }
                track.release()
                _isPlaying = false
                Log.d("AudioPlayStream", "AudioTrack released")
            } catch (e: Exception) {
                Log.e("AudioPlayStream", "Failed to release AudioTrack", e)
            }
        }
        audioTrack = null
    }
}