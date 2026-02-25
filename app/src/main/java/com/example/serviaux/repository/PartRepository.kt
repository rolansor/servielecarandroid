package com.example.serviaux.repository

import com.example.serviaux.data.dao.PartDao
import com.example.serviaux.data.entity.Part
import kotlinx.coroutines.flow.Flow

class PartRepository(private val dao: PartDao) {
    fun getAll(): Flow<List<Part>> = dao.getAllIncludingInactive()
    fun getById(id: Long): Flow<Part?> = dao.getById(id)
    fun search(query: String): Flow<List<Part>> = dao.searchIncludingInactive(query)
    suspend fun getByIdDirect(id: Long): Part? = dao.getByIdDirect(id)

    suspend fun insert(part: Part): Long = dao.insert(part)
    suspend fun update(part: Part) = dao.update(part.copy(updatedAt = System.currentTimeMillis()))
}
