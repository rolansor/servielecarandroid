/**
 * CommissionScreen.kt - Pantalla de gestión de comisiones (solo admin).
 *
 * Muestra las comisiones pendientes de pago agrupadas por mecánico.
 * Permite seleccionar comisiones individual o por grupo y pagarlas
 * en lote. Tras el pago muestra un resumen con opción de generar
 * y compartir PDF del reporte de comisiones pagadas.
 * Incluye pestaña de historial de comisiones ya pagadas.
 */
package com.example.serviaux.ui.commissions

import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.serviaux.data.entity.PendingCommissionRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommissionScreen(
    onNavigateBack: () -> Unit,
    viewModel: CommissionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = 40.dp,
                title = { Text("Comisiones") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            if (!uiState.paymentCompleted && !uiState.showHistory && uiState.selectedIds.isNotEmpty()) {
                val selectedTotal = uiState.pendingCommissions
                    .filter { it.id in uiState.selectedIds }
                    .sumOf { it.commissionAmount }
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${uiState.selectedIds.size} seleccionadas",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Total: $${String.format(Locale.US, "%.2f", selectedTotal)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Button(
                            onClick = { viewModel.paySelected() },
                            enabled = !uiState.isLoading
                        ) {
                            Text("Pagar seleccionados")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.paymentCompleted) {
                PaymentSummary(
                    paidCommissions = uiState.paidCommissions,
                    mechanicNames = uiState.mechanicNames,
                    pdfGenerating = uiState.pdfGenerating,
                    onGenerateAndShare = { viewModel.generateAndSharePdf(context) },
                    onBack = { viewModel.resetPaymentState() }
                )
            } else {
                // Tabs: Pendientes / Historial
                PrimaryTabRow(
                    selectedTabIndex = if (uiState.showHistory) 1 else 0
                ) {
                    Tab(
                        selected = !uiState.showHistory,
                        onClick = { if (uiState.showHistory) viewModel.toggleHistory() },
                        text = { Text("Pendientes") }
                    )
                    Tab(
                        selected = uiState.showHistory,
                        onClick = { if (!uiState.showHistory) viewModel.toggleHistory() },
                        text = { Text("Historial") }
                    )
                }

                if (uiState.isLoading && !uiState.showHistory && uiState.pendingCommissions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.showHistory) {
                    if (uiState.isLoading && uiState.paidHistory.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        PaidHistoryList(
                            commissions = uiState.paidHistory,
                            mechanicNames = uiState.historyMechanicNames
                        )
                    }
                } else {
                    PendingCommissionsList(
                        commissions = uiState.pendingCommissions,
                        mechanicNames = uiState.mechanicNames,
                        selectedIds = uiState.selectedIds,
                        onToggleSelection = { viewModel.toggleSelection(it) },
                        onSelectAllForMechanic = { viewModel.selectAllForMechanic(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingCommissionsList(
    commissions: List<PendingCommissionRow>,
    mechanicNames: Map<Long, String>,
    selectedIds: Set<Long>,
    onToggleSelection: (Long) -> Unit,
    onSelectAllForMechanic: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (commissions.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No hay comisiones pendientes",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val grouped = commissions.groupBy { it.mechanicId }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grouped.forEach { (mechanicId, mechanicCommissions) ->
            item(key = "header_$mechanicId") {
                val allSelected = mechanicCommissions.all { it.id in selectedIds }
                val mechanicTotal = mechanicCommissions.sumOf { it.commissionAmount }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectAllForMechanic(mechanicId) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = allSelected,
                            onCheckedChange = { onSelectAllForMechanic(mechanicId) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mechanicNames[mechanicId] ?: "Mecánico #$mechanicId",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${mechanicCommissions.size} comisiones pendientes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", mechanicTotal)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            items(mechanicCommissions, key = { "comm_${it.id}" }) { commission ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleSelection(commission.id) }
                        .padding(start = 16.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = commission.id in selectedIds,
                        onCheckedChange = { onToggleSelection(commission.id) }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Orden #${commission.workOrderId}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = commission.vehiclePlate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", commission.commissionAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            item(key = "divider_$mechanicId") {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun PaidHistoryList(
    commissions: List<PendingCommissionRow>,
    mechanicNames: Map<Long, String>,
    modifier: Modifier = Modifier
) {
    val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es"))

    if (commissions.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No hay comisiones pagadas",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val grouped = commissions.groupBy { it.mechanicId }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grouped.forEach { (mechanicId, mechanicCommissions) ->
            item(key = "hist_header_$mechanicId") {
                val mechanicTotal = mechanicCommissions.sumOf { it.commissionAmount }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mechanicNames[mechanicId] ?: "Mecánico #$mechanicId",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${mechanicCommissions.size} comisiones pagadas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", mechanicTotal)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            items(mechanicCommissions, key = { "hist_${it.id}" }) { commission ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Orden #${commission.workOrderId}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row {
                            Text(
                                text = commission.vehiclePlate,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            commission.paidAt?.let {
                                Text(
                                    text = " · ${dateFmt.format(Date(it))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", commission.commissionAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            item(key = "hist_divider_$mechanicId") {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun PaymentSummary(
    paidCommissions: List<PendingCommissionRow>,
    mechanicNames: Map<Long, String>,
    pdfGenerating: Boolean,
    onGenerateAndShare: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPaid = paidCommissions.sumOf { it.commissionAmount }
    val grouped = paidCommissions.groupBy { it.mechanicId }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Pago registrado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${paidCommissions.size} comisiones pagadas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Total: $${String.format(Locale.US, "%.2f", totalPaid)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        grouped.forEach { (mechanicId, commissions) ->
            item(key = "summary_$mechanicId") {
                Card {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = mechanicNames[mechanicId] ?: "Mecánico #$mechanicId",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        commissions.forEach { c ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Orden #${c.workOrderId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = c.vehiclePlate,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", c.commissionAmount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Subtotal",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", commissions.sumOf { it.commissionAmount })}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onGenerateAndShare,
                    enabled = !pdfGenerating,
                    modifier = Modifier.weight(1f)
                ) {
                    if (pdfGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(16.dp).width(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    } else {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text("Generar y Compartir PDF")
                }
                Button(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Volver")
                }
            }
        }
    }
}
