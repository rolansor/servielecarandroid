package com.example.serviaux

import android.app.Application
import com.example.serviaux.di.AppContainer

class ServiauxApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
