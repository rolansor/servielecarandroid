package com.example.serviaux.ui.theme

import androidx.compose.ui.graphics.Color

// === Mechanic / Workshop Theme Colors ===

// Primary: Deep steel blue (like tool steel)
val SteelBlue80 = Color(0xFF8EAEBF)       // Light variant for dark theme
val SteelBlue40 = Color(0xFF1B3A4B)       // Dark variant for light theme
val SteelBlue30 = Color(0xFF142D3A)       // Even darker for containers in light
val SteelBlue90 = Color(0xFFD0E1EA)       // Very light for containers in dark

// Secondary: Warm orange/amber (sparks, warning signs)
val Amber80 = Color(0xFFF5C77E)            // Light variant for dark theme
val Amber40 = Color(0xFFE67E22)            // Dark variant for light theme
val Amber30 = Color(0xFFBF6516)            // Darker for containers in light
val Amber90 = Color(0xFFFDE8C8)            // Very light for containers in dark

// Tertiary: Cool gray (like brushed metal)
val MetalGray80 = Color(0xFFB8C4C5)        // Light variant for dark theme
val MetalGray40 = Color(0xFF7F8C8D)        // Dark variant for light theme
val MetalGray30 = Color(0xFF5D6A6B)        // Darker for containers in light
val MetalGray90 = Color(0xFFDDE3E3)        // Very light for containers in dark

// Error: Red-orange like brake lights
val BrakeRed80 = Color(0xFFF5A8A0)         // Light variant for dark theme
val BrakeRed40 = Color(0xFFE74C3C)         // Dark variant for light theme

// Backgrounds & Surfaces
val DarkCharcoal = Color(0xFF1A1A2E)       // Dark theme background
val DarkNavy = Color(0xFF16213E)           // Dark theme surface
val DarkSurfaceVariant = Color(0xFF1E2A3A) // Dark theme surface variant
val LightGray = Color(0xFFF5F5F5)          // Light theme background
val White = Color(0xFFFFFFFF)              // Light theme surface
val LightSurfaceVariant = Color(0xFFE8ECEF) // Light theme surface variant

// On-colors for dark theme
val OnDarkPrimary = Color(0xFF0A1F2B)
val OnDarkSecondary = Color(0xFF3D2200)
val OnDarkBackground = Color(0xFFE8E8E8)
val OnDarkSurface = Color(0xFFE0E0E0)
val OnDarkSurfaceVariant = Color(0xFFB0BEC5)

// On-colors for light theme
val OnLightPrimary = Color(0xFFFFFFFF)
val OnLightSecondary = Color(0xFFFFFFFF)
val OnLightBackground = Color(0xFF1A1A1A)
val OnLightSurface = Color(0xFF1A1A1A)
val OnLightSurfaceVariant = Color(0xFF4A5568)

// Outline
val OutlineDark = Color(0xFF3A4A5A)
val OutlineLight = Color(0xFFB0BEC5)

// === Status Colors (adjusted for workshop theme) ===
val StatusRecibido = Color(0xFF2196F3)       // Blue - received
val StatusDiagnostico = Color(0xFFE67E22)    // Orange - diagnosis
val StatusEnProceso = Color(0xFFF1C40F)      // Yellow - in progress
val StatusEsperaRepuesto = Color(0xFFE91E63) // Pink - waiting for parts
val StatusListo = Color(0xFF27AE60)          // Green - ready
val StatusEntregado = Color(0xFF2ECC71)      // Light green - delivered
val StatusCancelado = Color(0xFF95A5A6)      // Gray - cancelled

// === Priority Colors ===
val PriorityAlta = Color(0xFFE74C3C)    // Red - high
val PriorityMedia = Color(0xFFE67E22)   // Orange - medium
val PriorityBaja = Color(0xFF27AE60)    // Green - low
