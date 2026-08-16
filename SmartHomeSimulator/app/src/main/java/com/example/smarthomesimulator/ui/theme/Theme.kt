package com.example.smarthomesimulator.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = LuxuryGold,
    onPrimary = Color.Black,
    primaryContainer = LuxuryGoldVariant,
    onPrimaryContainer = Color.White,
    secondary = LuxuryEmerald,
    onSecondary = Color.White,
    background = LuxuryOnyx,
    onBackground = LuxuryOffWhite,
    surface = LuxuryCharcoal,
    onSurface = LuxuryOffWhite,
    surfaceVariant = LuxuryCharcoal.copy(alpha = 0.7f),
    onSurfaceVariant = LuxuryGrey,
    outline = LuxuryGrey.copy(alpha = 0.5f),
    error = Color(0xFFFF4842)
)

private val LightColorScheme = lightColorScheme(
    primary = LuxuryLightAccent,
    onPrimary = Color.White,
    secondary = LuxuryEmerald,
    onSecondary = Color.White,
    background = LuxuryLightBg,
    onBackground = LuxuryLightAccent,
    surface = LuxuryLightSurface,
    onSurface = LuxuryLightAccent,
    surfaceVariant = Color(0xFFF4F6F8),
    onSurfaceVariant = LuxuryGrey,
    outline = LuxuryGrey
)

@Composable
fun SmartHomeSimulatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
