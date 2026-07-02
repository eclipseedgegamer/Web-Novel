package com.eclipse.webnovel.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppTheme(val label: String) {
    EMERALD("Emerald"),
    DARK("Dark"),
    LIGHT("Light"),
    SAND("Sand");

    val isLight: Boolean get() = this == LIGHT || this == SAND

    companion object {
        val Default = EMERALD
        fun fromName(name: String?): AppTheme = entries.firstOrNull { it.name == name } ?: Default
    }
}

@Composable
fun WebNovelTheme(
    appTheme: AppTheme = AppTheme.Default,
    content: @Composable () -> Unit,
) {
    val colors = when (appTheme) {
        AppTheme.EMERALD -> EmeraldColors
        AppTheme.DARK -> DarkColors
        AppTheme.LIGHT -> LightColors
        AppTheme.SAND -> SandColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = appTheme.isLight
            controller.isAppearanceLightNavigationBars = appTheme.isLight
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = WebNovelTypography,
        content = content,
    )
}
