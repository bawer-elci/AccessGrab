package com.baverelci.accessgrab.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    secondary = PrimaryGreen,
    tertiary = AccentYellow,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    error = ErrorRed,
    onPrimary = DarkBackground,
    onSecondary = DarkBackground,
    onBackground = TextHighEmphasis,
    onSurface = TextHighEmphasis,
    outline = OutlineColor
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = PrimaryGreen,
    tertiary = AccentYellow,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    error = ErrorRed,
    onPrimary = DarkBackground,
    onSecondary = DarkBackground,
    onBackground = TextHighEmphasis,
    onSurface = TextHighEmphasis,
    outline = OutlineColor
)

private val UltraHighContrastColorScheme = darkColorScheme(
    primary = Color(0xFFFFEA00),
    secondary = Color(0xFF00FF66),
    tertiary = Color(0xFF00E5FF),
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFF181800),
    error = Color(0xFFFF4D4D),
    onPrimary = Color(0xFF000000),
    onSecondary = Color(0xFF000000),
    onBackground = Color(0xFFFFEA00),
    onSurface = Color(0xFFFFFFFF),
    outline = Color(0xFFFFEA00)
)

@Composable
fun AccessGrabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrastLargeText: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        highContrastLargeText -> UltraHighContrastColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val typography = if (highContrastLargeText) LargeHighContrastTypography else Typography
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
