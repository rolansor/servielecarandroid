package com.example.serviaux.repository

import com.example.serviaux.data.dao.VehicleDao
import com.example.serviaux.data.entity.Vehicle
import kotlinx.coroutines.flow.Flow

class VehicleRepository(private val dao: VehicleDao) {
    fun getAll(): Flow<List<Vehicle>> = dao.getAll()
    fun getById(id: Long): Flow<Vehicle?> = dao.getById(id)
    fun getByCustomer(customerId: Long): Flow<List<Vehicle>> = dao.getByCustomer(customerId)
    fun search(query: String): Flow<List<Vehicle>> = dao.search(query)
    suspend fun getByIdDirect(id: Long): Vehicle? = dao.getByIdDirect(id)

    suspend fun insert(vehicle: Vehicle): Long = dao.insert(vehicle)
    suspend fun update(vehicle: Vehicle) = dao.update(vehicle.copy(updatedAt = System.currentTimeMillis()))
}
