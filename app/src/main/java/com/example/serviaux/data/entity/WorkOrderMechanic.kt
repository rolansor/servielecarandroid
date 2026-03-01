package com.example.serviaux.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "work_order_mechanics",
    indices = [
        Index(value = ["workOrderId"]),
        Index(value = ["mechanicId"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = WorkOrder::class,
            parentColumns = ["id"],
            childColumns = ["workOrderId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["mechanicId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WorkOrderMechanic(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workOrderId: Long,
    val mechanicId: Long,
    val commissionType: String,
    val commissionValue: Double,
    val commissionAmount: Double,
    val commissionPaid: Boolean = false,
    val paidAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
