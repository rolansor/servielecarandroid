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

    suspend fun insert(part: Part): Long = dao.insert(part)
    suspend fun update(part: Part) = dao.update(part.copy(updatedAt = System.currentTimeMillis()))
}
