package com.example.serviaux.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviaux.ServiauxApp
import com.example.serviaux.data.ServiauxDatabase
import com.example.serviaux.data.entity.AppointmentStatus
import com.example.serviaux.data.entity.OrderStatus
import com.example.serviaux.data.entity.User
import com.example.serviaux.data.entity.UserRole
import com.example.serviaux.data.entity.WorkOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val recibido: Int = 0,
    val enDiagnostico: Int = 0,
    val enProceso: Int = 0,
    val enEsperaRepuesto: Int = 0,
    val listo: Int = 0,
    val entregado: Int = 0,
    val currentUserName: String = "",
    val currentUserRole: UserRole = UserRole.MECANICO,
    val recentOrders: List<WorkOrder> = emptyList(),
    val totalActiveOrders: Int = 0,
    val vehicleMap: Map<Long, String> = emptyMap(),
    val customerMap: Map<Long, String> = emptyMap(),
    val turnosPendientes: Int = 0,
    val turnosConfirmados: Int = 0,
    val showSampleDataDialog: Boolean = false,
    val loadingSampleData: Boolean = false
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<ServiauxApp>()
    private val workOrderRepo get() = app.container.workOrderRepository
    private val vehicleRepo get() = app.container.vehicleRepository
    private val customerRepo get() = app.container.customerRepository
    private val appointmentRepo get() = app.container.appointmentRepository
    private val session get() = app.container.sessionManager

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<User?> = session.currentUser

    private val activeStatuses = setOf(
        OrderStatus.RECIBIDO, OrderStatus.EN_DIAGNOSTICO,
        OrderStatus.EN_PROCESO, OrderStatus.EN_ESPERA_REPUESTO, OrderStatus.LISTO
    )

    init {
        if (ServiauxDatabase.needsSamplePrompt(application)) {
            _uiState.update { it.copy(showSampleDataDialog = true) }
        }

        viewModelScope.launch {
            session.currentUser.collect { user ->
                _uiState.update {
                    it.copy(
                        currentUserName = user?.name ?: "",
                        currentUserRole = user?.role ?: UserRole.MECANICO
                    )
                }
            }
        }

        collectStatusCount(OrderStatus.RECIBIDO) { count ->
            _uiState.update { it.copy(recibido = count) }
        }
        collectStatusCount(OrderStatus.EN_DIAGNOSTICO) { count ->
            _uiState.update { it.copy(enDiagnostico = count) }
        }
        collectStatusCount(OrderStatus.EN_PROCESO) { count ->
            _uiState.update { it.copy(enProceso = count) }
        }
        collectStatusCount(OrderStatus.EN_ESPERA_REPUESTO) { count ->
            _uiState.update { it.copy(enEsperaRepuesto = count) }
        }
        collectStatusCount(OrderStatus.LISTO) { count ->
            _uiState.update { it.copy(listo = count) }
        }
        collectStatusCount(OrderStatus.ENTREGADO) { count ->
            _uiState.update { it.copy(entregado = count) }
        }

        viewModelScope.launch {
            workOrderRepo.getAll().map { orders ->
                orders.filter { it.status in activeStatuses }.take(5)
            }.collect { recent ->
                _uiState.update {
                    it.copy(
                        recentOrders = recent,
                        totalActiveOrders = recent.size
                    )
                }
            }
        }

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

        viewModelScope.launch {
            appointmentRepo.countByStatus(AppointmentStatus.PENDIENTE).collect { count ->
                _uiState.update { it.copy(turnosPendientes = count) }
            }
        }
        viewModelScope.launch {
            appointmentRepo.countByStatus(AppointmentStatus.CONFIRMADO).collect { count ->
                _uiState.update { it.copy(turnosConfirmados = count) }
            }
        }
    }

    fun loadSampleData() {
        _uiState.update { it.copy(loadingSampleData = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<ServiauxApp>()
            val db = ServiauxDatabase.getInstance(context)
            ServiauxDatabase.loadSampleData(context, db)
            ServiauxDatabase.clearSamplePrompt(context)
            _uiState.update { it.copy(showSampleDataDialog = false, loadingSampleData = false) }
        }
    }

    fun dismissSampleDataDialog() {
        val context = getApplication<ServiauxApp>()
        ServiauxDatabase.clearSamplePrompt(context)
        _uiState.update { it.copy(showSampleDataDialog = false) }
    }

    fun logout() {
        app.container.authRepository.logout()
    }

    private fun collectStatusCount(status: OrderStatus, onUpdate: (Int) -> Unit) {
        viewModelScope.launch {
            workOrderRepo.countByStatus(status).collect { count ->
                onUpdate(count)
            }
        }
    }
}
