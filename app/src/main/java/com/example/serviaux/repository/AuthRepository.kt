/**
 * AuthRepository.kt - Repositorio de autenticación y gestión de usuarios.
 *
 * Encapsula la lógica de login (verificación de credenciales con SHA-256),
 * restauración de sesión desde SharedPreferences, y operaciones CRUD de usuarios
 * (creación, actualización, cambio de contraseña, activación/desactivación).
 */
package com.example.serviaux.repository

import com.example.serviaux.data.dao.UserDao
import com.example.serviaux.data.entity.User
import com.example.serviaux.data.entity.UserRole
import com.example.serviaux.util.SecurityUtils
import com.example.serviaux.util.SessionManager
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de autenticación y administración de usuarios.
 *
 * @property userDao DAO de acceso a la tabla de usuarios.
 * @property sessionManager Administrador de sesión para persistir el estado de login.
 */
class AuthRepository(
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) {
    val currentUser = sessionManager.currentUser

    /**
     * Intenta iniciar sesión verificando credenciales.
     * @return [Result.success] con el usuario si las credenciales son válidas,
     *         [Result.failure] con mensaje descriptivo en español si falla.
     */
    suspend fun login(username: String, password: String): Result<User> {
        val user = userDao.getByUsername(username)
            ?: return Result.failure(Exception("Usuario no encontrado"))
        if (!user.active) return Result.failure(Exception("Usuario desactivado"))
        if (!SecurityUtils.verifyPassword(password, user.passwordHash))
            return Result.failure(Exception("Contraseña incorrecta"))
        sessionManager.login(user)
        return Result.success(user)
    }

    fun logout() = sessionManager.logout()

    /** Intenta restaurar la sesión desde SharedPreferences; retorna false si no hay sesión válida. */
    suspend fun tryRestoreSession(): Boolean {
        val userId = sessionManager.savedUserId ?: return false
        val user = userDao.getByIdDirect(userId) ?: return false
        if (!user.active) {
            sessionManager.logout()
            return false
        }
        sessionManager.restoreSession(user)
        return true
    }

    fun getAllUsers(): Flow<List<User>> = userDao.getAll()
    fun getUserById(id: Long): Flow<User?> = userDao.getById(id)
    fun getMechanics(): Flow<List<User>> = userDao.getByRole(UserRole.MECANICO)

    suspend fun getUserByUsername(username: String): User? = userDao.getByUsername(username)

    suspend fun createUser(name: String, username: String, role: UserRole, password: String, commissionType: String = "NINGUNA", commissionValue: Double = 0.0): Result<Long> {
        val existing = userDao.getByUsername(username)
        if (existing != null) return Result.failure(Exception("El usuario ya existe"))
        val hash = SecurityUtils.hashPassword(password)
        val id = userDao.insert(User(name = name, username = username, role = role, passwordHash = hash, commissionType = commissionType, commissionValue = commissionValue))
        return Result.success(id)
    }

    suspend fun updateUser(user: User): Result<Unit> {
        userDao.update(user.copy(updatedAt = System.currentTimeMillis()))
        return Result.success(Unit)
    }

    suspend fun resetPassword(userId: Long, newPassword: String): Result<Unit> {
        val user = userDao.getByIdDirect(userId) ?: return Result.failure(Exception("Usuario no encontrado"))
        val hash = SecurityUtils.hashPassword(newPassword)
        userDao.update(user.copy(passwordHash = hash, updatedAt = System.currentTimeMillis()))
        return Result.success(Unit)
    }

    suspend fun toggleUserActive(userId: Long): Result<Unit> {
        val user = userDao.getByIdDirect(userId) ?: return Result.failure(Exception("Usuario no encontrado"))
        userDao.update(user.copy(active = !user.active, updatedAt = System.currentTimeMillis()))
        return Result.success(Unit)
    }
}
