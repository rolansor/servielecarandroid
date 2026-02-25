package com.example.serviaux.ui.customers

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviaux.ServiauxApp
import com.example.serviaux.data.entity.Customer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomerUiState(
    val customers: List<Customer> = emptyList(),
    val searchQuery: String = "",
    val selectedCustomer: Customer? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val formFullName: String = "",
    val formPhone: String = "",
    val formIdNumber: String = "",
    val formEmail: String = "",
    val formAddress: String = "",
    val formNotes: String = "",
    val formNameError: String? = null,
    val formPhoneError: String? = null,
    val formIdNumberError: String? = null,
    val formEmailError: String? = null,
    val isEditing: Boolean = false,
    val editingCustomerId: Long? = null,
    val savedSuccessfully: Boolean = false
)

class CustomerViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<ServiauxApp>()
    private val customerRepo get() = app.container.customerRepository

    private val _uiState = MutableStateFlow(CustomerUiState())
    val uiState: StateFlow<CustomerUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadAllCustomers()
    }

    private fun loadAllCustomers() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            customerRepo.getAll().collect { list ->
                _uiState.update { it.copy(customers = list) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.isBlank()) {
                customerRepo.getAll().collect { list ->
                    _uiState.update { it.copy(customers = list) }
                }
            } else {
                customerRepo.search(query).collect { list ->
                    _uiState.update { it.copy(customers = list) }
                }
            }
        }
    }

    fun loadCustomer(id: Long) {
        viewModelScope.launch {
            customerRepo.getById(id).collect { customer ->
                _uiState.update { it.copy(selectedCustomer = customer) }
            }
        }
    }

    fun onFormFullNameChange(value: String) {
        if (value.length <= 100) {
            _uiState.update { it.copy(formFullName = value, formNameError = null) }
        }
    }

    fun onFormPhoneChange(value: String) {
        if (value.length <= 15) {
            val filtered = value.filter { c -> c.isDigit() || c == '-' }
            _uiState.update { it.copy(formPhone = filtered, formPhoneError = null) }
        }
    }

    fun onFormIdNumberChange(value: String) {
        if (value.length <= 10) {
            val filtered = value.filter { c -> c.isDigit() }
            _uiState.update { it.copy(formIdNumber = filtered, formIdNumberError = null) }
        }
    }

    fun onFormEmailChange(value: String) {
        if (value.length <= 100) {
            _uiState.update { it.copy(formEmail = value, formEmailError = null) }
        }
    }

    fun onFormAddressChange(value: String) { _uiState.update { it.copy(formAddress = value) } }
    fun onFormNotesChange(value: String) { _uiState.update { it.copy(formNotes = value) } }

    private companion object {
        val NAME_REGEX = Regex("^[\\p{L} ]+$")
        val PHONE_REGEX = Regex("^[\\d-]+$")
        val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }

    private fun validateName(): String? {
        val trimmed = _uiState.value.formFullName.trim()
        return when {
            trimmed.isBlank() -> "El nombre es obligatorio"
            trimmed.length < 3 -> "M\u00ednimo 3 caracteres"
            !NAME_REGEX.matches(trimmed) -> "El nombre solo debe contener letras"
            else -> null
        }
    }

    private fun validatePhone(): String? {
        val trimmed = _uiState.value.formPhone.trim()
        return when {
            trimmed.isBlank() -> "Tel\u00e9fono es obligatorio"
            !PHONE_REGEX.matches(trimmed) -> "Formato de tel\u00e9fono inv\u00e1lido"
            trimmed.replace("-", "").length < 7 -> "M\u00ednimo 7 d\u00edgitos"
            else -> null
        }
    }

    private fun validateIdNumber(): String? {
        val trimmed = _uiState.value.formIdNumber.trim()
        if (trimmed.isBlank()) return null
        return when {
            !trimmed.all { it.isDigit() } -> "Solo n\u00fameros"
            trimmed.length != 10 -> "La c\u00e9dula debe tener 10 d\u00edgitos"
            else -> null
        }
    }

    private fun validateEmail(): String? {
        val trimmed = _uiState.value.formEmail.trim()
        if (trimmed.isBlank()) return null
        return if (!EMAIL_REGEX.matches(trimmed)) "Formato de correo inv\u00e1lido" else null
    }

    fun validateFieldOnFocusLost(field: String) {
        when (field) {
            "name" -> _uiState.update { it.copy(formNameError = validateName()) }
            "phone" -> _uiState.update { it.copy(formPhoneError = validatePhone()) }
            "idNumber" -> _uiState.update { it.copy(formIdNumberError = validateIdNumber()) }
            "email" -> _uiState.update { it.copy(formEmailError = validateEmail()) }
        }
    }

    private fun validateForm(): Boolean {
        val nameError = validateName()
        val phoneError = validatePhone()
        val idNumberError = validateIdNumber()
        val emailError = validateEmail()

        _uiState.update {
            it.copy(
                formNameError = nameError,
                formPhoneError = phoneError,
                formIdNumberError = idNumberError,
                formEmailError = emailError
            )
        }

        return nameError == null && phoneError == null && idNumberError == null && emailError == null
    }

    fun saveCustomer() {
        if (!validateForm()) return

        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                if (state.isEditing && state.editingCustomerId != null) {
                    val existing = customerRepo.getByIdDirect(state.editingCustomerId)
                    if (existing != null) {
                        customerRepo.update(
                            existing.copy(
                                fullName = state.formFullName.trim(),
                                phone = state.formPhone.trim(),
                                idNumber = state.formIdNumber.trim().ifBlank { null },
                                email = state.formEmail.trim().ifBlank { null },
                                address = state.formAddress.trim().ifBlank { null },
                                notes = state.formNotes.trim().ifBlank { null }
                            )
                        )
                    }
                } else {
                    customerRepo.insert(
                        Customer(
                            fullName = state.formFullName.trim(),
                            phone = state.formPhone.trim(),
                            idNumber = state.formIdNumber.trim().ifBlank { null },
                            email = state.formEmail.trim().ifBlank { null },
                            address = state.formAddress.trim().ifBlank { null },
                            notes = state.formNotes.trim().ifBlank { null }
                        )
                    )
                }
                _uiState.update { it.copy(isLoading = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al guardar") }
            }
        }
    }

    fun loadAndPrepareEdit(customerId: Long) {
        viewModelScope.launch {
            val customer = customerRepo.getByIdDirect(customerId)
            customer?.let { prepareEditCustomer(it) }
        }
    }

    fun prepareNewCustomer() {
        _uiState.update {
            it.copy(
                formFullName = "", formPhone = "", formIdNumber = "",
                formEmail = "", formAddress = "", formNotes = "",
                formNameError = null, formPhoneError = null,
                formIdNumberError = null, formEmailError = null,
                isEditing = false, editingCustomerId = null, error = null
            )
        }
    }

    fun prepareEditCustomer(customer: Customer) {
        _uiState.update {
            it.copy(
                formFullName = customer.fullName,
                formPhone = customer.phone,
                formIdNumber = customer.idNumber ?: "",
                formEmail = customer.email ?: "",
                formAddress = customer.address ?: "",
                formNotes = customer.notes ?: "",
                formNameError = null, formPhoneError = null,
                formIdNumberError = null, formEmailError = null,
                isEditing = true,
                editingCustomerId = customer.id,
                error = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
