/**
 * CustomerViewModel.kt - ViewModel del módulo de clientes.
 *
 * Gestiona la lista de clientes con búsqueda reactiva, el detalle
 * de un cliente específico (con sus vehículos) y el formulario de
 * creación/edición de clientes.
 */
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

/** Estado de la UI del módulo de clientes (lista, detalle y formulario). */
data class CustomerUiState(
    val customers: List<Customer> = emptyList(),
    val searchQuery: String = "",
    val currentPage: Int = 0,
    val totalCount: Int = 0,
    val pageSize: Int = 100,
    val selectedCustomer: Customer? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val formFullName: String = "",
    val formPhone: String = "",
    val formDocType: String = "CEDULA",
    val formIdNumber: String = "",
    val formEmail: String = "",
    val formAddress: String = "",
    val formNotes: String = "",
    val formNameError: String? = null,
    val formPhoneError: String? = null,
    val formIdNumberError: String? = null,
    val formEmailError: String? = null,
    val emailSuggestions: List<String> = emptyList(),
    val isEditing: Boolean = false,
    val editingCustomerId: Long? = null,
    val savedSuccessfully: Boolean = false,
    val isListLoaded: Boolean = false
)

class CustomerViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<ServiauxApp>()
    private val customerRepo get() = app.container.customerRepository

    private val _uiState = MutableStateFlow(CustomerUiState())
    val uiState: StateFlow<CustomerUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    companion object {
        val DOC_TYPES = listOf("CEDULA", "PASAPORTE", "EXTRANJERIA", "VISA", "OTRO")
        private val NAME_REGEX = Regex("^[\\p{L} ]+$")
        private val PHONE_REGEX = Regex("^[\\d-]+$")
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        private const val DEFAULT_EMAIL = "noreply@noreply.com"
        val EMAIL_DOMAINS = listOf("@gmail.com", "@hotmail.com", "@outlook.com", "@outlook.es", "@yahoo.com")
    }

    init {
        loadCustomersPage(0)
        loadTotalCount()
    }

    private fun loadTotalCount() {
        viewModelScope.launch {
            customerRepo.getTotalCount().collect { count ->
                _uiState.update { it.copy(totalCount = count) }
            }
        }
    }

    private fun loadCustomersPage(page: Int) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val offset = page * _uiState.value.pageSize
            customerRepo.getPaginated(_uiState.value.pageSize, offset).collect { list ->
                _uiState.update { it.copy(customers = list, currentPage = page, isListLoaded = true) }
            }
        }
    }

    fun nextPage() {
        val state = _uiState.value
        if ((state.currentPage + 1) * state.pageSize < state.totalCount) {
            _uiState.update { it.copy(searchQuery = "") }
            loadCustomersPage(state.currentPage + 1)
        }
    }

    fun previousPage() {
        val state = _uiState.value
        if (state.currentPage > 0) {
            _uiState.update { it.copy(searchQuery = "") }
            loadCustomersPage(state.currentPage - 1)
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.isBlank()) {
                val offset = _uiState.value.currentPage * _uiState.value.pageSize
                customerRepo.getPaginated(_uiState.value.pageSize, offset).collect { list ->
                    _uiState.update { it.copy(customers = list, isListLoaded = true) }
                }
            } else {
                customerRepo.search(query).collect { list ->
                    _uiState.update { it.copy(customers = list, isListLoaded = true) }
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
            _uiState.update { it.copy(formFullName = value.uppercase(), formNameError = null) }
        }
    }

    fun onFormPhoneChange(value: String) {
        if (value.length <= 15) {
            val filtered = value.filter { c -> c.isDigit() || c == '-' }
            _uiState.update { it.copy(formPhone = filtered, formPhoneError = null) }
        }
    }

    fun onFormDocTypeChange(value: String) {
        _uiState.update { it.copy(formDocType = value, formIdNumber = "", formIdNumberError = null) }
    }

    fun onFormIdNumberChange(value: String) {
        val docType = _uiState.value.formDocType
        val maxLen = if (docType == "CEDULA") 10 else 30
        if (value.length <= maxLen) {
            val filtered = if (docType == "CEDULA") {
                value.filter { it.isDigit() }
            } else {
                value.uppercase()
            }
            _uiState.update { it.copy(formIdNumber = filtered, formIdNumberError = null) }
        }
    }

    fun onFormEmailChange(value: String) {
        if (value.length <= 100) {
            val suggestions = if (value.isNotBlank() && !value.contains("@")) {
                EMAIL_DOMAINS.map { domain -> "$value$domain" }
            } else {
                emptyList()
            }
            _uiState.update { it.copy(formEmail = value, formEmailError = null, emailSuggestions = suggestions) }
        }
    }

    fun onEmailSuggestionSelected(email: String) {
        _uiState.update { it.copy(formEmail = email, emailSuggestions = emptyList(), formEmailError = null) }
    }

    fun onFormAddressChange(value: String) { _uiState.update { it.copy(formAddress = value.uppercase()) } }
    fun onFormNotesChange(value: String) { _uiState.update { it.copy(formNotes = value.uppercase()) } }

    private fun validateName(): String? {
        val trimmed = _uiState.value.formFullName.trim()
        return when {
            trimmed.isBlank() -> "El nombre es obligatorio"
            trimmed.length < 3 -> "Mínimo 3 caracteres"
            !NAME_REGEX.matches(trimmed) -> "El nombre solo debe contener letras"
            else -> null
        }
    }

    private fun validatePhone(): String? {
        val trimmed = _uiState.value.formPhone.trim()
        return when {
            trimmed.isBlank() -> "Teléfono es obligatorio"
            !PHONE_REGEX.matches(trimmed) -> "Formato de teléfono inválido"
            trimmed.replace("-", "").length < 7 -> "Mínimo 7 dígitos"
            else -> null
        }
    }

    private fun validateIdNumber(): String? {
        val trimmed = _uiState.value.formIdNumber.trim()
        if (trimmed.isBlank()) return null
        val docType = _uiState.value.formDocType
        return when (docType) {
            "CEDULA" -> validateCedulaEcuatoriana(trimmed)
            "PASAPORTE" -> if (trimmed.length < 5) "Mínimo 5 caracteres" else null
            else -> if (trimmed.length < 3) "Mínimo 3 caracteres" else null
        }
    }

    /**
     * Valida una cédula ecuatoriana según el algoritmo oficial del Registro Civil.
     * - 10 dígitos
     * - Primeros 2 dígitos: código de provincia (01-24)
     * - Tercer dígito: menor a 6
     * - Dígito verificador (módulo 10)
     */
    private fun validateCedulaEcuatoriana(cedula: String): String? {
        if (!cedula.all { it.isDigit() }) return "Solo números"
        if (cedula.length != 10) return "La cédula debe tener 10 dígitos"

        val provincia = cedula.substring(0, 2).toInt()
        if (provincia < 1 || provincia > 24) return "Código de provincia inválido"

        val tercerDigito = cedula[2].digitToInt()
        if (tercerDigito >= 6) return "Tercer dígito inválido"

        // Algoritmo módulo 10
        val coeficientes = intArrayOf(2, 1, 2, 1, 2, 1, 2, 1, 2)
        var suma = 0
        for (i in 0 until 9) {
            var valor = cedula[i].digitToInt() * coeficientes[i]
            if (valor > 9) valor -= 9
            suma += valor
        }
        val digitoVerificador = if (suma % 10 == 0) 0 else 10 - (suma % 10)
        val ultimoDigito = cedula[9].digitToInt()

        return if (digitoVerificador != ultimoDigito) "Cédula inválida" else null
    }

    private fun validateEmail(): String? {
        val trimmed = _uiState.value.formEmail.trim()
        if (trimmed.isBlank()) return null
        return if (!EMAIL_REGEX.matches(trimmed)) "Formato de correo inválido" else null
    }

    fun validateFieldOnFocusLost(field: String) {
        when (field) {
            "name" -> _uiState.update { it.copy(formNameError = validateName()) }
            "phone" -> _uiState.update { it.copy(formPhoneError = validatePhone()) }
            "idNumber" -> {
                val formatError = validateIdNumber()
                _uiState.update { it.copy(formIdNumberError = formatError) }
                if (formatError == null) checkDuplicateDocument()
            }
            "email" -> _uiState.update { it.copy(formEmailError = validateEmail()) }
        }
    }

    private fun checkDuplicateDocument() {
        val state = _uiState.value
        val idNumber = state.formIdNumber.trim()
        if (idNumber.isBlank()) return
        viewModelScope.launch {
            val existing = customerRepo.findByDocument(state.formDocType, idNumber)
            if (existing != null && existing.id != state.editingCustomerId) {
                _uiState.update { it.copy(formIdNumberError = "Ya existe un cliente con este documento: ${existing.fullName}") }
            }
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
        val emailToSave = state.formEmail.trim().ifBlank { DEFAULT_EMAIL }

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
                                docType = state.formDocType,
                                idNumber = state.formIdNumber.trim().ifBlank { null },
                                email = emailToSave,
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
                            docType = state.formDocType,
                            idNumber = state.formIdNumber.trim().ifBlank { null },
                            email = emailToSave,
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
                formFullName = "", formPhone = "", formDocType = "CEDULA",
                formIdNumber = "", formEmail = "", formAddress = "", formNotes = "",
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
                formPhone = customer.phone ?: "",
                formDocType = customer.docType,
                formIdNumber = customer.idNumber ?: "",
                formEmail = if (customer.email == DEFAULT_EMAIL) "" else customer.email ?: "",
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

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            if (customerRepo.canDelete(customer.id)) {
                customerRepo.delete(customer)
            } else {
                _uiState.update { it.copy(error = "No se puede eliminar: tiene veh\u00edculos u \u00f3rdenes asociadas") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
