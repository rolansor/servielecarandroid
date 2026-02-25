package com.example.serviaux.repository

import com.example.serviaux.data.dao.CatalogDao
import com.example.serviaux.data.entity.CatalogBrand
import com.example.serviaux.data.entity.CatalogModel
import com.example.serviaux.data.entity.CatalogColor
import com.example.serviaux.data.entity.CatalogPartBrand
import com.example.serviaux.data.entity.CatalogService
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import org.json.JSONArray

class CatalogRepository(private val dao: CatalogDao) {
    // Brands
    fun getAllBrands(): Flow<List<CatalogBrand>> = dao.getAllBrands()
    suspend fun getAllBrandsDirect(): List<CatalogBrand> = dao.getAllBrandsDirect()
    suspend fun insertBrand(name: String): Long = dao.insertBrand(CatalogBrand(name = name))
    suspend fun updateBrand(brand: CatalogBrand) = dao.updateBrand(brand)
    suspend fun deleteBrand(brand: CatalogBrand) = dao.deleteBrand(brand)

    // Models
    fun getModelsByBrand(brandId: Long): Flow<List<CatalogModel>> = dao.getModelsByBrand(brandId)
    suspend fun insertModel(brandId: Long, name: String): Long = dao.insertModel(CatalogModel(brandId = brandId, name = name))
    suspend fun updateModel(model: CatalogModel) = dao.updateModel(model)
    suspend fun deleteModel(model: CatalogModel) = dao.deleteModel(model)

    // Colors
    fun getAllColors(): Flow<List<CatalogColor>> = dao.getAllColors()
    suspend fun insertColor(name: String): Long = dao.insertColor(CatalogColor(name = name))
    suspend fun updateColor(color: CatalogColor) = dao.updateColor(color)
    suspend fun deleteColor(color: CatalogColor) = dao.deleteColor(color)

    // Part Brands
    fun getAllPartBrands(): Flow<List<CatalogPartBrand>> = dao.getAllPartBrands()
    suspend fun insertPartBrand(name: String): Long = dao.insertPartBrand(CatalogPartBrand(name = name))
    suspend fun updatePartBrand(partBrand: CatalogPartBrand) = dao.updatePartBrand(partBrand)
    suspend fun deletePartBrand(partBrand: CatalogPartBrand) = dao.deletePartBrand(partBrand)

    // Services
    fun getAllServices(): Flow<List<CatalogService>> = dao.getAllServices()
    suspend fun getAllServicesDirect(): List<CatalogService> = dao.getAllServicesDirect()
    fun getServiceCategories(): Flow<List<String>> = dao.getServiceCategories()

    // Export all catalogs as JSON string
    suspend fun exportToJson(): String {
        val brands = dao.getAllBrandsDirect()
        val models = dao.getAllModelsDirect()
        val colors = dao.getAllColorsDirect()
        val partBrands = dao.getAllPartBrandsDirect()

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

        return json.toString(2)
    }

    // Import from JSON string, replaces all existing data
    suspend fun importFromJson(jsonString: String) {
        val json = JSONObject(jsonString)

        // Clear existing
        dao.deleteAllModels()
        dao.deleteAllBrands()
        dao.deleteAllColors()
        dao.deleteAllPartBrands()

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
    }
}
