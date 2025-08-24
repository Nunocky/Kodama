package org.nunocky.kodama.vad

typealias Listener = (bytes: ByteArray, isSpeech: Boolean) -> Unit

abstract class VoiceActivityDetectionInterface {
    protected val listener = mutableListOf<Listener>()

    fun addListener(listener: Listener) {
        this.listener.add(listener)
    }

    abstract fun start()
}