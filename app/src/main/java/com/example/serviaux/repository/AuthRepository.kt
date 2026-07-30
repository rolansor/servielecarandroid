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
        // Migración transparente de hashes antiguos (SHA-256 de una pasada) a PBKDF2. Este es el
        // único momento en que se conoce la contraseña en claro y se puede recalcular.
        if (SecurityUtils.needsRehash(user.passwordHash)) {
            runCatching {
                userDao.update(
                    user.copy(
                        passwordHash = SecurityUtils.hashPassword(password),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
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

    /** Lectura puntual de un usuario; útil cuando no se quiere observar cambios. */
    suspend fun getUserByIdDirect(id: Long): User? = userDao.getByIdDirect(id)

    suspend fun createUser(name: String, username: String, role: UserRole, password: String, commissionType: String = "NINGUNA", commissionValue: Double = 0.0): Result<Long> {
        // Incluye cuentas desactivadas: el índice único de la tabla no distingue por estado.
        val existing = userDao.findByUsernameIncludingInactive(username)
        if (existing != null) return Result.failure(Exception("El usuario ya existe"))
        val hash = SecurityUtils.hashPassword(password)
        val id = userDao.insert(User(name = name, username = username, role = role, passwordHash = hash, commissionType = commissionType, commissionValue = commissionValue))
        return Result.success(id)
    }

    /**
     * Actualiza un usuario validando que el nombre de usuario siga siendo único.
     *
     * `createUser` ya lo validaba, pero aquí faltaba: renombrar un usuario a uno existente
     * llegaba al índice único de la tabla y fallaba con el error crudo de SQLite.
     */
    suspend fun updateUser(user: User): Result<Unit> {
        val existing = userDao.findByUsernameIncludingInactive(user.username)
        if (existing != null && existing.id != user.id) {
            return Result.failure(Exception("El usuario '${user.username}' ya existe"))
        }
        // No se puede degradar al último administrador: nadie podría volver a administrar el
        // sistema, y no hay pantalla para recuperarlo.
        val previous = userDao.getByIdDirect(user.id)
        val dropsAdmin = previous?.role == UserRole.ADMIN && previous.active &&
            (user.role != UserRole.ADMIN || !user.active)
        if (dropsAdmin && userDao.countActiveAdmins() <= 1) {
            return Result.failure(Exception("Debe quedar al menos un administrador activo"))
        }
        userDao.update(user.copy(updatedAt = System.currentTimeMillis()))
        return Result.success(Unit)
    }

    suspend fun resetPassword(userId: Long, newPassword: String): Result<Unit> {
        val user = userDao.getByIdDirect(userId) ?: return Result.failure(Exception("Usuario no encontrado"))
        val hash = SecurityUtils.hashPassword(newPassword)
        userDao.update(user.copy(passwordHash = hash, updatedAt = System.currentTimeMillis()))
        return Result.success(Unit)
    }

    /**
     * Activa o desactiva una cuenta.
     *
     * Dos barreras contra quedarse fuera del sistema: no se puede desactivar la propia cuenta en
     * uso, ni al último administrador activo. Antes cualquiera de las dos dejaba el taller sin
     * ningún acceso administrativo y la única salida era reinstalar o restaurar un respaldo.
     */
    suspend fun toggleUserActive(userId: Long): Result<Unit> {
        val user = userDao.getByIdDirect(userId) ?: return Result.failure(Exception("Usuario no encontrado"))
        if (user.active) {
            if (userId == sessionManager.currentUserId) {
                return Result.failure(Exception("No puede desactivar la cuenta con la que está trabajando"))
            }
            if (user.role == UserRole.ADMIN && userDao.countActiveAdmins() <= 1) {
                return Result.failure(Exception("Debe quedar al menos un administrador activo"))
            }
        }
        userDao.update(user.copy(active = !user.active, updatedAt = System.currentTimeMillis()))
        return Result.success(Unit)
    }
}
