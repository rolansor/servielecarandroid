package com.example.serviaux.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviaux.ServiauxApp
import com.example.serviaux.data.entity.OrderStatus
import com.example.serviaux.data.entity.User
import com.example.serviaux.data.entity.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val currentUserRole: UserRole = UserRole.MECANICO
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<ServiauxApp>()
    private val workOrderRepo get() = app.container.workOrderRepository
    private val session get() = app.container.sessionManager

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val currentUser: StateFlow<User?> = session.currentUser

    init {
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
