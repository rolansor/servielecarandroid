/**
 * TallerViewModel.kt - Estado del tablero "El taller hoy".
 *
 * Reemplaza al antiguo DashboardViewModel: en lugar de contadores sueltos,
 * agrupa las órdenes activas por estado para pintarlas como secciones.
 * Hereda de aquel el diálogo de primer arranque (cargar datos de ejemplo).
 */
package com.example.serviaux.ui.taller

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviaux.ServiauxApp
import com.example.serviaux.data.ServiauxDatabase
import com.example.serviaux.data.entity.OrderStatus
import com.example.serviaux.data.entity.User
import com.example.serviaux.data.entity.UserRole
import com.example.serviaux.data.entity.Vehicle
import com.example.serviaux.data.entity.WorkOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Orden de las secciones del tablero: el flujo de trabajo del taller. */
val TALLER_SECTION_ORDER = listOf(
    OrderStatus.RECIBIDO,
    OrderStatus.EN_DIAGNOSTICO,
    OrderStatus.EN_PROCESO,
    OrderStatus.EN_ESPERA_REPUESTO,
    OrderStatus.LISTO
)

data class TallerUiState(
    val ordersByStatus: Map<OrderStatus, List<WorkOrder>> = emptyMap(),
    val totalActivos: Int = 0,
    val currentUserName: String = "",
    val currentUserRole: UserRole = UserRole.MECANICO,
    val vehicleMap: Map<Long, Vehicle> = emptyMap(),
    val customerMap: Map<Long, String> = emptyMap(),
    val userNameMap: Map<Long, String> = emptyMap(),
    val showSampleDataDialog: Boolean = false,
    val loadingSampleData: Boolean = false
)

class TallerViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<ServiauxApp>()
    private val workOrderRepo get() = app.container.workOrderRepository
    private val vehicleRepo get() = app.container.vehicleRepository
    private val customerRepo get() = app.container.customerRepository
    private val authRepo get() = app.container.authRepository
    private val session get() = app.container.sessionManager

    private val _uiState = MutableStateFlow(TallerUiState())
    val uiState: StateFlow<TallerUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<User?> = session.currentUser

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

        viewModelScope.launch {
            workOrderRepo.getAll().collect { orders ->
                val active = orders.filter { it.status in TALLER_SECTION_ORDER }
                _uiState.update {
                    it.copy(
                        ordersByStatus = active.groupBy { o -> o.status },
                        totalActivos = active.size
                    )
                }
            }
        }

        viewModelScope.launch {
            vehicleRepo.getAll().collect { vehicles ->
                _uiState.update { it.copy(vehicleMap = vehicles.associateBy { v -> v.id }) }
            }
        }
        viewModelScope.launch {
            customerRepo.getAll().collect { customers ->
                _uiState.update { it.copy(customerMap = customers.associate { c -> c.id to c.fullName }) }
            }
        }
        viewModelScope.launch {
            authRepo.getAllUsers().collect { users ->
                _uiState.update { it.copy(userNameMap = users.associate { u -> u.id to u.name }) }
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
}
