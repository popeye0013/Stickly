package com.stickly.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stickly.app.data.ThemeMode

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SticklyApp(
    viewModel: MainViewModel,
    onApplySystemBars: (Boolean) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val systemDark = isSystemInDarkTheme()
    val dark = when (state.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    LaunchedEffect(dark) { onApplySystemBars(dark) }

    BackHandler(enabled = state.screen is Screen.Welcome) {
        viewModel.finishWelcome()
    }

    BackHandler(enabled = state.screen !is Screen.Home && state.screen !is Screen.Welcome) {
        viewModel.openHome()
    }

    SticklyTheme(dynamicColor = state.dynamicColor, themeMode = state.themeMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            AnimatedContent(
                targetState = state.screen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(240)) togetherWith fadeOut(animationSpec = tween(120))
                },
                label = "Stickly screens",
            ) { screen ->
                when (screen) {
                    Screen.Welcome -> WelcomeScreen(onContinue = viewModel::finishWelcome)
                    Screen.Home -> HomeScreen(state, viewModel)
                    Screen.Settings -> SettingsScreen(state, viewModel)
                    Screen.Profile -> ProfileScreen(state, viewModel)
                    is Screen.Detail -> {
                        val pack = viewModel.packById(screen.packId)
                        if (pack != null) {
                            PackDetailScreen(state, pack, viewModel)
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background,
                            ) {
                                Text(
                                    text = "This pack is no longer available.",
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                }
            }

            when (val import = state.importState) {
                is ImportState.Running -> ImportProgressDialog(import, viewModel::cancelImport)
                is ImportState.Success, is ImportState.Error ->
                    ImportResultDialog(import, viewModel::dismissImportResult)
                else -> Unit
            }
        }
    }
}
