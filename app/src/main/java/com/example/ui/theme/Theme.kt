package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val KomikNarasiColorScheme = darkColorScheme(
    primary = EnergyOrange,
    onPrimary = PureWhite,
    primaryContainer = EnergyOrangeSoft,
    onPrimaryContainer = Color(0xFFFFD8BD),
    secondary = GoldAccent,
    onSecondary = Color(0xFF161204),
    secondaryContainer = GoldSoft,
    onSecondaryContainer = Color(0xFFFFECB3),
    tertiary = PureWhite,
    onTertiary = StudioObsidian,
    background = StudioObsidian,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = StudioSurface,
    surfaceContainer = StudioSurfaceElevated,
    surfaceContainerHigh = StudioCard,
    outline = StudioBorder,
    outlineVariant = Color(0xFF26262F),
    error = CrimsonAlert,
    onError = PureWhite
)

private val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun KomikNarasiTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KomikNarasiColorScheme,
        typography = VeloCutTypography,
        shapes = StudioShapes,
        content = content
    )
}
