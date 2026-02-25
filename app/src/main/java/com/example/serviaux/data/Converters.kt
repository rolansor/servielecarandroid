package com.example.serviaux.data

import androidx.room.TypeConverter
import com.example.serviaux.data.entity.*

class Converters {
    @TypeConverter fun fromUserRole(value: UserRole): String = value.name
    @TypeConverter fun toUserRole(value: String): UserRole = UserRole.valueOf(value)
    @TypeConverter fun fromOrderStatus(value: OrderStatus?): String? = value?.name
    @TypeConverter fun toOrderStatus(value: String?): OrderStatus? = value?.let { OrderStatus.valueOf(it) }
    @TypeConverter fun fromPriority(value: Priority): String = value.name
    @TypeConverter fun toPriority(value: String): Priority = Priority.valueOf(value)
    @TypeConverter fun fromPaymentMethod(value: PaymentMethod): String = value.name
    @TypeConverter fun toPaymentMethod(value: String): PaymentMethod = PaymentMethod.valueOf(value)
}
