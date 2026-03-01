/**
 * Customer.kt - Entidad de cliente del taller.
 *
 * Representa a un cliente que posee uno o más vehículos.
 * Es referenciado por [Vehicle] y [WorkOrder] mediante claves foráneas.
 */
package com.example.serviaux.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cliente del taller automotriz.
 *
 * @property fullName Nombre completo del cliente.
 * @property docType Tipo de documento: CEDULA, PASAPORTE, EXTRANJERIA, VISA, OTRO.
 * @property idNumber Número de documento de identidad, opcional.
 * @property phone Teléfono de contacto principal.
 * @property email Correo electrónico (por defecto noreply@noreply.com).
 * @property address Dirección física, opcional.
 * @property notes Observaciones internas sobre el cliente.
 */
@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val docType: String = "CEDULA",
    val idNumber: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
