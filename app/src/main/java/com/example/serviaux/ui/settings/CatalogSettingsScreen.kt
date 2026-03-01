/**
 * CatalogSettingsScreen.kt - Pantalla de configuración de catálogos.
 *
 * Interfaz de administración para los 9 tipos de catálogo del sistema:
 * marcas de vehículos (con modelos), colores, marcas de repuestos,
 * servicios predefinidos, tipos de vehículo, accesorios, quejas y diagnósticos.
 *
 * Cada sección es expandible y permite crear, editar y eliminar elementos.
 * Incluye exportación e importación de catálogos en formato JSON.
 * Solo accesible para administradores.
 */
package com.example.serviaux.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.serviaux.data.entity.CatalogBrand
import com.example.serviaux.data.entity.CatalogComplaint
import com.example.serviaux.data.entity.CatalogDiagnosis
import com.example.serviaux.data.entity.CatalogService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: CatalogViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val tabs = listOf("Marcas", "Colores", "Repuestos", "Servicios", "Tipos Veh.", "Accesorios", "Motivos", "Diagn\u00f3sticos")

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }

    CatalogDialogs(uiState = uiState, viewModel = viewModel)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mantenimiento Cat\u00e1logos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> viewModel.showAddBrandDialog()
                        1 -> viewModel.showAddColorDialog()
                        2 -> viewModel.showAddPartBrandDialog()
                        3 -> viewModel.showAddServiceDialog()
                        4 -> viewModel.showAddVehicleTypeDialog()
                        5 -> viewModel.showAddAccessoryDialog()
                        6 -> viewModel.showAddComplaintDialog()
                        7 -> {
                            val complaintId = uiState.selectedComplaintId
                            if (complaintId != null) {
                                viewModel.showAddDiagnosisDialog(complaintId)
                            }
                        }
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Scrollable tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 8.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> BrandsTab(uiState = uiState, viewModel = viewModel)
                1 -> SimpleListTab(
                    items = uiState.colors.map { SimpleItem(it.id, it.name) },
                    onEdit = { id -> uiState.colors.find { it.id == id }?.let { viewModel.showEditColorDialog(it) } },
                    onDelete = { id, name -> viewModel.showDeleteConfirmation("color", id, name) }
                )
                2 -> SimpleListTab(
                    items = uiState.partBrands.map { SimpleItem(it.id, it.name) },
                    onEdit = { id -> uiState.partBrands.find { it.id == id }?.let { viewModel.showEditPartBrandDialog(it) } },
                    onDelete = { id, name -> viewModel.showDeleteConfirmation("partBrand", id, name) }
                )
                3 -> ServicesTab(uiState = uiState, viewModel = viewModel)
                4 -> SimpleListTab(
                    items = uiState.vehicleTypes.map { SimpleItem(it.id, it.name) },
                    onEdit = { id -> uiState.vehicleTypes.find { it.id == id }?.let { viewModel.showEditVehicleTypeDialog(it) } },
                    onDelete = { id, name -> viewModel.showDeleteConfirmation("vehicleType", id, name) }
                )
                5 -> SimpleListTab(
                    items = uiState.accessories.map { SimpleItem(it.id, it.name) },
                    onEdit = { id -> uiState.accessories.find { it.id == id }?.let { viewModel.showEditAccessoryDialog(it) } },
                    onDelete = { id, name -> viewModel.showDeleteConfirmation("accessory", id, name) }
                )
                6 -> ComplaintsTab(uiState = uiState, viewModel = viewModel)
                7 -> DiagnosesTab(uiState = uiState, viewModel = viewModel)
            }
        }
    }
}

// ─── Simple reusable item for flat list tabs ─────────────────────────

private data class SimpleItem(val id: Long, val name: String)

@Composable
private fun SimpleListTab(
    items: List<SimpleItem>,
    onEdit: (Long) -> Unit,
    onDelete: (Long, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp, horizontal = 0.dp)
    ) {
        items(items, key = { it.id }) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onEdit(item.id) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { onDelete(item.id, item.name) }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Brands Tab (hierarchical with models) ───────────────────────────

@Composable
private fun BrandsTab(uiState: CatalogUiState, viewModel: CatalogViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)
    ) {
        items(uiState.brands, key = { it.id }) { brand ->
            BrandItem(
                brand = brand,
                isExpanded = uiState.selectedBrandId == brand.id,
                models = if (uiState.selectedBrandId == brand.id) uiState.models else emptyList(),
                onToggle = {
                    if (uiState.selectedBrandId == brand.id) viewModel.selectBrand(null)
                    else viewModel.selectBrand(brand)
                },
                onEditBrand = { viewModel.showEditBrandDialog(brand) },
                onDeleteBrand = { viewModel.showDeleteConfirmation("brand", brand.id, brand.name) },
                onAddModel = { viewModel.showAddModelDialog(brand.id) },
                onEditModel = { model -> viewModel.showEditModelDialog(model) },
                onDeleteModel = { model -> viewModel.showDeleteConfirmation("model", model.id, model.name) }
            )
        }
    }
}

