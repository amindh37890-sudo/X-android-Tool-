package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NexusColorScheme = darkColorScheme(
    primary = NexusCyan,
    onPrimary = Color(0xFF001F28),
    primaryContainer = Color(0xFF004D5A),
    onPrimaryContainer = Color(0xFF80F1FF),

    secondary = NexusViolet,
    onSecondary = Color(0xFF280063),
    secondaryContainer = Color(0xFF45009E),
    onSecondaryContainer = Color(0xFFEADBFF),

    tertiary = NexusGreen,
    onTertiary = Color(0xFF003919),
    tertiaryContainer = Color(0xFF005227),
    onTertiaryContainer = Color(0xFF67FE9E),

    background = NexusBackground,
    onBackground = NexusTextPrimary,
    surface = NexusSurface,
    onSurface = NexusTextPrimary,
    surfaceVariant = NexusSurfaceVariant,
    onSurfaceVariant = NexusTextSecondary,

    outline = NexusCardBorder,
    outlineVariant = Color(0xFF1E293B),

    error = NexusRed,
    onError = Color.White
)

@Composable
fun NexusTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NexusColorScheme,
        typography = Typography,
        content = content
    )
}
