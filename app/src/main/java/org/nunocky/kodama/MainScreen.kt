package org.nunocky.kodama

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.nunocky.kodama.ui.theme.KodamaTheme

@Composable
fun MainScreen() {
    KodamaTheme {
        Scaffold(modifier = Modifier.Companion.fillMaxSize()) { innerPadding ->
            Greeting(
                name = "Android",
                modifier = Modifier.Companion.padding(innerPadding)
            )
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    KodamaTheme {
        Greeting("Android")
    }
}