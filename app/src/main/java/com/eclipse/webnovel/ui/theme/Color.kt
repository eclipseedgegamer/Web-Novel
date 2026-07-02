package com.eclipse.webnovel.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// One accent across the app: Emerald. Sand swaps to Terracotta.
val Emerald = Color(0xFF10B981)
val EmeraldBright = Color(0xFF34D399)
val Terracotta = Color(0xFFC2673E)

// ── Emerald (default): deep-green surfaces, emerald accent ──────────────
val EmeraldColors: ColorScheme = darkColorScheme(
    primary = EmeraldBright,
    onPrimary = Color(0xFF00251A),
    primaryContainer = Color(0xFF0E3A2B),
    onPrimaryContainer = Color(0xFFB9F6DD),
    secondary = Emerald,
    onSecondary = Color(0xFF00251A),
    background = Color(0xFF04140F),
    onBackground = Color(0xFFE7F0EB),
    surface = Color(0xFF071F16),
    onSurface = Color(0xFFE7F0EB),
    surfaceVariant = Color(0xFF102A20),
    onSurfaceVariant = Color(0xFFA9C3B7),
    outline = Color(0xFF2C4A3D),
)

// ── Dark (AMOLED): pure black, emerald accent ──────────────────────────
val DarkColors: ColorScheme = darkColorScheme(
    primary = EmeraldBright,
    onPrimary = Color(0xFF00251A),
    primaryContainer = Color(0xFF0C2D22),
    onPrimaryContainer = Color(0xFFB9F6DD),
    secondary = Emerald,
    onSecondary = Color(0xFF00140E),
    background = Color(0xFF000000),
    onBackground = Color(0xFFECECEC),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF161616),
    onSurfaceVariant = Color(0xFFBDBDBD),
    outline = Color(0xFF2A2A2A),
)

// ── Light: near-white surfaces, emerald accent ─────────────────────────
val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF0E9E6E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB9F6DD),
    onPrimaryContainer = Color(0xFF00281B),
    secondary = Color(0xFF0E9E6E),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFFBFCFB),
    onBackground = Color(0xFF10201A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF10201A),
    surfaceVariant = Color(0xFFECF2EE),
    onSurfaceVariant = Color(0xFF44574E),
    outline = Color(0xFFC4D2CB),
)

// ── Sand: cream surfaces, terracotta accent (serif headers) ────────────
val SandColors: ColorScheme = lightColorScheme(
    primary = Terracotta,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF3D9C9),
    onPrimaryContainer = Color(0xFF3A1B0C),
    secondary = Color(0xFF7C6F5B),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF3EBDD),
    onBackground = Color(0xFF3A2E22),
    surface = Color(0xFFFAF3E7),
    onSurface = Color(0xFF3A2E22),
    surfaceVariant = Color(0xFFEBE0CE),
    onSurfaceVariant = Color(0xFF6B5D4C),
    outline = Color(0xFFCBBBA2),
)
