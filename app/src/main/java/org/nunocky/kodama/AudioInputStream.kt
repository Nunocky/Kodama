package org.nunocky.kodama


abstract class AudioInputStream {
    abstract class Listener {
        abstract fun onFrame(bytes: ByteArray, isSpeech: Boolean)
    }

    protected val listener = ArrayList<Listener>()

    fun addListener(listener: Listener) {
        this.listener.add(listener)
    }

    abstract fun start()
}