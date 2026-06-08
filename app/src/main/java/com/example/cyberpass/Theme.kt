package com.example.cyberpass

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

private val JetBrainsMono = FontFamily.Monospace

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = JetBrainsMono),
    displayMedium = TextStyle(fontFamily = JetBrainsMono),
    displaySmall = TextStyle(fontFamily = JetBrainsMono),
    headlineLarge = TextStyle(fontFamily = JetBrainsMono),
    headlineMedium = TextStyle(fontFamily = JetBrainsMono),
    headlineSmall = TextStyle(fontFamily = JetBrainsMono),
    titleLarge = TextStyle(fontFamily = JetBrainsMono),
    titleMedium = TextStyle(fontFamily = JetBrainsMono),
    titleSmall = TextStyle(fontFamily = JetBrainsMono),
    bodyLarge = TextStyle(fontFamily = JetBrainsMono),
    bodyMedium = TextStyle(fontFamily = JetBrainsMono),
    bodySmall = TextStyle(fontFamily = JetBrainsMono),
    labelLarge = TextStyle(fontFamily = JetBrainsMono),
    labelMedium = TextStyle(fontFamily = JetBrainsMono),
    labelSmall = TextStyle(fontFamily = JetBrainsMono)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF2EFC54),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF111111),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF00FF41),
    background = Color(0xFF111111),
    surface = Color(0xFF121212),
    onSurface = Color.White
)

@Composable
fun CyberPassTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = AppTypography,
        content = content
    )
}
