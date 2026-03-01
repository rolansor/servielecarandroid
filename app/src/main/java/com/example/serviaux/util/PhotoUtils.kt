/**
 * PhotoUtils.kt - Utilidades para gestión de fotos y archivos adjuntos.
 *
 * Centraliza las operaciones de archivos para fotos de vehículos/órdenes
 * y archivos adjuntos de órdenes de trabajo:
 * - Creación de archivos temporales con nombres únicos.
 * - Copia desde URIs externas al almacenamiento interno de la app.
 * - Serialización/deserialización de listas de rutas (formato CSV).
 * - Eliminación de archivos.
 *
 * Las fotos se guardan en `filesDir/vehicle_photos/` y los archivos
 * adjuntos en `filesDir/work_order_files/`.
 */
package com.example.serviaux.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilidades estáticas para gestión de fotos y archivos adjuntos.
 */
object PhotoUtils {

    private const val PHOTOS_DIR = "vehicle_photos"

    /** Crea un archivo JPG con nombre único basado en timestamp y nanoTime. */
    fun createTempPhotoFile(context: Context, prefix: String = "VEH"): File {
        val dir = File(context.filesDir, PHOTOS_DIR).apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(dir, "${prefix}_${timestamp}_${System.nanoTime()}.jpg")
    }

    /** Genera una URI compartible vía FileProvider para un archivo local. */
    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /** Convierte una lista de rutas en cadena CSV; retorna null si la lista está vacía. */
    fun serializePaths(paths: List<String>): String? {
        return if (paths.isEmpty()) null else paths.joinToString(",")
    }

    /** Deserializa una cadena CSV de rutas, filtrando las que ya no existen en disco. */
    fun parsePaths(photoPaths: String?): List<String> {
        if (photoPaths.isNullOrBlank()) return emptyList()
        return photoPaths.split(",").filter { it.isNotBlank() && File(it).exists() }
    }

    fun deletePhoto(path: String) {
        val file = File(path)
        if (file.exists()) file.delete()
    }

    /**
     * Copia una imagen desde una URI externa al almacenamiento interno.
     * @return El archivo destino si la copia fue exitosa, null en caso de error.
     */
    fun copyUriToInternalStorage(context: Context, uri: Uri, prefix: String = "VEH"): File? {
        return try {
            val destFile = createTempPhotoFile(context, prefix)
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (destFile.exists() && destFile.length() > 0) destFile else {
                destFile.delete()
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // ── Archivos adjuntos de órdenes ──────────────────────────────────

    private const val FILES_DIR = "work_order_files"

    /**
     * Copia un archivo adjunto desde una URI externa al almacenamiento interno.
     * Preserva la extensión original del archivo.
     */
    fun copyFileToInternalStorage(context: Context, uri: Uri, prefix: String = "WO"): File? {
        return try {
            val dir = File(context.filesDir, FILES_DIR).apply { mkdirs() }
            val originalName = getFileName(context, uri)
            val extension = originalName?.substringAfterLast('.', "") ?: ""
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "${prefix}_${timestamp}_${System.nanoTime()}" +
                if (extension.isNotBlank()) ".$extension" else ""
            val destFile = File(dir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (destFile.exists() && destFile.length() > 0) destFile else {
                destFile.delete()
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Obtiene el nombre original del archivo desde el ContentResolver o la ruta de la URI. */
    fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) name = cursor.getString(idx)
                }
            }
        }
        if (name == null) {
            name = uri.path?.substringAfterLast('/')
        }
        return name
    }

    fun getFileExtension(path: String): String {
        return path.substringAfterLast('.', "").lowercase()
    }

    fun deleteFile(path: String) {
        val file = File(path)
        if (file.exists()) file.delete()
    }
}
