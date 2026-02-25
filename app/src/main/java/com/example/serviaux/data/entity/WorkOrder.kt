package com.example.serviaux.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "work_orders",
    indices = [
        Index(value = ["vehicleId"]),
        Index(value = ["customerId"]),
        Index(value = ["assignedMechanicId"]),
        Index(value = ["status"])
    ],
    foreignKeys = [
        ForeignKey(entity = Vehicle::class, parentColumns = ["id"], childColumns = ["vehicleId"]),
        ForeignKey(entity = Customer::class, parentColumns = ["id"], childColumns = ["customerId"])
    ]
)
data class WorkOrder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val customerId: Long,
    val entryDate: Long = System.currentTimeMillis(),
    val status: OrderStatus = OrderStatus.RECIBIDO,
    val priority: Priority = Priority.MEDIA,
    val customerComplaint: String,
    val initialDiagnosis: String? = null,
    val assignedMechanicId: Long? = null,
    val entryMileage: Int? = null,
    val fuelLevel: String? = null,
    val checklistNotes: String? = null,
    val totalLabor: Double = 0.0,
    val totalParts: Double = 0.0,
    val total: Double = 0.0,
    val createdBy: Long,
    val updatedBy: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
