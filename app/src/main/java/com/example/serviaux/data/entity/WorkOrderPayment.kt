/**
 * WorkOrderPayment.kt - Entidad de pago/abono de una orden.
 *
 * Registra cada pago o abono realizado por el cliente hacia una [WorkOrder].
 * Soporta descuentos por pago y múltiples métodos de pago.
 * Se elimina en cascada al borrar la orden padre.
 */
package com.example.serviaux.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Pago o abono registrado contra una orden de trabajo.
 *
 * @property workOrderId FK a la orden de trabajo (CASCADE al eliminar).
 * @property amount Monto bruto del pago.
 * @property discount Descuento aplicado sobre el monto (por defecto 0.0).
 * @property method Método de pago utilizado.
 * @property date Fecha/hora del pago (millis epoch).
 * @property notes Referencia de transferencia u observaciones del pago.
 */
@Entity(
    tableName = "work_order_payments",
    indices = [Index(value = ["workOrderId"])],
    foreignKeys = [
        ForeignKey(entity = WorkOrder::class, parentColumns = ["id"], childColumns = ["workOrderId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class WorkOrderPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workOrderId: Long,
    val amount: Double,
    val discount: Double = 0.0,
    val method: PaymentMethod,
    val date: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
