/**
 * CatalogRepository.kt - Repositorio de catálogos del sistema.
 *
 * Centraliza las operaciones CRUD para los 9 tipos de catálogo y proporciona
 * funciones de exportación/importación en formato JSON para respaldos.
 * Los catálogos alimentan los dropdowns y autocompletados de toda la aplicación.
 */
package com.example.serviaux.repository

import androidx.room.withTransaction
import com.example.serviaux.data.ServiauxDatabase
import com.example.serviaux.data.dao.CatalogDao
import com.example.serviaux.data.entity.CatalogBrand
import com.example.serviaux.data.entity.CatalogModel
import com.example.serviaux.data.entity.CatalogColor
import com.example.serviaux.data.entity.CatalogPartBrand
import com.example.serviaux.data.entity.CatalogService
import com.example.serviaux.data.entity.CatalogVehicleType
import com.example.serviaux.data.entity.CatalogAccessory
import com.example.serviaux.data.entity.CatalogComplaint
import com.example.serviaux.data.entity.CatalogDiagnosis
import com.example.serviaux.data.entity.CatalogOilType
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import org.json.JSONArray

/**
 * Repositorio unificado de catálogos.
 * Cada sección corresponde a un tipo de catálogo con operaciones CRUD completas.
 */
class CatalogRepository(
    private val dao: CatalogDao,
    private val database: ServiauxDatabase
) {
    // ── Marcas de vehículos ────────────────────────────────────────────
    fun getAllBrands(): Flow<List<CatalogBrand>> = dao.getAllBrands()
    suspend fun getAllBrandsDirect(): List<CatalogBrand> = dao.getAllBrandsDirect()
    suspend fun insertBrand(name: String): Long = dao.insertBrand(CatalogBrand(name = name))
    suspend fun updateBrand(brand: CatalogBrand) = dao.updateBrand(brand)
    suspend fun deleteBrand(brand: CatalogBrand) = dao.deleteBrand(brand)

    // ── Modelos ─────────────────────────────────────────────────────────
    fun getModelsByBrand(brandId: Long): Flow<List<CatalogModel>> = dao.getModelsByBrand(brandId)
    suspend fun insertModel(brandId: Long, name: String): Long = dao.insertModel(CatalogModel(brandId = brandId, name = name))
    suspend fun updateModel(model: CatalogModel) = dao.updateModel(model)
    suspend fun deleteModel(model: CatalogModel) = dao.deleteModel(model)

    // ── Colores ─────────────────────────────────────────────────────────
    fun getAllColors(): Flow<List<CatalogColor>> = dao.getAllColors()
    suspend fun insertColor(name: String): Long = dao.insertColor(CatalogColor(name = name))
    suspend fun updateColor(color: CatalogColor) = dao.updateColor(color)
    suspend fun deleteColor(color: CatalogColor) = dao.deleteColor(color)

    // ── Marcas de repuestos ───────────────────────────────────────────
    fun getAllPartBrands(): Flow<List<CatalogPartBrand>> = dao.getAllPartBrands()
    suspend fun insertPartBrand(name: String): Long = dao.insertPartBrand(CatalogPartBrand(name = name))
    suspend fun updatePartBrand(partBrand: CatalogPartBrand) = dao.updatePartBrand(partBrand)
    suspend fun deletePartBrand(partBrand: CatalogPartBrand) = dao.deletePartBrand(partBrand)

    // ── Servicios predefinidos ──────────────────────────────────────────
    fun getAllServices(): Flow<List<CatalogService>> = dao.getAllServices()
    suspend fun getAllServicesDirect(): List<CatalogService> = dao.getAllServicesDirect()
    fun getServiceCategories(): Flow<List<String>> = dao.getServiceCategories()
    fun getServicesByVehicleType(type: String): Flow<List<CatalogService>> = dao.getServicesByVehicleType(type)
    suspend fun insertService(category: String, name: String, defaultPrice: Double, vehicleType: String? = null): Long =
        dao.insertService(CatalogService(category = category, name = name, defaultPrice = defaultPrice, vehicleType = vehicleType))
    suspend fun updateService(service: CatalogService) = dao.updateService(service)
    suspend fun deleteService(service: CatalogService) = dao.deleteService(service)

    // ── Tipos de vehículo ────────────────────────────────────────────
    fun getAllVehicleTypes(): Flow<List<CatalogVehicleType>> = dao.getAllVehicleTypes()
    suspend fun getAllVehicleTypesDirect(): List<CatalogVehicleType> = dao.getAllVehicleTypesDirect()
    suspend fun insertVehicleType(name: String): Long = dao.insertVehicleType(CatalogVehicleType(name = name))
    suspend fun updateVehicleType(vt: CatalogVehicleType) = dao.updateVehicleType(vt)
    suspend fun deleteVehicleType(vt: CatalogVehicleType) = dao.deleteVehicleType(vt)

    // ── Accesorios ──────────────────────────────────────────────────────
    fun getAllAccessories(): Flow<List<CatalogAccessory>> = dao.getAllAccessories()
    suspend fun getAllAccessoriesDirect(): List<CatalogAccessory> = dao.getAllAccessoriesDirect()
    suspend fun insertAccessory(name: String): Long = dao.insertAccessory(CatalogAccessory(name = name))
    suspend fun updateAccessory(acc: CatalogAccessory) = dao.updateAccessory(acc)
    suspend fun deleteAccessory(acc: CatalogAccessory) = dao.deleteAccessory(acc)

    // ── Quejas ──────────────────────────────────────────────────────────
    fun getAllComplaints(): Flow<List<CatalogComplaint>> = dao.getAllComplaints()
    suspend fun getAllComplaintsDirect(): List<CatalogComplaint> = dao.getAllComplaintsDirect()
    suspend fun insertComplaint(name: String): Long = dao.insertComplaint(CatalogComplaint(name = name))
    suspend fun updateComplaint(complaint: CatalogComplaint) = dao.updateComplaint(complaint)
    suspend fun deleteComplaint(complaint: CatalogComplaint) = dao.deleteComplaint(complaint)

    // ── Diagnósticos ────────────────────────────────────────────────────
    fun getAllDiagnoses(): Flow<List<CatalogDiagnosis>> = dao.getAllDiagnoses()
    fun getDiagnosesByComplaint(complaintId: Long): Flow<List<CatalogDiagnosis>> = dao.getDiagnosesByComplaint(complaintId)
    suspend fun getAllDiagnosesDirect(): List<CatalogDiagnosis> = dao.getAllDiagnosesDirect()
    suspend fun insertDiagnosis(complaintId: Long, name: String): Long = dao.insertDiagnosis(CatalogDiagnosis(complaintId = complaintId, name = name))
    suspend fun updateDiagnosis(diagnosis: CatalogDiagnosis) = dao.updateDiagnosis(diagnosis)
    suspend fun deleteDiagnosis(diagnosis: CatalogDiagnosis) = dao.deleteDiagnosis(diagnosis)

    // ── Tipos de aceite ────────────────────────────────────────────────
    fun getAllOilTypes(): Flow<List<CatalogOilType>> = dao.getAllOilTypes()
    suspend fun getAllOilTypesDirect(): List<CatalogOilType> = dao.getAllOilTypesDirect()
    suspend fun insertOilType(name: String): Long = dao.insertOilType(CatalogOilType(name = name))
    suspend fun updateOilType(oilType: CatalogOilType) = dao.updateOilType(oilType)
    suspend fun deleteOilType(oilType: CatalogOilType) = dao.deleteOilType(oilType)

    // ── Exportación/Importación JSON ──────────────────────────────────

    /** Exporta todos los catálogos como cadena JSON estructurada. */
    suspend fun exportToJson(): String {
        val brands = dao.getAllBrandsDirect()
        val models = dao.getAllModelsDirect()
        val colors = dao.getAllColorsDirect()
        val partBrands = dao.getAllPartBrandsDirect()
        val services = dao.getAllServicesDirect()
        val vehicleTypes = dao.getAllVehicleTypesDirect()
        val accessories = dao.getAllAccessoriesDirect()
        val complaints = dao.getAllComplaintsDirect()
        val diagnoses = dao.getAllDiagnosesDirect()
        val oilTypes = dao.getAllOilTypesDirect()

        val json = JSONObject()

        // Brands with models
        val brandsJson = JSONObject()
        for (brand in brands) {
            val brandModels = models.filter { it.brandId == brand.id }.map { it.name }
            brandsJson.put(brand.name, JSONArray(brandModels))
        }
        json.put("marcas", brandsJson)

        // Colors
        json.put("colores", JSONArray(colors.map { it.name }))

        // Part brands
        json.put("marcas_repuestos", JSONArray(partBrands.map { it.name }))

        // Services (grouped by category)
        val servicesJson = JSONObject()
        val servicesByCategory = services.groupBy { it.category }
        for ((category, catServices) in servicesByCategory) {
            val arr = JSONArray()
            for (s in catServices) {
                arr.put(JSONObject().apply {
                    put("nombre", s.name)
                    put("precio", s.defaultPrice)
                    put("tipo_vehiculo", s.vehicleType ?: JSONObject.NULL)
                })
            }
            servicesJson.put(category, arr)
        }
        json.put("servicios", servicesJson)

        // Vehicle types
        json.put("tipos_vehiculo", JSONArray(vehicleTypes.map { it.name }))

        // Accessories
        json.put("accesorios", JSONArray(accessories.map { it.name }))

        // Complaints with diagnoses
        val complaintsJson = JSONObject()
        for (complaint in complaints) {
            val complaintDiagnoses = diagnoses.filter { it.complaintId == complaint.id }.map { it.name }
            complaintsJson.put(complaint.name, JSONArray(complaintDiagnoses))
        }
        json.put("motivos", complaintsJson)

        // Oil types
        json.put("tipos_aceite", JSONArray(oilTypes.map { it.name }))

        return json.toString(2)
    }

    /**
     * Catálogos ya interpretados desde un JSON, listos para escribir en la base.
     * Se construye completo en memoria antes de tocar nada.
     */
    private data class ParsedCatalogs(
        val brands: List<Pair<String, List<String>>>,
        val colors: List<String>,
        val partBrands: List<String>,
        val services: List<CatalogService>,
        val vehicleTypes: List<String>,
        val accessories: List<String>,
        val oilTypes: List<String>,
        val complaints: List<Pair<String, List<String>>>
    )

    /**
     * Importa catálogos desde JSON, reemplazando todos los datos existentes.
     *
     * El JSON se interpreta **por completo antes** de borrar nada y la escritura va en una
     * transacción. Antes, un solo campo mal formado a mitad del archivo dejaba el taller sin
     * marcas, modelos, colores ni motivos, porque el borrado ya se había ejecutado.
     *
     * @throws org.json.JSONException si el archivo no tiene la estructura esperada; en ese caso
     *   la base queda intacta.
     */
    suspend fun importFromJson(jsonString: String) {
        val parsed = parseCatalogs(JSONObject(jsonString))
        database.withTransaction {
            dao.deleteAllDiagnoses()
            dao.deleteAllComplaints()
            dao.deleteAllModels()
            dao.deleteAllBrands()
            dao.deleteAllColors()
            dao.deleteAllPartBrands()
            dao.deleteAllServices()
            dao.deleteAllVehicleTypes()
            dao.deleteAllAccessories()
            dao.deleteAllOilTypes()

            parsed.brands.forEach { (brandName, models) ->
                val brandId = dao.insertBrand(CatalogBrand(name = brandName))
                models.forEach { dao.insertModel(CatalogModel(brandId = brandId, name = it)) }
            }
            parsed.colors.forEach { dao.insertColor(CatalogColor(name = it)) }
            parsed.partBrands.forEach { dao.insertPartBrand(CatalogPartBrand(name = it)) }
            parsed.services.forEach { dao.insertService(it) }
            parsed.vehicleTypes.forEach { dao.insertVehicleType(CatalogVehicleType(name = it)) }
            parsed.accessories.forEach { dao.insertAccessory(CatalogAccessory(name = it)) }
            parsed.oilTypes.forEach { dao.insertOilType(CatalogOilType(name = it)) }
            parsed.complaints.forEach { (complaintName, diagnoses) ->
                val complaintId = dao.insertComplaint(CatalogComplaint(name = complaintName))
                diagnoses.forEach { dao.insertDiagnosis(CatalogDiagnosis(complaintId = complaintId, name = it)) }
            }
        }
    }

    /** Interpreta el JSON de catálogos. Lanza excepción si la estructura es inválida. */
    private fun parseCatalogs(json: JSONObject): ParsedCatalogs {
        fun stringList(key: String): List<String> {
            if (!json.has(key)) return emptyList()
            val arr = json.getJSONArray(key)
            return (0 until arr.length()).map { arr.getString(it) }
        }

        fun nestedList(key: String): List<Pair<String, List<String>>> {
            if (!json.has(key)) return emptyList()
            val obj = json.getJSONObject(key)
            return obj.keys().asSequence().map { parentName ->
                val arr = obj.getJSONArray(parentName)
                parentName to (0 until arr.length()).map { arr.getString(it) }
            }.toList()
        }

        val services = mutableListOf<CatalogService>()
        if (json.has("servicios")) {
            val servicesJson = json.getJSONObject("servicios")
            for (category in servicesJson.keys()) {
                val arr = servicesJson.getJSONArray(category)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    services.add(
                        CatalogService(
                            category = category,
                            name = obj.getString("nombre"),
                            defaultPrice = obj.optDouble("precio", 10.0),
                            vehicleType = if (obj.has("tipo_vehiculo") && !obj.isNull("tipo_vehiculo"))
                                obj.getString("tipo_vehiculo") else null
                        )
                    )
                }
            }
        }

        return ParsedCatalogs(
            brands = nestedList("marcas"),
            colors = stringList("colores"),
            partBrands = stringList("marcas_repuestos"),
            services = services,
            vehicleTypes = stringList("tipos_vehiculo"),
            accessories = stringList("accesorios"),
            oilTypes = stringList("tipos_aceite"),
            complaints = nestedList("motivos")
        )
    }
}
