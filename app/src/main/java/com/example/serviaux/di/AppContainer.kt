package com.example.serviaux.di

import android.content.Context
import com.example.serviaux.data.ServiauxDatabase
import com.example.serviaux.repository.*
import com.example.serviaux.util.SessionManager

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
        partDao = database.partDao()
    )
    val backupRepository = BackupRepository(database)
}
