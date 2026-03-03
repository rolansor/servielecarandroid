/**
 * CommissionRepository.kt - Repositorio de comisiones de mecánicos.
 *
 * Gestiona la consulta de comisiones pendientes y el pago en lote.
 * Combina datos de [WorkOrderMechanicDao] (comisiones) con [UserDao]
 * (nombres de mecánicos) para la pantalla de gestión de comisiones.
 */
package com.example.serviaux.repository

import com.example.serviaux.data.dao.UserDao
import com.example.serviaux.data.dao.WorkOrderMechanicDao

class CommissionRepository(
    private val workOrderMechanicDao: WorkOrderMechanicDao,
    private val userDao: UserDao
) {
    suspend fun getUnpaidCommissions() = workOrderMechanicDao.getUnpaidCommissions()
    suspend fun getPaidCommissions() = workOrderMechanicDao.getPaidCommissions()
    suspend fun batchMarkAsPaid(ids: List<Long>) = workOrderMechanicDao.batchMarkAsPaid(ids)
    suspend fun getMechanicName(mechanicId: Long) = userDao.getByIdDirect(mechanicId)?.name
}
