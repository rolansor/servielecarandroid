package com.example.serviaux.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "appointments",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["vehicleId"]),
        Index(value = ["status"]),
        Index(value = ["scheduledDate"])
    ],
    foreignKeys = [
        ForeignKey(entity = Customer::class, parentColumns = ["id"], childColumns = ["customerId"]),
        ForeignKey(entity = Vehicle::class, parentColumns = ["id"], childColumns = ["vehicleId"])
    ]
)
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val vehicleId: Long,
    val scheduledDate: Long,
    val notes: String? = null,
    val status: AppointmentStatus = AppointmentStatus.PENDIENTE,
    val workOrderId: Long? = null,
    val createdBy: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
