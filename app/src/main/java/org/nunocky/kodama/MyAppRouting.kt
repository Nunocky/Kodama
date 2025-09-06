package org.nunocky.kodama

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import org.nunocky.kodama.ui.main.MainScreen
import org.nunocky.kodama.ui.setup.SetupScreen

@Serializable
object Setup

@Serializable
object Home

@Composable
fun MyAppRouting() {
    val context = LocalContext.current
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Setup) {
        composable<Home> {
            MainScreen(
                onBack = {
                    // アプリを終了
                    if (context is ComponentActivity) {
                        context.finish()
                    }
                },
            )
        }
        composable<Setup> {
            SetupScreen(onNavigateToMain = {
                navController.navigate(Home)
            })
        }
    }
}