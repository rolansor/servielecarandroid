/**
 * Converters.kt - Conversores de tipo para Room.
 *
 * Permite que Room almacene y recupere enumeraciones del dominio
 * ([UserRole], [OrderStatus], [Priority], [PaymentMethod]) como cadenas
 * de texto en SQLite, utilizando el nombre del enum como valor persistido.
 */
package com.example.serviaux.data

import androidx.room.TypeConverter
import com.example.serviaux.data.entity.*

/**
 * Conversores bidireccionales entre enums del dominio y su representación String en la BD.
 *
 * Registrado globalmente en [ServiauxDatabase] mediante `@TypeConverters`.
 */
class Converters {
    @TypeConverter fun fromUserRole(value: UserRole): String = value.name
    @TypeConverter fun toUserRole(value: String): UserRole = UserRole.valueOf(value)
    @TypeConverter fun fromOrderStatus(value: OrderStatus?): String? = value?.name
    @TypeConverter fun toOrderStatus(value: String?): OrderStatus? = value?.let { OrderStatus.valueOf(it) }
    @TypeConverter fun fromPriority(value: Priority): String = value.name
    @TypeConverter fun toPriority(value: String): Priority = Priority.valueOf(value)
    @TypeConverter fun fromPaymentMethod(value: PaymentMethod): String = value.name
    @TypeConverter fun toPaymentMethod(value: String): PaymentMethod = PaymentMethod.valueOf(value)
    @TypeConverter fun fromArrivalCondition(value: ArrivalCondition): String = value.name
    @TypeConverter fun toArrivalCondition(value: String): ArrivalCondition = ArrivalCondition.valueOf(value)
    @TypeConverter fun fromOrderType(value: OrderType): String = value.name
    @TypeConverter fun toOrderType(value: String): OrderType = OrderType.valueOf(value)
}
