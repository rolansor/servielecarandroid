package com.example.serviaux.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "work_order_payments",
    indices = [Index(value = ["workOrderId"])],
    foreignKeys = [
        ForeignKey(entity = WorkOrder::class, parentColumns = ["id"], childColumns = ["workOrderId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class WorkOrderPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workOrderId: Long,
    val amount: Double,
    val discount: Double = 0.0,
    val method: PaymentMethod,
    val date: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
