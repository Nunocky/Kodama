package org.nunocky.kodama

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 音声録音を管理するクラス
 */
class AudioRecorder(private val context: Context) {
    private var outputStream: FileOutputStream? = null
    private var pcmFile: File? = null

    fun startRecording(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "audio_${timeStamp}.pcm"
        val cacheDir = context.cacheDir
        pcmFile = File(cacheDir, fileName)
        outputStream = FileOutputStream(pcmFile)
        return pcmFile
    }

    fun write(data: ByteArray) {
        outputStream?.write(data)
    }

    fun stopRecording(): File? {
        outputStream?.flush()
        outputStream?.close()
        return pcmFile
    }
}
