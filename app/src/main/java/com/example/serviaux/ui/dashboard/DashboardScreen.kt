package com.example.serviaux.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.serviaux.data.entity.OrderStatus
import com.example.serviaux.data.entity.UserRole
import com.example.serviaux.ui.components.StatusChip
import com.example.serviaux.ui.theme.StatusDiagnostico
import com.example.serviaux.ui.theme.StatusEnProceso
import com.example.serviaux.ui.theme.StatusEntregado
import com.example.serviaux.ui.theme.StatusEsperaRepuesto
import com.example.serviaux.ui.theme.StatusListo
import com.example.serviaux.ui.theme.StatusRecibido
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToCustomers: () -> Unit,
    onNavigateToVehicles: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToOrdersByStatus: (OrderStatus) -> Unit = {},
    onNavigateToOrderDetail: (Long) -> Unit = {},
    onNavigateToParts: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToCatalogSettings: () -> Unit,
    onNavigateToNewOrder: () -> Unit,
    onNavigateToNewCustomer: () -> Unit,
    onNavigateToNewVehicle: () -> Unit,
    onNavigateToAppointments: () -> Unit = {},
    onNavigateToCommissions: () -> Unit = {},
    onNavigateToBackup: () -> Unit,
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.showSampleDataDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Datos iniciales") },
            text = {
                if (uiState.loadingSampleData) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("Cargando datos de ejemplo...")
                    }
                } else {
                    Text("¿Desea cargar datos de ejemplo para explorar la aplicación? Incluye clientes, vehículos, repuestos y órdenes de prueba.")
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.loadSampleData() },
                    enabled = !uiState.loadingSampleData
                ) {
                    Text("Cargar ejemplos")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.dismissSampleDataDialog() },
                    enabled = !uiState.loadingSampleData
                ) {
                    Text("Empezar de cero")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Serviaux") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    if (uiState.currentUserRole == UserRole.ADMIN) {
                        IconButton(onClick = onNavigateToCatalogSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configuraci\u00f3n"
                            )
                        }
                    }
                    IconButton(onClick = {
                        viewModel.logout()
                        onLogout()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesi\u00f3n"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header with avatar and greeting
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar with initials
                val initials = uiState.currentUserName
                    .split(" ")
                    .take(2)
                    .mapNotNull { it.firstOrNull()?.uppercase() }
                    .joinToString("")
                    .ifEmpty { "?" }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = initials,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Hola, ${uiState.currentUserName}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = uiState.currentUserRole.displayName,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("es"))
                        Text(
                            text = dateFormat.format(Date()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // KPI Cards with gradients
            Text(
                text = "\u00d3rdenes por Estado",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                KpiCard("Recibido", uiState.recibido, StatusRecibido, Modifier.weight(1f)) { onNavigateToOrdersByStatus(OrderStatus.RECIBIDO) }
                KpiCard("Diagn\u00f3stico", uiState.enDiagnostico, StatusDiagnostico, Modifier.weight(1f)) { onNavigateToOrdersByStatus(OrderStatus.EN_DIAGNOSTICO) }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                KpiCard("En Proceso", uiState.enProceso, StatusEnProceso, Modifier.weight(1f)) { onNavigateToOrdersByStatus(OrderStatus.EN_PROCESO) }
                KpiCard("En Espera", uiState.enEsperaRepuesto, StatusEsperaRepuesto, Modifier.weight(1f)) { onNavigateToOrdersByStatus(OrderStatus.EN_ESPERA_REPUESTO) }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                KpiCard("Listo", uiState.listo, StatusListo, Modifier.weight(1f)) { onNavigateToOrdersByStatus(OrderStatus.LISTO) }
                KpiCard("Entregado", uiState.entregado, StatusEntregado, Modifier.weight(1f)) { onNavigateToOrdersByStatus(OrderStatus.ENTREGADO) }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Actions - horizontal row
            Text(
                text = "Acciones R\u00e1pidas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Turnos summary
            val totalTurnos = uiState.turnosPendientes + uiState.turnosConfirmados
            if (totalTurnos > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToAppointments),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Turnos del día",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "${uiState.turnosPendientes} pendientes, ${uiState.turnosConfirmados} confirmados",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Text(
                            text = totalTurnos.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                QuickActionCard("Cliente", Icons.Default.PersonAdd, onNavigateToNewCustomer, Modifier.weight(1f))
                QuickActionCard("Vehículo", Icons.Default.DirectionsCar, onNavigateToNewVehicle, Modifier.weight(1f))
                QuickActionCard("Orden", Icons.Default.NoteAdd, onNavigateToNewOrder, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Active Orders
            if (uiState.recentOrders.isNotEmpty()) {
                Text(
                    text = "\u00d3rdenes Activas Recientes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                uiState.recentOrders.forEach { order ->
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onNavigateToOrderDetail(order.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .fillMaxHeight()
                                    .background(statusColor, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Orden #${order.id}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val customerName = uiState.customerMap[order.customerId] ?: ""
                                    val vehiclePlate = uiState.vehicleMap[order.vehicleId]?.substringBefore(" -") ?: ""
                                    if (customerName.isNotBlank() || vehiclePlate.isNotBlank()) {
                                        Text(
                                            text = listOf(customerName, vehiclePlate).filter { it.isNotBlank() }.joinToString(" - "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                StatusChip(status = order.status)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Modules
            Text(
                text = "M\u00f3dulos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Build module list
            val modules = buildList {
                add(Triple("Clientes", Icons.Default.People, onNavigateToCustomers))
                add(Triple("Vehículos", Icons.Default.DirectionsCar, onNavigateToVehicles))
                add(Triple("Órdenes", Icons.Default.Build, onNavigateToOrders))
                add(Triple("Turnos", Icons.Default.CalendarMonth, onNavigateToAppointments))
                add(Triple("Repuestos", Icons.Default.Handyman, onNavigateToParts))
                if (uiState.currentUserRole == UserRole.ADMIN) {
                    add(Triple("Usuarios", Icons.Default.Group, onNavigateToUsers))
                    add(Triple("Comisiones", Icons.Default.Payments, onNavigateToCommissions))
                    add(Triple("Reportes", Icons.Default.Assessment, onNavigateToReports))
                    add(Triple("Catálogos", Icons.Default.Settings, onNavigateToCatalogSettings))
                    add(Triple("Respaldos", Icons.Default.SaveAlt, onNavigateToBackup))
                }
            }
            // Grid de 3 columnas
            modules.chunked(3).forEach { rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowItems.forEach { (title, icon, onClick) ->
                        ModuleCard(title, icon, onClick, Modifier.weight(1f))
                    }
                    // Fill empty slots to keep equal widths
                    repeat(3 - rowItems.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun KpiCard(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            color.copy(alpha = 0.15f),
                            color.copy(alpha = 0.05f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    fontSize = 36.sp
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun ModuleCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
