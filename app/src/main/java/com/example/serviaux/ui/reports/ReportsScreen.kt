/**
 * ReportsScreen.kt - Pantalla de reportes del sistema (rediseño índigo).
 *
 * Solo accesible para administradores. Chips de período arriba (mes actual,
 * 90 días, año, rango a elegir) y tres tarjetas:
 * - "Facturado en …": cifra grande, comparación vs período anterior y barras
 *   por semana o por mes (Compose puro, sin librerías de gráficos).
 * - "De dónde viene": desglose mano de obra / repuestos / extras.
 * - "Top repuestos": los más usados del período.
 * Los filtros del rango personalizado usan DatePickerDialog nativo.
 */
package com.example.serviaux.ui.reports

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.serviaux.ui.theme.Aqua200
import com.example.serviaux.ui.theme.Aqua600
import com.example.serviaux.ui.theme.Aqua800
import com.example.serviaux.ui.theme.ErrorContainerRed
import com.example.serviaux.ui.theme.Indigo200
import com.example.serviaux.ui.theme.Indigo600
import com.example.serviaux.ui.theme.Indigo700
import com.example.serviaux.ui.theme.Neutral200
import com.example.serviaux.ui.theme.Neutral400
import com.example.serviaux.ui.theme.OnErrorContainerRed
import com.example.serviaux.util.ExcelSheet
import com.example.serviaux.util.formatMoney
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ReportsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showExcelDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.exportError) {
        uiState.exportError?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearExportError()
        }
    }

    if (showExcelDialog) {
        ExcelSheetsDialog(
            onDismiss = { showExcelDialog = false },
            onGenerate = { sheets ->
                showExcelDialog = false
                viewModel.exportExcel(context, sheets)
            }
        )
    }

    val monthChipLabel = remember {
        SimpleDateFormat("MMMM", Locale("es")).format(Date())
            .replaceFirstChar { it.uppercase() }
    }
    val yearChipLabel = remember {
        Calendar.getInstance().get(Calendar.YEAR).toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = 40.dp,
                title = { Text("Reportes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // ── Chips de período ──
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PeriodChip(monthChipLabel, uiState.period == ReportPeriod.MES) {
                    viewModel.selectPeriod(ReportPeriod.MES)
                }
                PeriodChip("90 días", uiState.period == ReportPeriod.DIAS90) {
                    viewModel.selectPeriod(ReportPeriod.DIAS90)
                }
                PeriodChip(yearChipLabel, uiState.period == ReportPeriod.ANIO) {
                    viewModel.selectPeriod(ReportPeriod.ANIO)
                }
                PeriodChip("Elegir…", uiState.period == ReportPeriod.PERSONALIZADO) {
                    viewModel.selectPeriod(ReportPeriod.PERSONALIZADO)
                }
            }

            if (uiState.period == ReportPeriod.PERSONALIZADO) {
                Spacer(modifier = Modifier.height(8.dp))
                CustomRangePickers(
                    dateFrom = uiState.dateFrom,
                    dateTo = uiState.dateTo,
                    onRangeChange = { from, to -> viewModel.setDateRange(from, to) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            } else {
                RevenueCard(uiState, monthChipLabel, yearChipLabel)
                Spacer(modifier = Modifier.height(12.dp))
                BreakdownCard(uiState)
                Spacer(modifier = Modifier.height(12.dp))
                TopPartsCard(uiState)
                Spacer(modifier = Modifier.height(12.dp))
                ExportCard(
                    exporting = uiState.isExporting,
                    onExcel = { showExcelDialog = true },
                    onPdf = { viewModel.exportDashboardPdf(context) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PeriodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = CircleShape,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

// ── Tarjeta: facturado + gráfico ─────────────────────────────────────────

@Composable
private fun RevenueCard(uiState: ReportsUiState, monthLabel: String, yearLabel: String) {
    val periodTitle = when (uiState.period) {
        ReportPeriod.MES -> "FACTURADO EN ${monthLabel.uppercase(Locale("es"))}"
        ReportPeriod.DIAS90 -> "FACTURADO EN 90 DÍAS"
        ReportPeriod.ANIO -> "FACTURADO EN $yearLabel"
        ReportPeriod.PERSONALIZADO -> "FACTURADO EN EL PERÍODO"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionLabel(periodTitle)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatMoney(uiState.totalRevenue),
                    style = MaterialTheme.typography.headlineLarge
                )
                val prev = uiState.prevRevenue
                if (prev != null && prev > 0.0) {
                    Spacer(modifier = Modifier.width(10.dp))
                    ComparisonPill(
                        current = uiState.totalRevenue,
                        previous = prev,
                        label = uiState.prevLabel
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            val ticket = if (uiState.orders.isNotEmpty())
                uiState.totalRevenue / uiState.orders.size else 0.0
            Text(
                text = "${uiState.orders.size} " +
                    (if (uiState.orders.size == 1) "orden" else "órdenes") +
                    " · ticket promedio ${formatMoney(ticket)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (uiState.buckets.any { it.amount > 0 }) {
                Spacer(modifier = Modifier.height(16.dp))
                BarChart(uiState.buckets)
            }
        }
    }
}

@Composable
private fun ComparisonPill(current: Double, previous: Double, label: String) {
    val pct = ((current - previous) / previous * 100).roundToInt()
    val (bg, fg) = when {
        pct > 0 -> Aqua200 to Aqua800
        pct < 0 -> ErrorContainerRed to OnErrorContainerRed
        else -> Neutral200 to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val sign = if (pct > 0) "+" else ""
    Text(
        text = "$sign$pct% $label",
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun BarChart(buckets: List<ChartBucket>) {
    val max = buckets.maxOf { it.amount }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(if (buckets.size > 8) 3.dp else 8.dp)
    ) {
        buckets.forEachIndexed { index, bucket ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val frac = if (max > 0) (bucket.amount / max).toFloat() else 0f
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((8 + 104 * frac).dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (bucket.amount >= max && max > 0) Indigo700 else Indigo200
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Con muchas barras (90 días) etiquetamos una de cada cuatro
                val showLabel = buckets.size <= 8 || index % 4 == 0
                Text(
                    text = if (showLabel) bucket.label else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

// ── Tarjeta: desglose por origen ─────────────────────────────────────────

@Composable
private fun BreakdownCard(uiState: ReportsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionLabel("DE DÓNDE VIENE")
            val max = maxOf(uiState.laborTotal, uiState.partsTotal, uiState.extrasTotal, 0.01)
            BreakdownRow("Mano de obra", uiState.laborTotal, max, Indigo600)
            BreakdownRow("Repuestos", uiState.partsTotal, max, Aqua600)
            BreakdownRow("Extras", uiState.extrasTotal, max, Neutral400)
        }
    }
}

@Composable
private fun BreakdownRow(label: String, amount: Double, max: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(104.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(CircleShape)
                .background(Neutral200)
        ) {
            val frac = (amount / max).toFloat().coerceIn(0.04f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth(frac)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Text(
            text = formatMoney(amount),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 88.dp)
        )
    }
}

// ── Tarjeta: top repuestos ───────────────────────────────────────────────

@Composable
private fun TopPartsCard(uiState: ReportsUiState) {
    val title = when (uiState.period) {
        ReportPeriod.MES -> "TOP REPUESTOS DEL MES"
        ReportPeriod.ANIO -> "TOP REPUESTOS DEL AÑO"
        else -> "TOP REPUESTOS DEL PERÍODO"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionLabel(title)
            Spacer(modifier = Modifier.height(8.dp))
            if (uiState.topParts.isEmpty()) {
                Text(
                    text = "Sin repuestos usados en el período",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                uiState.topParts.forEachIndexed { index, (part, qty) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}. ${part.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$qty uds",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (index < uiState.topParts.size - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

// ── Tarjeta: exportar ────────────────────────────────────────────────────

@Composable
private fun ExportCard(
    exporting: Boolean,
    onExcel: () -> Unit,
    onPdf: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                SectionLabel("EXPORTAR")
                if (exporting) {
                    Spacer(modifier = Modifier.width(10.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.height(14.dp).width(14.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            ExportRow(
                icon = { Icon(Icons.Default.GridOn, contentDescription = null, tint = Indigo700) },
                title = "Excel del período",
                subtitle = "Órdenes, mecánicos, trabajos, repuestos, tipos y clientes",
                enabled = !exporting,
                onClick = onExcel
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            ExportRow(
                icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Indigo700) },
                title = "PDF resumen",
                subtitle = "Dashboard del período para compartir o imprimir",
                enabled = !exporting,
                onClick = onPdf
            )
        }
    }
}

@Composable
private fun ExportRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        icon()
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExcelSheetsDialog(
    onDismiss: () -> Unit,
    onGenerate: (Set<ExcelSheet>) -> Unit
) {
    var selected by remember { mutableStateOf(ExcelSheet.entries.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excel del período") },
        text = {
            Column {
                Text(
                    text = "Elija las hojas que llevará el archivo:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                ExcelSheet.entries.forEach { sheet ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = if (sheet in selected) selected - sheet else selected + sheet
                            }
                    ) {
                        Checkbox(
                            checked = sheet in selected,
                            onCheckedChange = { checked ->
                                selected = if (checked) selected + sheet else selected - sheet
                            }
                        )
                        Text(sheet.title, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onGenerate(selected) },
                enabled = selected.isNotEmpty()
            ) {
                Text("Generar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// ── Auxiliares ───────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun CustomRangePickers(
    dateFrom: Long,
    dateTo: Long,
    onRangeChange: (Long, Long) -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("es")) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = dateFormat.format(Date(dateFrom)),
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Desde") },
                trailingIcon = {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Seleccionar fecha")
                },
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable {
                        val calendar = Calendar.getInstance().apply { timeInMillis = dateFrom }
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val cal = Calendar.getInstance().apply {
                                    set(year, month, dayOfMonth, 0, 0, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                onRangeChange(cal.timeInMillis, dateTo)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = dateFormat.format(Date(dateTo)),
                onValueChange = {},
                readOnly = true,
                enabled = false,
                label = { Text("Hasta") },
                trailingIcon = {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Seleccionar fecha")
                },
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable {
                        val calendar = Calendar.getInstance().apply { timeInMillis = dateTo }
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val cal = Calendar.getInstance().apply {
                                    set(year, month, dayOfMonth, 23, 59, 59)
                                    set(Calendar.MILLISECOND, 999)
                                }
                                onRangeChange(dateFrom, cal.timeInMillis)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
            )
        }
    }
}
