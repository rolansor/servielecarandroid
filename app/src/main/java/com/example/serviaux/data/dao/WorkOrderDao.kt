/**
 * WorkOrderDao.kt - DAO de acceso a datos de órdenes de trabajo.
 *
 * Es el DAO más complejo del sistema. Incluye consultas de filtrado por estado,
 * rango de fechas, mecánico asignado, así como operaciones de eliminación en
 * cascada manual de tablas relacionadas (service_lines, work_order_parts, etc.).
 */
package com.example.serviaux.data.dao

import androidx.room.*
import com.example.serviaux.data.entity.OrderStatus
import com.example.serviaux.data.entity.WorkOrder
import kotlinx.coroutines.flow.Flow

data class WorkOrderServiceDesc(val workOrderId: Long, val description: String)
data class WorkOrderPartName(val workOrderId: Long, val partName: String)

/**
 * DAO para la entidad [WorkOrder] y operaciones de limpieza de tablas hijas.
 */
@Dao
interface WorkOrderDao {
    @Insert
    suspend fun insert(workOrder: WorkOrder): Long

    @Update
    suspend fun update(workOrder: WorkOrder)

    @Query("SELECT * FROM work_orders ORDER BY createdAt DESC")
    fun getAll(): Flow<List<WorkOrder>>

    @Query("SELECT * FROM work_orders WHERE id = :id")
    fun getById(id: Long): Flow<WorkOrder?>

    @Query("SELECT * FROM work_orders WHERE id = :id")
    suspend fun getByIdDirect(id: Long): WorkOrder?

    @Query("SELECT * FROM work_orders WHERE vehicleId = :vehicleId ORDER BY createdAt DESC")
    fun getByVehicle(vehicleId: Long): Flow<List<WorkOrder>>

    @Query("SELECT * FROM work_orders WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getByCustomer(customerId: Long): Flow<List<WorkOrder>>

    @Query("SELECT * FROM work_orders WHERE status = :status ORDER BY createdAt DESC")
    fun getByStatus(status: OrderStatus): Flow<List<WorkOrder>>

    @Query("SELECT * FROM work_orders WHERE assignedMechanicId = :mechanicId ORDER BY createdAt DESC")
    fun getByMechanic(mechanicId: Long): Flow<List<WorkOrder>>

    /** Conteo reactivo por estado; alimenta los contadores del dashboard. */
    @Query("SELECT COUNT(*) FROM work_orders WHERE status = :status")
    fun countByStatus(status: OrderStatus): Flow<Int>

    @Query("SELECT * FROM work_orders WHERE createdAt BETWEEN :from AND :to ORDER BY createdAt DESC")
    fun getByDateRange(from: Long, to: Long): Flow<List<WorkOrder>>

    @Query("SELECT * FROM work_orders WHERE status = :status AND createdAt BETWEEN :from AND :to ORDER BY createdAt DESC")
    fun getByStatusAndDateRange(status: OrderStatus, from: Long, to: Long): Flow<List<WorkOrder>>

    /** Suma total facturada en un rango de fechas, excluyendo órdenes canceladas; usado en reportes. */
    @Query("SELECT COALESCE(SUM(total), 0.0) FROM work_orders WHERE createdAt BETWEEN :from AND :to AND status != 'CANCELADO'")
    fun getTotalByDateRange(from: Long, to: Long): Flow<Double>

    @Query("SELECT * FROM work_orders")
    suspend fun getAllDirect(): List<WorkOrder>

    @Query("SELECT * FROM work_orders WHERE entryDate BETWEEN :fromMs AND :toMs ORDER BY entryDate DESC")
    suspend fun getByDateRangeDirect(fromMs: Long, toMs: Long): List<WorkOrder>

    @Query("DELETE FROM work_orders WHERE id = :id")
    suspend fun deleteById(id: Long)

    // ── Eliminación manual de tablas hijas (complementa CASCADE) ───────

    @Query("DELETE FROM service_lines WHERE workOrderId = :workOrderId")
    suspend fun deleteServiceLinesByOrder(workOrderId: Long)

    @Query("DELETE FROM work_order_parts WHERE workOrderId = :workOrderId")
    suspend fun deletePartsByOrder(workOrderId: Long)

    @Query("DELETE FROM work_order_payments WHERE workOrderId = :workOrderId")
    suspend fun deletePaymentsByOrder(workOrderId: Long)

    @Query("DELETE FROM work_order_status_log WHERE workOrderId = :workOrderId")
    suspend fun deleteStatusLogByOrder(workOrderId: Long)

    @Query("DELETE FROM work_order_extras WHERE workOrderId = :workOrderId")
    suspend fun deleteExtrasByOrder(workOrderId: Long)

    @Query("DELETE FROM work_orders")
    suspend fun deleteAll()

    @Query("SELECT sl.workOrderId, sl.description FROM service_lines sl")
    suspend fun getAllServiceDescriptions(): List<WorkOrderServiceDesc>

    @Query("SELECT wop.workOrderId, p.name as partName FROM work_order_parts wop JOIN parts p ON wop.partId = p.id")
    suspend fun getAllPartNames(): List<WorkOrderPartName>
}
