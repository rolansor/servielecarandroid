/**
 * ShareUtils.kt - Utilidades para compartir archivos via Intent.
 *
 * Permite compartir reportes PDF generados por [PdfReportGenerator]
 * a través del sistema de compartir de Android (WhatsApp, email, etc.).
 * Utiliza FileProvider para generar URIs seguras.
 */
package com.example.serviaux.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object ShareUtils {

    /**
     * Abre el diálogo de compartir de Android con el archivo PDF adjunto.
     * @param context Contexto de la actividad o aplicación.
     * @param file Archivo PDF a compartir.
     */
    fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Orden de Trabajo - SERVIAUX")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir reporte"))
    }
}
