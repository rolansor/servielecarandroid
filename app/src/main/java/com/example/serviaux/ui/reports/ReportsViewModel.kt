/**
 * ReportsViewModel.kt - ViewModel del módulo de reportes.
 *
 * Solo accesible para administradores. Genera el reporte del período elegido
 * (mes actual, 90 días, año en curso o rango personalizado):
 * - Total facturado y comparación contra el período anterior equivalente.
 * - Barras de facturación por semana (mes/90 días) o por mes (año).
 * - Desglose por origen: mano de obra / repuestos / extras.
 * - Repuestos más utilizados con cantidades.
 *
 * El criterio de fecha es `createdAt`, el mismo que usaba el reporte anterior
 * (getByDateRange/getTotalByDateRange); el facturado suma `total` de todas
 * las órdenes del período.
 */
package com.example.serviaux.ui.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.serviaux.ServiauxApp
import com.example.serviaux.data.entity.Part
import com.example.serviaux.data.entity.WorkOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class ReportPeriod { MES, DIAS90, ANIO, PERSONALIZADO }

/** Una barra del gráfico: etiqueta corta (S1, Ene…) y monto facturado. */
data class ChartBucket(val label: String, val amount: Double)

data class ReportsUiState(
    val period: ReportPeriod = ReportPeriod.MES,
    val dateFrom: Long = 0L,
    val dateTo: Long = 0L,
    val orders: List<WorkOrder> = emptyList(),
    val totalRevenue: Double = 0.0,
    /** Facturado del período anterior equivalente; null si no aplica comparar. */
    val prevRevenue: Double? = null,
    /** Texto de la comparación, ej. "vs junio", "vs 2025". */
    val prevLabel: String = "",
    val laborTotal: Double = 0.0,
    val partsTotal: Double = 0.0,
    val extrasTotal: Double = 0.0,
    val buckets: List<ChartBucket> = emptyList(),
    val topParts: List<Pair<Part, Long>> = emptyList(),
    val isLoading: Boolean = false
)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<ServiauxApp>()
    private val workOrderRepo get() = app.container.workOrderRepository
    private val partRepo get() = app.container.partRepository

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        selectPeriod(ReportPeriod.MES)
    }

    fun selectPeriod(period: ReportPeriod) {
        val now = System.currentTimeMillis()
        val (from, to) = when (period) {
            ReportPeriod.MES -> startOfCurrentMonth() to now
            ReportPeriod.DIAS90 -> startOfDay(now - 90L * DAY_MS) to now
            ReportPeriod.ANIO -> startOfCurrentYear() to now
            // Elegir…: conserva el rango que hubiera; los pickers lo ajustan después
            ReportPeriod.PERSONALIZADO ->
                (_uiState.value.dateFrom.takeIf { it > 0 } ?: startOfDay(now - 30L * DAY_MS)) to
                    (_uiState.value.dateTo.takeIf { it > 0 } ?: now)
        }
        _uiState.update { it.copy(period = period, dateFrom = from, dateTo = to) }
        loadReport()
    }

    /** Usado por los pickers del modo "Elegir…". */
    fun setDateRange(from: Long, to: Long) {
        _uiState.update { it.copy(period = ReportPeriod.PERSONALIZADO, dateFrom = from, dateTo = to) }
        loadReport()
    }

    fun loadReport() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val orders = workOrderRepo.getByDateRange(state.dateFrom, state.dateTo)
                    .firstOrNull() ?: emptyList()

                val (prevFrom, prevTo) = previousRange(state.period, state.dateFrom, state.dateTo)
                val prevRevenue = workOrderRepo.getTotalByDateRange(prevFrom, prevTo).firstOrNull()

                val topPartsRaw = workOrderRepo.getTopParts(state.dateFrom, state.dateTo, limit = 5)
                val topParts = topPartsRaw.mapNotNull { topPart ->
                    partRepo.getByIdDirect(topPart.partId)?.let { Pair(it, topPart.totalQty) }
                }

                _uiState.update {
                    it.copy(
                        orders = orders,
                        totalRevenue = orders.sumOf { o -> o.total },
                        prevRevenue = prevRevenue,
                        prevLabel = previousLabel(state.period, state.dateFrom),
                        laborTotal = orders.sumOf { o -> o.totalLabor },
                        partsTotal = orders.sumOf { o -> o.totalParts },
                        extrasTotal = orders.sumOf { o -> o.totalExtras },
                        buckets = buildBuckets(orders, state.period, state.dateFrom, state.dateTo),
                        topParts = topParts,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // ── Rangos y comparaciones ──────────────────────────────────────────

    private fun previousRange(period: ReportPeriod, from: Long, to: Long): Pair<Long, Long> =
        when (period) {
            ReportPeriod.MES -> {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = from
                    add(Calendar.MONTH, -1)
                }
                cal.timeInMillis to (from - 1)
            }
            ReportPeriod.ANIO -> {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = from
                    add(Calendar.YEAR, -1)
                }
                cal.timeInMillis to (from - 1)
            }
            else -> {
                val len = (to - from).coerceAtLeast(DAY_MS)
                (from - len) to (from - 1)
            }
        }

    private fun previousLabel(period: ReportPeriod, from: Long): String = when (period) {
        ReportPeriod.MES -> {
            val prev = Calendar.getInstance().apply {
                timeInMillis = from
                add(Calendar.MONTH, -1)
            }
            "vs " + SimpleDateFormat("MMMM", Locale("es")).format(prev.time)
        }
        ReportPeriod.DIAS90 -> "vs 90 días previos"
        ReportPeriod.ANIO -> {
            val year = Calendar.getInstance().apply { timeInMillis = from }.get(Calendar.YEAR)
            "vs ${year - 1}"
        }
        ReportPeriod.PERSONALIZADO -> "vs período previo"
    }

    // ── Barras del gráfico ──────────────────────────────────────────────

    private fun buildBuckets(
        orders: List<WorkOrder>,
        period: ReportPeriod,
        from: Long,
        to: Long
    ): List<ChartBucket> = when (period) {
        ReportPeriod.MES -> {
            val cal = Calendar.getInstance().apply { timeInMillis = from }
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val weeks = (daysInMonth + 6) / 7
            val sums = DoubleArray(weeks)
            orders.forEach { o ->
                val day = Calendar.getInstance().apply { timeInMillis = o.createdAt }
                    .get(Calendar.DAY_OF_MONTH)
                val idx = ((day - 1) / 7).coerceIn(0, weeks - 1)
                sums[idx] += o.total
            }
            sums.mapIndexed { i, amount -> ChartBucket("S${i + 1}", amount) }
        }
        ReportPeriod.ANIO -> {
            val labels = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun",
                "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
            val sums = DoubleArray(12)
            orders.forEach { o ->
                val month = Calendar.getInstance().apply { timeInMillis = o.createdAt }
                    .get(Calendar.MONTH)
                sums[month] += o.total
            }
            sums.mapIndexed { i, amount -> ChartBucket(labels[i], amount) }
        }
        else -> {
            // Semanas contadas desde el inicio del rango (90 días o personalizado)
            val weeks = (((to - from) / WEEK_MS) + 1).toInt().coerceIn(1, 18)
            val sums = DoubleArray(weeks)
            orders.forEach { o ->
                val idx = ((o.createdAt - from) / WEEK_MS).toInt().coerceIn(0, weeks - 1)
                sums[idx] += o.total
            }
            sums.mapIndexed { i, amount -> ChartBucket("S${i + 1}", amount) }
        }
    }

    // ── Fechas auxiliares ───────────────────────────────────────────────

    private fun startOfCurrentMonth(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun startOfCurrentYear(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    companion object {
        private const val DAY_MS = 24L * 60 * 60 * 1000
        private const val WEEK_MS = 7L * DAY_MS
    }
}
