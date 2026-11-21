package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BlazeDarkColorScheme = darkColorScheme(
    primary = BlazeOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF421C00),
    onPrimaryContainer = BlazeAmber,
    secondary = BlazeAmber,
    onSecondary = Color(0xFF1A1200),
    secondaryContainer = CardSurfaceElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = ZoneCyan,
    onTertiary = Color(0xFF001F24),
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = TacticalSurface,
    onSurface = TextPrimary,
    surfaceVariant = CardSurface,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = CrimsonAlert,
    onError = Color.White
)

@Composable
fun BlazeDropTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BlazeDarkColorScheme,
        typography = BlazeTypography,
        content = content
    )
}
