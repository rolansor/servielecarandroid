/**
 * CommissionPdfGenerator.kt - Generador de reportes PDF de comisiones pagadas.
 *
 * Genera un documento PDF profesional con:
 * - Encabezado con logo y título "REPORTE DE COMISIONES".
 * - Tablas agrupadas por mecánico con columnas: #, Orden, Vehículo, Monto.
 * - Subtotal por mecánico y total general en box verde.
 * - Pie de página con timestamp de generación.
 *
 * Reutiliza las mismas dimensiones A4, colores y helpers que [PdfReportGenerator].
 * El PDF se guarda en `filesDir/reports/Comisiones_yyyy-MM-dd_HHmm.pdf`.
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
import com.example.serviaux.data.entity.PendingCommissionRow
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CommissionPdfGenerator {

    // ── Dimensiones de página A4 en puntos ─────────────────────────────
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val ML = 36f          // margin left
    private const val MR = 559f         // margin right
    private const val MT = 36f          // margin top
    private const val MB = 806f         // margin bottom

    // Row heights
    private const val ROW_H = 16f
    private const val TABLE_HEADER_H = 18f
    private const val SECTION_GAP = 10f

    private val dateTimeFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es"))
    private val fileDateFmt = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale("es"))

    // Colors (mismos que PdfReportGenerator)
    private const val COL_ACCENT = 0xFFE65100.toInt()
    private const val COL_HEADER_BG = 0xFF37474F.toInt()
    private const val COL_HEADER_TEXT = 0xFFFFFFFF.toInt()
    private const val COL_TABLE_HEADER_BG = 0xFFECEFF1.toInt()
    private const val COL_ALT_ROW = 0xFFF5F5F5.toInt()
    private const val COL_TEXT = 0xFF333333.toInt()
    private const val COL_TEXT_BOLD = 0xFF1A1A1A.toInt()
    private const val COL_MUTED = 0xFF666666.toInt()
    private const val COL_TOTAL_BG = 0xFFE8F5E9.toInt()
    private const val COL_TOTAL_TEXT = 0xFF1B5E20.toInt()
    private const val COL_DIVIDER = 0xFFBDBDBD.toInt()

    /**
     * Genera el PDF de comisiones pagadas.
     *
     * @param context Contexto para acceder a recursos (logo) y filesDir.
     * @param commissions Lista de comisiones recién pagadas.
     * @param mechanicNames Mapa de mechanicId → nombre del mecánico.
     * @return Archivo PDF guardado en `filesDir/reports/`.
     */
    fun generate(
        context: Context,
        commissions: List<PendingCommissionRow>,
        mechanicNames: Map<Long, String>
    ): File {
        val doc = PdfDocument()
        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
        var c = page.canvas
        var y = MT

        // ── Paints ──
        val pTitle = paint(20f, COL_TEXT_BOLD, bold = true)
        val pSubtitle = paint(10f, COL_MUTED)
        val pSectionHeader = paint(10f, COL_HEADER_TEXT, bold = true)
        val pBody = paint(9f, COL_TEXT)
        val pSmall = paint(7.5f, COL_MUTED)
        val pTableHeader = paint(8.5f, COL_TEXT_BOLD, bold = true)
        val pMoney = paint(9f, COL_TEXT)
        val pMoneyBold = paint(9f, COL_TEXT, bold = true)
        val pTotal = paint(14f, COL_TOTAL_TEXT, bold = true)
        val pTotalLabel = paint(11f, COL_TEXT_BOLD, bold = true)

        val bgAccent = fill(COL_ACCENT)
        val bgHeader = fill(COL_HEADER_BG)
        val bgTableHead = fill(COL_TABLE_HEADER_BG)
        val bgAltRow = fill(COL_ALT_ROW)
        val bgTotal = fill(COL_TOTAL_BG)
        val pLine = Paint().apply { color = COL_DIVIDER; strokeWidth = 0.6f; isAntiAlias = true }

        // ── Helpers ──
        fun newPage() {
            doc.finishPage(page)
            pageNum++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
            c = page.canvas
            y = MT
        }

        fun ensureSpace(need: Float) {
            if (y + need > MB) newPage()
        }

        fun hLine() {
            c.drawLine(ML, y, MR, y, pLine)
            y += 1f
        }

        fun rightAlignAt(text: String, rightX: Float, atY: Float, paint: Paint) {
            c.drawText(text, rightX - paint.measureText(text), atY, paint)
        }

        fun sectionHeader(title: String) {
            ensureSpace(28f)
            c.drawRoundRect(RectF(ML, y, MR, y + 16f), 3f, 3f, bgHeader)
            c.drawText(title, ML + 8f, y + 11.5f, pSectionHeader)
            y += 28f
        }

        fun drawTableHeaderBg() {
            c.drawRect(ML, y, MR, y + TABLE_HEADER_H, bgTableHead)
        }

        fun drawRowBg(index: Int) {
            if (index % 2 == 1) {
                c.drawRect(ML, y - 1f, MR, y + ROW_H - 3f, bgAltRow)
            }
        }

        // ══════════════════════════════════════════
        //  HEADER
        // ══════════════════════════════════════════
        c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 5f, bgAccent)
        y = MT + 5f

        val logo = getLogoBitmap(context, 128)
        if (logo != null) {
            val logoRect = RectF(ML, y - 4f, ML + 56f, y + 52f)
            val logoPaint = Paint().apply { isFilterBitmap = true; isAntiAlias = true }
            c.drawBitmap(logo, null, logoRect, logoPaint)
        }
        val lOff = if (logo != null) 64f else 0f

        c.drawText("SERVIELECAR", ML + lOff, y + 16f, pTitle)
        c.drawText("Taller Automotriz", ML + lOff, y + 28f, pSubtitle)

        val reportTitle = paint(14f, COL_ACCENT, bold = true)
        rightAlignAt("REPORTE DE COMISIONES", MR, y + 16f, reportTitle)
        y += 14f
        rightAlignAt(dateTimeFmt.format(Date()), MR, y + 16f, pSubtitle)

        y += 32f
        c.drawRect(ML, y, MR, y + 2f, bgAccent)
        y += SECTION_GAP

        // ══════════════════════════════════════════
        //  COMISIONES POR MECÁNICO
        // ══════════════════════════════════════════
        val grouped = commissions.groupBy { it.mechanicId }
        val colNum = ML + 4f
        val colOrder = ML + 30f
        val colVehicle = ML + 130f
        val colAmountRight = MR - 8f
        var grandTotal = 0.0

        grouped.forEach { (mechanicId, items) ->
            val mechanicName = mechanicNames[mechanicId] ?: "Mecánico #$mechanicId"
            sectionHeader(mechanicName)

            drawTableHeaderBg()
            val headerTextY = y + 12f
            c.drawText("#", colNum, headerTextY, pTableHeader)
            c.drawText("Orden", colOrder, headerTextY, pTableHeader)
            c.drawText("Vehículo", colVehicle, headerTextY, pTableHeader)
            rightAlignAt("Monto", colAmountRight, headerTextY, pTableHeader)
            y += TABLE_HEADER_H + 2f

            items.forEachIndexed { i, comm ->
                ensureSpace(ROW_H)
                drawRowBg(i)
                val rowTextY = y + 10f
                c.drawText("${i + 1}", colNum, rowTextY, pBody)
                c.drawText("#${comm.workOrderId}", colOrder, rowTextY, pBody)
                c.drawText(comm.vehiclePlate, colVehicle, rowTextY, pBody)
                rightAlignAt(money(comm.commissionAmount), colAmountRight, rowTextY, pMoney)
                y += ROW_H
            }

            val subtotal = items.sumOf { it.commissionAmount }
            grandTotal += subtotal
            y += 4f
            ensureSpace(16f)
            c.drawLine(MR - 160f, y, MR, y, pLine)
            y += 12f
            c.drawText("Subtotal:", MR - 160f, y, pMoneyBold)
            rightAlignAt(money(subtotal), colAmountRight, y, pMoneyBold)
            y += 6f
            hLine()
            y += SECTION_GAP
        }

        // ══════════════════════════════════════════
        //  TOTAL BOX
        // ══════════════════════════════════════════
        val boxH = 40f
        ensureSpace(boxH + 8f)
        val boxLeft = MR - 260f
        c.drawRoundRect(RectF(boxLeft, y, MR, y + boxH), 6f, 6f, bgTotal)

        val labelX = boxLeft + 14f
        val valueX = MR - 14f
        y += 26f
        c.drawText("TOTAL PAGADO:", labelX, y, pTotalLabel)
        rightAlignAt(money(grandTotal), valueX, y, pTotal)
        y += 20f

        // ══════════════════════════════════════════
        //  FOOTER
        // ══════════════════════════════════════════
        ensureSpace(24f)
        hLine()
        y += 8f
        c.drawText("Generado: ${dateTimeFmt.format(Date())}", ML, y, pSmall)
        val footerRight = "SERVIELECAR - Taller Automotriz"
        c.drawText(footerRight, MR - pSmall.measureText(footerRight), y, pSmall)

        // Bottom accent bar
        c.drawRect(0f, PAGE_HEIGHT - 4f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgAccent)

        doc.finishPage(page)

        // ── Save file ──
        val dir = File(context.filesDir, "reports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "Comisiones_${fileDateFmt.format(Date())}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()

        return file
    }

    // ── Funciones auxiliares ──────────────────────────────────────────

    private fun getLogoBitmap(context: Context, sizePx: Int): Bitmap? {
        return try {
            val opts = BitmapFactory.Options().apply { inScaled = false }
            val bmp = BitmapFactory.decodeResource(context.resources, R.drawable.servielecar_logo, opts)
                ?: BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher, opts)
                ?: return null
            Bitmap.createScaledBitmap(bmp, sizePx, sizePx, true)
        } catch (_: Exception) {
            null
        }
    }

    private fun money(amount: Double): String = "$${String.format("%.2f", amount)}"

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
}
