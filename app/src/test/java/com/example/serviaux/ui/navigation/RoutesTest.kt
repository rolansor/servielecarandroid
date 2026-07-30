package com.example.serviaux.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de los constructores de rutas.
 *
 * Son Kotlin puro, sin Android: si una ruta construida deja de coincidir con la plantilla que
 * registra el grafo, la navegación falla en silencio (la pantalla se abre sin sus argumentos).
 */
class RoutesTest {

    @Test
    fun `las rutas con id se construyen sobre la plantilla registrada`() {
        assertEquals("customers/7", Routes.customerDetail(7))
        assertEquals("vehicles/7", Routes.vehicleDetail(7))
        assertEquals("workorders/7", Routes.workOrderDetail(7))
        assertEquals("workorders/edit/7", Routes.workOrderEdit(7))
    }

    @Test
    fun `los formularios sin id no llevan parametros`() {
        assertEquals("customers/form", Routes.customerForm())
        assertEquals("vehicles/form", Routes.vehicleForm())
        assertEquals("parts/form", Routes.partForm())
        assertEquals("users/form", Routes.userForm())
        assertEquals("appointments/form", Routes.appointmentForm())
        assertEquals("service_history", Routes.serviceHistory())
        assertEquals("workorders", Routes.workOrderList())
    }

    @Test
    fun `los formularios con id llevan el parametro de consulta`() {
        assertEquals("customers/form?customerId=3", Routes.customerForm(3))
        assertEquals("parts/form?partId=3", Routes.partForm(3))
        assertEquals("users/form?userId=3", Routes.userForm(3))
        assertEquals("appointments/form?appointmentId=3", Routes.appointmentForm(3))
        assertEquals("service_history?customerId=3", Routes.serviceHistory(3))
        assertEquals("workorders?status=LISTO", Routes.workOrderList("LISTO"))
    }

    @Test
    fun `el formulario de vehiculo combina sus parametros opcionales`() {
        assertEquals("vehicles/form?customerId=1", Routes.vehicleForm(customerId = 1))
        assertEquals("vehicles/form?vehicleId=2", Routes.vehicleForm(vehicleId = 2))
        assertEquals(
            "vehicles/form?customerId=1&vehicleId=2",
            Routes.vehicleForm(customerId = 1, vehicleId = 2)
        )
    }

    @Test
    fun `la conversion de turno a orden pasa los tres identificadores`() {
        assertEquals(
            "workorders/form?customerId=1&vehicleId=2&appointmentId=3",
            Routes.workOrderFormFromAppointment(1, 2, 3)
        )
    }

    @Test
    fun `las rutas de administracion cubren las pantallas restringidas`() {
        // Si se agrega una pantalla solo-admin y se olvida aquí, el guard del grafo la deja pasar.
        assertTrue(Routes.USER_LIST in Routes.ADMIN_ROUTES)
        assertTrue(Routes.USER_FORM in Routes.ADMIN_ROUTES)
        assertTrue(Routes.REPORTS in Routes.ADMIN_ROUTES)
        assertTrue(Routes.COMMISSIONS in Routes.ADMIN_ROUTES)
        assertTrue(Routes.CATALOG_SETTINGS in Routes.ADMIN_ROUTES)
        assertTrue(Routes.BACKUP in Routes.ADMIN_ROUTES)
        assertEquals(6, Routes.ADMIN_ROUTES.size)
    }

    @Test
    fun `el dashboard y el login no son rutas restringidas`() {
        assertTrue(Routes.DASHBOARD !in Routes.ADMIN_ROUTES)
        assertTrue(Routes.LOGIN !in Routes.ADMIN_ROUTES)
    }
}
