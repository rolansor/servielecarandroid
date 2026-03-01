/**
 * Enums.kt - Enumeraciones del dominio de Serviaux.
 *
 * Define los tipos enumerados utilizados en toda la aplicación:
 * roles de usuario, estados de orden, prioridades y métodos de pago.
 * Cada valor incluye un [displayName] en español para mostrar en la UI.
 */
package com.example.serviaux.data.entity

/**
 * Rol de un usuario en el sistema.
 *
 * Determina los permisos y las vistas accesibles:
 * - [ADMIN]: acceso total (usuarios, reportes, catálogos, respaldos).
 * - [RECEPCIONISTA]: gestión de clientes, vehículos y órdenes.
 * - [MECANICO]: consulta de órdenes asignadas.
 */
enum class UserRole(val displayName: String) {
    ADMIN("Administrador"),
    RECEPCIONISTA("Recepcionista"),
    MECANICO("Mecánico")
}

/**
 * Estado del ciclo de vida de una orden de trabajo.
 *
 * Flujo principal:
 * RECIBIDO -> EN_DIAGNOSTICO -> EN_PROCESO -> LISTO -> ENTREGADO.
 * [EN_ESPERA_REPUESTO] es un estado intermedio cuando faltan piezas.
 * [CANCELADO] puede ocurrir desde cualquier estado previo a la entrega.
 */
enum class OrderStatus(val displayName: String) {
    RECIBIDO("Recibido"),
    EN_DIAGNOSTICO("En Diagnóstico"),
    EN_PROCESO("En Proceso"),
    EN_ESPERA_REPUESTO("En Espera de Repuesto"),
    LISTO("Listo"),
    ENTREGADO("Entregado"),
    CANCELADO("Cancelado")
}

/** Nivel de prioridad asignado a una orden de trabajo. */
enum class Priority(val displayName: String) {
    BAJA("Baja"),
    MEDIA("Media"),
    ALTA("Alta")
}

/** Tipo de comisión que puede recibir un mecánico. */
enum class CommissionType(val displayName: String) {
    NINGUNA("No comisiona"),
    FIJA("Por trabajo ($)"),
    PORCENTAJE("Porcentaje (%)")
}

/** Tipo de orden de trabajo. */
enum class OrderType(val displayName: String) {
    SERVICIO_NUEVO("Servicio Nuevo"),
    CORRECTIVO("Correctivo"),
    PREVENTIVO("Preventivo"),
    GARANTIA("Garantía"),
    DIAGNOSTICO("Diagnóstico"),
    REVISION("Revisión")
}

/** Condición de llegada del vehículo al taller. */
enum class ArrivalCondition(val displayName: String) {
    RODANDO("Llegó rodando"),
    GRUA("Llegó en grúa"),
    ENCENDIDO_NO_RUEDA("Encendido / No rueda"),
    NO_ENCIENDE("No enciende"),
    EMPUJADO("Llegó empujado")
}

/** Método de pago registrado para un abono o pago total de una orden. */
enum class PaymentMethod(val displayName: String) {
    EFECTIVO("Efectivo"),
    TRANSFERENCIA("Transferencia"),
    TARJETA_CREDITO("Tarjeta de Crédito"),
    TARJETA_DEBITO("Tarjeta de Débito"),
    MIXTA("Mixta"),
    OTRO("Otro")
}
