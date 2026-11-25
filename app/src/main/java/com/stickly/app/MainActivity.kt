package com.stickly.app

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.stickly.app.data.ThemeMode
import com.stickly.app.ui.MainViewModel
import com.stickly.app.ui.SticklyApp

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val dark = isDarkTheme(viewModel.state.value.themeMode)
        enableEdgeToEdge(
            statusBarStyle = systemBarStyle(dark),
            navigationBarStyle = systemBarStyle(dark),
        )
        applySystemBars(dark)
        handleIncomingIntent(intent)
        setContent {
            SticklyApp(viewModel, onApplySystemBars = ::applySystemBars)
        }
    }

    private fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
        ThemeMode.SYSTEM ->
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    private fun systemBarStyle(dark: Boolean): SystemBarStyle = if (dark) {
        SystemBarStyle.dark(Color.TRANSPARENT)
    } else {
        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
    }

    private fun applySystemBars(dark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent) {
        val link = when (intent.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }
        if (!link.isNullOrBlank()) viewModel.setUrl(link)
    }
}
