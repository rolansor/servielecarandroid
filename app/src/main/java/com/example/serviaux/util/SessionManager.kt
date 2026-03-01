/**
 * SessionManager.kt - Gestión de sesión del usuario actual.
 *
 * Persiste el ID del usuario en SharedPreferences para permitir
 * restauración de sesión (login biométrico). Expone el usuario actual
 * como [StateFlow] para que los ViewModels reaccionen a cambios de sesión.
 *
 * También centraliza las verificaciones de permisos basadas en el rol del usuario.
 */
package com.example.serviaux.util

import android.content.Context
import android.content.SharedPreferences
import com.example.serviaux.data.entity.User
import com.example.serviaux.data.entity.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Administrador de sesión del usuario.
 *
 * Se instancia una vez en [AppContainer] y se inyecta en los repositorios y ViewModels.
 * La sesión persiste el [User.id] en SharedPreferences (`serviaux_session`).
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("serviaux_session", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    /** ID del usuario guardado en SharedPreferences; null si no hay sesión previa. */
    val savedUserId: Long?
        get() {
            val id = prefs.getLong("user_id", -1L)
            return if (id == -1L) null else id
        }

    val hasSavedSession: Boolean
        get() = savedUserId != null

    fun login(user: User) {
        _currentUser.value = user
        prefs.edit().putLong("user_id", user.id).apply()
    }

    fun logout() {
        _currentUser.value = null
        prefs.edit().remove("user_id").apply()
    }

    fun restoreSession(user: User) {
        _currentUser.value = user
    }

    val isLoggedIn: Boolean get() = _currentUser.value != null
    val currentUserId: Long get() = _currentUser.value?.id ?: 0
    val currentUserRole: UserRole get() = _currentUser.value?.role ?: UserRole.MECANICO

    // ── Verificaciones de permisos ─────────────────────────────────────

    fun hasRole(vararg roles: UserRole): Boolean = _currentUser.value?.role in roles
    fun isAdmin(): Boolean = hasRole(UserRole.ADMIN)
    fun canManageUsers(): Boolean = isAdmin()
    fun canCreateOrders(): Boolean = hasRole(UserRole.ADMIN, UserRole.RECEPCIONISTA)
    fun canDeleteOrders(): Boolean = isAdmin()
}
