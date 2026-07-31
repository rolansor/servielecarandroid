/**
 * Type.kt - Configuración de tipografía Material 3 de Serviaux (rediseño).
 *
 * Dos voces, según el handoff de diseño:
 * - Caprasimo 400 (display): títulos de pantalla, placas y cifras grandes.
 * - Figtree 400/600/700 (cuerpo): todo lo demás. Es una fuente variable,
 *   así que cada peso se instancia con el eje `wght`.
 *
 * Mínimos de legibilidad "de pie": cuerpo operativo 14sp; el secundario
 * (12–13.5sp) solo para metadatos; etiquetas de sección en MAYÚSCULAS con
 * letter-spacing amplio.
 */
package com.example.serviaux.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.serviaux.R

/** Caprasimo: solo existe en 400; se usa como voz display. */
val Caprasimo = FontFamily(
    Font(R.font.caprasimo, weight = FontWeight.Normal)
)

/** Figtree variable: pesos instanciados vía eje wght (API 26+). */
@OptIn(ExperimentalTextApi::class)
val Figtree = FontFamily(
    Font(
        R.font.figtree,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        R.font.figtree,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        R.font.figtree,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        R.font.figtree,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    ),
)

val Typography = Typography(
    // ── Voz display: Caprasimo (títulos, placas, cifras de dinero) ──
    displayLarge = TextStyle(
        fontFamily = Caprasimo,
        fontSize = 46.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Caprasimo,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.25).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = Caprasimo,
        fontSize = 27.sp,
        lineHeight = 32.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = Caprasimo,
        fontSize = 30.sp,
        lineHeight = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Caprasimo,
        fontSize = 26.sp,
        lineHeight = 32.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Caprasimo,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Caprasimo,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    // ── Voz de cuerpo: Figtree ──
    titleMedium = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.2.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    // Etiquetas de sección: MAYÚSCULAS con tracking .06em (aplicado en uso)
    labelSmall = TextStyle(
        fontFamily = Figtree,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.7.sp,
    ),
)
