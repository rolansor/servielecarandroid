/**
 * WorkOrderRepository.kt - Repositorio central de órdenes de trabajo.
 *
 * Es el repositorio más complejo del sistema. Coordina múltiples DAOs para:
 * - CRUD de órdenes con registro automático de cambios de estado.
 * - Gestión de líneas de servicio, repuestos y pagos.
 * - Ajuste automático de stock al agregar/quitar repuestos.
 * - Recálculo automático de totales (mano de obra + repuestos).
 * - Eliminación completa de una orden con restauración de stock y limpieza de archivos.
 * - Consultas para reportes (totales por rango de fechas, repuestos más usados).
 */
package com.example.serviaux.repository

import com.example.serviaux.data.dao.*
import com.example.serviaux.data.entity.*
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de órdenes de trabajo.
 *
 * Coordina [WorkOrderDao], [ServiceLineDao], [WorkOrderPartDao],
 * [WorkOrderPaymentDao], [WorkOrderStatusLogDao] y [PartDao] para
 * mantener la consistencia de datos en todas las operaciones.
 */
class WorkOrderRepository(
    private val workOrderDao: WorkOrderDao,
    private val serviceLineDao: ServiceLineDao,
    private val workOrderPartDao: WorkOrderPartDao,
    private val workOrderPaymentDao: WorkOrderPaymentDao,
    private val workOrderStatusLogDao: WorkOrderStatusLogDao,
    private val partDao: PartDao,
    private val workOrderMechanicDao: WorkOrderMechanicDao,
    private val workOrderExtraDao: WorkOrderExtraDao
) {
    fun getAll(): Flow<List<WorkOrder>> = workOrderDao.getAll()
    fun getById(id: Long): Flow<WorkOrder?> = workOrderDao.getById(id)
    suspend fun getByIdDirect(id: Long): WorkOrder? = workOrderDao.getByIdDirect(id)
    fun getByVehicle(vehicleId: Long): Flow<List<WorkOrder>> = workOrderDao.getByVehicle(vehicleId)
    fun getByCustomer(customerId: Long): Flow<List<WorkOrder>> = workOrderDao.getByCustomer(customerId)
    fun getByStatus(status: OrderStatus): Flow<List<WorkOrder>> = workOrderDao.getByStatus(status)
    fun getByMechanic(mechanicId: Long): Flow<List<WorkOrder>> = workOrderDao.getByMechanic(mechanicId)
    fun countByStatus(status: OrderStatus): Flow<Int> = workOrderDao.countByStatus(status)
    fun getByDateRange(from: Long, to: Long): Flow<List<WorkOrder>> = workOrderDao.getByDateRange(from, to)
    fun getByStatusAndDateRange(status: OrderStatus, from: Long, to: Long): Flow<List<WorkOrder>> = workOrderDao.getByStatusAndDateRange(status, from, to)
    fun getTotalByDateRange(from: Long, to: Long): Flow<Double> = workOrderDao.getTotalByDateRange(from, to)
    suspend fun getAllServiceDescriptions() = workOrderDao.getAllServiceDescriptions()
    suspend fun getAllPartNames() = workOrderDao.getAllPartNames()

    /** Inserta una orden y registra el estado inicial en el historial. */
    suspend fun insert(workOrder: WorkOrder): Long {
        val id = workOrderDao.insert(workOrder)
        workOrderStatusLogDao.insert(
            WorkOrderStatusLog(
                workOrderId = id,
                oldStatus = null,
                newStatus = workOrder.status,
                changedByUserId = workOrder.createdBy,
                note = "Orden creada"
            )
        )
        return id
    }

    suspend fun update(workOrder: WorkOrder) {
        workOrderDao.update(workOrder.copy(updatedAt = System.currentTimeMillis()))
    }

    /** Cambia el estado de una orden y registra la transición en el historial. */
    suspend fun changeStatus(orderId: Long, newStatus: OrderStatus, userId: Long, note: String? = null) {
        val order = workOrderDao.getByIdDirect(orderId) ?: return
        val oldStatus = order.status
        workOrderDao.update(order.copy(status = newStatus, updatedBy = userId, updatedAt = System.currentTimeMillis()))
        workOrderStatusLogDao.insert(
            WorkOrderStatusLog(
                workOrderId = orderId,
                oldStatus = oldStatus,
                newStatus = newStatus,
                changedByUserId = userId,
                note = note
            )
        )
    }

    suspend fun assignMechanic(orderId: Long, mechanicId: Long, userId: Long) {
        val order = workOrderDao.getByIdDirect(orderId) ?: return
        workOrderDao.update(order.copy(assignedMechanicId = mechanicId, updatedBy = userId, updatedAt = System.currentTimeMillis()))
    }

    // ── Líneas de servicio (mano de obra) ────────────────────────────
    fun getServiceLines(workOrderId: Long): Flow<List<ServiceLine>> = serviceLineDao.getByWorkOrder(workOrderId)

    suspend fun addServiceLine(serviceLine: ServiceLine): Long {
        val id = serviceLineDao.insert(serviceLine)
        recalculateTotals(serviceLine.workOrderId)
        return id
    }

    suspend fun updateServiceLine(serviceLine: ServiceLine) {
        serviceLineDao.update(serviceLine)
        recalculateTotals(serviceLine.workOrderId)
    }

    suspend fun deleteServiceLine(serviceLine: ServiceLine) {
        serviceLineDao.delete(serviceLine)
        recalculateTotals(serviceLine.workOrderId)
    }

    // ── Repuestos de la orden ────────────────────────────────────────
    fun getWorkOrderParts(workOrderId: Long): Flow<List<WorkOrderPart>> = workOrderPartDao.getByWorkOrder(workOrderId)

    /** Agrega un repuesto a la orden, disminuye stock y recalcula totales. */
    suspend fun addWorkOrderPart(workOrderPart: WorkOrderPart): Long {
        val id = workOrderPartDao.insert(workOrderPart)
        partDao.decreaseStock(workOrderPart.partId, workOrderPart.quantity)
        recalculateTotals(workOrderPart.workOrderId)
        return id
    }

    suspend fun getLastPartPriceForCustomer(partId: Long, customerId: Long): Double? =
        workOrderPartDao.getLastPriceForCustomer(partId, customerId)

    suspend fun updateWorkOrderPart(workOrderPart: WorkOrderPart) {
        workOrderPartDao.update(workOrderPart)
        recalculateTotals(workOrderPart.workOrderId)
    }

    suspend fun deleteWorkOrderPart(workOrderPart: WorkOrderPart) {
        workOrderPartDao.delete(workOrderPart)
        recalculateTotals(workOrderPart.workOrderId)
    }

    // ── Pagos ──────────────────────────────────────────────────────────
    fun getPayments(workOrderId: Long): Flow<List<WorkOrderPayment>> = workOrderPaymentDao.getByWorkOrder(workOrderId)
    suspend fun getPaymentsDirect(workOrderId: Long): List<WorkOrderPayment> = workOrderPaymentDao.getByWorkOrderDirect(workOrderId)
    fun getTotalPayments(workOrderId: Long): Flow<Double> = workOrderPaymentDao.getTotalPayments(workOrderId)

    suspend fun addPayment(payment: WorkOrderPayment): Long = workOrderPaymentDao.insert(payment)

    // Status Log
    fun getStatusLog(workOrderId: Long): Flow<List<WorkOrderStatusLog>> = workOrderStatusLogDao.getByWorkOrder(workOrderId)

    // ── Extras de la orden ──────────────────────────────────────────
    fun getWorkOrderExtras(workOrderId: Long): Flow<List<WorkOrderExtra>> = workOrderExtraDao.getByWorkOrder(workOrderId)

    suspend fun addWorkOrderExtra(extra: WorkOrderExtra): Long {
        val id = workOrderExtraDao.insert(extra)
        recalculateTotals(extra.workOrderId)
        return id
    }

    suspend fun updateWorkOrderExtra(extra: WorkOrderExtra) {
        workOrderExtraDao.update(extra)
        recalculateTotals(extra.workOrderId)
    }

    suspend fun deleteWorkOrderExtra(extra: WorkOrderExtra) {
        workOrderExtraDao.delete(extra)
        recalculateTotals(extra.workOrderId)
    }

    // ── Mecánicos de la orden ──────────────────────────────────────
    fun getOrderMechanics(workOrderId: Long) = workOrderMechanicDao.getByWorkOrder(workOrderId)
    suspend fun getOrderMechanicsDirect(workOrderId: Long) = workOrderMechanicDao.getByWorkOrderDirect(workOrderId)

    suspend fun addMechanicToOrder(mechanic: WorkOrderMechanic): Long = workOrderMechanicDao.insert(mechanic)

    suspend fun removeMechanicFromOrder(mechanic: WorkOrderMechanic) = workOrderMechanicDao.delete(mechanic)

    suspend fun updateOrderMechanic(mechanic: WorkOrderMechanic) = workOrderMechanicDao.update(mechanic)

    suspend fun toggleCommissionPaid(mechanic: WorkOrderMechanic) {
        if (mechanic.commissionPaid) {
            workOrderMechanicDao.markAsUnpaid(mechanic.id)
        } else {
            workOrderMechanicDao.markAsPaid(mechanic.id)
        }
    }

    // ── Eliminación completa de orden ────────────────────────────────

    /** Elimina la orden y todos sus datos relacionados. Restaura el stock y retorna rutas de archivos para limpieza. */
    suspend fun deleteOrder(orderId: Long): Pair<List<String>, List<String>> {
        val order = workOrderDao.getByIdDirect(orderId)
        val photoPaths = order?.photoPaths?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        val filePaths = order?.filePaths?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        // Restore stock for parts used in this order
        val parts = workOrderPartDao.getByWorkOrderDirect(orderId)
        for (part in parts) {
            partDao.increaseStock(part.partId, part.quantity)
        }
        workOrderDao.deleteServiceLinesByOrder(orderId)
        workOrderDao.deletePartsByOrder(orderId)
        workOrderDao.deleteExtrasByOrder(orderId)
        workOrderDao.deletePaymentsByOrder(orderId)
        workOrderDao.deleteStatusLogByOrder(orderId)
        workOrderMechanicDao.deleteByWorkOrder(orderId)
        workOrderDao.deleteById(orderId)
        return photoPaths to filePaths
    }

    // ── Reportes ────────────────────────────────────────────────────────
    suspend fun getTopParts(from: Long, to: Long, limit: Int = 10): List<TopPartResult> =
        workOrderPartDao.getTopParts(from, to, limit)

    // ── Recálculo de totales ──────────────────────────────────────────

    /** Recalcula totalLabor, totalParts y total sumando las líneas de servicio y repuestos. */
    private suspend fun recalculateTotals(workOrderId: Long) {
        val order = workOrderDao.getByIdDirect(workOrderId) ?: return
        val totalLabor = serviceLineDao.getTotalLabor(workOrderId)
        val totalParts = workOrderPartDao.getTotalParts(workOrderId)
        val totalExtras = workOrderExtraDao.getTotalExtras(workOrderId)
        workOrderDao.update(
            order.copy(
                totalLabor = totalLabor,
                totalParts = totalParts,
                totalExtras = totalExtras,
                total = totalLabor + totalParts + totalExtras,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
