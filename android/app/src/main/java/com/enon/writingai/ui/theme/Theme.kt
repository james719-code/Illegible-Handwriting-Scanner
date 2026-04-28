package com.enon.writingai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ForestPrimaryContainer,
    onPrimary = SurfaceContainerLowestTone,
    primaryContainer = ForestPrimary,
    onPrimaryContainer = SurfaceContainerLowestTone,
    secondary = Color(0xFF9CC9C1),
    onSecondary = Color(0xFF102725),
    tertiary = Color(0xFFE4B186),
    onTertiary = Color(0xFF351A04),
    background = Night,
    onBackground = SurfaceContainerLowestTone,
    surface = NightCard,
    onSurface = SurfaceContainerLowestTone,
    surfaceVariant = NightSoft,
    onSurfaceVariant = Color(0xFFB7C6C1),
    surfaceTint = ForestPrimaryContainer,
    outline = Color(0xFF7F9691),
    outlineVariant = Color(0xFF5E716D),
    inverseSurface = InverseOnSurfaceTone,
    inverseOnSurface = InverseSurfaceTone,
    surfaceBright = Color(0xFF31423F),
    surfaceDim = Color(0xFF101816),
    surfaceContainerLowest = Color(0xFF111917),
    surfaceContainerLow = Color(0xFF182320),
    surfaceContainer = Color(0xFF1F2B28),
    surfaceContainerHigh = Color(0xFF263632),
    surfaceContainerHighest = Color(0xFF2E413D),
)

private val LightColorScheme = lightColorScheme(
    primary = ForestPrimary,
    onPrimary = SurfaceContainerLowestTone,
    primaryContainer = ForestPrimaryContainer,
    onPrimaryContainer = SurfaceContainerLowestTone,
    secondary = Color(0xFF5D716C),
    onSecondary = SurfaceContainerLowestTone,
    tertiary = CopperAccent,
    onTertiary = SurfaceContainerLowestTone,
    background = PaperBase,
    onBackground = InkOnPaper,
    surface = PaperBase,
    onSurface = InkOnPaper,
    surfaceVariant = SurfaceContainerTone,
    onSurfaceVariant = OnSurfaceMuted,
    surfaceTint = ForestPrimary,
    outline = OutlineSoft,
    outlineVariant = OutlineVariantSoft,
    inverseSurface = InverseSurfaceTone,
    inverseOnSurface = InverseOnSurfaceTone,
    surfaceBright = SurfaceContainerLowestTone,
    surfaceDim = SurfaceContainerHighTone,
    surfaceContainerLowest = SurfaceContainerLowestTone,
    surfaceContainerLow = SurfaceContainerLowTone,
    surfaceContainer = SurfaceContainerTone,
    surfaceContainerHigh = SurfaceContainerHighTone,
    surfaceContainerHighest = SurfaceContainerHighestTone,
)

@Composable
fun WritingAiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}
