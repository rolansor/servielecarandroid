package com.example.serviaux.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "work_order_status_log",
    indices = [Index(value = ["workOrderId"])],
    foreignKeys = [
        ForeignKey(entity = WorkOrder::class, parentColumns = ["id"], childColumns = ["workOrderId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class WorkOrderStatusLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workOrderId: Long,
    val oldStatus: OrderStatus?,
    val newStatus: OrderStatus,
    val changedByUserId: Long,
    val changedAt: Long = System.currentTimeMillis(),
    val note: String? = null
)
