package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF04131D),
    primaryContainer = Color(0xFF0C354E),
    onPrimaryContainer = Color(0xFFB9E6FE),
    secondary = PrismViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3B1B5E),
    onSecondaryContainer = PrismVioletSoft,
    tertiary = AmberSpark,
    onTertiary = Color(0xFF1F1300),
    tertiaryContainer = Color(0xFF452B03),
    onTertiaryContainer = Color(0xFFFDE68A),
    background = ObsidianBg,
    onBackground = SlateTextPrimary,
    surface = ObsidianSurface,
    onSurface = SlateTextPrimary,
    surfaceVariant = ObsidianSurfaceHigh,
    onSurfaceVariant = SlateTextSecondary,
    outline = SlateOutline,
    error = Color(0xFFF87171),
    onError = Color(0xFF2D0606)
)

private val LightColorScheme = lightColorScheme(
    primary = DeepIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF075985),
    secondary = DeepViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E8FF),
    onSecondaryContainer = Color(0xFF581C87),
    tertiary = WarmAmber,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF92400E),
    background = DaylightBg,
    onBackground = InkTextPrimary,
    surface = DaylightSurface,
    onSurface = InkTextPrimary,
    surfaceVariant = DaylightSurfaceHigh,
    onSurfaceVariant = InkTextSecondary,
    outline = DaylightOutline,
    error = Color(0xFFDC2626),
    onError = Color.White
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
