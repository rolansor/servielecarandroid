/**
 * CommissionViewModel.kt - ViewModel del módulo de comisiones.
 *
 * Gestiona la pantalla de pago de comisiones a mecánicos (solo admin):
 * - Carga comisiones pendientes agrupadas por mecánico.
 * - Selección individual o por grupo para pago en lote.
 * - Marca como pagadas en batch vía [CommissionRepository].
 * - Genera PDF de resumen del pago realizado.
 */
package com.example.serviaux.ui.commissions

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviaux.ServiauxApp
import com.example.serviaux.data.entity.PendingCommissionRow
import com.example.serviaux.repository.CommissionRepository
import com.example.serviaux.util.CommissionPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.example.serviaux.util.ShareUtils
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CommissionUiState(
    val pendingCommissions: List<PendingCommissionRow> = emptyList(),
    val mechanicNames: Map<Long, String> = emptyMap(),
    val selectedIds: Set<Long> = emptySet(),
    val paymentCompleted: Boolean = false,
    val paidCommissions: List<PendingCommissionRow> = emptyList(),
    val pdfGenerating: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showHistory: Boolean = false,
    val paidHistory: List<PendingCommissionRow> = emptyList(),
    val historyMechanicNames: Map<Long, String> = emptyMap()
)

class CommissionViewModel(application: Application) : AndroidViewModel(application) {
    private val commissionRepository: CommissionRepository =
        (application as ServiauxApp).container.commissionRepository

    private val _uiState = MutableStateFlow(CommissionUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadPendingCommissions()
    }

    fun loadPendingCommissions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val commissions = withContext(Dispatchers.IO) {
                    commissionRepository.getUnpaidCommissions()
                }
                val mechanicIds = commissions.map { it.mechanicId }.distinct()
                val names = mutableMapOf<Long, String>()
                withContext(Dispatchers.IO) {
                    mechanicIds.forEach { id ->
                        commissionRepository.getMechanicName(id)?.let { names[id] = it }
                    }
                }
                _uiState.update {
                    it.copy(
                        pendingCommissions = commissions,
                        mechanicNames = names,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al cargar comisiones") }
            }
        }
    }

    fun toggleSelection(id: Long) {
        _uiState.update {
            val newSet = it.selectedIds.toMutableSet()
            if (id in newSet) newSet.remove(id) else newSet.add(id)
            it.copy(selectedIds = newSet)
        }
    }

    fun selectAllForMechanic(mechanicId: Long) {
        _uiState.update { state ->
            val mechanicIds = state.pendingCommissions
                .filter { it.mechanicId == mechanicId }
                .map { it.id }
                .toSet()
            val allSelected = mechanicIds.all { it in state.selectedIds }
            val newSet = state.selectedIds.toMutableSet()
            if (allSelected) newSet.removeAll(mechanicIds) else newSet.addAll(mechanicIds)
            state.copy(selectedIds = newSet)
        }
    }

    fun paySelected() {
        val state = _uiState.value
        val ids = state.selectedIds.toList()
        if (ids.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                withContext(Dispatchers.IO) {
                    commissionRepository.batchMarkAsPaid(ids)
                }
                val paidItems = state.pendingCommissions.filter { it.id in state.selectedIds }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        paymentCompleted = true,
                        paidCommissions = paidItems,
                        selectedIds = emptySet()
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al pagar comisiones") }
            }
        }
    }

    fun generateAndSharePdf(context: Context) {
        val state = _uiState.value
        if (state.paidCommissions.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(pdfGenerating = true) }
            try {
                val file = withContext(Dispatchers.IO) {
                    CommissionPdfGenerator.generate(context, state.paidCommissions, state.mechanicNames)
                }
                _uiState.update { it.copy(pdfGenerating = false) }
                ShareUtils.sharePdf(context, file)
            } catch (e: Exception) {
                _uiState.update { it.copy(pdfGenerating = false, error = e.message ?: "Error al generar PDF") }
            }
        }
    }

    fun resetPaymentState() {
        _uiState.update {
            it.copy(
                paymentCompleted = false,
                paidCommissions = emptyList()
            )
        }
        loadPendingCommissions()
    }

    fun toggleHistory() {
        val showHistory = !_uiState.value.showHistory
        _uiState.update { it.copy(showHistory = showHistory) }
        if (showHistory && _uiState.value.paidHistory.isEmpty()) {
            loadPaidHistory()
        }
    }

    private fun loadPaidHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val history = withContext(Dispatchers.IO) {
                    commissionRepository.getPaidCommissions()
                }
                val mechanicIds = history.map { it.mechanicId }.distinct()
                val names = _uiState.value.mechanicNames.toMutableMap()
                withContext(Dispatchers.IO) {
                    mechanicIds.forEach { id ->
                        if (id !in names) {
                            commissionRepository.getMechanicName(id)?.let { names[id] = it }
                        }
                    }
                }
                _uiState.update {
                    it.copy(
                        paidHistory = history,
                        mechanicNames = names,
                        historyMechanicNames = names,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Error al cargar historial") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
