package com.example.serviaux.data.dao

import androidx.room.*
import com.example.serviaux.data.entity.Vehicle
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Insert
    suspend fun insert(vehicle: Vehicle): Long

    @Update
    suspend fun update(vehicle: Vehicle)

    @Query("SELECT * FROM vehicles ORDER BY plate")
    fun getAll(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    fun getById(id: Long): Flow<Vehicle?>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getByIdDirect(id: Long): Vehicle?

    @Query("SELECT * FROM vehicles WHERE customerId = :customerId")
    fun getByCustomer(customerId: Long): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE plate LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' OR model LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<Vehicle>>
}
