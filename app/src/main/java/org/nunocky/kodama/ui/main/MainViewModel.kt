package org.nunocky.kodama.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import org.nunocky.kodama.usecase.VoiceInputUseCase
import javax.inject.Inject

private const val TAG = "MainViewModel"

@HiltViewModel
class MainViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val voiceInputUseCase: VoiceInputUseCase,
) : ViewModel() {
    // 音声入力の状態
    val voiceInputState = voiceInputUseCase.state
    val isMicActive = voiceInputUseCase.micOnPendingRequest

    fun onMicActiveChanged(active: Boolean) {
        voiceInputUseCase.requestMicInput(active)
    }

    override fun onCleared() {
        voiceInputUseCase.requestMicInput(false)
        super.onCleared()
    }
}