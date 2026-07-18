package com.adshield.detector.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BrandPrimary = Color(0xFF2F6FED)
val BrandDanger = Color(0xFFE5484D)
val BrandWarning = Color(0xFFF5A623)
val BrandSafe = Color(0xFF12B76A)
val BackgroundDark = Color(0xFF0F1420)
val SurfaceDark = Color(0xFF171E2E)
val SurfaceDarkAlt = Color(0xFF1F2A40)
val TextPrimary = Color(0xFFEAF0FB)
val TextSecondary = Color(0xFF93A0BD)

private val AdShieldColorScheme = darkColorScheme(
    primary = BrandPrimary,
    error = BrandDanger,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceDarkAlt,
    onSurfaceVariant = TextSecondary
)

@Composable
fun AdShieldTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AdShieldColorScheme,
        content = content
    )
}
