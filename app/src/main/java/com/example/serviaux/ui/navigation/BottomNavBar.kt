/**
 * BottomNavBar.kt - Barra de navegación inferior del rediseño.
 *
 * Cinco destinos fijos: Taller · Órdenes · Autos · Clientes · Más.
 * Lo administrativo vive dentro de "Más" (solo visible para ADMIN allí);
 * Turnos, Historial y Repuestos también viven en "Más".
 */
package com.example.serviaux.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Routes.DASHBOARD, "Taller", Icons.Default.Garage),
    BottomNavItem(Routes.WORK_ORDER_LIST_BASE, "Órdenes", Icons.Default.Build),
    BottomNavItem(Routes.VEHICLE_LIST, "Autos", Icons.Default.DirectionsCar),
    BottomNavItem(Routes.CUSTOMER_LIST, "Clientes", Icons.Default.People),
    BottomNavItem(Routes.MORE, "Más", Icons.Default.MoreHoriz)
)

@Composable
fun ServiauxBottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            val selected = when (item.route) {
                Routes.WORK_ORDER_LIST_BASE ->
                    currentRoute == "workorders" || currentRoute?.startsWith("workorders?") == true
                else -> currentRoute == item.route
            }
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                // El diseño pinta la pestaña activa en índigo; el default de M3
                // usa secondaryContainer (verde-agua en este tema).
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}
