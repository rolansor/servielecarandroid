/**
 * BackupRepository.kt - Repositorio de respaldos (exportación e importación).
 *
 * Gestiona la creación de respaldos en formato ZIP que contienen:
 * - Un archivo `manifest.json` con metadatos (fecha, versión, categorías).
 * - Archivos JSON por cada tabla incluida en el respaldo.
 * - Fotos del directorio `vehicle_photos/` (si se incluyen órdenes).
 *
 * Soporta exportación selectiva por categoría, filtrado por año,
 * e importación parcial con selección de categorías a restaurar.
 * Los respaldos se guardan en `filesDir/backups/`.
 */
package com.example.serviaux.repository

import android.content.Context
import android.net.Uri
import com.example.serviaux.data.ServiauxDatabase
import com.example.serviaux.data.entity.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Categorías de datos para exportación/importación selectiva.
 * Cada categoría agrupa una o más tablas de la BD.
 */
enum class BackupCategory(val label: String) {
    USERS("Usuarios"),
    CLIENTS_VEHICLES("Clientes y Vehículos"),
    PRODUCTS("Productos"),
    WORK_ORDERS("Órdenes de Trabajo"),
    APPOINTMENTS("Turnos"),
    CATALOGS("Catálogos")
}

/**
 * Repositorio de respaldos ZIP con exportación/importación de datos.
 *
 * Accede directamente a los DAOs de [ServiauxDatabase] para serializar/deserializar
 * todas las entidades como JSON dentro de archivos ZIP.
 */
class BackupRepository(private val database: ServiauxDatabase) {

    companion object {
        private const val PHOTOS_DIR = "vehicle_photos"
        private const val BACKUPS_DIR = "backups"
        private const val MANIFEST_FILE = "manifest.json"
        private const val DB_VERSION = 2

        private val CATEGORY_TABLES = mapOf(
            BackupCategory.USERS to listOf("users"),
            BackupCategory.CLIENTS_VEHICLES to listOf("customers", "vehicles"),
            BackupCategory.PRODUCTS to listOf("parts"),
            BackupCategory.WORK_ORDERS to listOf("work_orders", "service_lines", "work_order_parts", "work_order_payments", "work_order_status_log", "work_order_mechanics"),
            BackupCategory.APPOINTMENTS to listOf("appointments"),
            BackupCategory.CATALOGS to listOf("catalog_brands", "catalog_models", "catalog_colors", "catalog_part_brands", "catalog_services", "catalog_vehicle_types", "catalog_accessories", "catalog_complaints", "catalog_diagnoses", "catalog_oil_types")
        )
    }

    data class BackupResult(
        val file: File? = null,
        val message: String = "",
        val success: Boolean = false,
        val counts: Map<String, Int> = emptyMap()
    )

