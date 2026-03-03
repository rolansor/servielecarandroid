/**
 * VehicleViewModel.kt - ViewModel del módulo de vehículos.
 *
 * Gestiona la lista de vehículos con búsqueda, el detalle de un
 * vehículo (con historial de órdenes y datos del propietario),
 * el formulario de creación/edición y la gestión de fotos.
 * Los campos de marca, modelo, color y tipo de vehículo se alimentan
 * desde los catálogos del sistema.
 */
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
val FUEL_TYPES = listOf("Gasolina", "Di\u00e9sel", "El\u00e9ctrico", "H\u00edbrido")

data class VehicleUiState(
    val vehicles: List<Vehicle> = emptyList(),
    val searchQuery: String = "",
    val currentPage: Int = 0,
    val totalCount: Int = 0,
    val pageSize: Int = 100,
    val selectedVehicle: Vehicle? = null,
    val vehicleOrders: List<WorkOrder> = emptyList(),
    val customerName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val availableBrands: List<String> = emptyList(),
    val availableModels: List<String> = emptyList(),
    val availableColors: List<String> = emptyList(),
    val availableVehicleTypes: List<String> = emptyList(),
    val availableOilTypes: List<String> = emptyList(),
    val formVehicleType: String = "",
    val formFuelType: String = "",
    val formOilType: String = "",
    val formOilTypeSearch: String = "",
    val formOilCapacity: String = "",
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
    val formCustomerSearch: String = "",
    val formBrandSearch: String = "",
    val formModelSearch: String = "",
    val formColorSearch: String = "",
    val formVehicleTypeSearch: String = "",
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
    val formRegistrationPhotoPaths: List<String> = emptyList(),
    val formPhotoPaths: List<String> = emptyList(),
    val pendingPhotoTarget: String = "vehicle",
    val pendingPhotoUri: Uri? = null,
    val isListLoaded: Boolean = false
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
        loadVehiclesPage(0)
        loadTotalCount()
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
        viewModelScope.launch {
            catalogRepo.getAllVehicleTypes().collect { types ->
                _uiState.update { it.copy(availableVehicleTypes = types.map { t -> t.name }) }
            }
        }
        viewModelScope.launch {
            catalogRepo.getAllOilTypes().collect { oilTypes ->
                _uiState.update { it.copy(availableOilTypes = oilTypes.map { o -> o.name }) }
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

    private fun loadTotalCount() {
        viewModelScope.launch {
            vehicleRepo.getTotalCount().collect { count ->
                _uiState.update { it.copy(totalCount = count) }
            }
        }
    }

    private fun loadVehiclesPage(page: Int) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val offset = page * _uiState.value.pageSize
            vehicleRepo.getPaginated(_uiState.value.pageSize, offset).collect { list ->
                _uiState.update { it.copy(vehicles = list, currentPage = page, isListLoaded = true) }
            }
        }
    }

    fun nextPage() {
        val state = _uiState.value
        if ((state.currentPage + 1) * state.pageSize < state.totalCount) {
            _uiState.update { it.copy(searchQuery = "") }
            loadVehiclesPage(state.currentPage + 1)
        }
    }

    fun previousPage() {
        val state = _uiState.value
        if (state.currentPage > 0) {
            _uiState.update { it.copy(searchQuery = "") }
            loadVehiclesPage(state.currentPage - 1)
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (query.isBlank()) {
                val offset = _uiState.value.currentPage * _uiState.value.pageSize
                vehicleRepo.getPaginated(_uiState.value.pageSize, offset).collect { list ->
                    _uiState.update { it.copy(vehicles = list, isListLoaded = true) }
                }
            } else {
                vehicleRepo.search(query).collect { list ->
                    _uiState.update { it.copy(vehicles = list, isListLoaded = true) }
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
    fun onFormColorSearchChange(value: String) {
        _uiState.update { it.copy(formColorSearch = value.uppercase(), formColor = value.uppercase()) }
    }
    fun onFormColorSelected(colorName: String) {
        _uiState.update { it.copy(formColor = colorName, formColorSearch = colorName) }
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
    fun onFormVehicleTypeChange(value: String) {
        _uiState.update { it.copy(formVehicleType = value) }
    }
    fun onFormVehicleTypeSearchChange(value: String) {
        _uiState.update { it.copy(formVehicleTypeSearch = value.uppercase(), formVehicleType = value.uppercase()) }
    }
    fun onFormVehicleTypeSelected(typeName: String) {
        _uiState.update { it.copy(formVehicleType = typeName, formVehicleTypeSearch = typeName) }
    }
    fun onFormFuelTypeChange(value: String) {
        _uiState.update { it.copy(formFuelType = value) }
    }
    fun onFormOilTypeSearchChange(value: String) {
        _uiState.update { it.copy(formOilTypeSearch = value.uppercase(), formOilType = value.uppercase()) }
    }
    fun onFormOilTypeSelected(name: String) {
        _uiState.update { it.copy(formOilType = name, formOilTypeSearch = name) }
    }
    fun onFormOilCapacityChange(value: String) {
        _uiState.update { it.copy(formOilCapacity = value) }
    }
    fun onFormNotesChange(value: String) { _uiState.update { it.copy(formNotes = value.uppercase()) } }
    fun onFormVersionChange(value: String) {
        _uiState.update { it.copy(formVersion = value.uppercase()) }
    }
    fun onFormCustomerIdChange(value: Long?) {
        _uiState.update { it.copy(formCustomerId = value, formCustomerError = null) }
    }

    fun onFormCustomerSearchChange(value: String) {
        _uiState.update { it.copy(formCustomerSearch = value.uppercase(), formCustomerError = null) }
    }

    fun onFormCustomerSelected(customerId: Long) {
        val customer = _uiState.value.customers.find { it.id == customerId }
        _uiState.update {
            it.copy(
                formCustomerId = customerId,
                formCustomerSearch = customer?.fullName ?: "",
                formCustomerError = null
            )
        }
    }

    fun onFormBrandSearchChange(value: String) {
        _uiState.update { it.copy(formBrandSearch = value.uppercase(), formBrand = value.uppercase(), formModel = "", formModelSearch = "", formBrandError = null) }
        loadModelsForBrand(value.uppercase())
    }

    fun onFormBrandSelected(brandName: String) {
        _uiState.update {
            it.copy(
                formBrand = brandName,
                formBrandSearch = brandName,
                formModel = "",
                formModelSearch = "",
                formBrandError = null
            )
        }
        loadModelsForBrand(brandName)
    }

    fun onFormModelSearchChange(value: String) {
        _uiState.update { it.copy(formModelSearch = value.uppercase(), formModel = value.uppercase(), formModelError = null) }
    }

    fun onFormModelSelected(modelName: String) {
        _uiState.update {
            it.copy(
                formModel = modelName,
                formModelSearch = modelName,
                formModelError = null
            )
        }
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
            "plate" -> {
                val formatError = validatePlate()
                _uiState.update { it.copy(formPlateError = formatError) }
                if (formatError == null) checkDuplicatePlate()
            }
            "brand" -> _uiState.update { it.copy(formBrandError = validateBrand()) }
            "model" -> _uiState.update { it.copy(formModelError = validateModel()) }
            "year" -> _uiState.update { it.copy(formYearError = validateYear()) }
            "vin" -> _uiState.update { it.copy(formVinError = validateVin()) }
        }
    }

    private fun checkDuplicatePlate() {
        val plate = _uiState.value.formPlate.trim().uppercase()
        if (plate.isBlank()) return
        viewModelScope.launch {
            val existing = vehicleRepo.findByPlate(plate)
            if (existing != null && existing.id != _uiState.value.editingVehicleId) {
                _uiState.update { it.copy(formPlateError = "Ya existe un veh\u00edculo con esta placa") }
            }
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
                                vehicleType = state.formVehicleType.ifBlank { null },
                                fuelType = state.formFuelType.ifBlank { null },
                                oilType = state.formOilType.ifBlank { null },
                                oilCapacity = state.formOilCapacity.trim().ifBlank { null },
                                engineDisplacement = state.formEngineDisplacement.trim().ifBlank { null },
                                engineNumber = state.formEngineNumber.trim().ifBlank { null },
                                drivetrain = state.formDrivetrain,
                                transmission = state.formTransmission,
                                notes = state.formNotes.trim().ifBlank { null },
                                registrationPhotoPaths = PhotoUtils.serializePaths(state.formRegistrationPhotoPaths),
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
                            vehicleType = state.formVehicleType.ifBlank { null },
                            fuelType = state.formFuelType.ifBlank { null },
                            oilType = state.formOilType.ifBlank { null },
                            oilCapacity = state.formOilCapacity.trim().ifBlank { null },
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
                formYear = "", formVin = "", formColor = "", formVehicleType = "", formFuelType = "Gasolina",
                formOilType = "", formOilTypeSearch = "", formOilCapacity = "",
                formNotes = "", formEngineDisplacement = "", formEngineNumber = "",
                formDrivetrain = "4x2", formTransmission = "Manual",
                formCustomerId = customerId, formCustomerSearch = "", formBrandSearch = "", formModelSearch = "", formColorSearch = "", formVehicleTypeSearch = "",
                isEditing = false, editingVehicleId = null, error = null,
                formCustomerError = null, formPlateError = null, formBrandError = null,
                formModelError = null, formYearError = null, formVinError = null,
                formRegistrationPhotoPaths = emptyList(), formPhotoPaths = emptyList(), pendingPhotoUri = null
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
                formColorSearch = vehicle.color ?: "",
                formVehicleType = vehicle.vehicleType ?: "",
                formVehicleTypeSearch = vehicle.vehicleType ?: "",
                formFuelType = vehicle.fuelType ?: "",
                formOilType = vehicle.oilType ?: "",
                formOilTypeSearch = vehicle.oilType ?: "",
                formOilCapacity = vehicle.oilCapacity ?: "",
                formEngineDisplacement = vehicle.engineDisplacement ?: "",
                formEngineNumber = vehicle.engineNumber ?: "",
                formDrivetrain = vehicle.drivetrain,
                formTransmission = vehicle.transmission,
                formNotes = vehicle.notes ?: "",
                formCustomerId = vehicle.customerId,
                formBrandSearch = vehicle.brand,
                formModelSearch = vehicle.model,
                isEditing = true,
                editingVehicleId = vehicle.id,
                error = null,
                formCustomerError = null, formPlateError = null, formBrandError = null,
                formModelError = null, formYearError = null, formVinError = null,
                formRegistrationPhotoPaths = PhotoUtils.parsePaths(vehicle.registrationPhotoPaths),
                formPhotoPaths = PhotoUtils.parsePaths(vehicle.photoPaths),
                pendingPhotoUri = null
            )
        }
        loadModelsForBrand(vehicle.brand)
        // Load customer name async since customers list may not be ready yet
        viewModelScope.launch {
            val customer = customerRepo.getByIdDirect(vehicle.customerId)
            _uiState.update { it.copy(formCustomerSearch = customer?.fullName ?: "") }
        }
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

    fun prepareCameraFile(target: String = "vehicle"): Uri? {
        val context = getApplication<Application>()
        val prefix = if (target == "registration") "MAT" else "VEH"
        val file = PhotoUtils.createTempPhotoFile(context, prefix)
        pendingPhotoFile = file
        val uri = PhotoUtils.getUriForFile(context, file)
        _uiState.update { it.copy(pendingPhotoUri = uri, pendingPhotoTarget = target) }
        return uri
    }

    fun onPhotoTaken(success: Boolean) {
        val file = pendingPhotoFile
        val target = _uiState.value.pendingPhotoTarget
        if (success && file != null && file.exists() && file.length() > 0) {
            _uiState.update {
                if (target == "registration") {
                    it.copy(
                        formRegistrationPhotoPaths = it.formRegistrationPhotoPaths + file.absolutePath,
                        pendingPhotoUri = null
                    )
                } else {
                    it.copy(
                        formPhotoPaths = it.formPhotoPaths + file.absolutePath,
                        pendingPhotoUri = null
                    )
                }
            }
        } else {
            file?.delete()
            _uiState.update { it.copy(pendingPhotoUri = null) }
        }
        pendingPhotoFile = null
    }

    fun addPhotoFromGallery(uri: Uri, target: String = "vehicle") {
        val context = getApplication<Application>()
        val prefix = if (target == "registration") "MAT" else "VEH"
        val file = PhotoUtils.copyUriToInternalStorage(context, uri, prefix)
        if (file != null) {
            _uiState.update {
                if (target == "registration") {
                    it.copy(formRegistrationPhotoPaths = it.formRegistrationPhotoPaths + file.absolutePath)
                } else {
                    it.copy(formPhotoPaths = it.formPhotoPaths + file.absolutePath)
                }
            }
        }
    }

    fun onPhotoTakenForReplace(success: Boolean, target: String, index: Int) {
        val file = pendingPhotoFile
        if (success && file != null && file.exists() && file.length() > 0) {
            _uiState.update {
                if (target == "registration") {
                    val paths = it.formRegistrationPhotoPaths.toMutableList()
                    if (index in paths.indices) {
                        PhotoUtils.deletePhoto(paths[index])
                        paths[index] = file.absolutePath
                    }
                    it.copy(formRegistrationPhotoPaths = paths, pendingPhotoUri = null)
                } else {
                    val paths = it.formPhotoPaths.toMutableList()
                    if (index in paths.indices) {
                        PhotoUtils.deletePhoto(paths[index])
                        paths[index] = file.absolutePath
                    }
                    it.copy(formPhotoPaths = paths, pendingPhotoUri = null)
                }
            }
        } else {
            file?.delete()
            _uiState.update { it.copy(pendingPhotoUri = null) }
        }
        pendingPhotoFile = null
    }

    fun replacePhotoFromGallery(uri: Uri, target: String, index: Int) {
        val context = getApplication<Application>()
        val prefix = if (target == "registration") "MAT" else "VEH"
        val file = PhotoUtils.copyUriToInternalStorage(context, uri, prefix)
        if (file != null) {
            _uiState.update {
                if (target == "registration") {
                    val paths = it.formRegistrationPhotoPaths.toMutableList()
                    if (index in paths.indices) {
                        PhotoUtils.deletePhoto(paths[index])
                        paths[index] = file.absolutePath
                    }
                    it.copy(formRegistrationPhotoPaths = paths)
                } else {
                    val paths = it.formPhotoPaths.toMutableList()
                    if (index in paths.indices) {
                        PhotoUtils.deletePhoto(paths[index])
                        paths[index] = file.absolutePath
                    }
                    it.copy(formPhotoPaths = paths)
                }
            }
        }
    }

    fun removeRegistrationPhoto(index: Int) {
        val paths = _uiState.value.formRegistrationPhotoPaths.toMutableList()
        if (index in paths.indices) {
            PhotoUtils.deletePhoto(paths[index])
            paths.removeAt(index)
            _uiState.update { it.copy(formRegistrationPhotoPaths = paths) }
        }
    }

    fun removePhoto(index: Int) {
        val paths = _uiState.value.formPhotoPaths.toMutableList()
        if (index in paths.indices) {
            PhotoUtils.deletePhoto(paths[index])
            paths.removeAt(index)
            _uiState.update { it.copy(formPhotoPaths = paths) }
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            if (vehicleRepo.canDelete(vehicle.id)) {
                vehicleRepo.delete(vehicle)
            } else {
                _uiState.update { it.copy(error = "No se puede eliminar: tiene \u00f3rdenes asociadas") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
