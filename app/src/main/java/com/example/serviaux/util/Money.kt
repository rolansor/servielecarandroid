/**
 * Money.kt - Formato de moneda unificado de Serviaux.
 *
 * Decisión de producto del rediseño: moneda siempre como `$1.234,56`
 * (separador de miles con punto, decimales con coma — formato Ecuador),
 * en todas las pantallas. El saldo es el único número que se pinta de
 * color (ver `SaldoSaldado` / `SaldoPendiente` en el tema).
 */
package com.example.serviaux.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val moneySymbols = DecimalFormatSymbols(Locale("es", "EC")).apply {
    groupingSeparator = '.'
    decimalSeparator = ','
}

// DecimalFormat no es thread-safe y algunos mensajes se arman en coroutines:
// se sincroniza el acceso.
private val moneyFormat = DecimalFormat("$#,##0.00", moneySymbols)

/** `3450.0` → `$3.450,00`. Los negativos salen como `-$1.234,56`. */
fun formatMoney(amount: Double): String = synchronized(moneyFormat) {
    if (amount < 0) "-" + moneyFormat.format(-amount) else moneyFormat.format(amount)
}

private val kmFormat = DecimalFormat("#,##0", moneySymbols)

/** `98400` → `98.400 km` (mismo separador de miles que la moneda). */
fun formatKm(km: Int): String = synchronized(kmFormat) { kmFormat.format(km) + " km" }
