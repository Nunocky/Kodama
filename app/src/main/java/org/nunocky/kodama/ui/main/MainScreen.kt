package org.nunocky.kodama.ui.main

import android.content.Context
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.nunocky.kodama.ui.theme.KodamaTheme
import org.nunocky.kodama.usecase.VoiceInputState

fun createAnimatedImageDrawableFromImageDecoder(
    context: Context,
    uri: Uri
): AnimatedImageDrawable {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    val drawable = ImageDecoder.decodeDrawable(source)
    return drawable as AnimatedImageDrawable
}

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onBack: () -> Unit = { },
) {
    // マイクの有効・無効
    val isMicActive by viewModel.isMicActive.collectAsState()

    // 現在の音声処理状態
    val voiceInputState by viewModel.voiceInputState.collectAsState()

    // UI操作が可能かどうか
    val canUseUI = voiceInputState == VoiceInputState.UNVOICING

    KodamaTheme {
        BackHandler {
            onBack()
        }

        Scaffold(modifier = Modifier.Companion.fillMaxSize()) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {

                Spacer(modifier = Modifier.Companion.padding(vertical = 8.dp))

                HorizontalDivider(
                    Modifier.padding(vertical = 8.dp),
                    DividerDefaults.Thickness,
                    DividerDefaults.color,
                )

                // マイク入力
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = CenterVertically,
                ) {
                    Text(text = "マイク入力 : ${if (isMicActive) "ON" else "OFF"}")
                    Spacer(modifier = Modifier.weight(1f))
                    Switch(
                        checked = isMicActive,
                        onCheckedChange = { checked ->
                            viewModel.onMicActiveChanged(checked)
                        },
                    )
                }

                // 音声入力の状態
                Text(
                    text = "音声入力の状態: ${
                        when (voiceInputState) {
                            VoiceInputState.UNVOICING -> "待機中"
                            VoiceInputState.VOICING -> "発話中"
                            VoiceInputState.PLAYING -> "再生中"
                        }
                    }"
                )
            }
        }
    }
}