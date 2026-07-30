/**
 * BackupScreen.kt - Pantalla de gestión de respaldos.
 *
 * Permite exportar e importar respaldos ZIP del sistema:
 * - Exportación: selección de categorías o exportación por año.
 * - Importación: selección de archivo ZIP con checklist de categorías a restaurar.
 * - Muestra resumen de datos actuales y resultados de la última importación.
 * - Sección Dropbox: vincular/desvincular cuenta, subir y descargar respaldos en la nube.
 *
 * Solo accesible para administradores.
 */
package com.example.serviaux.ui.backup

import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.serviaux.repository.BackupCategory
import com.example.serviaux.repository.BackupContent
import com.example.serviaux.util.DropboxHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.requestImport(it) }
    }

    // Detectar onResume para callback de autenticación de Dropbox
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onDropboxAuthResult()
                viewModel.checkDropboxLink()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Mostrar mensajes toast
    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearMessage()
        }
    }

    // Diálogo de lista de respaldos en Dropbox
    if (uiState.showDropboxBackups) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDropboxBackups() },
            title = { Text("Respaldos en Dropbox") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (uiState.loadingDropboxBackups) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else if (uiState.dropboxBackups.isEmpty()) {
                        Text(
                            text = "No se encontraron respaldos en Dropbox.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        uiState.dropboxBackups.forEach { entry ->
                            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                .format(Date(entry.modified))
                            val sizeStr = Formatter.formatShortFileSize(context, entry.size)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { viewModel.downloadFromDropbox(context, entry) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = entry.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "$dateStr · $sizeStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDropboxBackups() }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Diálogo de selección de año
    if (uiState.showYearPicker) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissYearPicker() },
            title = { Text("Seleccionar Año") },
            text = {
                Column {
                    Text(
                        text = "Seleccione el año a exportar:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    uiState.availableYears.forEach { year ->
                        OutlinedButton(
                            onClick = { viewModel.exportByYear(context, year) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(year.toString())
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.dismissYearPicker() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de confirmación de importación con checklist de categorías
    if (uiState.showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelImport() },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Restaurar respaldo") },
            text = {
                Column {
                    if (uiState.loadingContents) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        // Qué trae el archivo: puede ser solo datos, solo fotos o ambos.
                        val inspection = uiState.inspection
                        if (inspection != null && inspection.valid) {
                            Text(
                                text = buildString {
                                    append("Contenido: ${inspection.content.label}")
                                    if (inspection.photoCount > 0) {
                                        append(" · ${inspection.photoCount} fotos")
                                    }
                                    if (inspection.exportDate > 0) {
                                        append("\nGenerado: ${dateFormat.format(Date(inspection.exportDate))}")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if ((inspection?.photoCount ?: 0) > 0) {
                            Text(
                                text = "Las fotos se agregan sin borrar las que ya están en el dispositivo.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (uiState.backupContents.isNotEmpty()) {
                            Text(
                                text = "Seleccione las categorías a restaurar.\nLos datos actuales de cada categoría seleccionada serán reemplazados.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        if (uiState.backupContents.isEmpty()) {
                            Text(
                                text = if ((inspection?.photoCount ?: 0) > 0)
                                    "Este respaldo contiene solo fotos: se restaurarán las imágenes y los datos actuales no se tocan."
                                else
                                    "No se encontraron datos en el respaldo.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if ((inspection?.photoCount ?: 0) > 0)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    MaterialTheme.colorScheme.error
                            )
                        } else {
                            BackupCategory.entries.forEach { category ->
                                val count = uiState.backupContents[category]
                                if (count != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = category in uiState.importCategories,
                                            onCheckedChange = { viewModel.toggleImportCategory(category) }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = category.label,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "$count registros",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    // Un respaldo de solo fotos no tiene categorías que marcar y se restaura igual.
                    onClick = { viewModel.confirmImport(context) },
                    enabled = (uiState.importCategories.isNotEmpty() && uiState.backupContents.isNotEmpty()) ||
                        ((uiState.inspection?.photoCount ?: 0) > 0 && uiState.backupContents.isEmpty()),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Restaurar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelImport() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Confirmación de optimización de fotos: es irreversible sobre los archivos del dispositivo.
    if (uiState.showOptimizeConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissOptimizeConfirm() },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Optimizar fotos existentes") },
            text = {
                Column {
                    Text(
                        "Las fotos guardadas se reducirán a un tamaño más liviano (1600 px de lado " +
                            "mayor). Se ven igual en la app y en los reportes PDF, pero los archivos " +
                            "originales a resolución completa no se pueden recuperar."
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Exporte un respaldo completo antes de continuar: ese respaldo conserva las " +
                            "fotos originales.",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.optimizePhotos() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Optimizar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissOptimizeConfirm() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = 40.dp,
                title = { Text("Respaldos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Resumen de datos actuales
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Datos actuales",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (uiState.recordCounts.isEmpty()) {
                        Text(
                            text = "Cargando...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        uiState.recordCounts.forEach { (label, count) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Sección de exportación
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Exportar Respaldo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Qué incluye el respaldo: los datos pesan poco, las fotos son casi todo.
                    Column(modifier = Modifier.fillMaxWidth()) {
                        BackupContent.entries.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setBackupContent(option) }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.backupContent == option,
                                    onClick = { viewModel.setBackupContent(option) }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = option.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Situación del incremental: cuántas fotos faltan por respaldar.
                    if (uiState.backupContent == BackupContent.ALL_INCREMENTAL) {
                        val status = uiState.photoBackupStatus
                        Spacer(modifier = Modifier.height(8.dp))
                        val pendingMb = status.pendingBytes.toDouble() / (1024 * 1024)
                        Text(
                            text = if (status.lastBackupAt == 0L)
                                "Nunca se han respaldado fotos: este respaldo incluirá las " +
                                    "${status.totalPhotos} existentes."
                            else
                                "Último respaldo con fotos: ${dateFormat.format(Date(status.lastBackupAt))}. " +
                                    "Pendientes: ${status.pendingPhotos} de ${status.totalPhotos} " +
                                    "(${String.format(Locale.US, "%.1f", pendingMb)} MB).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (status.lastBackupAt > 0L) {
                            TextButton(onClick = { viewModel.resetPhotoBackupMarker() }) {
                                Text("Volver a incluir todas las fotos", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    if (uiState.backupContent.includesData) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Categorías de datos a incluir:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Checklist de categorías para exportar
                        BackupCategory.entries.forEach { category ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = category in uiState.exportCategories,
                                    onCheckedChange = { viewModel.toggleExportCategory(category) }
                                )
                                Text(
                                    text = category.label,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.exportBackup(context) },
                        enabled = !uiState.exporting && !uiState.importing &&
                            (!uiState.backupContent.includesData || uiState.exportCategories.isNotEmpty()),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.exporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Exportando...")
                        } else {
                            Text(
                                when (uiState.backupContent) {
                                    BackupContent.DATA_ONLY -> "Exportar Datos"
                                    BackupContent.MEDIA_ONLY -> "Exportar Fotos"
                                    BackupContent.ALL_INCREMENTAL -> "Exportar Todo"
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.showYearPicker() },
                        enabled = !uiState.exporting && !uiState.importing && uiState.availableYears.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Exportar por Año")
                    }
                }
            }

            // Sección de importación
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Restaurar Respaldo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Selecciona un archivo .zip de respaldo. Podrás elegir qué categorías restaurar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                        },
                        enabled = !uiState.exporting && !uiState.importing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.importing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restaurando...")
                        } else {
                            Text("Seleccionar Archivo")
                        }
                    }
                }
            }

            // Resultados de importación
            uiState.importResult?.let { results ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Resumen de restauración",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        results.forEach { (table, count) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = table,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Optimización de fotos: las imágenes son casi todo el peso del respaldo.
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Optimizar Fotos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val photoMb = uiState.photoStorageBytes.toDouble() / (1024 * 1024)
                    Text(
                        text = "Las fotos ocupan ${String.format(Locale.US, "%.1f", photoMb)} MB y son la mayor parte " +
                            "del tamaño del respaldo. Las fotos nuevas ya se comprimen al tomarlas; " +
                            "esta acción reduce las que se guardaron antes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (uiState.optimizingPhotos) {
                        val progress = uiState.optimizeProgress
                        if (progress != null && progress.second > 0) {
                            LinearProgressIndicator(
                                progress = { progress.first.toFloat() / progress.second },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Optimizando ${progress.first} de ${progress.second}...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.requestOptimizePhotos() },
                            enabled = !uiState.exporting && !uiState.importing && uiState.photoStorageBytes > 0,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Optimizar fotos existentes")
                        }
                    }
                }
            }

            HorizontalDivider()

            // Sección de Dropbox
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Dropbox",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (!uiState.dropboxLinked) {
                        Text(
                            text = "Vincula tu cuenta de Dropbox para subir y descargar respaldos en la nube.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { DropboxHelper.startAuth(context) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Vincular Dropbox")
                        }
                    } else {
                        Text(
                            text = "Cuenta vinculada",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.uploadToDropbox(context) },
                            enabled = !uiState.dropboxUploading && !uiState.exporting && uiState.exportCategories.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.dropboxUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Subiendo...")
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Subir Respaldo a Dropbox")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.loadDropboxBackups(context) },
                            enabled = !uiState.dropboxDownloading && !uiState.loadingDropboxBackups,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (uiState.dropboxDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Descargando...")
                            } else {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Descargar de Dropbox")
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = { viewModel.unlinkDropbox(context) }
                        ) {
                            Text(
                                "Desvincular",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
