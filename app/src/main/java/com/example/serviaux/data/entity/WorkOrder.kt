/**
 * WorkOrder.kt - Entidad principal de orden de trabajo.
 *
 * Representa una orden de servicio en el taller. Es la entidad central del dominio
 * y se relaciona con [Vehicle], [Customer], [ServiceLine], [WorkOrderPart],
 * [WorkOrderPayment] y [WorkOrderStatusLog].
 *
 * Los totales ([totalLabor], [totalParts], [total]) se recalculan automáticamente
 * desde el repositorio cada vez que se modifican líneas de servicio o repuestos.
 */
package com.example.serviaux.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Orden de trabajo del taller.
 *
 * @property vehicleId FK al vehículo atendido.
 * @property customerId FK al cliente propietario.
 * @property entryDate Fecha/hora de ingreso del vehículo (millis epoch).
 * @property status Estado actual en el flujo de trabajo.
 * @property priority Nivel de urgencia de la orden.
 * @property customerComplaint Descripción de la queja o solicitud del cliente.
 * @property initialDiagnosis Diagnóstico preliminar del mecánico.
 * @property assignedMechanicId FK al usuario mecánico asignado, opcional.
 * @property entryMileage Kilometraje de entrada registrado en recepción.
 * @property fuelLevel Nivel de combustible al ingreso.
 * @property checklistNotes Notas del checklist de recepción (JSON o texto libre).
 * @property totalLabor Suma de costos de mano de obra (calculado).
 * @property totalParts Suma de costos de repuestos (calculado).
 * @property total Monto total de la orden (labor + repuestos).
 * @property photoPaths Rutas de fotos separadas por comas (máximo 6).
 * @property filePaths Rutas de archivos adjuntos separadas por comas.
 * @property deliveryNote Nota de entrega al cliente.
 * @property invoiceNumber Número de factura asociado.
 * @property notes Notas internas adicionales.
 * @property createdBy FK al usuario que creó la orden.
 * @property updatedBy FK al usuario que realizó la última modificación.
 */
@Entity(
    tableName = "work_orders",
    indices = [
        Index(value = ["vehicleId"]),
        Index(value = ["customerId"]),
        Index(value = ["assignedMechanicId"]),
        Index(value = ["status"])
    ],
    foreignKeys = [
        ForeignKey(entity = Vehicle::class, parentColumns = ["id"], childColumns = ["vehicleId"]),
        ForeignKey(entity = Customer::class, parentColumns = ["id"], childColumns = ["customerId"])
    ]
)
data class WorkOrder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val customerId: Long,
    val entryDate: Long = System.currentTimeMillis(),
    val status: OrderStatus = OrderStatus.RECIBIDO,
    val priority: Priority = Priority.MEDIA,
    val orderType: OrderType = OrderType.SERVICIO_NUEVO,
    val customerComplaint: String,
    val initialDiagnosis: String? = null,
    val arrivalCondition: ArrivalCondition = ArrivalCondition.RODANDO,
    val assignedMechanicId: Long? = null,
    val entryMileage: Int? = null,
    val fuelLevel: String? = null,
    val checklistNotes: String? = null,
    val totalLabor: Double = 0.0,
    val totalParts: Double = 0.0,
    val total: Double = 0.0,
    val photoPaths: String? = null,
    val filePaths: String? = null,
    val deliveryNote: String? = null,
    val invoiceNumber: String? = null,
    val notes: String? = null,
    val createdBy: Long,
    val updatedBy: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
