package com.example.serviaux.data.dao

import androidx.room.*
import com.example.serviaux.data.entity.CatalogBrand
import com.example.serviaux.data.entity.CatalogModel
import com.example.serviaux.data.entity.CatalogColor
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    // Brands
    @Query("SELECT * FROM catalog_brands ORDER BY name")
    fun getAllBrands(): Flow<List<CatalogBrand>>

    @Query("SELECT * FROM catalog_brands ORDER BY name")
    suspend fun getAllBrandsDirect(): List<CatalogBrand>

    @Insert
    suspend fun insertBrand(brand: CatalogBrand): Long

    @Update
    suspend fun updateBrand(brand: CatalogBrand)

    @Delete
    suspend fun deleteBrand(brand: CatalogBrand)

    @Query("DELETE FROM catalog_brands")
    suspend fun deleteAllBrands()

    // Models
    @Query("SELECT * FROM catalog_models WHERE brandId = :brandId ORDER BY name")
    fun getModelsByBrand(brandId: Long): Flow<List<CatalogModel>>

    @Query("SELECT * FROM catalog_models ORDER BY name")
    suspend fun getAllModelsDirect(): List<CatalogModel>

    @Insert
    suspend fun insertModel(model: CatalogModel): Long

    @Update
    suspend fun updateModel(model: CatalogModel)

    @Delete
    suspend fun deleteModel(model: CatalogModel)

    @Query("DELETE FROM catalog_models")
    suspend fun deleteAllModels()

    // Colors
    @Query("SELECT * FROM catalog_colors ORDER BY name")
    fun getAllColors(): Flow<List<CatalogColor>>

    @Query("SELECT * FROM catalog_colors ORDER BY name")
    suspend fun getAllColorsDirect(): List<CatalogColor>

    @Insert
    suspend fun insertColor(color: CatalogColor): Long

    @Update
    suspend fun updateColor(color: CatalogColor)

    @Delete
    suspend fun deleteColor(color: CatalogColor)

    @Query("DELETE FROM catalog_colors")
    suspend fun deleteAllColors()
}
