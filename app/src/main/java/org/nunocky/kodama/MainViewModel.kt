package org.nunocky.kodama

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
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

    private var audioFileInputStream: AudioFileInputStream? = null
    private var audioTrack: AudioTrack? = null
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
                // val fileFd = assetManager.openFd("sample_voice01.wav")
                val inputStream = assetManager.open("sample_voice01.wav")

                audioFileInputStream = AudioFileInputStream(inputStream)

                // AudioTrackの初期化 (16kHz, 16bit, Mono)
                val sampleRate = 16000
                val channelConfig = AudioFormat.CHANNEL_OUT_MONO
                val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(audioFormat)
                            .setChannelMask(channelConfig)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .build()

                audioTrack?.play()

                // VADリスナーを追加
                audioFileInputStream?.addListener(object : AudioInputStream.Listener() {
                    override fun onFrame(bytes: ByteArray, isSpeech: Boolean) {
                        Log.d("AudioPlayUseCase", "onFrame: isSpeech=$isSpeech, bytes=${bytes.size}")
                        _isSpeaking.value = isSpeech
                        
                        // チャンクを音声再生
                        audioTrack?.write(bytes, 0, bytes.size)
                    }
                })

                // sample_voice01.wavのチャンクを読み、オーディア再生ストリームとVADオブジェクトに送る
                audioFileInputStream?.start()

                // AudioFileInputStreamの処理が完了したらisPlayingとisSpeakingをfalseにする
                _isPlaying.value = false
                _isSpeaking.value = false
                
                // AudioTrackをリソース解放
                audioTrack?.stop()
                audioTrack?.release()
                audioTrack = null

            } catch (e: Exception) {
                _isPlaying.value = false
                _isSpeaking.value = false
                
                // AudioTrackをリソース解放
                audioTrack?.stop()
                audioTrack?.release()
                audioTrack = null
                
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

        // AudioTrackをリソース解放
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        
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