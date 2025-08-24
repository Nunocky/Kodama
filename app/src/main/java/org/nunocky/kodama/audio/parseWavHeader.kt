package org.nunocky.kodama.audio

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class WavFileInfo(
    val sampleRate: Int,
    val channels: Int,
    val bitsPerSample: Int,
    val bytesPerSample: Int,
    val blockAlign: Int,
    val audioFormat: Int
)

fun parseWavHeader(audioHeader: ByteArray): WavFileInfo {
    require(audioHeader.size >= 44) { "WAVヘッダーのサイズが不足しています" }

    val buffer = ByteBuffer.wrap(audioHeader).order(ByteOrder.LITTLE_ENDIAN)

    // RIFFヘッダー確認
    val riffHeader = ByteArray(4).apply { buffer.get(this) }
    require(String(riffHeader) == "RIFF") { "RIFFヘッダーが見つかりません" }

    // ファイルサイズ（使用しないのでスキップ）
    buffer.getInt()

    // WAVEヘッダー確認
    val waveHeader = ByteArray(4).apply { buffer.get(this) }
    require(String(waveHeader) == "WAVE") { "WAVEヘッダーが見つかりません" }

    // fmtチャンク確認
    val fmtHeader = ByteArray(4).apply { buffer.get(this) }
    require(String(fmtHeader) == "fmt ") { "fmtチャンクが見つかりません" }

    // fmtチャンクサイズ（使用しないのでスキップ）
    buffer.getInt()

    val audioFormat = buffer.getShort().toInt()
    val channels = buffer.getShort().toInt()
    val sampleRate = buffer.getInt()
    val byteRate = buffer.getInt() // 使用しない
    val blockAlign = buffer.getShort().toInt()
    val bitsPerSample = buffer.getShort().toInt()
    val bytesPerSample = bitsPerSample / 8

    return WavFileInfo(
        sampleRate = sampleRate,
        channels = channels,
        bitsPerSample = bitsPerSample,
        bytesPerSample = bytesPerSample,
        blockAlign = blockAlign,
        audioFormat = audioFormat
    )
}