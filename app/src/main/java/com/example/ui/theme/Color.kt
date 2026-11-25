package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Komik Narasi Studio — Luxury Obsidian, Clean White & Energy Orange Palette
val StudioObsidian = Color(0xFF0B0B0D)
val StudioSurface = Color(0xFF16161A)
val StudioSurfaceElevated = Color(0xFF1E1E24)
val StudioCard = Color(0xFF24242C)
val StudioBorder = Color(0xFF32323D)

val EnergyOrange = Color(0xFFFF6B00)
val EnergyOrangeDark = Color(0xFFD95800)
val EnergyOrangeSoft = Color(0xFF3A1C07)
val GoldAccent = Color(0xFFFFC107)
val GoldSoft = Color(0xFF332704)

val PureWhite = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFFF5F5F7)
val TextSecondary = Color(0xFFA8A8B3)
val TextMuted = Color(0xFF71717D)

val CrimsonAlert = Color(0xFFFF4D4F)
val CrimsonSoft = Color(0xFF3F1517)
val EmeraldSuccess = Color(0xFF22C55E)

val KomikNarasiHeroGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF16161A),
        Color(0xFFFF6B00),
        Color(0xFFFFC107)
    )
)

val KomikNarasiProgressGradient = Brush.horizontalGradient(
    colors = listOf(
        Color(0xFF16161A),
        Color(0xFFFF6B00),
        Color(0xFFFFC107)
    )
)
