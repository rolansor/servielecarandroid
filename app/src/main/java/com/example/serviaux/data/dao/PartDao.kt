package com.example.serviaux.data.dao

import androidx.room.*
import com.example.serviaux.data.entity.Part
import kotlinx.coroutines.flow.Flow

@Dao
interface PartDao {
    @Insert
    suspend fun insert(part: Part): Long

    @Update
    suspend fun update(part: Part)

    @Query("SELECT * FROM parts WHERE active = 1 ORDER BY name")
    fun getAll(): Flow<List<Part>>

    @Query("SELECT * FROM parts ORDER BY name")
    fun getAllIncludingInactive(): Flow<List<Part>>

    @Query("SELECT * FROM parts WHERE id = :id")
    fun getById(id: Long): Flow<Part?>

    @Query("SELECT * FROM parts WHERE id = :id")
    suspend fun getByIdDirect(id: Long): Part?

    @Query("SELECT * FROM parts WHERE active = 1 AND (name LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%')")
    fun search(query: String): Flow<List<Part>>

    @Query("SELECT * FROM parts WHERE name LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%' ORDER BY name")
    fun searchIncludingInactive(query: String): Flow<List<Part>>

    @Query("UPDATE parts SET currentStock = currentStock - :qty WHERE id = :partId AND currentStock >= :qty")
    suspend fun decreaseStock(partId: Long, qty: Int): Int
}
