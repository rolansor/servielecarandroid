/**
 * WorkOrderFormScreen.kt - Formulario de creación/edición de órdenes de trabajo.
 *
 * Campos principales: cliente (searchable), vehículo (filtrado por cliente),
 * prioridad, queja del cliente, diagnóstico inicial, mecánico asignado,
 * kilometraje de entrada, nivel de combustible, checklist de accesorios,
 * notas y fotos.
 *
 * En modo edición (con orderId) carga los datos existentes.
 * Al crear una orden, navega automáticamente al detalle de la nueva orden.
 */
package com.example.serviaux.ui.workorders

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.example.serviaux.data.entity.ArrivalCondition
import com.example.serviaux.data.entity.OrderType
import com.example.serviaux.data.entity.Priority
import com.example.serviaux.ui.components.SearchableDropdown
import com.example.serviaux.ui.components.SearchableItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkOrderFormScreen(
    orderId: Long? = null,
    preselectedCustomerId: Long? = null,
    preselectedVehicleId: Long? = null,
    appointmentId: Long? = null,
    onNavigateBack: () -> Unit,
    onOrderCreated: (Long) -> Unit,
    viewModel: WorkOrderViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val isEditing = orderId != null

    LaunchedEffect(Unit) {
        viewModel.loadCustomers()
        if (orderId != null) {
            viewModel.prepareEdit(orderId)
        } else if (preselectedCustomerId != null && preselectedVehicleId != null) {
            viewModel.prefillFromAppointment(preselectedCustomerId, preselectedVehicleId, appointmentId ?: 0)
        }
    }

    LaunchedEffect(uiState.catalogAccessories) {
        if (!isEditing) {
            viewModel.initChecklist()
        }
    }

    LaunchedEffect(uiState.createdOrderId) {
        uiState.createdOrderId?.let { id ->
            viewModel.clearCreatedOrderId()
            onOrderCreated(id)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Camera & Gallery
    val context = LocalContext.current
    var showPhotoDialog by remember { mutableStateOf(false) }
    var dialogPhotoPath by remember { mutableStateOf("") }
    var dialogPhotoIndex by remember { mutableStateOf(0) }
    var replacingPhoto by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (replacingPhoto) {
            viewModel.onPhotoTakenForReplace(success, dialogPhotoIndex)
            replacingPhoto = false
        } else {
            viewModel.onPhotoTaken(success)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (replacingPhoto && uris.isNotEmpty()) {
            viewModel.replacePhotoFromGallery(uris.first(), dialogPhotoIndex)
            replacingPhoto = false
        } else {
            uris.forEach { uri -> viewModel.addPhotoFromGallery(uri) }
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> uris.forEach { uri -> viewModel.addFormFile(uri) } }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.prepareCameraFile()?.let { uri -> cameraLauncher.launch(uri) }
        }
    }

    fun launchCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            viewModel.prepareCameraFile()?.let { uri -> cameraLauncher.launch(uri) }
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var vehicleDropdownExpanded by remember { mutableStateOf(false) }
    var fuelDropdownExpanded by remember { mutableStateOf(false) }
    var checklistExpanded by remember { mutableStateOf(false) }
    var showAdmissionDatePicker by remember { mutableStateOf(false) }
    val admissionDateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es")) }
    val admissionDateText = uiState.formAdmissionDate?.let { admissionDateFormat.format(Date(it)) } ?: ""

    val selectedVehicleName = uiState.customerVehicles.find { it.id == uiState.formVehicleId }?.let { "${it.plate} - ${it.brand} ${it.model}" } ?: ""

    val fuelLevels = listOf("Vac\u00edo", "1/4", "1/2", "3/4", "Lleno")

    // Customer search items
    val customerItems = remember(uiState.customers) {
        uiState.customers.sortedByDescending { it.createdAt }.map {
            SearchableItem(it.id, it.fullName, it.idNumber)
        }
    }

    // Complaint suggestions
    val complaintItems = remember(uiState.catalogComplaints) {
        uiState.catalogComplaints.map { SearchableItem(it.id, it.name) }
    }

    var orderTypeDropdownExpanded by remember { mutableStateOf(false) }
    var arrivalDropdownExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Editar Orden" else "Nueva Orden de Trabajo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (isEditing) {
                // Read-only customer/vehicle display
                OutlinedTextField(
                    value = uiState.customerName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Cliente") },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.vehicleName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Veh\u00edculo") },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Customer autocomplete search
                SearchableDropdown(
                    value = uiState.formCustomerSearch,
                    onValueChange = { viewModel.onFormCustomerSearchChange(it) },
                    items = customerItems,
                    onItemSelected = { viewModel.onFormCustomerIdChange(it.id) },
                    label = "Cliente *",
                    isError = uiState.formCustomerError != null,
                    supportingText = uiState.formCustomerError?.let { error -> { Text(error, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Vehicle selector (filtered by customer)
                ExposedDropdownMenuBox(
                    expanded = vehicleDropdownExpanded,
                    onExpandedChange = { vehicleDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedVehicleName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Veh\u00edculo *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehicleDropdownExpanded) },
                        enabled = uiState.formCustomerId != null,
                        isError = uiState.formVehicleError != null,
                        supportingText = uiState.formVehicleError?.let { error -> { Text(error, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = vehicleDropdownExpanded,
                        onDismissRequest = { vehicleDropdownExpanded = false }
                    ) {
                        uiState.customerVehicles.forEach { vehicle ->
                            DropdownMenuItem(
                                text = { Text("${vehicle.plate} - ${vehicle.brand} ${vehicle.model}") },
                                onClick = {
                                    viewModel.onFormVehicleIdChange(vehicle.id)
                                    vehicleDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Complaint with autocomplete suggestions
            SearchableDropdown(
                value = uiState.formComplaint,
                onValueChange = { viewModel.onFormComplaintChange(it) },
                items = complaintItems,
                onItemSelected = { item ->
                    viewModel.onFormComplaintChange(item.name)
                },
                label = "Motivo de la visita / Queja *",
                isError = uiState.formComplaintError != null,
                supportingText = {
                    if (uiState.formComplaintError != null) {
                        Text(uiState.formComplaintError!!, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("${uiState.formComplaint.length}/500")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Order type dropdown
            ExposedDropdownMenuBox(
                expanded = orderTypeDropdownExpanded,
                onExpandedChange = { orderTypeDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.formOrderType.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de Orden") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = orderTypeDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = orderTypeDropdownExpanded,
                    onDismissRequest = { orderTypeDropdownExpanded = false }
                ) {
                    OrderType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayName) },
                            onClick = {
                                viewModel.onFormOrderTypeChange(type)
                                orderTypeDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Priority radio group
            Text(
                text = "Prioridad",
                style = MaterialTheme.typography.labelLarge
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Priority.entries.forEach { priority ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = uiState.formPriority.name == priority.name,
                            onClick = { viewModel.onFormPriorityChange(priority) }
                        )
                        Text(text = priority.displayName)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Arrival condition dropdown
            Text(
                text = "Condición de llegada",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            ExposedDropdownMenuBox(
                expanded = arrivalDropdownExpanded,
                onExpandedChange = { arrivalDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.formArrivalCondition.displayName,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = arrivalDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = arrivalDropdownExpanded,
                    onDismissRequest = { arrivalDropdownExpanded = false }
                ) {
                    ArrivalCondition.entries.forEach { condition ->
                        DropdownMenuItem(
                            text = { Text(condition.displayName) },
                            onClick = {
                                viewModel.onFormArrivalConditionChange(condition)
                                arrivalDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Admission date picker
            OutlinedTextField(
                value = admissionDateText,
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha de Ingreso") },
                trailingIcon = {
                    IconButton(onClick = { showAdmissionDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Seleccionar fecha de ingreso")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Photos section for vehicle reception condition
            Text(
                text = "Fotos de recepci\u00f3n (${uiState.formPhotoPaths.size}/6)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(uiState.formPhotoPaths) { index, path ->
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(File(path)).build(),
                        contentDescription = "Foto ${index + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable {
                                dialogPhotoPath = path
                                dialogPhotoIndex = index
                                showPhotoDialog = true
                            }
                    )
                }
                if (uiState.formPhotoPaths.size < 6) {
                    item {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { launchCamera() }) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = "Tomar foto", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                }
                                IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                                    Icon(Icons.Default.Image, contentDescription = "Elegir de galer\u00eda", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Checklist section (collapsible)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { checklistExpanded = !checklistExpanded }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Checklist del Veh\u00edculo",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                val checkedCount = uiState.formChecklist.count { it.value }
                if (checkedCount > 0) {
                    Text(
                        text = "$checkedCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Icon(
                    imageVector = if (checklistExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (checklistExpanded) "Contraer" else "Expandir"
                )
            }
            AnimatedVisibility(visible = checklistExpanded) {
                Column {
                    uiState.formChecklist.forEach { (name, checked) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onChecklistItemToggle(name, !checked) }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { viewModel.onChecklistItemToggle(name, it) }
                            )
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.saveOrder() },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (isEditing) "Guardar Cambios" else "Crear Orden")
                }
            }
        }
    }

    // Photo action dialog
    if (showPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoDialog = false },
            title = { Text("Foto") },
            text = {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(File(dialogPhotoPath)).build(),
                    contentDescription = "Vista previa",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        showPhotoDialog = false
                        replacingPhoto = true
                        launchCamera()
                    }) {
                        Text("Reemplazar")
                    }
                    TextButton(onClick = {
                        showPhotoDialog = false
                        replacingPhoto = true
                        galleryLauncher.launch("image/*")
                    }) {
                        Text("Cambiar")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPhotoDialog = false
                    viewModel.removeFormPhoto(dialogPhotoIndex)
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // Admission date picker dialog
    if (showAdmissionDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.formAdmissionDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showAdmissionDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onFormAdmissionDateChange(it) }
                    showAdmissionDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showAdmissionDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
