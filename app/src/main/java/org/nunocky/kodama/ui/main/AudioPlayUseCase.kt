package org.nunocky.kodama.ui.main

import android.content.Context
import android.media.MediaPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.nunocky.kodama.audio.AudioRecorder
import org.nunocky.kodama.audio.PcmToWavConverter
import org.nunocky.kodama.vad.MicStreamVoiceActivityDetection
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// 録音開始前に保存するチャンク数のデフォルト値
private const val PRE_RECORDING_BUFFER_SIZE = 1

/**
 * 音声の再生と録音を管理するユースケース
 *
 * 音声の再生、マイク入力の開始・停止、音声の録音とWAV変換を行う。
 * マイク入力の状態（話しているかどうか）を監視し、
 * 録音開始・停止のトリガーとする。
 */
@Singleton
class AudioPlayUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var micStreamVAD: MicStreamVoiceActivityDetection? = null

    // 再生
    private var mediaPlayer: MediaPlayer? = null

    // 録音・変換
    private var audioRecorder: AudioRecorder? = null
    private var pcmFile: File? = null
    private var lastSpeakingState: Boolean = false
    private val wavConverter = PcmToWavConverter()

    // マイク一時停止制御
    private var isMicTemporarilyPaused = false
    private var wasUserMicEnabled = false

    // 最新チャンクの一時保存
    private var preRecordingBufferSize = PRE_RECORDING_BUFFER_SIZE // 保存するチャンク個数（設定可能）
    private val preRecordingBuffer = mutableListOf<ByteArray>() // 循環バッファ

    fun startMicInput() {
        if (micStreamVAD != null) return

        // 一時停止中の場合は復帰処理
        if (isMicTemporarilyPaused) {
            wasUserMicEnabled = true
            return
        }

        audioRecorder = AudioRecorder(context)

        // バッファをクリア
        preRecordingBuffer.clear()

        micStreamVAD = MicStreamVoiceActivityDetection(scope, context).apply {
            addListener { bytes, isSpeech ->
                // マイク入力ON時は常に最新チャンクを保存
                updatePreRecordingBuffer(bytes)

                // isSpeaking状態変化検知
                if (!lastSpeakingState && isSpeech) {
                    // false→true: 録音開始
                    pcmFile = audioRecorder?.startRecording()
                    // 一時保存していたチャンクを最初に書き込み
                    writePreRecordingBuffer()
                }
                if (isSpeech) {
                    // true: データ書き込み
                    audioRecorder?.write(bytes)
                }
                if (lastSpeakingState && !isSpeech) {
                    // true→false: 録音停止・WAV変換
                    val file = audioRecorder?.stopRecording()
                    file?.let {
                        val wavFile = File(it.parent, it.nameWithoutExtension + ".wav")
                        wavConverter.convert(it, wavFile)
                        // 追加: WAVファイル再生
                        playWavFile(wavFile)
                    }
                }
                lastSpeakingState = isSpeech
                _isSpeaking.value = isSpeech
            }
        }

        micStreamVAD?.start()
    }

    /**
     * 最新チャンクを一時保存バッファに追加する
     */
    private fun updatePreRecordingBuffer(bytes: ByteArray) {
        // バッファが満杯の場合は古いものを削除
        if (preRecordingBuffer.size >= preRecordingBufferSize) {
            preRecordingBuffer.removeAt(0)
        }
        // 新しいチャンクを追加
        preRecordingBuffer.add(bytes.copyOf())
    }

    /**
     * 録音開始時に一時保存バッファの内容を録音ファイルに書き込む
     */
    private fun writePreRecordingBuffer() {
        for (chunk in preRecordingBuffer) {
            audioRecorder?.write(chunk)
        }
    }

    /**
     * WAVファイルを再生する
     */
    private fun playWavFile(wavFile: File) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(wavFile.absolutePath)
            setOnPreparedListener {
                // 再生開始前にマイクを一時停止
                pauseMicTemporarily()
                _isPlaying.value = true
                start()
            }
            setOnCompletionListener {
                // 再生完了時にマイクを復帰
                _isPlaying.value = false
                resumeMicIfNeeded()
                release()
                mediaPlayer = null
            }
            setOnErrorListener { _, _, _ ->
                // エラー時もマイクを復帰
                _isPlaying.value = false
                resumeMicIfNeeded()
                release()
                mediaPlayer = null
                true
            }
            prepareAsync()
        }
    }

    /**
     * マイク入力を一時的に停止する
     */
    private fun pauseMicTemporarily() {
        if (micStreamVAD != null && !isMicTemporarilyPaused) {
            wasUserMicEnabled = true
            isMicTemporarilyPaused = true
            micStreamVAD?.stop()
            micStreamVAD = null
            _isSpeaking.value = false
            audioRecorder?.stopRecording()
            audioRecorder = null
            lastSpeakingState = false
            preRecordingBuffer.clear()
        }
    }

    /**
     * マイクの一時停止を解除し、必要なら再開する
     */
    private fun resumeMicIfNeeded() {
        if (isMicTemporarilyPaused && wasUserMicEnabled) {
            isMicTemporarilyPaused = false
            startMicInput()
        }
    }

    /**
     * マイク入力を停止し、リソースを解放する
     */
    fun stopMicInput() {
        micStreamVAD?.stop()
        micStreamVAD = null
        _isSpeaking.value = false
        audioRecorder?.stopRecording()
        audioRecorder = null
        lastSpeakingState = false

        // 一時停止状態をリセット
        isMicTemporarilyPaused = false
        wasUserMicEnabled = false

        // バッファをクリア
        preRecordingBuffer.clear()

        // MediaPlayerのリソース解放
        _isPlaying.value = false
        mediaPlayer?.release()
        mediaPlayer = null
    }
}