package com.example.serviaux.data.dao

import androidx.room.*
import com.example.serviaux.data.entity.WorkOrderExtra
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkOrderExtraDao {
    @Insert
    suspend fun insert(extra: WorkOrderExtra): Long

    @Update
    suspend fun update(extra: WorkOrderExtra)

    @Delete
    suspend fun delete(extra: WorkOrderExtra)

    @Query("SELECT * FROM work_order_extras WHERE workOrderId = :workOrderId ORDER BY createdAt ASC")
    fun getByWorkOrder(workOrderId: Long): Flow<List<WorkOrderExtra>>

    @Query("SELECT COALESCE(SUM(cost - discount), 0.0) FROM work_order_extras WHERE workOrderId = :workOrderId")
    suspend fun getTotalExtras(workOrderId: Long): Double

    @Query("SELECT * FROM work_order_extras")
    suspend fun getAllDirect(): List<WorkOrderExtra>

    @Query("DELETE FROM work_order_extras WHERE workOrderId = :workOrderId")
    suspend fun deleteByWorkOrder(workOrderId: Long)

    @Query("DELETE FROM work_order_extras")
    suspend fun deleteAll()
}
