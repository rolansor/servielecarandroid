package com.example.serviaux.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.serviaux.data.entity.CatalogBrand

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
    val tabs = listOf("Marcas", "Colores", "Marcas Repuesto")

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

    // Handle export JSON - share or copy to clipboard
    LaunchedEffect(uiState.exportJson) {
        uiState.exportJson?.let { json ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Serviaux Catalog", json))

            try {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_TEXT, json)
                    putExtra(Intent.EXTRA_SUBJECT, "Serviaux - Cat\u00e1logos")
                }
                context.startActivity(Intent.createChooser(shareIntent, "Compartir cat\u00e1logo"))
            } catch (_: Exception) {
                // If share fails, clipboard already has it
            }

            snackbarHostState.showSnackbar("JSON copiado al portapapeles")
            viewModel.clearExportJson()
        }
    }

    // Dialog rendering
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
            // Import/Export buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.exportCatalog() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Exportar")
                }
                OutlinedButton(
                    onClick = { viewModel.showImportDialog() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Importar")
                }
            }

            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
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
                1 -> ColorsTab(uiState = uiState, viewModel = viewModel)
                2 -> PartBrandsTab(uiState = uiState, viewModel = viewModel)
            }
        }
    }
}

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
                    if (uiState.selectedBrandId == brand.id) {
                        viewModel.selectBrand(null)
                    } else {
                        viewModel.selectBrand(brand)
                    }
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
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Brand row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Contraer" else "Expandir",
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
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar marca",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Models sub-list
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(start = 32.dp, end = 16.dp, bottom = 8.dp)) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Modelos",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    models.forEach { model ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = model.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onEditModel(model) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar modelo", modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onDeleteModel(model) }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar modelo",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
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

@Composable
private fun ColorsTab(uiState: CatalogUiState, viewModel: CatalogViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)
    ) {
        items(uiState.colors, key = { it.id }) { color ->
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
                        text = color.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.showEditColorDialog(color) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar color", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { viewModel.showDeleteConfirmation("color", color.id, color.name) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar color",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PartBrandsTab(uiState: CatalogUiState, viewModel: CatalogViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)
    ) {
        items(uiState.partBrands, key = { it.id }) { partBrand ->
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
                        text = partBrand.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { viewModel.showEditPartBrandDialog(partBrand) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
                    }
                    IconButton(
                        onClick = { viewModel.showDeleteConfirmation("partBrand", partBrand.id, partBrand.name) },
                        modifier = Modifier.size(36.dp)
                    ) {
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

        is CatalogDialogState.ImportDialog -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDialog() },
                title = { Text("Importar Cat\u00e1logo") },
                text = {
                    Column {
                        Text(
                            text = "Pegue el JSON del cat\u00e1logo. Esto reemplazar\u00e1 todos los datos existentes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = dialog.jsonText,
                            onValueChange = { viewModel.updateDialogText(it) },
                            label = { Text("JSON") },
                            minLines = 10,
                            maxLines = 15,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.confirmImport(dialog.jsonText) },
                        enabled = dialog.jsonText.isNotBlank()
                    ) {
                        Text("Importar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDialog() }) {
                        Text("Cancelar")
                    }
                }
            )
        }
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
