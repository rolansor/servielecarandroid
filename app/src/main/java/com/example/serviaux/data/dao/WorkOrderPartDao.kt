/**
 * WorkOrderPartDao.kt - DAO de repuestos asignados a órdenes.
 *
 * Además del CRUD, incluye consultas de agregación ([getTotalParts]),
 * reportes de repuestos más usados ([getTopParts]) y consulta del último
 * precio aplicado a un cliente ([getLastPriceForCustomer]).
 */
package com.example.serviaux.data.dao

import androidx.room.*
import com.example.serviaux.data.entity.WorkOrderPart
import kotlinx.coroutines.flow.Flow

/** DAO para la entidad [WorkOrderPart]. */
@Dao
interface WorkOrderPartDao {
    @Insert
    suspend fun insert(workOrderPart: WorkOrderPart): Long

    @Update
    suspend fun update(workOrderPart: WorkOrderPart)

    @Delete
    suspend fun delete(workOrderPart: WorkOrderPart)

    @Query("SELECT * FROM work_order_parts WHERE workOrderId = :workOrderId")
    fun getByWorkOrder(workOrderId: Long): Flow<List<WorkOrderPart>>

    @Query("SELECT COALESCE(SUM(subtotal), 0.0) FROM work_order_parts WHERE workOrderId = :workOrderId")
    suspend fun getTotalParts(workOrderId: Long): Double

    /** Repuestos más utilizados en un período, agrupados por partId; usado en reportes. */
    @Query("SELECT partId, SUM(quantity) as totalQty FROM work_order_parts wop INNER JOIN work_orders wo ON wop.workOrderId = wo.id WHERE wo.createdAt BETWEEN :from AND :to GROUP BY partId ORDER BY totalQty DESC LIMIT :limit")
    suspend fun getTopParts(from: Long, to: Long, limit: Int = 10): List<TopPartResult>

    @Query("SELECT * FROM work_order_parts WHERE workOrderId = :workOrderId")
    suspend fun getByWorkOrderDirect(workOrderId: Long): List<WorkOrderPart>

    @Query("SELECT * FROM work_order_parts")
    suspend fun getAllDirect(): List<WorkOrderPart>

    @Query("DELETE FROM work_order_parts")
    suspend fun deleteAll()

    /**
     * Obtiene el último precio aplicado de un repuesto para un cliente específico.
     * Útil para sugerir precios consistentes en órdenes recurrentes.
     */
    @Query("""
        SELECT wop.appliedUnitPrice FROM work_order_parts wop
        INNER JOIN work_orders wo ON wop.workOrderId = wo.id
        WHERE wop.partId = :partId AND wo.customerId = :customerId
        ORDER BY wo.createdAt DESC LIMIT 1
    """)
    suspend fun getLastPriceForCustomer(partId: Long, customerId: Long): Double?
}

/** Resultado de la consulta de repuestos más usados con cantidad total acumulada. */
data class TopPartResult(
    val partId: Long,
    val totalQty: Long
)
