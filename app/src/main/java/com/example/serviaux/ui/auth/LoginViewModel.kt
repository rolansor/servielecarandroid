/**
 * LoginViewModel.kt - ViewModel de la pantalla de inicio de sesión.
 *
 * Gestiona el flujo de autenticación:
 * 1. Verifica si hay sesión guardada -> solicita autenticación biométrica.
 * 2. Si no hay sesión o falla la biometría -> muestra formulario de usuario/contraseña.
 * 3. Valida credenciales contra [AuthRepository] con hash SHA-256.
 */
package com.example.serviaux.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviaux.ServiauxApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de la UI de la pantalla de login.
 *
 * @property isCheckingSession True mientras se verifica si hay sesión previa.
 * @property needsBiometric True si se requiere autenticación biométrica para restaurar sesión.
 * @property isLoggedIn True tras un login exitoso; dispara la navegación al dashboard.
 */
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val error: String? = null,
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isCheckingSession: Boolean = true,
    val needsBiometric: Boolean = false
)

/** ViewModel de la pantalla de login con soporte biométrico. */
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<ServiauxApp>()
    private val authRepo get() = app.container.authRepository
    private val session get() = app.container.sessionManager

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        checkSavedSession()
    }

    private fun checkSavedSession() {
        if (session.hasSavedSession) {
            // Has saved session → need biometric to unlock
            _uiState.update {
                it.copy(isCheckingSession = false, needsBiometric = true)
            }
        } else {
            // No saved session → show login form
            _uiState.update { it.copy(isCheckingSession = false) }
        }
    }

    fun onBiometricSuccess() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val restored = authRepo.tryRestoreSession()
                if (restored) {
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                } else {
                    // User was deleted/deactivated, clear and show form
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            needsBiometric = false,
                            error = "La sesión expiró. Inicie sesión nuevamente."
                        )
                    }
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, needsBiometric = false)
                }
            }
        }
    }

    fun onBiometricFailed() {
        // Show regular login form
        _uiState.update { it.copy(needsBiometric = false) }
    }

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun login() {
        val state = _uiState.value
        if (state.username.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "Ingrese usuario y contraseña") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val result = authRepo.login(state.username.trim(), state.password)
                result.fold(
                    onSuccess = {
                        _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error desconocido") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
