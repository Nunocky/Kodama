package org.nunocky.kodama

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 音声の再生と録音を管理するユースケース
 *
 * 音声の再生、マイク入力の開始・停止、音声の録音とWAV変換を行う。
 * マイク入力の状態（話しているかどうか）を監視し、
 * 録音開始・停止のトリガーとする。
 */
@Singleton
class AudioPlayUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var micStreamVAD: MicStreamVoiceActivityDetection? = null

    // 追加: 録音・変換用
    private var audioRecorder: AudioRecorder? = null
    private var pcmFile: File? = null
    private var lastSpeakingState: Boolean = false
    private val wavConverter = PcmToWavConverter()

    // 追加: 再生用
    private var mediaPlayer: android.media.MediaPlayer? = null
    
    // 追加: マイク一時停止制御用
    private var isMicTemporarilyPaused = false
    private var wasUserMicEnabled = false

    fun startMicInput() {
        if (micStreamVAD != null) return
        
        // 一時停止中の場合は復帰処理
        if (isMicTemporarilyPaused) {
            wasUserMicEnabled = true
            return
        }
        
        audioRecorder = AudioRecorder(context)
        micStreamVAD = MicStreamVoiceActivityDetection(scope, { bytes, isSpeech ->
            // isSpeaking状態変化検知
            if (!lastSpeakingState && isSpeech) {
                // false→true: 録音開始
                pcmFile = audioRecorder?.startRecording()
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
        }, context)
        micStreamVAD?.start()
    }

    // 追加: WAVファイル再生処理
    private fun playWavFile(wavFile: File) {
        mediaPlayer?.release()
        mediaPlayer = android.media.MediaPlayer().apply {
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
    
    // 追加: マイクの一時停止
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
        }
    }
    
    // 追加: 必要に応じてマイクを復帰
    private fun resumeMicIfNeeded() {
        if (isMicTemporarilyPaused && wasUserMicEnabled) {
            isMicTemporarilyPaused = false
            startMicInput()
        }
    }

    fun stopMicInput() {
        micStreamVAD?.stop()
        micStreamVAD = null
        _isSpeaking.value = false
        audioRecorder?.stopRecording()
        audioRecorder = null
        lastSpeakingState = false
        
        // 一時停止状態もリセット
        isMicTemporarilyPaused = false
        wasUserMicEnabled = false
        
        // 追加: MediaPlayerリソース解放
        _isPlaying.value = false
        mediaPlayer?.release()
        mediaPlayer = null
    }
}