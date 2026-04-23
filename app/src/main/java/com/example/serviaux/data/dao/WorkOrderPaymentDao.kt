/**
 * WorkOrderPaymentDao.kt - DAO de pagos/abonos de órdenes.
 *
 * Gestiona los registros de pago asociados a una orden.
 * Incluye [getTotalPayments] para calcular el saldo pagado.
 */
package com.example.serviaux.data.dao

import androidx.room.*
import com.example.serviaux.data.entity.WorkOrderPayment
import kotlinx.coroutines.flow.Flow

/**
 * Proyección agregada de pagos por orden.
 * Usada para mostrar abono y saldo pendiente en la lista de órdenes
 * sin tener que cargar todos los pagos individualmente.
 */
data class WorkOrderPaymentSummary(
    val workOrderId: Long,
    val totalPaid: Double,
    val totalDiscount: Double
)

/** DAO para la entidad [WorkOrderPayment]. */
@Dao
interface WorkOrderPaymentDao {
    @Insert
    suspend fun insert(payment: WorkOrderPayment): Long

    @Delete
    suspend fun delete(payment: WorkOrderPayment)

    @Query("SELECT * FROM work_order_payments WHERE workOrderId = :workOrderId")
    fun getByWorkOrder(workOrderId: Long): Flow<List<WorkOrderPayment>>

    @Query("SELECT * FROM work_order_payments WHERE workOrderId = :workOrderId")
    suspend fun getByWorkOrderDirect(workOrderId: Long): List<WorkOrderPayment>

    /** Suma reactiva de todos los pagos de una orden; usado para mostrar saldo pendiente. */
    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM work_order_payments WHERE workOrderId = :workOrderId")
    fun getTotalPayments(workOrderId: Long): Flow<Double>

    /** Resúmenes de pagos agrupados por orden, para mostrar abono/saldo en la lista. */
    @Query("SELECT workOrderId, COALESCE(SUM(amount), 0.0) AS totalPaid, COALESCE(SUM(discount), 0.0) AS totalDiscount FROM work_order_payments GROUP BY workOrderId")
    fun getAllPaymentSummaries(): Flow<List<WorkOrderPaymentSummary>>

    @Query("SELECT * FROM work_order_payments")
    suspend fun getAllDirect(): List<WorkOrderPayment>

    @Query("DELETE FROM work_order_payments")
    suspend fun deleteAll()
}
