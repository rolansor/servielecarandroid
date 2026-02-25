package com.example.serviaux.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviaux.ServiauxApp
import com.example.serviaux.data.entity.CatalogBrand
import com.example.serviaux.data.entity.CatalogModel
import com.example.serviaux.data.entity.CatalogColor
import com.example.serviaux.data.entity.CatalogPartBrand
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CatalogUiState(
    val brands: List<CatalogBrand> = emptyList(),
    val models: List<CatalogModel> = emptyList(),
    val colors: List<CatalogColor> = emptyList(),
    val partBrands: List<CatalogPartBrand> = emptyList(),
    val selectedBrandId: Long? = null,
    val selectedBrandName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val exportJson: String? = null,
    val dialogState: CatalogDialogState = CatalogDialogState.None
)

sealed class CatalogDialogState {
    data object None : CatalogDialogState()
    data class AddBrand(val name: String = "") : CatalogDialogState()
    data class EditBrand(val brand: CatalogBrand, val name: String) : CatalogDialogState()
    data class AddModel(val brandId: Long, val name: String = "") : CatalogDialogState()
    data class EditModel(val model: CatalogModel, val name: String) : CatalogDialogState()
    data class AddColor(val name: String = "") : CatalogDialogState()
    data class EditColor(val color: CatalogColor, val name: String) : CatalogDialogState()
    data class AddPartBrand(val name: String = "") : CatalogDialogState()
    data class EditPartBrand(val partBrand: CatalogPartBrand, val name: String) : CatalogDialogState()
    data class ConfirmDelete(val type: String, val id: Long, val name: String) : CatalogDialogState()
    data class ImportDialog(val jsonText: String = "") : CatalogDialogState()
}

class CatalogViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<ServiauxApp>()
    private val catalogRepo get() = app.container.catalogRepository

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    private var modelsJob: Job? = null

    init {
        loadBrands()
        loadColors()
        loadPartBrands()
    }

    private fun loadBrands() {
        viewModelScope.launch {
            catalogRepo.getAllBrands().collect { brands ->
                _uiState.update { it.copy(brands = brands) }
            }
        }
    }

    private fun loadColors() {
        viewModelScope.launch {
            catalogRepo.getAllColors().collect { colors ->
                _uiState.update { it.copy(colors = colors) }
            }
        }
    }

    private fun loadPartBrands() {
        viewModelScope.launch {
            catalogRepo.getAllPartBrands().collect { partBrands ->
                _uiState.update { it.copy(partBrands = partBrands) }
            }
        }
    }

    fun selectBrand(brand: CatalogBrand?) {
        _uiState.update {
            it.copy(
                selectedBrandId = brand?.id,
                selectedBrandName = brand?.name ?: "",
                models = emptyList()
            )
        }
        modelsJob?.cancel()
        if (brand != null) {
            modelsJob = viewModelScope.launch {
                catalogRepo.getModelsByBrand(brand.id).collect { models ->
                    _uiState.update { it.copy(models = models) }
                }
            }
        }
    }

    // Dialog management
    fun showAddBrandDialog() {
        _uiState.update { it.copy(dialogState = CatalogDialogState.AddBrand()) }
    }

    fun showEditBrandDialog(brand: CatalogBrand) {
        _uiState.update { it.copy(dialogState = CatalogDialogState.EditBrand(brand, brand.name)) }
    }

    fun showAddModelDialog(brandId: Long) {
        _uiState.update { it.copy(dialogState = CatalogDialogState.AddModel(brandId)) }
    }

    fun showEditModelDialog(model: CatalogModel) {
        _uiState.update { it.copy(dialogState = CatalogDialogState.EditModel(model, model.name)) }
    }

    fun showAddColorDialog() {
        _uiState.update { it.copy(dialogState = CatalogDialogState.AddColor()) }
    }

    fun showEditColorDialog(color: CatalogColor) {
        _uiState.update { it.copy(dialogState = CatalogDialogState.EditColor(color, color.name)) }
    }

    fun showAddPartBrandDialog() {
        _uiState.update { it.copy(dialogState = CatalogDialogState.AddPartBrand()) }
    }

    fun showEditPartBrandDialog(partBrand: CatalogPartBrand) {
        _uiState.update { it.copy(dialogState = CatalogDialogState.EditPartBrand(partBrand, partBrand.name)) }
    }

    fun showDeleteConfirmation(type: String, id: Long, name: String) {
        _uiState.update { it.copy(dialogState = CatalogDialogState.ConfirmDelete(type, id, name)) }
    }

    fun showImportDialog() {
        _uiState.update { it.copy(dialogState = CatalogDialogState.ImportDialog()) }
    }

    fun dismissDialog() {
        _uiState.update { it.copy(dialogState = CatalogDialogState.None) }
    }

    fun updateDialogText(text: String) {
        _uiState.update { state ->
            val newDialog = when (val d = state.dialogState) {
                is CatalogDialogState.AddBrand -> d.copy(name = text)
                is CatalogDialogState.EditBrand -> d.copy(name = text)
                is CatalogDialogState.AddModel -> d.copy(name = text)
                is CatalogDialogState.EditModel -> d.copy(name = text)
                is CatalogDialogState.AddColor -> d.copy(name = text)
                is CatalogDialogState.EditColor -> d.copy(name = text)
                is CatalogDialogState.AddPartBrand -> d.copy(name = text)
                is CatalogDialogState.EditPartBrand -> d.copy(name = text)
                is CatalogDialogState.ImportDialog -> d.copy(jsonText = text)
                else -> d
            }
            state.copy(dialogState = newDialog)
        }
    }

    // CRUD operations
    fun confirmAddBrand(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                catalogRepo.insertBrand(name.trim())
                _uiState.update { it.copy(dialogState = CatalogDialogState.None, successMessage = "Marca agregada") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al agregar marca") }
            }
        }
    }

    fun confirmEditBrand(brand: CatalogBrand, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            try {
                catalogRepo.updateBrand(brand.copy(name = newName.trim()))
                _uiState.update { it.copy(dialogState = CatalogDialogState.None, successMessage = "Marca actualizada") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al actualizar marca") }
            }
        }
    }

    fun confirmAddModel(brandId: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                catalogRepo.insertModel(brandId, name.trim())
                _uiState.update { it.copy(dialogState = CatalogDialogState.None, successMessage = "Modelo agregado") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al agregar modelo") }
            }
        }
    }

    fun confirmEditModel(model: CatalogModel, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            try {
                catalogRepo.updateModel(model.copy(name = newName.trim()))
                _uiState.update { it.copy(dialogState = CatalogDialogState.None, successMessage = "Modelo actualizado") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al actualizar modelo") }
            }
        }
    }

    fun confirmAddColor(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                catalogRepo.insertColor(name.trim())
                _uiState.update { it.copy(dialogState = CatalogDialogState.None, successMessage = "Color agregado") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al agregar color") }
            }
        }
    }

    fun confirmEditColor(color: CatalogColor, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            try {
                catalogRepo.updateColor(color.copy(name = newName.trim()))
                _uiState.update { it.copy(dialogState = CatalogDialogState.None, successMessage = "Color actualizado") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al actualizar color") }
            }
        }
    }

    fun confirmAddPartBrand(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                catalogRepo.insertPartBrand(name.trim())
                _uiState.update { it.copy(dialogState = CatalogDialogState.None, successMessage = "Marca de repuesto agregada") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al agregar") }
            }
        }
    }

    fun confirmEditPartBrand(partBrand: CatalogPartBrand, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            try {
                catalogRepo.updatePartBrand(partBrand.copy(name = newName.trim()))
                _uiState.update { it.copy(dialogState = CatalogDialogState.None, successMessage = "Marca de repuesto actualizada") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al actualizar") }
            }
        }
    }

    fun confirmDelete() {
        val dialog = _uiState.value.dialogState as? CatalogDialogState.ConfirmDelete ?: return
        viewModelScope.launch {
            try {
                when (dialog.type) {
                    "brand" -> {
                        val brand = _uiState.value.brands.find { it.id == dialog.id }
                        if (brand != null) {
                            catalogRepo.deleteBrand(brand)
                            // If this was the selected brand, deselect
                            if (_uiState.value.selectedBrandId == dialog.id) {
                                selectBrand(null)
                            }
                        }
                    }
                    "model" -> {
                        val model = _uiState.value.models.find { it.id == dialog.id }
                        if (model != null) catalogRepo.deleteModel(model)
                    }
                    "color" -> {
                        val color = _uiState.value.colors.find { it.id == dialog.id }
                        if (color != null) catalogRepo.deleteColor(color)
                    }
                    "partBrand" -> {
                        val partBrand = _uiState.value.partBrands.find { it.id == dialog.id }
                        if (partBrand != null) catalogRepo.deletePartBrand(partBrand)
                    }
                }
                _uiState.update { it.copy(dialogState = CatalogDialogState.None, successMessage = "Eliminado correctamente") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al eliminar") }
            }
        }
    }

    // Export
    fun exportCatalog() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val json = catalogRepo.exportToJson()
                _uiState.update { it.copy(isLoading = false, exportJson = json) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al exportar") }
            }
        }
    }

    fun clearExportJson() {
        _uiState.update { it.copy(exportJson = null) }
    }

    // Import
    fun confirmImport(jsonText: String) {
        if (jsonText.isBlank()) return
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, dialogState = CatalogDialogState.None) }
                catalogRepo.importFromJson(jsonText)
                // Reset selection after import
                selectBrand(null)
                _uiState.update { it.copy(isLoading = false, successMessage = "Importaci\u00f3n exitosa") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al importar. Verifique el formato JSON.") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
