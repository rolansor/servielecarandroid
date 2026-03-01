/**
 * ServiauxApp.kt - Clase Application de Serviaux.
 *
 * Punto de entrada de la aplicación. Inicializa el [AppContainer]
 * que contiene todas las dependencias (BD, repositorios, sesión).
 */
package com.example.serviaux

import android.app.Application
import com.example.serviaux.di.AppContainer

/**
 * Aplicación Serviaux.
 *
 * Expone el [container] de dependencias accesible desde cualquier
 * ViewModel mediante `(application as ServiauxApp).container`.
 */
class ServiauxApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
