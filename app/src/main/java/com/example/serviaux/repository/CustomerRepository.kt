/**
 * CustomerRepository.kt - Repositorio de clientes.
 *
 * Envuelve el [CustomerDao] con lógica adicional mínima:
 * actualiza automáticamente el campo [updatedAt] al modificar un cliente.
 */
package com.example.serviaux.repository

import com.example.serviaux.data.dao.CustomerDao
import com.example.serviaux.data.entity.Customer
import kotlinx.coroutines.flow.Flow

/** Repositorio para operaciones CRUD de clientes del taller. */
class CustomerRepository(private val dao: CustomerDao) {
    fun getAll(): Flow<List<Customer>> = dao.getAll()
    fun getById(id: Long): Flow<Customer?> = dao.getById(id)
    fun search(query: String): Flow<List<Customer>> = dao.search(query)
    fun getPaginated(limit: Int, offset: Int): Flow<List<Customer>> = dao.getPaginated(limit, offset)
    fun getTotalCount(): Flow<Int> = dao.getTotalCount()
    suspend fun getByIdDirect(id: Long): Customer? = dao.getByIdDirect(id)

    suspend fun findByDocument(docType: String, idNumber: String): Customer? = dao.findByDocument(docType, idNumber)
    suspend fun insert(customer: Customer): Long = dao.insert(customer)
    suspend fun update(customer: Customer) = dao.update(customer.copy(updatedAt = System.currentTimeMillis()))
    suspend fun delete(customer: Customer) = dao.delete(customer)
    suspend fun canDelete(customerId: Long): Boolean =
        dao.countVehicles(customerId) == 0 && dao.countOrders(customerId) == 0
}
