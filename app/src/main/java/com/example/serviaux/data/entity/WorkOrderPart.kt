package com.example.serviaux.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "work_order_parts",
    indices = [
        Index(value = ["workOrderId"]),
        Index(value = ["partId"])
    ],
    foreignKeys = [
        ForeignKey(entity = WorkOrder::class, parentColumns = ["id"], childColumns = ["workOrderId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Part::class, parentColumns = ["id"], childColumns = ["partId"])
    ]
)
data class WorkOrderPart(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workOrderId: Long,
    val partId: Long,
    val quantity: Int,
    val appliedUnitPrice: Double,
    val subtotal: Double,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
