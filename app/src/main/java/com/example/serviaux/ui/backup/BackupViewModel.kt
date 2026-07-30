/**
 * BackupViewModel.kt - ViewModel del módulo de respaldos.
 *
 * Gestiona la exportación e importación de respaldos ZIP:
 * - Exportación selectiva por categoría o por año.
 * - Importación desde archivo ZIP con selección de categorías a restaurar.
 * - Muestra conteos de registros actuales y del respaldo a importar.
 * - Compartir el archivo exportado vía Intent del sistema.
 * - Integración con Dropbox: vincular cuenta, subir, listar y descargar respaldos.
 */
package com.example.serviaux.ui.backup

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviaux.ServiauxApp
import com.example.serviaux.repository.BackupCategory
import com.example.serviaux.repository.BackupContent
import com.example.serviaux.repository.BackupInspection
import com.example.serviaux.repository.BackupRepository
import com.example.serviaux.repository.PhotoBackupStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.serviaux.util.DropboxBackupEntry
import com.example.serviaux.util.DropboxHelper
import com.example.serviaux.util.PhotoUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupUiState(
    val exporting: Boolean = false,
    val importing: Boolean = false,
    val message: String? = null,
    val exportedFile: File? = null,
    val recordCounts: Map<String, Int> = emptyMap(),
    val showConfirmDialog: Boolean = false,
    val pendingImportUri: Uri? = null,
    val importResult: Map<String, Int>? = null,
    val availableYears: List<Int> = emptyList(),
    val selectedYear: Int? = null,
    val showYearPicker: Boolean = false,
    val exportCategories: Set<BackupCategory> = BackupCategory.entries.toSet(),
    val importCategories: Set<BackupCategory> = BackupCategory.entries.toSet(),
    val backupContents: Map<BackupCategory, Int> = emptyMap(),
    val loadingContents: Boolean = false,
    /** Qué incluirá el próximo respaldo: solo datos, solo fotos o todo con fotos nuevas. */
    val backupContent: BackupContent = BackupContent.ALL_INCREMENTAL,
    /** Cuántas fotos hay y cuántas faltan por respaldar. */
    val photoBackupStatus: PhotoBackupStatus = PhotoBackupStatus(),
    /** Resumen del archivo que se está por restaurar. */
    val inspection: BackupInspection? = null,
    // Dropbox
    val dropboxLinked: Boolean = false,
    val dropboxUploading: Boolean = false,
    val dropboxDownloading: Boolean = false,
    val dropboxBackups: List<DropboxBackupEntry> = emptyList(),
    val showDropboxBackups: Boolean = false,
    val loadingDropboxBackups: Boolean = false,
    // Optimización de fotos
    /** Espacio que ocupan hoy las fotos almacenadas, en bytes. */
    val photoStorageBytes: Long = 0,
    val optimizingPhotos: Boolean = false,
    val optimizeProgress: Pair<Int, Int>? = null,
    val showOptimizeConfirm: Boolean = false
)

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    private val backupRepository: BackupRepository =
        (application as ServiauxApp).container.backupRepository

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadRecordCounts()
        loadAvailableYears()
        checkDropboxLink()
        loadPhotoStorageSize()
        loadPhotoBackupStatus()
    }

    // ── Optimización de fotos ────────────────────────────────────────────

    private fun loadPhotoStorageSize() {
        viewModelScope.launch {
            try {
                val bytes = PhotoUtils.photoStorageSize(getApplication())
                _uiState.update { it.copy(photoStorageBytes = bytes) }
            } catch (_: Exception) { }
        }
    }

    fun requestOptimizePhotos() {
        _uiState.update { it.copy(showOptimizeConfirm = true) }
    }

    fun dismissOptimizeConfirm() {
        _uiState.update { it.copy(showOptimizeConfirm = false) }
    }

    /**
     * Recomprime las fotos ya almacenadas. Es irreversible sobre los archivos locales,
     * por eso la pantalla insiste en exportar un respaldo completo antes.
     */
    fun optimizePhotos() {
        _uiState.update {
            it.copy(showOptimizeConfirm = false, optimizingPhotos = true, message = null, optimizeProgress = 0 to 0)
        }
        viewModelScope.launch {
            try {
                val report = PhotoUtils.optimizeExistingPhotos(getApplication()) { processed, total ->
                    _uiState.update { it.copy(optimizeProgress = processed to total) }
                }
                val savedMb = report.bytesSaved.toDouble() / (1024 * 1024)
                _uiState.update {
                    it.copy(
                        optimizingPhotos = false,
                        optimizeProgress = null,
                        message = if (report.optimizedPhotos == 0)
                            "Las ${report.totalPhotos} fotos ya estaban optimizadas"
                        else
                            "${report.optimizedPhotos} de ${report.totalPhotos} fotos optimizadas. " +
                                "Espacio liberado: ${String.format(java.util.Locale.US, "%.1f", savedMb)} MB"
                    )
                }
                loadPhotoStorageSize()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        optimizingPhotos = false,
                        optimizeProgress = null,
                        message = "Error al optimizar fotos: ${e.message}"
                    )
                }
            }
        }
    }

    fun loadRecordCounts() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val counts = backupRepository.getRecordCounts()
                _uiState.update { it.copy(recordCounts = counts) }
            } catch (_: Exception) { }
        }
    }

    private fun loadAvailableYears() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val years = backupRepository.getAvailableYears()
                _uiState.update { it.copy(availableYears = years) }
            } catch (_: Exception) { }
        }
    }

    fun showYearPicker() {
        _uiState.update { it.copy(showYearPicker = true) }
    }

    fun dismissYearPicker() {
        _uiState.update { it.copy(showYearPicker = false) }
    }

    fun toggleExportCategory(category: BackupCategory) {
        _uiState.update { state ->
            val current = state.exportCategories
            val updated = if (category in current) current - category else current + category
            state.copy(exportCategories = updated)
        }
    }

    fun toggleImportCategory(category: BackupCategory) {
        _uiState.update { state ->
            val current = state.importCategories
            // Solo permitir alternar categorías que existen en el respaldo
            if (category !in state.backupContents) return@update state
            val updated = if (category in current) current - category else current + category
            state.copy(importCategories = updated)
        }
    }

    fun exportByYear(context: Context, year: Int) {
        _uiState.update { it.copy(showYearPicker = false, exporting = true, message = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val result = backupRepository.exportByYear(context, year)
            _uiState.update {
                it.copy(
                    exporting = false,
                    message = result.message,
                    exportedFile = result.file
                )
            }
            if (result.success && result.file != null) {
                shareBackupFile(context, result.file)
            }
        }
    }

    fun exportBackup(context: Context) {
        val state = _uiState.value
        val categories = state.exportCategories
        if (state.backupContent.includesData && categories.isEmpty()) {
            _uiState.update { it.copy(message = "Seleccione al menos una categoría") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(exporting = true, message = null) }
            val result = backupRepository.exportToZip(context, categories, state.backupContent)
            _uiState.update {
                it.copy(
                    exporting = false,
                    message = result.message,
                    exportedFile = result.file
                )
            }
            if (result.success && result.file != null) {
                shareBackupFile(context, result.file)
                loadPhotoBackupStatus()
            }
        }
    }

    /** Cambia qué incluye el respaldo: solo datos, solo fotos, o todo con fotos nuevas. */
    fun setBackupContent(content: BackupContent) {
        _uiState.update { it.copy(backupContent = content) }
    }

    private fun loadPhotoBackupStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val status = backupRepository.photoBackupStatus(getApplication())
                _uiState.update { it.copy(photoBackupStatus = status) }
            } catch (_: Exception) { }
        }
    }

    /**
     * Olvida qué fotos ya se respaldaron: el próximo respaldo con fotos las incluirá todas.
     * Útil si se perdieron los respaldos anteriores.
     */
    fun resetPhotoBackupMarker() {
        backupRepository.resetPhotoBackupMarker(getApplication())
        _uiState.update { it.copy(message = "El próximo respaldo incluirá todas las fotos") }
        loadPhotoBackupStatus()
    }

    private fun shareBackupFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Respaldo Serviaux")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir respaldo").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun requestImport(uri: Uri) {
        _uiState.update { it.copy(loadingContents = true, pendingImportUri = uri) }
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val inspection = backupRepository.inspectBackup(context, uri)
            _uiState.update {
                it.copy(
                    loadingContents = false,
                    showConfirmDialog = true,
                    inspection = inspection,
                    backupContents = inspection.categories,
                    importCategories = inspection.categories.keys.toSet()
                )
            }
        }
    }

    fun confirmImport(context: Context) {
        val state = _uiState.value
        val uri = state.pendingImportUri ?: return
        val categories = state.importCategories
        // Un respaldo de solo fotos no trae categorías de datos y se restaura igual.
        val photosOnly = state.inspection?.categories?.isEmpty() == true &&
            (state.inspection?.photoCount ?: 0) > 0
        if (categories.isEmpty() && !photosOnly) {
            _uiState.update { it.copy(message = "Seleccione al menos una categoría") }
            return
        }
        _uiState.update { it.copy(showConfirmDialog = false, importing = true, message = null, importResult = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val result = backupRepository.importFromZip(context, uri, categories)
            _uiState.update {
                it.copy(
                    importing = false,
                    message = result.message,
                    pendingImportUri = null,
                    importResult = if (result.success) result.counts else null,
                    backupContents = emptyMap(),
                    inspection = null
                )
            }
            if (result.success) {
                loadRecordCounts()
                loadPhotoStorageSize()
            }
        }
    }

    fun cancelImport() {
        _uiState.update {
            it.copy(
                showConfirmDialog = false,
                pendingImportUri = null,
                backupContents = emptyMap(),
                inspection = null
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, importResult = null) }
    }

    // ── Dropbox ──────────────────────────────────────────────────────

    /** Verifica si hay una cuenta de Dropbox vinculada y actualiza el estado. */
    fun checkDropboxLink() {
        val context = getApplication<Application>()
        _uiState.update { it.copy(dropboxLinked = DropboxHelper.isLinked(context)) }
    }

    /** Procesa el resultado de OAuth2 al regresar del navegador (onResume). */
    fun onDropboxAuthResult() {
        val context = getApplication<Application>()
        if (DropboxHelper.handleAuthResult(context)) {
            _uiState.update { it.copy(dropboxLinked = true, message = "Dropbox vinculado correctamente") }
        }
    }

    /**
     * Exporta las categorías seleccionadas a ZIP y lo sube a Dropbox.
     * El archivo se guarda en /Serviaux/{dispositivo}/serviaux_backup_YYYY-MM-dd.zip.
     */
    fun uploadToDropbox(context: Context) {
        val state = _uiState.value
        val categories = state.exportCategories
        if (state.backupContent.includesData && categories.isEmpty()) {
            _uiState.update { it.copy(message = "Seleccione al menos una categoría") }
            return
        }
        _uiState.update { it.copy(dropboxUploading = true, message = null) }
        viewModelScope.launch(Dispatchers.IO) {
            // Paso 1: Exportar ZIP local con el contenido elegido
            val exportResult = backupRepository.exportToZip(context, categories, state.backupContent)
            if (!exportResult.success || exportResult.file == null) {
                _uiState.update { it.copy(dropboxUploading = false, message = exportResult.message) }
                return@launch
            }
            // Paso 2: Subir a Dropbox. El incremental lleva la hora en el nombre para no
            // sobrescribir otro respaldo del mismo día, que contiene fotos distintas.
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val remoteName = when (state.backupContent) {
                BackupContent.DATA_ONLY -> "serviaux_datos_$dateStr.zip"
                BackupContent.MEDIA_ONLY -> "serviaux_fotos_$dateStr.zip"
                BackupContent.ALL_INCREMENTAL -> {
                    val timeStr = SimpleDateFormat("HHmm", Locale.US).format(Date())
                    "serviaux_backup_${dateStr}_$timeStr.zip"
                }
            }
            val uploadResult = DropboxHelper.uploadFile(context, exportResult.file, remoteName)
            uploadResult.fold(
                onSuccess = { path ->
                    _uiState.update {
                        it.copy(dropboxUploading = false, message = "Respaldo subido a Dropbox: ${path.substringAfterLast("/")}")
                    }
                    loadPhotoBackupStatus()
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(dropboxUploading = false, message = "Error subiendo a Dropbox: ${e.message}")
                    }
                }
            )
        }
    }

    /** Lista los respaldos disponibles en Dropbox y muestra el diálogo de selección. */
    fun loadDropboxBackups(context: Context) {
        _uiState.update { it.copy(loadingDropboxBackups = true, showDropboxBackups = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val result = DropboxHelper.listBackups(context)
            result.fold(
                onSuccess = { list ->
                    _uiState.update {
                        it.copy(loadingDropboxBackups = false, dropboxBackups = list)
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            loadingDropboxBackups = false,
                            showDropboxBackups = false,
                            message = "Error listando respaldos: ${e.message}"
                        )
                    }
                }
            )
        }
    }

    /**
     * Descarga un respaldo de Dropbox al caché local y abre el diálogo de importación.
     * Reutiliza [requestImport] para mostrar el checklist de categorías.
     */
    fun downloadFromDropbox(context: Context, entry: DropboxBackupEntry) {
        _uiState.update { it.copy(dropboxDownloading = true, showDropboxBackups = false) }
        viewModelScope.launch(Dispatchers.IO) {
            val localFile = File(context.cacheDir, entry.name.substringAfterLast("/"))
            val result = DropboxHelper.downloadFile(context, entry.path, localFile)
            result.fold(
                onSuccess = { file ->
                    _uiState.update { it.copy(dropboxDownloading = false) }
                    // Abrir flujo de importación normal con el archivo descargado
                    val uri = Uri.fromFile(file)
                    requestImport(uri)
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(dropboxDownloading = false, message = "Error descargando: ${e.message}")
                    }
                }
            )
        }
    }

    /** Desvincula la cuenta de Dropbox eliminando la credencial almacenada. */
    fun unlinkDropbox(context: Context) {
        DropboxHelper.logout(context)
        _uiState.update { it.copy(dropboxLinked = false, message = "Dropbox desvinculado") }
    }

    /** Cierra el diálogo de lista de respaldos de Dropbox. */
    fun dismissDropboxBackups() {
        _uiState.update { it.copy(showDropboxBackups = false) }
    }
}
