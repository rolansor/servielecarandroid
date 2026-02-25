package com.example.serviaux.ui.backup

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviaux.ServiauxApp
import com.example.serviaux.repository.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class BackupUiState(
    val exporting: Boolean = false,
    val importing: Boolean = false,
    val message: String? = null,
    val exportedFile: File? = null,
    val recordCounts: Map<String, Int> = emptyMap(),
    val showConfirmDialog: Boolean = false,
    val pendingImportUri: Uri? = null,
    val importResult: Map<String, Int>? = null
)

class BackupViewModel(application: Application) : AndroidViewModel(application) {
    private val backupRepository: BackupRepository =
        (application as ServiauxApp).container.backupRepository

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadRecordCounts()
    }

    fun loadRecordCounts() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val counts = backupRepository.getRecordCounts()
                _uiState.update { it.copy(recordCounts = counts) }
            } catch (_: Exception) { }
        }
    }

    fun exportBackup(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(exporting = true, message = null) }
            val result = backupRepository.exportToZip(context)
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
        _uiState.update { it.copy(showConfirmDialog = true, pendingImportUri = uri) }
    }

    fun confirmImport(context: Context) {
        val uri = _uiState.value.pendingImportUri ?: return
        _uiState.update { it.copy(showConfirmDialog = false, importing = true, message = null, importResult = null) }
        viewModelScope.launch(Dispatchers.IO) {
            val result = backupRepository.importFromZip(context, uri)
            _uiState.update {
                it.copy(
                    importing = false,
                    message = result.message,
                    pendingImportUri = null,
                    importResult = if (result.success) result.counts else null
                )
            }
            if (result.success) {
                loadRecordCounts()
            }
        }
    }

    fun cancelImport() {
        _uiState.update { it.copy(showConfirmDialog = false, pendingImportUri = null) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null, importResult = null) }
    }
}
