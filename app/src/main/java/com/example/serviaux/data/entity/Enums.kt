package com.example.serviaux.data.entity

enum class UserRole(val displayName: String) {
    ADMIN("Administrador"),
    RECEPCIONISTA("Recepcionista"),
    MECANICO("Mecánico")
}

enum class OrderStatus(val displayName: String) {
    RECIBIDO("Recibido"),
    EN_DIAGNOSTICO("En Diagnóstico"),
    EN_PROCESO("En Proceso"),
    EN_ESPERA_REPUESTO("En Espera de Repuesto"),
    LISTO("Listo"),
    ENTREGADO("Entregado"),
    CANCELADO("Cancelado")
}

enum class Priority(val displayName: String) {
    BAJA("Baja"),
    MEDIA("Media"),
    ALTA("Alta")
}

enum class PaymentMethod(val displayName: String) {
    EFECTIVO("Efectivo"),
    TRANSFERENCIA("Transferencia"),
    TARJETA("Tarjeta"),
    OTRO("Otro")
}
