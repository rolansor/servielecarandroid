/**
 * VehicleFormScreen.kt - Formulario de creacion/edicion de vehiculos.
 *
 * Campos: placa, marca, modelo, version, ano, color, tipo, VIN, kilometraje,
 * motor, traccion, transmision, notas y fotos (maximo 6).
 * Los campos de marca, modelo, color y tipo se autocomplejan desde catalogos.
 * Soporta captura de fotos con camara y seleccion desde galeria.
 */
package com.example.serviaux.ui.vehicles

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.serviaux.ui.components.SearchableDropdown
import com.example.serviaux.ui.components.SearchableItem
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleFormScreen(
    vehicleId: Long? = null,
    preselectedCustomerId: Long? = null,
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: VehicleViewModel = viewModel(factory = VehicleViewModel.Factory)
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val isEditing = vehicleId != null

    LaunchedEffect(Unit) {
        viewModel.loadCustomers()
        if (vehicleId != null) {
            viewModel.loadVehicle(vehicleId)
        } else {
            viewModel.prepareNew(preselectedCustomerId)
        }
    }

    LaunchedEffect(uiState.selectedVehicle) {
        if (isEditing && uiState.selectedVehicle != null && !uiState.isEditing) {
            viewModel.prepareEdit(uiState.selectedVehicle!!)
        }
    }

    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) {
            onSaved()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val customerItems = remember(uiState.customers) {
        uiState.customers.sortedByDescending { it.createdAt }.map {
            SearchableItem(it.id, it.fullName, it.idNumber)
        }
    }
    val brandItems = remember(uiState.availableBrands) {
        uiState.availableBrands.mapIndexed { index, name -> SearchableItem(index.toLong(), name) }
    }

    val vehicleTypeItems = remember(uiState.availableVehicleTypes) {
        uiState.availableVehicleTypes.mapIndexed { index, name -> SearchableItem(index.toLong(), name) }
    }

    val modelItems = remember(uiState.availableModels) {
        uiState.availableModels.mapIndexed { index, name -> SearchableItem(index.toLong(), name) }
    }

    // Year dropdown state
    var yearExpanded by remember { mutableStateOf(false) }
    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val yearOptions = remember { (currentYear downTo 1975).map { it.toString() } }

    val colorItems = remember(uiState.availableColors) {
        uiState.availableColors.mapIndexed { index, name -> SearchableItem(index.toLong(), name) }
    }

    // Photo action dialog state
    var showPhotoDialog by remember { mutableStateOf(false) }
    var dialogPhotoPath by remember { mutableStateOf("") }
    var dialogPhotoTarget by remember { mutableStateOf("vehicle") }
    var dialogPhotoIndex by remember { mutableStateOf(0) }
    var replacingPhoto by remember { mutableStateOf(false) }

    // Camera & Gallery
    val context = LocalContext.current
    var activePhotoTarget by remember { mutableStateOf("vehicle") }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (replacingPhoto) {
            viewModel.onPhotoTakenForReplace(success, dialogPhotoTarget, dialogPhotoIndex)
            replacingPhoto = false
        } else {
            viewModel.onPhotoTaken(success)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (replacingPhoto && uris.isNotEmpty()) {
            viewModel.replacePhotoFromGallery(uris.first(), dialogPhotoTarget, dialogPhotoIndex)
            replacingPhoto = false
        } else {
            uris.forEach { uri -> viewModel.addPhotoFromGallery(uri, activePhotoTarget) }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.prepareCameraFile(activePhotoTarget)?.let { uri -> cameraLauncher.launch(uri) }
        }
    }

    fun launchCamera(target: String) {
        activePhotoTarget = target
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            viewModel.prepareCameraFile(target)?.let { uri -> cameraLauncher.launch(uri) }
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun launchGallery(target: String) {
        activePhotoTarget = target
        galleryLauncher.launch("image/*")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = 40.dp,
                title = { Text(if (isEditing) "Editar Veh\u00edculo" else "Nuevo Veh\u00edculo") },
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
            // 1. Customer selector (autocomplete)
            SearchableDropdown(
                value = uiState.formCustomerSearch,
                onValueChange = { viewModel.onFormCustomerSearchChange(it) },
                items = customerItems,
                onItemSelected = { viewModel.onFormCustomerSelected(it.id) },
                label = "Cliente *",
                isError = uiState.formCustomerError != null,
                supportingText = uiState.formCustomerError?.let { error -> { Text(error) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Tipo de vehículo (autocomplete)
            SearchableDropdown(
                value = uiState.formVehicleTypeSearch,
                onValueChange = { viewModel.onFormVehicleTypeSearchChange(it) },
                items = vehicleTypeItems,
                onItemSelected = { viewModel.onFormVehicleTypeSelected(it.name) },
                label = "Tipo de Veh\u00edculo",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Placa
            OutlinedTextField(
                value = uiState.formPlate,
                onValueChange = { viewModel.onFormPlateChange(it) },
                label = { Text("Placa *") },
                singleLine = true,
                isError = uiState.formPlateError != null,
                supportingText = uiState.formPlateError?.let { error -> { Text(error) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (!it.isFocused) viewModel.validateFieldOnFocusLost("plate") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Marca (Brand) autocomplete
            SearchableDropdown(
                value = uiState.formBrandSearch,
                onValueChange = { viewModel.onFormBrandSearchChange(it) },
                items = brandItems,
                onItemSelected = { viewModel.onFormBrandSelected(it.name) },
                label = "Marca *",
                isError = uiState.formBrandError != null,
                supportingText = uiState.formBrandError?.let { error -> { Text(error) } },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Modelo (Model) autocomplete filtered by brand
            SearchableDropdown(
                value = uiState.formModelSearch,
                onValueChange = { viewModel.onFormModelSearchChange(it) },
                items = modelItems,
                onItemSelected = { viewModel.onFormModelSelected(it.name) },
                label = "Modelo *",
                isError = uiState.formModelError != null,
                supportingText = uiState.formModelError?.let { error -> { Text(error) } },
                enabled = uiState.formBrand.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Version (free text)
            OutlinedTextField(
                value = uiState.formVersion,
                onValueChange = { viewModel.onFormVersionChange(it) },
                label = { Text("Versi\u00f3n") },
                placeholder = { Text("ej: 1.6 GL, 2.0 GLS Premium, SR Turbo") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 6. Año (picker)
            ExposedDropdownMenuBox(
                expanded = yearExpanded,
                onExpandedChange = { yearExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.formYear,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("A\u00f1o") },
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                    isError = uiState.formYearError != null,
                    supportingText = uiState.formYearError?.let { error -> { Text(error) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = yearExpanded,
                    onDismissRequest = { yearExpanded = false }
                ) {
                    yearOptions.forEach { year ->
                        DropdownMenuItem(
                            text = { Text(year) },
                            onClick = {
                                viewModel.onFormYearChange(year)
                                yearExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 7. Color (autocomplete)
            SearchableDropdown(
                value = uiState.formColorSearch,
                onValueChange = { viewModel.onFormColorSearchChange(it) },
                items = colorItems,
                onItemSelected = { viewModel.onFormColorSelected(it.name) },
                label = "Color",
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 8. Cilindraje
            OutlinedTextField(
                value = uiState.formEngineDisplacement,
                onValueChange = { viewModel.onFormEngineDisplacementChange(it) },
                label = { Text("Cilindraje") },
                placeholder = { Text("ej: 1.5") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 9. Numero de motor
            OutlinedTextField(
                value = uiState.formEngineNumber,
                onValueChange = { viewModel.onFormEngineNumberChange(it) },
                label = { Text("N\u00famero de motor") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 10. VIN
            OutlinedTextField(
                value = uiState.formVin,
                onValueChange = { viewModel.onFormVinChange(it) },
                label = { Text("VIN") },
                singleLine = true,
                isError = uiState.formVinError != null,
                supportingText = uiState.formVinError?.let { error -> { Text(error) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (!it.isFocused) viewModel.validateFieldOnFocusLost("vin") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 11. Traccion
            Text(
                text = "Tracci\u00f3n",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                FilterChip(
                    selected = uiState.formDrivetrain == "4x2",
                    onClick = { viewModel.onFormDrivetrainChange("4x2") },
                    label = { Text("4x2") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = uiState.formDrivetrain == "4x4",
                    onClick = { viewModel.onFormDrivetrainChange("4x4") },
                    label = { Text("4x4") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 13. Transmision
            Text(
                text = "Transmisi\u00f3n",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                FilterChip(
                    selected = uiState.formTransmission == "Manual",
                    onClick = { viewModel.onFormTransmissionChange("Manual") },
                    label = { Text("Manual") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilterChip(
                    selected = uiState.formTransmission == "Autom\u00e1tico",
                    onClick = { viewModel.onFormTransmissionChange("Autom\u00e1tico") },
                    label = { Text("Autom\u00e1tico") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Combustible
            Text(
                text = "Combustible",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FUEL_TYPES.take(2).forEach { fuel ->
                    FilterChip(
                        selected = uiState.formFuelType == fuel,
                        onClick = { viewModel.onFormFuelTypeChange(if (uiState.formFuelType == fuel) "" else fuel) },
                        label = { Text(fuel) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FUEL_TYPES.drop(2).forEach { fuel ->
                    FilterChip(
                        selected = uiState.formFuelType == fuel,
                        onClick = { viewModel.onFormFuelTypeChange(if (uiState.formFuelType == fuel) "" else fuel) },
                        label = { Text(fuel) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tipo de aceite + Capacidad de aceite
            val oilTypeItems = remember(uiState.availableOilTypes) {
                uiState.availableOilTypes.mapIndexed { index, name -> SearchableItem(index.toLong(), name) }
            }
            val oilCapacityOptions = remember {
                buildList {
                    add("")
                    var v = 0.5
                    while (v <= 10.0) {
                        val label = if (v % 1.0 == 0.0) "${v.toInt()} gal\u00f3n${if (v > 1.0) "es" else ""}"
                        else "${v.toInt()} 1/2 gal\u00f3n${if (v > 1.5) "es" else ""}"
                        add(label)
                        v += 0.5
                    }
                }
            }
            var oilCapacityExpanded by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchableDropdown(
                    value = uiState.formOilTypeSearch,
                    onValueChange = { viewModel.onFormOilTypeSearchChange(it) },
                    items = oilTypeItems,
                    onItemSelected = { viewModel.onFormOilTypeSelected(it.name) },
                    label = "Tipo de Aceite",
                    modifier = Modifier.weight(1f)
                )

                // Capacidad de aceite (step selector)
                ExposedDropdownMenuBox(
                    expanded = oilCapacityExpanded,
                    onExpandedChange = { oilCapacityExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = uiState.formOilCapacity,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Capacidad de Aceite") },
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = oilCapacityExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = oilCapacityExpanded,
                        onDismissRequest = { oilCapacityExpanded = false }
                    ) {
                        oilCapacityOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.ifBlank { "Sin especificar" }) },
                                onClick = {
                                    viewModel.onFormOilCapacityChange(option)
                                    oilCapacityExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fotos de Matrícula (max 2)
            Text(
                text = "Fotos de Matr\u00edcula (${uiState.formRegistrationPhotoPaths.size}/2)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(uiState.formRegistrationPhotoPaths) { index, path ->
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(File(path)).build(),
                        contentDescription = "Matr\u00edcula ${index + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable {
                                dialogPhotoPath = path
                                dialogPhotoTarget = "registration"
                                dialogPhotoIndex = index
                                showPhotoDialog = true
                            }
                    )
                }
                if (uiState.formRegistrationPhotoPaths.size < 2) {
                    item {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { launchCamera("registration") }) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = "Tomar foto", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                }
                                IconButton(onClick = { launchGallery("registration") }) {
                                    Icon(Icons.Default.Image, contentDescription = "Elegir de galer\u00eda", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fotos del Vehículo (max 6)
            Text(
                text = "Fotos del Veh\u00edculo (${uiState.formPhotoPaths.size}/6)",
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
                                dialogPhotoTarget = "vehicle"
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
                                IconButton(onClick = { launchCamera("vehicle") }) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = "Tomar foto", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                }
                                IconButton(onClick = { launchGallery("vehicle") }) {
                                    Icon(Icons.Default.Image, contentDescription = "Elegir de galer\u00eda", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 14. Notas
            OutlinedTextField(
                value = uiState.formNotes,
                onValueChange = { viewModel.onFormNotesChange(it) },
                label = { Text("Notas") },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.saveVehicle() },
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
                    Text("Guardar")
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
                        launchCamera(dialogPhotoTarget)
                    }) {
                        Text("Reemplazar")
                    }
                    TextButton(onClick = {
                        showPhotoDialog = false
                        replacingPhoto = true
                        launchGallery(dialogPhotoTarget)
                    }) {
                        Text("Cambiar")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPhotoDialog = false
                    if (dialogPhotoTarget == "registration") {
                        viewModel.removeRegistrationPhoto(dialogPhotoIndex)
                    } else {
                        viewModel.removePhoto(dialogPhotoIndex)
                    }
                }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}
