package com.example.serviaux.ui.workorders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.serviaux.data.entity.Priority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkOrderFormScreen(
    onNavigateBack: () -> Unit,
    onOrderCreated: (Long) -> Unit,
    viewModel: WorkOrderViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.loadCustomers()
    }

    LaunchedEffect(uiState.createdOrderId) {
        uiState.createdOrderId?.let { orderId ->
            viewModel.clearCreatedOrderId()
            onOrderCreated(orderId)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var vehicleDropdownExpanded by remember { mutableStateOf(false) }
    var fuelDropdownExpanded by remember { mutableStateOf(false) }

    val selectedCustomerName = uiState.customers.find { it.id == uiState.formCustomerId }?.fullName ?: ""
    val selectedVehicleName = uiState.customerVehicles.find { it.id == uiState.formVehicleId }?.let { "${it.plate} - ${it.brand} ${it.model}" } ?: ""

    val fuelLevels = listOf("Vac\u00edo", "1/4", "1/2", "3/4", "Lleno")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva Orden de Trabajo") },
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
            // Customer selector
            ExposedDropdownMenuBox(
                expanded = customerDropdownExpanded,
                onExpandedChange = { customerDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedCustomerName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Cliente *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                    isError = uiState.formCustomerError != null,
                    supportingText = uiState.formCustomerError?.let { error -> { Text(error, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = customerDropdownExpanded,
                    onDismissRequest = { customerDropdownExpanded = false }
                ) {
                    uiState.customers.forEach { customer ->
                        DropdownMenuItem(
                            text = { Text(customer.fullName) },
                            onClick = {
                                viewModel.onFormCustomerIdChange(customer.id)
                                customerDropdownExpanded = false
                            }
                        )
                    }
                }
            }

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

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.formComplaint,
                onValueChange = { viewModel.onFormComplaintChange(it) },
                label = { Text("Motivo de la visita / Queja *") },
                minLines = 3,
                isError = uiState.formComplaintError != null,
                supportingText = {
                    if (uiState.formComplaintError != null) {
                        Text(uiState.formComplaintError!!, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("${uiState.formComplaint.length}/500")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (!it.isFocused) viewModel.validateFieldOnFocusLost("complaint") }
            )

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

            OutlinedTextField(
                value = uiState.formDiagnosis,
                onValueChange = { viewModel.onFormDiagnosisChange(it) },
                label = { Text("Diagn\u00f3stico inicial") },
                minLines = 2,
                supportingText = { Text("${uiState.formDiagnosis.length}/500") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.formEntryMileage,
                onValueChange = { viewModel.onFormEntryMileageChange(it) },
                label = { Text("Kilometraje de entrada") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = uiState.formMileageError != null,
                supportingText = uiState.formMileageError?.let { error -> { Text(error, color = MaterialTheme.colorScheme.error) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (!it.isFocused) viewModel.validateFieldOnFocusLost("entryMileage") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Fuel level dropdown
            ExposedDropdownMenuBox(
                expanded = fuelDropdownExpanded,
                onExpandedChange = { fuelDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.formFuelLevel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Nivel de Combustible") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = fuelDropdownExpanded,
                    onDismissRequest = { fuelDropdownExpanded = false }
                ) {
                    fuelLevels.forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level) },
                            onClick = {
                                viewModel.onFormFuelLevelChange(level)
                                fuelDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = uiState.formChecklistNotes,
                onValueChange = { viewModel.onFormChecklistNotesChange(it) },
                label = { Text("Notas de checklist") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.createOrder() },
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
                    Text("Crear Orden")
                }
            }
        }
    }
}
