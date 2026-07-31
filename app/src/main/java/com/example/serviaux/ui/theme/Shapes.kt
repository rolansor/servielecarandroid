/**
 * Shapes.kt - Formas del tema de Serviaux (rediseño).
 *
 * El sistema sobre-redondea: tarjetas y secciones 28dp, diálogos 28dp,
 * y todo lo pequeño (chips, botones, inputs de búsqueda) en píldora.
 * Los botones M3 ya son píldora por defecto (CircleShape).
 *
 * Mapeo M3:
 * - extraSmall: menús y tooltips (moderado para que los dropdown no floten raros)
 * - small: chips y campos
 * - medium: tarjetas (Card usa medium por defecto)
 * - large/extraLarge: secciones grandes, diálogos, bottom sheets
 */
package com.example.serviaux.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val ServiauxShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
