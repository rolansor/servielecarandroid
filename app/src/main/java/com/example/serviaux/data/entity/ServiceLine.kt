/**
 * ServiceLine.kt - Entidad de línea de servicio (mano de obra).
 *
 * Cada línea representa un trabajo o servicio realizado en una [WorkOrder].
 * Se elimina en cascada al borrar la orden padre.
 * La suma de [laborCost] de todas las líneas determina el [WorkOrder.totalLabor].
 */
package com.example.serviaux.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Línea de servicio/mano de obra asociada a una orden de trabajo.
 *
 * @property workOrderId FK a la orden de trabajo (CASCADE al eliminar).
 * @property description Descripción del servicio realizado.
 * @property laborCost Costo de la mano de obra para este servicio.
 * @property notes Observaciones adicionales sobre el servicio.
 */
@Entity(
    tableName = "service_lines",
    indices = [Index(value = ["workOrderId"])],
    foreignKeys = [
        ForeignKey(
            entity = WorkOrder::class,
            parentColumns = ["id"],
            childColumns = ["workOrderId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ServiceLine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workOrderId: Long,
    val description: String,
    val laborCost: Double,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
