package com.example.serviaux.data.entity

import androidx.room.*

@Entity(tableName = "catalog_brands")
data class CatalogBrand(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(index = true) val name: String
)

@Entity(
    tableName = "catalog_models",
    foreignKeys = [ForeignKey(entity = CatalogBrand::class, parentColumns = ["id"], childColumns = ["brandId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("brandId")]
)
data class CatalogModel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val brandId: Long,
    val name: String
)

@Entity(tableName = "catalog_colors")
data class CatalogColor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
