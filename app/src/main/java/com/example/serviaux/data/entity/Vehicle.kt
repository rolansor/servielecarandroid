package com.example.serviaux.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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
    val currentMileage: Int? = null,
    val engineDisplacement: String? = null,
    val engineNumber: String? = null,
    val drivetrain: String = "4x2",
    val transmission: String = "Manual",
    val notes: String? = null,
    val photoPaths: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
