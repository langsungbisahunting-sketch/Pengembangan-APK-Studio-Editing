package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val StudioDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF041318),
    primaryContainer = Color(0xFF003B46),
    onPrimaryContainer = ElectricCyan,
    secondary = NeonCoral,
    onSecondary = Color.White,
    secondaryContainer = NeonCoralSoft,
    onSecondaryContainer = Color(0xFFFFD9E2),
    tertiary = VioletOverlay,
    onTertiary = Color.White,
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
    outlineVariant = Color(0xFF212636),
    error = NeonCoral,
    onError = Color.White
)

private val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun VeloCutTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StudioDarkColorScheme,
        typography = VeloCutTypography,
        shapes = StudioShapes,
        content = content
    )
}
