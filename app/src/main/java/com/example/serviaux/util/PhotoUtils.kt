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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utilidades estáticas para gestión de fotos y archivos adjuntos.
 */
object PhotoUtils {

    private const val PHOTOS_DIR = "vehicle_photos"

    /**
     * Lado mayor máximo de una foto almacenada, en píxeles.
     *
     * Las cámaras de los teléfonos entregan imágenes de 3 a 5 MB que no aportan nada frente a
     * este tamaño: en pantalla se ven como miniaturas y en el PDF ocupan 80 pt. Reducirlas aquí
     * es lo que mantiene manejables el almacenamiento del dispositivo y los respaldos.
     */
    const val MAX_PHOTO_DIMENSION = 1600

    /** Calidad de recompresión JPEG. */
    const val JPEG_QUALITY = 80

    /** Máximo de fotos por vehículo y por orden. */
    const val MAX_PHOTOS = 6

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
     * Copia una imagen desde una URI externa al almacenamiento interno y la comprime.
     *
     * Se ejecuta en [Dispatchers.IO]: la URI puede apuntar a un proveedor remoto (Drive,
     * Fotos) y la copia implicaría descarga por red.
     *
     * @return El archivo destino si la copia fue exitosa, null en caso de error.
     */
    suspend fun copyUriToInternalStorage(context: Context, uri: Uri, prefix: String = "VEH"): File? =
        withContext(Dispatchers.IO) {
            try {
                val destFile = createTempPhotoFile(context, prefix)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (destFile.exists() && destFile.length() > 0) {
                    compressPhotoInPlace(destFile)
                    destFile
                } else {
                    destFile.delete()
                    null
                }
            } catch (_: Exception) {
                null
            }
        }

    /**
     * Comprime una foto sobre sí misma: la reduce a [MAX_PHOTO_DIMENSION] en su lado mayor y la
     * reescribe como JPEG de calidad [JPEG_QUALITY].
     *
     * La rotación indicada por el EXIF se aplica a los píxeles, porque al reescribir el archivo
     * la metadata se pierde: así la foto se ve derecha en cualquier visor y en los PDF.
     * Si algo falla, el archivo original se deja intacto.
     *
     * @return bytes ahorrados (0 si no hizo falta comprimir o no se pudo).
     */
    suspend fun compressPhotoInPlace(file: File): Long = withContext(Dispatchers.IO) {
        if (!file.exists() || file.length() == 0L) return@withContext 0L
        val originalSize = file.length()
        var source: Bitmap? = null
        var rotated: Bitmap? = null
        var scaled: Bitmap? = null
        try {
            // 1. Dimensiones sin cargar los píxeles.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext 0L

            val rotationDegrees = exifRotationDegrees(file)
            val longestSide = maxOf(bounds.outWidth, bounds.outHeight)
            if (longestSide <= MAX_PHOTO_DIMENSION && rotationDegrees == 0 && originalSize < 600_000L) {
                // Ya es pequeña y está derecha: recomprimir solo degradaría la imagen.
                return@withContext 0L
            }

            // 2. Decodificar submuestreando para no cargar el bitmap completo en memoria.
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(longestSide, MAX_PHOTO_DIMENSION)
            }
            source = BitmapFactory.decodeFile(file.absolutePath, options) ?: return@withContext 0L

            // 3. Aplicar la rotación EXIF a los píxeles.
            rotated = if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            } else source

            // 4. Ajuste fino al tamaño objetivo conservando la proporción.
            val currentLongest = maxOf(rotated.width, rotated.height)
            scaled = if (currentLongest > MAX_PHOTO_DIMENSION) {
                val ratio = MAX_PHOTO_DIMENSION.toFloat() / currentLongest
                Bitmap.createScaledBitmap(
                    rotated,
                    (rotated.width * ratio).toInt().coerceAtLeast(1),
                    (rotated.height * ratio).toInt().coerceAtLeast(1),
                    true
                )
            } else rotated

