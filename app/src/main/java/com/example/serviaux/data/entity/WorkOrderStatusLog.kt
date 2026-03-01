/**
 * WorkOrderStatusLog.kt - Entidad de registro de cambios de estado.
 *
 * Mantiene un historial de auditoría de cada transición de estado
 * de una [WorkOrder], incluyendo quién realizó el cambio y cuándo.
 * Se elimina en cascada al borrar la orden padre.
 */
package com.example.serviaux.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Registro de cambio de estado de una orden de trabajo.
 *
 * @property workOrderId FK a la orden de trabajo (CASCADE al eliminar).
 * @property oldStatus Estado anterior (null si es la creación inicial).
 * @property newStatus Nuevo estado asignado.
 * @property changedByUserId FK al usuario que realizó el cambio.
 * @property changedAt Fecha/hora del cambio (millis epoch).
 * @property note Comentario opcional sobre el motivo del cambio.
 */
@Entity(
    tableName = "work_order_status_log",
    indices = [Index(value = ["workOrderId"])],
    foreignKeys = [
        ForeignKey(entity = WorkOrder::class, parentColumns = ["id"], childColumns = ["workOrderId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class WorkOrderStatusLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workOrderId: Long,
    val oldStatus: OrderStatus?,
    val newStatus: OrderStatus,
    val changedByUserId: Long,
    val changedAt: Long = System.currentTimeMillis(),
    val note: String? = null
)
