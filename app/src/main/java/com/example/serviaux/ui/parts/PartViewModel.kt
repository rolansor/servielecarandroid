package com.example.serviaux.ui.parts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviaux.ServiauxApp
import com.example.serviaux.data.entity.Part
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PartUiState(
    val parts: List<Part> = emptyList(),
    val searchQuery: String = "",
    val selectedPart: Part? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val availablePartBrands: List<String> = emptyList(),
    val formName: String = "",
    val formCode: String = "",
    val formBrand: String = "",
    val formUnitCost: String = "",
    val formSalePrice: String = "",
    val formCurrentStock: String = "",
    val formActive: Boolean = true,
    val isEditing: Boolean = false,
    val editingPartId: Long? = null,
    val savedSuccessfully: Boolean = false,
    val formNameError: String? = null,
    val formCodeError: String? = null,
    val formBrandError: String? = null,
    val formUnitCostError: String? = null,
    val formSalePriceError: String? = null,
    val formCurrentStockError: String? = null
)

class PartViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<ServiauxApp>()
    private val partRepo get() = app.container.partRepository
    private val catalogRepo get() = app.container.catalogRepository

    private val _uiState = MutableStateFlow(PartUiState())
    val uiState: StateFlow<PartUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadAllParts()
        loadPartBrands()
    }

    private fun loadPartBrands() {
        viewModelScope.launch {
            catalogRepo.getAllPartBrands().collect { brands ->
                _uiState.update { it.copy(availablePartBrands = brands.map { b -> b.name }) }
            }
        }
    }

    private fun loadAllParts() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            partRepo.getAll().collect { list ->
                _uiState.update { it.copy(parts = list) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.isBlank()) {
                partRepo.getAll().collect { list ->
                    _uiState.update { it.copy(parts = list) }
                }
            } else {
                partRepo.search(query).collect { list ->
                    _uiState.update { it.copy(parts = list) }
                }
            }
        }
    }

    fun onFormNameChange(value: String) {
        if (value.length <= 100) {
            _uiState.update { it.copy(formName = value, formNameError = null) }
        }
    }

    fun onFormCodeChange(value: String) {
        if (value.length <= 5 && value.all { it.isDigit() }) {
            _uiState.update { it.copy(formCode = value, formCodeError = null) }
        }
    }

    fun onFormBrandChange(value: String) {
        _uiState.update { it.copy(formBrand = value, formBrandError = null) }
    }

    fun onFormUnitCostChange(value: String) {
        if (value.length <= 10) {
            _uiState.update { it.copy(formUnitCost = value, formUnitCostError = null) }
        }
    }

    fun onFormSalePriceChange(value: String) {
        if (value.length <= 10) {
            _uiState.update { it.copy(formSalePrice = value, formSalePriceError = null) }
        }
    }

    fun onFormCurrentStockChange(value: String) {
        if (value.length <= 5) {
            _uiState.update { it.copy(formCurrentStock = value, formCurrentStockError = null) }
        }
    }

    fun onFormActiveChange(value: Boolean) { _uiState.update { it.copy(formActive = value) } }

    private val nameRegex = Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ0-9 \\-()/ ]*$")
    private val codeRegex = Regex("^[0-9]{1,5}$")

    private fun validateName(): String? {
        val trimmed = _uiState.value.formName.trim()
        return when {
            trimmed.isBlank() -> "El nombre es obligatorio"
            trimmed.length < 2 -> "Mínimo 2 caracteres"
            !nameRegex.matches(trimmed) -> "Solo letras, números, espacios, guiones, paréntesis y barras"
            else -> null
        }
    }

    private fun validateCode(): String? {
        val trimmed = _uiState.value.formCode.trim()
        return if (trimmed.isNotEmpty() && !codeRegex.matches(trimmed)) "Solo números, máximo 5 dígitos" else null
    }

    private fun validateUnitCost(): String? {
        val value = _uiState.value.formUnitCost
        if (value.isNotBlank()) {
            val cost = value.toDoubleOrNull()
            if (cost == null || cost < 0) return "Debe ser un número válido"
        }
        return null
    }

    private fun validateSalePrice(): String? {
        val state = _uiState.value
        if (state.formSalePrice.isNotBlank()) {
            val price = state.formSalePrice.toDoubleOrNull()
            if (price == null || price < 0) return "Debe ser un número válido"
            if (state.formUnitCost.isNotBlank()) {
                val cost = state.formUnitCost.toDoubleOrNull()
                if (cost != null && price < cost) return "El precio de venta es menor al costo unitario"
            }
        }
        return null
    }

    private fun validateCurrentStock(): String? {
        val value = _uiState.value.formCurrentStock
        if (value.isNotBlank()) {
            val stock = value.toIntOrNull()
            if (stock == null || stock < 0) return "Debe ser un número entero válido"
        }
        return null
    }

    fun validateFieldOnFocusLost(field: String) {
        when (field) {
            "name" -> _uiState.update { it.copy(formNameError = validateName()) }
            "code" -> _uiState.update { it.copy(formCodeError = validateCode()) }
            "unitCost" -> _uiState.update { it.copy(formUnitCostError = validateUnitCost()) }
            "salePrice" -> _uiState.update { it.copy(formSalePriceError = validateSalePrice()) }
            "currentStock" -> _uiState.update { it.copy(formCurrentStockError = validateCurrentStock()) }
        }
    }

    private fun validateForm(): Boolean {
        val nameError = validateName()
        val codeError = validateCode()
        val brandError: String? = null
        val unitCostError = validateUnitCost()
        val salePriceError = validateSalePrice()
        val currentStockError = validateCurrentStock()

        _uiState.update {
            it.copy(
                formNameError = nameError,
                formCodeError = codeError,
                formBrandError = brandError,
                formUnitCostError = unitCostError,
                formSalePriceError = salePriceError,
                formCurrentStockError = currentStockError
            )
        }

        return nameError == null && codeError == null && brandError == null &&
                unitCostError == null && salePriceError == null && currentStockError == null
    }

    fun savePart() {
        if (!validateForm()) return

        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val salePrice = state.formSalePrice.toDoubleOrNull()
                if (state.isEditing && state.editingPartId != null) {
                    val existing = partRepo.getByIdDirect(state.editingPartId)
                    if (existing != null) {
                        partRepo.update(
                            existing.copy(
                                name = state.formName.trim(),
                                code = state.formCode.trim().ifBlank { null },
                                brand = state.formBrand.trim().ifBlank { null },
                                unitCost = state.formUnitCost.toDoubleOrNull() ?: 0.0,
                                salePrice = salePrice,
                                currentStock = state.formCurrentStock.toIntOrNull() ?: 0,
                                active = state.formActive
                            )
                        )
                    }
                } else {
                    partRepo.insert(
                        Part(
                            name = state.formName.trim(),
                            code = state.formCode.trim().ifBlank { null },
                            brand = state.formBrand.trim().ifBlank { null },
                            unitCost = state.formUnitCost.toDoubleOrNull() ?: 0.0,
                            salePrice = salePrice,
                            currentStock = state.formCurrentStock.toIntOrNull() ?: 0,
                            active = state.formActive
                        )
                    )
                }
                _uiState.update { it.copy(isLoading = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al guardar") }
            }
        }
    }

    fun loadAndPrepareEdit(partId: Long) {
        viewModelScope.launch {
            partRepo.getById(partId).collect { part ->
                part?.let { prepareEdit(it) }
            }
        }
    }

    fun prepareNew() {
        _uiState.update {
            it.copy(
                formName = "", formCode = "", formBrand = "",
                formUnitCost = "", formSalePrice = "", formCurrentStock = "",
                formActive = true, isEditing = false, editingPartId = null, error = null,
                formNameError = null, formCodeError = null, formBrandError = null,
                formUnitCostError = null, formSalePriceError = null, formCurrentStockError = null
            )
        }
    }

    fun prepareEdit(part: Part) {
        _uiState.update {
            it.copy(
                formName = part.name,
                formCode = part.code ?: "",
                formBrand = part.brand ?: "",
                formUnitCost = part.unitCost.toString(),
                formSalePrice = part.salePrice?.toString() ?: "",
                formCurrentStock = part.currentStock.toString(),
                formActive = part.active,
                isEditing = true,
                editingPartId = part.id,
                selectedPart = part,
                error = null,
                formNameError = null, formCodeError = null, formBrandError = null,
                formUnitCostError = null, formSalePriceError = null, formCurrentStockError = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
