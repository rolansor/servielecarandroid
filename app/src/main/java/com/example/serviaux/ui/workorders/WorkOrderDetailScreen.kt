package com.example.serviaux.ui.workorders

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.example.serviaux.data.entity.OrderStatus
import com.example.serviaux.data.entity.PaymentMethod
import com.example.serviaux.ui.components.ConfirmDialog
import com.example.serviaux.ui.components.InfoRow
import com.example.serviaux.ui.components.PriorityChip
import com.example.serviaux.ui.components.SectionTitle
import com.example.serviaux.ui.components.StatusChip
import com.example.serviaux.util.PhotoUtils
import com.example.serviaux.util.ShareUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkOrderDetailScreen(
    orderId: Long,
    onNavigateBack: () -> Unit,
    viewModel: WorkOrderViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es")) }

    var showStatusDialog by remember { mutableStateOf(false) }
    var showMechanicDialog by remember { mutableStateOf(false) }
    var showServiceLineDialog by remember { mutableStateOf(false) }
    var showPartDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showDeleteServiceLineDialog by remember { mutableStateOf<Long?>(null) }
    var showDeletePartDialog by remember { mutableStateOf<Long?>(null) }

    // Camera for order photos
    val detailContext = LocalContext.current
    val detailCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success -> viewModel.onDetailPhotoTaken(success) }

    val detailPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.prepareDetailCameraFile()?.let { uri -> detailCameraLauncher.launch(uri) }
        }
    }

    fun launchDetailCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            detailContext, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            viewModel.prepareDetailCameraFile()?.let { uri -> detailCameraLauncher.launch(uri) }
        } else {
            detailPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(orderId) {
        viewModel.loadOrderDetail(orderId)
        viewModel.loadMechanics()
        viewModel.loadAvailableParts()
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.pdfFile) {
        uiState.pdfFile?.let { file ->
            ShareUtils.sharePdf(detailContext, file)
            viewModel.clearPdf()
        }
    }

    val order = uiState.selectedOrder

    // Status change dialog
    if (showStatusDialog) {
        StatusChangeDialog(
            currentStatus = order?.status ?: OrderStatus.RECIBIDO,
            onStatusSelected = { newStatus ->
                viewModel.changeStatus(newStatus)
                showStatusDialog = false
            },
            onDismiss = { showStatusDialog = false }
        )
    }

    // Mechanic assignment dialog
    if (showMechanicDialog) {
        MechanicAssignDialog(
            mechanics = uiState.mechanics,
            onMechanicSelected = { mechanicId ->
                viewModel.assignMechanic(mechanicId)
                showMechanicDialog = false
            },
            onDismiss = { showMechanicDialog = false }
        )
    }

    // Service line dialog
    if (showServiceLineDialog) {
        ServiceLineDialog(
            description = uiState.serviceLineFormDescription,
            laborCost = uiState.serviceLineFormLaborCost,
            catalogServices = uiState.catalogServices,
            onDescriptionChange = { if (it.length <= 200) viewModel.onServiceLineDescriptionChange(it) },
            onLaborCostChange = { viewModel.onServiceLineLaborCostChange(it) },
            onSave = {
                viewModel.addServiceLine()
                if (uiState.serviceLineFormDescription.trim().length >= 3
                    && (uiState.serviceLineFormLaborCost.toDoubleOrNull() ?: -1.0) >= 0.0
                ) {
                    showServiceLineDialog = false
                }
            },
            onDismiss = { showServiceLineDialog = false }
        )
    }

    // Part dialog
    if (showPartDialog) {
        PartDialog(
            availableParts = uiState.availableParts,
            selectedPartId = uiState.partFormSelectedPartId,
            quantity = uiState.partFormQuantity,
            onPartSelected = { viewModel.onPartSelectedChange(it) },
            onQuantityChange = { newVal ->
                val filtered = newVal.filter { it.isDigit() }
                viewModel.onPartQuantityChange(filtered)
            },
            onSave = {
                if (uiState.partFormSelectedPartId != null && (uiState.partFormQuantity.toIntOrNull() ?: 0) >= 1) {
                    viewModel.addPart()
                    showPartDialog = false
                } else {
                    viewModel.addPart() // triggers error message
                }
            },
            onDismiss = { showPartDialog = false }
        )
    }

    // Payment dialog
    if (showPaymentDialog) {
        val totalPaid = uiState.payments.sumOf { it.amount }
        val remainingBalance = (order?.total ?: 0.0) - totalPaid
        PaymentDialog(
            amount = uiState.paymentFormAmount,
            method = uiState.paymentFormMethod,
            notes = uiState.paymentFormNotes,
            remainingBalance = remainingBalance,
            onAmountChange = { viewModel.onPaymentAmountChange(it) },
            onMethodChange = { viewModel.onPaymentMethodChange(it) },
            onNotesChange = { viewModel.onPaymentNotesChange(it) },
            onSave = {
                val parsedAmount = uiState.paymentFormAmount.toDoubleOrNull()
                if (parsedAmount != null && parsedAmount > 0 && parsedAmount <= remainingBalance) {
                    viewModel.addPayment()
                    showPaymentDialog = false
                }
                // Dialog will show inline errors for invalid input
            },
            onDismiss = { showPaymentDialog = false }
        )
    }

    // Delete service line confirmation
    showDeleteServiceLineDialog?.let { serviceLineId ->
        val serviceLine = uiState.serviceLines.find { it.id == serviceLineId }
        if (serviceLine != null) {
            ConfirmDialog(
                title = "Eliminar Servicio",
                message = "\u00bfDesea eliminar \"${serviceLine.description}\"?",
                onConfirm = {
                    viewModel.deleteServiceLine(serviceLine)
                    showDeleteServiceLineDialog = null
                },
                onDismiss = { showDeleteServiceLineDialog = null }
            )
        }
    }

    // Delete part confirmation
    showDeletePartDialog?.let { partItemId ->
        val partItem = uiState.orderParts.find { it.id == partItemId }
        if (partItem != null) {
            ConfirmDialog(
                title = "Eliminar Repuesto",
                message = "\u00bfDesea eliminar este repuesto de la orden?",
                onConfirm = {
                    viewModel.deletePart(partItem)
                    showDeletePartDialog = null
                },
                onDismiss = { showDeletePartDialog = null }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Orden #$orderId") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (uiState.pdfGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.generatePdf(detailContext) }) {
                            Icon(Icons.Default.Share, contentDescription = "Compartir reporte")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (order == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status and priority header
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Estado",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    StatusChip(status = order.status)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Prioridad",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    PriorityChip(priority = order.priority)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showStatusDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cambiar Estado")
                                }
                                OutlinedButton(
                                    onClick = { showMechanicDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Asignar Mec\u00e1nico")
                                }
                            }
                        }
                    }
                }

                // General Information
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionTitle("Informaci\u00f3n General")
                            InfoRow(label = "Fecha Ingreso", value = dateFormat.format(Date(order.entryDate)))
                            InfoRow(label = "Cliente", value = uiState.customerName.ifBlank { "Cliente #${order.customerId}" })
                            InfoRow(label = "Veh\u00edculo", value = uiState.vehicleName.ifBlank { "Veh\u00edculo #${order.vehicleId}" })
                            InfoRow(label = "Queja del Cliente", value = order.customerComplaint)
                            InfoRow(label = "Diagn\u00f3stico Inicial", value = order.initialDiagnosis ?: "N/A")
                            InfoRow(label = "Mec\u00e1nico Asignado", value = uiState.mechanics.find { it.id == order.assignedMechanicId }?.name ?: "Sin asignar")
                            InfoRow(label = "Kilometraje Entrada", value = order.entryMileage?.let { "$it km" } ?: "N/A")
                            InfoRow(label = "Nivel Combustible", value = order.fuelLevel ?: "N/A")
                            InfoRow(label = "Notas Checklist", value = order.checklistNotes ?: "N/A")
                        }
                    }
                }

                // Photos section
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionTitle("Fotos (${uiState.detailPhotoPaths.size}/${PhotoUtils.MAX_PHOTOS})")
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(uiState.detailPhotoPaths) { index, path ->
                                    Box(modifier = Modifier.size(100.dp)) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(detailContext)
                                                .data(File(path))
                                                .build(),
                                            contentDescription = "Foto ${index + 1}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                        )
                                        IconButton(
                                            onClick = { viewModel.removeDetailPhoto(index) },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(24.dp)
                                                .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Eliminar foto",
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                                if (uiState.detailPhotoPaths.size < PhotoUtils.MAX_PHOTOS) {
                                    item {
                                        Box(
                                            modifier = Modifier
                                                .size(100.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            IconButton(onClick = { launchDetailCamera() }) {
                                                Icon(
                                                    Icons.Default.AddAPhoto,
                                                    contentDescription = "Tomar foto",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Service Lines
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionTitle("Servicios / Mano de Obra", modifier = Modifier.weight(1f))
                                IconButton(onClick = { showServiceLineDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = "Agregar servicio")
                                }
                            }

                            if (uiState.serviceLines.isEmpty()) {
                                Text(
                                    text = "No hay servicios registrados",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                items(uiState.serviceLines, key = { "sl_${it.id}" }) { serviceLine ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = serviceLine.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = String.format("$%.2f", serviceLine.laborCost),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { showDeleteServiceLineDialog = serviceLine.id }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Work Order Parts
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionTitle("Repuestos Utilizados", modifier = Modifier.weight(1f))
                                IconButton(onClick = { showPartDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = "Agregar repuesto")
                                }
                            }

                            if (uiState.orderParts.isEmpty()) {
                                Text(
                                    text = "No hay repuestos registrados",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                items(uiState.orderParts, key = { "op_${it.id}" }) { orderPart ->
                    val partName = uiState.availableParts.find { it.id == orderPart.partId }?.name ?: "Repuesto #${orderPart.partId}"
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = partName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Cant: ${orderPart.quantity} x $${String.format("%.2f", orderPart.appliedUnitPrice)} = $${String.format("%.2f", orderPart.subtotal)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { showDeletePartDialog = orderPart.id }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Totals Summary
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionTitle("Resumen")
                            InfoRow(label = "Mano de Obra", value = String.format("$%.2f", order.totalLabor))
                            InfoRow(label = "Repuestos", value = String.format("$%.2f", order.totalParts))
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "TOTAL",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = String.format("$%.2f", order.total),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Payments
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionTitle("Pagos", modifier = Modifier.weight(1f))
                                IconButton(onClick = { showPaymentDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = "Agregar pago")
                                }
                            }

                            if (uiState.payments.isEmpty()) {
                                Text(
                                    text = "No hay pagos registrados",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                items(uiState.payments, key = { "pay_${it.id}" }) { payment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = payment.method.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = String.format("$%.2f", payment.amount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = dateFormat.format(Date(payment.date)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            payment.notes?.let { note ->
                                Text(
                                    text = note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Status Log
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionTitle("Historial de Estados")

                            if (uiState.statusLog.isEmpty()) {
                                Text(
                                    text = "Sin historial",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                items(uiState.statusLog, key = { "log_${it.id}" }) { logEntry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                logEntry.oldStatus?.let { old ->
                                    Text(
                                        text = old.displayName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = " \u2192 ",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text(
                                    text = logEntry.newStatus.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = dateFormat.format(Date(logEntry.changedAt)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            logEntry.note?.let { note ->
                                Text(
                                    text = note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

// --- Dialogs ---

@Composable
private fun StatusChangeDialog(
    currentStatus: OrderStatus,
    onStatusSelected: (OrderStatus) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar Estado") },
        text = {
            Column {
                OrderStatus.entries.forEach { status ->
                    TextButton(
                        onClick = { onStatusSelected(status) },
                        enabled = status != currentStatus,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = status.displayName,
                            fontWeight = if (status == currentStatus) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun MechanicAssignDialog(
    mechanics: List<com.example.serviaux.data.entity.User>,
    onMechanicSelected: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Asignar Mec\u00e1nico") },
        text = {
            Column {
                if (mechanics.isEmpty()) {
                    Text("No hay mec\u00e1nicos disponibles")
                } else {
                    mechanics.forEach { mechanic ->
                        TextButton(
                            onClick = { onMechanicSelected(mechanic.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(mechanic.name)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceLineDialog(
    description: String,
    laborCost: String,
    catalogServices: List<com.example.serviaux.data.entity.CatalogService>,
    onDescriptionChange: (String) -> Unit,
    onLaborCostChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var laborCostError by remember { mutableStateOf<String?>(null) }
    var suggestionsExpanded by remember { mutableStateOf(false) }

    val filteredServices = remember(description, catalogServices) {
        if (description.isBlank()) emptyList()
        else catalogServices.filter {
            it.name.contains(description, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Servicio") },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = suggestionsExpanded && filteredServices.isNotEmpty(),
                    onExpandedChange = { }
                ) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            onDescriptionChange(it)
                            descriptionError = null
                            suggestionsExpanded = it.isNotBlank()
                        },
                        label = { Text("Descripci\u00f3n *") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (description.isNotBlank()) {
                                IconButton(onClick = {
                                    onDescriptionChange("")
                                    onLaborCostChange("")
                                    suggestionsExpanded = false
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpiar", modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        singleLine = true,
                        isError = descriptionError != null,
                        supportingText = if (descriptionError != null) {
                            { Text(descriptionError!!, color = MaterialTheme.colorScheme.error) }
                        } else {
                            { Text("${description.length}/200") }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                    )
                    if (filteredServices.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = suggestionsExpanded,
                            onDismissRequest = { suggestionsExpanded = false }
                        ) {
                            filteredServices.forEach { service ->
                                DropdownMenuItem(
                                    text = { Text(service.name) },
                                    onClick = {
                                        onDescriptionChange(service.name)
                                        onLaborCostChange(String.format("%.2f", service.defaultPrice))
                                        descriptionError = null
                                        laborCostError = null
                                        suggestionsExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = laborCost,
                    onValueChange = {
                        onLaborCostChange(it)
                        laborCostError = null
                    },
                    label = { Text("Costo *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = laborCostError != null,
                    supportingText = laborCostError?.let { error -> { Text(error, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                var hasError = false
                if (description.isBlank() || description.trim().length < 3) {
                    descriptionError = if (description.isBlank()) "Descripci\u00f3n es obligatoria" else "M\u00ednimo 3 caracteres"
                    hasError = true
                }
                val parsedCost = laborCost.toDoubleOrNull()
                if (laborCost.isBlank() || parsedCost == null || parsedCost < 0) {
                    laborCostError = "Costo de mano de obra es obligatorio"
                    hasError = true
                }
                if (!hasError) {
                    onSave()
                }
            }) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PartDialog(
    availableParts: List<com.example.serviaux.data.entity.Part>,
    selectedPartId: Long?,
    quantity: String,
    onPartSelected: (Long?) -> Unit,
    onQuantityChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var suggestionsExpanded by remember { mutableStateOf(false) }
    val selectedPart = availableParts.find { it.id == selectedPartId }
    var partError by remember { mutableStateOf<String?>(null) }
    var quantityError by remember { mutableStateOf<String?>(null) }

    val filteredParts = remember(searchQuery, availableParts) {
        if (searchQuery.isBlank()) emptyList()
        else availableParts.filter { it.active }.filter { part ->
            part.code?.contains(searchQuery, ignoreCase = true) == true ||
            part.name.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Repuesto") },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = suggestionsExpanded && filteredParts.isNotEmpty(),
                    onExpandedChange = { }
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            partError = null
                            suggestionsExpanded = it.isNotBlank()
                            if (it.isBlank()) onPartSelected(null)
                        },
                        label = { Text("Buscar por c\u00f3digo o nombre *") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    onPartSelected(null)
                                    suggestionsExpanded = false
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Limpiar", modifier = Modifier.size(20.dp))
                                }
                            }
                        },
                        singleLine = true,
                        isError = partError != null,
                        supportingText = partError?.let { error -> { Text(error, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                    )
                    if (filteredParts.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = suggestionsExpanded,
                            onDismissRequest = { suggestionsExpanded = false }
                        ) {
                            filteredParts.forEach { part ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("${part.code ?: ""} - ${part.name}")
                                            Text(
                                                text = "Stock: ${part.currentStock} | $${String.format("%.2f", part.salePrice ?: part.unitCost)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        onPartSelected(part.id)
                                        searchQuery = "${part.code ?: ""} - ${part.name}"
                                        partError = null
                                        suggestionsExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                if (selectedPart != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Precio: $${String.format("%.2f", selectedPart.salePrice ?: selectedPart.unitCost)} | Stock: ${selectedPart.currentStock}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = {
                        onQuantityChange(it)
                        quantityError = null
                    },
                    label = { Text("Cantidad *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = quantityError != null,
                    supportingText = quantityError?.let { error -> { Text(error, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                var hasError = false
                if (selectedPartId == null) {
                    partError = "Debe seleccionar un repuesto"
                    hasError = true
                }
                val parsedQty = quantity.toIntOrNull()
                if (parsedQty == null || parsedQty < 1) {
                    quantityError = "Cantidad debe ser al menos 1"
                    hasError = true
                }
                if (!hasError) {
                    onSave()
                }
            }) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentDialog(
    amount: String,
    method: PaymentMethod,
    notes: String,
    remainingBalance: Double,
    onAmountChange: (String) -> Unit,
    onMethodChange: (PaymentMethod) -> Unit,
    onNotesChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var methodDropdownExpanded by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Pago") },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        onAmountChange(it)
                        amountError = null
                    },
                    label = { Text("Monto *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amountError != null,
                    supportingText = if (amountError != null) {
                        { Text(amountError!!, color = MaterialTheme.colorScheme.error) }
                    } else {
                        { Text("Balance pendiente: $${String.format("%.2f", remainingBalance)}") }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = methodDropdownExpanded,
                    onExpandedChange = { methodDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = method.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("M\u00e9todo de Pago") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = methodDropdownExpanded,
                        onDismissRequest = { methodDropdownExpanded = false }
                    ) {
                        PaymentMethod.entries.forEach { pm ->
                            DropdownMenuItem(
                                text = { Text(pm.displayName) },
                                onClick = {
                                    onMethodChange(pm)
                                    methodDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text("Notas") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedAmount = amount.toDoubleOrNull()
                if (parsedAmount == null || parsedAmount <= 0) {
                    amountError = "Ingrese un monto v\u00e1lido mayor a 0"
                } else if (parsedAmount > remainingBalance) {
                    amountError = "El monto excede el balance pendiente ($${String.format("%.2f", remainingBalance)})"
                } else {
                    onSave()
                }
            }) {
                Text("Registrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
