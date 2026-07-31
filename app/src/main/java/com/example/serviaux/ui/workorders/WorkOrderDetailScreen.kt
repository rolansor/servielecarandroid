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
 * - Galería de fotos y archivos adjuntos.
 * - Cambio de estado, asignación de mecánico, generación de PDF.
 * - Notas de entrega, número de factura.
 * - Eliminación de la orden (solo admin).
 */
package com.example.serviaux.ui.workorders

import com.example.serviaux.util.formatMoney

import android.Manifest
import com.example.serviaux.ui.theme.SaldoPendiente
import com.example.serviaux.ui.theme.SaldoSaldado
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.example.serviaux.data.entity.OrderStatus
import com.example.serviaux.data.entity.PaymentMethod
import com.example.serviaux.ui.components.CollapsibleSection
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WorkOrderDetailScreen(
    orderId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit = {},
    viewModel: WorkOrderViewModel = viewModel(factory = WorkOrderViewModel.Factory)
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
    var showExtraDialog by remember { mutableStateOf(false) }
    var showDeleteExtraDialog by remember { mutableStateOf<Long?>(null) }
    var showDeleteOrderDialog by remember { mutableStateOf(false) }
    var deleteConfirmationText by remember { mutableStateOf("") }
    var viewingPhotoPath by remember { mutableStateOf<String?>(null) }
    var viewingPhotoIndex by remember { mutableIntStateOf(-1) }
    /** Texto con el que se prellena el alta rápida de repuesto; null si no está abierta. */
    var newPartPrefillName by remember { mutableStateOf<String?>(null) }

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
    val isLocked = order?.status == OrderStatus.CERRADO
    val isAdmin = uiState.isAdmin

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
            hasDiscount = uiState.serviceLineFormHasDiscount,
            discountAmount = uiState.serviceLineFormDiscount,
            onDescriptionChange = { if (it.length <= 200) viewModel.onServiceLineDescriptionChange(it) },
            onLaborCostChange = { viewModel.onServiceLineLaborCostChange(it) },
            onDiscountToggle = { viewModel.onServiceLineDiscountToggle(it) },
            onDiscountChange = { viewModel.onServiceLineDiscountChange(it) },
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
            hasDiscount = uiState.partFormHasDiscount,
            discountAmount = uiState.partFormDiscount,
            onPartSelected = { viewModel.onPartSelectedChange(it) },
            onQuantityChange = { newVal ->
                val filtered = newVal.filter { it.isDigit() }
                viewModel.onPartQuantityChange(filtered)
            },
            onPriceChange = { viewModel.onPartPriceChange(it) },
            onDiscountToggle = { viewModel.onPartDiscountToggle(it) },
            onDiscountChange = { viewModel.onPartDiscountChange(it) },
            onRequestCreatePart = { prefill -> newPartPrefillName = prefill },
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

    // Alta rápida de repuesto, encima del diálogo de la orden.
    newPartPrefillName?.let { prefill ->
        NewPartDialog(
            initialName = prefill,
            onCreate = { name, code, brand, price, stock ->
                viewModel.createPartFromOrder(name, code, brand, price, stock)
                newPartPrefillName = null
            },
            onDismiss = { newPartPrefillName = null }
        )
    }

    // Payment dialog
    if (showPaymentDialog) {
        val totalPaid = uiState.payments.sumOf { it.amount }
        val totalDiscounts = uiState.payments.sumOf { it.discount }
        // Al corregir un pago, lo que ya aportaba vuelve a estar disponible: si no, editar un
        // pago que dejó la orden saldada sería imposible (el balance pendiente sería 0).
        val editingPayment = uiState.editingPaymentId?.let { id -> uiState.payments.find { it.id == id } }
        val remainingBalance = (order?.total ?: 0.0) - totalPaid - totalDiscounts +
            (editingPayment?.let { it.amount + it.discount } ?: 0.0)
        PaymentDialog(
            amount = uiState.paymentFormAmount,
            discount = uiState.paymentFormDiscount,
            method = uiState.paymentFormMethod,
            notes = uiState.paymentFormNotes,
            remainingBalance = remainingBalance,
            isEditing = editingPayment != null,
            onAmountChange = { viewModel.onPaymentAmountChange(it) },
            onDiscountChange = { viewModel.onPaymentDiscountChange(it) },
            onMethodChange = { viewModel.onPaymentMethodChange(it) },
            onNotesChange = { viewModel.onPaymentNotesChange(it) },
            onSave = {
                if (editingPayment != null) viewModel.updatePayment() else viewModel.addPayment()
                showPaymentDialog = false
            },
            onDismiss = {
                viewModel.cancelEditPayment()
                showPaymentDialog = false
            }
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

    // Extra dialog
    if (showExtraDialog) {
        ExtraDialog(
            description = uiState.extraFormDescription,
            cost = uiState.extraFormCost,
            hasDiscount = uiState.extraFormHasDiscount,
            discountAmount = uiState.extraFormDiscount,
            category = uiState.extraFormCategory,
            isEditing = uiState.editingExtraId != null,
            onDescriptionChange = { viewModel.onExtraFormDescriptionChange(it) },
            onCostChange = { viewModel.onExtraFormCostChange(it) },
            onDiscountToggle = { viewModel.onExtraFormDiscountToggle(it) },
            onDiscountChange = { viewModel.onExtraFormDiscountChange(it) },
            onCategoryChange = { viewModel.onExtraFormCategoryChange(it) },
            onSave = {
                viewModel.saveExtra()
                showExtraDialog = false
            },
            onDismiss = {
                viewModel.cancelEditExtra()
                showExtraDialog = false
            }
        )
    }

    // Delete extra confirmation
    showDeleteExtraDialog?.let { extraId ->
        val extra = uiState.orderExtras.find { it.id == extraId }
        if (extra != null) {
            ConfirmDialog(
                title = "Eliminar Extra",
                message = "¿Desea eliminar \"${extra.description}\"?",
                onConfirm = {
                    viewModel.deleteExtra(extra)
                    showDeleteExtraDialog = null
                },
                onDismiss = { showDeleteExtraDialog = null }
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
                if (!isLocked && viewingPhotoIndex >= 0) {
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
                expandedHeight = 40.dp,
                title = { Text("Orden #$orderId") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveDetailFields()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onNavigateToEdit(orderId) },
                        enabled = !isLocked
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar orden")
                    }
                    // Eliminar una orden es irreversible (borra servicios, repuestos, pagos,
                    // comisiones y fotos): solo administradores.
                    if (isAdmin) {
                        IconButton(
                            onClick = { showDeleteOrderDialog = true },
                            enabled = !isLocked
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar orden",
                                tint = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.error
                            )
                        }
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
                // Estado y prioridad (expandido por defecto)
                item {
                    CollapsibleSection(
                        title = "Estado",
                        summary = "${order.status.displayName} \u00b7 ${order.priority.displayName}",
                        initiallyExpanded = true
                    ) {
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
                        if (isAdmin) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Cambiar Estado",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val hasMechanics = uiState.orderMechanics.isNotEmpty()
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                OrderStatus.entries.forEach { status ->
                                    val needsMechanic = status in listOf(OrderStatus.LISTO, OrderStatus.ENTREGADO)
                                    val isEnabled = status != order.status && !(needsMechanic && !hasMechanics)
                                    FilterChip(
                                        selected = status == order.status,
                                        onClick = { if (isEnabled) viewModel.changeStatus(status) },
                                        enabled = isEnabled,
                                        label = { Text(status.displayName, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                            if (!hasMechanics) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Asigne un mec\u00e1nico para marcar como Listo o Entregado",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Mec\u00e1nicos: justo despu\u00e9s del estado, porque el cambio a
                // LISTO/ENTREGADO depende de tener mec\u00e1nico asignado.
                item {
                    val mechanicsSummary = if (uiState.orderMechanics.isEmpty()) {
                        "Sin asignar"
                    } else {
                        uiState.orderMechanics.joinToString(", ") { wm ->
                            uiState.mechanics.find { it.id == wm.mechanicId }?.name ?: "Mec\u00e1nico #${wm.mechanicId}"
                        }
                    }
                    CollapsibleSection(
                        title = "Mec\u00e1nicos",
                        summary = mechanicsSummary,
                        initiallyExpanded = true,
                        headerAction = if (!isLocked && isAdmin) {
                            {
                                IconButton(onClick = { showMechanicDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = "Agregar mec\u00e1nico")
                                }
                            }
                        } else null
                    ) {
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
                                            "$typeLabel \u2022 ${formatMoney(wm.commissionAmount)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    val (badgeText, badgeContainerColor, badgeContentColor) = when {
                                        wm.commissionType == "NINGUNA" -> Triple(
                                            "Sin comisi\u00f3n",
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        wm.commissionPaid -> Triple(
                                            "Pagada",
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        else -> Triple(
                                            "Pendiente",
                                            MaterialTheme.colorScheme.errorContainer,
                                            MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    Surface(
                                        color = badgeContainerColor,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = badgeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = badgeContentColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    if (!isLocked && isAdmin) {
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

                // General Information
                item {
                    CollapsibleSection(
                        title = "Informaci\u00f3n General",
                        summary = uiState.vehicleName.ifBlank { uiState.customerName }
                    ) {
                        Column {
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
                    CollapsibleSection(
                        title = "Datos del Proceso",
                        summary = uiState.detailInvoiceNumber.takeIf { it.isNotBlank() }?.let { "Fact. $it" }
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))

                            // Cada campo se persiste al perder el foco v\u00eda saveDetailFields().
                            var mileageWasFocused by remember { mutableStateOf(false) }
                            OutlinedTextField(
                                value = uiState.detailEntryMileage,
                                onValueChange = { viewModel.onDetailEntryMileageChange(it) },
                                label = { Text("Kilometraje de Entrada") },
                                singleLine = true,
                                enabled = !isLocked,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (mileageWasFocused && !focusState.isFocused) {
                                            viewModel.saveDetailFields()
                                        }
                                        mileageWasFocused = focusState.isFocused
                                    }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            var fuelExpanded by remember { mutableStateOf(false) }
                            val fuelLevels = listOf("Vac\u00edo", "1/4", "1/2", "3/4", "Lleno")
                            ExposedDropdownMenuBox(
                                expanded = fuelExpanded,
                                onExpandedChange = { if (!isLocked) fuelExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = uiState.detailFuelLevel,
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = !isLocked,
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
                            var deliveryWasFocused by remember { mutableStateOf(false) }
                            OutlinedTextField(
                                value = uiState.detailDeliveryNote,
                                onValueChange = { viewModel.onDetailDeliveryNoteChange(it.uppercase()) },
                                label = { Text("Nota de Entrega") },
                                singleLine = true,
                                enabled = !isLocked,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (deliveryWasFocused && !focusState.isFocused) {
                                            viewModel.saveDetailFields()
                                        }
                                        deliveryWasFocused = focusState.isFocused
                                    }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            var invoiceWasFocused by remember { mutableStateOf(false) }
                            OutlinedTextField(
                                value = uiState.detailInvoiceNumber,
                                onValueChange = { viewModel.onDetailInvoiceNumberChange(it.uppercase()) },
                                label = { Text("Factura") },
                                singleLine = true,
                                enabled = !isLocked,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (invoiceWasFocused && !focusState.isFocused) {
                                            viewModel.saveDetailFields()
                                        }
                                        invoiceWasFocused = focusState.isFocused
                                    }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            var notesWasFocused by remember { mutableStateOf(false) }
                            OutlinedTextField(
                                value = uiState.detailNotes,
                                onValueChange = { viewModel.onDetailNotesChange(it.uppercase()) },
                                label = { Text("Notas") },
                                enabled = !isLocked,
                                minLines = 2,
                                maxLines = 4,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (notesWasFocused && !focusState.isFocused) {
                                            viewModel.saveDetailFields()
                                        }
                                        notesWasFocused = focusState.isFocused
                                    }
                            )
                        }
                    }
                }

                // Photos section
                item {
                    CollapsibleSection(
                        title = "Fotos",
                        summary = "${uiState.detailPhotoPaths.size}"
                    ) {
                        Column {
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
                                                enabled = !isLocked
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
                                                enabled = !isLocked
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
                    CollapsibleSection(
                        title = "Archivos Adjuntos",
                        summary = "${uiState.detailFilePaths.size}",
                        headerAction = {
                            IconButton(
                                onClick = { detailFileLauncher.launch(arrayOf("*/*")) },
                                enabled = !isLocked
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = "Adjuntar archivo")
                            }
                        }
                    ) {
                        Column {
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
                                    if (!isLocked) IconButton(
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

                // Service Lines
                item {
                    CollapsibleSection(
                        title = "Servicios / Mano de Obra",
                        summary = "${uiState.serviceLines.size} · ${formatMoney(order.totalLabor)}",
                        headerAction = {
                            IconButton(
                                onClick = {
                                    viewModel.cancelEditServiceLine()
                                    showServiceLineDialog = true
                                },
                                enabled = !isLocked
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Agregar servicio")
                            }
                        }
                    ) {
                        if (uiState.serviceLines.isEmpty()) {
                            Text(
                                text = "No hay servicios registrados",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.serviceLines.forEach { serviceLine ->
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
                                            if (serviceLine.discount > 0) {
                                                Text(
                                                    text = formatMoney(serviceLine.laborCost),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    textDecoration = TextDecoration.LineThrough,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "Desc: -${formatMoney(serviceLine.discount)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                                Text(
                                                    text = formatMoney(serviceLine.laborCost - serviceLine.discount),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            } else {
                                                Text(
                                                    text = formatMoney(serviceLine.laborCost),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.startEditServiceLine(serviceLine)
                                                showServiceLineDialog = true
                                            },
                                            enabled = !isLocked
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Editar",
                                                tint = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = { showDeleteServiceLineDialog = serviceLine.id },
                                            enabled = !isLocked
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Eliminar",
                                                tint = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Work Order Parts
                item {
                    CollapsibleSection(
                        title = "Repuestos Utilizados",
                        summary = "${uiState.orderParts.size} · ${formatMoney(order.totalParts)}",
                        headerAction = {
                            IconButton(
                                onClick = { showPartDialog = true },
                                enabled = !isLocked
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Agregar repuesto")
                            }
                        }
                    ) {
                        if (uiState.orderParts.isEmpty()) {
                            Text(
                                text = "No hay repuestos registrados",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.orderParts.forEach { orderPart ->
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
                                            if (orderPart.discount > 0) {
                                                Text(
                                                    text = "Cant: ${orderPart.quantity} x ${formatMoney(orderPart.appliedUnitPrice)} = ${formatMoney(orderPart.subtotal)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    textDecoration = TextDecoration.LineThrough,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "Desc: -${formatMoney(orderPart.discount)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                                Text(
                                                    text = formatMoney(orderPart.subtotal - orderPart.discount),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            } else {
                                                Text(
                                                    text = "Cant: ${orderPart.quantity} x ${formatMoney(orderPart.appliedUnitPrice)} = ${formatMoney(orderPart.subtotal)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.startEditPart(orderPart)
                                                showPartDialog = true
                                            },
                                            enabled = !isLocked
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Editar",
                                                tint = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = { showDeletePartDialog = orderPart.id },
                                            enabled = !isLocked
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Eliminar",
                                                tint = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Extras
                item {
                    CollapsibleSection(
                        title = "Extras",
                        summary = "${uiState.orderExtras.size} · ${formatMoney(order.totalExtras)}",
                        headerAction = {
                            IconButton(
                                onClick = {
                                    viewModel.cancelEditExtra()
                                    showExtraDialog = true
                                },
                                enabled = !isLocked
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Agregar extra")
                            }
                        }
                    ) {
                        if (uiState.orderExtras.isEmpty()) {
                            Text(
                                text = "No hay extras registrados",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.orderExtras.forEach { extra ->
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
                                                text = extra.description,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            extra.category?.let { cat ->
                                                Text(
                                                    text = cat,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (extra.discount > 0) {
                                                Text(
                                                    text = formatMoney(extra.cost),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    textDecoration = TextDecoration.LineThrough,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "Desc: -${formatMoney(extra.discount)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                                Text(
                                                    text = formatMoney(extra.cost - extra.discount),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            } else {
                                                Text(
                                                    text = formatMoney(extra.cost),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.startEditExtra(extra)
                                                showExtraDialog = true
                                            },
                                            enabled = !isLocked
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Editar",
                                                tint = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = { showDeleteExtraDialog = extra.id },
                                            enabled = !isLocked
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Eliminar",
                                                tint = if (isLocked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
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
                            InfoRow(label = "Mano de Obra", value = formatMoney(order.totalLabor))
                            InfoRow(label = "Repuestos", value = formatMoney(order.totalParts))
                            if (order.totalExtras > 0) {
                                InfoRow(label = "Extras", value = formatMoney(order.totalExtras))
                            }
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
                                    text = formatMoney(order.total),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            val totalPaidSummary = uiState.payments.sumOf { it.amount }
                            val totalDiscountsSummary = uiState.payments.sumOf { it.discount }
                            val balanceSummary = (order.total - totalPaidSummary - totalDiscountsSummary).coerceAtLeast(0.0)
                            // Semántica de saldo del rediseño: verde-agua = saldado,
                            // índigo = abonado o sin pagos. Único número coloreado.
                            val balanceColor = when {
                                order.total <= 0.0 -> MaterialTheme.colorScheme.onSurfaceVariant
                                balanceSummary <= 0.01 -> SaldoSaldado
                                else -> SaldoPendiente
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            InfoRow(label = "Pagado", value = formatMoney(totalPaidSummary))
                            if (totalDiscountsSummary > 0.0) {
                                InfoRow(label = "Descuentos", value = formatMoney(totalDiscountsSummary))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "SALDO",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = formatMoney(balanceSummary),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = balanceColor
                                )
                            }
                        }
                    }
                }

                // Payments
                item {
                    val totalPaidHeader = uiState.payments.sumOf { it.amount }
                    CollapsibleSection(
                        title = "Pagos",
                        summary = "${uiState.payments.size} · ${formatMoney(totalPaidHeader)}",
                        headerAction = {
                            IconButton(
                                onClick = {
                                    // Limpia cualquier edición previa: este botón registra un pago nuevo.
                                    viewModel.cancelEditPayment()
                                    showPaymentDialog = true
                                },
                                enabled = !isLocked
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Agregar pago")
                            }
                        }
                    ) {
                        if (uiState.payments.isEmpty()) {
                            Text(
                                text = "No hay pagos registrados",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.payments.forEach { payment ->
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
                                                text = formatMoney(payment.amount),
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
                                                    text = "-" + formatMoney(payment.discount),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
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
                                            // Corregir un cobro mal registrado sin tener que borrarlo y volver a crearlo.
                                            IconButton(
                                                onClick = {
                                                    viewModel.startEditPayment(payment)
                                                    showPaymentDialog = true
                                                },
                                                enabled = !isLocked,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Editar pago",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // El historial de cambios de estado se sigue registrando en BD
                // (work_order_status_log) pero ya no se presenta en la UI.

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

// --- Dialogs ---

@Composable
private fun StatusChangeDialog(
    currentStatus: OrderStatus,
    hasMechanics: Boolean,
    onStatusSelected: (OrderStatus) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar Estado") },
        text = {
            Column {
                if (!hasMechanics) {
                    Text(
                        text = "Debe asignar al menos un mecánico para marcar como Listo o Entregado.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                OrderStatus.entries.forEach { status ->
                    val needsMechanic = status in listOf(OrderStatus.LISTO, OrderStatus.ENTREGADO)
                    val isEnabled = status != currentStatus && !(needsMechanic && !hasMechanics)
                    TextButton(
                        onClick = { onStatusSelected(status) },
                        enabled = isEnabled,
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
    hasDiscount: Boolean = false,
    discountAmount: String = "",
    onDescriptionChange: (String) -> Unit,
    onLaborCostChange: (String) -> Unit,
    onDiscountToggle: (Boolean) -> Unit = {},
    onDiscountChange: (String) -> Unit = {},
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
        }.take(5)
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
                                        onLaborCostChange(String.format(Locale.US, "%.2f", service.defaultPrice))
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
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Aplicar descuento", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = hasDiscount, onCheckedChange = onDiscountToggle)
                }
                if (hasDiscount) {
                    OutlinedTextField(
                        value = discountAmount,
                        onValueChange = onDiscountChange,
                        label = { Text("Monto descuento") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = { Text("$") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
    hasDiscount: Boolean = false,
    discountAmount: String = "",
    onPartSelected: (Long?) -> Unit,
    onQuantityChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onDiscountToggle: (Boolean) -> Unit = {},
    onDiscountChange: (String) -> Unit = {},
    onRequestCreatePart: (prefillName: String) -> Unit = {},
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val selectedPart = availableParts.find { it.id == selectedPartId }
    var searchQuery by remember { mutableStateOf(
        if (isEditing && selectedPart != null) "${selectedPart.code ?: ""} - ${selectedPart.name}" else ""
    ) }

    // Cuando la selección cambia desde fuera del diálogo —por ejemplo al crear un repuesto
    // nuevo— el campo de búsqueda se sincroniza con la pieza seleccionada.
    LaunchedEffect(selectedPartId) {
        val part = availableParts.find { it.id == selectedPartId }
        if (part != null) searchQuery = "${part.code ?: ""} - ${part.name}"
    }
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
                .take(5)
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
                                                text = "Stock: ${part.currentStock} | ${formatMoney(part.salePrice ?: part.unitCost)}",
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
                // Si la pieza no está en el catálogo, se puede dar de alta sin salir de la orden.
                if (!isEditing && selectedPart == null) {
                    TextButton(
                        onClick = { onRequestCreatePart(searchQuery.trim()) },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (searchQuery.length >= 3)
                                "Crear repuesto \"${searchQuery.trim().take(20)}\""
                            else
                                "Crear repuesto nuevo",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
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
                    // El campo ya no se vacía al recibir el foco: tocarlo para verificar el
                    // importe borraba el precio y, al guardar, la edición se perdía.
                    OutlinedTextField(
                        value = price,
                        onValueChange = { onPriceChange(it) },
                        label = { Text("Precio Unit. *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = { Text("$") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Aplicar descuento", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = hasDiscount, onCheckedChange = onDiscountToggle)
                }
                if (hasDiscount) {
                    OutlinedTextField(
                        value = discountAmount,
                        onValueChange = onDiscountChange,
                        label = { Text("Monto descuento") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = { Text("$") },
                        modifier = Modifier.fillMaxWidth()
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
                // El precio también se valida aquí: sin esto el diálogo se cerraba y el
                // guardado fallaba después, perdiendo lo editado.
                val parsedPrice = price.replace(',', '.').toDoubleOrNull()
                if (parsedPrice == null || parsedPrice < 0.0) {
                    partError = "Ingrese un precio unitario valido"
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

/**
 * Alta rápida de un repuesto desde el diálogo de la orden.
 *
 * Pide lo mínimo para poder facturarlo; el resto de la ficha (descripción, costo real) se
 * completa después en el módulo de Repuestos.
 */
@Composable
private fun NewPartDialog(
    initialName: String,
    onCreate: (name: String, code: String, brand: String, price: String, stock: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName.uppercase()) }
    var code by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("0") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Repuesto") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it.uppercase()
                        nameError = null
                    },
                    label = { Text("Nombre *") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { error -> { Text(error, color = MaterialTheme.colorScheme.error) } },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase() },
                        label = { Text("Código") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it.uppercase() },
                        label = { Text("Marca") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = {
                            price = it
                            priceError = null
                        },
                        label = { Text("Precio venta *") },
                        prefix = { Text("$") },
                        singleLine = true,
                        isError = priceError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = stock,
                        onValueChange = { stock = it.filter { c -> c.isDigit() }.take(5) },
                        label = { Text("Stock inicial") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (priceError != null) {
                    Text(
                        text = priceError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Se guarda en el catálogo de repuestos y queda seleccionado en la orden.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                var hasError = false
                if (name.isBlank()) {
                    nameError = "El nombre es obligatorio"
                    hasError = true
                }
                val parsedPrice = price.replace(',', '.').toDoubleOrNull()
                if (parsedPrice == null || parsedPrice < 0.0) {
                    priceError = "Ingrese un precio válido"
                    hasError = true
                }
                if (!hasError) onCreate(name, code, brand, price, stock)
            }) {
                Text("Crear y seleccionar")
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
private fun ExtraDialog(
    description: String,
    cost: String,
    hasDiscount: Boolean = false,
    discountAmount: String = "",
    category: String?,
    isEditing: Boolean = false,
    onDescriptionChange: (String) -> Unit,
    onCostChange: (String) -> Unit,
    onDiscountToggle: (Boolean) -> Unit = {},
    onDiscountChange: (String) -> Unit = {},
    onCategoryChange: (String?) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var costError by remember { mutableStateOf<String?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val categories = listOf("Ferretería", "Tercerizado", "Repuesto externo", "Herramienta", "Otro")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Editar Extra" else "Agregar Extra") },
        text = {
            Column {
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        onDescriptionChange(it.uppercase())
                        descriptionError = null
                    },
                    label = { Text("Descripción *") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    singleLine = true,
                    isError = descriptionError != null,
                    supportingText = if (descriptionError != null) {
                        { Text(descriptionError!!, color = MaterialTheme.colorScheme.error) }
                    } else {
                        { Text("${description.length}/200") }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = cost,
                    onValueChange = {
                        onCostChange(it)
                        costError = null
                    },
                    label = { Text("Costo *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    prefix = { Text("$") },
                    isError = costError != null,
                    supportingText = costError?.let { error -> { Text(error, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = category ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoría") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sin categoría") },
                            onClick = {
                                onCategoryChange(null)
                                categoryExpanded = false
                            }
                        )
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    onCategoryChange(cat)
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Aplicar descuento", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = hasDiscount, onCheckedChange = onDiscountToggle)
                }
                if (hasDiscount) {
                    OutlinedTextField(
                        value = discountAmount,
                        onValueChange = onDiscountChange,
                        label = { Text("Monto descuento") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = { Text("$") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                var hasError = false
                if (description.isBlank() || description.trim().length < 3) {
                    descriptionError = if (description.isBlank()) "Descripción es obligatoria" else "Mínimo 3 caracteres"
                    hasError = true
                }
                val parsedCost = cost.toDoubleOrNull()
                if (parsedCost == null || parsedCost < 0) {
                    costError = "Costo inválido"
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
    isEditing: Boolean,
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
        title = { Text(if (isEditing) "Editar Pago" else "Registrar Pago") },
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
                        { Text("Balance pendiente: ${formatMoney(remainingBalance)}") }
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
                    onValueChange = { onNotesChange(it.uppercase()) },
                    label = { Text("Notas") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
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
                    amountError = "El total (monto + descuento) excede el balance pendiente (${formatMoney(remainingBalance)})"
                } else {
                    onSave()
                }
            }) {
                Text(if (isEditing) "Guardar" else "Registrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
