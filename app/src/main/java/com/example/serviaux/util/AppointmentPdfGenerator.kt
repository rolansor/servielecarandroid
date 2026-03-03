/**
 * AppointmentPdfGenerator.kt - Generador de reportes PDF de turnos/citas.
 *
 * Genera un documento PDF profesional con:
 * - Encabezado con logo y titulo "REPORTE DE TURNOS".
 * - Tabla con columnas: #, Cliente, Vehiculo, Fecha, Estado, Notas.
 * - Resumen por estado al final.
 * - Pie de pagina con timestamp de generacion.
 *
 * Reutiliza las mismas dimensiones A4, colores y helpers que [PdfReportGenerator].
 * El PDF se guarda en `filesDir/reports/Turnos_yyyy-MM-dd_HHmm.pdf`.
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
import com.example.serviaux.data.entity.Appointment
import com.example.serviaux.data.entity.AppointmentStatus
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppointmentPdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val ML = 36f
    private const val MR = 559f
    private const val MT = 36f
    private const val MB = 806f

    private const val ROW_H = 16f
    private const val TABLE_HEADER_H = 18f
    private const val SECTION_GAP = 10f

    private val dateTimeFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es"))
    private val fileDateFmt = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale("es"))

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

    // Status colors matching the UI
    private const val COL_PENDIENTE = 0xFFFF9800.toInt()
    private const val COL_CONFIRMADO = 0xFF4CAF50.toInt()
    private const val COL_CANCELADO = 0xFF9E9E9E.toInt()
    private const val COL_CONVERTIDO = 0xFF2196F3.toInt()

    fun generate(
        context: Context,
        appointments: List<Appointment>,
        customerNames: Map<Long, String>,
        vehicleDescriptions: Map<Long, String>
    ): File {
        val doc = PdfDocument()
        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create())
        var c = page.canvas
        var y = MT

        val pTitle = paint(20f, COL_TEXT_BOLD, bold = true)
        val pSubtitle = paint(10f, COL_MUTED)
        val pSectionHeader = paint(10f, COL_HEADER_TEXT, bold = true)
        val pBody = paint(8f, COL_TEXT)
        val pBodyBold = paint(8f, COL_TEXT, bold = true)
        val pSmall = paint(7.5f, COL_MUTED)
        val pTableHeader = paint(8f, COL_TEXT_BOLD, bold = true)
        val pStatus = paint(7.5f, 0xFFFFFFFF.toInt(), bold = true)
        val pTotal = paint(12f, COL_TOTAL_TEXT, bold = true)
        val pTotalLabel = paint(10f, COL_TEXT_BOLD, bold = true)

        val bgAccent = fill(COL_ACCENT)
        val bgHeader = fill(COL_HEADER_BG)
        val bgTableHead = fill(COL_TABLE_HEADER_BG)
        val bgAltRow = fill(COL_ALT_ROW)
        val bgTotal = fill(COL_TOTAL_BG)
        val pLine = Paint().apply { color = COL_DIVIDER; strokeWidth = 0.6f; isAntiAlias = true }

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

        fun drawTableHeaderBg() {
            c.drawRect(ML, y, MR, y + TABLE_HEADER_H, bgTableHead)
        }

        fun drawRowBg(index: Int) {
            if (index % 2 == 1) {
                c.drawRect(ML, y - 1f, MR, y + ROW_H - 3f, bgAltRow)
            }
        }

        fun statusColor(status: AppointmentStatus): Int = when (status) {
            AppointmentStatus.PENDIENTE -> COL_PENDIENTE
            AppointmentStatus.CONFIRMADO -> COL_CONFIRMADO
            AppointmentStatus.CANCELADO -> COL_CANCELADO
            AppointmentStatus.CONVERTIDO -> COL_CONVERTIDO
        }

        fun drawStatusBadge(text: String, x: Float, atY: Float, color: Int) {
            val textW = pStatus.measureText(text)
            val badgeW = textW + 8f
            val badgeH = 11f
            val badgePaint = fill(color)
            c.drawRoundRect(RectF(x, atY - badgeH + 2f, x + badgeW, atY + 2f), 3f, 3f, badgePaint)
            c.drawText(text, x + 4f, atY, pStatus)
        }

        fun wrapAndDrawText(text: String, x: Float, maxWidth: Float, atY: Float, paint: Paint): Float {
            val words = text.split(" ")
            var line = ""
            var currentY = atY
            for (word in words) {
                val test = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(test) > maxWidth && line.isNotEmpty()) {
                    c.drawText(line, x, currentY, paint)
                    currentY += 10f
                    line = word
                } else {
                    line = test
                }
            }
            if (line.isNotEmpty()) {
                c.drawText(line, x, currentY, paint)
            }
            return currentY
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
        rightAlignAt("REPORTE DE TURNOS", MR, y + 16f, reportTitle)
        y += 14f
        rightAlignAt(dateTimeFmt.format(Date()), MR, y + 16f, pSubtitle)
        rightAlignAt("Total: ${appointments.size} turnos", MR, y + 28f, pSubtitle)

        y += 40f
        c.drawRect(ML, y, MR, y + 2f, bgAccent)
        y += SECTION_GAP

        // ══════════════════════════════════════════
        //  TABLE
        // ══════════════════════════════════════════
        val colNum = ML + 4f
        val colCustomer = ML + 26f
        val colVehicle = ML + 166f
        val colDate = ML + 310f
        val colStatus = ML + 400f
        val colNotes = ML + 470f

        drawTableHeaderBg()
        val headerTextY = y + 12f
        c.drawText("#", colNum, headerTextY, pTableHeader)
        c.drawText("Cliente", colCustomer, headerTextY, pTableHeader)
        c.drawText("Vehiculo", colVehicle, headerTextY, pTableHeader)
        c.drawText("Fecha", colDate, headerTextY, pTableHeader)
        c.drawText("Estado", colStatus, headerTextY, pTableHeader)
        c.drawText("Notas", colNotes, headerTextY, pTableHeader)
        y += TABLE_HEADER_H + 2f

        appointments.forEachIndexed { i, appt ->
            val customerName = customerNames[appt.customerId] ?: "Cliente #${appt.customerId}"
            val vehicleDesc = vehicleDescriptions[appt.vehicleId] ?: "Vehiculo #${appt.vehicleId}"
            val dateStr = dateTimeFmt.format(Date(appt.scheduledDate))
            val notes = appt.notes ?: ""

            // Calculate row height based on content
            val hasNotes = notes.isNotBlank()
            val rowHeight = if (hasNotes) ROW_H + 10f else ROW_H

            ensureSpace(rowHeight)
            drawRowBg(i)
            val rowTextY = y + 10f

            c.drawText("${i + 1}", colNum, rowTextY, pBody)

            // Customer name - truncate if too long
            val maxCustomerW = colVehicle - colCustomer - 6f
            val customerDisplay = truncateText(customerName, pBody, maxCustomerW)
            c.drawText(customerDisplay, colCustomer, rowTextY, pBody)

            // Vehicle - truncate if too long
            val maxVehicleW = colDate - colVehicle - 6f
            val vehicleDisplay = truncateText(vehicleDesc, pBody, maxVehicleW)
            c.drawText(vehicleDisplay, colVehicle, rowTextY, pBody)

            // Date
            c.drawText(dateStr, colDate, rowTextY, pBody)

            // Status badge
            drawStatusBadge(appt.status.displayName, colStatus, rowTextY, statusColor(appt.status))

            // Notes (truncated)
            if (hasNotes) {
                val maxNotesW = MR - colNotes - 4f
                val notesDisplay = truncateText(notes, pSmall, maxNotesW)
                c.drawText(notesDisplay, colNotes, rowTextY, pSmall)
            }

            y += rowHeight
        }

        // ══════════════════════════════════════════
        //  SUMMARY BY STATUS
        // ══════════════════════════════════════════
        y += SECTION_GAP
        hLine()
        y += SECTION_GAP

        c.drawRoundRect(RectF(ML, y, MR, y + 16f), 3f, 3f, bgHeader)
        c.drawText("RESUMEN POR ESTADO", ML + 8f, y + 11.5f, pSectionHeader)
        y += 28f

        val statusCounts = appointments.groupBy { it.status }
        val boxH = 30f + (statusCounts.size * 16f)
        ensureSpace(boxH + 8f)
        val boxLeft = ML
        c.drawRoundRect(RectF(boxLeft, y, MR, y + boxH), 6f, 6f, bgTotal)

        var summaryY = y + 16f
        AppointmentStatus.entries.forEach { status ->
            val count = statusCounts[status]?.size ?: 0
            if (count > 0) {
                drawStatusBadge(status.displayName, boxLeft + 14f, summaryY, statusColor(status))
                rightAlignAt("$count turno${if (count != 1) "s" else ""}", MR - 14f, summaryY, pTotalLabel)
                summaryY += 16f
            }
        }
        summaryY += 4f
        c.drawLine(boxLeft + 14f, summaryY - 8f, MR - 14f, summaryY - 8f, pLine)
        rightAlignAt("Total: ${appointments.size} turnos", MR - 14f, summaryY + 4f, pTotal)
        y = summaryY + 20f

        // ══════════════════════════════════════════
        //  FOOTER
        // ══════════════════════════════════════════
        ensureSpace(24f)
        hLine()
        y += 8f
        c.drawText("Generado: ${dateTimeFmt.format(Date())}", ML, y, pSmall)
        val footerRight = "SERVIELECAR - Taller Automotriz"
        c.drawText(footerRight, MR - pSmall.measureText(footerRight), y, pSmall)

        c.drawRect(0f, PAGE_HEIGHT - 4f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgAccent)

        doc.finishPage(page)

        val dir = File(context.filesDir, "reports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "Turnos_${fileDateFmt.format(Date())}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()

        return file
    }

    /**
     * Genera un PDF de un solo turno con formato de ficha detallada.
     */
    fun generateSingle(
        context: Context,
        appointment: Appointment,
        customerName: String,
        vehicleDescription: String
    ): File {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        val c = page.canvas
        var y = MT

        val pTitle = paint(20f, COL_TEXT_BOLD, bold = true)
        val pSubtitle = paint(10f, COL_MUTED)
        val pSectionHeader = paint(10f, COL_HEADER_TEXT, bold = true)
        val pLabel = paint(9f, COL_MUTED)
        val pValue = paint(10f, COL_TEXT, bold = true)
        val pBody = paint(9f, COL_TEXT)
        val pSmall = paint(7.5f, COL_MUTED)
        val pStatus = paint(10f, 0xFFFFFFFF.toInt(), bold = true)

        val bgAccent = fill(COL_ACCENT)
        val bgHeader = fill(COL_HEADER_BG)
        val bgTotal = fill(COL_TOTAL_BG)
        val pLine = Paint().apply { color = COL_DIVIDER; strokeWidth = 0.6f; isAntiAlias = true }

        fun rightAlignAt(text: String, rightX: Float, atY: Float, paint: Paint) {
            c.drawText(text, rightX - paint.measureText(text), atY, paint)
        }

        fun hLine() {
            c.drawLine(ML, y, MR, y, pLine)
            y += 1f
        }

        fun infoRow(label: String, value: String) {
            c.drawText(label, ML + 8f, y, pLabel)
            c.drawText(value, ML + 120f, y, pValue)
            y += 16f
        }

        fun statusColor(status: AppointmentStatus): Int = when (status) {
            AppointmentStatus.PENDIENTE -> COL_PENDIENTE
            AppointmentStatus.CONFIRMADO -> COL_CONFIRMADO
            AppointmentStatus.CANCELADO -> COL_CANCELADO
            AppointmentStatus.CONVERTIDO -> COL_CONVERTIDO
        }

        // ── HEADER ──
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
        rightAlignAt("TURNO #${appointment.id}", MR, y + 16f, reportTitle)

        // Status badge
        val statusText = appointment.status.displayName
        val statusTextW = pStatus.measureText(statusText)
        val badgeW = statusTextW + 12f
        val badgeX = MR - badgeW
        val badgeY = y + 22f
        c.drawRoundRect(RectF(badgeX, badgeY, badgeX + badgeW, badgeY + 16f), 4f, 4f, fill(statusColor(appointment.status)))
        c.drawText(statusText, badgeX + 6f, badgeY + 12f, pStatus)

        y += 52f
        c.drawRect(ML, y, MR, y + 2f, bgAccent)
        y += SECTION_GAP + 6f

        // ── DATOS DEL TURNO ──
        c.drawRoundRect(RectF(ML, y, MR, y + 16f), 3f, 3f, bgHeader)
        c.drawText("DATOS DEL TURNO", ML + 8f, y + 11.5f, pSectionHeader)
        y += 28f

        infoRow("Cliente:", customerName)
        infoRow("Vehiculo:", vehicleDescription)
        infoRow("Fecha:", dateTimeFmt.format(Date(appointment.scheduledDate)))
        infoRow("Estado:", appointment.status.displayName)
        infoRow("Creado:", dateTimeFmt.format(Date(appointment.createdAt)))

        if (!appointment.notes.isNullOrBlank()) {
            y += 6f
            c.drawRoundRect(RectF(ML, y, MR, y + 16f), 3f, 3f, bgHeader)
            c.drawText("NOTAS", ML + 8f, y + 11.5f, pSectionHeader)
            y += 24f

            // Wrap notes text
            val words = appointment.notes.split(" ")
            var line = ""
            val maxW = MR - ML - 16f
            for (word in words) {
                val test = if (line.isEmpty()) word else "$line $word"
                if (pBody.measureText(test) > maxW && line.isNotEmpty()) {
                    c.drawText(line, ML + 8f, y, pBody)
                    y += 14f
                    line = word
                } else {
                    line = test
                }
            }
            if (line.isNotEmpty()) {
                c.drawText(line, ML + 8f, y, pBody)
                y += 14f
            }
        }

        // ── FOOTER ──
        y += SECTION_GAP
        hLine()
        y += 8f
        c.drawText("Generado: ${dateTimeFmt.format(Date())}", ML, y, pSmall)
        val footerRight = "SERVIELECAR - Taller Automotriz"
        c.drawText(footerRight, MR - pSmall.measureText(footerRight), y, pSmall)

        c.drawRect(0f, PAGE_HEIGHT - 4f, PAGE_WIDTH.toFloat(), PAGE_HEIGHT.toFloat(), bgAccent)

        doc.finishPage(page)

        val dir = File(context.filesDir, "reports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "Turno_${appointment.id}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()

        return file
    }

    private fun truncateText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var truncated = text
        while (truncated.isNotEmpty() && paint.measureText("$truncated...") > maxWidth) {
            truncated = truncated.dropLast(1)
        }
        return if (truncated.isEmpty()) text.take(5) + "..." else "$truncated..."
    }

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