            // 5. Escribir a un temporal y sustituir solo si el resultado es válido y más liviano.
            val temp = File(file.parentFile, "${file.name}.tmp")
            val written = temp.outputStream().use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
            if (!written || temp.length() == 0L) {
                temp.delete()
                return@withContext 0L
            }
            if (temp.length() >= originalSize && rotationDegrees == 0) {
                // No se gana nada y la orientación ya era correcta.
                temp.delete()
                return@withContext 0L
            }
            val newSize = temp.length()
            if (!temp.renameTo(file)) {
                // renameTo puede fallar si el destino existe en algunos sistemas de archivos.
                temp.copyTo(file, overwrite = true)
                temp.delete()
            }
            (originalSize - newSize).coerceAtLeast(0L)
        } catch (_: Exception) {
            0L
        } catch (_: OutOfMemoryError) {
            0L
        } finally {
            // rotated y scaled pueden ser la misma instancia que source: no reciclar dos veces.
            if (scaled != null && scaled !== rotated && scaled !== source) scaled.recycle()
            if (rotated != null && rotated !== source) rotated.recycle()
            source?.recycle()
        }
    }

    /** Grados de rotación declarados en el EXIF de la imagen (0 si no hay o no se puede leer). */
    private fun exifRotationDegrees(file: File): Int = try {
        when (ExifInterface(file.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } catch (_: Exception) {
        0
    }

    /** Mayor potencia de 2 que deja el lado mayor por encima del objetivo (evita perder detalle). */
    private fun calculateInSampleSize(longestSide: Int, target: Int): Int {
        var sampleSize = 1
        while (longestSide / (sampleSize * 2) >= target) {
            sampleSize *= 2
        }
        return sampleSize
    }

    /** Resultado de la optimización masiva de fotos existentes. */
    data class OptimizationReport(
        val totalPhotos: Int,
        val optimizedPhotos: Int,
        val bytesSaved: Long
    )

    /** Tamaño total que ocupan hoy las fotos almacenadas, en bytes. */
    suspend fun photoStorageSize(context: Context): Long = withContext(Dispatchers.IO) {
        photoFiles(context).sumOf { it.length() }
    }

    /**
     * Recomprime todas las fotos ya almacenadas. Proceso puntual para el material capturado
     * antes de que la compresión fuera automática.
     *
     * Es destructivo sobre los archivos locales (no sobre la base de datos): conviene exportar
     * un respaldo completo antes, que conserva los originales.
     *
     * @param onProgress se invoca con (procesadas, total) para poder mostrar avance.
     */
    suspend fun optimizeExistingPhotos(
        context: Context,
        onProgress: (processed: Int, total: Int) -> Unit = { _, _ -> }
    ): OptimizationReport = withContext(Dispatchers.IO) {
        val files = photoFiles(context)
        var optimized = 0
        var saved = 0L
        files.forEachIndexed { index, file ->
            val gain = compressPhotoInPlace(file)
            if (gain > 0) {
                optimized++
                saved += gain
            }
            onProgress(index + 1, files.size)
        }
        OptimizationReport(totalPhotos = files.size, optimizedPhotos = optimized, bytesSaved = saved)
    }

    private fun photoFiles(context: Context): List<File> {
        val dir = File(context.filesDir, PHOTOS_DIR)
        if (!dir.isDirectory) return emptyList()
        return dir.listFiles()?.filter { it.isFile && it.extension.lowercase() in setOf("jpg", "jpeg", "png") }
            ?: emptyList()
    }

    // ── Archivos adjuntos de órdenes ──────────────────────────────────

    private const val FILES_DIR = "work_order_files"

    /**
     * Copia un archivo adjunto desde una URI externa al almacenamiento interno.
     * Preserva la extensión original del archivo.
     */
    suspend fun copyFileToInternalStorage(context: Context, uri: Uri, prefix: String = "WO"): File? =
        withContext(Dispatchers.IO) {
        try {
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
