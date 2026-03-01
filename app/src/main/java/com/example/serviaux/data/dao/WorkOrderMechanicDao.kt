package com.example.serviaux.data.dao

import androidx.room.*
import com.example.serviaux.data.entity.WorkOrderMechanic
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkOrderMechanicDao {
    @Insert
    suspend fun insert(mechanic: WorkOrderMechanic): Long

    @Update
    suspend fun update(mechanic: WorkOrderMechanic)

    @Delete
    suspend fun delete(mechanic: WorkOrderMechanic)

    @Query("SELECT * FROM work_order_mechanics WHERE workOrderId = :workOrderId")
    fun getByWorkOrder(workOrderId: Long): Flow<List<WorkOrderMechanic>>

    @Query("SELECT * FROM work_order_mechanics WHERE workOrderId = :workOrderId")
    suspend fun getByWorkOrderDirect(workOrderId: Long): List<WorkOrderMechanic>

    @Query("SELECT * FROM work_order_mechanics WHERE mechanicId = :mechanicId")
    fun getByMechanic(mechanicId: Long): Flow<List<WorkOrderMechanic>>

    @Query("UPDATE work_order_mechanics SET commissionPaid = 1, paidAt = :paidAt WHERE id = :id")
    suspend fun markAsPaid(id: Long, paidAt: Long = System.currentTimeMillis())

    @Query("UPDATE work_order_mechanics SET commissionPaid = 0, paidAt = NULL WHERE id = :id")
    suspend fun markAsUnpaid(id: Long)

    @Query("SELECT * FROM work_order_mechanics")
    suspend fun getAllDirect(): List<WorkOrderMechanic>

    @Query("DELETE FROM work_order_mechanics")
    suspend fun deleteAll()

    @Query("DELETE FROM work_order_mechanics WHERE workOrderId = :workOrderId")
    suspend fun deleteByWorkOrder(workOrderId: Long)
}