    suspend fun exportToZip(context: Context, categories: Set<BackupCategory> = BackupCategory.entries.toSet()): BackupResult {
        return try {
            val backupDir = File(context.cacheDir, BACKUPS_DIR).apply { mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val zipFile = File(backupDir, "respaldo_serviaux_$timestamp.zip")

            val includedTables = categories.flatMap { CATEGORY_TABLES[it] ?: emptyList() }.toSet()

            // Fetch data only for selected categories
            val users = if ("users" in includedTables) database.userDao().getAllDirect() else emptyList()
            val customers = if ("customers" in includedTables) database.customerDao().getAllDirect() else emptyList()
            val vehicles = if ("vehicles" in includedTables) database.vehicleDao().getAllDirect() else emptyList()
            val parts = if ("parts" in includedTables) database.partDao().getAllDirect() else emptyList()
            val workOrders = if ("work_orders" in includedTables) database.workOrderDao().getAllDirect() else emptyList()
            val serviceLines = if ("service_lines" in includedTables) database.serviceLineDao().getAllDirect() else emptyList()
            val workOrderParts = if ("work_order_parts" in includedTables) database.workOrderPartDao().getAllDirect() else emptyList()
            val workOrderPayments = if ("work_order_payments" in includedTables) database.workOrderPaymentDao().getAllDirect() else emptyList()
            val statusLogs = if ("work_order_status_log" in includedTables) database.workOrderStatusLogDao().getAllDirect() else emptyList()
            val workOrderMechanics = if ("work_order_mechanics" in includedTables) database.workOrderMechanicDao().getAllDirect() else emptyList()
            val appointments = if ("appointments" in includedTables) database.appointmentDao().getAllDirect() else emptyList()
            val catalogBrands = if ("catalog_brands" in includedTables) database.catalogDao().getAllBrandsDirect() else emptyList()
            val catalogModels = if ("catalog_models" in includedTables) database.catalogDao().getAllModelsDirect() else emptyList()
            val catalogColors = if ("catalog_colors" in includedTables) database.catalogDao().getAllColorsDirect() else emptyList()
            val catalogPartBrands = if ("catalog_part_brands" in includedTables) database.catalogDao().getAllPartBrandsDirect() else emptyList()
            val catalogServices = if ("catalog_services" in includedTables) database.catalogDao().getAllServicesDirect() else emptyList()
            val catalogVehicleTypes = if ("catalog_vehicle_types" in includedTables) database.catalogDao().getAllVehicleTypesDirect() else emptyList()
            val catalogAccessories = if ("catalog_accessories" in includedTables) database.catalogDao().getAllAccessoriesDirect() else emptyList()
            val catalogComplaints = if ("catalog_complaints" in includedTables) database.catalogDao().getAllComplaintsDirect() else emptyList()
            val catalogDiagnoses = if ("catalog_diagnoses" in includedTables) database.catalogDao().getAllDiagnosesDirect() else emptyList()
            val catalogOilTypes = if ("catalog_oil_types" in includedTables) database.catalogDao().getAllOilTypesDirect() else emptyList()

            val counts = mutableMapOf<String, Int>()
            if ("users" in includedTables) counts["users"] = users.size
            if ("customers" in includedTables) counts["customers"] = customers.size
            if ("vehicles" in includedTables) counts["vehicles"] = vehicles.size
            if ("parts" in includedTables) counts["parts"] = parts.size
            if ("work_orders" in includedTables) counts["work_orders"] = workOrders.size
            if ("service_lines" in includedTables) counts["service_lines"] = serviceLines.size
            if ("work_order_parts" in includedTables) counts["work_order_parts"] = workOrderParts.size
            if ("work_order_payments" in includedTables) counts["work_order_payments"] = workOrderPayments.size
            if ("work_order_status_log" in includedTables) counts["work_order_status_log"] = statusLogs.size
            if ("work_order_mechanics" in includedTables) counts["work_order_mechanics"] = workOrderMechanics.size
            if ("appointments" in includedTables) counts["appointments"] = appointments.size
            if ("catalog_brands" in includedTables) counts["catalog_brands"] = catalogBrands.size
            if ("catalog_models" in includedTables) counts["catalog_models"] = catalogModels.size
            if ("catalog_colors" in includedTables) counts["catalog_colors"] = catalogColors.size
            if ("catalog_part_brands" in includedTables) counts["catalog_part_brands"] = catalogPartBrands.size
            if ("catalog_services" in includedTables) counts["catalog_services"] = catalogServices.size
            if ("catalog_vehicle_types" in includedTables) counts["catalog_vehicle_types"] = catalogVehicleTypes.size
            if ("catalog_accessories" in includedTables) counts["catalog_accessories"] = catalogAccessories.size
            if ("catalog_complaints" in includedTables) counts["catalog_complaints"] = catalogComplaints.size
            if ("catalog_diagnoses" in includedTables) counts["catalog_diagnoses"] = catalogDiagnoses.size
            if ("catalog_oil_types" in includedTables) counts["catalog_oil_types"] = catalogOilTypes.size

            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zip ->
                // Manifest with categories
                val categoriesArray = JSONArray()
                categories.forEach { categoriesArray.put(it.name) }
                val manifest = JSONObject().apply {
                    put("app", "serviaux")
                    put("dbVersion", DB_VERSION)
                    put("exportDate", System.currentTimeMillis())
                    put("exportDateFormatted", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
                    put("counts", JSONObject(counts.toMap()))
                    put("categories", categoriesArray)
                }
                writeZipEntry(zip, MANIFEST_FILE, manifest.toString(2))

                // Data files (only for included tables)
                if ("users" in includedTables) writeZipEntry(zip, "data/users.json", usersToJson(users))
                if ("customers" in includedTables) writeZipEntry(zip, "data/customers.json", customersToJson(customers))
                if ("vehicles" in includedTables) writeZipEntry(zip, "data/vehicles.json", vehiclesToJson(vehicles))
                if ("parts" in includedTables) writeZipEntry(zip, "data/parts.json", partsToJson(parts))
                if ("work_orders" in includedTables) writeZipEntry(zip, "data/work_orders.json", workOrdersToJson(workOrders))
                if ("service_lines" in includedTables) writeZipEntry(zip, "data/service_lines.json", serviceLinesToJson(serviceLines))
                if ("work_order_parts" in includedTables) writeZipEntry(zip, "data/work_order_parts.json", workOrderPartsToJson(workOrderParts))
                if ("work_order_payments" in includedTables) writeZipEntry(zip, "data/work_order_payments.json", workOrderPaymentsToJson(workOrderPayments))
                if ("work_order_status_log" in includedTables) writeZipEntry(zip, "data/work_order_status_log.json", statusLogsToJson(statusLogs))
                if ("work_order_mechanics" in includedTables) writeZipEntry(zip, "data/work_order_mechanics.json", workOrderMechanicsToJson(workOrderMechanics))
                if ("appointments" in includedTables) writeZipEntry(zip, "data/appointments.json", appointmentsToJson(appointments))
                if ("catalog_brands" in includedTables) writeZipEntry(zip, "data/catalog_brands.json", catalogBrandsToJson(catalogBrands))
                if ("catalog_models" in includedTables) writeZipEntry(zip, "data/catalog_models.json", catalogModelsToJson(catalogModels))
                if ("catalog_colors" in includedTables) writeZipEntry(zip, "data/catalog_colors.json", catalogColorsToJson(catalogColors))
                if ("catalog_part_brands" in includedTables) writeZipEntry(zip, "data/catalog_part_brands.json", catalogPartBrandsToJson(catalogPartBrands))
                if ("catalog_services" in includedTables) writeZipEntry(zip, "data/catalog_services.json", catalogServicesToJson(catalogServices))
                if ("catalog_vehicle_types" in includedTables) writeZipEntry(zip, "data/catalog_vehicle_types.json", catalogVehicleTypesToJson(catalogVehicleTypes))
                if ("catalog_accessories" in includedTables) writeZipEntry(zip, "data/catalog_accessories.json", catalogAccessoriesToJson(catalogAccessories))
                if ("catalog_complaints" in includedTables) writeZipEntry(zip, "data/catalog_complaints.json", catalogComplaintsToJson(catalogComplaints))
                if ("catalog_diagnoses" in includedTables) writeZipEntry(zip, "data/catalog_diagnoses.json", catalogDiagnosesToJson(catalogDiagnoses))
                if ("catalog_oil_types" in includedTables) writeZipEntry(zip, "data/catalog_oil_types.json", catalogOilTypesToJson(catalogOilTypes))

                // Photos (only if vehicles or work orders are included)
                val photosDir = File(context.filesDir, PHOTOS_DIR)
                if (photosDir.exists()) {
                    val allPhotoPaths = mutableSetOf<String>()
                    if ("vehicles" in includedTables) {
                        vehicles.forEach { v ->
                            v.photoPaths?.split(",")?.filter { it.isNotBlank() }?.forEach { allPhotoPaths.add(it) }
                        }
                    }
                    if ("work_orders" in includedTables) {
                        workOrders.forEach { wo ->
                            wo.photoPaths?.split(",")?.filter { it.isNotBlank() }?.forEach { allPhotoPaths.add(it) }
                        }
                    }
                    for (path in allPhotoPaths) {
                        val file = File(path)
                        if (file.exists()) {
                            zip.putNextEntry(ZipEntry("photos/${file.name}"))
                            file.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
            }

            BackupResult(
                file = zipFile,
                message = "Respaldo exportado exitosamente",
                success = true,
                counts = counts
            )
        } catch (e: Exception) {
            BackupResult(message = "Error al exportar: ${e.message}", success = false)
        }
    }

    suspend fun exportByYear(context: Context, year: Int): BackupResult {
        return try {
            val backupDir = File(context.cacheDir, BACKUPS_DIR).apply { mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val zipFile = File(backupDir, "respaldo_serviaux_${year}_$timestamp.zip")

            // Calculate year range in epoch ms
            val calStart = Calendar.getInstance(TimeZone.getDefault()).apply {
                set(year, Calendar.JANUARY, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val calEnd = Calendar.getInstance(TimeZone.getDefault()).apply {
                set(year, Calendar.DECEMBER, 31, 23, 59, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val fromMs = calStart.timeInMillis
            val toMs = calEnd.timeInMillis

            // Fetch year-filtered work orders
            val workOrders = database.workOrderDao().getByDateRangeDirect(fromMs, toMs)
            val orderIds = workOrders.map { it.id }.toSet()

            // Fetch related data for those orders
            val allServiceLines = database.serviceLineDao().getAllDirect()
            val serviceLines = allServiceLines.filter { it.workOrderId in orderIds }
            val allWorkOrderParts = database.workOrderPartDao().getAllDirect()
            val workOrderParts = allWorkOrderParts.filter { it.workOrderId in orderIds }
            val allPayments = database.workOrderPaymentDao().getAllDirect()
            val workOrderPayments = allPayments.filter { it.workOrderId in orderIds }
            val allStatusLogs = database.workOrderStatusLogDao().getAllDirect()
            val statusLogs = allStatusLogs.filter { it.workOrderId in orderIds }
            val allWorkOrderMechanics = database.workOrderMechanicDao().getAllDirect()
            val workOrderMechanics = allWorkOrderMechanics.filter { it.workOrderId in orderIds }

            // Fetch all master data (always full)
            val users = database.userDao().getAllDirect()
            val customers = database.customerDao().getAllDirect()
            val vehicles = database.vehicleDao().getAllDirect()
            val parts = database.partDao().getAllDirect()
            val catalogBrands = database.catalogDao().getAllBrandsDirect()
            val catalogModels = database.catalogDao().getAllModelsDirect()
            val catalogColors = database.catalogDao().getAllColorsDirect()
            val catalogPartBrands = database.catalogDao().getAllPartBrandsDirect()
            val catalogServices = database.catalogDao().getAllServicesDirect()
            val catalogVehicleTypes = database.catalogDao().getAllVehicleTypesDirect()
            val catalogAccessories = database.catalogDao().getAllAccessoriesDirect()
            val catalogComplaints = database.catalogDao().getAllComplaintsDirect()
            val catalogDiagnoses = database.catalogDao().getAllDiagnosesDirect()
            val catalogOilTypes = database.catalogDao().getAllOilTypesDirect()
            val appointments = database.appointmentDao().getAllDirect()

            val counts = mapOf(
                "users" to users.size,
                "customers" to customers.size,
                "vehicles" to vehicles.size,
                "parts" to parts.size,
                "work_orders" to workOrders.size,
                "service_lines" to serviceLines.size,
                "work_order_parts" to workOrderParts.size,
                "work_order_payments" to workOrderPayments.size,
                "work_order_status_log" to statusLogs.size,
                "work_order_mechanics" to workOrderMechanics.size,
                "appointments" to appointments.size
            )

            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zip ->
                val manifest = JSONObject().apply {
                    put("app", "serviaux")
                    put("dbVersion", DB_VERSION)
                    put("exportType", "yearly")
                    put("year", year)
                    put("exportDate", System.currentTimeMillis())
                    put("exportDateFormatted", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
                    put("counts", JSONObject(counts))
                }
                writeZipEntry(zip, MANIFEST_FILE, manifest.toString(2))

                writeZipEntry(zip, "data/users.json", usersToJson(users))
                writeZipEntry(zip, "data/customers.json", customersToJson(customers))
                writeZipEntry(zip, "data/vehicles.json", vehiclesToJson(vehicles))
                writeZipEntry(zip, "data/parts.json", partsToJson(parts))
                writeZipEntry(zip, "data/work_orders.json", workOrdersToJson(workOrders))
                writeZipEntry(zip, "data/service_lines.json", serviceLinesToJson(serviceLines))
                writeZipEntry(zip, "data/work_order_parts.json", workOrderPartsToJson(workOrderParts))
                writeZipEntry(zip, "data/work_order_payments.json", workOrderPaymentsToJson(workOrderPayments))
                writeZipEntry(zip, "data/work_order_status_log.json", statusLogsToJson(statusLogs))
                writeZipEntry(zip, "data/work_order_mechanics.json", workOrderMechanicsToJson(workOrderMechanics))
                writeZipEntry(zip, "data/appointments.json", appointmentsToJson(appointments))
                writeZipEntry(zip, "data/catalog_brands.json", catalogBrandsToJson(catalogBrands))
                writeZipEntry(zip, "data/catalog_models.json", catalogModelsToJson(catalogModels))
                writeZipEntry(zip, "data/catalog_colors.json", catalogColorsToJson(catalogColors))
                writeZipEntry(zip, "data/catalog_part_brands.json", catalogPartBrandsToJson(catalogPartBrands))
                writeZipEntry(zip, "data/catalog_services.json", catalogServicesToJson(catalogServices))
                writeZipEntry(zip, "data/catalog_vehicle_types.json", catalogVehicleTypesToJson(catalogVehicleTypes))
                writeZipEntry(zip, "data/catalog_accessories.json", catalogAccessoriesToJson(catalogAccessories))
                writeZipEntry(zip, "data/catalog_complaints.json", catalogComplaintsToJson(catalogComplaints))
                writeZipEntry(zip, "data/catalog_diagnoses.json", catalogDiagnosesToJson(catalogDiagnoses))
                writeZipEntry(zip, "data/catalog_oil_types.json", catalogOilTypesToJson(catalogOilTypes))

                // Photos only for orders in this year
                val photosDir = File(context.filesDir, PHOTOS_DIR)
                if (photosDir.exists()) {
                    val allPhotoPaths = mutableSetOf<String>()
                    workOrders.forEach { wo ->
                        wo.photoPaths?.split(",")?.filter { it.isNotBlank() }?.forEach { allPhotoPaths.add(it) }
                    }
                    // Include vehicle photos for vehicles referenced by orders
                    val vehicleIds = workOrders.map { it.vehicleId }.toSet()
                    vehicles.filter { it.id in vehicleIds }.forEach { v ->
                        v.photoPaths?.split(",")?.filter { it.isNotBlank() }?.forEach { allPhotoPaths.add(it) }
                    }
                    for (path in allPhotoPaths) {
                        val file = File(path)
                        if (file.exists()) {
                            zip.putNextEntry(ZipEntry("photos/${file.name}"))
                            file.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
            }

            BackupResult(
                file = zipFile,
                message = "Respaldo del año $year exportado exitosamente (${workOrders.size} órdenes)",
                success = true,
                counts = counts
            )
        } catch (e: Exception) {
            BackupResult(message = "Error al exportar: ${e.message}", success = false)
        }
    }

    suspend fun getAvailableYears(): List<Int> {
        val orders = database.workOrderDao().getAllDirect()
        if (orders.isEmpty()) return listOf(Calendar.getInstance().get(Calendar.YEAR))
        val cal = Calendar.getInstance()
        return orders.map { order ->
            cal.timeInMillis = order.entryDate
            cal.get(Calendar.YEAR)
        }.distinct().sorted().reversed()
    }

    suspend fun getBackupContents(context: Context, zipUri: Uri): Map<BackupCategory, Int> {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(zipUri)
                ?: return emptyMap()

            val entries = mutableMapOf<String, ByteArray>()
            ZipInputStream(inputStream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        entries[entry.name] = zip.readBytes()
                    }
                    entry = zip.nextEntry
                }
            }

            val manifestBytes = entries[MANIFEST_FILE] ?: return emptyMap()
            val manifest = JSONObject(String(manifestBytes))
            if (manifest.optString("app", "") != "serviaux") return emptyMap()

            val counts = manifest.optJSONObject("counts") ?: JSONObject()
            val result = mutableMapOf<BackupCategory, Int>()

            for (category in BackupCategory.entries) {
                val tables = CATEGORY_TABLES[category] ?: continue
                val totalRecords = tables.sumOf { counts.optInt(it, 0) }
                // Check if the backup has data files for this category
                val hasData = tables.any { entries.containsKey("data/$it.json") }
                if (hasData) {
                    result[category] = totalRecords
                }
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    suspend fun importFromZip(context: Context, zipUri: Uri, categories: Set<BackupCategory> = BackupCategory.entries.toSet()): BackupResult {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(zipUri)
                ?: return BackupResult(message = "No se pudo abrir el archivo", success = false)

            // Read ZIP into memory-mapped entries
            val entries = mutableMapOf<String, ByteArray>()
            ZipInputStream(inputStream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        entries[entry.name] = zip.readBytes()
                    }
                    entry = zip.nextEntry
                }
            }

            // Validate manifest
            val manifestBytes = entries[MANIFEST_FILE]
                ?: return BackupResult(message = "Archivo de respaldo inválido: falta manifest.json", success = false)
            val manifest = JSONObject(String(manifestBytes))
            val app = manifest.optString("app", "")
            if (app != "serviaux") {
                return BackupResult(message = "El archivo no es un respaldo de Serviaux", success = false)
            }
            // Se acepta cualquier versión de respaldo; los deserializadores
            // manejan campos faltantes con valores por defecto.

            val includedTables = categories.flatMap { CATEGORY_TABLES[it] ?: emptyList() }.toSet()

            val userDao = database.userDao()
            val customerDao = database.customerDao()
            val vehicleDao = database.vehicleDao()
            val partDao = database.partDao()
            val catalogDao = database.catalogDao()
            val workOrderDao = database.workOrderDao()
            val serviceLineDao = database.serviceLineDao()
            val workOrderPartDao = database.workOrderPartDao()
            val workOrderPaymentDao = database.workOrderPaymentDao()
            val statusLogDao = database.workOrderStatusLogDao()
            val workOrderMechanicDao = database.workOrderMechanicDao()

            // Delete data for selected categories (in reverse FK order)
            if (BackupCategory.WORK_ORDERS in categories) {
                workOrderMechanicDao.deleteAll()
                statusLogDao.deleteAll()
                workOrderPaymentDao.deleteAll()
                workOrderPartDao.deleteAll()
                serviceLineDao.deleteAll()
                workOrderDao.deleteAll()
            }
            if (BackupCategory.PRODUCTS in categories) {
                partDao.deleteAll()
            }
            if (BackupCategory.CLIENTS_VEHICLES in categories) {
                vehicleDao.deleteAll()
                customerDao.deleteAll()
            }
            if (BackupCategory.USERS in categories) {
                userDao.deleteAll()
            }
            if (BackupCategory.CATALOGS in categories) {
                catalogDao.deleteAllDiagnoses()
                catalogDao.deleteAllComplaints()
                catalogDao.deleteAllOilTypes()
                catalogDao.deleteAllAccessories()
                catalogDao.deleteAllVehicleTypes()
                catalogDao.deleteAllServices()
                catalogDao.deleteAllPartBrands()
                catalogDao.deleteAllColors()
                catalogDao.deleteAllModels()
                catalogDao.deleteAllBrands()
            }

            val counts = mutableMapOf<String, Int>()

            // Import in FK order, only for selected categories

            // Users
            if ("users" in includedTables) {
                entries["data/users.json"]?.let { bytes ->
                    val items = jsonToUsers(String(bytes))
                    items.forEach { userDao.insert(it) }
                    counts["users"] = items.size
                }
            }

            // Customers
            if ("customers" in includedTables) {
                entries["data/customers.json"]?.let { bytes ->
                    val items = jsonToCustomers(String(bytes))
                    items.forEach { customerDao.insert(it) }
                    counts["customers"] = items.size
                }
            }

            // Vehicles
            if ("vehicles" in includedTables) {
                entries["data/vehicles.json"]?.let { bytes ->
                    val items = jsonToVehicles(String(bytes))
                    items.forEach { vehicleDao.insert(it) }
                    counts["vehicles"] = items.size
                }
            }

            // Parts
            if ("parts" in includedTables) {
                entries["data/parts.json"]?.let { bytes ->
                    val items = jsonToParts(String(bytes))
                    items.forEach { partDao.insert(it) }
                    counts["parts"] = items.size
                }
            }

            // Catalogs
            if ("catalog_brands" in includedTables) {
                entries["data/catalog_brands.json"]?.let { bytes ->
                    val items = jsonToCatalogBrands(String(bytes))
                    items.forEach { catalogDao.insertBrand(it) }
                    counts["catalog_brands"] = items.size
                }
            }
            if ("catalog_models" in includedTables) {
                entries["data/catalog_models.json"]?.let { bytes ->
                    val items = jsonToCatalogModels(String(bytes))
                    items.forEach { catalogDao.insertModel(it) }
                    counts["catalog_models"] = items.size
                }
            }
            if ("catalog_colors" in includedTables) {
                entries["data/catalog_colors.json"]?.let { bytes ->
                    val items = jsonToCatalogColors(String(bytes))
                    items.forEach { catalogDao.insertColor(it) }
                    counts["catalog_colors"] = items.size
                }
            }
            if ("catalog_part_brands" in includedTables) {
                entries["data/catalog_part_brands.json"]?.let { bytes ->
                    val items = jsonToCatalogPartBrands(String(bytes))
                    items.forEach { catalogDao.insertPartBrand(it) }
                    counts["catalog_part_brands"] = items.size
                }
            }
            if ("catalog_services" in includedTables) {
                entries["data/catalog_services.json"]?.let { bytes ->
                    val items = jsonToCatalogServices(String(bytes))
                    items.forEach { catalogDao.insertService(it) }
                    counts["catalog_services"] = items.size
                }
            }
            if ("catalog_vehicle_types" in includedTables) {
                entries["data/catalog_vehicle_types.json"]?.let { bytes ->
                    val items = jsonToCatalogVehicleTypes(String(bytes))
                    items.forEach { catalogDao.insertVehicleType(it) }
                    counts["catalog_vehicle_types"] = items.size
                }
            }
            if ("catalog_accessories" in includedTables) {
                entries["data/catalog_accessories.json"]?.let { bytes ->
                    val items = jsonToCatalogAccessories(String(bytes))
                    items.forEach { catalogDao.insertAccessory(it) }
                    counts["catalog_accessories"] = items.size
                }
            }
            if ("catalog_complaints" in includedTables) {
                entries["data/catalog_complaints.json"]?.let { bytes ->
                    val items = jsonToCatalogComplaints(String(bytes))
                    items.forEach { catalogDao.insertComplaint(it) }
                    counts["catalog_complaints"] = items.size
                }
            }
            if ("catalog_diagnoses" in includedTables) {
                entries["data/catalog_diagnoses.json"]?.let { bytes ->
                    val items = jsonToCatalogDiagnoses(String(bytes))
                    items.forEach { catalogDao.insertDiagnosis(it) }
                    counts["catalog_diagnoses"] = items.size
                }
            }
            if ("catalog_oil_types" in includedTables) {
                entries["data/catalog_oil_types.json"]?.let { bytes ->
                    val items = jsonToCatalogOilTypes(String(bytes))
                    items.forEach { catalogDao.insertOilType(it) }
                    counts["catalog_oil_types"] = items.size
                }
            }

            // Work Orders
            if ("work_orders" in includedTables) {
                entries["data/work_orders.json"]?.let { bytes ->
                    val items = jsonToWorkOrders(String(bytes))
                    items.forEach { workOrderDao.insert(it) }
                    counts["work_orders"] = items.size
                }
            }
            if ("service_lines" in includedTables) {
                entries["data/service_lines.json"]?.let { bytes ->
                    val items = jsonToServiceLines(String(bytes))
                    items.forEach { serviceLineDao.insert(it) }
                    counts["service_lines"] = items.size
                }
            }
            if ("work_order_parts" in includedTables) {
                entries["data/work_order_parts.json"]?.let { bytes ->
                    val items = jsonToWorkOrderParts(String(bytes))
                    items.forEach { workOrderPartDao.insert(it) }
                    counts["work_order_parts"] = items.size
                }
            }
            if ("work_order_payments" in includedTables) {
                entries["data/work_order_payments.json"]?.let { bytes ->
                    val items = jsonToWorkOrderPayments(String(bytes))
                    items.forEach { workOrderPaymentDao.insert(it) }
                    counts["work_order_payments"] = items.size
                }
            }
            if ("work_order_status_log" in includedTables) {
                entries["data/work_order_status_log.json"]?.let { bytes ->
                    val items = jsonToStatusLogs(String(bytes))
                    items.forEach { statusLogDao.insert(it) }
                    counts["work_order_status_log"] = items.size
                }
            }
            if ("work_order_mechanics" in includedTables) {
                entries["data/work_order_mechanics.json"]?.let { bytes ->
                    val items = jsonToWorkOrderMechanics(String(bytes))
                    items.forEach { workOrderMechanicDao.insert(it) }
                    counts["work_order_mechanics"] = items.size
                }
            }
            if ("appointments" in includedTables) {
                entries["data/appointments.json"]?.let { bytes ->
                    val items = jsonToAppointments(String(bytes))
                    database.appointmentDao().deleteAll()
                    items.forEach { database.appointmentDao().insert(it) }
                    counts["appointments"] = items.size
                }
            }

            // Restore photos (if vehicles or work orders are in scope)
            if (BackupCategory.CLIENTS_VEHICLES in categories || BackupCategory.WORK_ORDERS in categories) {
                val photosDir = File(context.filesDir, PHOTOS_DIR).apply { mkdirs() }
                var photoCount = 0
                entries.filter { it.key.startsWith("photos/") }.forEach { (name, bytes) ->
                    val fileName = name.removePrefix("photos/")
                    if (fileName.isNotBlank()) {
                        val destFile = File(photosDir, fileName)
                        destFile.writeBytes(bytes)
                        photoCount++
                    }
                }

                if (photoCount > 0) {
                    val photoDirPath = photosDir.absolutePath
                    if (BackupCategory.CLIENTS_VEHICLES in categories) {
                        val importedVehicles = vehicleDao.getAllDirect()
                        importedVehicles.filter { !it.photoPaths.isNullOrBlank() }.forEach { vehicle ->
                            val updatedPaths = vehicle.photoPaths!!.split(",").map { path ->
                                val fn = File(path).name
                                "$photoDirPath/$fn"
                            }.joinToString(",")
                            vehicleDao.update(vehicle.copy(photoPaths = updatedPaths))
                        }
                    }
                    if (BackupCategory.WORK_ORDERS in categories) {
                        val importedOrders = workOrderDao.getAllDirect()
                        importedOrders.filter { !it.photoPaths.isNullOrBlank() }.forEach { order ->
                            val updatedPaths = order.photoPaths!!.split(",").map { path ->
                                val fn = File(path).name
                                "$photoDirPath/$fn"
                            }.joinToString(",")
                            workOrderDao.update(order.copy(photoPaths = updatedPaths))
                        }
                    }
                }
            }

            BackupResult(
                message = "Respaldo restaurado exitosamente",
                success = true,
                counts = counts
            )
        } catch (e: Exception) {
            BackupResult(message = "Error al restaurar: ${e.message}", success = false)
        }
    }

    suspend fun getRecordCounts(): Map<String, Int> {
        return mapOf(
            "Usuarios" to database.userDao().getAllDirect().size,
            "Clientes" to database.customerDao().getAllDirect().size,
            "Veh\u00edculos" to database.vehicleDao().getAllDirect().size,
            "Repuestos" to database.partDao().getAllDirect().size,
            "\u00d3rdenes" to database.workOrderDao().getAllDirect().size,
            "Servicios" to database.serviceLineDao().getAllDirect().size,
            "Pagos" to database.workOrderPaymentDao().getAllDirect().size,
            "Turnos" to database.appointmentDao().getAllDirect().size
        )
    }

    // ── JSON Serialization ──────────────────────────────────────

    private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray())
        zip.closeEntry()
    }

    private fun usersToJson(users: List<User>): String {
        val arr = JSONArray()
        users.forEach { u ->
            arr.put(JSONObject().apply {
                put("id", u.id)
                put("name", u.name)
                put("username", u.username)
                put("role", u.role.name)
                put("passwordHash", u.passwordHash)
                put("commissionType", u.commissionType)
                put("commissionValue", u.commissionValue)
                put("active", u.active)
                put("createdAt", u.createdAt)
                put("updatedAt", u.updatedAt)
            })
        }
        return arr.toString(2)
    }

    private fun customersToJson(customers: List<Customer>): String {
        val arr = JSONArray()
        customers.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("fullName", c.fullName)
                put("docType", c.docType)
                put("idNumber", c.idNumber ?: JSONObject.NULL)
                put("phone", c.phone ?: JSONObject.NULL)
                put("email", c.email ?: JSONObject.NULL)
                put("address", c.address ?: JSONObject.NULL)
                put("notes", c.notes ?: JSONObject.NULL)
                put("createdAt", c.createdAt)
                put("updatedAt", c.updatedAt)
            })
        }
        return arr.toString(2)
    }

    private fun vehiclesToJson(vehicles: List<Vehicle>): String {
        val arr = JSONArray()
        vehicles.forEach { v ->
            arr.put(JSONObject().apply {
                put("id", v.id)
                put("customerId", v.customerId)
                put("plate", v.plate)
                put("brand", v.brand)
                put("model", v.model)
                put("version", v.version ?: JSONObject.NULL)
                put("year", v.year ?: JSONObject.NULL)
                put("vin", v.vin ?: JSONObject.NULL)
                put("color", v.color ?: JSONObject.NULL)
                put("vehicleType", v.vehicleType ?: JSONObject.NULL)
                put("fuelType", v.fuelType ?: JSONObject.NULL)
                put("oilType", v.oilType ?: JSONObject.NULL)
                put("oilCapacity", v.oilCapacity ?: JSONObject.NULL)
                put("currentMileage", v.currentMileage ?: JSONObject.NULL)
                put("engineDisplacement", v.engineDisplacement ?: JSONObject.NULL)
                put("engineNumber", v.engineNumber ?: JSONObject.NULL)
                put("drivetrain", v.drivetrain)
                put("transmission", v.transmission)
                put("notes", v.notes ?: JSONObject.NULL)
                put("registrationPhotoPaths", v.registrationPhotoPaths ?: JSONObject.NULL)
                put("photoPaths", v.photoPaths ?: JSONObject.NULL)
                put("createdAt", v.createdAt)
                put("updatedAt", v.updatedAt)
            })
        }
        return arr.toString(2)
    }

    private fun partsToJson(parts: List<Part>): String {
        val arr = JSONArray()
        parts.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("description", p.description ?: JSONObject.NULL)
                put("code", p.code ?: JSONObject.NULL)
                put("brand", p.brand ?: JSONObject.NULL)
                put("unitCost", p.unitCost)
                put("salePrice", p.salePrice ?: JSONObject.NULL)
                put("currentStock", p.currentStock)
                put("active", p.active)
                put("createdAt", p.createdAt)
                put("updatedAt", p.updatedAt)
            })
        }
        return arr.toString(2)
    }

    private fun workOrdersToJson(orders: List<WorkOrder>): String {
        val arr = JSONArray()
        orders.forEach { o ->
            arr.put(JSONObject().apply {
                put("id", o.id)
                put("vehicleId", o.vehicleId)
                put("customerId", o.customerId)
                put("entryDate", o.entryDate)
                put("admissionDate", o.admissionDate ?: JSONObject.NULL)
                put("status", o.status.name)
                put("priority", o.priority.name)
                put("orderType", o.orderType.name)
                put("customerComplaint", o.customerComplaint)
                put("initialDiagnosis", o.initialDiagnosis ?: JSONObject.NULL)
                put("arrivalCondition", o.arrivalCondition.name)
                put("assignedMechanicId", o.assignedMechanicId ?: JSONObject.NULL)
                put("entryMileage", o.entryMileage ?: JSONObject.NULL)
                put("fuelLevel", o.fuelLevel ?: JSONObject.NULL)
                put("checklistNotes", o.checklistNotes ?: JSONObject.NULL)
                put("totalLabor", o.totalLabor)
                put("totalParts", o.totalParts)
                put("total", o.total)
                put("photoPaths", o.photoPaths ?: JSONObject.NULL)
                put("filePaths", o.filePaths ?: JSONObject.NULL)
                put("deliveryNote", o.deliveryNote ?: JSONObject.NULL)
                put("invoiceNumber", o.invoiceNumber ?: JSONObject.NULL)
                put("notes", o.notes ?: JSONObject.NULL)
                put("createdBy", o.createdBy)
                put("updatedBy", o.updatedBy)
                put("createdAt", o.createdAt)
                put("updatedAt", o.updatedAt)
            })
        }
        return arr.toString(2)
    }

    private fun serviceLinesToJson(lines: List<ServiceLine>): String {
        val arr = JSONArray()
        lines.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("workOrderId", s.workOrderId)
                put("description", s.description)
                put("laborCost", s.laborCost)
                put("discount", s.discount)
                put("notes", s.notes ?: JSONObject.NULL)
                put("createdAt", s.createdAt)
                put("updatedAt", s.updatedAt)
            })
        }
        return arr.toString(2)
    }

    private fun workOrderPartsToJson(parts: List<WorkOrderPart>): String {
        val arr = JSONArray()
        parts.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("workOrderId", p.workOrderId)
                put("partId", p.partId)
                put("quantity", p.quantity)
                put("appliedUnitPrice", p.appliedUnitPrice)
                put("subtotal", p.subtotal)
                put("discount", p.discount)
                put("createdAt", p.createdAt)
                put("updatedAt", p.updatedAt)
            })
        }
        return arr.toString(2)
    }

    private fun workOrderPaymentsToJson(payments: List<WorkOrderPayment>): String {
        val arr = JSONArray()
        payments.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("workOrderId", p.workOrderId)
                put("amount", p.amount)
                put("discount", p.discount)
                put("method", p.method.name)
                put("date", p.date)
                put("notes", p.notes ?: JSONObject.NULL)
                put("createdAt", p.createdAt)
                put("updatedAt", p.updatedAt)
            })
        }
        return arr.toString(2)
    }

    private fun statusLogsToJson(logs: List<WorkOrderStatusLog>): String {
        val arr = JSONArray()
        logs.forEach { l ->
            arr.put(JSONObject().apply {
                put("id", l.id)
                put("workOrderId", l.workOrderId)
                put("oldStatus", l.oldStatus?.name ?: JSONObject.NULL)
                put("newStatus", l.newStatus.name)
                put("changedByUserId", l.changedByUserId)
                put("changedAt", l.changedAt)
                put("note", l.note ?: JSONObject.NULL)
            })
        }
        return arr.toString(2)
    }

    private fun workOrderMechanicsToJson(mechanics: List<WorkOrderMechanic>): String {
        val arr = JSONArray()
        mechanics.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id)
                put("workOrderId", m.workOrderId)
                put("mechanicId", m.mechanicId)
                put("commissionType", m.commissionType)
                put("commissionValue", m.commissionValue)
                put("commissionAmount", m.commissionAmount)
                put("commissionPaid", m.commissionPaid)
                put("paidAt", m.paidAt ?: JSONObject.NULL)
                put("createdAt", m.createdAt)
            })
        }
        return arr.toString(2)
    }

    private fun appointmentsToJson(appointments: List<Appointment>): String {
        val arr = JSONArray()
        appointments.forEach { a ->
            arr.put(JSONObject().apply {
                put("id", a.id)
                put("customerId", a.customerId)
                put("vehicleId", a.vehicleId)
                put("scheduledDate", a.scheduledDate)
                put("notes", a.notes ?: JSONObject.NULL)
                put("status", a.status.name)
                put("workOrderId", a.workOrderId ?: JSONObject.NULL)
                put("createdBy", a.createdBy)
                put("createdAt", a.createdAt)
                put("updatedAt", a.updatedAt)
            })
        }
        return arr.toString(2)
    }

    private fun jsonToAppointments(json: String): List<Appointment> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Appointment(
                id = o.getLong("id"),
                customerId = o.getLong("customerId"),
                vehicleId = o.getLong("vehicleId"),
                scheduledDate = o.getLong("scheduledDate"),
                notes = if (o.isNull("notes")) null else o.getString("notes"),
                status = AppointmentStatus.valueOf(o.getString("status")),
                workOrderId = if (o.isNull("workOrderId")) null else o.getLong("workOrderId"),
                createdBy = o.getLong("createdBy"),
                createdAt = o.getLong("createdAt"),
                updatedAt = o.getLong("updatedAt")
            )
        }
    }

    private fun catalogBrandsToJson(brands: List<CatalogBrand>): String {
        val arr = JSONArray()
        brands.forEach { b ->
            arr.put(JSONObject().apply {
                put("id", b.id)
                put("name", b.name)
            })
        }
        return arr.toString(2)
    }

    private fun catalogModelsToJson(models: List<CatalogModel>): String {
        val arr = JSONArray()
        models.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id)
                put("brandId", m.brandId)
                put("name", m.name)
            })
        }
        return arr.toString(2)
    }

    private fun catalogColorsToJson(colors: List<CatalogColor>): String {
        val arr = JSONArray()
        colors.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
            })
        }
        return arr.toString(2)
    }

    private fun catalogPartBrandsToJson(brands: List<CatalogPartBrand>): String {
        val arr = JSONArray()
        brands.forEach { b ->
            arr.put(JSONObject().apply {
                put("id", b.id)
                put("name", b.name)
            })
        }
        return arr.toString(2)
    }

    private fun catalogServicesToJson(services: List<CatalogService>): String {
        val arr = JSONArray()
        services.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("category", s.category)
                put("name", s.name)
                put("defaultPrice", s.defaultPrice)
                put("vehicleType", s.vehicleType ?: JSONObject.NULL)
            })
        }
        return arr.toString(2)
    }

    private fun catalogVehicleTypesToJson(types: List<CatalogVehicleType>): String {
        val arr = JSONArray()
        types.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id)
                put("name", t.name)
            })
        }
        return arr.toString(2)
    }

    private fun catalogAccessoriesToJson(accessories: List<CatalogAccessory>): String {
        val arr = JSONArray()
        accessories.forEach { a ->
            arr.put(JSONObject().apply {
                put("id", a.id)
                put("name", a.name)
            })
        }
        return arr.toString(2)
    }

    private fun catalogOilTypesToJson(oilTypes: List<CatalogOilType>): String {
        val arr = JSONArray()
        oilTypes.forEach { o ->
            arr.put(JSONObject().apply {
                put("id", o.id)
                put("name", o.name)
            })
        }
        return arr.toString(2)
    }

    private fun catalogComplaintsToJson(complaints: List<CatalogComplaint>): String {
        val arr = JSONArray()
        complaints.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
            })
        }
        return arr.toString(2)
    }

    private fun catalogDiagnosesToJson(diagnoses: List<CatalogDiagnosis>): String {
        val arr = JSONArray()
        diagnoses.forEach { d ->
            arr.put(JSONObject().apply {
                put("id", d.id)
                put("complaintId", d.complaintId)
                put("name", d.name)
            })
        }
        return arr.toString(2)
    }

    // ── JSON Deserialization ──────────────────────────────────────

    private fun jsonToUsers(json: String): List<User> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            User(
                id = o.getLong("id"),
                name = o.getString("name"),
                username = o.getString("username"),
                role = UserRole.valueOf(o.getString("role")),
                passwordHash = o.getString("passwordHash"),
                commissionType = o.optString("commissionType", "NINGUNA"),
                commissionValue = o.optDouble("commissionValue", 0.0),
                active = o.optBoolean("active", true),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
            )
        }
    }

    private fun jsonToCustomers(json: String): List<Customer> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Customer(
                id = o.getLong("id"),
                fullName = o.getString("fullName"),
                docType = o.optString("docType", "CEDULA"),
                idNumber = o.optStringOrNull("idNumber"),
                phone = o.optStringOrNull("phone"),
                email = o.optStringOrNull("email"),
                address = o.optStringOrNull("address"),
                notes = o.optStringOrNull("notes"),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
            )
        }
    }

    private fun jsonToVehicles(json: String): List<Vehicle> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Vehicle(
                id = o.getLong("id"),
                customerId = o.getLong("customerId"),
                plate = o.getString("plate"),
                brand = o.getString("brand"),
                model = o.getString("model"),
                version = o.optStringOrNull("version"),
                year = o.optIntOrNull("year"),
                vin = o.optStringOrNull("vin"),
                color = o.optStringOrNull("color"),
                vehicleType = o.optStringOrNull("vehicleType"),
                fuelType = o.optStringOrNull("fuelType"),
                oilType = o.optStringOrNull("oilType"),
                oilCapacity = o.optStringOrNull("oilCapacity"),
                currentMileage = o.optIntOrNull("currentMileage"),
                engineDisplacement = o.optStringOrNull("engineDisplacement"),
                engineNumber = o.optStringOrNull("engineNumber"),
                drivetrain = o.optString("drivetrain", "4x2"),
                transmission = o.optString("transmission", "Manual"),
                notes = o.optStringOrNull("notes"),
                registrationPhotoPaths = o.optStringOrNull("registrationPhotoPaths"),
                photoPaths = o.optStringOrNull("photoPaths"),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
            )
        }
    }

    private fun jsonToParts(json: String): List<Part> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Part(
                id = o.getLong("id"),
                name = o.getString("name"),
                description = o.optStringOrNull("description"),
                code = o.optStringOrNull("code"),
                brand = o.optStringOrNull("brand"),
                unitCost = o.getDouble("unitCost"),
                salePrice = o.optDoubleOrNull("salePrice"),
                currentStock = o.optInt("currentStock", 0),
                active = o.optBoolean("active", true),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
            )
        }
    }

    private fun jsonToWorkOrders(json: String): List<WorkOrder> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            WorkOrder(
                id = o.getLong("id"),
                vehicleId = o.getLong("vehicleId"),
                customerId = o.getLong("customerId"),
                entryDate = o.optLong("entryDate", System.currentTimeMillis()),
                admissionDate = if (o.has("admissionDate") && !o.isNull("admissionDate")) o.getLong("admissionDate") else null,
                status = OrderStatus.valueOf(o.getString("status")),
                priority = Priority.valueOf(o.getString("priority")),
                orderType = try { OrderType.valueOf(o.getString("orderType")) } catch (_: Exception) { OrderType.SERVICIO_NUEVO },
                customerComplaint = o.getString("customerComplaint"),
                initialDiagnosis = o.optStringOrNull("initialDiagnosis"),
                arrivalCondition = try { ArrivalCondition.valueOf(o.getString("arrivalCondition")) } catch (_: Exception) { ArrivalCondition.RODANDO },
                assignedMechanicId = o.optLongOrNull("assignedMechanicId"),
                entryMileage = o.optIntOrNull("entryMileage"),
                fuelLevel = o.optStringOrNull("fuelLevel"),
                checklistNotes = o.optStringOrNull("checklistNotes"),
                totalLabor = o.optDouble("totalLabor", 0.0),
                totalParts = o.optDouble("totalParts", 0.0),
                total = o.optDouble("total", 0.0),
                photoPaths = o.optStringOrNull("photoPaths"),
                filePaths = o.optStringOrNull("filePaths"),
                deliveryNote = o.optStringOrNull("deliveryNote"),
                invoiceNumber = o.optStringOrNull("invoiceNumber"),
                notes = o.optStringOrNull("notes"),
                createdBy = o.getLong("createdBy"),
                updatedBy = o.getLong("updatedBy"),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
            )
        }
    }

    private fun jsonToServiceLines(json: String): List<ServiceLine> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ServiceLine(
                id = o.getLong("id"),
                workOrderId = o.getLong("workOrderId"),
                description = o.getString("description"),
                laborCost = o.getDouble("laborCost"),
                discount = o.optDouble("discount", 0.0),
                notes = o.optStringOrNull("notes"),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
            )
        }
    }

    private fun jsonToWorkOrderParts(json: String): List<WorkOrderPart> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            WorkOrderPart(
                id = o.getLong("id"),
                workOrderId = o.getLong("workOrderId"),
                partId = o.getLong("partId"),
                quantity = o.getInt("quantity"),
                appliedUnitPrice = o.getDouble("appliedUnitPrice"),
                subtotal = o.getDouble("subtotal"),
                discount = o.optDouble("discount", 0.0),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
            )
        }
    }

    private fun jsonToWorkOrderPayments(json: String): List<WorkOrderPayment> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            WorkOrderPayment(
                id = o.getLong("id"),
                workOrderId = o.getLong("workOrderId"),
                amount = o.getDouble("amount"),
                discount = o.optDouble("discount", 0.0),
                method = PaymentMethod.valueOf(o.getString("method")),
                date = o.optLong("date", System.currentTimeMillis()),
                notes = o.optStringOrNull("notes"),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis())
            )
        }
    }

    private fun jsonToStatusLogs(json: String): List<WorkOrderStatusLog> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            WorkOrderStatusLog(
                id = o.getLong("id"),
                workOrderId = o.getLong("workOrderId"),
                oldStatus = o.optStringOrNull("oldStatus")?.let { OrderStatus.valueOf(it) },
                newStatus = OrderStatus.valueOf(o.getString("newStatus")),
                changedByUserId = o.getLong("changedByUserId"),
                changedAt = o.optLong("changedAt", System.currentTimeMillis()),
                note = o.optStringOrNull("note")
            )
        }
    }

    private fun jsonToWorkOrderMechanics(json: String): List<WorkOrderMechanic> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            WorkOrderMechanic(
                id = o.getLong("id"),
                workOrderId = o.getLong("workOrderId"),
                mechanicId = o.getLong("mechanicId"),
                commissionType = o.getString("commissionType"),
                commissionValue = o.getDouble("commissionValue"),
                commissionAmount = o.getDouble("commissionAmount"),
                commissionPaid = o.optBoolean("commissionPaid", false),
                paidAt = if (o.isNull("paidAt")) null else o.getLong("paidAt"),
                createdAt = o.optLong("createdAt", System.currentTimeMillis())
            )
        }
    }

    private fun jsonToCatalogBrands(json: String): List<CatalogBrand> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            CatalogBrand(id = o.getLong("id"), name = o.getString("name"))
        }
    }

    private fun jsonToCatalogModels(json: String): List<CatalogModel> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            CatalogModel(id = o.getLong("id"), brandId = o.getLong("brandId"), name = o.getString("name"))
        }
    }

    private fun jsonToCatalogColors(json: String): List<CatalogColor> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            CatalogColor(id = o.getLong("id"), name = o.getString("name"))
        }
    }

    private fun jsonToCatalogPartBrands(json: String): List<CatalogPartBrand> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            CatalogPartBrand(id = o.getLong("id"), name = o.getString("name"))
        }
    }

    private fun jsonToCatalogServices(json: String): List<CatalogService> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            CatalogService(
                id = o.getLong("id"),
                category = o.getString("category"),
                name = o.getString("name"),
                defaultPrice = o.optDouble("defaultPrice", 10.0),
                vehicleType = o.optStringOrNull("vehicleType")
            )
        }
    }

    private fun jsonToCatalogVehicleTypes(json: String): List<CatalogVehicleType> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            CatalogVehicleType(id = o.getLong("id"), name = o.getString("name"))
        }
    }

    private fun jsonToCatalogAccessories(json: String): List<CatalogAccessory> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            CatalogAccessory(id = o.getLong("id"), name = o.getString("name"))
        }
    }

    private fun jsonToCatalogOilTypes(json: String): List<CatalogOilType> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            CatalogOilType(id = o.getLong("id"), name = o.getString("name"))
        }
    }

    private fun jsonToCatalogComplaints(json: String): List<CatalogComplaint> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            CatalogComplaint(id = o.getLong("id"), name = o.getString("name"))
        }
    }

    private fun jsonToCatalogDiagnoses(json: String): List<CatalogDiagnosis> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            CatalogDiagnosis(
                id = o.getLong("id"),
                complaintId = o.getLong("complaintId"),
                name = o.getString("name")
            )
        }
    }

    // ── Helper extensions for nullable JSON values ──────────────

    private fun JSONObject.optStringOrNull(key: String): String? {
        return if (isNull(key)) null else optString(key, null)
    }

    private fun JSONObject.optIntOrNull(key: String): Int? {
        return if (isNull(key)) null else optInt(key).takeIf { has(key) }
    }

    private fun JSONObject.optLongOrNull(key: String): Long? {
        return if (isNull(key)) null else optLong(key).takeIf { has(key) }
    }

    private fun JSONObject.optDoubleOrNull(key: String): Double? {
        return if (isNull(key)) null else optDouble(key).takeIf { has(key) }
    }
}
