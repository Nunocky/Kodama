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

    private val scope = CoroutineScope(Dispatchers.IO)
    private var micStreamVAD: MicStreamVoiceActivityDetection? = null

    // 追加: 録音・変換用
    private var audioRecorder: AudioRecorder? = null
    private var pcmFile: File? = null
    private var lastSpeakingState: Boolean = false
    private val wavConverter = PcmToWavConverter()

    // 追加: 再生用
    private var mediaPlayer: android.media.MediaPlayer? = null

    fun startMicInput() {
        if (micStreamVAD != null) return
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
            setOnPreparedListener { start() }
            setOnCompletionListener {
                release()
                mediaPlayer = null
            }
            prepareAsync()
        }
    }

    fun stopMicInput() {
        micStreamVAD?.stop()
        micStreamVAD = null
        _isSpeaking.value = false
        audioRecorder?.stopRecording()
        audioRecorder = null
        lastSpeakingState = false
        // 追加: MediaPlayerリソース解放
        mediaPlayer?.release()
        mediaPlayer = null
    }
}