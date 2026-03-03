/**
 * AppContainer.kt - Contenedor de inyección de dependencias manual.
 *
 * Reemplaza a Hilt/Dagger para mantener compatibilidad con AGP 9.x.
 * Instancia la base de datos, el gestor de sesión y todos los repositorios
 * del sistema. Se inicializa una vez en [ServiauxApp.onCreate].
 *
 * Los ViewModels acceden a este contenedor mediante
 * `(application as ServiauxApp).container`.
 */
package com.example.serviaux.di

import android.content.Context
import com.example.serviaux.data.ServiauxDatabase
import com.example.serviaux.repository.*
import com.example.serviaux.util.SessionManager

/**
 * Contenedor de dependencias de la aplicación.
 *
 * Mantiene instancias singleton de la BD, el [SessionManager] y todos los repositorios.
 */
class AppContainer(context: Context) {
    val database = ServiauxDatabase.getInstance(context)
    val sessionManager = SessionManager(context)

    val authRepository = AuthRepository(database.userDao(), sessionManager)
    val customerRepository = CustomerRepository(database.customerDao())
    val vehicleRepository = VehicleRepository(database.vehicleDao())
    val partRepository = PartRepository(database.partDao())
    val catalogRepository = CatalogRepository(database.catalogDao())
    val workOrderRepository = WorkOrderRepository(
        workOrderDao = database.workOrderDao(),
        serviceLineDao = database.serviceLineDao(),
        workOrderPartDao = database.workOrderPartDao(),
        workOrderPaymentDao = database.workOrderPaymentDao(),
        workOrderStatusLogDao = database.workOrderStatusLogDao(),
        partDao = database.partDao(),
        workOrderMechanicDao = database.workOrderMechanicDao()
    )
    val appointmentRepository = AppointmentRepository(database.appointmentDao())
    val commissionRepository = CommissionRepository(database.workOrderMechanicDao(), database.userDao())
    val backupRepository = BackupRepository(database)
}
