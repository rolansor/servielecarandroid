/**
 * CustomerDao.kt - DAO de acceso a datos de clientes.
 *
 * Proporciona operaciones CRUD y búsqueda por nombre, teléfono o cédula
 * para la tabla [customers]. Usado por [CustomerRepository].
 */
package com.example.serviaux.data.dao

import androidx.room.*
import com.example.serviaux.data.entity.Customer
import kotlinx.coroutines.flow.Flow

/**
 * DAO para la entidad [Customer].
 */
@Dao
interface CustomerDao {
    @Insert
    suspend fun insert(customer: Customer): Long

    @Update
    suspend fun update(customer: Customer)

    /** Todos los clientes ordenados por fecha de creación (reactivo para la lista). */
    @Query("SELECT * FROM customers ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    fun getById(id: Long): Flow<Customer?>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getByIdDirect(id: Long): Customer?

    /** Busca clientes por nombre, teléfono o número de identidad (búsqueda parcial). */
    @Query("SELECT * FROM customers WHERE fullName LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR idNumber LIKE '%' || :query || '%'")
    fun search(query: String): Flow<List<Customer>>

    /** Busca un cliente por tipo y número de documento para validar duplicados. */
    @Query("SELECT * FROM customers WHERE docType = :docType AND idNumber = :idNumber LIMIT 1")
    suspend fun findByDocument(docType: String, idNumber: String): Customer?

    /** Consulta directa para exportación de respaldos. */
    @Query("SELECT * FROM customers")
    suspend fun getAllDirect(): List<Customer>

    @Delete
    suspend fun delete(customer: Customer)

    /** Cuenta vehículos asociados a un cliente. */
    @Query("SELECT COUNT(*) FROM vehicles WHERE customerId = :customerId")
    suspend fun countVehicles(customerId: Long): Int

    /** Cuenta órdenes asociadas a un cliente. */
    @Query("SELECT COUNT(*) FROM work_orders WHERE customerId = :customerId")
    suspend fun countOrders(customerId: Long): Int

    /** Elimina todos los registros; usado durante la restauración de respaldos. */
    @Query("DELETE FROM customers")
    suspend fun deleteAll()
}
