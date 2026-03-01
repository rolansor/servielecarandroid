/**
 * WorkOrderDetailScreen.kt - Pantalla de detalle de una orden de trabajo.
 *
 * Es la pantalla más compleja del sistema. Muestra:
 * - Información del vehículo y cliente.
 * - Queja del cliente y diagnóstico.
 * - Checklist de accesorios recibidos.
 * - Tabla de servicios (mano de obra) con CRUD inline.
 * - Tabla de repuestos con búsqueda, precios y ajuste de stock.
 * - Registro de pagos con descuentos y múltiples métodos de pago.
 * - Historial de cambios de estado.
 * - Galería de fotos y archivos adjuntos.
 * - Cambio de estado, asignación de mecánico, generación de PDF.
 * - Notas de entrega, número de factura.
 * - Eliminación de la orden (solo admin).
 */
package com.example.serviaux.ui.workorders

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.mutableIntStateOf
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
    onNavigateToEdit: (Long) -> Unit = {},
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
    var showDeleteOrderDialog by remember { mutableStateOf(false) }
    var deleteConfirmationText by remember { mutableStateOf("") }
    var viewingPhotoPath by remember { mutableStateOf<String?>(null) }
    var viewingPhotoIndex by remember { mutableIntStateOf(-1) }
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }

    // Camera & Gallery for order photos
    val detailContext = LocalContext.current
    val detailCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success -> viewModel.onDetailPhotoTaken(success) }

    val detailGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> uris.forEach { uri -> viewModel.addDetailPhotoFromGallery(uri) } }

    val detailFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> uris.forEach { uri -> viewModel.addDetailFile(uri) } }

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

    LaunchedEffect(uiState.orderDeleted) {
        if (uiState.orderDeleted) {
            viewModel.clearOrderDeleted()
            onNavigateBack()
        }
    }

    val order = uiState.selectedOrder
    val isEntregado = order?.status == OrderStatus.ENTREGADO
    val isAdmin = uiState.isAdmin

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
            assignedMechanicIds = uiState.orderMechanics.map { it.mechanicId },
            onMechanicAdded = { mechanicId, commType, commValue ->
                viewModel.addMechanicToOrder(mechanicId, commType, commValue)
                showMechanicDialog = false
            },
            onDismiss = { showMechanicDialog = false }
        )
    }

    // Service line dialog
    if (showServiceLineDialog) {
        val vehicleType = uiState.selectedVehicle?.vehicleType
        val filteredCatalogServices = if (vehicleType.isNullOrBlank()) {
            uiState.catalogServices
        } else {
            uiState.catalogServices.filter { it.vehicleType == null || it.vehicleType == vehicleType }
        }
        val isEditingServiceLine = uiState.editingServiceLineId != null
        ServiceLineDialog(
            description = uiState.serviceLineFormDescription,
            laborCost = uiState.serviceLineFormLaborCost,
            catalogServices = filteredCatalogServices,
            isEditing = isEditingServiceLine,
            onDescriptionChange = { if (it.length <= 200) viewModel.onServiceLineDescriptionChange(it) },
            onLaborCostChange = { viewModel.onServiceLineLaborCostChange(it) },
            onSave = {
                viewModel.saveServiceLine()
                if (uiState.serviceLineFormDescription.trim().length >= 3
                    && (uiState.serviceLineFormLaborCost.toDoubleOrNull() ?: -1.0) >= 0.0
                ) {
                    showServiceLineDialog = false
                }
            },
            onDismiss = {
                viewModel.cancelEditServiceLine()
                showServiceLineDialog = false
            }
        )
    }

    // Part dialog
    if (showPartDialog) {
        val isEditingPart = uiState.editingWorkOrderPartId != null
        PartDialog(
            availableParts = uiState.availableParts,
            selectedPartId = uiState.partFormSelectedPartId,
            quantity = uiState.partFormQuantity,
            price = uiState.partFormPrice,
            isEditing = isEditingPart,
            onPartSelected = { viewModel.onPartSelectedChange(it) },
            onQuantityChange = { newVal ->
                val filtered = newVal.filter { it.isDigit() }
                viewModel.onPartQuantityChange(filtered)
            },
            onPriceChange = { viewModel.onPartPriceChange(it) },
            onSave = {
                if (isEditingPart) {
                    viewModel.updatePart()
                    showPartDialog = false
                } else if (uiState.partFormSelectedPartId != null && (uiState.partFormQuantity.toIntOrNull() ?: 0) >= 1) {
                    viewModel.addPart()
                    showPartDialog = false
                } else {
                    viewModel.addPart() // triggers error message
                }
            },
            onDismiss = {
                viewModel.cancelEditPart()
                showPartDialog = false
            }
        )
    }

    // Payment dialog
    if (showPaymentDialog) {
        val totalPaid = uiState.payments.sumOf { it.amount }
        val totalDiscounts = uiState.payments.sumOf { it.discount }
        val remainingBalance = (order?.total ?: 0.0) - totalPaid - totalDiscounts
        PaymentDialog(
            amount = uiState.paymentFormAmount,
            discount = uiState.paymentFormDiscount,
            method = uiState.paymentFormMethod,
            notes = uiState.paymentFormNotes,
            remainingBalance = remainingBalance,
            onAmountChange = { viewModel.onPaymentAmountChange(it) },
            onDiscountChange = { viewModel.onPaymentDiscountChange(it) },
            onMethodChange = { viewModel.onPaymentMethodChange(it) },
            onNotesChange = { viewModel.onPaymentNotesChange(it) },
            onSave = {
                val parsedAmount = uiState.paymentFormAmount.toDoubleOrNull() ?: 0.0
                val parsedDiscount = uiState.paymentFormDiscount.toDoubleOrNull() ?: 0.0
                if ((parsedAmount + parsedDiscount) > 0 && (parsedAmount + parsedDiscount) <= remainingBalance + 0.01) {
                    viewModel.addPayment()
                    showPaymentDialog = false
                }
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

    // Delete order confirmation dialog - requires typing order number
    if (showDeleteOrderDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteOrderDialog = false
                deleteConfirmationText = ""
            },
            title = {
                Text("Eliminar Orden #$orderId", color = MaterialTheme.colorScheme.error)
            },
            text = {
                Column {
                    Text(
                        "Esta accion eliminara permanentemente la orden, todos sus servicios, repuestos, pagos, historial y fotos.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Escriba el numero de la orden ($orderId) para confirmar:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = deleteConfirmationText,
                        onValueChange = { deleteConfirmationText = it.filter { c -> c.isDigit() } },
                        label = { Text("Numero de orden") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteOrder(orderId)
                        showDeleteOrderDialog = false
                        deleteConfirmationText = ""
                    },
                    enabled = deleteConfirmationText == orderId.toString(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteOrderDialog = false
                    deleteConfirmationText = ""
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Unsaved changes dialog
    if (showUnsavedChangesDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog = false },
            title = { Text("Cambios sin guardar") },
            text = { Text("\u00bfDesea guardar los cambios antes de salir?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.saveDetailFields()
                    showUnsavedChangesDialog = false
                    onNavigateBack()
                }) {
                    Text("Guardar y salir")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showUnsavedChangesDialog = false
                        onNavigateBack()
                    }) {
                        Text("Salir sin guardar")
                    }
                    TextButton(onClick = { showUnsavedChangesDialog = false }) {
                        Text("Cancelar")
                    }
                }
            }
        )
    }

    // Full-screen photo viewer
    viewingPhotoPath?.let { path ->
        AlertDialog(
            onDismissRequest = {
                viewingPhotoPath = null
                viewingPhotoIndex = -1
            },
            confirmButton = {
                TextButton(onClick = {
                    viewingPhotoPath = null
                    viewingPhotoIndex = -1
                }) {
                    Text("Cerrar")
                }
            },
            dismissButton = {
                if (!isEntregado && viewingPhotoIndex >= 0) {
                    TextButton(
                        onClick = {
                            viewModel.removeDetailPhoto(viewingPhotoIndex)
                            viewingPhotoPath = null
                            viewingPhotoIndex = -1
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Eliminar")
                    }
                }
            },
            text = {
                AsyncImage(
                    model = ImageRequest.Builder(detailContext)
                        .data(File(path))
                        .build(),
                    contentDescription = "Foto",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                )
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Orden #$orderId") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.detailFieldsChanged) {
                            showUnsavedChangesDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onNavigateToEdit(orderId) },
                        enabled = !isEntregado
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar orden")
                    }
                    IconButton(
                        onClick = { showDeleteOrderDialog = true },
                        enabled = !isEntregado
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar orden",
                            tint = if (isEntregado) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.error
                        )
                    }
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
                            InfoRow(label = "Tipo de Orden", value = order.orderType.displayName)
                            InfoRow(label = "Queja del Cliente", value = order.customerComplaint)
                            InfoRow(label = "Condición de Llegada", value = order.arrivalCondition.displayName)
                            InfoRow(label = "Mec\u00e1nico Asignado", value = uiState.mechanics.find { it.id == order.assignedMechanicId }?.name ?: "Sin asignar")
                            // Checklist items
                            val checklistItems = remember(order.checklistNotes) {
                                order.checklistNotes?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                            }
                            InfoRow(
                                label = "Checklist",
                                value = if (checklistItems.isEmpty()) "Sin items marcados" else checklistItems.joinToString(", ")
                            )
                        }
                    }
                }

                // Editable detail fields
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionTitle("Datos del Proceso")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = uiState.detailEntryMileage,
                                onValueChange = { viewModel.onDetailEntryMileageChange(it) },
                                label = { Text("Kilometraje de Entrada") },
                                singleLine = true,
                                enabled = !isEntregado,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            var fuelExpanded by remember { mutableStateOf(false) }
                            val fuelLevels = listOf("Vac\u00edo", "1/4", "1/2", "3/4", "Lleno")
                            ExposedDropdownMenuBox(
                                expanded = fuelExpanded,
                                onExpandedChange = { if (!isEntregado) fuelExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = uiState.detailFuelLevel,
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = !isEntregado,
                                    label = { Text("Nivel de Combustible") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelExpanded) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                )
                                ExposedDropdownMenu(
                                    expanded = fuelExpanded,
                                    onDismissRequest = { fuelExpanded = false }
                                ) {
                                    fuelLevels.forEach { level ->
                                        DropdownMenuItem(
                                            text = { Text(level) },
                                            onClick = {
                                                viewModel.onDetailFuelLevelChange(level)
                                                fuelExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = uiState.detailDeliveryNote,
                                onValueChange = { viewModel.onDetailDeliveryNoteChange(it) },
                                label = { Text("Nota de Entrega") },
                                singleLine = true,
                                enabled = !isEntregado,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = uiState.detailInvoiceNumber,
                                onValueChange = { viewModel.onDetailInvoiceNumberChange(it) },
                                label = { Text("Factura") },
                                singleLine = true,
                                enabled = !isEntregado,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = uiState.detailNotes,
                                onValueChange = { viewModel.onDetailNotesChange(it) },
                                label = { Text("Notas") },
                                enabled = !isEntregado,
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (uiState.detailFieldsChanged) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.saveDetailFields() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Guardar Datos del Proceso")
                                }
                            }
                        }
                    }
                }

                // Photos section
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SectionTitle("Fotos (${uiState.detailPhotoPaths.size})")
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(uiState.detailPhotoPaths) { index, path ->
                                    Box(modifier = Modifier
                                        .size(100.dp)
                                        .clickable {
                                            viewingPhotoPath = path
                                            viewingPhotoIndex = index
                                        }
                                    ) {
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
                                    }
                                }
                                item {
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            IconButton(
                                                onClick = { launchDetailCamera() },
                                                enabled = !isEntregado
                                            ) {
                                                Icon(
                                                    Icons.Default.AddAPhoto,
                                                    contentDescription = "Tomar foto",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { detailGalleryLauncher.launch("image/*") },
                                                enabled = !isEntregado
                                            ) {
                                                Icon(
                                                    Icons.Default.Image,
                                                    contentDescription = "Elegir de galería",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Files section
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionTitle("Archivos Adjuntos (${uiState.detailFilePaths.size})", modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = { detailFileLauncher.launch(arrayOf("*/*")) },
                                    enabled = !isEntregado
                                ) {
                                    Icon(Icons.Default.AttachFile, contentDescription = "Adjuntar archivo")
                                }
                            }
                            if (uiState.detailFilePaths.isEmpty()) {
                                Text(
                                    text = "No hay archivos adjuntos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            uiState.detailFilePaths.forEachIndexed { index, path ->
                                val fileName = path.substringAfterLast('/')
                                val extension = PhotoUtils.getFileExtension(path).uppercase()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = fileName,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1
                                        )
                                        if (extension.isNotBlank()) {
                                            Text(
                                                text = extension,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            val file = File(path)
                                            if (file.exists()) {
                                                val uri = PhotoUtils.getUriForFile(detailContext, file)
                                                val mime = detailContext.contentResolver.getType(uri) ?: "*/*"
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(uri, mime)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                detailContext.startActivity(intent)
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.OpenInNew,
                                            contentDescription = "Abrir archivo",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (!isEntregado) IconButton(
                                        onClick = { viewModel.removeDetailFile(index) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Eliminar archivo",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Mecánicos
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionTitle("Mec\u00e1nicos", modifier = Modifier.weight(1f))
                                if (!isEntregado && isAdmin) {
                                    IconButton(onClick = { showMechanicDialog = true }) {
                                        Icon(Icons.Default.Add, contentDescription = "Agregar mec\u00e1nico")
                                    }
                                }
                            }

                            if (uiState.orderMechanics.isEmpty()) {
                                Text(
                                    "Sin mec\u00e1nicos asignados",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                uiState.orderMechanics.forEach { wm ->
                                    val mechName = uiState.mechanics.find { it.id == wm.mechanicId }?.name ?: "Mec\u00e1nico #${wm.mechanicId}"
                                    val typeLabel = when (wm.commissionType) {
                                        "FIJA" -> "Fija"
                                        "PORCENTAJE" -> "${wm.commissionValue}%"
                                        else -> "Sin comisi\u00f3n"
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(mechName, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                "$typeLabel \u2022 $${String.format("%.2f", wm.commissionAmount)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (isAdmin) {
                                            Checkbox(
                                                checked = wm.commissionPaid,
                                                onCheckedChange = { viewModel.toggleCommissionPaid(wm) }
                                            )
                                            Text(
                                                if (wm.commissionPaid) "Pagada" else "Pendiente",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        if (!isEntregado && isAdmin) {
                                            IconButton(onClick = { viewModel.removeMechanicFromOrder(wm) }) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Eliminar",
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.error
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
                                IconButton(
                                    onClick = {
                                        viewModel.cancelEditServiceLine()
                                        showServiceLineDialog = true
                                    },
                                    enabled = !isEntregado
                                ) {
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
                            containerColor = if (uiState.editingServiceLineId == serviceLine.id)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                            IconButton(
                                onClick = {
                                    viewModel.startEditServiceLine(serviceLine)
                                    showServiceLineDialog = true
                                },
                                enabled = !isEntregado
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Editar",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { showDeleteServiceLineDialog = serviceLine.id },
                                enabled = !isEntregado
                            ) {
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
                                IconButton(
                                    onClick = { showPartDialog = true },
                                    enabled = !isEntregado
                                ) {
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
                    val part = uiState.availableParts.find { it.id == orderPart.partId }
                    val partName = part?.let { p ->
                        if (!p.code.isNullOrBlank()) "${p.code} - ${p.name}" else p.name
                    } ?: "Repuesto #${orderPart.partId}"
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
                            IconButton(
                                onClick = {
                                    viewModel.startEditPart(orderPart)
                                    showPartDialog = true
                                },
                                enabled = !isEntregado
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Editar",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(
                                onClick = { showDeletePartDialog = orderPart.id },
                                enabled = !isEntregado
                            ) {
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
                                IconButton(
                                    onClick = { showPaymentDialog = true }
                                ) {
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
                            if (payment.discount > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Descuento",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = String.format("-$%.2f", payment.discount),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
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
                                        text = "${old.displayName}  >>  ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MechanicAssignDialog(
    mechanics: List<com.example.serviaux.data.entity.User>,
    assignedMechanicIds: List<Long>,
    onMechanicAdded: (mechanicId: Long, commissionType: String, commissionValue: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMechanic by remember { mutableStateOf<com.example.serviaux.data.entity.User?>(null) }
    var commissionType by remember { mutableStateOf("") }
    var commissionValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar Mec\u00e1nico") },
        text = {
            Column {
                if (selectedMechanic == null) {
                    val available = mechanics.filter { it.id !in assignedMechanicIds }
                    if (available.isEmpty()) {
                        Text("No hay mec\u00e1nicos disponibles")
                    } else {
                        available.forEach { mechanic ->
                            val commLabel = try {
                                com.example.serviaux.data.entity.CommissionType.valueOf(mechanic.commissionType).displayName
                            } catch (_: Exception) { "" }
                            TextButton(
                                onClick = {
                                    selectedMechanic = mechanic
                                    commissionType = mechanic.commissionType
                                    commissionValue = if (mechanic.commissionValue > 0) mechanic.commissionValue.toString() else ""
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(mechanic.name)
                                    if (commLabel.isNotBlank() && mechanic.commissionType != "NINGUNA") {
                                        Text(
                                            commLabel + if (mechanic.commissionValue > 0) " - ${mechanic.commissionValue}" else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text("Mec\u00e1nico: ${selectedMechanic!!.name}", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(12.dp))

                    var typeExpanded by remember { mutableStateOf(false) }
                    val typeLabel = when (commissionType) {
                        "FIJA" -> "Por trabajo ($)"
                        "PORCENTAJE" -> "Porcentaje (%)"
                        else -> "No comisiona"
                    }
                    ExposedDropdownMenuBox(
                        expanded = typeExpanded,
                        onExpandedChange = { typeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = typeLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo de Comisi\u00f3n") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = typeExpanded,
                            onDismissRequest = { typeExpanded = false }
                        ) {
                            listOf("NINGUNA" to "No comisiona", "FIJA" to "Por trabajo ($)", "PORCENTAJE" to "Porcentaje (%)").forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        commissionType = value
                                        typeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (commissionType != "NINGUNA") {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = commissionValue,
                            onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() || c == '.' }) commissionValue = it },
                            label = { Text(if (commissionType == "FIJA") "Valor ($)" else "Porcentaje (%)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (selectedMechanic != null) {
                TextButton(
                    onClick = {
                        val mechId = selectedMechanic!!.id
                        val cv = commissionValue.toDoubleOrNull() ?: 0.0
                        onMechanicAdded(mechId, commissionType, cv)
                    },
                    enabled = commissionType == "NINGUNA" || (commissionValue.toDoubleOrNull() ?: 0.0) > 0
                ) {
                    Text("Agregar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (selectedMechanic != null) {
                    selectedMechanic = null
                } else {
                    onDismiss()
                }
            }) {
                Text(if (selectedMechanic != null) "Atr\u00e1s" else "Cancelar")
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
    isEditing: Boolean = false,
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
        title = { Text(if (isEditing) "Editar Servicio" else "Agregar Servicio") },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = suggestionsExpanded && filteredServices.isNotEmpty(),
                    onExpandedChange = { }
                ) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = {
                            onDescriptionChange(it.uppercase())
                            descriptionError = null
                            suggestionsExpanded = it.isNotBlank()
                        },
                        label = { Text("Descripci\u00f3n *") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
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
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(service.name, modifier = Modifier.weight(1f))
                                            if (service.vehicleType != null) {
                                                Text(
                                                    text = service.vehicleType,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                            }
                                        }
                                    },
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
                val parsedCost = laborCost.toDoubleOrNull() ?: (if (laborCost.isBlank()) 0.0 else null)
                if (parsedCost == null || parsedCost < 0) {
                    laborCostError = "Costo inv\u00e1lido"
                    hasError = true
                } else if (laborCost.isBlank()) {
                    onLaborCostChange("0")
                }
                if (!hasError) {
                    onSave()
                }
            }) {
                Text(if (isEditing) "Guardar" else "Agregar")
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
    price: String,
    isEditing: Boolean = false,
    onPartSelected: (Long?) -> Unit,
    onQuantityChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val selectedPart = availableParts.find { it.id == selectedPartId }
    var searchQuery by remember { mutableStateOf(
        if (isEditing && selectedPart != null) "${selectedPart.code ?: ""} - ${selectedPart.name}" else ""
    ) }
    var suggestionsExpanded by remember { mutableStateOf(false) }
    var partError by remember { mutableStateOf<String?>(null) }
    var quantityError by remember { mutableStateOf<String?>(null) }

    val filteredParts = remember(searchQuery, availableParts) {
        if (searchQuery.length < 3) {
            availableParts.take(0)
        } else {
            val query = searchQuery.trim()
            availableParts.filter { it.active }
                .filter { part ->
                    part.code?.contains(query, ignoreCase = true) == true ||
                    part.name.contains(query, ignoreCase = true)
                }
                .sortedWith(compareBy<com.example.serviaux.data.entity.Part> { part ->
                    when {
                        part.code.equals(query, ignoreCase = true) -> 0
                        part.code?.startsWith(query, ignoreCase = true) == true -> 1
                        part.name.startsWith(query, ignoreCase = true) -> 2
                        else -> 3
                    }
                })
                .take(10)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Editar Repuesto" else "Agregar Repuesto") },
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
                            suggestionsExpanded = it.length >= 3
                            if (it.isBlank()) onPartSelected(null)
                        },
                        label = { Text("Buscar (min. 3 caracteres) *") },
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
                        text = "Stock disponible: ${selectedPart.currentStock}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = price,
                        onValueChange = { onPriceChange(it) },
                        label = { Text("Precio Unit. *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = { Text("$") },
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused && price.isNotEmpty()) {
                                    onPriceChange("")
                                }
                            }
                    )
                }
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
                Text(if (isEditing) "Guardar" else "Agregar")
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
    discount: String,
    method: PaymentMethod,
    notes: String,
    remainingBalance: Double,
    onAmountChange: (String) -> Unit,
    onDiscountChange: (String) -> Unit,
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
                OutlinedTextField(
                    value = discount,
                    onValueChange = {
                        onDiscountChange(it)
                        amountError = null
                    },
                    label = { Text("Descuento") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text("Opcional - monto de descuento aplicado") },
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
                val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                val parsedDiscount = discount.toDoubleOrNull() ?: 0.0
                val totalPayment = parsedAmount + parsedDiscount
                if (parsedAmount < 0 || parsedDiscount < 0) {
                    amountError = "Los valores no pueden ser negativos"
                } else if (totalPayment <= 0) {
                    amountError = "Ingrese un monto o descuento mayor a 0"
                } else if (totalPayment > remainingBalance + 0.01) {
                    amountError = "El total (monto + descuento) excede el balance pendiente ($${String.format("%.2f", remainingBalance)})"
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
