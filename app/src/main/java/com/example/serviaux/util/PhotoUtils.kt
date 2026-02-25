package com.example.serviaux.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PhotoUtils {

    private const val PHOTOS_DIR = "vehicle_photos"
    const val MAX_PHOTOS = 6

    fun createTempPhotoFile(context: Context, prefix: String = "VEH"): File {
        val dir = File(context.filesDir, PHOTOS_DIR).apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(dir, "${prefix}_${timestamp}_${System.nanoTime()}.jpg")
    }

    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun serializePaths(paths: List<String>): String? {
        return if (paths.isEmpty()) null else paths.joinToString(",")
    }

    fun parsePaths(photoPaths: String?): List<String> {
        if (photoPaths.isNullOrBlank()) return emptyList()
        return photoPaths.split(",").filter { it.isNotBlank() && File(it).exists() }
    }

    fun deletePhoto(path: String) {
        val file = File(path)
        if (file.exists()) file.delete()
    }
}
