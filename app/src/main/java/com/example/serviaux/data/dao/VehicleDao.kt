/**
 * VehicleDao.kt - DAO de acceso a datos de vehículos.
 *
 * Operaciones CRUD y búsqueda por placa, marca o modelo.
 * Incluye consulta por cliente para la vista de detalle de cliente.
 */
package com.example.serviaux.data.dao

import androidx.room.*
import com.example.serviaux.data.entity.Vehicle
import kotlinx.coroutines.flow.Flow

/**
 * DAO para la entidad [Vehicle].
 */
@Dao
interface VehicleDao {
    @Insert
    suspend fun insert(vehicle: Vehicle): Long

    @Update
    suspend fun update(vehicle: Vehicle)

    /** Todos los vehículos ordenados por fecha de creación descendente (reactivo). */
    @Query("SELECT * FROM vehicles ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    fun getById(id: Long): Flow<Vehicle?>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getByIdDirect(id: Long): Vehicle?

    /** Vehículos de un cliente específico; ordenados por fecha de creación descendente. */
    @Query("SELECT * FROM vehicles WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getByCustomer(customerId: Long): Flow<List<Vehicle>>

    /** Búsqueda parcial por placa, marca o modelo. */
    @Query("SELECT * FROM vehicles WHERE plate LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' OR model LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<Vehicle>>

    /** Busca un vehículo por placa para validar duplicados. */
    @Query("SELECT * FROM vehicles WHERE plate = :plate LIMIT 1")
    suspend fun findByPlate(plate: String): Vehicle?

    @Delete
    suspend fun delete(vehicle: Vehicle)

    /** Cuenta órdenes asociadas a un vehículo. */
    @Query("SELECT COUNT(*) FROM work_orders WHERE vehicleId = :vehicleId")
    suspend fun countOrders(vehicleId: Long): Int

    /** Consulta directa para exportación de respaldos. */
    @Query("SELECT * FROM vehicles")
    suspend fun getAllDirect(): List<Vehicle>

    @Query("DELETE FROM vehicles")
    suspend fun deleteAll()

    /** Consulta paginada de vehículos. */
    @Query("SELECT * FROM vehicles ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    fun getPaginated(limit: Int, offset: Int): Flow<List<Vehicle>>

    /** Total de vehículos para cálculo de páginas. */
    @Query("SELECT COUNT(*) FROM vehicles")
    fun getTotalCount(): Flow<Int>
}
