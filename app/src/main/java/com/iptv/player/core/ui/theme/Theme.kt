package com.iptv.player.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

private val DarkColors = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF60A5FA),
    background = Color(0xFF0E0F13),
    onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF181A20),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF1F2430),
    border = Color(0xFF3B82F6),
)

@Composable
fun IptvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content,
    )
}
