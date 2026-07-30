package com.example.serviaux.data.dao

import androidx.room.*
import com.example.serviaux.data.entity.PendingCommissionRow
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

    /**
     * Actualiza solo el monto de comisión. Se usa al recalcular por cambios en la mano de obra;
     * como escritura parcial, no puede pisar `commissionPaid` ni `paidAt`.
     *
     * La entidad no tiene columna `updatedAt`, así que no hay marca de tiempo que refrescar.
     */
    @Query("UPDATE work_order_mechanics SET commissionAmount = :amount WHERE id = :id")
    suspend fun updateCommissionAmount(id: Long, amount: Double)

    @Query("SELECT * FROM work_order_mechanics")
    suspend fun getAllDirect(): List<WorkOrderMechanic>

    @Query("DELETE FROM work_order_mechanics")
    suspend fun deleteAll()

    @Query("DELETE FROM work_order_mechanics WHERE workOrderId = :workOrderId")
    suspend fun deleteByWorkOrder(workOrderId: Long)

    @Query("""
        SELECT wm.*, v.plate AS vehiclePlate
        FROM work_order_mechanics wm
        INNER JOIN work_orders wo ON wm.workOrderId = wo.id
        INNER JOIN vehicles v ON wo.vehicleId = v.id
        WHERE wm.commissionPaid = 0
            AND wm.commissionType != 'NINGUNA'
            AND wo.status IN ('LISTO', 'ENTREGADO', 'CERRADO')
        ORDER BY wm.mechanicId, wm.createdAt DESC
    """)
    suspend fun getUnpaidCommissions(): List<PendingCommissionRow>

    @Query("UPDATE work_order_mechanics SET commissionPaid = 1, paidAt = :paidAt WHERE id IN (:ids)")
    suspend fun batchMarkAsPaid(ids: List<Long>, paidAt: Long = System.currentTimeMillis())

    @Query("""
        SELECT wm.*, v.plate AS vehiclePlate
        FROM work_order_mechanics wm
        INNER JOIN work_orders wo ON wm.workOrderId = wo.id
        INNER JOIN vehicles v ON wo.vehicleId = v.id
        WHERE wm.commissionPaid = 1
            AND wm.commissionType != 'NINGUNA'
        ORDER BY wm.paidAt DESC
    """)
    suspend fun getPaidCommissions(): List<PendingCommissionRow>
}
