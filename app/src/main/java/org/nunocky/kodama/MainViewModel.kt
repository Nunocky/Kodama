package org.nunocky.kodama

import android.content.Context
import android.media.AudioFormat
import android.util.Log
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var audioFileInputStream: FileStreamVoiceActivityDetection? = null
    private var audioPlayStream: AudioPlayStream? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun start() {
        if (_isPlaying.value) return // 既に再生中の場合は何もしない
        Log.d("AudioPlayUseCase", "Starting playback")

        _isPlaying.value = true

        playbackJob = scope.launch {
            try {
                // AudioFileInputStream オブジェクトを作る
                // asset/sample_voice01.wavを開く
                val assetManager = context.assets
                val inputStream = assetManager.open("sample_voice01.wav")

                audioFileInputStream = FileStreamVoiceActivityDetection(inputStream)

                // AudioPlayStream の初期化 (16kHz, 16bit, Mono)
                val sampleRate = 16000
                val channelConfig = AudioFormat.CHANNEL_OUT_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT

                audioPlayStream = AudioPlayStreamImpl()
                if (audioPlayStream?.initialize(sampleRate, channelConfig, audioFormat) != true) {
                    Log.e("AudioPlayUseCase", "Failed to initialize AudioPlayStream")
                    _isPlaying.value = false
                    return@launch
                }

                // 音声の再生を開始
                audioPlayStream?.start()

                // VADリスナーを追加
                audioFileInputStream?.addListener(object : AudioInputStream.Listener() {
                    override fun onFrame(bytes: ByteArray, isSpeech: Boolean) {
                        Log.d(
                            "AudioPlayUseCase",
                            "onFrame: isSpeech=$isSpeech, bytes=${bytes.size}"
                        )
                        _isSpeaking.value = isSpeech

                        // チャンクをAudioStreamで再生
                        audioPlayStream?.write(bytes, 0, bytes.size)
                    }
                })

                // sample_voice01.wavのチャンクを読み、オーディア再生ストリームとVADオブジェクトに送る
                audioFileInputStream?.start()

                // AudioFileInputStreamの処理が完了したらisPlayingとisSpeakingをfalseにする
                _isPlaying.value = false
                _isSpeaking.value = false

                // AudioPlayStream をリソース解放
                audioPlayStream?.stop()
                audioPlayStream?.release()
                audioPlayStream = null

            } catch (e: Exception) {
                _isPlaying.value = false
                _isSpeaking.value = false

                // AudioPlayStream をリソース解放
                audioPlayStream?.stop()
                audioPlayStream?.release()
                audioPlayStream = null

                e.printStackTrace()
            }
        }
    }

    fun stop() {
        Log.d("AudioPlayUseCase", "Stopping playback")
        _isPlaying.value = false
        _isSpeaking.value = false

        // 非同期処理を停止
        playbackJob?.cancel()
        playbackJob = null

        // AudioPlayStreamをリソース解放
        audioPlayStream?.stop()
        audioPlayStream?.release()
        audioPlayStream = null

        // AudioFileInputStreamをクリーンアップ
        audioFileInputStream = null
    }
}


@HiltViewModel
class MainViewModel @Inject constructor(
    private val audioPlayUseCase: AudioPlayUseCase
) : ViewModel() {

    val isPlaying: StateFlow<Boolean> = audioPlayUseCase.isPlaying
    val isSpeaking: StateFlow<Boolean> = audioPlayUseCase.isSpeaking

    fun startPlayback() {
        audioPlayUseCase.start()
    }

    fun stopPlayback() {
        audioPlayUseCase.stop()
    }

    fun togglePlayback() {
        if (audioPlayUseCase.isPlaying.value) {
            stopPlayback()
        } else {
            startPlayback()
        }
    }
}