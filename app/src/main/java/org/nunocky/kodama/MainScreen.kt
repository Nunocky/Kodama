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

    val isPlaying by viewModel.isPlaying.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()

    KodamaTheme {
        Scaffold(modifier = Modifier.Companion.fillMaxSize()) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                // 再生ボタン(Pauseボタン)
                // 1. 一度押したら asset/sample_voice01.wavを再生する。
                // 2. もう一度押したら再生を停止する
                // 3. ファイルを最後まで再生したら停止状態に変わる
                if (isPlaying) {
                    // 停止ボタン
                    IconButton(onClick = {
                        viewModel.stopPlayback()
                    }) {
                        Text("⏸️")
                    }
                } else {
                    // 再生ボタン
                    IconButton(onClick = {
                        viewModel.startPlayback()
                    }) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                    }
                }
                
                // 会話状態の表示
                Text(
                    text = if (isSpeaking) "会話中" else "停止中",
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}
