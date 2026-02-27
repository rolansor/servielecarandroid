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

@Entity(tableName = "catalog_part_brands")
data class CatalogPartBrand(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "catalog_services")
data class CatalogService(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val name: String,
    val defaultPrice: Double = 10.0,
    val vehicleType: String? = null
)

@Entity(tableName = "catalog_vehicle_types")
data class CatalogVehicleType(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "catalog_accessories")
data class CatalogAccessory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(tableName = "catalog_complaints")
data class CatalogComplaint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "catalog_diagnoses",
    foreignKeys = [ForeignKey(entity = CatalogComplaint::class, parentColumns = ["id"], childColumns = ["complaintId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("complaintId")]
)
data class CatalogDiagnosis(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val complaintId: Long,
    val name: String
)
