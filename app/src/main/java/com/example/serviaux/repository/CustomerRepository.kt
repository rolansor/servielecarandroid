package com.example.serviaux.repository

import com.example.serviaux.data.dao.CustomerDao
import com.example.serviaux.data.entity.Customer
import kotlinx.coroutines.flow.Flow

class CustomerRepository(private val dao: CustomerDao) {
    fun getAll(): Flow<List<Customer>> = dao.getAll()
    fun getById(id: Long): Flow<Customer?> = dao.getById(id)
    fun search(query: String): Flow<List<Customer>> = dao.search(query)
    suspend fun getByIdDirect(id: Long): Customer? = dao.getByIdDirect(id)

    suspend fun insert(customer: Customer): Long = dao.insert(customer)
    suspend fun update(customer: Customer) = dao.update(customer.copy(updatedAt = System.currentTimeMillis()))
}
