package com.example.smartcoaster.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SmartColors = lightColorScheme(
    primary = Color(0xFF0058BC),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E2FF),
    secondary = Color(0xFF355DA3),
    surface = Color(0xFFFAF9FE),
    surfaceContainer = Color(0xFFEEEDF3),
    surfaceContainerLow = Color(0xFFF4F3F8),
    onSurface = Color(0xFF1A1B1F),
    onSurfaceVariant = Color(0xFF414755)
)

@Composable
fun SmartCoasterTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = SmartColors, content = content)
}
