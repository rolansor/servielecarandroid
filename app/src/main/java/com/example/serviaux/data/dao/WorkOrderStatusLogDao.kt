package com.example.serviaux.data.dao

import androidx.room.*
import com.example.serviaux.data.entity.WorkOrderStatusLog
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkOrderStatusLogDao {
    @Insert
    suspend fun insert(log: WorkOrderStatusLog): Long

    @Query("SELECT * FROM work_order_status_log WHERE workOrderId = :workOrderId ORDER BY changedAt DESC")
    fun getByWorkOrder(workOrderId: Long): Flow<List<WorkOrderStatusLog>>
}
