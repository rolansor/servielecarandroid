package com.example.serviaux.data.dao

import androidx.room.*
import com.example.serviaux.data.entity.Appointment
import com.example.serviaux.data.entity.AppointmentStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {
    @Insert
    suspend fun insert(appointment: Appointment): Long

    @Update
    suspend fun update(appointment: Appointment)

    @Query("DELETE FROM appointments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM appointments ORDER BY scheduledDate DESC")
    fun getAll(): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE id = :id")
    fun getById(id: Long): Flow<Appointment?>

    @Query("SELECT * FROM appointments WHERE id = :id")
    suspend fun getByIdDirect(id: Long): Appointment?

    @Query("SELECT * FROM appointments WHERE status = :status ORDER BY scheduledDate DESC")
    fun getByStatus(status: AppointmentStatus): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE customerId = :customerId ORDER BY scheduledDate DESC")
    fun getByCustomer(customerId: Long): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE scheduledDate BETWEEN :from AND :to ORDER BY scheduledDate ASC")
    fun getByDateRange(from: Long, to: Long): Flow<List<Appointment>>

    @Query("SELECT * FROM appointments WHERE status IN ('PENDIENTE', 'CONFIRMADO') AND scheduledDate BETWEEN :from AND :to ORDER BY scheduledDate ASC")
    fun getActiveByDateRange(from: Long, to: Long): Flow<List<Appointment>>

    @Query("SELECT COUNT(*) FROM appointments WHERE status = :status")
    fun countByStatus(status: AppointmentStatus): Flow<Int>

    @Query("DELETE FROM appointments")
    suspend fun deleteAll()

    @Query("SELECT * FROM appointments")
    suspend fun getAllDirect(): List<Appointment>
}
