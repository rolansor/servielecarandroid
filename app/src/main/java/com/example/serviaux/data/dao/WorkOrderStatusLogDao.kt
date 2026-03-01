/**
 * WorkOrderStatusLogDao.kt - DAO del historial de cambios de estado.
 *
 * Registra cada transición de estado de una orden para auditoría.
 * Los registros se ordenan por fecha descendente para mostrar el más reciente primero.
 */
package com.example.serviaux.data.dao

import androidx.room.*
import com.example.serviaux.data.entity.WorkOrderStatusLog
import kotlinx.coroutines.flow.Flow

/** DAO para la entidad [WorkOrderStatusLog]. */
@Dao
interface WorkOrderStatusLogDao {
    @Insert
    suspend fun insert(log: WorkOrderStatusLog): Long

    @Query("SELECT * FROM work_order_status_log WHERE workOrderId = :workOrderId ORDER BY changedAt DESC")
    fun getByWorkOrder(workOrderId: Long): Flow<List<WorkOrderStatusLog>>

    @Query("SELECT * FROM work_order_status_log")
    suspend fun getAllDirect(): List<WorkOrderStatusLog>

    @Query("DELETE FROM work_order_status_log")
    suspend fun deleteAll()
}
