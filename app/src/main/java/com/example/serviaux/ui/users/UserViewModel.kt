/**
 * UserViewModel.kt - ViewModel del módulo de administración de usuarios.
 *
 * Solo accesible para administradores. Gestiona la lista de usuarios,
 * la creación de nuevos usuarios con rol y contraseña, la edición de datos,
 * el cambio de contraseña y la activación/desactivación de cuentas.
 */
package com.example.serviaux.ui.users

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviaux.ServiauxApp
import com.example.serviaux.data.entity.CommissionType
import com.example.serviaux.data.entity.User
import com.example.serviaux.data.entity.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado de la UI del módulo de usuarios (lista y formulario). */
data class UserUiState(
    val users: List<User> = emptyList(),
    val selectedUser: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val formName: String = "",
    val formUsername: String = "",
    val formRole: UserRole = UserRole.MECANICO,
    val formCommissionType: CommissionType = CommissionType.NINGUNA,
    val formCommissionValue: String = "",
    val formCommissionValueError: String? = null,
    val formPassword: String = "",
    val isEditing: Boolean = false,
    val editingUserId: Long? = null,
    val newPassword: String = "",
    val savedSuccessfully: Boolean = false,
    val formNameError: String? = null,
    val formUsernameError: String? = null,
    val formPasswordError: String? = null,
    val newPasswordError: String? = null,
    val isListLoaded: Boolean = false
)

class UserViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<ServiauxApp>()
    private val authRepo get() = app.container.authRepository

    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepo.getAllUsers().collect { list ->
                _uiState.update { it.copy(users = list, isListLoaded = true) }
            }
        }
    }

    fun onFormNameChange(value: String) {
        if (value.length <= 80) {
            _uiState.update { it.copy(formName = value, formNameError = null) }
        }
    }

    fun onFormUsernameChange(value: String) {
        if (value.length <= 30) {
            _uiState.update { it.copy(formUsername = value.lowercase(), formUsernameError = null) }
        }
    }

    fun onFormRoleChange(value: UserRole) {
        _uiState.update {
            if (value != UserRole.MECANICO) {
                it.copy(formRole = value, formCommissionType = CommissionType.NINGUNA, formCommissionValue = "", formCommissionValueError = null)
            } else {
                it.copy(formRole = value)
            }
        }
    }

    fun onFormCommissionTypeChange(value: CommissionType) {
        _uiState.update {
            it.copy(
                formCommissionType = value,
                formCommissionValue = if (value == CommissionType.NINGUNA) "" else it.formCommissionValue,
                formCommissionValueError = null
            )
        }
    }

    fun onFormCommissionValueChange(value: String) {
        if (value.length <= 10 && value.all { it.isDigit() || it == '.' }) {
            _uiState.update { it.copy(formCommissionValue = value, formCommissionValueError = null) }
        }
    }

    fun onFormPasswordChange(value: String) {
        if (value.length <= 50) {
            _uiState.update { it.copy(formPassword = value, formPasswordError = null) }
        }
    }

    fun onNewPasswordChange(value: String) {
        _uiState.update { it.copy(newPassword = value, newPasswordError = null) }
    }

    private val nameRegex = Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ ]+$")
    private val usernameRegex = Regex("^[a-z0-9_]+$")

    private fun validateName(): String? {
        val trimmed = _uiState.value.formName.trim()
        return when {
            trimmed.isBlank() -> "El nombre es obligatorio"
            trimmed.length < 3 -> "Mínimo 3 caracteres"
            !nameRegex.matches(trimmed) -> "Solo letras y espacios"
            else -> null
        }
    }

    private fun validateUsername(): String? {
        val trimmed = _uiState.value.formUsername.trim()
        return when {
            trimmed.isBlank() -> "El usuario es obligatorio"
            trimmed.length < 3 -> "Mínimo 3 caracteres"
            !usernameRegex.matches(trimmed) -> "Solo letras, números y guiones bajos"
            else -> null
        }
    }

    private fun validatePassword(): String? {
        return if (!_uiState.value.isEditing) {
            when {
                _uiState.value.formPassword.isBlank() -> "La contraseña es obligatoria"
                _uiState.value.formPassword.length < 6 -> "Mínimo 6 caracteres"
                else -> null
            }
        } else null
    }

    private fun validateCommissionValue(): String? {
        val state = _uiState.value
        if (state.formRole == UserRole.MECANICO && state.formCommissionType != CommissionType.NINGUNA) {
            val value = state.formCommissionValue.toDoubleOrNull()
            return when {
                value == null || value <= 0 -> "Ingrese un valor mayor a 0"
                else -> null
            }
        }
        return null
    }

    fun validateFieldOnFocusLost(field: String) {
        when (field) {
            "name" -> _uiState.update { it.copy(formNameError = validateName()) }
            "username" -> {
                val formatError = validateUsername()
                _uiState.update { it.copy(formUsernameError = formatError) }
                if (formatError == null) checkDuplicateUsername()
            }
            "password" -> _uiState.update { it.copy(formPasswordError = validatePassword()) }
            "commissionValue" -> _uiState.update { it.copy(formCommissionValueError = validateCommissionValue()) }
        }
    }

    private fun checkDuplicateUsername() {
        val username = _uiState.value.formUsername.trim()
        if (username.isBlank()) return
        viewModelScope.launch {
            val existing = authRepo.getUserByUsername(username)
            if (existing != null && existing.id != _uiState.value.editingUserId) {
                _uiState.update { it.copy(formUsernameError = "Este usuario ya existe") }
            }
        }
    }

    private fun validateForm(): Boolean {
        val nameError = validateName()
        val usernameError = validateUsername()
        val passwordError = validatePassword()
        val commissionError = validateCommissionValue()

        _uiState.update {
            it.copy(
                formNameError = nameError,
                formUsernameError = usernameError,
                formPasswordError = passwordError,
                formCommissionValueError = commissionError
            )
        }

        return nameError == null && usernameError == null && passwordError == null && commissionError == null
    }

    fun saveUser() {
        if (!validateForm()) return

        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                if (state.isEditing && state.editingUserId != null) {
                    val existing = _uiState.value.selectedUser
                    if (existing != null) {
                        val result = authRepo.updateUser(
                            existing.copy(
                                name = state.formName.trim(),
                                username = state.formUsername.trim(),
                                role = state.formRole,
                                commissionType = if (state.formRole == UserRole.MECANICO) state.formCommissionType.name else "NINGUNA",
                                commissionValue = if (state.formRole == UserRole.MECANICO && state.formCommissionType != CommissionType.NINGUNA) state.formCommissionValue.toDoubleOrNull() ?: 0.0 else 0.0
                            )
                        )
                        result.onFailure { e ->
                            _uiState.update { it.copy(isLoading = false, error = e.message) }
                            return@launch
                        }
                    }
                } else {
                    val commType = if (state.formRole == UserRole.MECANICO) state.formCommissionType.name else "NINGUNA"
                    val commValue = if (state.formRole == UserRole.MECANICO && state.formCommissionType != CommissionType.NINGUNA) state.formCommissionValue.toDoubleOrNull() ?: 0.0 else 0.0
                    val result = authRepo.createUser(
                        name = state.formName.trim(),
                        username = state.formUsername.trim(),
                        role = state.formRole,
                        password = state.formPassword,
                        commissionType = commType,
                        commissionValue = commValue
                    )
                    result.onFailure { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                        return@launch
                    }
                }
                _uiState.update { it.copy(isLoading = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al guardar") }
            }
        }
    }

    fun loadAndPrepareEdit(userId: Long) {
        viewModelScope.launch {
            authRepo.getUserById(userId).collect { user ->
                user?.let { prepareEdit(it) }
            }
        }
    }

    fun prepareNew() {
        _uiState.update {
            it.copy(
                formName = "", formUsername = "", formRole = UserRole.MECANICO,
                formCommissionType = CommissionType.NINGUNA, formCommissionValue = "", formCommissionValueError = null,
                formPassword = "", isEditing = false, editingUserId = null,
                selectedUser = null, error = null,
                formNameError = null, formUsernameError = null,
                formPasswordError = null, newPasswordError = null
            )
        }
    }

    fun prepareEdit(user: User) {
        val commType = try { CommissionType.valueOf(user.commissionType) } catch (_: Exception) { CommissionType.NINGUNA }
        _uiState.update {
            it.copy(
                formName = user.name,
                formUsername = user.username,
                formRole = user.role,
                formCommissionType = commType,
                formCommissionValue = if (commType != CommissionType.NINGUNA && user.commissionValue > 0) user.commissionValue.toString() else "",
                formCommissionValueError = null,
                formPassword = "",
                isEditing = true,
                editingUserId = user.id,
                selectedUser = user,
                error = null,
                formNameError = null, formUsernameError = null,
                formPasswordError = null, newPasswordError = null
            )
        }
    }

    fun toggleActive(userId: Long) {
        viewModelScope.launch {
            try {
                val result = authRepo.toggleUserActive(userId)
                result.onFailure { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al cambiar estado") }
            }
        }
    }

    fun resetPassword(userId: Long) {
        val newPass = _uiState.value.newPassword
        if (newPass.isBlank()) {
            _uiState.update { it.copy(newPasswordError = "Ingrese nueva contraseña") }
            return
        }
        if (newPass.length < 6) {
            _uiState.update { it.copy(newPasswordError = "Mínimo 6 caracteres") }
            return
        }
        viewModelScope.launch {
            try {
                val result = authRepo.resetPassword(userId, newPass)
                result.fold(
                    onSuccess = { _uiState.update { it.copy(newPassword = "") } },
                    onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al resetear contraseña") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
