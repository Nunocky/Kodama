package org.nunocky.kodama.io

import android.media.MediaPlayer
import java.io.File
import javax.inject.Inject

class AudioPlayer @Inject constructor(
) {
    interface Listener {
        fun onStart()
        fun onComplete()
        fun onError()
    }

    var listener: Listener? = null
    private var mediaPlayer: MediaPlayer? = null

    // WAVファイル再生処理
    fun playWav(wavFile: File) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(wavFile.absolutePath)
            setOnPreparedListener {
                listener?.onStart()
                start()
            }
            setOnCompletionListener {
                release()
                listener?.onComplete()
                mediaPlayer = null
            }
            setOnErrorListener { _, _, _ ->
                release()
                mediaPlayer = null
                listener?.onError()
                true
            }
            prepareAsync()
        }
    }
}