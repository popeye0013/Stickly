package com.stickly.app.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.stickly.app.data.ThemeMode

private val Violet = Color(0xFF6750A4)
private val VioletLight = Color(0xFFEADDFF)
private val VioletDark = Color(0xFF381E72)
private val Teal = Color(0xFF00867C)
private val Coral = Color(0xFFFF7A59)

private val LightColors = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = VioletLight,
    onPrimaryContainer = VioletDark,
    secondary = Teal,
    secondaryContainer = Color(0xFFC8F2EC),
    tertiary = Coral,
    tertiaryContainer = Color(0xFFFFDAD2),
    surface = Color(0xFFFFFBFE),
    surfaceContainer = Color(0xFFF3EDF7),
    surfaceContainerHigh = Color(0xFFECE6F0),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = VioletDark,
    primaryContainer = VioletDark,
    onPrimaryContainer = VioletLight,
    secondary = Color(0xFF6AD5C9),
    secondaryContainer = Color(0xFF00524D),
    tertiary = Color(0xFFFFB49F),
    tertiaryContainer = Color(0xFF7A2A15),
    surface = Color(0xFF141218),
    surfaceContainer = Color(0xFF1E1B22),
    surfaceContainerHigh = Color(0xFF29252E),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SticklyTheme(
    dynamicColor: Boolean,
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (dark) DarkColors else LightColors
    }

    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        typography = SticklyTypography,
        content = content,
    )
}
