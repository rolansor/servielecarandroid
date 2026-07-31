package com.example.serviaux.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas del formato de moneda unificado (formato Ecuador: miles con punto,
 * decimales con coma). Es la cara visible de todos los importes de la app.
 */
class MoneyTest {

    @Test
    fun `formatea con separador de miles y dos decimales`() {
        assertEquals("$3.450,00", formatMoney(3450.0))
        assertEquals("$1.234,56", formatMoney(1234.56))
        assertEquals("$86.400,00", formatMoney(86400.0))
    }

    @Test
    fun `montos chicos y cero`() {
        assertEquals("$0,00", formatMoney(0.0))
        assertEquals("$0,50", formatMoney(0.5))
        assertEquals("$999,99", formatMoney(999.99))
    }

    @Test
    fun `negativos con el signo por delante`() {
        assertEquals("-$1.234,56", formatMoney(-1234.56))
    }

    @Test
    fun `redondea a dos decimales`() {
        assertEquals("$10,01", formatMoney(10.006))
        assertEquals("$10,00", formatMoney(10.004))
    }

    @Test
    fun `kilometraje con separador de miles`() {
        assertEquals("98.400 km", formatKm(98400))
        assertEquals("500 km", formatKm(500))
    }
}
