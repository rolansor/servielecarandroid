/**
 * AppPreferences.kt - Preferencias operativas del taller.
 *
 * Guarda ajustes que no son datos del negocio y que por eso no viven en la base de datos:
 * hoy, el mecánico que se asigna por defecto a las órdenes nuevas.
 *
 * Se usan SharedPreferences a propósito: agregar una columna a la tabla `users` obligaría a
 * una migración de Room y la base está configurada con `fallbackToDestructiveMigration`, que
 * borraría todos los datos del taller al instalar la nueva versión.
 */
package com.example.serviaux.util

import android.content.Context

/** Preferencias de la aplicación, accedidas por nombre de archivo propio. */
object AppPreferences {

    private const val PREFS = "serviaux_prefs"
    private const val KEY_DEFAULT_MECHANIC = "default_mechanic_id"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Mecánico que se asigna automáticamente a las órdenes nuevas; null si no hay ninguno. */
    fun defaultMechanicId(context: Context): Long? {
        val id = prefs(context).getLong(KEY_DEFAULT_MECHANIC, -1L)
        return if (id <= 0L) null else id
    }

    /** Define el mecánico por defecto, o lo quita si se pasa null. */
    fun setDefaultMechanicId(context: Context, mechanicId: Long?) {
        prefs(context).edit().apply {
            if (mechanicId == null) remove(KEY_DEFAULT_MECHANIC)
            else putLong(KEY_DEFAULT_MECHANIC, mechanicId)
        }.apply()
    }
}
