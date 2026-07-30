/**
 * WorkOrderViewModel.kt - ViewModel del módulo de órdenes de trabajo.
 *
 * Es el ViewModel más complejo del sistema. Gestiona:
 * - Lista de órdenes con filtro por estado.
 * - Detalle completo de una orden (servicios, repuestos, pagos, historial de estado).
 * - Formulario de creación/edición de órdenes con selección de cliente y vehículo.
 * - Cambios de estado con registro de auditoría.
 * - Gestión de fotos y archivos adjuntos.
 * - Generación de reportes PDF.
 * - Eliminación de órdenes con restauración de stock.
 */
package com.example.serviaux.ui.workorders

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.serviaux.ServiauxApp
import com.example.serviaux.data.dao.WorkOrderPaymentSummary
import com.example.serviaux.data.entity.*
import com.example.serviaux.util.AppPreferences
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
import java.util.Calendar
import java.util.Locale

/** Filtro por estado de pago de la orden, aplicado sobre la lista. */
enum class PaymentFilter(val displayName: String) {
    TODAS("Todas"),
    PAGADAS("Pagadas"),
    PARCIALES("Parciales"),
    NO_PAGADAS("Sin pagar")
}

data class WorkOrderUiState(
    val orders: List<WorkOrder> = emptyList(),
    val selectedOrder: WorkOrder? = null,
    val serviceLines: List<ServiceLine> = emptyList(),
    val orderParts: List<WorkOrderPart> = emptyList(),
    val payments: List<WorkOrderPayment> = emptyList(),
    val statusLog: List<WorkOrderStatusLog> = emptyList(),
    val orderMechanics: List<WorkOrderMechanic> = emptyList(),
    val availableParts: List<Part> = emptyList(),
    val mechanics: List<User> = emptyList(),
    val customers: List<Customer> = emptyList(),
    val customerVehicles: List<Vehicle> = emptyList(),
    val vehicleName: String = "",
    val customerName: String = "",
    val vehicleMap: Map<Long, String> = emptyMap(),
    val customerMap: Map<Long, String> = emptyMap(),
    val filter: OrderStatus? = null,
    val filterYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val availableYears: List<Int> = (2024..Calendar.getInstance().get(Calendar.YEAR)).toList(),
    val paymentFilter: PaymentFilter = PaymentFilter.TODAS,
    /** Mapa orderId -> (totalPagado, totalDescuentos) para mostrar abono y saldo en la lista. */
    val paymentSummaryMap: Map<Long, Pair<Double, Double>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
    // Create/edit order form
    val formCustomerId: Long? = null,
    val formCustomerSearch: String = "",
    val formVehicleId: Long? = null,
    val formComplaint: String = "",
    val formPriority: Priority = Priority.MEDIA,
    val formOrderType: OrderType = OrderType.SERVICIO_NUEVO,
    val formArrivalCondition: ArrivalCondition = ArrivalCondition.RODANDO,
    val formEntryMileage: String = "",
    val formFuelLevel: String = "1/2",
    val formChecklistNotes: String = "",
    val formDeliveryNote: String = "",
    val formInvoiceNumber: String = "",
    val formNotes: String = "",
    // Checklist
    val formChecklist: Map<String, Boolean> = emptyMap(),
    val catalogAccessories: List<CatalogAccessory> = emptyList(),
    // Complaint/Diagnosis catalogs
    val catalogComplaints: List<CatalogComplaint> = emptyList(),
    val catalogDiagnoses: List<CatalogDiagnosis> = emptyList(),
    val selectedComplaintId: Long? = null,
    // Edit mode
    val isEditing: Boolean = false,
    val editingOrderId: Long? = null,
    // Service line form
    val serviceLineFormDescription: String = "",
    val serviceLineFormLaborCost: String = "",
    val serviceLineFormHasDiscount: Boolean = false,
    val serviceLineFormDiscount: String = "",
    val editingServiceLineId: Long? = null,
    // Part form
    val partFormSelectedPartId: Long? = null,
    val partFormQuantity: String = "",
    val partFormPrice: String = "",
    val partFormHasDiscount: Boolean = false,
    val partFormDiscount: String = "",
    val editingWorkOrderPartId: Long? = null,
    // Payment form
    val paymentFormAmount: String = "",
    val paymentFormDiscount: String = "",
    val paymentFormMethod: PaymentMethod = PaymentMethod.EFECTIVO,
    val paymentFormNotes: String = "",
    /** Pago que se está corrigiendo; null si se está registrando uno nuevo. */
    val editingPaymentId: Long? = null,
    val createdOrderId: Long? = null,
    // Form validation errors
    val formCustomerError: String? = null,
    val formVehicleError: String? = null,
    val formComplaintError: String? = null,
    val catalogServices: List<CatalogService> = emptyList(),
    val formMileageError: String? = null,
    val formPhotoPaths: List<String> = emptyList(),
    val pendingPhotoUri: Uri? = null,
    val formFilePaths: List<String> = emptyList(),
    val detailPhotoPaths: List<String> = emptyList(),
    val detailPendingPhotoUri: Uri? = null,
    val detailFilePaths: List<String> = emptyList(),
    // Editable detail fields
    val detailEntryMileage: String = "",
    val detailFuelLevel: String = "",
    val detailDeliveryNote: String = "",
    val detailInvoiceNumber: String = "",
    val detailNotes: String = "",
    val selectedCustomer: Customer? = null,
    val selectedVehicle: Vehicle? = null,
    val pdfGenerating: Boolean = false,
    val pdfFile: File? = null,
    val orderDeleted: Boolean = false,
    val isAdmin: Boolean = false,
    val isListLoaded: Boolean = false,
    val searchQuery: String = "",
    val filteredOrders: List<WorkOrder> = emptyList(),
    val serviceDescriptionsMap: Map<Long, List<String>> = emptyMap(),
    val partNamesMap: Map<Long, List<String>> = emptyMap(),
    val formAdmissionDate: Long? = System.currentTimeMillis(),
    val formAppointmentId: Long? = null,
    // Extras
    val orderExtras: List<WorkOrderExtra> = emptyList(),
    val extraFormDescription: String = "",
    val extraFormCost: String = "",
    val extraFormHasDiscount: Boolean = false,
    val extraFormDiscount: String = "",
    val extraFormCategory: String? = null,
    val editingExtraId: Long? = null
)

class WorkOrderViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                val savedStateHandle = createSavedStateHandle()
                WorkOrderViewModel(app, savedStateHandle)
            }
        }
    }

    private val app get() = getApplication<ServiauxApp>()
    private val workOrderRepo get() = app.container.workOrderRepository
    private val customerRepo get() = app.container.customerRepository
    private val vehicleRepo get() = app.container.vehicleRepository
    private val partRepo get() = app.container.partRepository
    private val authRepo get() = app.container.authRepository
    private val catalogRepo get() = app.container.catalogRepository
    private val appointmentRepo get() = app.container.appointmentRepository
    private val session get() = app.container.sessionManager

    private val _uiState = MutableStateFlow(WorkOrderUiState())
    val uiState: StateFlow<WorkOrderUiState> = _uiState.asStateFlow()

    private var ordersJob: Job? = null
    private var diagnosesJob: Job? = null
    private var vehiclesJob: Job? = null
    private var pendingPhotoFile: File? = savedStateHandle.get<String>("pendingPhotoFile")?.let { File(it) }
    private var detailPendingPhotoFile: File? = savedStateHandle.get<String>("detailPendingPhotoFile")?.let { File(it) }

    /**
     * Id de la orden cuyos campos de "Datos del Proceso" ya se cargaron desde la BD.
     * Evita que las re-emisiones del Flow sobreescriban lo que el usuario está escribiendo.
     */
    private var detailFieldsLoadedForOrder: Long? = null

    init {
        _uiState.update { it.copy(isAdmin = session.isAdmin()) }
        restoreFormState()
        loadOrders()
        loadVehicleAndCustomerMaps()
        loadSearchMaps()
        loadCatalogServices()
        loadCatalogComplaints()
        loadCatalogDiagnoses()
        loadCatalogAccessories()
        loadPaymentSummaries()
    }

    private fun loadPaymentSummaries() {
        viewModelScope.launch {
            workOrderRepo.getAllPaymentSummaries().collect { summaries ->
                val map = summaries.associate { it.workOrderId to (it.totalPaid to it.totalDiscount) }
                _uiState.update { it.copy(paymentSummaryMap = map) }
                applySearchFilter()
            }
        }
    }

    private fun restoreFormState() {
        val hasSavedState = savedStateHandle.get<String>("formComplaint") != null
        if (!hasSavedState) return
        _uiState.update {
            it.copy(
                formCustomerId = savedStateHandle.get<Long>("formCustomerId"),
                formCustomerSearch = savedStateHandle.get<String>("formCustomerSearch") ?: "",
                formVehicleId = savedStateHandle.get<Long>("formVehicleId"),
                formComplaint = savedStateHandle.get<String>("formComplaint") ?: "",
                formPriority = savedStateHandle.get<String>("formPriority")?.let { p -> try { Priority.valueOf(p) } catch (_: Exception) { null } } ?: Priority.MEDIA,
                formOrderType = savedStateHandle.get<String>("formOrderType")?.let { t -> try { OrderType.valueOf(t) } catch (_: Exception) { null } } ?: OrderType.SERVICIO_NUEVO,
                formArrivalCondition = savedStateHandle.get<String>("formArrivalCondition")?.let { a -> try { ArrivalCondition.valueOf(a) } catch (_: Exception) { null } } ?: ArrivalCondition.RODANDO,
                formEntryMileage = savedStateHandle.get<String>("formEntryMileage") ?: "",
                formFuelLevel = savedStateHandle.get<String>("formFuelLevel") ?: "",
                formChecklistNotes = savedStateHandle.get<String>("formChecklistNotes") ?: "",
                formDeliveryNote = savedStateHandle.get<String>("formDeliveryNote") ?: "",
                formInvoiceNumber = savedStateHandle.get<String>("formInvoiceNumber") ?: "",
                formNotes = savedStateHandle.get<String>("formNotes") ?: "",
                formPhotoPaths = savedStateHandle.get<ArrayList<String>>("formPhotoPaths")?.toList() ?: emptyList(),
                formFilePaths = savedStateHandle.get<ArrayList<String>>("formFilePaths")?.toList() ?: emptyList(),
                formAdmissionDate = savedStateHandle.get<Long>("formAdmissionDate"),
                isEditing = savedStateHandle.get<Boolean>("isEditing") ?: false,
                editingOrderId = savedStateHandle.get<Long>("editingOrderId"),
                detailEntryMileage = savedStateHandle.get<String>("detailEntryMileage") ?: "",
                detailFuelLevel = savedStateHandle.get<String>("detailFuelLevel") ?: "",
                detailDeliveryNote = savedStateHandle.get<String>("detailDeliveryNote") ?: "",
                detailInvoiceNumber = savedStateHandle.get<String>("detailInvoiceNumber") ?: "",
                detailNotes = savedStateHandle.get<String>("detailNotes") ?: "",
                detailPhotoPaths = savedStateHandle.get<ArrayList<String>>("detailPhotoPaths")?.toList() ?: emptyList()
            )
        }
    }

    private fun saveFormState() {
        val state = _uiState.value
        savedStateHandle["formCustomerId"] = state.formCustomerId
        savedStateHandle["formCustomerSearch"] = state.formCustomerSearch
        savedStateHandle["formVehicleId"] = state.formVehicleId
        savedStateHandle["formComplaint"] = state.formComplaint
        savedStateHandle["formPriority"] = state.formPriority.name
        savedStateHandle["formOrderType"] = state.formOrderType.name
        savedStateHandle["formArrivalCondition"] = state.formArrivalCondition.name
        savedStateHandle["formEntryMileage"] = state.formEntryMileage
        savedStateHandle["formFuelLevel"] = state.formFuelLevel
        savedStateHandle["formChecklistNotes"] = state.formChecklistNotes
        savedStateHandle["formDeliveryNote"] = state.formDeliveryNote
        savedStateHandle["formInvoiceNumber"] = state.formInvoiceNumber
        savedStateHandle["formNotes"] = state.formNotes
        savedStateHandle["formPhotoPaths"] = ArrayList(state.formPhotoPaths)
        savedStateHandle["formFilePaths"] = ArrayList(state.formFilePaths)
        savedStateHandle["formAdmissionDate"] = state.formAdmissionDate
        savedStateHandle["isEditing"] = state.isEditing
        savedStateHandle["editingOrderId"] = state.editingOrderId
        savedStateHandle["detailEntryMileage"] = state.detailEntryMileage
        savedStateHandle["detailFuelLevel"] = state.detailFuelLevel
        savedStateHandle["detailDeliveryNote"] = state.detailDeliveryNote
        savedStateHandle["detailInvoiceNumber"] = state.detailInvoiceNumber
        savedStateHandle["detailNotes"] = state.detailNotes
        savedStateHandle["detailPhotoPaths"] = ArrayList(state.detailPhotoPaths)
    }

    private fun clearSavedState() {
        listOf(
            "pendingPhotoFile", "detailPendingPhotoFile",
            "formCustomerId", "formCustomerSearch", "formVehicleId", "formComplaint",
            "formPriority", "formOrderType", "formArrivalCondition",
            "formEntryMileage", "formFuelLevel", "formChecklistNotes",
            "formDeliveryNote", "formInvoiceNumber", "formNotes",
            "formPhotoPaths", "formFilePaths", "formAdmissionDate",
            "isEditing", "editingOrderId",
            "detailEntryMileage", "detailFuelLevel", "detailDeliveryNote",
            "detailInvoiceNumber", "detailNotes", "detailPhotoPaths"
        ).forEach { savedStateHandle.remove<Any>(it) }
    }

    private fun loadVehicleAndCustomerMaps() {
        viewModelScope.launch {
            vehicleRepo.getAll().collect { vehicles ->
                _uiState.update { it.copy(vehicleMap = vehicles.associate { v -> v.id to "${v.plate} - ${v.brand} ${v.model}" }) }
            }
        }
        viewModelScope.launch {
            customerRepo.getAll().collect { customers ->
                _uiState.update { it.copy(customerMap = customers.associate { c -> c.id to c.fullName }) }
            }
        }
    }

    private fun loadSearchMaps() {
        viewModelScope.launch {
            val serviceDescs = workOrderRepo.getAllServiceDescriptions()
            val partNames = workOrderRepo.getAllPartNames()
            _uiState.update {
                it.copy(
                    serviceDescriptionsMap = serviceDescs.groupBy({ it.workOrderId }, { it.description }),
                    partNamesMap = partNames.groupBy({ it.workOrderId }, { it.partName })
                )
            }
            applySearchFilter()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applySearchFilter()
    }

    fun onPaymentFilterChanged(filter: PaymentFilter) {
        _uiState.update { it.copy(paymentFilter = filter) }
        applySearchFilter()
    }

    private fun applySearchFilter() {
        val state = _uiState.value
        val terms = state.searchQuery.uppercase().split(" ").filter { it.isNotBlank() }
        val byText = if (terms.isEmpty()) state.orders else state.orders.filter { order ->
            val searchText = buildString {
                append(state.customerMap[order.customerId] ?: "")
                append(" ")
                append(state.vehicleMap[order.vehicleId] ?: "")
            }.uppercase()
            terms.all { term -> searchText.contains(term) }
        }
        val filtered = byText.filter { order -> matchesPaymentFilter(order, state) }
        _uiState.update { it.copy(filteredOrders = filtered) }
    }

    private fun matchesPaymentFilter(order: WorkOrder, state: WorkOrderUiState): Boolean {
        if (state.paymentFilter == PaymentFilter.TODAS) return true
        val (paid, discount) = state.paymentSummaryMap[order.id] ?: (0.0 to 0.0)
        val balance = order.total - paid - discount
        return when (state.paymentFilter) {
            PaymentFilter.PAGADAS -> order.total > 0.0 && balance <= 0.01
            PaymentFilter.NO_PAGADAS -> paid <= 0.0 && discount <= 0.0
            PaymentFilter.PARCIALES -> (paid > 0.0 || discount > 0.0) && balance > 0.01
            PaymentFilter.TODAS -> true
        }
    }

    private fun loadCatalogServices() {
        viewModelScope.launch {
            catalogRepo.getAllServices().collect { services ->
                _uiState.update { it.copy(catalogServices = services) }
            }
        }
    }

    private fun loadCatalogComplaints() {
        viewModelScope.launch {
            catalogRepo.getAllComplaints().collect { complaints ->
                _uiState.update { it.copy(catalogComplaints = complaints) }
            }
        }
    }

    private fun loadCatalogDiagnoses() {
        viewModelScope.launch {
            catalogRepo.getAllDiagnoses().collect { diagnoses ->
                _uiState.update { it.copy(catalogDiagnoses = diagnoses) }
            }
        }
    }

    private fun loadCatalogAccessories() {
        viewModelScope.launch {
            catalogRepo.getAllAccessories().collect { accessories ->
                _uiState.update { it.copy(catalogAccessories = accessories) }
            }
        }
    }

    private fun yearToDateRange(year: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(year, Calendar.JANUARY, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val from = cal.timeInMillis
        cal.set(year, Calendar.DECEMBER, 31, 23, 59, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val to = cal.timeInMillis
        return from to to
    }

    fun loadOrders(filter: OrderStatus? = _uiState.value.filter, year: Int? = _uiState.value.filterYear) {
        _uiState.update { it.copy(filter = filter, filterYear = year ?: Calendar.getInstance().get(Calendar.YEAR)) }
        loadSearchMaps()
        ordersJob?.cancel()
        ordersJob = viewModelScope.launch {
            val flow = when {
                year != null && filter != null -> {
                    val (from, to) = yearToDateRange(year)
                    workOrderRepo.getByStatusAndDateRange(filter, from, to)
                }
                year != null -> {
                    val (from, to) = yearToDateRange(year)
                    workOrderRepo.getByDateRange(from, to)
                }
                filter != null -> workOrderRepo.getByStatus(filter)
                else -> workOrderRepo.getAll()
            }
            flow.collect { list ->
                _uiState.update { it.copy(orders = list, isListLoaded = true) }
                applySearchFilter()
            }
        }
    }

    fun loadOrderDetail(orderId: Long) {
        detailFieldsLoadedForOrder = null
        viewModelScope.launch {
            workOrderRepo.getById(orderId).collect { order ->
                // El Flow re-emite cada vez que la fila cambia (recálculo de totales, cambio de
                // estado, fotos, el propio autoguardado). Los campos de "Datos del Proceso" solo
                // se rellenan desde la BD la primera vez que se carga la orden: si se
                // reescribieran en cada emisión, lo que el usuario está tecleando (típicamente el
                // kilometraje, aún sin guardar) se borraría delante de él, y el guardado por
                // pérdida de foco terminaría persistiendo el campo vacío.
                val isFirstLoad = detailFieldsLoadedForOrder != orderId
                _uiState.update { state ->
                    // Se adopta el valor de la BD salvo que el campo tenga una edición pendiente
                    // (el texto local difiere de lo que la BD tenía antes de esta emisión).
                    fun resolve(local: String, previous: String, incoming: String): String =
                        if (isFirstLoad || local == previous) incoming else local

                    val previous = state.selectedOrder
                    state.copy(
                        selectedOrder = order,
                        detailPhotoPaths = PhotoUtils.parsePaths(order?.photoPaths),
                        detailFilePaths = PhotoUtils.parsePaths(order?.filePaths),
                        detailEntryMileage = resolve(
                            state.detailEntryMileage,
                            previous?.entryMileage?.toString() ?: "",
                            order?.entryMileage?.toString() ?: ""
                        ),
                        detailFuelLevel = resolve(
                            state.detailFuelLevel,
                            previous?.fuelLevel ?: "1/2",
                            order?.fuelLevel ?: "1/2"
                        ),
                        detailDeliveryNote = resolve(
                            state.detailDeliveryNote,
                            previous?.deliveryNote ?: "",
                            order?.deliveryNote ?: ""
                        ),
                        detailInvoiceNumber = resolve(
                            state.detailInvoiceNumber,
                            previous?.invoiceNumber ?: "",
                            order?.invoiceNumber ?: ""
                        ),
                        detailNotes = resolve(
                            state.detailNotes,
                            previous?.notes ?: "",
                            order?.notes ?: ""
                        )
                    )
                }
                if (order != null) detailFieldsLoadedForOrder = orderId
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
        viewModelScope.launch {
            workOrderRepo.getOrderMechanics(orderId).collect { mechanics ->
                _uiState.update { it.copy(orderMechanics = mechanics) }
            }
        }
        viewModelScope.launch {
            workOrderRepo.getWorkOrderExtras(orderId).collect { extras ->
                _uiState.update { it.copy(orderExtras = extras) }
            }
        }
    }

    // Edit mode: prepare form with existing order data
    fun prepareEdit(orderId: Long) {
        clearSavedState()
        viewModelScope.launch {
            val order = workOrderRepo.getByIdDirect(orderId) ?: return@launch
            val customer = customerRepo.getByIdDirect(order.customerId)
            val vehicle = vehicleRepo.getByIdDirect(order.vehicleId)

            // Parse checklist from comma-separated string
            val checkedItems = order.checklistNotes?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
            val accessories = _uiState.value.catalogAccessories
            val checklistMap = accessories.associate { it.name to (it.name in checkedItems) }

            _uiState.update {
                it.copy(
                    isEditing = true,
                    editingOrderId = orderId,
                    formCustomerId = order.customerId,
                    formCustomerSearch = customer?.fullName ?: "",
                    formVehicleId = order.vehicleId,
                    formComplaint = order.customerComplaint,
                    formPriority = order.priority,
                    formOrderType = order.orderType,
                    formArrivalCondition = order.arrivalCondition,
                    formEntryMileage = order.entryMileage?.toString() ?: "",
                    formFuelLevel = order.fuelLevel ?: "",
                    formChecklist = checklistMap,
                    formPhotoPaths = PhotoUtils.parsePaths(order.photoPaths),
                    formFilePaths = PhotoUtils.parsePaths(order.filePaths),
                    formDeliveryNote = order.deliveryNote ?: "",
                    formInvoiceNumber = order.invoiceNumber ?: "",
                    formNotes = order.notes ?: "",
                    formAdmissionDate = order.admissionDate,
                    customerName = customer?.fullName ?: "",
                    vehicleName = vehicle?.let { v -> "${v.plate} - ${v.brand} ${v.model}" } ?: "",
                    formCustomerError = null,
                    formVehicleError = null,
                    formComplaintError = null,
                    formMileageError = null
                )
            }

            // Try to match complaint to catalog for diagnosis suggestions
            val complaints = _uiState.value.catalogComplaints
            val matchedComplaint = complaints.find { it.name.equals(order.customerComplaint.trim(), ignoreCase = true) }
            if (matchedComplaint != null) {
                _uiState.update { it.copy(selectedComplaintId = matchedComplaint.id) }
            }
        }
    }

    fun updateOrder() {
        val state = _uiState.value
        val orderId = state.editingOrderId ?: return
        if (!session.isLoggedIn) {
            _uiState.update { it.copy(error = "Debe iniciar sesión") }
            return
        }

        val complaintError = validateComplaint()
        val mileageError = validateEntryMileage()

        _uiState.update {
            it.copy(formComplaintError = complaintError, formMileageError = mileageError)
        }

        if (complaintError != null || mileageError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val existing = workOrderRepo.getByIdDirect(orderId) ?: return@launch
                val checkedItems = state.formChecklist.filter { it.value }.keys.joinToString(",")
                val updated = existing.copy(
                    admissionDate = state.formAdmissionDate,
                    customerComplaint = state.formComplaint.trim(),
                    priority = state.formPriority,
                    orderType = state.formOrderType,
                    arrivalCondition = state.formArrivalCondition,
                    entryMileage = state.formEntryMileage.toIntOrNull(),
                    fuelLevel = state.formFuelLevel.trim().ifBlank { null },
                    checklistNotes = checkedItems.ifBlank { null },
                    photoPaths = PhotoUtils.serializePaths(state.formPhotoPaths),
                    filePaths = PhotoUtils.serializePaths(state.formFilePaths),
                    deliveryNote = state.formDeliveryNote.trim().ifBlank { null },
                    invoiceNumber = state.formInvoiceNumber.trim().ifBlank { null },
                    notes = state.formNotes.trim().ifBlank { null },
                    updatedBy = session.currentUserId
                )
                workOrderRepo.update(updated)
                clearSavedState()
                _uiState.update { it.copy(isLoading = false, createdOrderId = orderId) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al actualizar orden") }
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
                val checkedItems = state.formChecklist.filter { it.value }.keys.joinToString(",")
                val order = WorkOrder(
                    customerId = state.formCustomerId!!,
                    vehicleId = state.formVehicleId!!,
                    admissionDate = state.formAdmissionDate ?: System.currentTimeMillis(),
                    customerComplaint = state.formComplaint.trim(),
                    priority = state.formPriority,
                    orderType = state.formOrderType,
                    arrivalCondition = state.formArrivalCondition,
                    entryMileage = state.formEntryMileage.toIntOrNull(),
                    fuelLevel = state.formFuelLevel.trim().ifBlank { null },
                    checklistNotes = checkedItems.ifBlank { null },
                    photoPaths = PhotoUtils.serializePaths(state.formPhotoPaths),
                    filePaths = PhotoUtils.serializePaths(state.formFilePaths),
                    deliveryNote = state.formDeliveryNote.trim().ifBlank { null },
                    invoiceNumber = state.formInvoiceNumber.trim().ifBlank { null },
                    notes = state.formNotes.trim().ifBlank { null },
                    createdBy = userId,
                    updatedBy = userId
                )
                val orderId = workOrderRepo.insert(order)
                assignDefaultMechanic(orderId)
                // Mark appointment as converted if applicable
                state.formAppointmentId?.let { appointmentId ->
                    appointmentRepo.markConverted(appointmentId, orderId)
                }
                clearSavedState()
                _uiState.update { it.copy(isLoading = false, createdOrderId = orderId) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al crear orden") }
            }
        }
    }

    fun saveOrder() {
        if (_uiState.value.isEditing) updateOrder() else createOrder()
    }

    fun changeStatus(newStatus: OrderStatus, note: String? = null) {
        val state = _uiState.value
        val orderId = state.selectedOrder?.id ?: return

        // Validar mecánico asignado para cerrar orden
        if (newStatus in listOf(OrderStatus.LISTO, OrderStatus.ENTREGADO)
            && state.orderMechanics.isEmpty()) {
            _uiState.update { it.copy(error = "Debe asignar al menos un mecánico antes de marcar como ${newStatus.displayName}") }
            return
        }

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

    /**
     * Asigna a la orden recién creada el mecánico configurado por defecto, con su tipo y valor
     * de comisión. Es editable después en el detalle.
     *
     * Si no hay mecánico por defecto, si ya no existe o si está desactivado, la orden se queda
     * sin mecánico y no se interrumpe la creación: la validación de LISTO/ENTREGADO obligará a
     * asignar uno más adelante.
     */
    private suspend fun assignDefaultMechanic(orderId: Long) {
        val mechanicId = AppPreferences.defaultMechanicId(getApplication()) ?: return
        try {
            val mechanic = authRepo.getUserByIdDirect(mechanicId) ?: return
            if (!mechanic.active || mechanic.role != UserRole.MECANICO) return
            // Una orden nueva no tiene mano de obra todavía, así que una comisión porcentual
            // arranca en 0 y se recalcula al agregar servicios.
            val commissionAmount = when (mechanic.commissionType) {
                CommissionType.FIJA.name -> mechanic.commissionValue
                else -> 0.0
            }
            workOrderRepo.addMechanicToOrder(
                WorkOrderMechanic(
                    workOrderId = orderId,
                    mechanicId = mechanicId,
                    commissionType = mechanic.commissionType,
                    commissionValue = mechanic.commissionValue,
                    commissionAmount = commissionAmount
                )
            )
            workOrderRepo.assignMechanic(orderId, mechanicId, session.currentUserId)
        } catch (_: Exception) {
            // No se bloquea la creación de la orden por no poder preasignar el mecánico.
        }
    }

    fun addMechanicToOrder(mechanicId: Long, commissionType: String, commissionValue: Double) {
        val state = _uiState.value
        val orderId = state.selectedOrder?.id ?: return
        val totalLabor = state.selectedOrder.totalLabor
        val commissionAmount = when (commissionType) {
            "PORCENTAJE" -> totalLabor * (commissionValue / 100.0)
            "FIJA" -> commissionValue
            else -> 0.0
        }
        viewModelScope.launch {
            try {
                workOrderRepo.addMechanicToOrder(
                    WorkOrderMechanic(
                        workOrderId = orderId,
                        mechanicId = mechanicId,
                        commissionType = commissionType,
                        commissionValue = commissionValue,
                        commissionAmount = commissionAmount
                    )
                )
                // Also set as assigned mechanic if first one
                if (state.orderMechanics.isEmpty()) {
                    workOrderRepo.assignMechanic(orderId, mechanicId, session.currentUserId)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al agregar mec\u00e1nico") }
            }
        }
    }

    fun removeMechanicFromOrder(mechanic: WorkOrderMechanic) {
        viewModelScope.launch {
            try {
                workOrderRepo.removeMechanicFromOrder(mechanic)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al eliminar mec\u00e1nico") }
            }
        }
    }

    fun toggleCommissionPaid(mechanic: WorkOrderMechanic) {
        viewModelScope.launch {
            try {
                workOrderRepo.toggleCommissionPaid(mechanic)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al actualizar comisi\u00f3n") }
            }
        }
    }

    // El recálculo de comisiones vive en WorkOrderRepository.recalculateTotals: es el único
    // punto por el que pasan todos los cambios de mano de obra, y así no depende de que la
    // pantalla se acuerde de invocarlo.

    // Service line: add or update
    fun saveServiceLine() {
        val state = _uiState.value
        val orderId = state.selectedOrder?.id ?: return
        if (state.serviceLineFormDescription.isBlank()) {
            _uiState.update { it.copy(error = "Ingrese descripcion del servicio") }
            return
        }
        val discount = if (state.serviceLineFormHasDiscount)
            state.serviceLineFormDiscount.toDoubleOrNull() ?: 0.0 else 0.0
        val laborCost = state.serviceLineFormLaborCost.toDoubleOrNull() ?: 0.0
        if (discount > laborCost) {
            _uiState.update { it.copy(error = "El descuento no puede exceder el costo") }
            return
        }
        viewModelScope.launch {
            try {
                val editingId = state.editingServiceLineId
                if (editingId != null) {
                    val existing = state.serviceLines.find { it.id == editingId } ?: return@launch
                    workOrderRepo.updateServiceLine(
                        existing.copy(
                            description = state.serviceLineFormDescription.trim(),
                            laborCost = state.serviceLineFormLaborCost.toDoubleOrNull() ?: 0.0,
                            discount = discount
                        )
                    )
                } else {
                    val serviceLine = ServiceLine(
                        workOrderId = orderId,
                        description = state.serviceLineFormDescription.trim(),
                        laborCost = state.serviceLineFormLaborCost.toDoubleOrNull() ?: 0.0,
                        discount = discount
                    )
                    workOrderRepo.addServiceLine(serviceLine)
                }
                _uiState.update {
                    it.copy(
                        serviceLineFormDescription = "",
                        serviceLineFormLaborCost = "",
                        serviceLineFormHasDiscount = false,
                        serviceLineFormDiscount = "",
                        editingServiceLineId = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al guardar servicio") }
            }
        }
    }

    fun addServiceLine() = saveServiceLine()

    fun startEditServiceLine(serviceLine: ServiceLine) {
        _uiState.update {
            it.copy(
                editingServiceLineId = serviceLine.id,
                serviceLineFormDescription = serviceLine.description,
                serviceLineFormLaborCost = String.format(Locale.US, "%.2f", serviceLine.laborCost),
                serviceLineFormHasDiscount = serviceLine.discount > 0.0,
                serviceLineFormDiscount = if (serviceLine.discount > 0.0) String.format(Locale.US, "%.2f", serviceLine.discount) else ""
            )
        }
    }

    fun cancelEditServiceLine() {
        _uiState.update {
            it.copy(
                editingServiceLineId = null,
                serviceLineFormDescription = "",
                serviceLineFormLaborCost = "",
                serviceLineFormHasDiscount = false,
                serviceLineFormDiscount = ""
            )
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
        if (quantity < 1) {
            _uiState.update { it.copy(error = "La cantidad debe ser al menos 1") }
            return
        }
        val unitPrice = state.partFormPrice.toDoubleOrNull() ?: run {
            _uiState.update { it.copy(error = "Ingrese precio valido") }
            return
        }
        val discount = if (_uiState.value.partFormHasDiscount)
            _uiState.value.partFormDiscount.toDoubleOrNull() ?: 0.0 else 0.0
        val subtotal = unitPrice * quantity
        if (discount > subtotal) {
            _uiState.update { it.copy(error = "El descuento no puede exceder el subtotal") }
            return
        }
        viewModelScope.launch {
            try {
                val workOrderPart = WorkOrderPart(
                    workOrderId = orderId,
                    partId = partId,
                    quantity = quantity,
                    appliedUnitPrice = unitPrice,
                    subtotal = subtotal,
                    discount = discount
                )
                val result = workOrderRepo.addWorkOrderPart(workOrderPart)
                _uiState.update {
                    it.copy(
                        partFormSelectedPartId = null,
                        partFormQuantity = "",
                        partFormPrice = "",
                        partFormHasDiscount = false,
                        partFormDiscount = "",
                        error = stockShortfallMessage(result.stockShortfall)
                    )
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

    fun startEditPart(workOrderPart: WorkOrderPart) {
        _uiState.update {
            it.copy(
                editingWorkOrderPartId = workOrderPart.id,
                partFormSelectedPartId = workOrderPart.partId,
                partFormQuantity = workOrderPart.quantity.toString(),
                partFormPrice = String.format(Locale.US, "%.2f", workOrderPart.appliedUnitPrice),
                partFormHasDiscount = workOrderPart.discount > 0.0,
                partFormDiscount = if (workOrderPart.discount > 0.0) String.format(Locale.US, "%.2f", workOrderPart.discount) else ""
            )
        }
    }

    fun cancelEditPart() {
        _uiState.update {
            it.copy(
                editingWorkOrderPartId = null,
                partFormSelectedPartId = null,
                partFormQuantity = "",
                partFormPrice = "",
                partFormHasDiscount = false,
                partFormDiscount = ""
            )
        }
    }

    // ── Extras ──────────────────────────────────────────────────────
    fun onExtraFormDescriptionChange(value: String) { _uiState.update { it.copy(extraFormDescription = value) } }
    fun onExtraFormCostChange(value: String) { _uiState.update { it.copy(extraFormCost = value) } }
    fun onExtraFormDiscountToggle(value: Boolean) { _uiState.update { it.copy(extraFormHasDiscount = value, extraFormDiscount = if (!value) "" else it.extraFormDiscount) } }
    fun onExtraFormDiscountChange(value: String) { _uiState.update { it.copy(extraFormDiscount = value) } }
    fun onExtraFormCategoryChange(value: String?) { _uiState.update { it.copy(extraFormCategory = value) } }

    fun saveExtra() {
        val state = _uiState.value
        val orderId = state.selectedOrder?.id ?: return
        if (state.extraFormDescription.isBlank()) {
            _uiState.update { it.copy(error = "Ingrese descripción del extra") }
            return
        }
        val cost = state.extraFormCost.toDoubleOrNull() ?: 0.0
        val discount = if (state.extraFormHasDiscount) state.extraFormDiscount.toDoubleOrNull() ?: 0.0 else 0.0
        if (discount > cost) {
            _uiState.update { it.copy(error = "El descuento no puede exceder el costo") }
            return
        }
        viewModelScope.launch {
            try {
                val editingId = state.editingExtraId
                if (editingId != null) {
                    val existing = state.orderExtras.find { it.id == editingId } ?: return@launch
                    workOrderRepo.updateWorkOrderExtra(
                        existing.copy(
                            description = state.extraFormDescription.trim(),
                            cost = cost,
                            discount = discount,
                            category = state.extraFormCategory,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                } else {
                    workOrderRepo.addWorkOrderExtra(
                        WorkOrderExtra(
                            workOrderId = orderId,
                            description = state.extraFormDescription.trim(),
                            cost = cost,
                            discount = discount,
                            category = state.extraFormCategory
                        )
                    )
                }
                clearExtraForm()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al guardar extra") }
            }
        }
    }

    fun startEditExtra(extra: WorkOrderExtra) {
        _uiState.update {
            it.copy(
                editingExtraId = extra.id,
                extraFormDescription = extra.description,
                extraFormCost = String.format(Locale.US, "%.2f", extra.cost),
                extraFormHasDiscount = extra.discount > 0.0,
                extraFormDiscount = if (extra.discount > 0.0) String.format(Locale.US, "%.2f", extra.discount) else "",
                extraFormCategory = extra.category
            )
        }
    }

    fun cancelEditExtra() { clearExtraForm() }

    private fun clearExtraForm() {
        _uiState.update {
            it.copy(
                editingExtraId = null,
                extraFormDescription = "",
                extraFormCost = "",
                extraFormHasDiscount = false,
                extraFormDiscount = "",
                extraFormCategory = null
            )
        }
    }

    fun deleteExtra(extra: WorkOrderExtra) {
        viewModelScope.launch {
            try {
                workOrderRepo.deleteWorkOrderExtra(extra)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al eliminar extra") }
            }
        }
    }

    /**
     * Crea un repuesto en el catálogo desde el diálogo de la orden y lo deja seleccionado.
     *
     * Evita tener que salir de la orden para dar de alta una pieza que no estaba registrada:
     * el repuesto queda en el inventario y listo para agregarse a la orden en el mismo paso.
     *
     * @param stock Existencia inicial. Se registra tal cual; el descuento por la línea de la
     *   orden se aplica después, al agregar el repuesto.
     */
    fun createPartFromOrder(
        name: String,
        code: String,
        brand: String,
        salePrice: String,
        stock: String
    ) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            _uiState.update { it.copy(error = "El nombre del repuesto es obligatorio") }
            return
        }
        val price = salePrice.replace(',', '.').toDoubleOrNull()
        if (price == null || price < 0.0) {
            _uiState.update { it.copy(error = "Ingrese un precio de venta valido") }
            return
        }
        val initialStock = stock.filter { it.isDigit() }.toIntOrNull() ?: 0
        viewModelScope.launch {
            try {
                val newId = partRepo.insert(
                    Part(
                        name = cleanName.uppercase(),
                        code = code.trim().ifBlank { null },
                        brand = brand.trim().ifBlank { null }?.uppercase(),
                        // Sin costo de adquisición conocido se toma el precio de venta como
                        // referencia; el administrador lo corrige después en Repuestos.
                        unitCost = price,
                        salePrice = price,
                        currentStock = initialStock
                    )
                )
                // Queda seleccionado en el diálogo con su precio, listo para agregar a la orden.
                _uiState.update {
                    it.copy(
                        partFormSelectedPartId = newId,
                        partFormPrice = String.format(Locale.US, "%.2f", price),
                        partFormQuantity = it.partFormQuantity.ifBlank { "1" }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al crear el repuesto") }
            }
        }
    }

    /** Mensaje de aviso cuando el inventario quedó en descubierto; null si había existencia. */
    private fun stockShortfallMessage(shortfall: Int): String? =
        if (shortfall > 0)
            "Stock insuficiente: el inventario quedo en descubierto por $shortfall unidad(es). Revise el repuesto."
        else null

    fun updatePart() {
        val state = _uiState.value
        val editingId = state.editingWorkOrderPartId ?: return
        val quantity = state.partFormQuantity.toIntOrNull() ?: run {
            _uiState.update { it.copy(error = "Ingrese cantidad válida") }
            return
        }
        if (quantity < 1) {
            _uiState.update { it.copy(error = "La cantidad debe ser al menos 1") }
            return
        }
        val unitPrice = state.partFormPrice.toDoubleOrNull() ?: run {
            _uiState.update { it.copy(error = "Ingrese precio válido") }
            return
        }
        val discount = if (state.partFormHasDiscount)
            state.partFormDiscount.toDoubleOrNull() ?: 0.0 else 0.0
        val subtotal = unitPrice * quantity
        if (discount > subtotal) {
            _uiState.update { it.copy(error = "El descuento no puede exceder el subtotal") }
            return
        }
        viewModelScope.launch {
            try {
                val existing = state.orderParts.find { it.id == editingId } ?: return@launch
                val updated = existing.copy(
                    // Si en el diálogo se cambió de repuesto, se respeta la nueva selección
                    // (el repositorio devuelve el stock del anterior y descuenta del nuevo).
                    partId = state.partFormSelectedPartId ?: existing.partId,
                    quantity = quantity,
                    appliedUnitPrice = unitPrice,
                    subtotal = subtotal,
                    discount = discount,
                    updatedAt = System.currentTimeMillis()
                )
                val shortfall = workOrderRepo.updateWorkOrderPart(updated)
                _uiState.update {
                    it.copy(
                        editingWorkOrderPartId = null,
                        partFormSelectedPartId = null,
                        partFormQuantity = "",
                        partFormPrice = "",
                        partFormHasDiscount = false,
                        partFormDiscount = "",
                        error = stockShortfallMessage(shortfall)
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al actualizar repuesto") }
            }
        }
    }

    fun addPayment() {
        val state = _uiState.value
        val order = state.selectedOrder ?: return
        val orderId = order.id
        if (order.status == OrderStatus.CERRADO) {
            _uiState.update { it.copy(error = "La orden esta cerrada: no admite nuevos pagos") }
            return
        }
        val amount = state.paymentFormAmount.toDoubleOrNull() ?: run {
            _uiState.update { it.copy(error = "Ingrese monto valido") }
            return
        }
        val discount = state.paymentFormDiscount.toDoubleOrNull() ?: 0.0
        if (amount < 0.0 || discount < 0.0) {
            _uiState.update { it.copy(error = "Los valores no pueden ser negativos") }
            return
        }
        viewModelScope.launch {
            try {
                val payment = WorkOrderPayment(
                    workOrderId = orderId,
                    amount = amount,
                    discount = discount,
                    method = state.paymentFormMethod,
                    notes = state.paymentFormNotes.trim().ifBlank { null }
                )
                workOrderRepo.addPayment(payment)
                _uiState.update {
                    it.copy(paymentFormAmount = "", paymentFormDiscount = "", paymentFormNotes = "")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al registrar pago") }
            }
        }
    }

    /** Carga un pago existente en el formulario para corregirlo. */
    fun startEditPayment(payment: WorkOrderPayment) {
        _uiState.update {
            it.copy(
                editingPaymentId = payment.id,
                paymentFormAmount = String.format(Locale.US, "%.2f", payment.amount),
                paymentFormDiscount = if (payment.discount > 0.0)
                    String.format(Locale.US, "%.2f", payment.discount) else "",
                paymentFormMethod = payment.method,
                paymentFormNotes = payment.notes ?: ""
            )
        }
    }

    /** Descarta la corrección en curso y limpia el formulario de pago. */
    fun cancelEditPayment() {
        _uiState.update {
            it.copy(
                editingPaymentId = null,
                paymentFormAmount = "",
                paymentFormDiscount = "",
                paymentFormMethod = PaymentMethod.EFECTIVO,
                paymentFormNotes = ""
            )
        }
    }

    /**
     * Guarda la corrección de un pago ya registrado.
     *
     * Conserva la fecha original del pago: se está corrigiendo un cobro que ocurrió antes,
     * no registrando uno nuevo.
     */
    fun updatePayment() {
        val state = _uiState.value
        val order = state.selectedOrder ?: return
        val editingId = state.editingPaymentId ?: return
        if (order.status == OrderStatus.CERRADO) {
            _uiState.update { it.copy(error = "La orden esta cerrada: no admite cambios en los pagos") }
            return
        }
        val amount = state.paymentFormAmount.toDoubleOrNull() ?: run {
            _uiState.update { it.copy(error = "Ingrese monto valido") }
            return
        }
        val discount = state.paymentFormDiscount.toDoubleOrNull() ?: 0.0
        if (amount < 0.0 || discount < 0.0) {
            _uiState.update { it.copy(error = "Los valores no pueden ser negativos") }
            return
        }
        if (amount + discount <= 0.0) {
            _uiState.update { it.copy(error = "Ingrese un monto o descuento mayor a 0") }
            return
        }
        viewModelScope.launch {
            try {
                val existing = state.payments.find { it.id == editingId } ?: return@launch
                workOrderRepo.updatePayment(
                    existing.copy(
                        amount = amount,
                        discount = discount,
                        method = state.paymentFormMethod,
                        notes = state.paymentFormNotes.trim().ifBlank { null },
                        updatedAt = System.currentTimeMillis()
                    )
                )
                _uiState.update {
                    it.copy(
                        editingPaymentId = null,
                        paymentFormAmount = "",
                        paymentFormDiscount = "",
                        paymentFormMethod = PaymentMethod.EFECTIVO,
                        paymentFormNotes = ""
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al actualizar el pago") }
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
        vehiclesJob?.cancel()
        vehiclesJob = viewModelScope.launch {
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
        val customerName = _uiState.value.customers.find { it.id == value }?.fullName ?: ""
        _uiState.update {
            it.copy(
                formCustomerId = value,
                formCustomerSearch = customerName,
                formVehicleId = null,
                customerVehicles = emptyList(),
                formCustomerError = null
            )
        }
        saveFormState()
        value?.let { loadVehiclesForCustomer(it) }
    }

    fun onFormCustomerSearchChange(value: String) {
        _uiState.update { it.copy(formCustomerSearch = value, formCustomerId = null, formVehicleId = null, customerVehicles = emptyList()) }
        saveFormState()
    }

    fun onFormVehicleIdChange(value: Long?) {
        _uiState.update { it.copy(formVehicleId = value, formVehicleError = null) }
        saveFormState()
    }

    fun onFormComplaintChange(value: String) {
        if (value.length <= 500) {
            _uiState.update { it.copy(formComplaint = value, formComplaintError = null) }
            // Try to match complaint to catalog for diagnosis suggestions
            val complaints = _uiState.value.catalogComplaints
            val matched = complaints.find { it.name.equals(value.trim(), ignoreCase = true) }
            _uiState.update { it.copy(selectedComplaintId = matched?.id) }
            saveFormState()
        }
    }

    fun onFormPriorityChange(value: Priority) { _uiState.update { it.copy(formPriority = value) }; saveFormState() }
    fun onFormOrderTypeChange(value: OrderType) { _uiState.update { it.copy(formOrderType = value) }; saveFormState() }
    fun onFormArrivalConditionChange(value: ArrivalCondition) { _uiState.update { it.copy(formArrivalCondition = value) }; saveFormState() }
    fun onFormEntryMileageChange(value: String) {
        val filtered = value.filter { it.isDigit() }.take(7)
        _uiState.update { it.copy(formEntryMileage = filtered, formMileageError = null) }
        saveFormState()
    }
    fun onFormFuelLevelChange(value: String) { _uiState.update { it.copy(formFuelLevel = value) }; saveFormState() }
    fun onFormChecklistNotesChange(value: String) { _uiState.update { it.copy(formChecklistNotes = value) }; saveFormState() }
    fun onFormDeliveryNoteChange(value: String) { _uiState.update { it.copy(formDeliveryNote = value) }; saveFormState() }
    fun onFormInvoiceNumberChange(value: String) { _uiState.update { it.copy(formInvoiceNumber = value) }; saveFormState() }
    fun onFormNotesChange(value: String) { _uiState.update { it.copy(formNotes = value) }; saveFormState() }
    fun onFormAdmissionDateChange(value: Long?) { _uiState.update { it.copy(formAdmissionDate = value) }; saveFormState() }

    fun prefillFromAppointment(customerId: Long, vehicleId: Long, appointmentId: Long) {
        viewModelScope.launch {
            val customer = customerRepo.getByIdDirect(customerId)
            _uiState.update {
                it.copy(
                    formCustomerId = customerId,
                    formCustomerSearch = customer?.fullName ?: "",
                    formVehicleId = vehicleId,
                    formAppointmentId = appointmentId,
                    formCustomerError = null,
                    formVehicleError = null
                )
            }
            loadVehiclesForCustomer(customerId)
        }
    }

    // Checklist toggle
    fun onChecklistItemToggle(name: String, checked: Boolean) {
        _uiState.update {
            it.copy(formChecklist = it.formChecklist + (name to checked))
        }
        saveFormState()
    }

    // Initialize checklist from accessories
    fun initChecklist() {
        val accessories = _uiState.value.catalogAccessories
        if (_uiState.value.formChecklist.isEmpty() && accessories.isNotEmpty()) {
            _uiState.update {
                it.copy(formChecklist = accessories.associate { acc -> acc.name to false })
            }
        }
    }

    fun onServiceLineDescriptionChange(value: String) { _uiState.update { it.copy(serviceLineFormDescription = value) } }
    fun onServiceLineLaborCostChange(value: String) {
        val filtered = value.replace(',', '.').filter { it.isDigit() || it == '.' }
        // Prevent multiple dots
        val dotCount = filtered.count { it == '.' }
        val sanitized = if (dotCount > 1) {
            val firstDot = filtered.indexOf('.')
            filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
        } else filtered
        _uiState.update { it.copy(serviceLineFormLaborCost = sanitized) }
    }

    fun onPartSelectedChange(value: Long?) {
        _uiState.update { it.copy(partFormSelectedPartId = value) }
        if (value != null) {
            viewModelScope.launch {
                val part = partRepo.getByIdDirect(value)
                // Try to get last price for same part + same customer
                val customerId = _uiState.value.selectedOrder?.customerId
                    ?: _uiState.value.formCustomerId
                val lastPrice = if (customerId != null) {
                    workOrderRepo.getLastPartPriceForCustomer(value, customerId)
                } else null
                val price = lastPrice ?: part?.salePrice ?: part?.unitCost ?: 0.0
                _uiState.update { it.copy(partFormPrice = String.format(Locale.US, "%.2f", price), partFormQuantity = "1") }
            }
        } else {
            _uiState.update { it.copy(partFormPrice = "") }
        }
    }
    fun onPartQuantityChange(value: String) { _uiState.update { it.copy(partFormQuantity = value) } }
    fun onPartPriceChange(value: String) {
        val filtered = value.replace(',', '.').filter { it.isDigit() || it == '.' }
        val dotCount = filtered.count { it == '.' }
        val sanitized = if (dotCount > 1) {
            val firstDot = filtered.indexOf('.')
            filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
        } else filtered
        _uiState.update { it.copy(partFormPrice = sanitized) }
    }

    fun onServiceLineDiscountToggle(enabled: Boolean) {
        _uiState.update {
            if (enabled) {
                val laborCost = it.serviceLineFormLaborCost.toDoubleOrNull() ?: 0.0
                it.copy(
                    serviceLineFormHasDiscount = true,
                    serviceLineFormDiscount = String.format(Locale.US, "%.2f", laborCost)
                )
            } else {
                it.copy(serviceLineFormHasDiscount = false, serviceLineFormDiscount = "")
            }
        }
    }

    fun onServiceLineDiscountChange(value: String) {
        val filtered = value.replace(',', '.').filter { it.isDigit() || it == '.' }
        val dotCount = filtered.count { it == '.' }
        val sanitized = if (dotCount > 1) {
            val firstDot = filtered.indexOf('.')
            filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
        } else filtered
        _uiState.update { it.copy(serviceLineFormDiscount = sanitized) }
    }

    fun onPartDiscountToggle(enabled: Boolean) {
        _uiState.update {
            if (enabled) {
                val qty = it.partFormQuantity.toIntOrNull() ?: 0
                val price = it.partFormPrice.toDoubleOrNull() ?: 0.0
                val subtotal = qty * price
                it.copy(
                    partFormHasDiscount = true,
                    partFormDiscount = String.format(Locale.US, "%.2f", subtotal)
                )
            } else {
                it.copy(partFormHasDiscount = false, partFormDiscount = "")
            }
        }
    }

    fun onPartDiscountChange(value: String) {
        val filtered = value.replace(',', '.').filter { it.isDigit() || it == '.' }
        val dotCount = filtered.count { it == '.' }
        val sanitized = if (dotCount > 1) {
            val firstDot = filtered.indexOf('.')
            filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
        } else filtered
        _uiState.update { it.copy(partFormDiscount = sanitized) }
    }

    fun onPaymentAmountChange(value: String) { _uiState.update { it.copy(paymentFormAmount = value) } }
    fun onPaymentDiscountChange(value: String) { _uiState.update { it.copy(paymentFormDiscount = value) } }
    fun onPaymentMethodChange(value: PaymentMethod) { _uiState.update { it.copy(paymentFormMethod = value) } }
    fun onPaymentNotesChange(value: String) { _uiState.update { it.copy(paymentFormNotes = value) } }

    // Photo management for create form
    fun prepareCameraFile(): Uri? {
        val context = getApplication<Application>()
        val file = PhotoUtils.createTempPhotoFile(context, "WO")
        pendingPhotoFile = file
        savedStateHandle["pendingPhotoFile"] = file.absolutePath
        val uri = PhotoUtils.getUriForFile(context, file)
        _uiState.update { it.copy(pendingPhotoUri = uri) }
        saveFormState()
        return uri
    }

    fun onPhotoTaken(success: Boolean) {
        val file = pendingPhotoFile
        if (success && file != null && file.exists() && file.length() > 0) {
            _uiState.update {
                it.copy(formPhotoPaths = it.formPhotoPaths + file.absolutePath, pendingPhotoUri = null)
            }
            // La cámara escribe el archivo a resolución completa: se comprime en segundo plano.
            viewModelScope.launch { PhotoUtils.compressPhotoInPlace(file) }
        } else {
            file?.delete()
            _uiState.update { it.copy(pendingPhotoUri = null) }
        }
        pendingPhotoFile = null
        savedStateHandle.remove<String>("pendingPhotoFile")
        saveFormState()
    }

    fun removeFormPhoto(index: Int) {
        val paths = _uiState.value.formPhotoPaths.toMutableList()
        if (index in paths.indices) {
            PhotoUtils.deletePhoto(paths[index])
            paths.removeAt(index)
            _uiState.update { it.copy(formPhotoPaths = paths) }
            saveFormState()
        }
    }

    fun addPhotoFromGallery(uri: Uri) {
        val context = getApplication<Application>()
        // El tope se aplica aquí, no solo ocultando el botón: el selector es múltiple y podían
        // entrar 15 fotos de una sola vez.
        if (_uiState.value.formPhotoPaths.size >= PhotoUtils.MAX_PHOTOS) {
            _uiState.update { it.copy(error = "Máximo ${PhotoUtils.MAX_PHOTOS} fotos por orden") }
            return
        }
        // Copiar y comprimir puede tardar segundos (la URI puede ser remota): fuera del hilo de UI.
        viewModelScope.launch {
            val file = PhotoUtils.copyUriToInternalStorage(context, uri, "WO")
            if (file != null) {
                _uiState.update { it.copy(formPhotoPaths = it.formPhotoPaths + file.absolutePath) }
                saveFormState()
            }
        }
    }

    private var replacingPhotoIndex: Int = -1

    fun onPhotoTakenForReplace(success: Boolean, index: Int) {
        val file = pendingPhotoFile
        if (success && file != null && file.exists() && file.length() > 0 && index >= 0) {
            val paths = _uiState.value.formPhotoPaths.toMutableList()
            if (index in paths.indices) {
                PhotoUtils.deletePhoto(paths[index])
                paths[index] = file.absolutePath
                _uiState.update { it.copy(formPhotoPaths = paths, pendingPhotoUri = null) }
                viewModelScope.launch { PhotoUtils.compressPhotoInPlace(file) }
            }
        } else {
            file?.delete()
            _uiState.update { it.copy(pendingPhotoUri = null) }
        }
        pendingPhotoFile = null
        savedStateHandle.remove<String>("pendingPhotoFile")
        replacingPhotoIndex = -1
        saveFormState()
    }

    fun replacePhotoFromGallery(uri: Uri, index: Int) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            val file = PhotoUtils.copyUriToInternalStorage(context, uri, "WO")
            if (file != null && index >= 0) {
                val paths = _uiState.value.formPhotoPaths.toMutableList()
                if (index in paths.indices) {
                    PhotoUtils.deletePhoto(paths[index])
                    paths[index] = file.absolutePath
                    _uiState.update { it.copy(formPhotoPaths = paths) }
                }
                saveFormState()
            }
        }
    }

    // File management for create/edit form
    fun addFormFile(uri: Uri) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            val file = PhotoUtils.copyFileToInternalStorage(context, uri, "WO")
            if (file != null) {
                _uiState.update { it.copy(formFilePaths = it.formFilePaths + file.absolutePath) }
                saveFormState()
            }
        }
    }

    fun removeFormFile(index: Int) {
        val paths = _uiState.value.formFilePaths.toMutableList()
        if (index in paths.indices) {
            PhotoUtils.deleteFile(paths[index])
            paths.removeAt(index)
            _uiState.update { it.copy(formFilePaths = paths) }
            saveFormState()
        }
    }

    // File management for detail screen (existing orders)
    fun addDetailFile(uri: Uri) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            val file = PhotoUtils.copyFileToInternalStorage(context, uri, "WO")
            if (file != null) {
                val newPaths = _uiState.value.detailFilePaths + file.absolutePath
                _uiState.update { it.copy(detailFilePaths = newPaths) }
                saveOrderFiles(newPaths)
            }
        }
    }

    fun removeDetailFile(index: Int) {
        val paths = _uiState.value.detailFilePaths.toMutableList()
        if (index in paths.indices) {
            PhotoUtils.deleteFile(paths[index])
            paths.removeAt(index)
            _uiState.update { it.copy(detailFilePaths = paths) }
            saveOrderFiles(paths)
        }
    }

    // Detail field change handlers — sólo actualizan UiState; el guardado real
    // se dispara desde el composable al perder el foco (onFocusChanged).
    fun onDetailEntryMileageChange(value: String) {
        val filtered = value.filter { it.isDigit() }.take(7)
        _uiState.update { it.copy(detailEntryMileage = filtered) }
        saveFormState()
    }
    /** El nivel de combustible es selección discreta de un dropdown: persistir al instante. */
    fun onDetailFuelLevelChange(value: String) {
        _uiState.update { it.copy(detailFuelLevel = value) }
        saveFormState()
        saveDetailFields()
    }
    fun onDetailDeliveryNoteChange(value: String) {
        _uiState.update { it.copy(detailDeliveryNote = value) }
        saveFormState()
    }
    fun onDetailInvoiceNumberChange(value: String) {
        _uiState.update { it.copy(detailInvoiceNumber = value) }
        saveFormState()
    }
    fun onDetailNotesChange(value: String) {
        _uiState.update { it.copy(detailNotes = value) }
        saveFormState()
    }

    /**
     * Persiste los campos editables del detalle. Se invoca al perder foco de cada campo.
     *
     * Escribe solo esas columnas: así no revierte fotos, totales ni estado que otra
     * operación haya guardado mientras el usuario tecleaba.
     */
    fun saveDetailFields() {
        val orderId = _uiState.value.selectedOrder?.id ?: return
        viewModelScope.launch {
            try {
                val state = _uiState.value
                workOrderRepo.updateProcessFields(
                    orderId = orderId,
                    entryMileage = state.detailEntryMileage.toIntOrNull(),
                    fuelLevel = state.detailFuelLevel.trim().ifBlank { null },
                    deliveryNote = state.detailDeliveryNote.trim().ifBlank { null },
                    invoiceNumber = state.detailInvoiceNumber.trim().ifBlank { null },
                    notes = state.detailNotes.trim().ifBlank { null },
                    updatedBy = session.currentUserId
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al guardar") }
            }
        }
    }

    private fun saveOrderFiles(paths: List<String>) {
        val orderId = _uiState.value.selectedOrder?.id ?: return
        viewModelScope.launch {
            try {
                workOrderRepo.updateFilePaths(orderId, PhotoUtils.serializePaths(paths))
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al guardar archivos") }
            }
        }
    }

    // Photo management for detail screen (existing orders)
    fun prepareDetailCameraFile(): Uri? {
        val context = getApplication<Application>()
        val file = PhotoUtils.createTempPhotoFile(context, "WO")
        detailPendingPhotoFile = file
        savedStateHandle["detailPendingPhotoFile"] = file.absolutePath
        val uri = PhotoUtils.getUriForFile(context, file)
        _uiState.update { it.copy(detailPendingPhotoUri = uri) }
        saveFormState()
        return uri
    }

    fun onDetailPhotoTaken(success: Boolean) {
        val file = detailPendingPhotoFile
        if (success && file != null && file.exists() && file.length() > 0) {
            val newPaths = _uiState.value.detailPhotoPaths + file.absolutePath
            _uiState.update { it.copy(detailPhotoPaths = newPaths, detailPendingPhotoUri = null) }
            saveOrderPhotos(newPaths)
            // La cámara escribe a resolución completa: se comprime en segundo plano.
            viewModelScope.launch { PhotoUtils.compressPhotoInPlace(file) }
        } else {
            file?.delete()
            _uiState.update { it.copy(detailPendingPhotoUri = null) }
        }
        detailPendingPhotoFile = null
        savedStateHandle.remove<String>("detailPendingPhotoFile")
        saveFormState()
    }

    fun addDetailPhotoFromGallery(uri: Uri) {
        val context = getApplication<Application>()
        if (_uiState.value.detailPhotoPaths.size >= PhotoUtils.MAX_PHOTOS) {
            _uiState.update { it.copy(error = "Máximo ${PhotoUtils.MAX_PHOTOS} fotos por orden") }
            return
        }
        viewModelScope.launch {
            val file = PhotoUtils.copyUriToInternalStorage(context, uri, "WO")
            if (file != null) {
                val newPaths = _uiState.value.detailPhotoPaths + file.absolutePath
                _uiState.update { it.copy(detailPhotoPaths = newPaths) }
                saveOrderPhotos(newPaths)
            }
        }
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
        val orderId = _uiState.value.selectedOrder?.id ?: return
        viewModelScope.launch {
            try {
                workOrderRepo.updatePhotoPaths(orderId, PhotoUtils.serializePaths(paths))
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
        // El modelo real es multi-mecánico (`work_order_mechanics`). Antes se resolvía el nombre
        // con `order.assignedMechanicId`, que el flujo de la app no escribía nunca: la fila
        // "Mecánico" simplemente no salía en el reporte del cliente.
        val mechanicName = state.orderMechanics
            .mapNotNull { assigned -> state.mechanics.find { it.id == assigned.mechanicId }?.name }
            .distinct()
            .joinToString(", ")
            .ifBlank { state.mechanics.find { it.id == order.assignedMechanicId }?.name }

        _uiState.update { it.copy(pdfGenerating = true) }

        viewModelScope.launch {
            try {
                val directPayments = if (state.payments.isEmpty()) {
                    workOrderRepo.getPaymentsDirect(order.id)
                } else {
                    state.payments
                }
                val reportData = WorkOrderReportData(
                    order = order,
                    customerName = customer?.fullName ?: state.customerName,
                    customerIdNumber = customer?.idNumber,
                    customerPhone = customer?.phone ?: "",
                    customerEmail = customer?.email,
                    customerAddress = customer?.address,
                    vehiclePlate = vehicle?.plate ?: "",
                    vehicleBrand = vehicle?.brand ?: "",
                    vehicleModel = vehicle?.model ?: "",
                    vehicleYear = vehicle?.year,
                    vehicleColor = vehicle?.color,
                    vehicleVin = vehicle?.vin,
                    vehicleVersion = vehicle?.version,
                    vehicleType = vehicle?.vehicleType,
                    vehicleFuelType = vehicle?.fuelType,
                    vehicleTransmission = vehicle?.transmission,
                    vehicleDrivetrain = vehicle?.drivetrain,
                    vehicleEngineDisplacement = vehicle?.engineDisplacement,
                    serviceLines = state.serviceLines,
                    orderParts = state.orderParts,
                    availableParts = state.availableParts,
                    orderExtras = state.orderExtras,
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

    fun deleteOrder(orderId: Long) {
        // Ultima barrera: la UI ya oculta el boton, pero el borrado es irreversible.
        if (!session.canDeleteOrders()) {
            _uiState.update { it.copy(error = "No tiene permisos para eliminar ordenes") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val (photoPaths, filePaths) = workOrderRepo.deleteOrder(orderId)
                // Delete photo files
                for (path in photoPaths) {
                    PhotoUtils.deletePhoto(path)
                }
                // Delete attached files
                for (path in filePaths) {
                    PhotoUtils.deleteFile(path)
                }
                _uiState.update { it.copy(isLoading = false, orderDeleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al eliminar orden") }
            }
        }
    }

    fun clearOrderDeleted() {
        _uiState.update { it.copy(orderDeleted = false) }
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
