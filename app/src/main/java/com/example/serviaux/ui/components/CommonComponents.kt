/**
 * CommonComponents.kt - Componentes Compose reutilizables de Serviaux.
 *
 * Incluye componentes comunes usados en múltiples pantallas:
 * - [ServiauxSearchBar]: barra de búsqueda con ícono.
 * - [StatusChip]: badge coloreado del estado de una orden.
 * - [PriorityChip]: badge coloreado de la prioridad de una orden.
 * - [SectionTitle]: título de sección con estilo consistente.
 * - [InfoRow]: fila de etiqueta-valor para detalles.
 * - [EmptyState]: indicador visual cuando no hay datos.
 * - [ConfirmDialog]: diálogo de confirmación genérico.
 */
package com.example.serviaux.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.serviaux.data.entity.OrderStatus
import com.example.serviaux.data.entity.Priority
import com.example.serviaux.ui.theme.Aqua200
import com.example.serviaux.ui.theme.Aqua800
import com.example.serviaux.ui.theme.ErrorContainerRed
import com.example.serviaux.ui.theme.Indigo200
import com.example.serviaux.ui.theme.Indigo800
import com.example.serviaux.ui.theme.OnErrorContainerRed
import com.example.serviaux.ui.theme.StatusCerradoBg
import com.example.serviaux.ui.theme.StatusCerradoText
import com.example.serviaux.ui.theme.StatusDiagnosticoBg
import com.example.serviaux.ui.theme.StatusDiagnosticoText
import com.example.serviaux.ui.theme.StatusEnProcesoBg
import com.example.serviaux.ui.theme.StatusEnProcesoText
import com.example.serviaux.ui.theme.StatusEntregadoBg
import com.example.serviaux.ui.theme.StatusEntregadoText
import com.example.serviaux.ui.theme.StatusEsperaBg
import com.example.serviaux.ui.theme.StatusEsperaBorder
import com.example.serviaux.ui.theme.StatusEsperaText
import com.example.serviaux.ui.theme.StatusListoBg
import com.example.serviaux.ui.theme.StatusListoText
import com.example.serviaux.ui.theme.StatusRecibidoBg
import com.example.serviaux.ui.theme.StatusRecibidoText

/** Barra de búsqueda con ícono de lupa. Usada en listas de clientes, vehículos, repuestos, etc. */
@Composable
fun ServiauxSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String = "Buscar...",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Buscar"
            )
        },
        singleLine = true,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Chip píldora del estado de una orden, con la semántica fija del rediseño:
 * índigo sólido = pasando ahora; verde-agua = terminado; borde punteado =
 * esperando algo de fuera (único punteado del sistema); neutro = inerte.
 */
@Composable
fun StatusChip(status: OrderStatus, modifier: Modifier = Modifier) {
    val (bg, textColor) = when (status) {
        OrderStatus.RECIBIDO -> StatusRecibidoBg to StatusRecibidoText
        OrderStatus.EN_DIAGNOSTICO -> StatusDiagnosticoBg to StatusDiagnosticoText
        OrderStatus.EN_PROCESO -> StatusEnProcesoBg to StatusEnProcesoText
        OrderStatus.EN_ESPERA_REPUESTO -> StatusEsperaBg to StatusEsperaText
        OrderStatus.LISTO -> StatusListoBg to StatusListoText
        OrderStatus.ENTREGADO -> StatusEntregadoBg to StatusEntregadoText
        OrderStatus.CERRADO -> StatusCerradoBg to StatusCerradoText
    }
    val dashed = status == OrderStatus.EN_ESPERA_REPUESTO
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .then(
                if (dashed) Modifier.drawBehind {
                    val strokeWidth = 1.5.dp.toPx()
                    drawRoundRect(
                        color = StatusEsperaBorder,
                        cornerRadius = CornerRadius(size.height / 2),
                        style = Stroke(
                            width = strokeWidth,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f))
                        )
                    )
                } else Modifier
            )
    ) {
        Text(
            text = status.displayName,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

/** Chip píldora de la prioridad de una orden, tintado desde las rampas. */
@Composable
fun PriorityChip(priority: Priority, modifier: Modifier = Modifier) {
    val (bg, textColor) = when (priority) {
        Priority.ALTA -> ErrorContainerRed to OnErrorContainerRed
        Priority.MEDIA -> Indigo200 to Indigo800
        Priority.BAJA -> Aqua200 to Aqua800
    }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
    ) {
        Text(
            text = priority.displayName,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp
        )
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(0.4f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f)
        )
    }
}

/** Indicador visual con ícono y mensaje para listas vacías. */
@Composable
fun EmptyState(
    message: String,
    icon: ImageVector = Icons.Default.Inbox,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Diálogo de confirmación genérico con botones "Confirmar" y "Cancelar". */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
