/**
 * Vehicle.kt - Entidad de vehículo del taller.
 *
 * Cada vehículo pertenece a un [Customer] y puede tener múltiples [WorkOrder].
 * La placa tiene índice único para evitar registros duplicados.
 * Las fotos se almacenan como rutas separadas por comas en [photoPaths].
 */
package com.example.serviaux.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Vehículo registrado en el taller.
 *
 * @property customerId FK al cliente propietario; CASCADE al eliminar cliente.
 * @property plate Placa única del vehículo (índice único).
 * @property brand Marca del vehículo (ej. Toyota, Ford).
 * @property model Modelo del vehículo (ej. Corolla, Ranger).
 * @property vehicleType Tipo de vehículo (sedán, camioneta, etc.), tomado del catálogo.
 * @property currentMileage Kilometraje actual al momento del último registro.
 * @property photoPaths Rutas de fotos separadas por comas (máximo 6).
 */
@Entity(
    tableName = "vehicles",
    indices = [
        Index(value = ["plate"], unique = true),
        Index(value = ["customerId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val plate: String,
    val brand: String,
    val model: String,
    val version: String? = null,
    val year: Int? = null,
    val vin: String? = null,
    val color: String? = null,
    val vehicleType: String? = null,
    val fuelType: String? = null,
    val currentMileage: Int? = null,
    val engineDisplacement: String? = null,
    val engineNumber: String? = null,
    val drivetrain: String = "4x2",
    val transmission: String = "Manual",
    val notes: String? = null,
    val registrationPhotoPaths: String? = null,
    val photoPaths: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
