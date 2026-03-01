/**
 * UserDao.kt - DAO de acceso a datos de usuarios.
 *
 * Proporciona operaciones CRUD y consultas específicas para la tabla [users].
 * Usado principalmente por [AuthRepository] para login y gestión de usuarios.
 */
package com.example.serviaux.data.dao

import androidx.room.*
import com.example.serviaux.data.entity.User
import com.example.serviaux.data.entity.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * DAO para la entidad [User].
 *
 * Incluye consultas reactivas (Flow) para la UI y consultas directas (suspend)
 * para operaciones puntuales como login y respaldos.
 */
@Dao
interface UserDao {
    @Insert
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User)

    /** Busca un usuario activo por nombre de usuario; usado en el proceso de login. */
    @Query("SELECT * FROM users WHERE username = :username AND active = 1 LIMIT 1")
    suspend fun getByUsername(username: String): User?

    /** Obtiene todos los usuarios ordenados por nombre (reactivo). */
    @Query("SELECT * FROM users ORDER BY name")
    fun getAll(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE id = :id")
    fun getById(id: Long): Flow<User?>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getByIdDirect(id: Long): User?

    /** Filtra usuarios activos por rol; usado para listar mecánicos en la asignación de órdenes. */
    @Query("SELECT * FROM users WHERE role = :role AND active = 1")
    fun getByRole(role: UserRole): Flow<List<User>>

    /** Consulta directa para exportación de respaldos. */
    @Query("SELECT * FROM users")
    suspend fun getAllDirect(): List<User>

    /** Elimina todos los registros; usado durante la restauración de respaldos. */
    @Query("DELETE FROM users")
    suspend fun deleteAll()
}
