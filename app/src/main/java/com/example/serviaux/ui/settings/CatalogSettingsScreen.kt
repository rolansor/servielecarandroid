/**
 * CatalogSettingsScreen.kt - Pantalla de configuración de catálogos.
 *
 * Rediseño s15: en vez de 9 pestañas, un hub con buscador global y una
 * lista de categorías con su conteo. Cada categoría abre su propia vista
 * con buscador; "Marcas y modelos" y "Motivos y diagnósticos" usan dos
 * pestañas planas (ya no hay anidado expandible). Agregar un modelo o un
 * diagnóstico pide la marca / el motivo con un dropdown buscable.
 *
 * El buscador del hub busca en todos los catálogos a la vez y tocar un
 * resultado abre su diálogo de edición. Solo accesible para administradores.
 */
package com.example.serviaux.ui.settings

import com.example.serviaux.util.formatMoney

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.serviaux.data.entity.CatalogBrand
import com.example.serviaux.data.entity.CatalogComplaint
import com.example.serviaux.data.entity.CatalogService
import com.example.serviaux.ui.components.SearchableDropdown
import com.example.serviaux.ui.components.SearchableItem

/** Categorías del hub de catálogos, en el orden en que se listan. */
enum class CatalogCategory(val title: String) {
    MARCAS_MODELOS("Marcas y modelos"),
    SERVICIOS("Servicios y precios"),
    MOTIVOS_DIAGNOSTICOS("Motivos y diagnósticos"),
    COLORES("Colores"),
    MARCAS_REPUESTOS("Marcas de repuestos"),
    ACCESORIOS("Accesorios del checklist"),
    ACEITES("Aceites"),
    TIPOS_VEHICULO("Tipos de vehículo")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: CatalogViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var openCategory by rememberSaveable { mutableStateOf<CatalogCategory?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    fun backToHub() {
        openCategory = null
        query = ""
        selectedTab = 0
    }

    BackHandler(enabled = openCategory != null) { backToHub() }

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
                expandedHeight = 40.dp,
                title = { Text(openCategory?.title ?: "Catálogos") },
                navigationIcon = {
                    IconButton(onClick = { if (openCategory != null) backToHub() else onNavigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            val category = openCategory
            if (category != null) {
                FloatingActionButton(
                    onClick = {
                        when (category) {
                            CatalogCategory.MARCAS_MODELOS ->
                                if (selectedTab == 0) viewModel.showAddBrandDialog()
                                else viewModel.showAddModelDialog()
                            CatalogCategory.MOTIVOS_DIAGNOSTICOS ->
                                if (selectedTab == 0) viewModel.showAddComplaintDialog()
                                else viewModel.showAddDiagnosisDialog()
                            CatalogCategory.SERVICIOS -> viewModel.showAddServiceDialog()
                            CatalogCategory.COLORES -> viewModel.showAddColorDialog()
                            CatalogCategory.MARCAS_REPUESTOS -> viewModel.showAddPartBrandDialog()
                            CatalogCategory.ACCESORIOS -> viewModel.showAddAccessoryDialog()
                            CatalogCategory.ACEITES -> viewModel.showAddOilTypeDialog()
                            CatalogCategory.TIPOS_VEHICULO -> viewModel.showAddVehicleTypeDialog()
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            CatalogSearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = if (openCategory == null) "Buscar en todos los catálogos"
                else "Buscar en ${openCategory!!.title.lowercase()}"
            )

            when (val category = openCategory) {
                null ->
                    if (query.isBlank()) {
                        CatalogHub(uiState = uiState, onOpenCategory = { openCategory = it; query = "" })
                    } else {
                        GlobalSearchResults(uiState = uiState, query = query, viewModel = viewModel)
                    }
                CatalogCategory.MARCAS_MODELOS ->
                    BrandsModelsContent(uiState, query, selectedTab, { selectedTab = it }, viewModel)
                CatalogCategory.MOTIVOS_DIAGNOSTICOS ->
                    ComplaintsDiagnosesContent(uiState, query, selectedTab, { selectedTab = it }, viewModel)
                CatalogCategory.SERVICIOS ->
                    ServicesContent(uiState, query, viewModel)
                CatalogCategory.COLORES -> SimpleCatalogList(
                    items = uiState.colors.map { CatalogRowData(it.id, it.name) },
                    query = query,
                    onEdit = { id -> uiState.colors.find { it.id == id }?.let { viewModel.showEditColorDialog(it) } },
                    onDelete = { id, name -> viewModel.showDeleteConfirmation("color", id, name) }
                )
                CatalogCategory.MARCAS_REPUESTOS -> SimpleCatalogList(
                    items = uiState.partBrands.map { CatalogRowData(it.id, it.name) },
                    query = query,
                    onEdit = { id -> uiState.partBrands.find { it.id == id }?.let { viewModel.showEditPartBrandDialog(it) } },
                    onDelete = { id, name -> viewModel.showDeleteConfirmation("partBrand", id, name) }
                )
                CatalogCategory.ACCESORIOS -> SimpleCatalogList(
                    items = uiState.accessories.map { CatalogRowData(it.id, it.name) },
                    query = query,
                    onEdit = { id -> uiState.accessories.find { it.id == id }?.let { viewModel.showEditAccessoryDialog(it) } },
                    onDelete = { id, name -> viewModel.showDeleteConfirmation("accessory", id, name) }
                )
                CatalogCategory.ACEITES -> SimpleCatalogList(
                    items = uiState.oilTypes.map { CatalogRowData(it.id, it.name) },
                    query = query,
                    onEdit = { id -> uiState.oilTypes.find { it.id == id }?.let { viewModel.showEditOilTypeDialog(it) } },
                    onDelete = { id, name -> viewModel.showDeleteConfirmation("oilType", id, name) }
                )
                CatalogCategory.TIPOS_VEHICULO -> SimpleCatalogList(
                    items = uiState.vehicleTypes.map { CatalogRowData(it.id, it.name) },
                    query = query,
                    onEdit = { id -> uiState.vehicleTypes.find { it.id == id }?.let { viewModel.showEditVehicleTypeDialog(it) } },
                    onDelete = { id, name -> viewModel.showDeleteConfirmation("vehicleType", id, name) }
                )
            }
        }
    }
}

// ─── Buscador ────────────────────────────────────────────────────────

@Composable
private fun CatalogSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Limpiar", modifier = Modifier.size(20.dp))
                }
            }
        },
        singleLine = true,
        shape = CircleShape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// ─── Hub: lista de categorías con conteo ─────────────────────────────

@Composable
private fun CatalogHub(
    uiState: CatalogUiState,
    onOpenCategory: (CatalogCategory) -> Unit
) {
    fun count(category: CatalogCategory): Int = when (category) {
        CatalogCategory.MARCAS_MODELOS -> uiState.brands.size + uiState.allModels.size
        CatalogCategory.SERVICIOS -> uiState.services.size
        CatalogCategory.MOTIVOS_DIAGNOSTICOS -> uiState.complaints.size + uiState.diagnoses.size
        CatalogCategory.COLORES -> uiState.colors.size
        CatalogCategory.MARCAS_REPUESTOS -> uiState.partBrands.size
        CatalogCategory.ACCESORIOS -> uiState.accessories.size
        CatalogCategory.ACEITES -> uiState.oilTypes.size
        CatalogCategory.TIPOS_VEHICULO -> uiState.vehicleTypes.size
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                CatalogCategory.entries.forEachIndexed { index, category ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(horizontal = 18.dp)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenCategory(category) }
                            .defaultMinSize(minHeight = 52.dp)
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = category.title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${count(category)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "El inventario de repuestos no vive aquí: se administra en su propia pestaña.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

// ─── Búsqueda global ─────────────────────────────────────────────────

private data class GlobalResult(
    val key: String,
    val name: String,
    val subtitle: String?,
    val onClick: () -> Unit
)

@Composable
private fun GlobalSearchResults(
    uiState: CatalogUiState,
    query: String,
    viewModel: CatalogViewModel
) {
    val brandNames = uiState.brands.associate { it.id to it.name }
    val complaintNames = uiState.complaints.associate { it.id to it.name }

    fun matches(text: String) = text.contains(query, ignoreCase = true)

    val groups: List<Pair<String, List<GlobalResult>>> = listOf(
        "Marcas" to uiState.brands.filter { matches(it.name) }
            .map { b -> GlobalResult("brand_${b.id}", b.name, null) { viewModel.showEditBrandDialog(b) } },
        "Modelos" to uiState.allModels.filter { matches(it.name) || matches(brandNames[it.brandId] ?: "") }
            .map { m -> GlobalResult("model_${m.id}", m.name, brandNames[m.brandId]) { viewModel.showEditModelDialog(m) } },
        "Servicios" to uiState.services.filter { matches(it.name) || matches(it.category) }
            .map { s -> GlobalResult("service_${s.id}", s.name, "${s.category} · ${formatMoney(s.defaultPrice)}") { viewModel.showEditServiceDialog(s) } },
        "Motivos" to uiState.complaints.filter { matches(it.name) }
            .map { c -> GlobalResult("complaint_${c.id}", c.name, null) { viewModel.showEditComplaintDialog(c) } },
        "Diagnósticos" to uiState.diagnoses.filter { matches(it.name) }
            .map { d -> GlobalResult("diagnosis_${d.id}", d.name, complaintNames[d.complaintId]) { viewModel.showEditDiagnosisDialog(d) } },
        "Colores" to uiState.colors.filter { matches(it.name) }
            .map { c -> GlobalResult("color_${c.id}", c.name, null) { viewModel.showEditColorDialog(c) } },
        "Marcas de repuestos" to uiState.partBrands.filter { matches(it.name) }
            .map { p -> GlobalResult("partBrand_${p.id}", p.name, null) { viewModel.showEditPartBrandDialog(p) } },
        "Accesorios" to uiState.accessories.filter { matches(it.name) }
            .map { a -> GlobalResult("accessory_${a.id}", a.name, null) { viewModel.showEditAccessoryDialog(a) } },
        "Aceites" to uiState.oilTypes.filter { matches(it.name) }
            .map { o -> GlobalResult("oil_${o.id}", o.name, null) { viewModel.showEditOilTypeDialog(o) } },
        "Tipos de vehículo" to uiState.vehicleTypes.filter { matches(it.name) }
            .map { v -> GlobalResult("vt_${v.id}", v.name, null) { viewModel.showEditVehicleTypeDialog(v) } }
    ).filter { it.second.isNotEmpty() }

    if (groups.isEmpty()) {
        Text(
            text = "Sin resultados para \"$query\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        groups.forEach { (label, results) ->
            item(key = "header_$label") {
                Text(
                    text = label.uppercase() + " (${results.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp)
                )
            }
            items(results, key = { it.key }) { result ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { result.onClick() }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(result.name, style = MaterialTheme.typography.bodyLarge)
                        if (result.subtitle != null) {
                            Text(
                                text = result.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ─── Fila estándar de las listas de categoría ────────────────────────

private data class CatalogRowData(val id: Long, val name: String, val subtitle: String? = null)

@Composable
private fun CatalogRow(
    item: CatalogRowData,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyLarge)
            if (item.subtitle != null) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Editar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Eliminar",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SimpleCatalogList(
    items: List<CatalogRowData>,
    query: String,
    onEdit: (Long) -> Unit,
    onDelete: (Long, String) -> Unit
) {
    val filtered = if (query.isBlank()) items
    else items.filter {
        it.name.contains(query, ignoreCase = true) ||
            it.subtitle?.contains(query, ignoreCase = true) == true
    }

    if (filtered.isEmpty()) {
        Text(
            text = if (query.isBlank()) "Sin elementos todavía" else "Sin resultados para \"$query\"",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)
    ) {
        items(filtered, key = { it.id }) { item ->
            CatalogRow(
                item = item,
                onEdit = { onEdit(item.id) },
                onDelete = { onDelete(item.id, item.name) }
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

// ─── Marcas y modelos (dos pestañas planas) ──────────────────────────

@Composable
private fun BrandsModelsContent(
    uiState: CatalogUiState,
    query: String,
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    viewModel: CatalogViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { onTabChange(0) }, text = { Text("Marcas") })
            Tab(selected = selectedTab == 1, onClick = { onTabChange(1) }, text = { Text("Modelos") })
        }
        if (selectedTab == 0) {
            val modelCount = uiState.allModels.groupingBy { it.brandId }.eachCount()
            SimpleCatalogList(
                items = uiState.brands.map { brand ->
                    val n = modelCount[brand.id] ?: 0
                    CatalogRowData(brand.id, brand.name, if (n == 1) "1 modelo" else "$n modelos")
                },
                query = query,
                onEdit = { id -> uiState.brands.find { it.id == id }?.let { viewModel.showEditBrandDialog(it) } },
                onDelete = { id, name -> viewModel.showDeleteConfirmation("brand", id, name) }
            )
        } else {
            val brandNames = uiState.brands.associate { it.id to it.name }
            SimpleCatalogList(
                items = uiState.allModels.map { model ->
                    CatalogRowData(model.id, model.name, brandNames[model.brandId] ?: "Sin marca")
                },
                query = query,
                onEdit = { id -> uiState.allModels.find { it.id == id }?.let { viewModel.showEditModelDialog(it) } },
                onDelete = { id, name -> viewModel.showDeleteConfirmation("model", id, name) }
            )
        }
    }
}

// ─── Motivos y diagnósticos (dos pestañas planas) ────────────────────

@Composable
private fun ComplaintsDiagnosesContent(
    uiState: CatalogUiState,
    query: String,
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    viewModel: CatalogViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { onTabChange(0) }, text = { Text("Motivos") })
            Tab(selected = selectedTab == 1, onClick = { onTabChange(1) }, text = { Text("Diagnósticos") })
        }
        if (selectedTab == 0) {
            val diagnosisCount = uiState.diagnoses.groupingBy { it.complaintId }.eachCount()
            SimpleCatalogList(
                items = uiState.complaints.map { complaint ->
                    val n = diagnosisCount[complaint.id] ?: 0
                    CatalogRowData(complaint.id, complaint.name, if (n == 1) "1 diagnóstico" else "$n diagnósticos")
                },
                query = query,
                onEdit = { id -> uiState.complaints.find { it.id == id }?.let { viewModel.showEditComplaintDialog(it) } },
                onDelete = { id, name -> viewModel.showDeleteConfirmation("complaint", id, name) }
            )
        } else {
            val complaintNames = uiState.complaints.associate { it.id to it.name }
            SimpleCatalogList(
                items = uiState.diagnoses.map { diagnosis ->
                    CatalogRowData(diagnosis.id, diagnosis.name, complaintNames[diagnosis.complaintId] ?: "Sin motivo")
                },
                query = query,
                onEdit = { id -> uiState.diagnoses.find { it.id == id }?.let { viewModel.showEditDiagnosisDialog(it) } },
                onDelete = { id, name -> viewModel.showDeleteConfirmation("diagnosis", id, name) }
            )
        }
    }
}

// ─── Servicios (agrupados por categoría; búsqueda aplana) ────────────

@Composable
private fun ServicesContent(
    uiState: CatalogUiState,
    query: String,
    viewModel: CatalogViewModel
) {
    if (query.isNotBlank()) {
        SimpleCatalogList(
            items = uiState.services
                .filter { it.name.contains(query, ignoreCase = true) || it.category.contains(query, ignoreCase = true) }
                .map { CatalogRowData(it.id, it.name, "${it.category} · ${formatMoney(it.defaultPrice)}") },
            query = "",
            onEdit = { id -> uiState.services.find { it.id == id }?.let { viewModel.showEditServiceDialog(it) } },
            onDelete = { id, name -> viewModel.showDeleteConfirmation("service", id, name) }
        )
        return
    }

    val servicesByCategory = uiState.services.groupBy { it.category }
    val categories = servicesByCategory.keys.sorted()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)
    ) {
        items(categories, key = { it }) { category ->
            val services = servicesByCategory[category] ?: emptyList()
            val isExpanded = uiState.expandedServiceCategory == category

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleServiceCategory(category) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(category, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = if (services.size == 1) "1 servicio" else "${services.size} servicios",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AnimatedVisibility(visible = isExpanded) {
                    Column {
                        services.forEach { service ->
                            ServiceRow(
                                service = service,
                                onEdit = { viewModel.showEditServiceDialog(service) },
                                onDelete = { viewModel.showDeleteConfirmation("service", service.id, service.name) }
                            )
                        }
                        TextButton(
                            onClick = { viewModel.showAddServiceDialog(category) },
                            modifier = Modifier.padding(start = 20.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Agregar en $category")
                        }
                    }
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun ServiceRow(
    service: CatalogService,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(start = 28.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(service.name, style = MaterialTheme.typography.bodyMedium)
            // El chip de tipo va junto al precio: al lado del nombre se
            // aplastaba y partía letra por letra con nombres largos.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = formatMoney(service.defaultPrice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                if (service.vehicleType != null) {
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
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Editar",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Eliminar",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
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
            ParentPickerInputDialog(
                title = "Agregar Modelo",
                parentLabel = "Marca",
                parents = uiState.brands.map { SearchableItem(it.id, it.name) },
                initialParentId = dialog.brandId,
                nameLabel = "Nombre del modelo",
                name = dialog.name,
                onNameChange = { viewModel.updateDialogText(it) },
                onConfirm = { parentId -> viewModel.confirmAddModel(parentId, dialog.name) },
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
                title = "Agregar Tipo de Vehículo",
                label = "Nombre del tipo",
                value = dialog.name,
                onValueChange = { viewModel.updateDialogText(it) },
                onConfirm = { viewModel.confirmAddVehicleType(dialog.name) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is CatalogDialogState.EditVehicleType -> {
            TextInputDialog(
                title = "Editar Tipo de Vehículo",
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

        is CatalogDialogState.AddOilType -> {
            TextInputDialog(
                title = "Agregar Tipo de Aceite",
                label = "Tipo de aceite",
                value = dialog.name,
                onValueChange = { viewModel.updateDialogText(it) },
                onConfirm = { viewModel.confirmAddOilType(dialog.name) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is CatalogDialogState.EditOilType -> {
            TextInputDialog(
                title = "Editar Tipo de Aceite",
                label = "Tipo de aceite",
                value = dialog.name,
                onValueChange = { viewModel.updateDialogText(it) },
                onConfirm = { viewModel.confirmEditOilType(dialog.oilType, dialog.name) },
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
            ParentPickerInputDialog(
                title = "Agregar Diagnóstico",
                parentLabel = "Motivo",
                parents = uiState.complaints.map { SearchableItem(it.id, it.name) },
                initialParentId = dialog.complaintId,
                nameLabel = "Nombre del diagnóstico",
                name = dialog.name,
                onNameChange = { viewModel.updateDialogText(it) },
                onConfirm = { parentId -> viewModel.confirmAddDiagnosis(parentId, dialog.name) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is CatalogDialogState.EditDiagnosis -> {
            TextInputDialog(
                title = "Editar Diagnóstico",
                label = "Nombre del diagnóstico",
                value = dialog.name,
                onValueChange = { viewModel.updateDialogText(it) },
                onConfirm = { viewModel.confirmEditDiagnosis(dialog.diagnosis, dialog.name) },
                onDismiss = { viewModel.dismissDialog() }
            )
        }

        is CatalogDialogState.ConfirmDelete -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                title = { Text("Confirmar eliminación") },
                text = {
                    Text("Está seguro que desea eliminar \"${dialog.name}\"?${
                        if (dialog.type == "brand") "\n\nEsto eliminará también todos sus modelos." else ""
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
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
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

/**
 * Diálogo de alta para elementos que dependen de un padre (modelo → marca,
 * diagnóstico → motivo). El padre se elige con dropdown buscable y puede
 * venir preseleccionado.
 */
@Composable
private fun ParentPickerInputDialog(
    title: String,
    parentLabel: String,
    parents: List<SearchableItem>,
    initialParentId: Long?,
    nameLabel: String,
    name: String,
    onNameChange: (String) -> Unit,
    onConfirm: (parentId: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedParentId by remember {
        mutableStateOf(initialParentId?.takeIf { id -> parents.any { it.id == id } })
    }
    var parentQuery by remember {
        mutableStateOf(parents.find { it.id == initialParentId }?.name ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SearchableDropdown(
                    value = parentQuery,
                    onValueChange = {
                        parentQuery = it
                        selectedParentId = parents.find { p -> p.name == it }?.id
                    },
                    items = parents,
                    onItemSelected = {
                        selectedParentId = it.id
                        parentQuery = it.name
                    },
                    label = parentLabel,
                    isError = parentQuery.isNotBlank() && selectedParentId == null,
                    supportingText = if (parentQuery.isNotBlank() && selectedParentId == null) {
                        { Text("Elija $parentLabel de la lista") }
                    } else null
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(nameLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { selectedParentId?.let { onConfirm(it) } },
                enabled = selectedParentId != null && name.isNotBlank()
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
                    label = { Text("Categoría") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth()
                )
                // Show existing categories as quick chips
                if (existingCategories.isNotEmpty() && category.isEmpty()) {
                    Text(
                        text = "Categorías existentes:",
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
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
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
                        label = { Text("Tipo de vehículo") },
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