@Composable
private fun BrandItem(
    brand: CatalogBrand,
    isExpanded: Boolean,
    models: List<com.example.serviaux.data.entity.CatalogModel>,
    onToggle: () -> Unit,
    onEditBrand: () -> Unit,
    onDeleteBrand: () -> Unit,
    onAddModel: () -> Unit,
    onEditModel: (com.example.serviaux.data.entity.CatalogModel) -> Unit,
    onDeleteModel: (com.example.serviaux.data.entity.CatalogModel) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = brand.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEditBrand, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar marca", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDeleteBrand, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar marca", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(start = 32.dp, end = 16.dp, bottom = 8.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Modelos (${models.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    models.forEach { model ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = model.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onEditModel(model) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar modelo", modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onDeleteModel(model) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar modelo", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(onClick = onAddModel) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Agregar modelo")
                    }
                }
            }
        }
    }
}

// ─── Services Tab (grouped by category) ──────────────────────────────

@Composable
private fun ServicesTab(uiState: CatalogUiState, viewModel: CatalogViewModel) {
    val servicesByCategory = uiState.services.groupBy { it.category }
    val categories = servicesByCategory.keys.sorted()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)
    ) {
        items(categories, key = { it }) { category ->
            val services = servicesByCategory[category] ?: emptyList()
            val isExpanded = uiState.expandedServiceCategory == category

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isExpanded)
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Column {
                    // Category header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleServiceCategory(category) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${services.size} servicios",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Services list
                    AnimatedVisibility(visible = isExpanded) {
                        Column(modifier = Modifier.padding(start = 32.dp, end = 16.dp, bottom = 8.dp)) {
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(4.dp))

                            services.forEach { service ->
                                ServiceItem(
                                    service = service,
                                    onEdit = { viewModel.showEditServiceDialog(service) },
                                    onDelete = { viewModel.showDeleteConfirmation("service", service.id, service.name) }
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(onClick = { viewModel.showAddServiceDialog(category) }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Agregar servicio")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceItem(
    service: CatalogService,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = service.name, style = MaterialTheme.typography.bodyMedium)
                if (service.vehicleType != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = service.vehicleType,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = "$${String.format("%.2f", service.defaultPrice)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(16.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
        }
    }
}

// ─── Complaints Tab ──────────────────────────────────────────────────

@Composable
private fun ComplaintsTab(uiState: CatalogUiState, viewModel: CatalogViewModel) {
    SimpleListTab(
        items = uiState.complaints.map { SimpleItem(it.id, it.name) },
        onEdit = { id -> uiState.complaints.find { it.id == id }?.let { viewModel.showEditComplaintDialog(it) } },
        onDelete = { id, name -> viewModel.showDeleteConfirmation("complaint", id, name) }
    )
}

// ─── Diagnoses Tab ───────────────────────────────────────────────────

@Composable
private fun DiagnosesTab(uiState: CatalogUiState, viewModel: CatalogViewModel) {
    val complaints = uiState.complaints
    val allDiagnoses = uiState.diagnoses

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)
    ) {
        items(complaints, key = { it.id }) { complaint ->
            val isExpanded = uiState.selectedComplaintId == complaint.id
            val diagnoses = allDiagnoses.filter { it.complaintId == complaint.id }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isExpanded)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectComplaint(if (isExpanded) null else complaint)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = complaint.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${diagnoses.size} diagn\u00f3sticos",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column(modifier = Modifier.padding(start = 32.dp, end = 16.dp, bottom = 8.dp)) {
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(4.dp))

                            if (diagnoses.isEmpty()) {
                                Text(
                                    text = "Sin diagn\u00f3sticos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            diagnoses.forEach { diagnosis ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = diagnosis.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { viewModel.showEditDiagnosisDialog(diagnosis) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(onClick = { viewModel.showDeleteConfirmation("diagnosis", diagnosis.id, diagnosis.name) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(onClick = { viewModel.showAddDiagnosisDialog(complaint.id) }) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Agregar diagn\u00f3stico")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Dialogs ─────────────────────────────────────────────────────────

@Composable
private fun CatalogDialogs(uiState: CatalogUiState, viewModel: CatalogViewModel) {
    when (val dialog = uiState.dialogState) {
        is CatalogDialogState.None -> {}

        is CatalogDialogState.AddBrand -> {
            TextInputDialog(
                title = "Agregar Marca",
                label = "Nombre de la marca",
                value = dialog.name,
                onValueChange = { viewModel.updateDialogText(it) },
                onConfirm = { viewModel.confirmAddBrand(dialog.name) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is CatalogDialogState.EditBrand -> {
            TextInputDialog(
                title = "Editar Marca",
                label = "Nombre de la marca",
                value = dialog.name,
                onValueChange = { viewModel.updateDialogText(it) },
                onConfirm = { viewModel.confirmEditBrand(dialog.brand, dialog.name) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is CatalogDialogState.AddModel -> {
            TextInputDialog(
                title = "Agregar Modelo",
                label = "Nombre del modelo",
                value = dialog.name,
                onValueChange = { viewModel.updateDialogText(it) },
                onConfirm = { viewModel.confirmAddModel(dialog.brandId, dialog.name) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is CatalogDialogState.EditModel -> {
            TextInputDialog(
                title = "Editar Modelo",
                label = "Nombre del modelo",
                value = dialog.name,
                onValueChange = { viewModel.updateDialogText(it) },
                onConfirm = { viewModel.confirmEditModel(dialog.model, dialog.name) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is CatalogDialogState.AddColor -> {
            TextInputDialog(
                title = "Agregar Color",
                label = "Nombre del color",
                value = dialog.name,
                onValueChange = { viewModel.updateDialogText(it) },
                onConfirm = { viewModel.confirmAddColor(dialog.name) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is CatalogDialogState.EditColor -> {
            TextInputDialog(
                title = "Editar Color",
                label = "Nombre del color",
                value = dialog.name,
                onValueChange = { viewModel.updateDialogText(it) },
                onConfirm = { viewModel.confirmEditColor(dialog.color, dialog.name) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is CatalogDialogState.AddPartBrand -> {
            TextInputDialog(
                title = "Agregar Marca de Repuesto",
                label = "Nombre de la marca",
                value = dialog.name,
                onValueChange = { viewModel.updateDialogText(it) },
                onConfirm = { viewModel.confirmAddPartBrand(dialog.name) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is CatalogDialogState.EditPartBrand -> {
            TextInputDialog(
                title = "Editar Marca de Repuesto",
                label = "Nombre de la marca",
                value = dialog.name,
                onValueChange = { viewModel.updateDialogText(it) },
                onConfirm = { viewModel.confirmEditPartBrand(dialog.partBrand, dialog.name) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is CatalogDialogState.AddService -> {
            ServiceInputDialog(
                title = "Agregar Servicio",
                category = dialog.category,
                name = dialog.name,
                price = dialog.price,
                vehicleType = dialog.vehicleType,
                existingCategories = uiState.services.map { it.category }.distinct().sorted(),
                onCategoryChange = { viewModel.updateServiceDialogField("category", it) },
                onNameChange = { viewModel.updateServiceDialogField("name", it) },
                onPriceChange = { viewModel.updateServiceDialogField("price", it) },
                onVehicleTypeChange = { viewModel.updateServiceDialogField("vehicleType", it) },
                onConfirm = { viewModel.confirmAddService(dialog.category, dialog.name, dialog.price, dialog.vehicleType) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is CatalogDialogState.EditService -> {
            ServiceInputDialog(
                title = "Editar Servicio",
                category = dialog.category,
                name = dialog.name,
                price = dialog.price,
                vehicleType = dialog.vehicleType,
                existingCategories = uiState.services.map { it.category }.distinct().sorted(),
                onCategoryChange = { viewModel.updateServiceDialogField("category", it) },
                onNameChange = { viewModel.updateServiceDialogField("name", it) },
                onPriceChange = { viewModel.updateServiceDialogField("price", it) },
                onVehicleTypeChange = { viewModel.updateServiceDialogField("vehicleType", it) },
                onConfirm = { viewModel.confirmEditService(dialog.service, dialog.category, dialog.name, dialog.price, dialog.vehicleType) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is CatalogDialogState.AddVehicleType -> {
            TextInputDialog(
                title = "Agregar Tipo de Veh\u00edculo",
                label = "Nombre del tipo",
                value = dialog.name,
                onValueChange = { viewModel.updateDialogText(it) },
                onConfirm = { viewModel.confirmAddVehicleType(dialog.name) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is CatalogDialogState.EditVehicleType -> {
            TextInputDialog(
                title = "Editar Tipo de Veh\u00edculo",
                label = "Nombre del tipo",
                value = dialog.name,
                onValueChange = { viewModel.updateDialogText(it) },
                onConfirm = { viewModel.confirmEditVehicleType(dialog.vt, dialog.name) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is CatalogDialogState.AddAccessory -> {
            TextInputDialog(
                title = "Agregar Accesorio",
                label = "Nombre del accesorio",
                value = dialog.name,
                onValueChange = { viewModel.updateDialogText(it) },
                onConfirm = { viewModel.confirmAddAccessory(dialog.name) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is CatalogDialogState.EditAccessory -> {
            TextInputDialog(
                title = "Editar Accesorio",
                label = "Nombre del accesorio",
                value = dialog.name,
                onValueChange = { viewModel.updateDialogText(it) },
                onConfirm = { viewModel.confirmEditAccessory(dialog.acc, dialog.name) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is CatalogDialogState.AddComplaint -> {
            TextInputDialog(
                title = "Agregar Motivo",
                label = "Nombre del motivo",
                value = dialog.name,
                onValueChange = { viewModel.updateDialogText(it) },
                onConfirm = { viewModel.confirmAddComplaint(dialog.name) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is CatalogDialogState.EditComplaint -> {
            TextInputDialog(
                title = "Editar Motivo",
                label = "Nombre del motivo",
                value = dialog.name,
                onValueChange = { viewModel.updateDialogText(it) },
                onConfirm = { viewModel.confirmEditComplaint(dialog.complaint, dialog.name) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is CatalogDialogState.AddDiagnosis -> {
            TextInputDialog(
                title = "Agregar Diagn\u00f3stico",
                label = "Nombre del diagn\u00f3stico",
                value = dialog.name,
                onValueChange = { viewModel.updateDialogText(it) },
                onConfirm = { viewModel.confirmAddDiagnosis(dialog.complaintId, dialog.name) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is CatalogDialogState.EditDiagnosis -> {
            TextInputDialog(
                title = "Editar Diagn\u00f3stico",
                label = "Nombre del diagn\u00f3stico",
                value = dialog.name,
                onValueChange = { viewModel.updateDialogText(it) },
                onConfirm = { viewModel.confirmEditDiagnosis(dialog.diagnosis, dialog.name) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is CatalogDialogState.ConfirmDelete -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                title = { Text("Confirmar eliminaci\u00f3n") },
                text = {
                    Text("Est\u00e1 seguro que desea eliminar \"${dialog.name}\"?${
                        if (dialog.type == "brand") "\n\nEsto eliminar\u00e1 tambi\u00e9n todos sus modelos." else ""
                    }")
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmDelete() }) {
                        Text("Eliminar", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDialog() }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        is CatalogDialogState.ImportDialog -> { /* Removed - use backup module */ }
    }
}

@Composable
private fun TextInputDialog(
    title: String,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = value.isNotBlank()) {
                Text("Guardar")
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
private fun ServiceInputDialog(
    title: String,
    category: String,
    name: String,
    price: String,
    vehicleType: String = "",
    existingCategories: List<String>,
    onCategoryChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onVehicleTypeChange: (String) -> Unit = {},
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val vehicleTypeOptions = listOf("", "SEDAN", "SUV", "CAMIONETA")
    var vtDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = category,
                    onValueChange = onCategoryChange,
                    label = { Text("Categor\u00eda") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // Show existing categories as quick chips
                if (existingCategories.isNotEmpty() && category.isEmpty()) {
                    Text(
                        text = "Categor\u00edas existentes:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        existingCategories.forEach { cat ->
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { onCategoryChange(cat) }
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Nombre del servicio") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = onPriceChange,
                    label = { Text("Precio por defecto ($)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                // Vehicle type dropdown
                ExposedDropdownMenuBox(
                    expanded = vtDropdownExpanded,
                    onExpandedChange = { vtDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = if (vehicleType.isBlank()) "Todos" else vehicleType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de veh\u00edculo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vtDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = vtDropdownExpanded,
                        onDismissRequest = { vtDropdownExpanded = false }
                    ) {
                        vehicleTypeOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(if (option.isBlank()) "Todos" else option) },
                                onClick = {
                                    onVehicleTypeChange(option)
                                    vtDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = category.isNotBlank() && name.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
