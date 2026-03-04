/**
 * DropboxHelper.kt - Singleton para integración con Dropbox.
 *
 * Encapsula autenticación OAuth2 PKCE, subida, descarga, listado y
 * eliminación de archivos de respaldo en Dropbox.
 *
 * Los respaldos se guardan en /Serviaux/{nombreDispositivo}/ con nombre basado
 * en la fecha del día, de modo que subir dos veces el mismo día sobrescribe
 * el archivo anterior.
 *
 * La credencial OAuth se almacena como JSON en SharedPreferences (dropbox_prefs).
 */
package com.example.serviaux.util

import android.content.Context
import android.os.Build
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.FolderMetadata
import com.dropbox.core.v2.files.ListFolderErrorException
import com.dropbox.core.v2.files.WriteMode
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Representa un archivo de respaldo almacenado en Dropbox. */
data class DropboxBackupEntry(
    val name: String,     // Nombre visible: "{dispositivo}/{archivo}"
    val path: String,     // Ruta completa en Dropbox (lowercase)
    val size: Long,       // Tamaño en bytes
    val modified: Long    // Fecha de modificación (epoch ms)
)

object DropboxHelper {
    private const val APP_KEY = "tmd7jeavfixk9g8"
    private const val DROPBOX_FOLDER = "/Serviaux"
    private const val PREFS_NAME = "dropbox_prefs"
    private const val KEY_CREDENTIAL = "dropbox_credential"

    /**
     * Nombre del dispositivo limpio para usar como subcarpeta.
     * Ej: "Samsung Galaxy S24" → "Samsung Galaxy S24"
     * Caracteres especiales se reemplazan con "_".
     */
    private val deviceName: String
        get() {
            val manufacturer = Build.MANUFACTURER.trim()
            val model = Build.MODEL.trim()
            // Quitar prefijo del fabricante si el modelo ya lo incluye
            val cleanModel = if (model.startsWith(manufacturer, ignoreCase = true)) {
                model.substring(manufacturer.length).trim()
            } else {
                model
            }
            return "$manufacturer $cleanModel".trim().replace(Regex("[^a-zA-Z0-9 _-]"), "_")
        }

    /** Carpeta remota del dispositivo actual: /Serviaux/{nombreDispositivo} */
    private val remoteFolder: String
        get() = "$DROPBOX_FOLDER/$deviceName"

    /** Inicia el flujo de autenticación OAuth2 PKCE. Abre el navegador del sistema. */
    fun startAuth(context: Context) {
        val scope: Collection<String?> = emptyList()
        Auth.startOAuth2PKCE(context, APP_KEY, DbxRequestConfig("serviaux-android"), scope)
    }

    /**
     * Procesa el resultado de la autenticación OAuth2.
     * Debe llamarse en onResume tras regresar del navegador.
     * @return true si se obtuvo y guardó la credencial exitosamente.
     */
    fun handleAuthResult(context: Context): Boolean {
        val credential = Auth.getDbxCredential() ?: return false
        saveCredential(context, credential)
        return true
    }

    /** Verifica si hay una credencial de Dropbox almacenada. */
    fun isLinked(context: Context): Boolean {
        return getCredential(context) != null
    }

    /** Elimina la credencial almacenada (desvincula la cuenta). */
    fun logout(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_CREDENTIAL)
            .apply()
    }

    /**
     * Sube un archivo ZIP a /Serviaux/{dispositivo}/serviaux_backup_YYYY-MM-dd.zip.
     * Si ya existe un respaldo del mismo día, se sobrescribe (WriteMode.OVERWRITE).
     * @return Result con la ruta remota del archivo subido.
     */
    fun uploadFile(context: Context, file: File): Result<String> {
        return try {
            val client = getClient(context)
                ?: return Result.failure(Exception("No vinculado a Dropbox"))
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val remotePath = "$remoteFolder/serviaux_backup_$dateStr.zip"
            FileInputStream(file).use { inputStream ->
                client.files()
                    .uploadBuilder(remotePath)
                    .withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(inputStream)
            }
            Result.success(remotePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lista todos los archivos .zip en las subcarpetas de /Serviaux/.
     * Recorre cada subcarpeta (dispositivo) y recopila los ZIPs encontrados.
     * @return Lista ordenada por fecha de modificación (más reciente primero).
     */
    fun listBackups(context: Context): Result<List<DropboxBackupEntry>> {
        return try {
            val client = getClient(context)
                ?: return Result.failure(Exception("No vinculado a Dropbox"))
            val entries = mutableListOf<DropboxBackupEntry>()

            // Listar subcarpetas de dispositivos dentro de /Serviaux/
            try {
                val folderResult = client.files().listFolder(DROPBOX_FOLDER)
                val folders = folderResult.entries.filterIsInstance<FolderMetadata>()

                for (folder in folders) {
                    val folderPath = folder.pathLower ?: continue
                    try {
                        // Listar ZIPs dentro de cada subcarpeta de dispositivo
                        val subResult = client.files().listFolder(folderPath)
                        subResult.entries
                            .filterIsInstance<FileMetadata>()
                            .filter { it.name.endsWith(".zip", ignoreCase = true) }
                            .forEach { meta ->
                                val filePath = meta.pathLower ?: return@forEach
                                entries.add(
                                    DropboxBackupEntry(
                                        name = "${folder.name}/${meta.name}",
                                        path = filePath,
                                        size = meta.size,
                                        modified = meta.serverModified.time
                                    )
                                )
                            }
                    } catch (_: Exception) {
                        // Subcarpeta inaccesible, se ignora
                    }
                }
            } catch (_: ListFolderErrorException) {
                // La carpeta /Serviaux/ aún no existe — lista vacía
            }

            entries.sortByDescending { it.modified }
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Descarga un archivo de Dropbox a un archivo local.
     * @param remotePath Ruta completa del archivo en Dropbox.
     * @param localFile Archivo local destino.
     */
    fun downloadFile(context: Context, remotePath: String, localFile: File): Result<File> {
        return try {
            val client = getClient(context)
                ?: return Result.failure(Exception("No vinculado a Dropbox"))
            FileOutputStream(localFile).use { outputStream ->
                client.files().download(remotePath).download(outputStream)
            }
            Result.success(localFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Elimina un archivo remoto de Dropbox. */
    fun deleteFile(context: Context, remotePath: String): Result<Unit> {
        return try {
            val client = getClient(context)
                ?: return Result.failure(Exception("No vinculado a Dropbox"))
            client.files().deleteV2(remotePath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Crea un cliente DbxClientV2 usando la credencial almacenada. */
    private fun getClient(context: Context): DbxClientV2? {
        val credential = getCredential(context) ?: return null
        val config = DbxRequestConfig("serviaux-android")
        return DbxClientV2(config, credential)
    }

    /** Serializa y guarda la credencial OAuth como JSON en SharedPreferences. */
    private fun saveCredential(context: Context, credential: DbxCredential) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CREDENTIAL, credential.toString())
            .apply()
    }

    /** Recupera la credencial OAuth desde SharedPreferences. */
    private fun getCredential(context: Context): DbxCredential? {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CREDENTIAL, null) ?: return null
        return try {
            DbxCredential.Reader.readFully(json)
        } catch (_: Exception) {
            null
        }
    }
}
