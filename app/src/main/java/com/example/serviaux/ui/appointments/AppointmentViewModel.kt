package com.example.serviaux.ui.appointments

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviaux.ServiauxApp
import com.example.serviaux.data.entity.*
import com.example.serviaux.util.AppointmentPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class AppointmentUiState(
    val appointments: List<Appointment> = emptyList(),
    val filter: AppointmentStatus? = null,
    val customerMap: Map<Long, String> = emptyMap(),
    val vehicleMap: Map<Long, String> = emptyMap(),
    val customers: List<Customer> = emptyList(),
    val customerVehicles: List<Vehicle> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    // Form fields
    val formCustomerId: Long? = null,
    val formCustomerSearch: String = "",
    val formVehicleId: Long? = null,
    val formScheduledDate: Long? = null,
    val formScheduledHour: Int = 8,
    val formScheduledMinute: Int = 0,
    val formNotes: String = "",
    val formStatus: AppointmentStatus = AppointmentStatus.PENDIENTE,
    // Form validation
    val formCustomerError: String? = null,
    val formVehicleError: String? = null,
    val formDateError: String? = null,
    // Edit mode
    val isEditing: Boolean = false,
    val editingAppointmentId: Long? = null,
    val savedSuccessfully: Boolean = false,
    val isListLoaded: Boolean = false,
    // PDF
    val pdfGenerating: Boolean = false,
    val pdfFile: File? = null
)

class AppointmentViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<ServiauxApp>()
    private val appointmentRepo get() = app.container.appointmentRepository
    private val customerRepo get() = app.container.customerRepository
    private val vehicleRepo get() = app.container.vehicleRepository
    private val session get() = app.container.sessionManager

    private val _uiState = MutableStateFlow(AppointmentUiState())
    val uiState: StateFlow<AppointmentUiState> = _uiState.asStateFlow()

    private var appointmentsJob: Job? = null
    private var vehiclesJob: Job? = null

    init {
        loadAppointments()
        loadMaps()
    }

    private fun loadMaps() {
        viewModelScope.launch {
            customerRepo.getAll().collect { customers ->
                _uiState.update { it.copy(customerMap = customers.associate { c -> c.id to c.fullName }) }
            }
        }
        viewModelScope.launch {
            vehicleRepo.getAll().collect { vehicles ->
                _uiState.update { it.copy(vehicleMap = vehicles.associate { v -> v.id to "${v.plate} - ${v.brand} ${v.model}" }) }
            }
        }
    }

    fun loadAppointments(filter: AppointmentStatus? = _uiState.value.filter) {
        _uiState.update { it.copy(filter = filter) }
        appointmentsJob?.cancel()
        appointmentsJob = viewModelScope.launch {
            val flow = if (filter != null) appointmentRepo.getByStatus(filter) else appointmentRepo.getAll()
            flow.collect { list ->
                _uiState.update { it.copy(appointments = list, isListLoaded = true) }
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

    fun onFormCustomerIdChange(value: Long?) {
        val customerName = _uiState.value.customers.find { it.id == value }?.fullName ?: ""
        _uiState.update {
            it.copy(
                formCustomerId = value,
                formCustomerSearch = customerName,
                formVehicleId = null,
                formCustomerError = null
            )
        }
        if (value != null) loadVehiclesForCustomer(value)
    }

    fun onFormCustomerSearchChange(value: String) {
        _uiState.update { it.copy(formCustomerSearch = value, formCustomerId = null, formVehicleId = null) }
    }

    fun onFormVehicleIdChange(value: Long?) {
        _uiState.update { it.copy(formVehicleId = value, formVehicleError = null) }
    }

    fun onFormScheduledDateChange(value: Long?) {
        _uiState.update { it.copy(formScheduledDate = value, formDateError = null) }
    }

    fun onFormTimeChange(hour: Int, minute: Int) {
        _uiState.update { it.copy(formScheduledHour = hour, formScheduledMinute = minute) }
    }

    fun onFormNotesChange(value: String) {
        _uiState.update { it.copy(formNotes = value) }
    }

    fun onFormStatusChange(value: AppointmentStatus) {
        _uiState.update { it.copy(formStatus = value) }
    }

    fun prepareEdit(appointmentId: Long) {
        viewModelScope.launch {
            val appointment = appointmentRepo.getByIdDirect(appointmentId) ?: return@launch
            val customer = customerRepo.getByIdDirect(appointment.customerId)

            val cal = java.util.Calendar.getInstance().apply { timeInMillis = appointment.scheduledDate }

            _uiState.update {
                it.copy(
                    isEditing = true,
                    editingAppointmentId = appointmentId,
                    formCustomerId = appointment.customerId,
                    formCustomerSearch = customer?.fullName ?: "",
                    formVehicleId = appointment.vehicleId,
                    formScheduledDate = appointment.scheduledDate,
                    formScheduledHour = cal.get(java.util.Calendar.HOUR_OF_DAY),
                    formScheduledMinute = cal.get(java.util.Calendar.MINUTE),
                    formNotes = appointment.notes ?: "",
                    formStatus = appointment.status,
                    formCustomerError = null,
                    formVehicleError = null,
                    formDateError = null
                )
            }

            if (appointment.customerId > 0) loadVehiclesForCustomer(appointment.customerId)
        }
    }

    fun save() {
        val state = _uiState.value
        if (!session.isLoggedIn) {
            _uiState.update { it.copy(error = "Debe iniciar sesión") }
            return
        }

        val customerError = if (state.formCustomerId == null) "Debe seleccionar un cliente" else null
        val vehicleError = if (state.formVehicleId == null) "Debe seleccionar un vehículo" else null
        val dateError = if (state.formScheduledDate == null) "Debe seleccionar una fecha" else null

        _uiState.update {
            it.copy(formCustomerError = customerError, formVehicleError = vehicleError, formDateError = dateError)
        }

        if (customerError != null || vehicleError != null || dateError != null) return

        // Combine date + time
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = state.formScheduledDate!!
            set(java.util.Calendar.HOUR_OF_DAY, state.formScheduledHour)
            set(java.util.Calendar.MINUTE, state.formScheduledMinute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val scheduledDate = cal.timeInMillis

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                if (state.isEditing && state.editingAppointmentId != null) {
                    val existing = appointmentRepo.getByIdDirect(state.editingAppointmentId) ?: return@launch
                    appointmentRepo.update(existing.copy(
                        customerId = state.formCustomerId!!,
                        vehicleId = state.formVehicleId!!,
                        scheduledDate = scheduledDate,
                        notes = state.formNotes.trim().ifBlank { null },
                        status = state.formStatus
                    ))
                } else {
                    appointmentRepo.insert(Appointment(
                        customerId = state.formCustomerId!!,
                        vehicleId = state.formVehicleId!!,
                        scheduledDate = scheduledDate,
                        notes = state.formNotes.trim().ifBlank { null },
                        createdBy = session.currentUserId
                    ))
                }
                _uiState.update { it.copy(isLoading = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al guardar turno") }
            }
        }
    }

    fun confirm(id: Long) {
        viewModelScope.launch {
            try { appointmentRepo.confirm(id) } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al confirmar turno") }
            }
        }
    }

    fun cancel(id: Long) {
        viewModelScope.launch {
            try { appointmentRepo.cancel(id) } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al cancelar turno") }
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            try { appointmentRepo.deleteById(id) } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Error al eliminar turno") }
            }
        }
    }

    fun clearForm() {
        _uiState.update {
            it.copy(
                formCustomerId = null, formCustomerSearch = "", formVehicleId = null,
                formScheduledDate = null, formScheduledHour = 8, formScheduledMinute = 0,
                formNotes = "", formStatus = AppointmentStatus.PENDIENTE,
                formCustomerError = null, formVehicleError = null, formDateError = null,
                isEditing = false, editingAppointmentId = null, savedSuccessfully = false
            )
        }
    }

    fun clearError() { _uiState.update { it.copy(error = null) } }
    fun clearSaved() { _uiState.update { it.copy(savedSuccessfully = false) } }

    fun generatePdf(context: Context) {
        val state = _uiState.value
        if (state.appointments.isEmpty()) {
            _uiState.update { it.copy(error = "No hay turnos para exportar") }
            return
        }
        _uiState.update { it.copy(pdfGenerating = true) }
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    AppointmentPdfGenerator.generate(
                        context = context,
                        appointments = state.appointments,
                        customerNames = state.customerMap,
                        vehicleDescriptions = state.vehicleMap
                    )
                }
                _uiState.update { it.copy(pdfGenerating = false, pdfFile = file) }
            } catch (e: Exception) {
                _uiState.update { it.copy(pdfGenerating = false, error = e.message ?: "Error al generar PDF") }
            }
        }
    }

    fun generateSinglePdf(context: Context, appointment: Appointment) {
        val state = _uiState.value
        _uiState.update { it.copy(pdfGenerating = true) }
        viewModelScope.launch {
            try {
                val customerName = state.customerMap[appointment.customerId] ?: "Cliente #${appointment.customerId}"
                val vehicleDesc = state.vehicleMap[appointment.vehicleId] ?: "Vehiculo #${appointment.vehicleId}"
                val file = withContext(Dispatchers.IO) {
                    AppointmentPdfGenerator.generateSingle(
                        context = context,
                        appointment = appointment,
                        customerName = customerName,
                        vehicleDescription = vehicleDesc
                    )
                }
                _uiState.update { it.copy(pdfGenerating = false, pdfFile = file) }
            } catch (e: Exception) {
                _uiState.update { it.copy(pdfGenerating = false, error = e.message ?: "Error al generar PDF") }
            }
        }
    }

    fun clearPdf() { _uiState.update { it.copy(pdfFile = null) } }
}
