package com.example.serviaux.ui.vehicles

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
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
    viewModel: VehicleViewModel = viewModel()
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
            SearchableItem(it.id, it.fullName, it.phone)
        }
    }
    val brandItems = remember(uiState.availableBrands) {
        uiState.availableBrands.mapIndexed { index, name -> SearchableItem(index.toLong(), name) }
    }

    // Model dropdown state
    var modelDropdownExpanded by remember { mutableStateOf(false) }
    val modelsForBrand = uiState.availableModels

    // Color dropdown state
    var colorDropdownExpanded by remember { mutableStateOf(false) }

    // Camera & Gallery
    val context = LocalContext.current
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success -> viewModel.onPhotoTaken(success) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris -> uris.forEach { uri -> viewModel.addPhotoFromGallery(uri) } }

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

    Scaffold(
        topBar = {
            TopAppBar(
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

            // 2. Placa
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

            // 4. Modelo (Model) dropdown filtered by brand
            ExposedDropdownMenuBox(
                expanded = modelDropdownExpanded,
                onExpandedChange = {
                    if (uiState.formBrand.isNotBlank()) modelDropdownExpanded = it
                }
            ) {
                OutlinedTextField(
                    value = uiState.formModel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Modelo *") },
                    singleLine = true,
                    enabled = uiState.formBrand.isNotBlank(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modelDropdownExpanded) },
                    isError = uiState.formModelError != null,
                    supportingText = uiState.formModelError?.let { error -> { Text(error) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = modelDropdownExpanded,
                    onDismissRequest = { modelDropdownExpanded = false }
                ) {
                    modelsForBrand.forEach { model ->
                        DropdownMenuItem(
                            text = { Text(model) },
                            onClick = {
                                viewModel.onFormModelChange(model)
                                modelDropdownExpanded = false
                            }
                        )
                    }
                }
            }

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

            // 6. Ano
            OutlinedTextField(
                value = uiState.formYear,
                onValueChange = { viewModel.onFormYearChange(it) },
                label = { Text("A\u00f1o") },
                singleLine = true,
                isError = uiState.formYearError != null,
                supportingText = uiState.formYearError?.let { error -> { Text(error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { if (!it.isFocused) viewModel.validateFieldOnFocusLost("year") }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 7. Color dropdown
            ExposedDropdownMenuBox(
                expanded = colorDropdownExpanded,
                onExpandedChange = { colorDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.formColor,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Color") },
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = colorDropdownExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = colorDropdownExpanded,
                    onDismissRequest = { colorDropdownExpanded = false }
                ) {
                    uiState.availableColors.forEach { color ->
                        DropdownMenuItem(
                            text = { Text(color) },
                            onClick = {
                                viewModel.onFormColorChange(color)
                                colorDropdownExpanded = false
                            }
                        )
                    }
                }
            }

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

            // Photos section
            Text(
                text = "Fotos (${uiState.formPhotoPaths.size})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uiState.formPhotoPaths) { index, path ->
                    Box(modifier = Modifier.size(100.dp)) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
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
                            onClick = { viewModel.removePhoto(index) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .background(
                                    MaterialTheme.colorScheme.errorContainer,
                                    CircleShape
                                )
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
                                Icon(
                                    Icons.Default.AddAPhoto,
                                    contentDescription = "Tomar foto",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            IconButton(onClick = { galleryLauncher.launch("image/*") }) {
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
}
