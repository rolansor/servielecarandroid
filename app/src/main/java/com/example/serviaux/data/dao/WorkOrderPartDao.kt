package com.example.serviaux.data.dao

import androidx.room.*
import com.example.serviaux.data.entity.WorkOrderPart
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT partId, SUM(quantity) as totalQty FROM work_order_parts wop INNER JOIN work_orders wo ON wop.workOrderId = wo.id WHERE wo.createdAt BETWEEN :from AND :to GROUP BY partId ORDER BY totalQty DESC LIMIT :limit")
    suspend fun getTopParts(from: Long, to: Long, limit: Int = 10): List<TopPartResult>

    @Query("SELECT * FROM work_order_parts WHERE workOrderId = :workOrderId")
    suspend fun getByWorkOrderDirect(workOrderId: Long): List<WorkOrderPart>

    @Query("SELECT * FROM work_order_parts")
    suspend fun getAllDirect(): List<WorkOrderPart>

    @Query("""
        SELECT wop.appliedUnitPrice FROM work_order_parts wop
        INNER JOIN work_orders wo ON wop.workOrderId = wo.id
        WHERE wop.partId = :partId AND wo.customerId = :customerId
        ORDER BY wo.createdAt DESC LIMIT 1
    """)
    suspend fun getLastPriceForCustomer(partId: Long, customerId: Long): Double?
}

data class TopPartResult(
    val partId: Long,
    val totalQty: Long
)
