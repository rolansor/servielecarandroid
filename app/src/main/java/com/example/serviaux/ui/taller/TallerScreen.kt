/**
 * TallerScreen.kt - "Órdenes activas": la pantalla de inicio del rediseño.
 *
 * Reemplaza al dashboard de módulos. Al abrir, nadie ve métricas: ve los
 * autos que el taller tiene dentro, agrupados por estado. Cada estado es
 * una sección vertical con su chip, el conteo y las tarjetas de orden
 * (placa grande, vehículo, queja, mecánico, antigüedad); los estados sin
 * órdenes no se muestran y las secciones van separadas por una línea.
 * Tocar la cabecera de una sección abre la lista filtrada; tocar una
 * tarjeta abre el detalle.
 *
 * Conserva el diálogo de primer arranque ("¿cargar datos de ejemplo?").
 */
package com.example.serviaux.ui.taller

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.serviaux.data.entity.OrderStatus
import com.example.serviaux.data.entity.WorkOrder
import com.example.serviaux.ui.components.StatusChip
import com.example.serviaux.ui.theme.Aqua700
import com.example.serviaux.ui.theme.Neutral100
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun TallerScreen(
    onNavigateToOrderDetail: (Long) -> Unit,
    onNavigateToOrdersByStatus: (OrderStatus) -> Unit,
    onNavigateToNewOrder: () -> Unit,
    viewModel: TallerViewModel = viewModel()
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
                if (!uiState.loadingSampleData) {
                    TextButton(onClick = { viewModel.loadSampleData() }) { Text("Cargar ejemplos") }
                }
            },
            dismissButton = {
                if (!uiState.loadingSampleData) {
                    OutlinedButton(onClick = { viewModel.dismissSampleDataDialog() }) { Text("Empezar vacío") }
                }
            }
        )
    }

    val headerDate = remember {
        SimpleDateFormat("EEEE d 'de' MMMM", Locale("es")).format(Date())
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToNewOrder,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva orden")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Órdenes activas",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "$headerDate · ${uiState.totalActivos} " +
                                if (uiState.totalActivos == 1) "auto dentro" else "autos dentro",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Iniciales del usuario en sesión (verde-agua = presencia, no acción)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Aqua700),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials(uiState.currentUserName),
                            style = MaterialTheme.typography.labelLarge,
                            color = Neutral100
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Solo los estados con órdenes; los vacíos no aparecen
            val sections = TALLER_SECTION_ORDER.mapNotNull { status ->
                uiState.ordersByStatus[status].orEmpty()
                    .takeIf { it.isNotEmpty() }?.let { status to it }
            }

            if (sections.isEmpty()) {
                item(key = "empty_all") {
                    Text(
                        text = "No hay órdenes activas en el taller",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }
            }

            sections.forEachIndexed { index, (status, orders) ->
                if (index > 0) {
                    item(key = "divider_$status") {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
                item(key = "header_$status") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .clickable { onNavigateToOrdersByStatus(status) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusChip(status = status)
                        Text(
                            text = "${orders.size}",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
                items(orders, key = { it.id }) { order ->
                    TallerOrderCard(
                        order = order,
                        uiState = uiState,
                        onClick = { onNavigateToOrderDetail(order.id) }
                    )
                }
            }
        }
    }
}

/** Tarjeta de orden del tablero: la placa manda, el resto acompaña. */
@Composable
private fun TallerOrderCard(
    order: WorkOrder,
    uiState: TallerUiState,
    onClick: () -> Unit
) {
    val vehicle = uiState.vehicleMap[order.vehicleId]
    val customerName = uiState.customerMap[order.customerId]
    val mechanicName = order.assignedMechanicId?.let { uiState.userNameMap[it] }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = vehicle?.plate ?: "Orden #${order.id}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = timeAgo(order.entryDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (vehicle != null) {
                Text(
                    text = listOfNotNull(vehicle.brand, vehicle.model, vehicle.year?.toString())
                        .joinToString(" "),
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Text(
                text = listOfNotNull(
                    order.customerComplaint.takeIf { it.isNotBlank() },
                    mechanicName ?: customerName
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        .take(2).map { it.first().uppercaseChar() }.joinToString("")
        .ifEmpty { "?" }

private fun timeAgo(entryMillis: Long): String {
    val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - entryMillis)
    return when {
        days <= 0L -> "hoy"
        days == 1L -> "ayer"
        else -> "hace $days días"
    }
}
