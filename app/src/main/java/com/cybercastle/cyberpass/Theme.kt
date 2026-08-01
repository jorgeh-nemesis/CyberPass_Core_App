package com.cybercastle.cyberpass

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

// Reserved strictly for sensitive credentials, hashes, and branding accents.
// Use MonoCredentialStyle (or this FontFamily directly) when rendering/editing
// passwords, verifiers, or the "CyberPass" wordmark - never for body text.
val JetBrainsMono = FontFamily.Monospace

private val AppSansSerif = FontFamily.SansSerif

// Apply to password/PIN fields, hashes, and other raw credential text.
val MonoCredentialStyle = TextStyle(fontFamily = JetBrainsMono)

// Standard surface tone used across cards, dialogs, and settings rows for
// consistent contrast against the near-black scaffold background.
val SurfaceTone = Color(0xFF222222)

val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = AppSansSerif),
    displayMedium = TextStyle(fontFamily = AppSansSerif),
    displaySmall = TextStyle(fontFamily = AppSansSerif),
    headlineLarge = TextStyle(fontFamily = AppSansSerif),
    headlineMedium = TextStyle(fontFamily = AppSansSerif),
    headlineSmall = TextStyle(fontFamily = AppSansSerif),
    titleLarge = TextStyle(fontFamily = AppSansSerif),
    titleMedium = TextStyle(fontFamily = AppSansSerif),
    titleSmall = TextStyle(fontFamily = AppSansSerif),
    bodyLarge = TextStyle(fontFamily = AppSansSerif),
    bodyMedium = TextStyle(fontFamily = AppSansSerif),
    bodySmall = TextStyle(fontFamily = AppSansSerif),
    labelLarge = TextStyle(fontFamily = AppSansSerif),
    labelMedium = TextStyle(fontFamily = AppSansSerif),
    labelSmall = TextStyle(fontFamily = AppSansSerif)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF2EFC54),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF111111),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF00FF41),
    background = Color(0xFF111111),
    surface = SurfaceTone,
    surfaceVariant = SurfaceTone,
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
