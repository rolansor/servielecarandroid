package com.example.serviaux.ui.vehicles

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviaux.ServiauxApp
import com.example.serviaux.data.entity.CatalogBrand
import com.example.serviaux.data.entity.Customer
import com.example.serviaux.data.entity.Vehicle
import com.example.serviaux.data.entity.WorkOrder
import com.example.serviaux.util.PhotoUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

val DRIVETRAINS = listOf("4x2", "4x4")
val TRANSMISSIONS = listOf("Manual", "Autom\u00e1tico")

data class VehicleUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val searchQuery: String = "",
    val selectedVehicle: Vehicle? = null,
    val vehicleOrders: List<WorkOrder> = emptyList(),
    val customerName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val availableBrands: List<String> = emptyList(),
    val availableModels: List<String> = emptyList(),
    val availableColors: List<String> = emptyList(),
    val formPlate: String = "",
    val formBrand: String = "",
    val formModel: String = "",
    val formYear: String = "",
    val formVin: String = "",
    val formColor: String = "",
    val formVersion: String = "",
    val formEngineDisplacement: String = "",
    val formEngineNumber: String = "",
    val formDrivetrain: String = "4x2",
    val formTransmission: String = "Manual",
    val formNotes: String = "",
    val formCustomerId: Long? = null,
    val isEditing: Boolean = false,
    val editingVehicleId: Long? = null,
    val customers: List<Customer> = emptyList(),
    val customerVehicles: List<Vehicle> = emptyList(),
    val savedSuccessfully: Boolean = false,
    val formCustomerError: String? = null,
    val formPlateError: String? = null,
    val formBrandError: String? = null,
    val formModelError: String? = null,
    val formYearError: String? = null,
    val formVinError: String? = null,
    val formPhotoPaths: List<String> = emptyList(),
    val pendingPhotoUri: Uri? = null
)

class VehicleViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<ServiauxApp>()
    private val vehicleRepo get() = app.container.vehicleRepository
    private val customerRepo get() = app.container.customerRepository
    private val workOrderRepo get() = app.container.workOrderRepository
    private val catalogRepo get() = app.container.catalogRepository

    private val _uiState = MutableStateFlow(VehicleUiState())
    val uiState: StateFlow<VehicleUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var modelsJob: Job? = null
    private var catalogBrands: List<CatalogBrand> = emptyList()
    private var pendingPhotoFile: File? = null

    init {
        loadAllVehicles()
        loadCatalogs()
    }

    private fun loadCatalogs() {
        viewModelScope.launch {
            catalogRepo.getAllBrands().collect { brands ->
                catalogBrands = brands
                _uiState.update { it.copy(availableBrands = brands.map { b -> b.name }) }
            }
        }
        viewModelScope.launch {
            catalogRepo.getAllColors().collect { colors ->
                _uiState.update { it.copy(availableColors = colors.map { c -> c.name }) }
            }
        }
    }

    private fun loadModelsForBrand(brandName: String) {
        modelsJob?.cancel()
        val brand = catalogBrands.find { it.name == brandName }
        if (brand != null) {
            modelsJob = viewModelScope.launch {
                catalogRepo.getModelsByBrand(brand.id).collect { models ->
                    _uiState.update { it.copy(availableModels = models.map { m -> m.name }) }
                }
            }
        } else {
            _uiState.update { it.copy(availableModels = emptyList()) }
        }
    }

    private fun loadAllVehicles() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            vehicleRepo.getAll().collect { list ->
                _uiState.update { it.copy(vehicles = list) }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.isBlank()) {
                vehicleRepo.getAll().collect { list ->
                    _uiState.update { it.copy(vehicles = list) }
                }
            } else {
                vehicleRepo.search(query).collect { list ->
                    _uiState.update { it.copy(vehicles = list) }
                }
            }
        }
    }

    fun loadVehicle(id: Long) {
        viewModelScope.launch {
            vehicleRepo.getById(id).collect { vehicle ->
                _uiState.update { it.copy(selectedVehicle = vehicle) }
                vehicle?.let { v ->
                    val customer = customerRepo.getByIdDirect(v.customerId)
                    _uiState.update { it.copy(customerName = customer?.fullName ?: "") }
                }
            }
        }
        viewModelScope.launch {
            workOrderRepo.getByVehicle(id).collect { orders ->
                _uiState.update { it.copy(vehicleOrders = orders) }
            }
        }
    }

    fun onFormPlateChange(value: String) {
        if (value.length <= 9) {
            _uiState.update { it.copy(formPlate = value.uppercase(), formPlateError = null) }
        }
    }
    fun onFormBrandChange(value: String) {
        _uiState.update { it.copy(formBrand = value, formModel = "", formBrandError = null) }
        loadModelsForBrand(value)
    }
    fun onFormModelChange(value: String) {
        _uiState.update { it.copy(formModel = value, formModelError = null) }
    }
    fun onFormYearChange(value: String) {
        if (value.length <= 4 && value.all { it.isDigit() }) {
            _uiState.update { it.copy(formYear = value, formYearError = null) }
        }
    }
    fun onFormVinChange(value: String) {
        if (value.length <= 17 && value.all { it.isLetterOrDigit() }) {
            _uiState.update { it.copy(formVin = value.uppercase(), formVinError = null) }
        }
    }
    fun onFormColorChange(value: String) {
        _uiState.update { it.copy(formColor = value) }
    }
    fun onFormVersionChange(value: String) {
        _uiState.update { it.copy(formVersion = value) }
    }
    fun onFormEngineDisplacementChange(value: String) {
        if (value.length <= 5 && value.all { it.isDigit() || it == '.' }) {
            _uiState.update { it.copy(formEngineDisplacement = value) }
        }
    }
    fun onFormEngineNumberChange(value: String) {
        if (value.length <= 30 && value.all { it.isLetterOrDigit() || it == '-' }) {
            _uiState.update { it.copy(formEngineNumber = value.uppercase()) }
        }
    }
    fun onFormDrivetrainChange(value: String) {
        _uiState.update { it.copy(formDrivetrain = value) }
    }
    fun onFormTransmissionChange(value: String) {
        _uiState.update { it.copy(formTransmission = value) }
    }
    fun onFormNotesChange(value: String) { _uiState.update { it.copy(formNotes = value) } }
    fun onFormCustomerIdChange(value: Long?) {
        _uiState.update { it.copy(formCustomerId = value, formCustomerError = null) }
    }

    private val plateRegex = Regex("^[A-Z]{1,4}-?[0-9]{1,4}$")

    private fun validatePlate(): String? {
        val trimmed = _uiState.value.formPlate.trim()
        return when {
            trimmed.isBlank() -> "La placa es obligatoria"
            !plateRegex.matches(trimmed) -> "Formato de placa inválido (ej: BCD-123)"
            else -> null
        }
    }

    private fun validateBrand(): String? {
        return if (_uiState.value.formBrand.isBlank()) "La marca es obligatoria" else null
    }

    private fun validateModel(): String? {
        return if (_uiState.value.formModel.isBlank()) "El modelo es obligatorio" else null
    }

    private fun validateYear(): String? {
        val year = _uiState.value.formYear
        if (year.isNotBlank()) {
            val yearInt = year.toIntOrNull()
            if (yearInt == null || yearInt < 1900 || yearInt > 2030) {
                return "Año inválido"
            }
        }
        return null
    }

    private fun validateVin(): String? {
        val vin = _uiState.value.formVin
        if (vin.isNotBlank()) {
            if (vin.length != 17 || !vin.all { it.isLetterOrDigit() }) {
                return "El VIN debe tener 17 caracteres alfanuméricos"
            }
        }
        return null
    }

    fun validateFieldOnFocusLost(field: String) {
        when (field) {
            "plate" -> _uiState.update { it.copy(formPlateError = validatePlate()) }
            "brand" -> _uiState.update { it.copy(formBrandError = validateBrand()) }
            "model" -> _uiState.update { it.copy(formModelError = validateModel()) }
            "year" -> _uiState.update { it.copy(formYearError = validateYear()) }
            "vin" -> _uiState.update { it.copy(formVinError = validateVin()) }
        }
    }

    private fun validateForm(): Boolean {
        val customerError = if (_uiState.value.formCustomerId == null) "Debe seleccionar un cliente" else null
        val plateError = validatePlate()
        val brandError = validateBrand()
        val modelError = validateModel()
        val yearError = validateYear()
        val vinError = validateVin()

        _uiState.update {
            it.copy(
                formCustomerError = customerError,
                formPlateError = plateError,
                formBrandError = brandError,
                formModelError = modelError,
                formYearError = yearError,
                formVinError = vinError
            )
        }

        return customerError == null && plateError == null && brandError == null &&
                modelError == null && yearError == null && vinError == null
    }

    fun saveVehicle() {
        if (!validateForm()) return
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                if (state.isEditing && state.editingVehicleId != null) {
                    val existing = vehicleRepo.getByIdDirect(state.editingVehicleId)
                    if (existing != null) {
                        vehicleRepo.update(
                            existing.copy(
                                plate = state.formPlate.trim().uppercase(),
                                brand = state.formBrand.trim(),
                                model = state.formModel.trim(),
                                version = state.formVersion.trim().ifBlank { null },
                                year = state.formYear.toIntOrNull(),
                                vin = state.formVin.trim().ifBlank { null },
                                color = state.formColor.trim().ifBlank { null },
                                engineDisplacement = state.formEngineDisplacement.trim().ifBlank { null },
                                engineNumber = state.formEngineNumber.trim().ifBlank { null },
                                drivetrain = state.formDrivetrain,
                                transmission = state.formTransmission,
                                notes = state.formNotes.trim().ifBlank { null },
                                photoPaths = PhotoUtils.serializePaths(state.formPhotoPaths),
                                customerId = state.formCustomerId!!
                            )
                        )
                    }
                } else {
                    vehicleRepo.insert(
                        Vehicle(
                            plate = state.formPlate.trim().uppercase(),
                            brand = state.formBrand.trim(),
                            model = state.formModel.trim(),
                            version = state.formVersion.trim().ifBlank { null },
                            year = state.formYear.toIntOrNull(),
                            vin = state.formVin.trim().ifBlank { null },
                            color = state.formColor.trim().ifBlank { null },
                            engineDisplacement = state.formEngineDisplacement.trim().ifBlank { null },
                            engineNumber = state.formEngineNumber.trim().ifBlank { null },
                            drivetrain = state.formDrivetrain,
                            transmission = state.formTransmission,
                            notes = state.formNotes.trim().ifBlank { null },
                            photoPaths = PhotoUtils.serializePaths(state.formPhotoPaths),
                            customerId = state.formCustomerId!!
                        )
                    )
                }
                _uiState.update { it.copy(isLoading = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al guardar") }
            }
        }
    }

    fun prepareNew(customerId: Long? = null) {
        _uiState.update {
            it.copy(
                formPlate = "", formBrand = "", formModel = "", formVersion = "",
                formYear = "", formVin = "", formColor = "",
                formNotes = "", formEngineDisplacement = "", formEngineNumber = "",
                formDrivetrain = "4x2", formTransmission = "Manual",
                formCustomerId = customerId, isEditing = false, editingVehicleId = null, error = null,
                formCustomerError = null, formPlateError = null, formBrandError = null,
                formModelError = null, formYearError = null, formVinError = null,
                formPhotoPaths = emptyList(), pendingPhotoUri = null
            )
        }
    }

    fun prepareEdit(vehicle: Vehicle) {
        _uiState.update {
            it.copy(
                formPlate = vehicle.plate,
                formBrand = vehicle.brand,
                formModel = vehicle.model,
                formVersion = vehicle.version ?: "",
                formYear = vehicle.year?.toString() ?: "",
                formVin = vehicle.vin ?: "",
                formColor = vehicle.color ?: "",
                formEngineDisplacement = vehicle.engineDisplacement ?: "",
                formEngineNumber = vehicle.engineNumber ?: "",
                formDrivetrain = vehicle.drivetrain,
                formTransmission = vehicle.transmission,
                formNotes = vehicle.notes ?: "",
                formCustomerId = vehicle.customerId,
                isEditing = true,
                editingVehicleId = vehicle.id,
                error = null,
                formCustomerError = null, formPlateError = null, formBrandError = null,
                formModelError = null, formYearError = null, formVinError = null,
                formPhotoPaths = PhotoUtils.parsePaths(vehicle.photoPaths),
                pendingPhotoUri = null
            )
        }
        loadModelsForBrand(vehicle.brand)
    }

    fun loadVehiclesByCustomer(customerId: Long) {
        viewModelScope.launch {
            vehicleRepo.getByCustomer(customerId).collect { list ->
                _uiState.update { it.copy(customerVehicles = list) }
            }
        }
    }

    fun loadCustomers() {
        viewModelScope.launch {
            customerRepo.getAll().collect { list ->
                _uiState.update { it.copy(customers = list) }
            }
        }
    }

    fun prepareCameraFile(): Uri? {
        if (_uiState.value.formPhotoPaths.size >= PhotoUtils.MAX_PHOTOS) return null
        val context = getApplication<Application>()
        val file = PhotoUtils.createTempPhotoFile(context)
        pendingPhotoFile = file
        val uri = PhotoUtils.getUriForFile(context, file)
        _uiState.update { it.copy(pendingPhotoUri = uri) }
        return uri
    }

    fun onPhotoTaken(success: Boolean) {
        val file = pendingPhotoFile
        if (success && file != null && file.exists() && file.length() > 0) {
            _uiState.update {
                it.copy(
                    formPhotoPaths = it.formPhotoPaths + file.absolutePath,
                    pendingPhotoUri = null
                )
            }
        } else {
            file?.delete()
            _uiState.update { it.copy(pendingPhotoUri = null) }
        }
        pendingPhotoFile = null
    }

    fun removePhoto(index: Int) {
        val paths = _uiState.value.formPhotoPaths.toMutableList()
        if (index in paths.indices) {
            PhotoUtils.deletePhoto(paths[index])
            paths.removeAt(index)
            _uiState.update { it.copy(formPhotoPaths = paths) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
