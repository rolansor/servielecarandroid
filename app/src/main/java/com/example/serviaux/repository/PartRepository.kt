/**
 * PartRepository.kt - Repositorio de repuestos/piezas del inventario.
 *
 * Envuelve el [PartDao] y expone todas las búsquedas incluyendo repuestos
 * inactivos para que los administradores puedan gestionarlos.
 * Actualiza [updatedAt] automáticamente al modificar un repuesto.
 */
package com.example.serviaux.repository

import com.example.serviaux.data.dao.PartDao
import com.example.serviaux.data.entity.Part
import kotlinx.coroutines.flow.Flow

/** Repositorio para operaciones CRUD de repuestos del inventario. */
class PartRepository(private val dao: PartDao) {
    fun getAll(): Flow<List<Part>> = dao.getAllIncludingInactive()
    fun getPaginated(limit: Int, offset: Int): Flow<List<Part>> = dao.getPaginated(limit, offset)
    fun getTotalCount(): Flow<Int> = dao.getTotalCount()
    fun getById(id: Long): Flow<Part?> = dao.getById(id)
    fun search(query: String): Flow<List<Part>> = dao.searchIncludingInactive(query)
    suspend fun getByIdDirect(id: Long): Part? = dao.getByIdDirect(id)

    /** Nombres de varios repuestos de una vez, para evitar una consulta por cada uno. */
    suspend fun getNamesByIds(ids: List<Long>): Map<Long, String> {
        if (ids.isEmpty()) return emptyMap()
        // SQLite admite hasta 999 parámetros por sentencia.
        return ids.distinct().chunked(900)
            .flatMap { dao.getNamesByIds(it) }
            .associate { it.id to it.name }
    }

    suspend fun insert(part: Part): Long = dao.insert(part)
    suspend fun update(part: Part) = dao.update(part.copy(updatedAt = System.currentTimeMillis()))
}
