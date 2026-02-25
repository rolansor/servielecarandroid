package com.example.serviaux.data.dao

import androidx.room.*
import com.example.serviaux.data.entity.WorkOrderPayment
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkOrderPaymentDao {
    @Insert
    suspend fun insert(payment: WorkOrderPayment): Long

    @Delete
    suspend fun delete(payment: WorkOrderPayment)

    @Query("SELECT * FROM work_order_payments WHERE workOrderId = :workOrderId")
    fun getByWorkOrder(workOrderId: Long): Flow<List<WorkOrderPayment>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM work_order_payments WHERE workOrderId = :workOrderId")
    fun getTotalPayments(workOrderId: Long): Flow<Double>
}
