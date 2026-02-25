package com.example.serviaux.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SteelBlue80,
    onPrimary = OnDarkPrimary,
    primaryContainer = SteelBlue40,
    onPrimaryContainer = SteelBlue90,
    secondary = Amber80,
    onSecondary = OnDarkSecondary,
    secondaryContainer = Amber40,
    onSecondaryContainer = Amber90,
    tertiary = MetalGray80,
    onTertiary = MetalGray30,
    tertiaryContainer = MetalGray40,
    onTertiaryContainer = MetalGray90,
    error = BrakeRed80,
    onError = OnDarkPrimary,
    errorContainer = BrakeRed40,
    onErrorContainer = BrakeRed80,
    background = DarkCharcoal,
    onBackground = OnDarkBackground,
    surface = DarkNavy,
    onSurface = OnDarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSurfaceVariant,
    outline = OutlineDark,
)

private val LightColorScheme = lightColorScheme(
    primary = SteelBlue40,
    onPrimary = OnLightPrimary,
    primaryContainer = SteelBlue90,
    onPrimaryContainer = SteelBlue30,
    secondary = Amber40,
    onSecondary = OnLightSecondary,
    secondaryContainer = Amber90,
    onSecondaryContainer = Amber30,
    tertiary = MetalGray40,
    onTertiary = OnLightPrimary,
    tertiaryContainer = MetalGray90,
    onTertiaryContainer = MetalGray30,
    error = BrakeRed40,
    onError = OnLightPrimary,
    errorContainer = BrakeRed80,
    onErrorContainer = BrakeRed40,
    background = LightGray,
    onBackground = OnLightBackground,
    surface = White,
    onSurface = OnLightSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = OnLightSurfaceVariant,
    outline = OutlineLight,
)

@Composable
fun ServiauxTheme(
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
