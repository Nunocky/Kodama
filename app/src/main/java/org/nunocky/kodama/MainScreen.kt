package org.nunocky.kodama

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import org.nunocky.kodama.ui.theme.KodamaTheme

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val isMicInputEnabled = viewModel.isMicInputEnabled.collectAsState(initial = false).value

    KodamaTheme {
        Scaffold(modifier = Modifier.Companion.fillMaxSize()) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                // マイク入力切替スイッチ
                Switch(
                    checked = isMicInputEnabled,
                    onCheckedChange = { checked ->
                        viewModel.setMicInputEnabled(checked)
                    },
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(text = if (isMicInputEnabled) "マイク入力ON" else "マイク入力OFF")

                // 会話状態の表示
                Text(
                    text = if (isSpeaking) "会話中" else "停止中",
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}
