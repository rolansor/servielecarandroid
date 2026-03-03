/**
 * ServiceLineDao.kt - DAO de líneas de servicio (mano de obra).
 *
 * Gestiona las líneas de servicio de cada orden de trabajo.
 * Incluye [getTotalLabor] para recalcular el total de mano de obra.
 */
package com.example.serviaux.data.dao

import androidx.room.*
import com.example.serviaux.data.entity.ServiceLine
import kotlinx.coroutines.flow.Flow

/** DAO para la entidad [ServiceLine]. */
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

    /** Suma el costo de mano de obra de todas las líneas de una orden; usado para recalcular [WorkOrder.totalLabor]. */
    @Query("SELECT COALESCE(SUM(laborCost - discount), 0.0) FROM service_lines WHERE workOrderId = :workOrderId")
    suspend fun getTotalLabor(workOrderId: Long): Double

    @Query("SELECT * FROM service_lines")
    suspend fun getAllDirect(): List<ServiceLine>

    @Query("DELETE FROM service_lines")
    suspend fun deleteAll()
}
