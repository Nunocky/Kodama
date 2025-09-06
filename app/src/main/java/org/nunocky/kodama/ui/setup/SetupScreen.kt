package org.nunocky.kodama.ui.setup

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupScreenViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    enum class SetupState {
        INITIALIZING,
        COMPLETED,
    }

    private val _setupState = MutableStateFlow(SetupState.INITIALIZING)
    val setupState = _setupState.asStateFlow()

    // アプリの初期化処理
    private suspend fun processSetup() {
        delay(2000L)
    }

    init {
        _setupState.value = SetupState.INITIALIZING

        viewModelScope.launch {
            processSetup()
            _setupState.value = SetupState.COMPLETED
        }
    }
}

@Composable
fun SetupScreen(
    viewModel: SetupScreenViewModel = hiltViewModel(),
    onBack: () -> Unit = { },
    onNavigateToMain: () -> Unit = { },
) {
    val setupState by viewModel.setupState.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()

        if (setupState == SetupScreenViewModel.SetupState.COMPLETED) {
            onNavigateToMain()
        }
    }
}