package com.example.serviaux.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val CUSTOMER_LIST = "customers"
    const val CUSTOMER_DETAIL = "customers/{customerId}"
    const val CUSTOMER_FORM = "customers/form?customerId={customerId}"
    const val VEHICLE_LIST = "vehicles"
    const val VEHICLE_DETAIL = "vehicles/{vehicleId}"
    const val VEHICLE_FORM = "vehicles/form?customerId={customerId}&vehicleId={vehicleId}"
    const val WORK_ORDER_LIST = "workorders"
    const val WORK_ORDER_DETAIL = "workorders/{orderId}"
    const val WORK_ORDER_FORM = "workorders/form"
    const val PART_LIST = "parts"
    const val PART_FORM = "parts/form?partId={partId}"
    const val USER_LIST = "users"
    const val USER_FORM = "users/form?userId={userId}"
    const val REPORTS = "reports"
    const val CATALOG_SETTINGS = "catalog_settings"

    fun customerDetail(id: Long) = "customers/$id"
    fun customerForm(id: Long? = null) = if (id != null) "customers/form?customerId=$id" else "customers/form"
    fun vehicleDetail(id: Long) = "vehicles/$id"
    fun vehicleForm(customerId: Long? = null, vehicleId: Long? = null): String {
        val params = mutableListOf<String>()
        customerId?.let { params.add("customerId=$it") }
        vehicleId?.let { params.add("vehicleId=$it") }
        return if (params.isEmpty()) "vehicles/form" else "vehicles/form?${params.joinToString("&")}"
    }
    fun workOrderDetail(id: Long) = "workorders/$id"
    fun partForm(id: Long? = null) = if (id != null) "parts/form?partId=$id" else "parts/form"
    fun userForm(id: Long? = null) = if (id != null) "users/form?userId=$id" else "users/form"
}
