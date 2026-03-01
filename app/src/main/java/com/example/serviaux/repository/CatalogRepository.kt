/**
 * CatalogRepository.kt - Repositorio de catálogos del sistema.
 *
 * Centraliza las operaciones CRUD para los 9 tipos de catálogo y proporciona
 * funciones de exportación/importación en formato JSON para respaldos.
 * Los catálogos alimentan los dropdowns y autocompletados de toda la aplicación.
 */
package com.example.serviaux.repository

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
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import org.json.JSONArray

/**
 * Repositorio unificado de catálogos.
 * Cada sección corresponde a un tipo de catálogo con operaciones CRUD completas.
 */
class CatalogRepository(private val dao: CatalogDao) {
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

        return json.toString(2)
    }

    /** Importa catálogos desde JSON, reemplazando todos los datos existentes. */
    suspend fun importFromJson(jsonString: String) {
        val json = JSONObject(jsonString)

        // Clear existing
        dao.deleteAllDiagnoses()
        dao.deleteAllComplaints()
        dao.deleteAllModels()
        dao.deleteAllBrands()
        dao.deleteAllColors()
        dao.deleteAllPartBrands()
        dao.deleteAllServices()
        dao.deleteAllVehicleTypes()
        dao.deleteAllAccessories()

        // Import brands and models
        if (json.has("marcas")) {
            val brandsJson = json.getJSONObject("marcas")
            for (brandName in brandsJson.keys()) {
                val brandId = dao.insertBrand(CatalogBrand(name = brandName))
                val modelsArray = brandsJson.getJSONArray(brandName)
                for (i in 0 until modelsArray.length()) {
                    dao.insertModel(CatalogModel(brandId = brandId, name = modelsArray.getString(i)))
                }
            }
        }

        // Import colors
        if (json.has("colores")) {
            val colorsArray = json.getJSONArray("colores")
            for (i in 0 until colorsArray.length()) {
                dao.insertColor(CatalogColor(name = colorsArray.getString(i)))
            }
        }

        // Import part brands
        if (json.has("marcas_repuestos")) {
            val partBrandsArray = json.getJSONArray("marcas_repuestos")
            for (i in 0 until partBrandsArray.length()) {
                dao.insertPartBrand(CatalogPartBrand(name = partBrandsArray.getString(i)))
            }
        }

        // Import services
        if (json.has("servicios")) {
            val servicesJson = json.getJSONObject("servicios")
            for (category in servicesJson.keys()) {
                val arr = servicesJson.getJSONArray(category)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val vType = if (obj.has("tipo_vehiculo") && !obj.isNull("tipo_vehiculo")) obj.getString("tipo_vehiculo") else null
                    dao.insertService(CatalogService(
                        category = category,
                        name = obj.getString("nombre"),
                        defaultPrice = obj.optDouble("precio", 10.0),
                        vehicleType = vType
                    ))
                }
            }
        }

        // Import vehicle types
        if (json.has("tipos_vehiculo")) {
            val arr = json.getJSONArray("tipos_vehiculo")
            for (i in 0 until arr.length()) {
                dao.insertVehicleType(CatalogVehicleType(name = arr.getString(i)))
            }
        }

        // Import accessories
        if (json.has("accesorios")) {
            val arr = json.getJSONArray("accesorios")
            for (i in 0 until arr.length()) {
                dao.insertAccessory(CatalogAccessory(name = arr.getString(i)))
            }
        }

        // Import complaints with diagnoses
        if (json.has("motivos")) {
            val complaintsJson = json.getJSONObject("motivos")
            for (complaintName in complaintsJson.keys()) {
                val complaintId = dao.insertComplaint(CatalogComplaint(name = complaintName))
                val diagnosesArray = complaintsJson.getJSONArray(complaintName)
                for (i in 0 until diagnosesArray.length()) {
                    dao.insertDiagnosis(CatalogDiagnosis(complaintId = complaintId, name = diagnosesArray.getString(i)))
                }
            }
        }
    }
}
