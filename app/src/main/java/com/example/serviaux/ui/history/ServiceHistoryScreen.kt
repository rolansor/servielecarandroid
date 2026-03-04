package com.example.serviaux.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.serviaux.data.entity.OrderStatus
import com.example.serviaux.data.entity.ServiceLine
import com.example.serviaux.data.entity.Vehicle
import com.example.serviaux.data.entity.WorkOrder
import com.example.serviaux.data.entity.WorkOrderPart
import com.example.serviaux.ui.components.SearchableDropdown
import com.example.serviaux.ui.components.SearchableItem
import com.example.serviaux.ui.components.StatusChip
import com.example.serviaux.ui.theme.StatusDiagnostico
import com.example.serviaux.ui.theme.StatusEnProceso
import com.example.serviaux.ui.theme.StatusEntregado
import com.example.serviaux.ui.theme.StatusEsperaRepuesto
import com.example.serviaux.ui.theme.StatusListo
import com.example.serviaux.ui.theme.StatusRecibido
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceHistoryScreen(
    customerId: Long? = null,
    onNavigateBack: () -> Unit,
    onNavigateToOrderDetail: (Long) -> Unit,
    viewModel: ServiceHistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es"))
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    LaunchedEffect(customerId) {
        if (customerId != null && uiState.selectedCustomer == null) {
            viewModel.selectCustomerById(customerId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = 40.dp,
                title = { Text("Historial de Servicios") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Customer selector
            if (uiState.selectedCustomer == null) {
                Spacer(modifier = Modifier.height(8.dp))
                SearchableDropdown(
                    value = uiState.customerQuery,
                    onValueChange = { viewModel.searchCustomers(it) },
                    items = uiState.customers.map { c ->
                        SearchableItem(
                            id = c.id,
                            name = c.fullName,
                            subtitle = c.idNumber
                        )
                    },
                    onItemSelected = { item ->
                        uiState.customers.find { it.id == item.id }?.let { viewModel.selectCustomer(it) }
                    },
                    label = "Buscar cliente por nombre o c\u00e9dula",
                    maxSuggestions = 5
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Seleccione un cliente para ver su historial de servicios",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                // Customer header with clear button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.selectedCustomer!!.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val idNumber = uiState.selectedCustomer!!.idNumber
                        if (!idNumber.isNullOrBlank()) {
                            Text(
                                text = idNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (customerId == null) {
                        IconButton(onClick = { viewModel.clearCustomer() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cambiar cliente")
                        }
                    }
                }

                // Search and year filters
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    label = { Text("Buscar servicio o repuesto") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (uiState.availableYears.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = uiState.filterYear == 0,
                                onClick = { viewModel.setFilterYear(0) },
                                label = { Text("Todos") }
                            )
                        }
                        items(uiState.availableYears) { year ->
                            FilterChip(
                                selected = uiState.filterYear == year,
                                onClick = { viewModel.setFilterYear(year) },
                                label = { Text(year.toString()) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.filteredOrders.isEmpty()) {
                    Text(
                        text = "No se encontraron \u00f3rdenes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                } else {
                    Text(
                        text = "${uiState.filteredOrders.size} orden(es)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.filteredOrders, key = { it.id }) { order ->
                            OrderHistoryCard(
                                order = order,
                                vehicle = uiState.vehicleMap[order.vehicleId],
                                services = uiState.serviceMap[order.id] ?: emptyList(),
                                parts = uiState.partMap[order.id] ?: emptyList(),
                                partNameMap = uiState.partNameMap,
                                dateFormat = dateFormat,
                                currencyFormat = currencyFormat,
                                onClick = { onNavigateToOrderDetail(order.id) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderHistoryCard(
    order: WorkOrder,
    vehicle: Vehicle?,
    services: List<ServiceLine>,
    parts: List<WorkOrderPart>,
    partNameMap: Map<Long, String>,
    dateFormat: SimpleDateFormat,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    val statusColor = when (order.status) {
        OrderStatus.RECIBIDO -> StatusRecibido
        OrderStatus.EN_DIAGNOSTICO -> StatusDiagnostico
        OrderStatus.EN_PROCESO -> StatusEnProceso
        OrderStatus.EN_ESPERA_REPUESTO -> StatusEsperaRepuesto
        OrderStatus.LISTO -> StatusListo
        OrderStatus.ENTREGADO -> StatusEntregado
        OrderStatus.CANCELADO -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = onClick
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(statusColor, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Header: vehicle info, date, mileage, status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (vehicle != null) "${vehicle.plate} - ${vehicle.brand} ${vehicle.model}" else "Orden #${order.id}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = dateFormat.format(Date(order.entryDate)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (order.entryMileage != null) {
                                Text(
                                    text = "${NumberFormat.getNumberInstance().format(order.entryMileage)} km",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    StatusChip(status = order.status)
                }

                // Services section
                if (services.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Servicios",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    services.forEach { service ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = service.description,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currencyFormat.format(service.laborCost - service.discount),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Parts section
                if (parts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Repuestos",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    parts.forEach { part ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${partNameMap[part.partId] ?: "Repuesto"} x${part.quantity}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currencyFormat.format(part.subtotal - part.discount),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Total
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Total: ",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = currencyFormat.format(order.total),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
