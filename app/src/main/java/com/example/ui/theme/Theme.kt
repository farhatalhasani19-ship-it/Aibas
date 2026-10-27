package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RomanticDarkColorScheme = darkColorScheme(
    primary = LavenderPrimary,
    onPrimary = LavenderOnPrimary,
    primaryContainer = LavenderContainer,
    onPrimaryContainer = LavenderOnContainer,
    secondary = SoftRoseSecondary,
    onSecondary = SoftRoseOnSecondary,
    secondaryContainer = SoftRoseContainer,
    onSecondaryContainer = SoftRoseOnContainer,
    tertiary = StarGoldTertiary,
    onTertiary = StarGoldOnTertiary,
    tertiaryContainer = StarGoldContainer,
    onTertiaryContainer = StarGoldOnContainer,
    background = MidnightBase,
    onBackground = TextPrimary,
    surface = MidnightSurface,
    onSurface = TextPrimary,
    surfaceVariant = MidnightCard,
    onSurfaceVariant = TextSecondary,
    outline = MidnightCardBorder,
    outlineVariant = Color(0x22FFFFFF)
)

@Composable
fun DairySayanggTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RomanticDarkColorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    DairySayanggTheme(content = content)
}
