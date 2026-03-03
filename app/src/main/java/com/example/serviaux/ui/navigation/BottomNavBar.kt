package com.example.serviaux.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Routes.DASHBOARD, "Inicio", Icons.Default.Dashboard),
    BottomNavItem(Routes.WORK_ORDER_LIST_BASE, "Órdenes", Icons.Default.Build),
    BottomNavItem(Routes.APPOINTMENT_LIST, "Turnos", Icons.Default.CalendarMonth),
    BottomNavItem(Routes.CUSTOMER_LIST, "Clientes", Icons.Default.People),
    BottomNavItem(Routes.VEHICLE_LIST, "Vehículos", Icons.Default.DirectionsCar)
)

@Composable
fun ServiauxBottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            val selected = when {
                item.route == Routes.WORK_ORDER_LIST_BASE ->
                    currentRoute?.startsWith("workorders") == true && currentRoute == "workorders" || currentRoute?.startsWith("workorders?") == true
                item.route == Routes.APPOINTMENT_LIST ->
                    currentRoute?.startsWith("appointments") == true
                else -> currentRoute == item.route
            }
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}
