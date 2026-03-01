/**
 * ReportsViewModel.kt - ViewModel del módulo de reportes.
 *
 * Solo accesible para administradores. Genera reportes por rango de fechas:
 * - Total facturado (excluyendo órdenes canceladas).
 * - Cantidad de órdenes en el período.
 * - Repuestos más utilizados con cantidades.
 * - Lista de órdenes filtradas por estado y/o rango de fechas.
 */
package com.example.serviaux.ui.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviaux.ServiauxApp
import com.example.serviaux.data.entity.Part
import com.example.serviaux.data.entity.WorkOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportsUiState(
    val orders: List<WorkOrder> = emptyList(),
    val totalRevenue: Double = 0.0,
    val topParts: List<Pair<Part, Long>> = emptyList(),
    val dateFrom: Long = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000,
    val dateTo: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false
)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<ServiauxApp>()
    private val workOrderRepo get() = app.container.workOrderRepository
    private val partRepo get() = app.container.partRepository

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadReport()
    }

    fun setDateRange(from: Long, to: Long) {
        _uiState.update { it.copy(dateFrom = from, dateTo = to) }
        loadReport()
    }

    fun loadReport() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Load orders by date range
                val orders = workOrderRepo.getByDateRange(state.dateFrom, state.dateTo).firstOrNull() ?: emptyList()
                _uiState.update { it.copy(orders = orders) }

                // Load total revenue
                val totalRevenue = workOrderRepo.getTotalByDateRange(state.dateFrom, state.dateTo).firstOrNull() ?: 0.0
                _uiState.update { it.copy(totalRevenue = totalRevenue) }

                // Load top parts
                val topPartsRaw = workOrderRepo.getTopParts(state.dateFrom, state.dateTo)
                val topParts = topPartsRaw.mapNotNull { topPart ->
                    val part = partRepo.getByIdDirect(topPart.partId)
                    part?.let { Pair(it, topPart.totalQty) }
                }
                _uiState.update { it.copy(topParts = topParts, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
