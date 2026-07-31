/**
 * Theme.kt - Configuración del tema Material 3 de Serviaux (rediseño índigo).
 *
 * Decisiones de producto del handoff de diseño (`docs/nuevo_diseño/`):
 * - Color dinámico APAGADO: la app tiene identidad propia (índigo).
 * - Tema claro forzado: el modo oscuro del rediseño aún no está diseñado
 *   (pendiente declarado del prototipo). Cuando exista, mapear las rampas
 *   aquí y quitar el forzado.
 */
package com.example.serviaux.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Esquema claro mapeado desde las rampas del prototipo.
 *
 * Regla de contraste: primarios/secundarios sólidos usan el paso 700 con
 * texto Neutral100; los contenedores tintados usan 200 con texto 800.
 */
private val LightColorScheme = lightColorScheme(
    primary = Indigo700,
    onPrimary = Neutral100,
    primaryContainer = Indigo200,
    onPrimaryContainer = Indigo800,
    inversePrimary = Indigo300,
    secondary = Aqua700,
    onSecondary = Neutral100,
    secondaryContainer = Aqua200,
    onSecondaryContainer = Aqua800,
    tertiary = Neutral700,
    onTertiary = Neutral100,
    tertiaryContainer = Neutral300,
    onTertiaryContainer = Neutral800,
    error = ErrorRed,
    onError = OnErrorWhite,
    errorContainer = ErrorContainerRed,
    onErrorContainer = OnErrorContainerRed,
    background = BgLila,
    onBackground = TextInk,
    surface = BgLila,
    onSurface = TextInk,
    surfaceVariant = SurfaceLila,
    onSurfaceVariant = Neutral700,
    surfaceTint = Indigo700,
    inverseSurface = Neutral900,
    inverseOnSurface = Neutral100,
    outline = Neutral500,
    outlineVariant = Neutral300,
    // Contenedores de superficie M3: las tarjetas y barras usan estos.
    surfaceBright = BgLila,
    surfaceDim = Neutral300,
    surfaceContainerLowest = Neutral100,
    surfaceContainerLow = SurfaceLila,
    surfaceContainer = SurfaceLila,
    surfaceContainerHigh = Neutral200,
    surfaceContainerHighest = Neutral200,
)

/**
 * Tema principal de la aplicación.
 *
 * Siempre claro y sin color dinámico, a propósito: ver cabecera del archivo.
 */
@Composable
fun ServiauxTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = ServiauxShapes,
        content = content
    )
}
