package org.nunocky.kodama.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.nunocky.kodama.Logger
import org.nunocky.kodama.audio.PcmToWavConverter
import org.nunocky.kodama.io.AudioPlayer
import org.nunocky.kodama.io.AudioRecorder
import org.nunocky.kodama.vad.MicStreamVoiceActivityDetector
import org.nunocky.kodama.vad.VoiceActivityDetector
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton


private const val TAG = "VoiceInput"

enum class VoiceInputState {
    UNVOICING,
    VOICING, // 発話中
    PLAYING,
}

/**
 * 音声入力
 * - マイクからの音声入力を管理する
 * - Voice Activity Detectorで音声の開始・終了を検出してリスナーに通知する
 *
 */
@Singleton
class VoiceInputUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val audioPlayer: AudioPlayer,
    private val scope: CoroutineScope,
) {
    // ユースケースの状態
    private val _state = MutableStateFlow(VoiceInputState.UNVOICING)
    val state: StateFlow<VoiceInputState> = _state.asStateFlow()

    // 追加: 録音・変換用
    private var audioRecorder: AudioRecorder? = null
    private var pcmFile: File? = null

    // Voice Activity Detector
    private var voiceActivityDetector: VoiceActivityDetector? = null

    // マイクの ON/OFF は、ユースケースの状態変化のときにこの値をみて判断する
    private var _micOnPendingRequest = MutableStateFlow(false)
    val micOnPendingRequest = _micOnPendingRequest.asStateFlow()

    fun requestMicInput(enable: Boolean) {
        _micOnPendingRequest.value = enable
    }

    /**
     * Voice Activity Detectionのリスナー
     * 音声の開始、フレーム、終了を検出して対応する
     */
    private val vadListener = object : VoiceActivityDetector.Listener {
        override var onVoiceStart: (() -> Unit) = {
            Logger.d(TAG, "VAD: onVoiceStart")
            // 録音開始
            pcmFile = audioRecorder?.startRecording()
            setNextState(VoiceInputState.VOICING)
        }
        override var onVoiceFrame: (List<ByteArray>) -> Unit = { frameList ->
            // 録音データ書き込み
            frameList.forEach { bytes ->
                Logger.d(TAG, "VAD: onVoiceFrame size=${bytes.size}")
                audioRecorder?.write(bytes)
            }
        }
        override var onVoiceEnd: (() -> Unit) = {
            Logger.d(TAG, "VAD: onVoiceEnd")
            setNextState(VoiceInputState.PLAYING)
            // 録音停止・WAV変換
            val file = audioRecorder?.stopRecording()
            file?.let {
                val wavFile = File(it.parent, it.nameWithoutExtension + ".wav")
                val wavConverter = PcmToWavConverter()
                wavConverter.convert(it, wavFile)

                audioPlayer.playWav(wavFile)
            }
        }
    }

    private val audioPlayListener = object : AudioPlayer.Listener {
        override fun onStart() {
            Logger.d(TAG, "AudioPlayer: onStart")
        }

        override fun onComplete() {
            Logger.d(TAG, "AudioPlayer: onComplete")
            setNextState(VoiceInputState.UNVOICING)
        }

        override fun onError() {
            Logger.d(TAG, "AudioPlayer: onError")
            setNextState(VoiceInputState.UNVOICING)
        }
    }

    /**
     * マイク入力開始
     */
    private fun startMicInput() {
        if (voiceActivityDetector != null) {
            return
        }

        audioRecorder = AudioRecorder(context)

        voiceActivityDetector = MicStreamVoiceActivityDetector(context)
        voiceActivityDetector?.listener = vadListener
        voiceActivityDetector?.start(scope)
    }

    /**
     * マイク入力停止
     */
    private fun stopMicInput() {
        Logger.d(TAG, "stopMicInput")
        voiceActivityDetector?.listener = null
        voiceActivityDetector?.stop()
        voiceActivityDetector = null

        audioRecorder?.stopRecording()
        audioRecorder = null
    }

    /**
     * 状態遷移
     */
    fun setNextState(nextState: VoiceInputState) {
        val current = _state.value

        if (current == nextState) {
            return
        }

        _state.value = nextState
    }

    init {
        audioPlayer.listener = audioPlayListener

        // マイクのON/OFFを状態とリクエストに応じて制御する
        scope.launch {
            combine(
                _micOnPendingRequest,
                _state,
            ) { requestPending, currentState ->
                // トリプルとして値を保持
                Pair(requestPending, currentState)
            }.collect { (micOnPendingRequest, currentState) ->
                Logger.d(
                    TAG,
                    "Mic Control: requestPending=$micOnPendingRequest, currentState=$currentState"
                )

                when (currentState) {
                    VoiceInputState.UNVOICING -> {
                        if (micOnPendingRequest) {
                            delay(300L)
                            startMicInput()
                        } else {
                            stopMicInput()
                        }
                    }

                    VoiceInputState.VOICING -> {
                    }

                    VoiceInputState.PLAYING -> {
                        stopMicInput()
                    }
                }
            }
        }
    }
}