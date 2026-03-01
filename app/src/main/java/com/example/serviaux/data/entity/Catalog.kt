/**
 * Catalog.kt - Entidades de catálogos del sistema.
 *
 * Define las tablas de catálogo que alimentan los dropdowns y autocompletados
 * en la UI: marcas de vehículos, modelos, colores, marcas de repuestos,
 * servicios predefinidos, tipos de vehículo, accesorios, quejas y diagnósticos.
 *
 * Los catálogos son administrados desde la pantalla de Configuración de Catálogos
 * y se incluyen en los respaldos exportables.
 */
package com.example.serviaux.data.entity

import androidx.room.*

// ── Marcas y modelos de vehículos ──────────────────────────────────────

/** Marca de vehículo (ej. Toyota, Ford, Chevrolet). */
@Entity(tableName = "catalog_brands")
data class CatalogBrand(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(index = true) val name: String
)

/** Modelo de vehículo asociado a una [CatalogBrand] (ej. Corolla, Ranger). CASCADE al eliminar marca. */
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

// ── Colores ────────────────────────────────────────────────────────────

/** Color de vehículo para el formulario de registro. */
@Entity(tableName = "catalog_colors")
data class CatalogColor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

// ── Repuestos ──────────────────────────────────────────────────────────

/** Marca de repuesto/pieza del inventario. */
@Entity(tableName = "catalog_part_brands")
data class CatalogPartBrand(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

// ── Servicios predefinidos ──────────────────────────────────────────────

/**
 * Servicio predefinido del taller con precio por defecto.
 *
 * @property category Categoría del servicio (ej. "Motor", "Frenos").
 * @property name Nombre del servicio.
 * @property defaultPrice Precio sugerido; el usuario puede modificarlo al agregar la línea.
 * @property vehicleType Tipo de vehículo al que aplica; null si aplica a todos.
 */
@Entity(tableName = "catalog_services")
data class CatalogService(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val name: String,
    val defaultPrice: Double = 10.0,
    val vehicleType: String? = null
)

// ── Tipos de vehículo, accesorios y diagnósticos ───────────────────────

/** Tipo de vehículo (ej. sedán, camioneta, SUV). */
@Entity(tableName = "catalog_vehicle_types")
data class CatalogVehicleType(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

/** Accesorio verificado en el checklist de recepción del vehículo. */
@Entity(tableName = "catalog_accessories")
data class CatalogAccessory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

/** Queja o síntoma reportado por el cliente. */
@Entity(tableName = "catalog_complaints")
data class CatalogComplaint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

/** Diagnóstico asociado a una [CatalogComplaint]. CASCADE al eliminar queja. */
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

// ── Tipos de aceite ───────────────────────────────────────────────────

/** Tipo de aceite de motor (ej. 5W-30, 10W-40). */
@Entity(tableName = "catalog_oil_types")
data class CatalogOilType(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
