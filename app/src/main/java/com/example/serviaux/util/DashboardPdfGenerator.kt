/**
 * DashboardPdfGenerator.kt - PDF "Resumen del período" (dashboard).
 *
 * Una o dos páginas A4 con lo importante del período elegido en Reportes:
 * facturado con comparación contra el período anterior, nº de órdenes y
 * ticket promedio, desglose mano de obra / repuestos / extras con barras,
 * top trabajos y top repuestos, tabla por mecánico y saldos pendientes.
 *
 * Sigue la línea gráfica índigo de los demás generadores (rampa en
 * ui/theme/Color.kt) y la misma estructura de cabecera/pie.
 */
package com.example.serviaux.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.serviaux.R
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Fila del top de trabajos o repuestos: nombre, veces/cantidad y total. */
data class DashboardTopRow(val name: String, val count: Long, val total: Double)

/** Fila de la tabla por mecánico. */
data class DashboardMechanicRow(
    val name: String,
    val orders: Int,
    val generated: Double,
    val paid: Double
) {
    val pending: Double get() = generated - paid
}

/** Datos consolidados del dashboard; los calcula el ViewModel. */
data class DashboardPdfData(
    val from: Long,
    val to: Long,
    val totalRevenue: Double,
    val prevRevenue: Double?,
    val prevLabel: String,
    val orderCount: Int,
    val laborTotal: Double,
    val partsTotal: Double,
    val extrasTotal: Double,
    val topJobs: List<DashboardTopRow>,
    val topParts: List<DashboardTopRow>,
    val mechanics: List<DashboardMechanicRow>,
    val pendingBalance: Double,
    val pendingOrders: Int
)

object DashboardPdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val ML = 36f
    private const val MR = 559f
    private const val MT = 36f
    private const val MB = 806f

    private val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale("es"))
    private val dateTimeFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es"))
    private val fileFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // Línea gráfica del rediseño índigo (rampa en ui/theme/Color.kt)
    private const val COL_ACCENT = 0xFF4A4BAB.toInt()          // Indigo700
    private const val COL_HEADER_BG = 0xFF363688.toInt()       // Indigo800
    private const val COL_HEADER_TEXT = 0xFFF7F6FB.toInt()     // Neutral100
    private const val COL_TABLE_HEADER_BG = 0xFFECEBF3.toInt() // Neutral200
    private const val COL_ALT_ROW = 0xFFF7F6FB.toInt()         // Neutral100
    private const val COL_TEXT = 0xFF2B2934.toInt()            // Neutral900
    private const val COL_TEXT_BOLD = 0xFF1E1C26.toInt()       // TextInk
    private const val COL_MUTED = 0xFF605D6F.toInt()           // Neutral700
    private const val COL_TOTAL_BG = 0xFFE3F6F2.toInt()        // Aqua100
    private const val COL_TOTAL_TEXT = 0xFF244841.toInt()      // Aqua800
    private const val COL_DIVIDER = 0xFFD9D7E4.toInt()         // Neutral300
    private const val COL_AQUA = 0xFF47857A.toInt()            // Aqua600
    private const val COL_NEUTRAL_BAR = 0xFFBAB7C8.toInt()     // Neutral400
    private const val COL_PENDING = 0xFF363688.toInt()         // Indigo800 (saldo pendiente)

    fun generate(context: Context, data: DashboardPdfData): File {
        val doc = PdfDocument()
        try {
            var pageNum = 1
            var page = doc.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
            )
            var c = page.canvas
            var y = MT

            val pTitle = paint(20f, COL_TEXT_BOLD, bold = true)
            val pSubtitle = paint(10f, COL_MUTED)
            val pSectionHeader = paint(10f, COL_HEADER_TEXT, bold = true)
            val pBody = paint(9f, COL_TEXT)
            val pBodyBold = paint(9f, COL_TEXT, bold = true)
            val pSmall = paint(7.5f, COL_MUTED)
            val pTableHeader = paint(8.5f, COL_TEXT_BOLD, bold = true)
            val pBig = paint(26f, COL_TEXT_BOLD, bold = true)

            val bgAccent = fill(COL_ACCENT)
            val bgHeader = fill(COL_HEADER_BG)
            val bgTableHead = fill(COL_TABLE_HEADER_BG)
            val bgAltRow = fill(COL_ALT_ROW)
            val bgTotal = fill(COL_TOTAL_BG)
            val pLine = Paint().apply { color = COL_DIVIDER; strokeWidth = 0.6f; isAntiAlias = true }

            fun newPage() {
                doc.finishPage(page)
                pageNum++
                page = doc.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
                )
                c = page.canvas
                y = MT
            }

            fun ensureSpace(need: Float) {
                if (y + need > MB) newPage()
            }

            fun sectionHeader(title: String) {
                ensureSpace(60f)
                c.drawRoundRect(RectF(ML, y, MR, y + 16f), 3f, 3f, bgHeader)
                c.drawText(title, ML + 8f, y + 11.5f, pSectionHeader)
                y += 28f
            }

            fun rightAlignAt(text: String, rightX: Float, atY: Float, p: Paint) {
                c.drawText(text, rightX - p.measureText(text), atY, p)
            }

            // ══ Cabecera ══
            c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 5f, bgAccent)
            y = MT + 5f
            val logo = getLogoBitmap(context, 128)
            if (logo != null) {
                val logoRect = RectF(ML, y - 4f, ML + 56f, y + 52f)
                c.drawBitmap(logo, null, logoRect, Paint().apply {
                    isFilterBitmap = true; isAntiAlias = true
                })
            }
            val lOff = if (logo != null) 64f else 0f
            c.drawText("SERVIELECAR", ML + lOff, y + 16f, pTitle)
            c.drawText("Taller Automotriz", ML + lOff, y + 28f, pSubtitle)

            val pDocTitle = paint(14f, COL_ACCENT, bold = true)
            rightAlignAt("Resumen del período", MR, y + 16f, pDocTitle)
            rightAlignAt(
                "${dateFmt.format(Date(data.from))} — ${dateFmt.format(Date(data.to))}",
                MR, y + 30f, pSubtitle
            )
            y += 62f

            // ══ Facturado ══
            sectionHeader("FACTURADO")
            c.drawText(formatMoney(data.totalRevenue), ML, y + 12f, pBig)
            val infoX = ML + pBig.measureText(formatMoney(data.totalRevenue)) + 14f
            val prev = data.prevRevenue
            if (prev != null && prev > 0.0) {
                val pct = ((data.totalRevenue - prev) / prev * 100).roundToInt()
                val sign = if (pct > 0) "+" else ""
                val pillText = "$sign$pct% ${data.prevLabel}"
                val pillPaint = paint(9f, if (pct >= 0) COL_TOTAL_TEXT else 0xFFBA1A1A.toInt(), bold = true)
                val pillBg = fill(if (pct >= 0) COL_TOTAL_BG else 0xFFFFDAD6.toInt())
                val w = pillPaint.measureText(pillText)
                c.drawRoundRect(RectF(infoX, y - 2f, infoX + w + 16f, y + 14f), 8f, 8f, pillBg)
                c.drawText(pillText, infoX + 8f, y + 9.5f, pillPaint)
            }
            y += 30f
            val ticket = if (data.orderCount > 0) data.totalRevenue / data.orderCount else 0.0
            c.drawText(
                "${data.orderCount} " +
                    (if (data.orderCount == 1) "orden" else "órdenes") +
                    " en el período · ticket promedio ${formatMoney(ticket)}",
                ML, y, pBody
            )
            y += 22f

            // ══ De dónde viene ══
            sectionHeader("DE DÓNDE VIENE")
            val maxSource = maxOf(data.laborTotal, data.partsTotal, data.extrasTotal, 0.01)
            val barLeft = ML + 92f
            val barRight = MR - 78f
            val barWidth = barRight - barLeft
            fun sourceBar(label: String, amount: Double, color: Int) {
                ensureSpace(18f)
                c.drawText(label, ML, y + 7f, pBody)
                c.drawRoundRect(RectF(barLeft, y, barRight, y + 8f), 4f, 4f, fill(COL_TABLE_HEADER_BG))
                val frac = (amount / maxSource).toFloat().coerceIn(0.03f, 1f)
                c.drawRoundRect(
                    RectF(barLeft, y, barLeft + barWidth * frac, y + 8f), 4f, 4f, fill(color)
                )
                rightAlignAt(formatMoney(amount), MR, y + 7f, pBodyBold)
                y += 18f
            }
            sourceBar("Mano de obra", data.laborTotal, COL_ACCENT)
            sourceBar("Repuestos", data.partsTotal, COL_AQUA)
            sourceBar("Extras", data.extrasTotal, COL_NEUTRAL_BAR)
            y += 8f

            // ══ Tops ══
            fun topTable(title: String, rows: List<DashboardTopRow>, unit: String) {
                sectionHeader(title)
                if (rows.isEmpty()) {
                    c.drawText("Sin datos en el período", ML, y, pBody)
                    y += 18f
                    return
                }
                c.drawRect(ML, y, MR, y + 16f, bgTableHead)
                c.drawText("Descripción", ML + 6f, y + 11f, pTableHeader)
                rightAlignAt(unit, MR - 96f, y + 11f, pTableHeader)
                rightAlignAt("Total", MR - 6f, y + 11f, pTableHeader)
                y += 20f
                rows.forEachIndexed { i, rowData ->
                    ensureSpace(15f)
                    if (i % 2 == 1) c.drawRect(ML, y - 10f, MR, y + 4f, bgAltRow)
                    c.drawText(
                        truncate("${i + 1}. ${rowData.name}", pBody, MR - ML - 180f),
                        ML + 6f, y, pBody
                    )
                    rightAlignAt("${rowData.count}", MR - 96f, y, pBody)
                    rightAlignAt(formatMoney(rowData.total), MR - 6f, y, pBody)
                    y += 15f
                }
                y += 10f
            }
            topTable("TOP TRABAJOS", data.topJobs, "Veces")
            topTable("TOP REPUESTOS", data.topParts, "Cant.")

            // ══ Por mecánico ══
            sectionHeader("POR MECÁNICO")
            if (data.mechanics.isEmpty()) {
                c.drawText("Sin mecánicos con órdenes en el período", ML, y, pBody)
                y += 18f
            } else {
                c.drawRect(ML, y, MR, y + 16f, bgTableHead)
                c.drawText("Mecánico", ML + 6f, y + 11f, pTableHeader)
                rightAlignAt("Órdenes", ML + 250f, y + 11f, pTableHeader)
                rightAlignAt("Generada", ML + 340f, y + 11f, pTableHeader)
                rightAlignAt("Pagada", ML + 430f, y + 11f, pTableHeader)
                rightAlignAt("Pendiente", MR - 6f, y + 11f, pTableHeader)
                y += 20f
                data.mechanics.forEachIndexed { i, m ->
                    ensureSpace(15f)
                    if (i % 2 == 1) c.drawRect(ML, y - 10f, MR, y + 4f, bgAltRow)
                    c.drawText(truncate(m.name, pBody, 190f), ML + 6f, y, pBody)
                    rightAlignAt("${m.orders}", ML + 250f, y, pBody)
                    rightAlignAt(formatMoney(m.generated), ML + 340f, y, pBody)
                    rightAlignAt(formatMoney(m.paid), ML + 430f, y, pBody)
                    rightAlignAt(formatMoney(m.pending), MR - 6f, y, pBodyBold)
                    y += 15f
                }
                y += 10f
            }

            // ══ Saldos ══
            ensureSpace(46f)
            val pendingPaint = paint(12f, COL_PENDING, bold = true)
            val okPaint = paint(12f, COL_TOTAL_TEXT, bold = true)
            c.drawRoundRect(RectF(ML, y, MR, y + 34f), 6f, 6f, bgTotal)
            if (data.pendingBalance > 0.01) {
                c.drawText("Pendiente de cobro del período", ML + 10f, y + 21f, pBodyBold)
                rightAlignAt(
                    "${formatMoney(data.pendingBalance)} en ${data.pendingOrders} " +
                        (if (data.pendingOrders == 1) "orden" else "órdenes"),
                    MR - 10f, y + 21f, pendingPaint
                )
            } else {
                c.drawText("Cartera del período", ML + 10f, y + 21f, pBodyBold)
                rightAlignAt("Todo cobrado", MR - 10f, y + 21f, okPaint)
            }
            y += 46f

            // ══ Pie ══
            ensureSpace(24f)
            c.drawLine(ML, y, MR, y, pLine)
            y += 9f
            c.drawText("Generado: ${dateTimeFmt.format(Date())}", ML, y, pSmall)
            val footerRight = "SERVIELECAR - Taller Automotriz"
            c.drawText(footerRight, MR - pSmall.measureText(footerRight), y, pSmall)
            c.drawRect(0f, PAGE_HEIGHT - 4f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgAccent)

            doc.finishPage(page)

            val dir = File(context.filesDir, "reports")
            if (!dir.exists()) dir.mkdirs()
            val file = File(
                dir,
                "resumen_serviaux_${fileFmt.format(Date(data.from))}_a_${fileFmt.format(Date(data.to))}.pdf"
            )
            FileOutputStream(file).use { doc.writeTo(it) }
            return file
        } finally {
            doc.close()
        }
    }

    // ── Auxiliares ──────────────────────────────────────────────────────

    private fun getLogoBitmap(context: Context, sizePx: Int): Bitmap? {
        return try {
            val opts = BitmapFactory.Options().apply { inScaled = false }
            val bmp = BitmapFactory.decodeResource(context.resources, R.drawable.serviaux_logo, opts)
                ?: BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher, opts)
                ?: return null
            Bitmap.createScaledBitmap(bmp, sizePx, sizePx, true)
        } catch (_: Exception) {
            null
        }
    }

    private fun paint(size: Float, color: Int, bold: Boolean = false) = Paint().apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
        isAntiAlias = true
    }

    private fun fill(color: Int) = Paint().apply {
        this.color = color
        isAntiAlias = true
    }

    private fun truncate(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var t = text
        while (t.length > 3 && paint.measureText("$t...") > maxWidth) t = t.dropLast(1)
        return "$t..."
    }
}
