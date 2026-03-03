/**
 * VehicleRepository.kt - Repositorio de vehículos.
 *
 * Envuelve el [VehicleDao] con lógica adicional mínima:
 * actualiza automáticamente el campo [updatedAt] al modificar un vehículo.
 */
package com.example.serviaux.repository

import com.example.serviaux.data.dao.VehicleDao
import com.example.serviaux.data.entity.Vehicle
import kotlinx.coroutines.flow.Flow

/** Repositorio para operaciones CRUD de vehículos del taller. */
class VehicleRepository(private val dao: VehicleDao) {
    fun getAll(): Flow<List<Vehicle>> = dao.getAll()
    fun getById(id: Long): Flow<Vehicle?> = dao.getById(id)
    fun getByCustomer(customerId: Long): Flow<List<Vehicle>> = dao.getByCustomer(customerId)
    fun search(query: String): Flow<List<Vehicle>> = dao.search(query)
    fun getPaginated(limit: Int, offset: Int): Flow<List<Vehicle>> = dao.getPaginated(limit, offset)
    fun getTotalCount(): Flow<Int> = dao.getTotalCount()
    suspend fun getByIdDirect(id: Long): Vehicle? = dao.getByIdDirect(id)

    suspend fun findByPlate(plate: String): Vehicle? = dao.findByPlate(plate)
    suspend fun insert(vehicle: Vehicle): Long = dao.insert(vehicle)
    suspend fun update(vehicle: Vehicle) = dao.update(vehicle.copy(updatedAt = System.currentTimeMillis()))
    suspend fun delete(vehicle: Vehicle) = dao.delete(vehicle)
    suspend fun canDelete(vehicleId: Long): Boolean = dao.countOrders(vehicleId) == 0
}
