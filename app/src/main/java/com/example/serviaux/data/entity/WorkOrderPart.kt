/**
 * WorkOrderPart.kt - Entidad de repuesto utilizado en una orden.
 *
 * Registra qué repuestos y en qué cantidad se usaron en una [WorkOrder].
 * Al crear/eliminar registros, el stock del [Part] correspondiente se ajusta
 * automáticamente desde el repositorio.
 * Se elimina en cascada al borrar la orden padre.
 */
package com.example.serviaux.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Repuesto asignado a una orden de trabajo.
 *
 * @property workOrderId FK a la orden de trabajo (CASCADE al eliminar).
 * @property partId FK al repuesto del inventario.
 * @property quantity Cantidad de unidades utilizadas.
 * @property appliedUnitPrice Precio unitario aplicado al momento del registro.
 * @property subtotal Resultado de [quantity] * [appliedUnitPrice].
 */
@Entity(
    tableName = "work_order_parts",
    indices = [
        Index(value = ["workOrderId"]),
        Index(value = ["partId"])
    ],
    foreignKeys = [
        ForeignKey(entity = WorkOrder::class, parentColumns = ["id"], childColumns = ["workOrderId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Part::class, parentColumns = ["id"], childColumns = ["partId"])
    ]
)
data class WorkOrderPart(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workOrderId: Long,
    val partId: Long,
    val quantity: Int,
    val appliedUnitPrice: Double,
    val subtotal: Double,
    val discount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
