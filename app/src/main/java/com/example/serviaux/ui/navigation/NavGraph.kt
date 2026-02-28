package com.example.serviaux.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.serviaux.ui.auth.LoginScreen
import com.example.serviaux.ui.backup.BackupScreen
import com.example.serviaux.ui.customers.CustomerDetailScreen
import com.example.serviaux.ui.customers.CustomerFormScreen
import com.example.serviaux.ui.customers.CustomerListScreen
import com.example.serviaux.ui.dashboard.DashboardScreen
import com.example.serviaux.ui.parts.PartFormScreen
import com.example.serviaux.ui.parts.PartListScreen
import com.example.serviaux.ui.reports.ReportsScreen
import com.example.serviaux.ui.settings.CatalogSettingsScreen
import com.example.serviaux.ui.users.UserFormScreen
import com.example.serviaux.ui.users.UserListScreen
import com.example.serviaux.ui.vehicles.VehicleDetailScreen
import com.example.serviaux.ui.vehicles.VehicleFormScreen
import com.example.serviaux.ui.vehicles.VehicleListScreen
import com.example.serviaux.ui.workorders.WorkOrderDetailScreen
import com.example.serviaux.ui.workorders.WorkOrderFormScreen
import com.example.serviaux.ui.workorders.WorkOrderListScreen
import com.example.serviaux.data.entity.OrderStatus

@Composable
fun ServiauxNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.LOGIN) {

        // Login
        composable(Routes.LOGIN) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Routes.DASHBOARD) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            })
        }

        // Dashboard
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNavigateToCustomers = { navController.navigate(Routes.CUSTOMER_LIST) },
                onNavigateToVehicles = { navController.navigate(Routes.VEHICLE_LIST) },
                onNavigateToOrders = { navController.navigate(Routes.workOrderList()) },
                onNavigateToOrdersByStatus = { status -> navController.navigate(Routes.workOrderList(status.name)) },
                onNavigateToParts = { navController.navigate(Routes.PART_LIST) },
                onNavigateToUsers = { navController.navigate(Routes.USER_LIST) },
                onNavigateToReports = { navController.navigate(Routes.REPORTS) },
                onNavigateToCatalogSettings = { navController.navigate(Routes.CATALOG_SETTINGS) },
                onNavigateToNewOrder = { navController.navigate(Routes.WORK_ORDER_FORM) },
                onNavigateToNewCustomer = { navController.navigate(Routes.customerForm()) },
                onNavigateToBackup = { navController.navigate(Routes.BACKUP) },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Customer routes
        composable(Routes.CUSTOMER_LIST) {
            CustomerListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { navController.navigate(Routes.customerDetail(it)) },
                onNavigateToForm = { navController.navigate(Routes.customerForm(it)) }
            )
        }

        composable(
            Routes.CUSTOMER_DETAIL,
            arguments = listOf(navArgument("customerId") { type = NavType.LongType })
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getLong("customerId") ?: return@composable
            CustomerDetailScreen(
                customerId = customerId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { navController.navigate(Routes.customerForm(it)) },
                onNavigateToVehicle = { navController.navigate(Routes.vehicleDetail(it)) },
                onNavigateToNewVehicle = { navController.navigate(Routes.vehicleForm(customerId = it)) }
            )
        }

        composable(
            Routes.CUSTOMER_FORM,
            arguments = listOf(
                navArgument("customerId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId")?.toLongOrNull()
            CustomerFormScreen(
                customerId = customerId,
                onNavigateBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        // Vehicle routes
        composable(Routes.VEHICLE_LIST) {
            VehicleListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { navController.navigate(Routes.vehicleDetail(it)) },
                onNavigateToForm = { navController.navigate(Routes.vehicleForm()) }
            )
        }

        composable(
            Routes.VEHICLE_DETAIL,
            arguments = listOf(navArgument("vehicleId") { type = NavType.LongType })
        ) { backStackEntry ->
            val vehicleId = backStackEntry.arguments?.getLong("vehicleId") ?: return@composable
            VehicleDetailScreen(
                vehicleId = vehicleId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { navController.navigate(Routes.vehicleForm(vehicleId = it)) },
                onNavigateToOrder = { navController.navigate(Routes.workOrderDetail(it)) },
                onNavigateToCustomer = { navController.navigate(Routes.customerDetail(it)) }
            )
        }

        composable(
            Routes.VEHICLE_FORM,
            arguments = listOf(
                navArgument("customerId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("vehicleId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getString("customerId")?.toLongOrNull()
            val vehicleId = backStackEntry.arguments?.getString("vehicleId")?.toLongOrNull()
            VehicleFormScreen(
                vehicleId = vehicleId,
                preselectedCustomerId = customerId,
                onNavigateBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        // Work Order routes
        composable(
            Routes.WORK_ORDER_LIST,
            arguments = listOf(navArgument("status") { type = NavType.StringType; defaultValue = "" })
        ) { backStackEntry ->
            val statusStr = backStackEntry.arguments?.getString("status") ?: ""
            val initialFilter = try { OrderStatus.valueOf(statusStr) } catch (_: Exception) { null }
            WorkOrderListScreen(
                initialFilter = initialFilter,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetail = { navController.navigate(Routes.workOrderDetail(it)) },
                onNavigateToForm = { navController.navigate(Routes.WORK_ORDER_FORM) }
            )
        }

        composable(
            Routes.WORK_ORDER_DETAIL,
            arguments = listOf(navArgument("orderId") { type = NavType.LongType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getLong("orderId") ?: return@composable
            WorkOrderDetailScreen(
                orderId = orderId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { navController.navigate(Routes.workOrderEdit(it)) }
            )
        }

        composable(Routes.WORK_ORDER_FORM) {
            WorkOrderFormScreen(
                onNavigateBack = { navController.popBackStack() },
                onOrderCreated = { orderId ->
                    navController.navigate(Routes.workOrderDetail(orderId)) {
                        popUpTo(Routes.WORK_ORDER_FORM) { inclusive = true }
                    }
                }
            )
        }

        composable(
            Routes.WORK_ORDER_EDIT,
            arguments = listOf(navArgument("orderId") { type = NavType.LongType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getLong("orderId") ?: return@composable
            WorkOrderFormScreen(
                orderId = orderId,
                onNavigateBack = { navController.popBackStack() },
                onOrderCreated = { id ->
                    navController.popBackStack()
                }
            )
        }

        // Part routes
        composable(Routes.PART_LIST) {
            PartListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToForm = { navController.navigate(Routes.partForm(it)) }
            )
        }

        composable(
            Routes.PART_FORM,
            arguments = listOf(
                navArgument("partId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val partId = backStackEntry.arguments?.getString("partId")?.toLongOrNull()
            PartFormScreen(
                partId = partId,
                onNavigateBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        // User routes
        composable(Routes.USER_LIST) {
            UserListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToForm = { navController.navigate(Routes.userForm(it)) }
            )
        }

        composable(
            Routes.USER_FORM,
            arguments = listOf(
                navArgument("userId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")?.toLongOrNull()
            UserFormScreen(
                userId = userId,
                onNavigateBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        // Reports
        composable(Routes.REPORTS) {
            ReportsScreen(onNavigateBack = { navController.popBackStack() })
        }

        // Catalog Settings
        composable(Routes.CATALOG_SETTINGS) {
            CatalogSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Backup
        composable(Routes.BACKUP) {
            BackupScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
