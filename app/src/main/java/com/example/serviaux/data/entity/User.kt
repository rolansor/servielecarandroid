/**
 * User.kt - Entidad de usuario del sistema.
 *
 * Representa a un usuario que puede iniciar sesión en la aplicación.
 * El campo [username] tiene índice único para evitar duplicados.
 * La contraseña se almacena como hash SHA-256 con salt (ver [SecurityUtils]).
 */
package com.example.serviaux.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Usuario del sistema Serviaux.
 *
 * @property id Identificador autogenerado.
 * @property name Nombre completo para mostrar en la UI.
 * @property username Nombre de usuario único para el inicio de sesión.
 * @property role Rol que determina los permisos del usuario.
 * @property passwordHash Hash SHA-256 con salt de la contraseña.
 * @property active Indica si el usuario puede iniciar sesión; los inactivos son ignorados en el login.
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val username: String,
    val role: UserRole,
    val passwordHash: String,
    val commissionType: String = "NINGUNA",
    val commissionValue: Double = 0.0,
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
