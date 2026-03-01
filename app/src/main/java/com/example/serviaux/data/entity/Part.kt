/**
 * Part.kt - Entidad de repuesto/pieza del inventario.
 *
 * Representa un artículo en el inventario del taller. El stock se ajusta
 * automáticamente al agregar o quitar [WorkOrderPart] de una orden.
 */
package com.example.serviaux.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Repuesto o pieza del inventario del taller.
 *
 * @property name Nombre descriptivo del repuesto.
 * @property code Código interno o del fabricante, opcional.
 * @property brand Marca del repuesto (puede venir del catálogo de marcas de repuestos).
 * @property unitCost Costo de adquisición por unidad.
 * @property salePrice Precio de venta al cliente; si es null se usa [unitCost].
 * @property currentStock Cantidad disponible en inventario.
 * @property active Si es false, el repuesto no aparece en búsquedas activas.
 */
@Entity(tableName = "parts")
data class Part(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val code: String? = null,
    val brand: String? = null,
    val unitCost: Double,
    val salePrice: Double? = null,
    val currentStock: Int = 0,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
