package com.example.serviaux.data.dao

import androidx.room.*
import com.example.serviaux.data.entity.ServiceLine
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceLineDao {
    @Insert
    suspend fun insert(serviceLine: ServiceLine): Long

    @Update
    suspend fun update(serviceLine: ServiceLine)

    @Delete
    suspend fun delete(serviceLine: ServiceLine)

    @Query("SELECT * FROM service_lines WHERE workOrderId = :workOrderId")
    fun getByWorkOrder(workOrderId: Long): Flow<List<ServiceLine>>

    @Query("SELECT COALESCE(SUM(laborCost), 0.0) FROM service_lines WHERE workOrderId = :workOrderId")
    suspend fun getTotalLabor(workOrderId: Long): Double
}
