package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CyberGreen,
    onPrimary = OnCyberGreen,
    secondary = TacticalTeal,
    background = SecureBlack,
    surface = SecureCharcoal,
    onBackground = SecureTextWhite,
    onSurface = SecureTextWhite,
    surfaceVariant = SecureSlate,
    onSurfaceVariant = SecureTextMuted,
    error = SecureTextError,
    onError = SecureBlack
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF007E3A),
    onPrimary = Color.White,
    secondary = Color(0xFF00838F),
    background = Color(0xFFF5F7FA),
    surface = Color.White,
    onBackground = Color(0xFF0F1215),
    onSurface = Color(0xFF0F1215),
    surfaceVariant = Color(0xFFE1E5EB),
    onSurfaceVariant = Color(0xFF455A64),
    error = Color(0xFFD32F2F),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force Dark theme by default for military-grade secure styling
  dynamicColor: Boolean = false, // Use our custom color tokens for a uniform tactical aesthetic
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
