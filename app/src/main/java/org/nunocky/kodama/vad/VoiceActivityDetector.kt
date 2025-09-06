package org.nunocky.kodama.vad

import kotlinx.coroutines.CoroutineScope

abstract class VoiceActivityDetector {
    enum class State {
        UNVOICED,
        VOICED,
    }

    protected var state: State = State.UNVOICED

    abstract fun start(scope: CoroutineScope)
    abstract fun stop()
    abstract fun isRunning(): Boolean

    interface Listener {
        var onVoiceStart: (() -> Unit)
        var onVoiceFrame: ((List<ByteArray>) -> Unit)
        var onVoiceEnd: (() -> Unit)
    }

    var listener: Listener? = null
}
