/**
 * MoreScreen.kt - Pestaña "Más": todo lo que perdió su botón de dashboard.
 *
 * Perfil del usuario en sesión + accesos agrupados:
 * - Para todos: Repuestos, Turnos, Historial de servicios
 *   (Clientes subió a la barra inferior; Repuestos bajó aquí).
 * - Solo ADMIN: Comisiones, Reportes, Catálogos, Usuarios, Respaldos
 *   (aquí solo se ocultan; el bloqueo real sigue en el guard del grafo
 *   con `Routes.ADMIN_ROUTES`).
 * - Cerrar sesión al pie.
 */
package com.example.serviaux.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.serviaux.ServiauxApp
import com.example.serviaux.data.entity.UserRole
import com.example.serviaux.ui.theme.Indigo700
import com.example.serviaux.ui.theme.Neutral100

@Composable
fun MoreScreen(
    onNavigateToParts: () -> Unit,
    onNavigateToAppointments: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToCommissions: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToCatalogSettings: () -> Unit,
    onNavigateToUsers: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as ServiauxApp).container }
    val currentUser by container.sessionManager.currentUser.collectAsStateWithLifecycle()
    val isAdmin = currentUser?.role == UserRole.ADMIN

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // ── Perfil ──
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Indigo700),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials(currentUser?.name ?: ""),
                    style = MaterialTheme.typography.titleMedium,
                    color = Neutral100
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = currentUser?.name ?: "",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = currentUser?.role?.displayName ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Accesos para todos ──
        MoreSection {
            MoreItem("Repuestos", onNavigateToParts)
            MoreItemDivider()
            MoreItem("Turnos", onNavigateToAppointments)
            MoreItemDivider()
            MoreItem("Historial de servicios", onNavigateToHistory)
        }

        // ── Solo administración ──
        if (isAdmin) {
            Spacer(modifier = Modifier.height(12.dp))
            MoreSection {
                MoreItem("Comisiones", onNavigateToCommissions)
                MoreItemDivider()
                MoreItem("Reportes", onNavigateToReports)
                MoreItemDivider()
                MoreItem("Catálogos", onNavigateToCatalogSettings)
                MoreItemDivider()
                MoreItem("Usuarios del taller", onNavigateToUsers)
                MoreItemDivider()
                MoreItem("Respaldos", onNavigateToBackup)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = {
                container.authRepository.logout()
                onLogout()
            }
        ) {
            Text(
                text = "Cerrar sesión",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** Tarjeta contenedora de una lista de accesos. */
@Composable
private fun MoreSection(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
private fun MoreItem(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .defaultMinSize(minHeight = 52.dp)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MoreItemDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(horizontal = 18.dp)
    )
}

private fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        .take(2).map { it.first().uppercaseChar() }.joinToString("")
        .ifEmpty { "?" }
