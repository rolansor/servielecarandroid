package com.example.serviaux.repository

import com.example.serviaux.data.dao.*
import com.example.serviaux.data.entity.*
import kotlinx.coroutines.flow.Flow

class WorkOrderRepository(
    private val workOrderDao: WorkOrderDao,
    private val serviceLineDao: ServiceLineDao,
    private val workOrderPartDao: WorkOrderPartDao,
    private val workOrderPaymentDao: WorkOrderPaymentDao,
    private val workOrderStatusLogDao: WorkOrderStatusLogDao,
    private val partDao: PartDao
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

    // Service Lines
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

    // Work Order Parts
    fun getWorkOrderParts(workOrderId: Long): Flow<List<WorkOrderPart>> = workOrderPartDao.getByWorkOrder(workOrderId)

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

    // Payments
    fun getPayments(workOrderId: Long): Flow<List<WorkOrderPayment>> = workOrderPaymentDao.getByWorkOrder(workOrderId)
    suspend fun getPaymentsDirect(workOrderId: Long): List<WorkOrderPayment> = workOrderPaymentDao.getByWorkOrderDirect(workOrderId)
    fun getTotalPayments(workOrderId: Long): Flow<Double> = workOrderPaymentDao.getTotalPayments(workOrderId)

    suspend fun addPayment(payment: WorkOrderPayment): Long = workOrderPaymentDao.insert(payment)

    // Status Log
    fun getStatusLog(workOrderId: Long): Flow<List<WorkOrderStatusLog>> = workOrderStatusLogDao.getByWorkOrder(workOrderId)

    // Delete work order and all related data, returns photo paths and file paths for cleanup
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
        workOrderDao.deletePaymentsByOrder(orderId)
        workOrderDao.deleteStatusLogByOrder(orderId)
        workOrderDao.deleteById(orderId)
        return photoPaths to filePaths
    }

    // Reports
    suspend fun getTopParts(from: Long, to: Long, limit: Int = 10): List<TopPartResult> =
        workOrderPartDao.getTopParts(from, to, limit)

    // Recalculate totals
    private suspend fun recalculateTotals(workOrderId: Long) {
        val order = workOrderDao.getByIdDirect(workOrderId) ?: return
        val totalLabor = serviceLineDao.getTotalLabor(workOrderId)
        val totalParts = workOrderPartDao.getTotalParts(workOrderId)
        workOrderDao.update(
            order.copy(
                totalLabor = totalLabor,
                totalParts = totalParts,
                total = totalLabor + totalParts,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
