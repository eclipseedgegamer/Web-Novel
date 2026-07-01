package com.eclipse.webnovel.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Emerald Green is the default accent. The full four-preset theme system
// (Emerald / Dark-AMOLED / Light / Sand) lands in a follow-up Phase 1 step;
// this is the minimal seed so the app themes correctly from day one.
private val Emerald = Color(0xFF10B981)
private val EmeraldContainer = Color(0xFF0B3D2E)

private val LightColors = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Emerald,
    onPrimary = Color(0xFF00251A),
    background = Color(0xFF04140F),
    surface = Color(0xFF04140F),
)

@Composable
fun WebNovelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
