package com.example.serviaux.repository

import com.example.serviaux.data.dao.AppointmentDao
import com.example.serviaux.data.entity.Appointment
import com.example.serviaux.data.entity.AppointmentStatus
import kotlinx.coroutines.flow.Flow

class AppointmentRepository(private val dao: AppointmentDao) {
    fun getAll(): Flow<List<Appointment>> = dao.getAll()
    fun getById(id: Long): Flow<Appointment?> = dao.getById(id)
    suspend fun getByIdDirect(id: Long): Appointment? = dao.getByIdDirect(id)
    fun getByStatus(status: AppointmentStatus): Flow<List<Appointment>> = dao.getByStatus(status)
    fun getByCustomer(customerId: Long): Flow<List<Appointment>> = dao.getByCustomer(customerId)
    fun getByDateRange(from: Long, to: Long): Flow<List<Appointment>> = dao.getByDateRange(from, to)
    fun getActiveByDateRange(from: Long, to: Long): Flow<List<Appointment>> = dao.getActiveByDateRange(from, to)
    fun countByStatus(status: AppointmentStatus): Flow<Int> = dao.countByStatus(status)

    suspend fun insert(appointment: Appointment): Long = dao.insert(appointment)

    suspend fun update(appointment: Appointment) =
        dao.update(appointment.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun cancel(id: Long) {
        val appointment = dao.getByIdDirect(id) ?: return
        dao.update(appointment.copy(status = AppointmentStatus.CANCELADO, updatedAt = System.currentTimeMillis()))
    }

    suspend fun confirm(id: Long) {
        val appointment = dao.getByIdDirect(id) ?: return
        dao.update(appointment.copy(status = AppointmentStatus.CONFIRMADO, updatedAt = System.currentTimeMillis()))
    }

    suspend fun markConverted(id: Long, workOrderId: Long) {
        val appointment = dao.getByIdDirect(id) ?: return
        dao.update(appointment.copy(
            status = AppointmentStatus.CONVERTIDO,
            workOrderId = workOrderId,
            updatedAt = System.currentTimeMillis()
        ))
    }

    suspend fun getAllDirect(): List<Appointment> = dao.getAllDirect()
    suspend fun deleteAll() = dao.deleteAll()
}
