/**
 * Routes.kt - Definición centralizada de rutas de navegación.
 *
 * Contiene las constantes de ruta para Navigation Compose y funciones
 * auxiliares para construir rutas con parámetros (IDs, filtros de estado).
 * Todas las pantallas del sistema se registran aquí.
 */
package com.example.serviaux.ui.navigation

/**
 * Constantes y constructores de rutas del grafo de navegación.
 *
 * Las rutas con parámetros usan la sintaxis de Navigation Compose:
 * - Parámetros de ruta: `{param}` (obligatorios).
 * - Parámetros de query: `?param={param}` (opcionales).
 */
object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val CUSTOMER_LIST = "customers"
    const val CUSTOMER_DETAIL = "customers/{customerId}"
    const val CUSTOMER_FORM = "customers/form?customerId={customerId}"
    const val VEHICLE_LIST = "vehicles"
    const val VEHICLE_DETAIL = "vehicles/{vehicleId}"
    const val VEHICLE_FORM = "vehicles/form?customerId={customerId}&vehicleId={vehicleId}"
    const val WORK_ORDER_LIST = "workorders?status={status}"
    const val WORK_ORDER_LIST_BASE = "workorders"
    const val WORK_ORDER_DETAIL = "workorders/{orderId}"
    const val WORK_ORDER_FORM = "workorders/form"
    const val WORK_ORDER_EDIT = "workorders/edit/{orderId}"
    const val PART_LIST = "parts"
    const val PART_FORM = "parts/form?partId={partId}"
    const val USER_LIST = "users"
    const val USER_FORM = "users/form?userId={userId}"
    const val REPORTS = "reports"
    const val APPOINTMENT_LIST = "appointments"
    const val APPOINTMENT_FORM = "appointments/form?appointmentId={appointmentId}"
    const val WORK_ORDER_FORM_WITH_PARAMS = "workorders/form?customerId={customerId}&vehicleId={vehicleId}&appointmentId={appointmentId}"
    const val CATALOG_SETTINGS = "catalog_settings"
    const val BACKUP = "backup"
    const val COMMISSIONS = "commissions"
    const val SERVICE_HISTORY = "service_history?customerId={customerId}"
    const val SERVICE_HISTORY_BASE = "service_history"

    fun serviceHistory(customerId: Long? = null) =
        if (customerId != null) "service_history?customerId=$customerId" else "service_history"
    fun appointmentForm(id: Long? = null) = if (id != null) "appointments/form?appointmentId=$id" else "appointments/form"
    fun workOrderFormFromAppointment(customerId: Long, vehicleId: Long, appointmentId: Long) =
        "workorders/form?customerId=$customerId&vehicleId=$vehicleId&appointmentId=$appointmentId"
    fun customerDetail(id: Long) = "customers/$id"
    fun customerForm(id: Long? = null) = if (id != null) "customers/form?customerId=$id" else "customers/form"
    fun vehicleDetail(id: Long) = "vehicles/$id"
    fun vehicleForm(customerId: Long? = null, vehicleId: Long? = null): String {
        val params = mutableListOf<String>()
        customerId?.let { params.add("customerId=$it") }
        vehicleId?.let { params.add("vehicleId=$it") }
        return if (params.isEmpty()) "vehicles/form" else "vehicles/form?${params.joinToString("&")}"
    }
    fun workOrderList(status: String? = null) = if (status != null) "workorders?status=$status" else "workorders"
    fun workOrderDetail(id: Long) = "workorders/$id"
    fun workOrderEdit(id: Long) = "workorders/edit/$id"
    fun partForm(id: Long? = null) = if (id != null) "parts/form?partId=$id" else "parts/form"
    fun userForm(id: Long? = null) = if (id != null) "users/form?userId=$id" else "users/form"
}
