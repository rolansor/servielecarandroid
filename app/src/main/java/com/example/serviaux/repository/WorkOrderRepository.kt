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

import androidx.room.withTransaction
import com.example.serviaux.data.ServiauxDatabase
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
    private val database: ServiauxDatabase,
    private val workOrderDao: WorkOrderDao,
    private val serviceLineDao: ServiceLineDao,
    private val workOrderPartDao: WorkOrderPartDao,
    private val workOrderPaymentDao: WorkOrderPaymentDao,
    private val workOrderStatusLogDao: WorkOrderStatusLogDao,
    private val partDao: PartDao,
    private val workOrderMechanicDao: WorkOrderMechanicDao,
    private val workOrderExtraDao: WorkOrderExtraDao,
    private val appointmentDao: AppointmentDao
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

    /**
     * Cambia el estado de una orden y registra la transición en el historial.
     *
     * Ambas escrituras van en una transacción: si fallara la segunda, la orden quedaría con un
     * estado que el historial no explica.
     */
    suspend fun changeStatus(orderId: Long, newStatus: OrderStatus, userId: Long, note: String? = null) =
        database.withTransaction {
            val order = workOrderDao.getByIdDirect(orderId) ?: return@withTransaction
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

    suspend fun addServiceLine(serviceLine: ServiceLine): Long = database.withTransaction {
        val id = serviceLineDao.insert(serviceLine)
        recalculateTotals(serviceLine.workOrderId)
        id
    }

    suspend fun updateServiceLine(serviceLine: ServiceLine) = database.withTransaction {
        serviceLineDao.update(serviceLine)
        recalculateTotals(serviceLine.workOrderId)
    }

    suspend fun deleteServiceLine(serviceLine: ServiceLine) = database.withTransaction {
        serviceLineDao.delete(serviceLine)
        recalculateTotals(serviceLine.workOrderId)
    }

    /** Líneas de servicio y repuestos de todas las órdenes de un cliente, en una consulta cada uno. */
    suspend fun getServiceLinesByCustomer(customerId: Long): List<ServiceLine> =
        serviceLineDao.getByCustomerDirect(customerId)

    suspend fun getWorkOrderPartsByCustomer(customerId: Long): List<WorkOrderPart> =
        workOrderPartDao.getByCustomerDirect(customerId)

    // ── Escrituras parciales de la orden ─────────────────────────────
    // Evitan que guardar una sección revierta lo que otra acaba de escribir.

    /** Guarda solo los campos de "Datos del Proceso" de la orden. */
    suspend fun updateProcessFields(
        orderId: Long,
        entryMileage: Int?,
        fuelLevel: String?,
        deliveryNote: String?,
        invoiceNumber: String?,
        notes: String?,
        updatedBy: Long
    ) = workOrderDao.updateProcessFields(
        id = orderId,
        entryMileage = entryMileage,
        fuelLevel = fuelLevel,
        deliveryNote = deliveryNote,
        invoiceNumber = invoiceNumber,
        notes = notes,
        updatedBy = updatedBy,
        updatedAt = System.currentTimeMillis()
    )

    /** Guarda solo las rutas de fotos de la orden. */
    suspend fun updatePhotoPaths(orderId: Long, photoPaths: String?) =
        workOrderDao.updatePhotoPaths(orderId, photoPaths, System.currentTimeMillis())

    /** Guarda solo las rutas de archivos adjuntos de la orden. */
    suspend fun updateFilePaths(orderId: Long, filePaths: String?) =
        workOrderDao.updateFilePaths(orderId, filePaths, System.currentTimeMillis())

    // ── Repuestos de la orden ────────────────────────────────────────
    fun getWorkOrderParts(workOrderId: Long): Flow<List<WorkOrderPart>> = workOrderPartDao.getByWorkOrder(workOrderId)

    /**
     * Agrega un repuesto a la orden, descuenta el stock y recalcula totales.
     *
     * Si no hay existencia suficiente la línea se registra igualmente —el taller no puede
     * quedar bloqueado por un inventario mal contado— y el stock queda en negativo para que
     * el descubierto sea visible y corregible.
     *
     * @return id de la línea creada y unidades que quedaron en descubierto (0 si había stock).
     */
    suspend fun addWorkOrderPart(workOrderPart: WorkOrderPart): AddPartResult =
        database.withTransaction {
            val id = workOrderPartDao.insert(workOrderPart)
            val shortfall = takeFromStock(workOrderPart.partId, workOrderPart.quantity)
            recalculateTotals(workOrderPart.workOrderId)
            AddPartResult(lineId = id, stockShortfall = shortfall)
        }

    suspend fun getLastPartPriceForCustomer(partId: Long, customerId: Long): Double? =
        workOrderPartDao.getLastPriceForCustomer(partId, customerId)

    /**
     * Actualiza una línea de repuesto ajustando el stock por la diferencia de cantidad
     * (y devolviendo/tomando del repuesto correcto si se cambió de pieza).
     *
     * @return unidades que quedaron en descubierto (0 si había stock).
     */
    suspend fun updateWorkOrderPart(workOrderPart: WorkOrderPart): Int = database.withTransaction {
        val previous = workOrderPartDao.getByIdDirect(workOrderPart.id)
        workOrderPartDao.update(workOrderPart)
        var shortfall = 0
        if (previous != null) {
            if (previous.partId != workOrderPart.partId) {
                // Se cambió la pieza: se devuelve todo al repuesto anterior y se toma del nuevo.
                partDao.increaseStock(previous.partId, previous.quantity)
                shortfall = takeFromStock(workOrderPart.partId, workOrderPart.quantity)
            } else {
                val delta = workOrderPart.quantity - previous.quantity
                if (delta > 0) shortfall = takeFromStock(workOrderPart.partId, delta)
                else if (delta < 0) partDao.increaseStock(workOrderPart.partId, -delta)
            }
        }
        recalculateTotals(workOrderPart.workOrderId)
        shortfall
    }

    /** Elimina la línea y devuelve al inventario las unidades que había consumido. */
    suspend fun deleteWorkOrderPart(workOrderPart: WorkOrderPart) = database.withTransaction {
        workOrderPartDao.delete(workOrderPart)
        if (workOrderPart.quantity > 0) {
            partDao.increaseStock(workOrderPart.partId, workOrderPart.quantity)
        }
        recalculateTotals(workOrderPart.workOrderId)
    }

    /**
     * Descuenta [qty] del stock. Intenta primero el descuento condicional (atómico);
     * si no hay existencia suficiente descuenta igualmente y reporta el descubierto.
     */
    private suspend fun takeFromStock(partId: Long, qty: Int): Int {
        if (qty <= 0) return 0
        if (partDao.decreaseStock(partId, qty) > 0) return 0
        val available = (partDao.getByIdDirect(partId)?.currentStock ?: 0).coerceAtLeast(0)
        partDao.decreaseStockUnchecked(partId, qty)
        return qty - available
    }

    // ── Pagos ──────────────────────────────────────────────────────────
    fun getPayments(workOrderId: Long): Flow<List<WorkOrderPayment>> = workOrderPaymentDao.getByWorkOrder(workOrderId)
    suspend fun getPaymentsDirect(workOrderId: Long): List<WorkOrderPayment> = workOrderPaymentDao.getByWorkOrderDirect(workOrderId)
    fun getTotalPayments(workOrderId: Long): Flow<Double> = workOrderPaymentDao.getTotalPayments(workOrderId)
    fun getAllPaymentSummaries(): Flow<List<com.example.serviaux.data.dao.WorkOrderPaymentSummary>> =
        workOrderPaymentDao.getAllPaymentSummaries()

    suspend fun addPayment(payment: WorkOrderPayment): Long = workOrderPaymentDao.insert(payment)

    /** Corrige un pago ya registrado. El saldo de la orden se deriva de los pagos, no se almacena. */
    suspend fun updatePayment(payment: WorkOrderPayment) = workOrderPaymentDao.update(payment)

    // Status Log
    fun getStatusLog(workOrderId: Long): Flow<List<WorkOrderStatusLog>> = workOrderStatusLogDao.getByWorkOrder(workOrderId)

    // ── Extras de la orden ──────────────────────────────────────────
    fun getWorkOrderExtras(workOrderId: Long): Flow<List<WorkOrderExtra>> = workOrderExtraDao.getByWorkOrder(workOrderId)

    suspend fun addWorkOrderExtra(extra: WorkOrderExtra): Long = database.withTransaction {
        val id = workOrderExtraDao.insert(extra)
        recalculateTotals(extra.workOrderId)
        id
    }

    suspend fun updateWorkOrderExtra(extra: WorkOrderExtra) = database.withTransaction {
        workOrderExtraDao.update(extra)
        recalculateTotals(extra.workOrderId)
    }

    suspend fun deleteWorkOrderExtra(extra: WorkOrderExtra) = database.withTransaction {
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

    /**
     * Elimina la orden y todos sus datos relacionados. Restaura el stock y retorna las rutas de
     * archivos para que el llamador borre las fotos y adjuntos del disco.
     *
     * Todo va en una transacción y el stock se devuelve **al final**: si el borrado fallara a
     * mitad, un reintento devolvería el stock por segunda vez e inflaría el inventario.
     */
    suspend fun deleteOrder(orderId: Long): Pair<List<String>, List<String>> =
        database.withTransaction {
            val order = workOrderDao.getByIdDirect(orderId)
            val photoPaths = order?.photoPaths?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
            val filePaths = order?.filePaths?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
            val parts = workOrderPartDao.getByWorkOrderDirect(orderId)

            workOrderDao.deleteServiceLinesByOrder(orderId)
            workOrderDao.deletePartsByOrder(orderId)
            workOrderDao.deleteExtrasByOrder(orderId)
            workOrderDao.deletePaymentsByOrder(orderId)
            workOrderDao.deleteStatusLogByOrder(orderId)
            workOrderMechanicDao.deleteByWorkOrder(orderId)
            workOrderDao.deleteById(orderId)

            // El turno que originó la orden queda sin referencia: si no, apuntaría a una orden
            // inexistente y seguiría marcado como convertido.
            appointmentDao.clearWorkOrderReference(orderId)

            for (part in parts) {
                partDao.increaseStock(part.partId, part.quantity)
            }
            photoPaths to filePaths
        }

    // ── Reportes ────────────────────────────────────────────────────────
    suspend fun getTopParts(from: Long, to: Long, limit: Int = 10): List<TopPartResult> =
        workOrderPartDao.getTopParts(from, to, limit)

    // ── Recálculo de totales ──────────────────────────────────────────

    /**
     * Recalcula los totales de la orden a partir de sus líneas y ajusta las comisiones.
     *
     * Los importes se redondean a dos decimales antes de guardarse: acumular el error binario
     * de `Double` hacía que la suma de las filas impresas en un PDF no cuadrara con su total.
     */
    private suspend fun recalculateTotals(workOrderId: Long) {
        val order = workOrderDao.getByIdDirect(workOrderId) ?: return
        val totalLabor = round2(serviceLineDao.getTotalLabor(workOrderId))
        val totalParts = round2(workOrderPartDao.getTotalParts(workOrderId))
        val totalExtras = round2(workOrderExtraDao.getTotalExtras(workOrderId))
        workOrderDao.update(
            order.copy(
                totalLabor = totalLabor,
                totalParts = totalParts,
                totalExtras = totalExtras,
                total = round2(totalLabor + totalParts + totalExtras),
                updatedAt = System.currentTimeMillis()
            )
        )
        recalculateCommissions(workOrderId, totalLabor)
    }

    /**
     * Ajusta el monto de comisión de los mecánicos de la orden según la mano de obra actual.
     *
     * Se invoca desde [recalculateTotals], que es el único punto por el que pasan todos los
     * cambios de servicios, repuestos y extras. Sin esto, una comisión porcentual quedaba
     * congelada en el valor que tenía al asignar al mecánico —normalmente $0, porque se asigna
     * antes de cargar la mano de obra— y se pagaba así.
     *
     * Las comisiones ya pagadas no se tocan: su importe es un hecho contable cerrado.
     */
    private suspend fun recalculateCommissions(workOrderId: Long, totalLabor: Double) {
        val mechanics = workOrderMechanicDao.getByWorkOrderDirect(workOrderId)
        for (mechanic in mechanics) {
            if (mechanic.commissionPaid) continue
            val newAmount = when (mechanic.commissionType) {
                CommissionType.PORCENTAJE.name -> round2(totalLabor * (mechanic.commissionValue / 100.0))
                CommissionType.FIJA.name -> round2(mechanic.commissionValue)
                else -> 0.0
            }
            if (kotlin.math.abs(newAmount - mechanic.commissionAmount) > 0.001) {
                workOrderMechanicDao.updateCommissionAmount(mechanic.id, newAmount)
            }
        }
    }

    /** Redondeo monetario a dos decimales. */
    private fun round2(value: Double): Double = kotlin.math.round(value * 100.0) / 100.0
}

/**
 * Resultado de agregar un repuesto a una orden.
 *
 * @property lineId Id de la línea creada.
 * @property stockShortfall Unidades que quedaron en descubierto por falta de existencia (0 si había stock).
 */
data class AddPartResult(
    val lineId: Long,
    val stockShortfall: Int
)
