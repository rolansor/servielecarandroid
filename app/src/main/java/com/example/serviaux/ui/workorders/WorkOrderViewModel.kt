package com.example.serviaux.ui.workorders

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviaux.ServiauxApp
import com.example.serviaux.data.entity.*
import com.example.serviaux.util.PdfReportGenerator
import com.example.serviaux.util.PhotoUtils
import com.example.serviaux.util.WorkOrderReportData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class WorkOrderUiState(
    val orders: List<WorkOrder> = emptyList(),
    val selectedOrder: WorkOrder? = null,
    val serviceLines: List<ServiceLine> = emptyList(),
    val orderParts: List<WorkOrderPart> = emptyList(),
    val payments: List<WorkOrderPayment> = emptyList(),
    val statusLog: List<WorkOrderStatusLog> = emptyList(),
    val availableParts: List<Part> = emptyList(),
    val mechanics: List<User> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val customerVehicles: List<Vehicle> = emptyList(),
    val vehicleName: String = "",
    val customerName: String = "",
    val filter: OrderStatus? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    // Create order form
    val formCustomerId: Long? = null,
    val formVehicleId: Long? = null,
    val formComplaint: String = "",
    val formDiagnosis: String = "",
    val formPriority: Priority = Priority.MEDIA,
    val formEntryMileage: String = "",
    val formFuelLevel: String = "",
    val formChecklistNotes: String = "",
    // Service line form
    val serviceLineFormDescription: String = "",
    val serviceLineFormLaborCost: String = "",
    // Part form
    val partFormSelectedPartId: Long? = null,
    val partFormQuantity: String = "",
    // Payment form
    val paymentFormAmount: String = "",
    val paymentFormMethod: PaymentMethod = PaymentMethod.EFECTIVO,
    val paymentFormNotes: String = "",
    val createdOrderId: Long? = null,
    // Form validation errors
    val formCustomerError: String? = null,
    val formVehicleError: String? = null,
    val formComplaintError: String? = null,
    val catalogServices: List<com.example.serviaux.data.entity.CatalogService> = emptyList(),
    val formMileageError: String? = null,
    val formPhotoPaths: List<String> = emptyList(),
    val pendingPhotoUri: Uri? = null,
    val detailPhotoPaths: List<String> = emptyList(),
    val detailPendingPhotoUri: Uri? = null,
    val selectedCustomer: Customer? = null,
    val selectedVehicle: Vehicle? = null,
    val pdfGenerating: Boolean = false,
    val pdfFile: File? = null
)

class WorkOrderViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<ServiauxApp>()
    private val workOrderRepo get() = app.container.workOrderRepository
    private val customerRepo get() = app.container.customerRepository
    private val vehicleRepo get() = app.container.vehicleRepository
    private val partRepo get() = app.container.partRepository
    private val authRepo get() = app.container.authRepository
    private val catalogRepo get() = app.container.catalogRepository
    private val session get() = app.container.sessionManager

    private val _uiState = MutableStateFlow(WorkOrderUiState())
    val uiState: StateFlow<WorkOrderUiState> = _uiState.asStateFlow()

    private var ordersJob: Job? = null
    private var pendingPhotoFile: File? = null
    private var detailPendingPhotoFile: File? = null

    init {
        loadOrders()
        loadCatalogServices()
    }

    private fun loadCatalogServices() {
        viewModelScope.launch {
            catalogRepo.getAllServices().collect { services ->
                _uiState.update { it.copy(catalogServices = services) }
            }
        }
    }

    fun loadOrders(filter: OrderStatus? = null) {
        _uiState.update { it.copy(filter = filter) }
        ordersJob?.cancel()
        ordersJob = viewModelScope.launch {
            val flow = if (filter != null) {
                workOrderRepo.getByStatus(filter)
            } else {
                workOrderRepo.getAll()
            }
            flow.collect { list ->
                _uiState.update { it.copy(orders = list) }
            }
        }
    }

    fun loadOrderDetail(orderId: Long) {
        viewModelScope.launch {
            workOrderRepo.getById(orderId).collect { order ->
                _uiState.update {
                    it.copy(
                        selectedOrder = order,
                        detailPhotoPaths = PhotoUtils.parsePaths(order?.photoPaths)
                    )
                }
                order?.let { o ->
                    val vehicle = vehicleRepo.getByIdDirect(o.vehicleId)
                    val customer = customerRepo.getByIdDirect(o.customerId)
                    _uiState.update {
                        it.copy(
                            vehicleName = vehicle?.let { v -> "${v.plate} - ${v.brand} ${v.model}" } ?: "",
                            customerName = customer?.fullName ?: "",
                            selectedCustomer = customer,
                            selectedVehicle = vehicle
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            workOrderRepo.getServiceLines(orderId).collect { lines ->
                _uiState.update { it.copy(serviceLines = lines) }
            }
        }
        viewModelScope.launch {
            workOrderRepo.getWorkOrderParts(orderId).collect { parts ->
                _uiState.update { it.copy(orderParts = parts) }
            }
        }
        viewModelScope.launch {
            workOrderRepo.getPayments(orderId).collect { payments ->
                _uiState.update { it.copy(payments = payments) }
            }
        }
        viewModelScope.launch {
            workOrderRepo.getStatusLog(orderId).collect { log ->
                _uiState.update { it.copy(statusLog = log) }
            }
        }
    }

    private fun validateComplaint(): String? {
        val complaint = _uiState.value.formComplaint
        return when {
            complaint.isBlank() -> "Describa el motivo de la visita"
            complaint.trim().length < 5 -> "Mínimo 5 caracteres"
            else -> null
        }
    }

    private fun validateEntryMileage(): String? {
        val mileage = _uiState.value.formEntryMileage
        return if (mileage.isNotBlank() && mileage.toIntOrNull() == null) "Solo números" else null
    }

    fun validateFieldOnFocusLost(field: String) {
        when (field) {
            "complaint" -> _uiState.update { it.copy(formComplaintError = validateComplaint()) }
            "entryMileage" -> _uiState.update { it.copy(formMileageError = validateEntryMileage()) }
        }
    }

    fun createOrder() {
        val state = _uiState.value
        if (!session.isLoggedIn) {
            _uiState.update { it.copy(error = "Debe iniciar sesión") }
            return
        }

        val customerError = if (state.formCustomerId == null) "Debe seleccionar un cliente" else null
        val vehicleError = if (state.formVehicleId == null) "Debe seleccionar un vehículo" else null
        val complaintError = validateComplaint()
        val mileageError = validateEntryMileage()

        _uiState.update {
            it.copy(
                formCustomerError = customerError,
                formVehicleError = vehicleError,
                formComplaintError = complaintError,
                formMileageError = mileageError
            )
        }

        if (customerError != null || vehicleError != null || complaintError != null || mileageError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val userId = session.currentUserId
                val order = WorkOrder(
                    customerId = state.formCustomerId!!,
                    vehicleId = state.formVehicleId!!,
                    customerComplaint = state.formComplaint.trim(),
                    initialDiagnosis = state.formDiagnosis.trim().ifBlank { null },
                    priority = state.formPriority,
                    entryMileage = state.formEntryMileage.toIntOrNull(),
                    fuelLevel = state.formFuelLevel.trim().ifBlank { null },
                    checklistNotes = state.formChecklistNotes.trim().ifBlank { null },
                    photoPaths = PhotoUtils.serializePaths(state.formPhotoPaths),
                    createdBy = userId,
                    updatedBy = userId
                )
                val orderId = workOrderRepo.insert(order)
                _uiState.update { it.copy(isLoading = false, createdOrderId = orderId) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al crear orden") }
            }
        }
    }

    fun changeStatus(newStatus: OrderStatus, note: String? = null) {
        val orderId = _uiState.value.selectedOrder?.id ?: return
        viewModelScope.launch {
            try {
                workOrderRepo.changeStatus(orderId, newStatus, session.currentUserId, note)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al cambiar estado") }
            }
        }
    }

    fun assignMechanic(mechanicId: Long) {
        val orderId = _uiState.value.selectedOrder?.id ?: return
        viewModelScope.launch {
            try {
                workOrderRepo.assignMechanic(orderId, mechanicId, session.currentUserId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al asignar mecanico") }
            }
        }
    }

    fun addServiceLine() {
        val state = _uiState.value
        val orderId = state.selectedOrder?.id ?: return
        if (state.serviceLineFormDescription.isBlank()) {
            _uiState.update { it.copy(error = "Ingrese descripcion del servicio") }
            return
        }
        viewModelScope.launch {
            try {
                val serviceLine = ServiceLine(
                    workOrderId = orderId,
                    description = state.serviceLineFormDescription.trim(),
                    laborCost = state.serviceLineFormLaborCost.toDoubleOrNull() ?: 0.0
                )
                workOrderRepo.addServiceLine(serviceLine)
                _uiState.update {
                    it.copy(
                        serviceLineFormDescription = "",
                        serviceLineFormLaborCost = ""
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al agregar servicio") }
            }
        }
    }

    fun deleteServiceLine(serviceLine: ServiceLine) {
        viewModelScope.launch {
            try {
                workOrderRepo.deleteServiceLine(serviceLine)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al eliminar servicio") }
            }
        }
    }

    fun addPart() {
        val state = _uiState.value
        val orderId = state.selectedOrder?.id ?: return
        val partId = state.partFormSelectedPartId ?: run {
            _uiState.update { it.copy(error = "Seleccione un repuesto") }
            return
        }
        val quantity = state.partFormQuantity.toIntOrNull() ?: run {
            _uiState.update { it.copy(error = "Ingrese cantidad valida") }
            return
        }
        viewModelScope.launch {
            try {
                val part = partRepo.getByIdDirect(partId) ?: run {
                    _uiState.update { it.copy(error = "Repuesto no encontrado") }
                    return@launch
                }
                val unitPrice = part.salePrice ?: 0.0
                val workOrderPart = WorkOrderPart(
                    workOrderId = orderId,
                    partId = partId,
                    quantity = quantity,
                    appliedUnitPrice = unitPrice,
                    subtotal = unitPrice * quantity
                )
                workOrderRepo.addWorkOrderPart(workOrderPart)
                _uiState.update {
                    it.copy(partFormSelectedPartId = null, partFormQuantity = "")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al agregar repuesto") }
            }
        }
    }

    fun deletePart(workOrderPart: WorkOrderPart) {
        viewModelScope.launch {
            try {
                workOrderRepo.deleteWorkOrderPart(workOrderPart)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al eliminar repuesto") }
            }
        }
    }

    fun addPayment() {
        val state = _uiState.value
        val orderId = state.selectedOrder?.id ?: return
        val amount = state.paymentFormAmount.toDoubleOrNull() ?: run {
            _uiState.update { it.copy(error = "Ingrese monto valido") }
            return
        }
        viewModelScope.launch {
            try {
                val payment = WorkOrderPayment(
                    workOrderId = orderId,
                    amount = amount,
                    method = state.paymentFormMethod,
                    notes = state.paymentFormNotes.trim().ifBlank { null }
                )
                workOrderRepo.addPayment(payment)
                _uiState.update {
                    it.copy(paymentFormAmount = "", paymentFormNotes = "")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al registrar pago") }
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

    fun loadVehiclesForCustomer(customerId: Long) {
        viewModelScope.launch {
            vehicleRepo.getByCustomer(customerId).collect { list ->
                _uiState.update { it.copy(customerVehicles = list) }
            }
        }
    }

    fun loadMechanics() {
        viewModelScope.launch {
            authRepo.getMechanics().collect { list ->
                _uiState.update { it.copy(mechanics = list) }
            }
        }
    }

    fun loadAvailableParts() {
        viewModelScope.launch {
            partRepo.getAll().collect { list ->
                _uiState.update { it.copy(availableParts = list) }
            }
        }
    }

    // Form field change handlers
    fun onFormCustomerIdChange(value: Long?) {
        _uiState.update { it.copy(formCustomerId = value, formVehicleId = null, customerVehicles = emptyList(), formCustomerError = null) }
        value?.let { loadVehiclesForCustomer(it) }
    }
    fun onFormVehicleIdChange(value: Long?) { _uiState.update { it.copy(formVehicleId = value, formVehicleError = null) } }
    fun onFormComplaintChange(value: String) {
        if (value.length <= 500) {
            _uiState.update { it.copy(formComplaint = value, formComplaintError = null) }
        }
    }
    fun onFormDiagnosisChange(value: String) {
        if (value.length <= 500) {
            _uiState.update { it.copy(formDiagnosis = value) }
        }
    }
    fun onFormPriorityChange(value: Priority) { _uiState.update { it.copy(formPriority = value) } }
    fun onFormEntryMileageChange(value: String) {
        val filtered = value.filter { it.isDigit() }.take(7)
        _uiState.update { it.copy(formEntryMileage = filtered, formMileageError = null) }
    }
    fun onFormFuelLevelChange(value: String) { _uiState.update { it.copy(formFuelLevel = value) } }
    fun onFormChecklistNotesChange(value: String) { _uiState.update { it.copy(formChecklistNotes = value) } }

    fun onServiceLineDescriptionChange(value: String) { _uiState.update { it.copy(serviceLineFormDescription = value) } }
    fun onServiceLineLaborCostChange(value: String) { _uiState.update { it.copy(serviceLineFormLaborCost = value) } }

    fun onPartSelectedChange(value: Long?) { _uiState.update { it.copy(partFormSelectedPartId = value) } }
    fun onPartQuantityChange(value: String) { _uiState.update { it.copy(partFormQuantity = value) } }

    fun onPaymentAmountChange(value: String) { _uiState.update { it.copy(paymentFormAmount = value) } }
    fun onPaymentMethodChange(value: PaymentMethod) { _uiState.update { it.copy(paymentFormMethod = value) } }
    fun onPaymentNotesChange(value: String) { _uiState.update { it.copy(paymentFormNotes = value) } }

    // Photo management for create form
    fun prepareCameraFile(): Uri? {
        if (_uiState.value.formPhotoPaths.size >= PhotoUtils.MAX_PHOTOS) return null
        val context = getApplication<Application>()
        val file = PhotoUtils.createTempPhotoFile(context, "WO")
        pendingPhotoFile = file
        val uri = PhotoUtils.getUriForFile(context, file)
        _uiState.update { it.copy(pendingPhotoUri = uri) }
        return uri
    }

    fun onPhotoTaken(success: Boolean) {
        val file = pendingPhotoFile
        if (success && file != null && file.exists() && file.length() > 0) {
            _uiState.update {
                it.copy(formPhotoPaths = it.formPhotoPaths + file.absolutePath, pendingPhotoUri = null)
            }
        } else {
            file?.delete()
            _uiState.update { it.copy(pendingPhotoUri = null) }
        }
        pendingPhotoFile = null
    }

    fun removeFormPhoto(index: Int) {
        val paths = _uiState.value.formPhotoPaths.toMutableList()
        if (index in paths.indices) {
            PhotoUtils.deletePhoto(paths[index])
            paths.removeAt(index)
            _uiState.update { it.copy(formPhotoPaths = paths) }
        }
    }

    // Photo management for detail screen (existing orders)
    fun prepareDetailCameraFile(): Uri? {
        if (_uiState.value.detailPhotoPaths.size >= PhotoUtils.MAX_PHOTOS) return null
        val context = getApplication<Application>()
        val file = PhotoUtils.createTempPhotoFile(context, "WO")
        detailPendingPhotoFile = file
        val uri = PhotoUtils.getUriForFile(context, file)
        _uiState.update { it.copy(detailPendingPhotoUri = uri) }
        return uri
    }

    fun onDetailPhotoTaken(success: Boolean) {
        val file = detailPendingPhotoFile
        if (success && file != null && file.exists() && file.length() > 0) {
            val newPaths = _uiState.value.detailPhotoPaths + file.absolutePath
            _uiState.update { it.copy(detailPhotoPaths = newPaths, detailPendingPhotoUri = null) }
            saveOrderPhotos(newPaths)
        } else {
            file?.delete()
            _uiState.update { it.copy(detailPendingPhotoUri = null) }
        }
        detailPendingPhotoFile = null
    }

    fun removeDetailPhoto(index: Int) {
        val paths = _uiState.value.detailPhotoPaths.toMutableList()
        if (index in paths.indices) {
            PhotoUtils.deletePhoto(paths[index])
            paths.removeAt(index)
            _uiState.update { it.copy(detailPhotoPaths = paths) }
            saveOrderPhotos(paths)
        }
    }

    private fun saveOrderPhotos(paths: List<String>) {
        val order = _uiState.value.selectedOrder ?: return
        viewModelScope.launch {
            try {
                workOrderRepo.update(order.copy(photoPaths = PhotoUtils.serializePaths(paths)))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al guardar fotos") }
            }
        }
    }

    fun generatePdf(context: Context) {
        val state = _uiState.value
        val order = state.selectedOrder ?: return
        val customer = state.selectedCustomer
        val vehicle = state.selectedVehicle
        val mechanicName = state.mechanics.find { it.id == order.assignedMechanicId }?.name

        _uiState.update { it.copy(pdfGenerating = true) }

        viewModelScope.launch {
            try {
                // Fetch payments directly in case Flow hasn't emitted yet
                val directPayments = if (state.payments.isEmpty()) {
                    workOrderRepo.getPaymentsDirect(order.id)
                } else {
                    state.payments
                }
                val reportData = WorkOrderReportData(
                    order = order,
                    customerName = customer?.fullName ?: state.customerName,
                    customerPhone = customer?.phone ?: "",
                    customerEmail = customer?.email,
                    customerAddress = customer?.address,
                    vehiclePlate = vehicle?.plate ?: "",
                    vehicleBrand = vehicle?.brand ?: "",
                    vehicleModel = vehicle?.model ?: "",
                    vehicleYear = vehicle?.year,
                    vehicleColor = vehicle?.color,
                    vehicleVin = vehicle?.vin,
                    serviceLines = state.serviceLines,
                    orderParts = state.orderParts,
                    availableParts = state.availableParts,
                    payments = directPayments,
                    mechanicName = mechanicName,
                    photoPaths = state.detailPhotoPaths
                )
                val file = withContext(Dispatchers.IO) {
                    PdfReportGenerator.generateWorkOrderPdf(context, reportData)
                }
                _uiState.update { it.copy(pdfGenerating = false, pdfFile = file) }
            } catch (e: Exception) {
                _uiState.update { it.copy(pdfGenerating = false, error = e.message ?: "Error al generar PDF") }
            }
        }
    }

    fun clearPdf() {
        _uiState.update { it.copy(pdfFile = null) }
    }

    fun clearCreatedOrderId() {
        _uiState.update { it.copy(createdOrderId = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
