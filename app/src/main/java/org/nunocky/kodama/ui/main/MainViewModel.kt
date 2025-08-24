package org.nunocky.kodama.ui.main

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    private val audioPlayUseCase: AudioPlayUseCase
) : ViewModel() {

    val isSpeaking: StateFlow<Boolean> = audioPlayUseCase.isSpeaking
    val isPlaying: StateFlow<Boolean> = audioPlayUseCase.isPlaying

    private val _isMicInputEnabled = MutableStateFlow(false)
    val isMicInputEnabled: StateFlow<Boolean> = _isMicInputEnabled.asStateFlow()

    fun setMicInputEnabled(enabled: Boolean) {
        _isMicInputEnabled.value = enabled
        if (enabled) {
            audioPlayUseCase.startMicInput()
        } else {
            audioPlayUseCase.stopMicInput()
        }
    }
}