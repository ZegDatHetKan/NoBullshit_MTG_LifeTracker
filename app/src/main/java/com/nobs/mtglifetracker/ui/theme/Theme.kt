package com.nobs.mtglifetracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.nobs.mtglifetracker.model.ThemeMode

/** Lets deeply nested UI pick the right player-accent variant without threading a flag. */
val LocalIsDark = staticCompositionLocalOf { true }

private val DarkScheme = darkColorScheme(
    primary = AccentDark,
    onPrimary = SurfaceDark,
    background = SurfaceDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceDarkElevated,
    onSurfaceVariant = OnSurfaceDark,
    error = DangerColor,
)

private val LightScheme = lightColorScheme(
    primary = AccentLight,
    onPrimary = SurfaceLightElevated,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceLightElevated,
    onSurfaceVariant = OnSurfaceLight,
    error = DangerColor,
)

@Composable
fun MtgTrackerTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    CompositionLocalProvider(LocalIsDark provides dark) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            content = content,
        )
    }
}
