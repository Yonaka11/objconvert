package com.mikazuki.pocketfamiliar.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PocketFamiliarColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFB8A7FF),
    secondary = Color(0xFF8DCCFF),
    tertiary = Color(0xFFFFC4DD),
    background = Color(0xFF101123),
    surface = Color(0xFF181A31),
    surfaceVariant = Color(0xFF252840),
    onPrimary = Color(0xFF26155F),
    onSecondary = Color(0xFF00344F),
    onBackground = Color(0xFFF5F2FF),
    onSurface = Color(0xFFF5F2FF),
    onSurfaceVariant = Color(0xFFD5D0EA),
)

@Composable
fun PocketFamiliarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PocketFamiliarColors,
        content = content,
    )
}
