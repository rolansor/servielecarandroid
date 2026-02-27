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
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupRepository(private val database: ServiauxDatabase) {

    companion object {
        private const val PHOTOS_DIR = "vehicle_photos"
        private const val BACKUPS_DIR = "backups"
        private const val MANIFEST_FILE = "manifest.json"
        private const val DB_VERSION = 5
    }

    data class BackupResult(
        val file: File? = null,
        val message: String = "",
        val success: Boolean = false,
        val counts: Map<String, Int> = emptyMap()
    )

    suspend fun exportToZip(context: Context): BackupResult {
        return try {
            val backupDir = File(context.cacheDir, BACKUPS_DIR).apply { mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val zipFile = File(backupDir, "respaldo_serviaux_$timestamp.zip")

            // Fetch all data
            val users = database.userDao().getAllDirect()
            val customers = database.customerDao().getAllDirect()
            val vehicles = database.vehicleDao().getAllDirect()
            val parts = database.partDao().getAllDirect()
            val workOrders = database.workOrderDao().getAllDirect()
            val serviceLines = database.serviceLineDao().getAllDirect()
            val workOrderParts = database.workOrderPartDao().getAllDirect()
            val workOrderPayments = database.workOrderPaymentDao().getAllDirect()
            val statusLogs = database.workOrderStatusLogDao().getAllDirect()
            val catalogBrands = database.catalogDao().getAllBrandsDirect()
            val catalogModels = database.catalogDao().getAllModelsDirect()
            val catalogColors = database.catalogDao().getAllColorsDirect()
            val catalogPartBrands = database.catalogDao().getAllPartBrandsDirect()
            val catalogServices = database.catalogDao().getAllServicesDirect()

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
                "catalog_brands" to catalogBrands.size,
                "catalog_models" to catalogModels.size,
                "catalog_colors" to catalogColors.size,
                "catalog_part_brands" to catalogPartBrands.size,
                "catalog_services" to catalogServices.size
            )

            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zip ->
                // Manifest
                val manifest = JSONObject().apply {
                    put("app", "serviaux")
                    put("dbVersion", DB_VERSION)
                    put("exportDate", System.currentTimeMillis())
                    put("exportDateFormatted", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
                    put("counts", JSONObject(counts))
                }
                writeZipEntry(zip, MANIFEST_FILE, manifest.toString(2))

                // Data files
                writeZipEntry(zip, "data/users.json", usersToJson(users))
                writeZipEntry(zip, "data/customers.json", customersToJson(customers))
                writeZipEntry(zip, "data/vehicles.json", vehiclesToJson(vehicles))
                writeZipEntry(zip, "data/parts.json", partsToJson(parts))
                writeZipEntry(zip, "data/work_orders.json", workOrdersToJson(workOrders))
                writeZipEntry(zip, "data/service_lines.json", serviceLinesToJson(serviceLines))
                writeZipEntry(zip, "data/work_order_parts.json", workOrderPartsToJson(workOrderParts))
                writeZipEntry(zip, "data/work_order_payments.json", workOrderPaymentsToJson(workOrderPayments))
                writeZipEntry(zip, "data/work_order_status_log.json", statusLogsToJson(statusLogs))
                writeZipEntry(zip, "data/catalog_brands.json", catalogBrandsToJson(catalogBrands))
                writeZipEntry(zip, "data/catalog_models.json", catalogModelsToJson(catalogModels))
                writeZipEntry(zip, "data/catalog_colors.json", catalogColorsToJson(catalogColors))
                writeZipEntry(zip, "data/catalog_part_brands.json", catalogPartBrandsToJson(catalogPartBrands))
                writeZipEntry(zip, "data/catalog_services.json", catalogServicesToJson(catalogServices))

                // Photos
                val photosDir = File(context.filesDir, PHOTOS_DIR)
                if (photosDir.exists()) {
                    val allPhotoPaths = mutableSetOf<String>()
                    vehicles.forEach { v ->
                        v.photoPaths?.split(",")?.filter { it.isNotBlank() }?.forEach { allPhotoPaths.add(it) }
                    }
                    workOrders.forEach { wo ->
                        wo.photoPaths?.split(",")?.filter { it.isNotBlank() }?.forEach { allPhotoPaths.add(it) }
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

    suspend fun importFromZip(context: Context, zipUri: Uri): BackupResult {
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
                ?: return BackupResult(message = "Archivo de respaldo inv\u00e1lido: falta manifest.json", success = false)
            val manifest = JSONObject(String(manifestBytes))
            val app = manifest.optString("app", "")
            if (app != "serviaux") {
                return BackupResult(message = "El archivo no es un respaldo de Serviaux", success = false)
            }
            val dbVersion = manifest.optInt("dbVersion", 0)
            if (dbVersion > DB_VERSION) {
                return BackupResult(
                    message = "Versi\u00f3n de respaldo ($dbVersion) no compatible con esta versi\u00f3n de la app ($DB_VERSION)",
                    success = false
                )
            }

            // Clear all tables
            database.clearAllTables()

            // Import data in FK order
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

            val counts = mutableMapOf<String, Int>()

            // Users
            entries["data/users.json"]?.let { bytes ->
                val items = jsonToUsers(String(bytes))
                items.forEach { userDao.insert(it) }
                counts["users"] = items.size
            }

            // Customers
            entries["data/customers.json"]?.let { bytes ->
                val items = jsonToCustomers(String(bytes))
                items.forEach { customerDao.insert(it) }
                counts["customers"] = items.size
            }

            // Vehicles (depends on customers)
            entries["data/vehicles.json"]?.let { bytes ->
                val items = jsonToVehicles(String(bytes))
                items.forEach { vehicleDao.insert(it) }
                counts["vehicles"] = items.size
            }

            // Parts
            entries["data/parts.json"]?.let { bytes ->
                val items = jsonToParts(String(bytes))
                items.forEach { partDao.insert(it) }
                counts["parts"] = items.size
            }

            // Catalog Brands
            entries["data/catalog_brands.json"]?.let { bytes ->
                val items = jsonToCatalogBrands(String(bytes))
                items.forEach { catalogDao.insertBrand(it) }
                counts["catalog_brands"] = items.size
            }

            // Catalog Models (depends on brands)
            entries["data/catalog_models.json"]?.let { bytes ->
                val items = jsonToCatalogModels(String(bytes))
                items.forEach { catalogDao.insertModel(it) }
                counts["catalog_models"] = items.size
            }

            // Catalog Colors
            entries["data/catalog_colors.json"]?.let { bytes ->
                val items = jsonToCatalogColors(String(bytes))
                items.forEach { catalogDao.insertColor(it) }
                counts["catalog_colors"] = items.size
            }

            // Catalog Part Brands
            entries["data/catalog_part_brands.json"]?.let { bytes ->
                val items = jsonToCatalogPartBrands(String(bytes))
                items.forEach { catalogDao.insertPartBrand(it) }
                counts["catalog_part_brands"] = items.size
            }

            // Catalog Services
            entries["data/catalog_services.json"]?.let { bytes ->
                val items = jsonToCatalogServices(String(bytes))
                items.forEach { catalogDao.insertService(it) }
                counts["catalog_services"] = items.size
            }

            // Work Orders (depends on vehicles, customers, users)
            entries["data/work_orders.json"]?.let { bytes ->
                val items = jsonToWorkOrders(String(bytes))
                items.forEach { workOrderDao.insert(it) }
                counts["work_orders"] = items.size
            }

            // Service Lines (depends on work orders)
            entries["data/service_lines.json"]?.let { bytes ->
                val items = jsonToServiceLines(String(bytes))
                items.forEach { serviceLineDao.insert(it) }
                counts["service_lines"] = items.size
            }

            // Work Order Parts (depends on work orders, parts)
            entries["data/work_order_parts.json"]?.let { bytes ->
                val items = jsonToWorkOrderParts(String(bytes))
                items.forEach { workOrderPartDao.insert(it) }
                counts["work_order_parts"] = items.size
            }

            // Work Order Payments (depends on work orders)
            entries["data/work_order_payments.json"]?.let { bytes ->
                val items = jsonToWorkOrderPayments(String(bytes))
                items.forEach { workOrderPaymentDao.insert(it) }
                counts["work_order_payments"] = items.size
            }

            // Status Logs (depends on work orders, users)
            entries["data/work_order_status_log.json"]?.let { bytes ->
                val items = jsonToStatusLogs(String(bytes))
                items.forEach { statusLogDao.insert(it) }
                counts["work_order_status_log"] = items.size
            }

            // Restore photos
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

            // Update photo paths to match new device path
            if (photoCount > 0) {
                val photoDirPath = photosDir.absolutePath
                // Re-read and update vehicles with photo paths
                val importedVehicles = vehicleDao.getAllDirect()
                importedVehicles.filter { !it.photoPaths.isNullOrBlank() }.forEach { vehicle ->
                    val updatedPaths = vehicle.photoPaths!!.split(",").map { path ->
                        val fileName = File(path).name
                        "$photoDirPath/$fileName"
                    }.joinToString(",")
                    vehicleDao.update(vehicle.copy(photoPaths = updatedPaths))
                }
                // Re-read and update work orders with photo paths
                val importedOrders = workOrderDao.getAllDirect()
                importedOrders.filter { !it.photoPaths.isNullOrBlank() }.forEach { order ->
                    val updatedPaths = order.photoPaths!!.split(",").map { path ->
                        val fileName = File(path).name
                        "$photoDirPath/$fileName"
                    }.joinToString(",")
                    workOrderDao.update(order.copy(photoPaths = updatedPaths))
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
            "Pagos" to database.workOrderPaymentDao().getAllDirect().size
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
                put("currentMileage", v.currentMileage ?: JSONObject.NULL)
                put("engineDisplacement", v.engineDisplacement ?: JSONObject.NULL)
                put("engineNumber", v.engineNumber ?: JSONObject.NULL)
                put("drivetrain", v.drivetrain)
                put("transmission", v.transmission)
                put("notes", v.notes ?: JSONObject.NULL)
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
                put("status", o.status.name)
                put("priority", o.priority.name)
                put("customerComplaint", o.customerComplaint)
                put("initialDiagnosis", o.initialDiagnosis ?: JSONObject.NULL)
                put("assignedMechanicId", o.assignedMechanicId ?: JSONObject.NULL)
                put("entryMileage", o.entryMileage ?: JSONObject.NULL)
                put("fuelLevel", o.fuelLevel ?: JSONObject.NULL)
                put("checklistNotes", o.checklistNotes ?: JSONObject.NULL)
                put("totalLabor", o.totalLabor)
                put("totalParts", o.totalParts)
                put("total", o.total)
                put("photoPaths", o.photoPaths ?: JSONObject.NULL)
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
                idNumber = o.optStringOrNull("idNumber"),
                phone = o.getString("phone"),
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
                currentMileage = o.optIntOrNull("currentMileage"),
                engineDisplacement = o.optStringOrNull("engineDisplacement"),
                engineNumber = o.optStringOrNull("engineNumber"),
                drivetrain = o.optString("drivetrain", "4x2"),
                transmission = o.optString("transmission", "Manual"),
                notes = o.optStringOrNull("notes"),
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
                status = OrderStatus.valueOf(o.getString("status")),
                priority = Priority.valueOf(o.getString("priority")),
                customerComplaint = o.getString("customerComplaint"),
                initialDiagnosis = o.optStringOrNull("initialDiagnosis"),
                assignedMechanicId = o.optLongOrNull("assignedMechanicId"),
                entryMileage = o.optIntOrNull("entryMileage"),
                fuelLevel = o.optStringOrNull("fuelLevel"),
                checklistNotes = o.optStringOrNull("checklistNotes"),
                totalLabor = o.optDouble("totalLabor", 0.0),
                totalParts = o.optDouble("totalParts", 0.0),
                total = o.optDouble("total", 0.0),
                photoPaths = o.optStringOrNull("photoPaths"),
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
