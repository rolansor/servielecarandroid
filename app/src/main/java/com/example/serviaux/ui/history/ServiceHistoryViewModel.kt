package com.example.serviaux.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviaux.ServiauxApp
import com.example.serviaux.data.entity.Customer
import com.example.serviaux.data.entity.ServiceLine
import com.example.serviaux.data.entity.Vehicle
import com.example.serviaux.data.entity.WorkOrder
import com.example.serviaux.data.entity.WorkOrderPart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class ServiceHistoryUiState(
    val customers: List<Customer> = emptyList(),
    val selectedCustomer: Customer? = null,
    val customerQuery: String = "",
    val orders: List<WorkOrder> = emptyList(),
    val filteredOrders: List<WorkOrder> = emptyList(),
    val vehicleMap: Map<Long, Vehicle> = emptyMap(),
    val serviceMap: Map<Long, List<ServiceLine>> = emptyMap(),
    val partMap: Map<Long, List<WorkOrderPart>> = emptyMap(),
    val partNameMap: Map<Long, String> = emptyMap(),
    val searchQuery: String = "",
    val filterYear: Int = 0,
    val availableYears: List<Int> = emptyList(),
    val isLoading: Boolean = false
)

class ServiceHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val app get() = getApplication<ServiauxApp>()
    private val customerRepo get() = app.container.customerRepository
    private val workOrderRepo get() = app.container.workOrderRepository
    private val vehicleRepo get() = app.container.vehicleRepository
    private val partRepo get() = app.container.partRepository

    private val _uiState = MutableStateFlow(ServiceHistoryUiState())
    val uiState: StateFlow<ServiceHistoryUiState> = _uiState.asStateFlow()

    private var ordersJob: Job? = null
    private var customersJob: Job? = null

    fun searchCustomers(query: String) {
        _uiState.update { it.copy(customerQuery = query) }
        customersJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(customers = emptyList()) }
            return
        }
        customersJob = viewModelScope.launch {
            customerRepo.search("%$query%").collect { customers ->
                _uiState.update { it.copy(customers = customers) }
            }
        }
    }

    fun selectCustomer(customer: Customer) {
        _uiState.update { it.copy(
            selectedCustomer = customer,
            customerQuery = customer.fullName,
            customers = emptyList(),
            isLoading = true
        ) }
        loadOrdersForCustomer(customer.id)
    }

    fun selectCustomerById(customerId: Long) {
        viewModelScope.launch {
            val customer = customerRepo.getByIdDirect(customerId) ?: return@launch
            selectCustomer(customer)
        }
    }

    fun clearCustomer() {
        ordersJob?.cancel()
        _uiState.update { ServiceHistoryUiState() }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun setFilterYear(year: Int) {
        _uiState.update { it.copy(filterYear = year) }
        applyFilters()
    }

    private fun loadOrdersForCustomer(customerId: Long) {
        ordersJob?.cancel()
        ordersJob = viewModelScope.launch {
            workOrderRepo.getByCustomer(customerId).collect { orders ->
                val sortedOrders = orders.sortedByDescending { it.entryDate }

                // Collect available years
                val years = sortedOrders.map { order ->
                    Calendar.getInstance().apply { timeInMillis = order.entryDate }.get(Calendar.YEAR)
                }.distinct().sorted().reversed()

                // Load vehicles
                val vehicleIds = sortedOrders.map { it.vehicleId }.distinct()
                val vehicleMap = mutableMapOf<Long, Vehicle>()
                vehicleIds.forEach { vid ->
                    vehicleRepo.getByIdDirect(vid)?.let { vehicleMap[vid] = it }
                }

                // Load services and parts for each order
                val serviceMap = mutableMapOf<Long, List<ServiceLine>>()
                val partMap = mutableMapOf<Long, List<WorkOrderPart>>()
                val partNameMap = mutableMapOf<Long, String>()

                sortedOrders.forEach { order ->
                    val services = mutableListOf<ServiceLine>()
                    val parts = mutableListOf<WorkOrderPart>()

                    // Collect services (using direct suspend calls via first())
                    launch {
                        workOrderRepo.getServiceLines(order.id).collect { lines ->
                            services.clear()
                            services.addAll(lines)
                            serviceMap[order.id] = services.toList()
                            _uiState.update { it.copy(serviceMap = serviceMap.toMap()) }
                        }
                    }

                    launch {
                        workOrderRepo.getWorkOrderParts(order.id).collect { woParts ->
                            parts.clear()
                            parts.addAll(woParts)
                            partMap[order.id] = parts.toList()

                            // Load part names
                            woParts.forEach { wop ->
                                if (!partNameMap.containsKey(wop.partId)) {
                                    partRepo.getByIdDirect(wop.partId)?.let { part ->
                                        partNameMap[wop.partId] = part.name
                                    }
                                }
                            }

                            _uiState.update { it.copy(
                                partMap = partMap.toMap(),
                                partNameMap = partNameMap.toMap()
                            ) }
                        }
                    }
                }

                _uiState.update { it.copy(
                    orders = sortedOrders,
                    vehicleMap = vehicleMap,
                    availableYears = years,
                    isLoading = false
                ) }
                applyFilters()
            }
        }
    }

    private fun applyFilters() {
        val state = _uiState.value
        var filtered = state.orders

        // Filter by year
        if (state.filterYear != 0) {
            filtered = filtered.filter { order ->
                Calendar.getInstance().apply { timeInMillis = order.entryDate }.get(Calendar.YEAR) == state.filterYear
            }
        }

        // Filter by search query (search in services and parts)
        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.lowercase()
            filtered = filtered.filter { order ->
                val services = state.serviceMap[order.id] ?: emptyList()
                val parts = state.partMap[order.id] ?: emptyList()
                services.any { it.description.lowercase().contains(query) } ||
                parts.any { part ->
                    val name = state.partNameMap[part.partId] ?: ""
                    name.lowercase().contains(query)
                }
            }
        }

        _uiState.update { it.copy(filteredOrders = filtered) }
    }
}
