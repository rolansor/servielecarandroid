/**
 * PartDao.kt - DAO de acceso a datos de repuestos/piezas.
 *
 * Además del CRUD estándar, incluye operaciones atómicas de ajuste de stock
 * ([decreaseStock], [increaseStock]) y búsqueda con o sin piezas inactivas.
 */
package com.example.serviaux.data.dao

import androidx.room.*
import com.example.serviaux.data.entity.Part
import kotlinx.coroutines.flow.Flow

/**
 * DAO para la entidad [Part].
 */
@Dao
interface PartDao {
    @Insert
    suspend fun insert(part: Part): Long

    @Update
    suspend fun update(part: Part)

    /** Solo repuestos activos, ordenados por nombre (para selección en órdenes). */
    @Query("SELECT * FROM parts WHERE active = 1 ORDER BY name")
    fun getAll(): Flow<List<Part>>

    /** Incluye repuestos inactivos (para administración del inventario). */
    @Query("SELECT * FROM parts ORDER BY name")
    fun getAllIncludingInactive(): Flow<List<Part>>

    /** Paginación: primeros N repuestos ordenados por nombre. */
    @Query("SELECT * FROM parts ORDER BY name LIMIT :limit OFFSET :offset")
    fun getPaginated(limit: Int, offset: Int): Flow<List<Part>>

    /** Conteo total de repuestos. */
    @Query("SELECT COUNT(*) FROM parts")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT * FROM parts WHERE id = :id")
    fun getById(id: Long): Flow<Part?>

    @Query("SELECT * FROM parts WHERE id = :id")
    suspend fun getByIdDirect(id: Long): Part?

    @Query("SELECT * FROM parts WHERE active = 1 AND (name LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%')")
    fun search(query: String): Flow<List<Part>>

    @Query("SELECT * FROM parts WHERE name LIKE '%' || :query || '%' OR code LIKE '%' || :query || '%' ORDER BY name")
    fun searchIncludingInactive(query: String): Flow<List<Part>>

    /**
     * Disminuye el stock atómicamente. Solo se ejecuta si hay suficiente existencia.
     * @return Número de filas afectadas (0 si no hay stock suficiente).
     */
    @Query("UPDATE parts SET currentStock = currentStock - :qty WHERE id = :partId AND currentStock >= :qty")
    suspend fun decreaseStock(partId: Long, qty: Int): Int

    /** Incrementa el stock atómicamente; usado al devolver repuestos de una orden eliminada. */
    @Query("UPDATE parts SET currentStock = currentStock + :qty WHERE id = :partId")
    suspend fun increaseStock(partId: Long, qty: Int)

    @Query("SELECT * FROM parts")
    suspend fun getAllDirect(): List<Part>

    @Query("DELETE FROM parts")
    suspend fun deleteAll()
}
