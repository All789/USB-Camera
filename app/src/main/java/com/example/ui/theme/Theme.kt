package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkMonitorColorScheme = darkColorScheme(
    primary = SignalCyan,
    onPrimary = Color.Black,
    primaryContainer = SignalCyanDim,
    onPrimaryContainer = Color.White,
    secondary = SignalGreen,
    onSecondary = Color.Black,
    tertiary = SignalAmber,
    onTertiary = Color.Black,
    background = MonitorBackground,
    onBackground = MonitorTextPrimary,
    surface = MonitorSurface,
    onSurface = MonitorTextPrimary,
    surfaceVariant = MonitorSurfaceVariant,
    onSurfaceVariant = MonitorTextSecondary,
    outline = MonitorTextMuted,
    error = SignalRed,
    onError = Color.White
)

@Composable
fun EasyUsbCameraTheme(
    content: @Composable () -> Unit
) {
    // A professional HDMI monitor always maintains dark theme for optimal video contrast and low glare
    MaterialTheme(
        colorScheme = DarkMonitorColorScheme,
        typography = Typography,
        content = content
    )
}
