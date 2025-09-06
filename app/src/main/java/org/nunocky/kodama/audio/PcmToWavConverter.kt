package org.nunocky.kodama.audio

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream

class PcmToWavConverter {
    fun convert(
        pcmFile: File,
        wavFile: File,
        sampleRate: Int = 16000,
        channels: Int = 1,
        bitsPerSample: Int = 16
    ) {
        val pcmSize = pcmFile.length().toInt()
        val wavOut = FileOutputStream(wavFile)
        writeWavHeader(wavOut, pcmSize, sampleRate, channels, bitsPerSample)
        val pcmIn = FileInputStream(pcmFile)
        val buffer = ByteArray(1024)
        var read: Int
        while (pcmIn.read(buffer).also { read = it } != -1) {
            wavOut.write(buffer, 0, read)
        }
        pcmIn.close()
        wavOut.flush()
        wavOut.close()
    }

    private fun writeWavHeader(
        out: OutputStream,
        pcmSize: Int,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val header = ByteArray(44)
        // RIFF header
        header[0] = 'R'.code.toByte();
        header[1] = 'I'.code.toByte();
        header[2] = 'F'.code.toByte();
        header[3] = 'F'.code.toByte()
        val totalDataLen = pcmSize + 36
        writeInt(header, 4, totalDataLen)
        header[8] = 'W'.code.toByte();
        header[9] = 'A'.code.toByte();
        header[10] = 'V'.code.toByte();
        header[11] = 'E'.code.toByte()
        // fmt chunk
        header[12] = 'f'.code.toByte();
        header[13] = 'm'.code.toByte();
        header[14] = 't'.code.toByte();
        header[15] = ' '.code.toByte()
        writeInt(header, 16, 16) // Subchunk1Size
        writeShort(header, 20, 1) // AudioFormat (PCM)
        writeShort(header, 22, channels)
        writeInt(header, 24, sampleRate)
        writeInt(header, 28, byteRate)
        writeShort(header, 32, channels * bitsPerSample / 8)
        writeShort(header, 34, bitsPerSample)
        // data chunk
        header[36] = 'd'.code.toByte();
        header[37] = 'a'.code.toByte();
        header[38] = 't'.code.toByte();
        header[39] = 'a'.code.toByte()
        writeInt(header, 40, pcmSize)
        out.write(header, 0, 44)
    }

    private fun writeInt(header: ByteArray, offset: Int, value: Int) {
        header[offset] = (value and 0xff).toByte()
        header[offset + 1] = ((value shr 8) and 0xff).toByte()
        header[offset + 2] = ((value shr 16) and 0xff).toByte()
        header[offset + 3] = ((value shr 24) and 0xff).toByte()
    }

    private fun writeShort(header: ByteArray, offset: Int, value: Int) {
        header[offset] = (value and 0xff).toByte()
        header[offset + 1] = ((value shr 8) and 0xff).toByte()
    }
}