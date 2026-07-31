/**
 * Color.kt - Paleta de colores del tema de Serviaux (rediseño índigo).
 *
 * Tokens tomados del handoff de diseño en `docs/nuevo_diseño/` (prototipo
 * "Serviaux App.dc.html"): tres rampas tonales 100→900 generadas sobre una
 * misma escala de luminosidad, de modo que el mismo paso de cualquier rampa
 * pesa visualmente igual.
 *
 * - Accent (índigo): acción / actividad ("está pasando ahora").
 * - Accent2 (verde-agua): terminado / confirmado.
 * - Neutral: texto secundario, fondos, estados inertes.
 *
 * Regla de contraste del sistema: los rellenos sólidos con texto claro usan
 * siempre el paso 700 de la rampa con texto Neutral100; nunca el paso 500.
 * Fondos tintados: paso 100/200 con texto del paso 800.
 */
package com.example.serviaux.ui.theme

import androidx.compose.ui.graphics.Color

// ── Suelo de la pantalla ────────────────────────────────────────────────
val BgLila = Color(0xFFF1EFF7)        // fondo de pantalla (lila muy claro)
val SurfaceLila = Color(0xFFE4E2F0)   // tarjetas y secciones
val TextInk = Color(0xFF1E1C26)       // texto principal

// ── Rampa accent: índigo ────────────────────────────────────────────────
val Indigo100 = Color(0xFFEEEEFF)
val Indigo200 = Color(0xFFDCDCFB)
val Indigo300 = Color(0xFFC3C3F5)
val Indigo400 = Color(0xFFA0A1EC)
val Indigo500 = Color(0xFF7C7EE0)
val Indigo600 = Color(0xFF6062CC)
val Indigo700 = Color(0xFF4A4BAB)
val Indigo800 = Color(0xFF363688)
val Indigo900 = Color(0xFF262560)

// ── Rampa accent2: verde-agua ───────────────────────────────────────────
val Aqua100 = Color(0xFFE3F6F2)
val Aqua200 = Color(0xFFCBEAE4)
val Aqua300 = Color(0xFFA9D8CF)
val Aqua400 = Color(0xFF7FBEB3)
val Aqua500 = Color(0xFF5DA296)
val Aqua600 = Color(0xFF47857A)
val Aqua700 = Color(0xFF35655D)
val Aqua800 = Color(0xFF244841)
val Aqua900 = Color(0xFF17302B)

// ── Rampa neutral ───────────────────────────────────────────────────────
val Neutral100 = Color(0xFFF7F6FB)
val Neutral200 = Color(0xFFECEBF3)
val Neutral300 = Color(0xFFD9D7E4)
val Neutral400 = Color(0xFFBAB7C8)
val Neutral500 = Color(0xFF9B98AB)
val Neutral600 = Color(0xFF7D7A8D)
val Neutral700 = Color(0xFF605D6F)
val Neutral800 = Color(0xFF43414F)
val Neutral900 = Color(0xFF2B2934)

// ── Error (fuera de las rampas; el prototipo no define rojo propio) ─────
val ErrorRed = Color(0xFFBA1A1A)
val OnErrorWhite = Color(0xFFFFFFFF)
val ErrorContainerRed = Color(0xFFFFDAD6)
val OnErrorContainerRed = Color(0xFF410002)

// ── Colores de estado de órdenes (chip: fondo + texto) ──────────────────
// Semántica fija: índigo sólido = pasando ahora; verde-agua = terminado;
// punteado = esperando algo de fuera; neutro = inerte.
val StatusRecibidoBg = Neutral100
val StatusRecibidoText = Neutral800
val StatusDiagnosticoBg = Indigo200
val StatusDiagnosticoText = Indigo800
val StatusEnProcesoBg = Indigo700          // sólido = "pasando ahora"
val StatusEnProcesoText = Neutral100
val StatusEsperaBg = Indigo100             // + borde punteado Indigo600
val StatusEsperaText = Indigo800
val StatusEsperaBorder = Indigo600         // único borde punteado del sistema
val StatusListoBg = Aqua700
val StatusListoText = Neutral100
val StatusEntregadoBg = Aqua200
val StatusEntregadoText = Aqua800
val StatusCerradoBg = Neutral300
val StatusCerradoText = Neutral800

// Color representativo por estado, para puntos y barras laterales donde el
// chip no cabe (listas). Mantienen los nombres históricos.
val StatusRecibido = Neutral600
val StatusDiagnostico = Indigo500
val StatusEnProceso = Indigo700
val StatusEsperaRepuesto = Indigo400
val StatusListo = Aqua700
val StatusEntregado = Aqua500
val StatusCancelado = Neutral400           // CERRADO

// ── Semántica de saldo ──────────────────────────────────────────────────
// El saldo es el ÚNICO número que se pinta de color.
val SaldoSaldado = Aqua800                 // $0,00 — saldado
val SaldoPendiente = Indigo800             // abonado o sin pagos

// ── Colores de prioridad ────────────────────────────────────────────────
val PriorityAlta = ErrorRed
val PriorityMedia = Indigo700
val PriorityBaja = Aqua700
