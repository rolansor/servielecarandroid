/**
 * CatalogViewModel.kt - ViewModel de la pantalla de configuración de catálogos.
 *
 * Gestiona las operaciones CRUD de los 9 tipos de catálogo del sistema.
 * Cada tipo tiene su propia sección con lista, formulario inline de creación
 * y edición. Soporta exportación/importación de catálogos en formato JSON.
 *
 * Solo accesible para administradores.
 */
package com.example.serviaux.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviaux.ServiauxApp
import com.example.serviaux.data.entity.CatalogBrand
import com.example.serviaux.data.entity.CatalogModel
import com.example.serviaux.data.entity.CatalogColor
import com.example.serviaux.data.entity.CatalogPartBrand
import com.example.serviaux.data.entity.CatalogService
import com.example.serviaux.data.entity.CatalogVehicleType
import com.example.serviaux.data.entity.CatalogAccessory
import com.example.serviaux.data.entity.CatalogComplaint
import com.example.serviaux.data.entity.CatalogDiagnosis
import com.example.serviaux.data.entity.CatalogOilType
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
    val services: List<CatalogService> = emptyList(),
    val vehicleTypes: List<CatalogVehicleType> = emptyList(),
    val accessories: List<CatalogAccessory> = emptyList(),
    val complaints: List<CatalogComplaint> = emptyList(),
    val diagnoses: List<CatalogDiagnosis> = emptyList(),
    val oilTypes: List<CatalogOilType> = emptyList(),
    val selectedComplaintId: Long? = null,
    val selectedBrandId: Long? = null,
    val selectedBrandName: String = "",
    val expandedServiceCategory: String? = null,
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
    data class AddService(val category: String = "", val name: String = "", val price: String = "10.0", val vehicleType: String = "") : CatalogDialogState()
    data class EditService(val service: CatalogService, val category: String, val name: String, val price: String, val vehicleType: String = "") : CatalogDialogState()
    data class AddVehicleType(val name: String = "") : CatalogDialogState()
    data class EditVehicleType(val vt: CatalogVehicleType, val name: String) : CatalogDialogState()
    data class AddAccessory(val name: String = "") : CatalogDialogState()
    data class EditAccessory(val acc: CatalogAccessory, val name: String) : CatalogDialogState()
    data class AddComplaint(val name: String = "") : CatalogDialogState()
    data class EditComplaint(val complaint: CatalogComplaint, val name: String) : CatalogDialogState()
    data class AddDiagnosis(val complaintId: Long, val name: String = "") : CatalogDialogState()
    data class EditDiagnosis(val diagnosis: CatalogDiagnosis, val name: String) : CatalogDialogState()
    data class AddOilType(val name: String = "") : CatalogDialogState()
    data class EditOilType(val oilType: CatalogOilType, val name: String) : CatalogDialogState()
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
        loadServices()
        loadVehicleTypes()
        loadAccessories()
        loadComplaints()
        loadDiagnoses()
        loadOilTypes()
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

    private fun loadServices() {
        viewModelScope.launch {
            catalogRepo.getAllServices().collect { services ->
                _uiState.update { it.copy(services = services) }
            }
        }
    }

    private fun loadVehicleTypes() {
        viewModelScope.launch {
            catalogRepo.getAllVehicleTypes().collect { vts ->
                _uiState.update { it.copy(vehicleTypes = vts) }
            }
        }
    }

    private fun loadAccessories() {
        viewModelScope.launch {
            catalogRepo.getAllAccessories().collect { accs ->
                _uiState.update { it.copy(accessories = accs) }
            }
        }
    }

    private fun loadComplaints() {
        viewModelScope.launch {
            catalogRepo.getAllComplaints().collect { complaints ->
                _uiState.update { it.copy(complaints = complaints) }
            }
        }
    }

    private fun loadDiagnoses() {
        viewModelScope.launch {
            catalogRepo.getAllDiagnoses().collect { diagnoses ->
                _uiState.update { it.copy(diagnoses = diagnoses) }
            }
        }
    }

    private fun loadOilTypes() {
        viewModelScope.launch {
            catalogRepo.getAllOilTypes().collect { oilTypes ->
                _uiState.update { it.copy(oilTypes = oilTypes) }
            }
        }
    }

    fun selectComplaint(complaint: CatalogComplaint?) {
        _uiState.update { it.copy(selectedComplaintId = complaint?.id) }
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

    fun toggleServiceCategory(category: String) {
        _uiState.update {
            it.copy(expandedServiceCategory = if (it.expandedServiceCategory == category) null else category)
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

    fun showAddServiceDialog(category: String = "") {
        _uiState.update { it.copy(dialogState = CatalogDialogState.AddService(category = category)) }
    }

    fun showEditServiceDialog(service: CatalogService) {
        _uiState.update {
            it.copy(dialogState = CatalogDialogState.EditService(
                service, service.category, service.name, service.defaultPrice.toString(), service.vehicleType ?: ""
            ))
        }
    }

    fun showAddVehicleTypeDialog() {
        _uiState.update { it.copy(dialogState = CatalogDialogState.AddVehicleType()) }
    }

    fun showEditVehicleTypeDialog(vt: CatalogVehicleType) {
        _uiState.update { it.copy(dialogState = CatalogDialogState.EditVehicleType(vt, vt.name)) }
    }

    fun showAddAccessoryDialog() {
        _uiState.update { it.copy(dialogState = CatalogDialogState.AddAccessory()) }
    }

    fun showEditAccessoryDialog(acc: CatalogAccessory) {
        _uiState.update { it.copy(dialogState = CatalogDialogState.EditAccessory(acc, acc.name)) }
    }

    fun showAddComplaintDialog() {
        _uiState.update { it.copy(dialogState = CatalogDialogState.AddComplaint()) }
    }

    fun showEditComplaintDialog(complaint: CatalogComplaint) {
        _uiState.update { it.copy(dialogState = CatalogDialogState.EditComplaint(complaint, complaint.name)) }
    }

    fun showAddDiagnosisDialog(complaintId: Long) {
        _uiState.update { it.copy(dialogState = CatalogDialogState.AddDiagnosis(complaintId)) }
    }

    fun showEditDiagnosisDialog(diagnosis: CatalogDiagnosis) {
        _uiState.update { it.copy(dialogState = CatalogDialogState.EditDiagnosis(diagnosis, diagnosis.name)) }
    }

    fun showAddOilTypeDialog() {
        _uiState.update { it.copy(dialogState = CatalogDialogState.AddOilType()) }
    }

    fun showEditOilTypeDialog(oilType: CatalogOilType) {
        _uiState.update { it.copy(dialogState = CatalogDialogState.EditOilType(oilType, oilType.name)) }
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
                is CatalogDialogState.AddVehicleType -> d.copy(name = text)
                is CatalogDialogState.EditVehicleType -> d.copy(name = text)
                is CatalogDialogState.AddAccessory -> d.copy(name = text)
                is CatalogDialogState.EditAccessory -> d.copy(name = text)
                is CatalogDialogState.AddComplaint -> d.copy(name = text)
                is CatalogDialogState.EditComplaint -> d.copy(name = text)
                is CatalogDialogState.AddDiagnosis -> d.copy(name = text)
                is CatalogDialogState.EditDiagnosis -> d.copy(name = text)
                is CatalogDialogState.AddOilType -> d.copy(name = text)
                is CatalogDialogState.EditOilType -> d.copy(name = text)
                is CatalogDialogState.ImportDialog -> d.copy(jsonText = text)
                else -> d
            }
            state.copy(dialogState = newDialog)
        }
    }

    fun updateServiceDialogField(field: String, value: String) {
        _uiState.update { state ->
            val newDialog = when (val d = state.dialogState) {
                is CatalogDialogState.AddService -> when (field) {
                    "category" -> d.copy(category = value)
                    "name" -> d.copy(name = value)
                    "price" -> d.copy(price = value)
                    "vehicleType" -> d.copy(vehicleType = value)
                    else -> d
                }
                is CatalogDialogState.EditService -> when (field) {
                    "category" -> d.copy(category = value)
                    "name" -> d.copy(name = value)
                    "price" -> d.copy(price = value)
                    "vehicleType" -> d.copy(vehicleType = value)
                    else -> d
                }
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

    fun confirmAddService(category: String, name: String, priceStr: String, vehicleType: String = "") {
        if (category.isBlank() || name.isBlank()) return
        val price = priceStr.toDoubleOrNull() ?: 10.0
        val vType = vehicleType.trim().ifBlank { null }
        viewModelScope.launch {
            try {
                catalogRepo.insertService(category.trim(), name.trim(), price, vType)
                _uiState.update { it.copy(dialogState = CatalogDialogState.None, successMessage = "Servicio agregado") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al agregar servicio") }
            }
        }
    }

    fun confirmEditService(service: CatalogService, category: String, name: String, priceStr: String, vehicleType: String = "") {
        if (category.isBlank() || name.isBlank()) return
        val price = priceStr.toDoubleOrNull() ?: service.defaultPrice
        val vType = vehicleType.trim().ifBlank { null }
        viewModelScope.launch {
            try {
                catalogRepo.updateService(service.copy(category = category.trim(), name = name.trim(), defaultPrice = price, vehicleType = vType))
                _uiState.update { it.copy(dialogState = CatalogDialogState.None, successMessage = "Servicio actualizado") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al actualizar servicio") }
            }
        }
    }

    fun confirmAddVehicleType(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                catalogRepo.insertVehicleType(name.trim())
                _uiState.update { it.copy(dialogState = CatalogDialogState.None, successMessage = "Tipo de veh\u00edculo agregado") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al agregar") }
            }
        }
    }

    fun confirmEditVehicleType(vt: CatalogVehicleType, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            try {
                catalogRepo.updateVehicleType(vt.copy(name = newName.trim()))
                _uiState.update { it.copy(dialogState = CatalogDialogState.None, successMessage = "Tipo de veh\u00edculo actualizado") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al actualizar") }
            }
        }
    }

    fun confirmAddAccessory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                catalogRepo.insertAccessory(name.trim())
                _uiState.update { it.copy(dialogState = CatalogDialogState.None, successMessage = "Accesorio agregado") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al agregar") }
            }
        }
    }

    fun confirmEditAccessory(acc: CatalogAccessory, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            try {
                catalogRepo.updateAccessory(acc.copy(name = newName.trim()))
                _uiState.update { it.copy(dialogState = CatalogDialogState.None, successMessage = "Accesorio actualizado") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al actualizar") }
            }
        }
    }

    fun confirmAddComplaint(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                catalogRepo.insertComplaint(name.trim())
                _uiState.update { it.copy(dialogState = CatalogDialogState.None, successMessage = "Motivo agregado") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al agregar") }
            }
        }
    }

    fun confirmEditComplaint(complaint: CatalogComplaint, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            try {
                catalogRepo.updateComplaint(complaint.copy(name = newName.trim()))
                _uiState.update { it.copy(dialogState = CatalogDialogState.None, successMessage = "Motivo actualizado") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al actualizar") }
            }
        }
    }

    fun confirmAddDiagnosis(complaintId: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                catalogRepo.insertDiagnosis(complaintId, name.trim())
                _uiState.update { it.copy(dialogState = CatalogDialogState.None, successMessage = "Diagn\u00f3stico agregado") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al agregar") }
            }
        }
    }

    fun confirmEditDiagnosis(diagnosis: CatalogDiagnosis, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            try {
                catalogRepo.updateDiagnosis(diagnosis.copy(name = newName.trim()))
                _uiState.update { it.copy(dialogState = CatalogDialogState.None, successMessage = "Diagn\u00f3stico actualizado") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al actualizar") }
            }
        }
    }

    fun confirmAddOilType(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                catalogRepo.insertOilType(name.trim())
                _uiState.update { it.copy(dialogState = CatalogDialogState.None, successMessage = "Tipo de aceite agregado") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al agregar") }
            }
        }
    }

    fun confirmEditOilType(oilType: CatalogOilType, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            try {
                catalogRepo.updateOilType(oilType.copy(name = newName.trim()))
                _uiState.update { it.copy(dialogState = CatalogDialogState.None, successMessage = "Tipo de aceite actualizado") }
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
                    "service" -> {
                        val service = _uiState.value.services.find { it.id == dialog.id }
                        if (service != null) catalogRepo.deleteService(service)
                    }
                    "vehicleType" -> {
                        val vt = _uiState.value.vehicleTypes.find { it.id == dialog.id }
                        if (vt != null) catalogRepo.deleteVehicleType(vt)
                    }
                    "accessory" -> {
                        val acc = _uiState.value.accessories.find { it.id == dialog.id }
                        if (acc != null) catalogRepo.deleteAccessory(acc)
                    }
                    "complaint" -> {
                        val complaint = _uiState.value.complaints.find { it.id == dialog.id }
                        if (complaint != null) catalogRepo.deleteComplaint(complaint)
                    }
                    "diagnosis" -> {
                        val diagnosis = _uiState.value.diagnoses.find { it.id == dialog.id }
                        if (diagnosis != null) catalogRepo.deleteDiagnosis(diagnosis)
                    }
                    "oilType" -> {
                        val oilType = _uiState.value.oilTypes.find { it.id == dialog.id }
                        if (oilType != null) catalogRepo.deleteOilType(oilType)
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
