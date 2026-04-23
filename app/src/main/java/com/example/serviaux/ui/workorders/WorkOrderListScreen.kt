/**
 * WorkOrderListScreen.kt - Pantalla de lista de órdenes de trabajo.
 *
 * Muestra todas las órdenes con búsqueda por cliente/placa y filtros (año,
 * estado, estado de pago) accesibles desde un ModalBottomSheet activado por
 * un botón "Filtros". Cada tarjeta muestra: número de orden, placa, estado,
 * prioridad, total, abono y saldo. Permite navegar al detalle o crear una
 * nueva orden. Soporta filtro inicial desde el dashboard al clickear un estado.
 */
package com.example.serviaux.ui.workorders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.serviaux.data.entity.OrderStatus
import com.example.serviaux.ui.components.EmptyState
import com.example.serviaux.ui.components.PriorityChip
import com.example.serviaux.ui.components.ShimmerLoadingList
import com.example.serviaux.ui.components.StatusChip
import com.example.serviaux.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkOrderListScreen(
    initialFilter: com.example.serviaux.data.entity.OrderStatus? = null,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToForm: () -> Unit,
    viewModel: WorkOrderViewModel = viewModel(factory = WorkOrderViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es")) }

    LaunchedEffect(initialFilter) {
        viewModel.loadOrders(filter = initialFilter)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    var showFilterSheet by remember { mutableStateOf(false) }

    val activeFilterCount = remember(uiState.filter, uiState.filterYear, uiState.paymentFilter) {
        var count = 0
        if (uiState.filter != null) count++
        if (uiState.filterYear != java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)) count++
        if (uiState.paymentFilter != PaymentFilter.TODAS) count++
        count
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                collapsedHeight = 40.dp,
                title = { Text("Órdenes de Trabajo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToForm) {
                Icon(Icons.Default.Add, contentDescription = "Nueva orden")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("Buscar por cliente o placa...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true
            )

            // Compact filter row: button + active filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { showFilterSheet = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (activeFilterCount == 0) "Filtros" else "Filtros ($activeFilterCount)"
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = { showFilterSheet = true },
                        label = { Text(uiState.filterYear.toString()) }
                    )
                    uiState.filter?.let { status ->
                        AssistChip(
                            onClick = { viewModel.loadOrders(filter = null) },
                            label = { Text(status.displayName) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Quitar",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                    if (uiState.paymentFilter != PaymentFilter.TODAS) {
                        AssistChip(
                            onClick = { viewModel.onPaymentFilterChanged(PaymentFilter.TODAS) },
                            label = { Text(uiState.paymentFilter.displayName) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Quitar",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            if (!uiState.isListLoaded) {
                ShimmerLoadingList()
            } else if (uiState.filteredOrders.isEmpty()) {
                EmptyState(
                    message = if (uiState.searchQuery.isNotEmpty()) "Sin resultados para \"${uiState.searchQuery}\"" else "No se encontraron órdenes",
                    icon = Icons.Default.Assignment
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.filteredOrders, key = { it.id }) { order ->
                        val statusColor = when (order.status) {
                            OrderStatus.RECIBIDO -> StatusRecibido
                            OrderStatus.EN_DIAGNOSTICO -> StatusDiagnostico
                            OrderStatus.EN_PROCESO -> StatusEnProceso
                            OrderStatus.EN_ESPERA_REPUESTO -> StatusEsperaRepuesto
                            OrderStatus.LISTO -> StatusListo
                            OrderStatus.ENTREGADO -> StatusEntregado
                            OrderStatus.CERRADO -> StatusCancelado
                        }
                        val (paid, discount) = uiState.paymentSummaryMap[order.id] ?: (0.0 to 0.0)
                        val balance = (order.total - paid - discount).coerceAtLeast(0.0)
                        val balanceColor = when {
                            order.total <= 0.0 -> MaterialTheme.colorScheme.onSurfaceVariant
                            balance <= 0.01 -> StatusListo
                            paid > 0.0 || discount > 0.0 -> Amber40
                            else -> BrakeRed40
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { onNavigateToDetail(order.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .background(
                                            statusColor,
                                            RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                                        )
                                )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Orden #${order.id}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val customerName = uiState.customerMap[order.customerId] ?: ""
                                        val vehiclePlate = uiState.vehicleMap[order.vehicleId]?.substringBefore(" -") ?: ""
                                        if (customerName.isNotBlank() || vehiclePlate.isNotBlank()) {
                                            Text(
                                                text = listOf(customerName, vehiclePlate).filter { it.isNotBlank() }.joinToString(" - "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    StatusChip(status = order.status)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PriorityChip(priority = order.priority)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = order.orderType.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = dateFormat.format(Date(order.entryDate)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = order.customerComplaint,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "Total: $%.2f", order.total),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "Abono: $%.2f", paid),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = String.format(Locale.US, "Saldo: $%.2f", balance),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = balanceColor
                                    )
                                }
                            }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            currentYear = uiState.filterYear,
            availableYears = uiState.availableYears,
            currentStatus = uiState.filter,
            currentPaymentFilter = uiState.paymentFilter,
            onYearSelected = { viewModel.loadOrders(year = it) },
            onStatusSelected = { viewModel.loadOrders(filter = it) },
            onPaymentFilterSelected = { viewModel.onPaymentFilterChanged(it) },
            onClearAll = {
                viewModel.loadOrders(
                    filter = null,
                    year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                )
                viewModel.onPaymentFilterChanged(PaymentFilter.TODAS)
            },
            onDismiss = { showFilterSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterBottomSheet(
    currentYear: Int,
    availableYears: List<Int>,
    currentStatus: OrderStatus?,
    currentPaymentFilter: PaymentFilter,
    onYearSelected: (Int) -> Unit,
    onStatusSelected: (OrderStatus?) -> Unit,
    onPaymentFilterSelected: (PaymentFilter) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtros",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onClearAll) { Text("Limpiar") }
            }

            Spacer(Modifier.height(8.dp))

            FilterSectionTitle("Año")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableYears.forEach { year ->
                    FilterChip(
                        selected = currentYear == year,
                        onClick = { onYearSelected(year) },
                        label = { Text(year.toString()) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            FilterSectionTitle("Estado")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = currentStatus == null,
                    onClick = { onStatusSelected(null) },
                    label = { Text("Todas") }
                )
                OrderStatus.entries.forEach { status ->
                    FilterChip(
                        selected = currentStatus == status,
                        onClick = { onStatusSelected(status) },
                        label = { Text(status.displayName) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            FilterSectionTitle("Estado de pago")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PaymentFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = currentPaymentFilter == filter,
                        onClick = { onPaymentFilterSelected(filter) },
                        label = { Text(filter.displayName) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            FilledTonalButton(
                onClick = {
                    scope.launch {
                        sheetState.hide()
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Aplicar")
            }
        }
    }
}

@Composable
private fun FilterSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
