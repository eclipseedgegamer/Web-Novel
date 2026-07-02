package com.eclipse.webnovel.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Selective liquid-glass surface for chrome/overlays (never behind reading text).
 *
 * Interim implementation: translucent tint + a 1dp specular border. True backdrop
 * blur (Haze) is added in the dedicated glass step — call sites won't change, only
 * this modifier's internals.
 */
@Composable
fun Modifier.liquidGlassSurface(
    shape: Shape = RoundedCornerShape(24.dp),
    tintAlpha: Float = 0.72f,
): Modifier {
    val surface = MaterialTheme.colorScheme.surface
    val specular = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.06f)),
    )
    return this
        .clip(shape)
        .background(surface.copy(alpha = tintAlpha), shape)
        .border(1.dp, specular, shape)
}
