/**
 * PdfReportGenerator.kt - Generador de reportes PDF de órdenes de trabajo.
 *
 * Genera un documento PDF profesional multipágina con:
 * - Encabezado con logo, número de orden y badge de estado.
 * - Datos del cliente y vehículo en columnas paralelas.
 * - Tablas de servicios (mano de obra), repuestos y pagos.
 * - Cuadro de totales con saldo pendiente.
 * - Miniaturas de fotos adjuntas.
 * - Pie de página con fecha de generación.
 *
 * Utiliza directamente la API de [PdfDocument] de Android (sin librerías externas).
 * El PDF se guarda en `filesDir/reports/` y se puede compartir vía [ShareUtils].
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
import com.example.serviaux.data.entity.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Datos necesarios para generar el reporte PDF de una orden de trabajo.
 * Agrupa información de la orden, cliente, vehículo, servicios, repuestos y pagos.
 */
data class WorkOrderReportData(
    val order: WorkOrder,
    val customerName: String,
    val customerIdNumber: String? = null,
    val customerPhone: String,
    val customerEmail: String?,
    val customerAddress: String?,
    val vehiclePlate: String,
    val vehicleBrand: String,
    val vehicleModel: String,
    val vehicleYear: Int?,
    val vehicleColor: String?,
    val vehicleVin: String?,
    val vehicleVersion: String? = null,
    val vehicleType: String? = null,
    val vehicleFuelType: String? = null,
    val vehicleTransmission: String? = null,
    val vehicleDrivetrain: String? = null,
    val vehicleEngineDisplacement: String? = null,
    val serviceLines: List<ServiceLine>,
    val orderParts: List<WorkOrderPart>,
    val availableParts: List<Part>,
    val payments: List<WorkOrderPayment>,
    val mechanicName: String?,
    val photoPaths: List<String> = emptyList()
)

/**
 * Generador de reportes PDF para órdenes de trabajo.
 *
 * Renderiza un PDF tamaño A4 (595x842 pts) con márgenes de 36pt,
 * soporte automático de paginación y tablas con filas alternadas.
 */
object PdfReportGenerator {

    // ── Dimensiones de página A4 en puntos ─────────────────────────────
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val ML = 36f          // margin left
    private const val MR = 559f         // margin right
    private const val MT = 36f          // margin top
    private const val MB = 806f         // margin bottom
    private const val CW = MR - ML     // content width

    // Row heights
    private const val ROW_H = 16f
    private const val SECTION_GAP = 10f
    private const val TABLE_HEADER_H = 18f

    private val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale("es"))
    private val dateTimeFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es"))

    // Colors
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
     * Genera el PDF completo de una orden de trabajo.
     *
     * @param context Contexto para acceder a recursos (logo) y filesDir.
     * @param data Datos consolidados de la orden.
     * @return Archivo PDF guardado en `filesDir/reports/`.
     */
    fun generateWorkOrderPdf(context: Context, data: WorkOrderReportData): File {
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
        val pBodyBold = paint(9f, COL_TEXT, bold = true)
        val pSmall = paint(7.5f, COL_MUTED)
        val pTableHeader = paint(8.5f, COL_TEXT_BOLD, bold = true)
        val pMoney = paint(9f, COL_TEXT)
        val pMoneyBold = paint(9f, COL_TEXT, bold = true)
        val pTotalLabel = paint(11f, COL_TEXT_BOLD, bold = true)
        val pTotal = paint(14f, COL_TOTAL_TEXT, bold = true)

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

        fun sectionHeader(title: String) {
            ensureSpace(28f)
            c.drawRoundRect(RectF(ML, y, MR, y + 16f), 3f, 3f, bgHeader)
            c.drawText(title, ML + 8f, y + 11.5f, pSectionHeader)
            y += 28f
        }

        // Fixed-offset label:value - label column is fixed width so values align
        fun labelVal(label: String, value: String, x: Float, maxW: Float, labelW: Float = 75f) {
            ensureSpace(14f)
            c.drawText(label, x, y, pBodyBold)
            val valX = x + labelW
            val valMaxW = maxW - labelW
            val wrapped = wrapText(value, pBody, valMaxW)
            wrapped.forEachIndexed { i, line ->
                if (i > 0) y += 11f
                c.drawText(line, valX, y, pBody)
            }
            y += 14f
        }

        fun drawTableHeaderBg() {
            c.drawRect(ML, y, MR, y + TABLE_HEADER_H, bgTableHead)
        }

        fun drawRowBg(index: Int) {
            if (index % 2 == 1) {
                c.drawRect(ML, y - 1f, MR, y + ROW_H - 3f, bgAltRow)
            }
        }

        // Right-aligned text at a specific vertical position
        fun rightAlignAt(text: String, rightX: Float, atY: Float, paint: Paint) {
            c.drawText(text, rightX - paint.measureText(text), atY, paint)
        }

        // ══════════════════════════════════════════
        //  HEADER
        // ══════════════════════════════════════════
        c.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 5f, bgAccent)
        y = MT + 5f

        // Logo from drawable (high-res Servielecar logo)
        val logo = getLogoBitmap(context, 128)
        if (logo != null) {
            // Draw at 56x56 on the PDF but decoded at 128px for sharpness
            val logoRect = RectF(ML, y - 4f, ML + 56f, y + 52f)
            val logoPaint = Paint().apply { isFilterBitmap = true; isAntiAlias = true }
            c.drawBitmap(logo, null, logoRect, logoPaint)
        }
        val lOff = if (logo != null) 64f else 0f

        c.drawText("SERVIELECAR", ML + lOff, y + 16f, pTitle)
        c.drawText("Taller Automotriz", ML + lOff, y + 28f, pSubtitle)

        // Order number + plate right-aligned
        val pOrderNum = paint(14f, COL_ACCENT, bold = true)
        val orderTxt = "#${data.order.id} ${data.vehiclePlate}"
        val orderTxtX = MR - pOrderNum.measureText(orderTxt)
        c.drawText(orderTxt, orderTxtX, y + 16f, pOrderNum)
        y += 14f // align date below order number

        val dateTxt = dateFmt.format(Date(data.order.entryDate))
        val dateX = MR - pSubtitle.measureText(dateTxt)
        c.drawText(dateTxt, dateX, y + 16f, pSubtitle)

        // Status badge
        val statusTxt = data.order.status.displayName
        val pStatus = paint(8f, COL_HEADER_TEXT, bold = true)
        val statusW = pStatus.measureText(statusTxt) + 14f
        val statusBg = fill(statusColor(data.order.status))
        c.drawRoundRect(RectF(MR - statusW, y + 21f, MR, y + 34f), 3f, 3f, statusBg)
        c.drawText(statusTxt, MR - statusW + 7f, y + 31f, pStatus)

        y += 48f
        c.drawRect(ML, y, MR, y + 2f, bgAccent)
        y += SECTION_GAP

        // ══════════════════════════════════════════
        //  CLIENT + VEHICLE (two columns)
        // ══════════════════════════════════════════
        val halfW = CW / 2f - 6f
        val rightCol = ML + CW / 2f + 6f
        val pad = 10f  // internal padding for section content

        // Client header
        c.drawRoundRect(RectF(ML, y, ML + halfW, y + 16f), 3f, 3f, bgHeader)
        c.drawText("DATOS DEL CLIENTE", ML + 8f, y + 11.5f, pSectionHeader)
        // Vehicle header
        c.drawRoundRect(RectF(rightCol, y, rightCol + halfW, y + 16f), 3f, 3f, bgHeader)
        c.drawText("DATOS DEL VEHÍCULO", rightCol + 8f, y + 11.5f, pSectionHeader)
        y += 28f

        val clientLabelW = 68f
        val vehicleLabelW = 70f

        val ySnap = y
        labelVal("Nombre:", data.customerName, ML + pad, halfW - pad, clientLabelW)
        data.customerIdNumber?.let { if (it.isNotBlank()) labelVal("Cédula:", it, ML + pad, halfW - pad, clientLabelW) }
        if (data.customerPhone.isNotBlank()) labelVal("Teléfono:", data.customerPhone, ML + pad, halfW - pad, clientLabelW)
        data.customerEmail?.let { if (it.isNotBlank()) labelVal("Email:", it, ML + pad, halfW - pad, clientLabelW) }
        data.customerAddress?.let { if (it.isNotBlank()) labelVal("Dirección:", it, ML + pad, halfW - pad, clientLabelW) }
        val yLeft = y

        y = ySnap
        labelVal("Placa:", data.vehiclePlate, rightCol + pad, halfW - pad, vehicleLabelW)
        data.vehicleType?.let { if (it.isNotBlank()) labelVal("Tipo:", it, rightCol + pad, halfW - pad, vehicleLabelW) }
        labelVal("Marca:", data.vehicleBrand, rightCol + pad, halfW - pad, vehicleLabelW)
        labelVal("Modelo:", data.vehicleModel, rightCol + pad, halfW - pad, vehicleLabelW)
        data.vehicleVersion?.let { if (it.isNotBlank()) labelVal("Versión:", it, rightCol + pad, halfW - pad, vehicleLabelW) }
        data.vehicleYear?.let { labelVal("Año:", it.toString(), rightCol + pad, halfW - pad, vehicleLabelW) }
        data.vehicleColor?.let { if (it.isNotBlank()) labelVal("Color:", it, rightCol + pad, halfW - pad, vehicleLabelW) }
        data.vehicleFuelType?.let { if (it.isNotBlank()) labelVal("Combustible:", it, rightCol + pad, halfW - pad, vehicleLabelW) }
        data.vehicleTransmission?.let { if (it.isNotBlank()) labelVal("Transmisión:", it, rightCol + pad, halfW - pad, vehicleLabelW) }
        data.vehicleDrivetrain?.let { if (it.isNotBlank()) labelVal("Tracción:", it, rightCol + pad, halfW - pad, vehicleLabelW) }
        data.vehicleEngineDisplacement?.let { if (it.isNotBlank()) labelVal("Motor:", it, rightCol + pad, halfW - pad, vehicleLabelW) }
        data.vehicleVin?.let { if (it.isNotBlank()) labelVal("VIN:", it, rightCol + pad, halfW - pad, vehicleLabelW) }
        val yRight = y

        y = maxOf(yLeft, yRight) + 2f
        hLine()
        y += SECTION_GAP

        // ══════════════════════════════════════════
        //  ORDER INFO
        // ══════════════════════════════════════════
        sectionHeader("INFORMACIÓN DE LA ORDEN")
        val infoLabelW = 80f

        val yInfo = y
        labelVal("Estado:", data.order.status.displayName, ML + pad, halfW - pad, infoLabelW)
        labelVal("Tipo:", data.order.orderType.displayName, ML + pad, halfW - pad, infoLabelW)
        labelVal("Prioridad:", data.order.priority.displayName, ML + pad, halfW - pad, infoLabelW)
        data.mechanicName?.let { labelVal("Mecánico:", it, ML + pad, halfW - pad, infoLabelW) }
        val yInfoL = y

        y = yInfo
        data.order.entryMileage?.let { labelVal("Kilometraje:", "$it km", rightCol + pad, halfW - pad, infoLabelW) }
        data.order.fuelLevel?.let { if (it.isNotBlank()) labelVal("Combustible:", it, rightCol + pad, halfW - pad, infoLabelW) }
        labelVal("Llegada:", data.order.arrivalCondition.displayName, rightCol + pad, halfW - pad, infoLabelW)
        data.order.admissionDate?.let { labelVal("F. Admisión:", dateFmt.format(Date(it)), rightCol + pad, halfW - pad, infoLabelW) }
        val yInfoR = y

        y = maxOf(yInfoL, yInfoR) + 2f

        if (data.order.customerComplaint.isNotBlank()) {
            ensureSpace(24f)
            c.drawText("Queja del cliente:", ML + pad, y, pBodyBold)
            y += 12f
            wrapText(data.order.customerComplaint, pBody, CW - pad * 2).forEach { line ->
                ensureSpace(12f)
                c.drawText(line, ML + pad + 8f, y, pBody)
                y += 11f
            }
            y += 2f
        }

        data.order.initialDiagnosis?.let { d ->
            if (d.isNotBlank()) {
                ensureSpace(24f)
                c.drawText("Diagnóstico:", ML + pad, y, pBodyBold)
                y += 12f
                wrapText(d, pBody, CW - pad * 2).forEach { line ->
                    ensureSpace(12f)
                    c.drawText(line, ML + pad + 8f, y, pBody)
                    y += 11f
                }
                y += 2f
            }
        }

        data.order.deliveryNote?.let { dn ->
            if (dn.isNotBlank()) {
                ensureSpace(24f)
                c.drawText("Nota de entrega:", ML + pad, y, pBodyBold)
                y += 12f
                wrapText(dn, pBody, CW - pad * 2).forEach { line ->
                    ensureSpace(12f)
                    c.drawText(line, ML + pad + 8f, y, pBody)
                    y += 11f
                }
                y += 2f
            }
        }

        data.order.invoiceNumber?.let { inv ->
            if (inv.isNotBlank()) {
                ensureSpace(14f)
                c.drawText("N° Factura: $inv", ML + pad, y, pBody)
                y += 14f
            }
        }

        data.order.notes?.let { notes ->
            if (notes.isNotBlank()) {
                ensureSpace(24f)
                c.drawText("Notas:", ML + pad, y, pBodyBold)
                y += 12f
                wrapText(notes, pBody, CW - pad * 2).forEach { line ->
                    ensureSpace(12f)
                    c.drawText(line, ML + pad + 8f, y, pBody)
                    y += 11f
                }
                y += 2f
            }
        }

        hLine()
        y += SECTION_GAP

        // ══════════════════════════════════════════
        //  SERVICES TABLE
        // ══════════════════════════════════════════
        if (data.serviceLines.isNotEmpty()) {
            sectionHeader("SERVICIOS (MANO DE OBRA)")

            // Column positions
            val sNum = ML + 4f
            val sDesc = ML + 30f
            val sCostRight = MR - 8f

            // Table header row
            drawTableHeaderBg()
            val headerTextY = y + 12f
            c.drawText("#", sNum, headerTextY, pTableHeader)
            c.drawText("Descripción", sDesc, headerTextY, pTableHeader)
            rightAlignAt("Costo", sCostRight, headerTextY, pTableHeader)
            y += TABLE_HEADER_H + 2f

            data.serviceLines.forEachIndexed { i, sl ->
                ensureSpace(if (sl.discount > 0) ROW_H * 2 else ROW_H)
                drawRowBg(i)
                val rowTextY = y + 10f
                c.drawText("${i + 1}", sNum, rowTextY, pBody)
                val maxDescW = MR - 90f - sDesc
                c.drawText(truncate(sl.description, pBody, maxDescW), sDesc, rowTextY, pBody)
                if (sl.discount > 0) {
                    rightAlignAt(money(sl.laborCost), sCostRight, rowTextY, pMoney)
                    y += ROW_H - 2f
                    val pDiscount = paint(8f, 0xFFD32F2F.toInt())
                    rightAlignAt("Desc: -${money(sl.discount)} = ${money(sl.laborCost - sl.discount)}", sCostRight, y + 10f, pDiscount)
                } else {
                    rightAlignAt(money(sl.laborCost), sCostRight, rowTextY, pMoney)
                }
                y += ROW_H
            }

            // Subtotal
            y += 4f
            ensureSpace(16f)
            c.drawLine(MR - 160f, y, MR, y, pLine)
            y += 12f
            c.drawText("Subtotal Mano de Obra:", MR - 160f, y, pMoneyBold)
            rightAlignAt(money(data.order.totalLabor), sCostRight, y, pMoneyBold)
            y += 6f
            hLine()
            y += SECTION_GAP
        }

        // ══════════════════════════════════════════
        //  PARTS TABLE
        // ══════════════════════════════════════════
        if (data.orderParts.isNotEmpty()) {
            sectionHeader("REPUESTOS")

            val pNum = ML + 4f
            val pName = ML + 30f
            val pQtyRight = MR - 150f
            val pUnitRight = MR - 85f
            val pSubRight = MR - 8f

            drawTableHeaderBg()
            val headerTextY = y + 12f
            c.drawText("#", pNum, headerTextY, pTableHeader)
            c.drawText("Repuesto", pName, headerTextY, pTableHeader)
            rightAlignAt("Cant.", pQtyRight, headerTextY, pTableHeader)
            rightAlignAt("P. Unit.", pUnitRight, headerTextY, pTableHeader)
            rightAlignAt("Subtotal", pSubRight, headerTextY, pTableHeader)
            y += TABLE_HEADER_H + 2f

            data.orderParts.forEachIndexed { i, wp ->
                ensureSpace(if (wp.discount > 0) ROW_H * 2 else ROW_H)
                drawRowBg(i)
                val part = data.availableParts.find { it.id == wp.partId }
                val name = part?.let { "${it.code ?: ""} ${it.name}".trim() } ?: "Repuesto #${wp.partId}"
                val rowTextY = y + 10f
                c.drawText("${i + 1}", pNum, rowTextY, pBody)
                c.drawText(truncate(name, pBody, pQtyRight - 50f - pName), pName, rowTextY, pBody)
                rightAlignAt("${wp.quantity}", pQtyRight, rowTextY, pBody)
                rightAlignAt(money(wp.appliedUnitPrice), pUnitRight, rowTextY, pMoney)
                rightAlignAt(money(wp.subtotal), pSubRight, rowTextY, pMoney)
                if (wp.discount > 0) {
                    y += ROW_H - 2f
                    val pDiscount = paint(8f, 0xFFD32F2F.toInt())
                    rightAlignAt("Desc: -${money(wp.discount)} = ${money(wp.subtotal - wp.discount)}", pSubRight, y + 10f, pDiscount)
                }
                y += ROW_H
            }

            y += 4f
            ensureSpace(16f)
            c.drawLine(MR - 160f, y, MR, y, pLine)
            y += 12f
            c.drawText("Subtotal Repuestos:", MR - 160f, y, pMoneyBold)
            rightAlignAt(money(data.order.totalParts), pSubRight, y, pMoneyBold)
            y += 6f
            hLine()
            y += SECTION_GAP
        }

        // ══════════════════════════════════════════
        //  PAYMENTS TABLE
        // ══════════════════════════════════════════
        if (data.payments.isNotEmpty()) {
            sectionHeader("PAGOS REGISTRADOS")

            val pyDate = ML + 4f
            val pyMethod = ML + 100f
            val pyNotes = ML + 210f
            val pyAmountRight = MR - 8f

            drawTableHeaderBg()
            val headerTextY = y + 12f
            c.drawText("Fecha", pyDate, headerTextY, pTableHeader)
            c.drawText("Método", pyMethod, headerTextY, pTableHeader)
            c.drawText("Notas", pyNotes, headerTextY, pTableHeader)
            rightAlignAt("Monto", pyAmountRight, headerTextY, pTableHeader)
            y += TABLE_HEADER_H + 2f

            data.payments.forEachIndexed { i, pay ->
                ensureSpace(ROW_H)
                drawRowBg(i)
                val rowTextY = y + 10f
                c.drawText(dateFmt.format(Date(pay.date)), pyDate, rowTextY, pBody)
                c.drawText(pay.method.displayName, pyMethod, rowTextY, pBody)
                val notesText = buildString {
                    if (pay.discount > 0) append("Desc: ${money(pay.discount)} ")
                    append(pay.notes ?: "")
                }.trim()
                c.drawText(truncate(notesText, pBody, MR - 90f - pyNotes), pyNotes, rowTextY, pBody)
                rightAlignAt(money(pay.amount), pyAmountRight, rowTextY, pMoney)
                y += ROW_H
            }

            val totalPaid = data.payments.sumOf { it.amount }
            val totalDiscounts = data.payments.sumOf { it.discount }
            y += 4f
            ensureSpace(16f)
            c.drawLine(MR - 160f, y, MR, y, pLine)
            y += 12f
            if (totalDiscounts > 0) {
                c.drawText("Total Descuentos:", MR - 160f, y, pMoneyBold)
                rightAlignAt("-${money(totalDiscounts)}", pyAmountRight, y, pMoneyBold)
                y += 14f
                ensureSpace(16f)
            }
            c.drawText("Total Pagado:", MR - 160f, y, pMoneyBold)
            rightAlignAt(money(totalPaid), pyAmountRight, y, pMoneyBold)
            y += 6f
            hLine()
            y += SECTION_GAP
        }

        // ══════════════════════════════════════════
        //  TOTALS BOX
        // ══════════════════════════════════════════
        val totalPaidFinal = data.payments.sumOf { it.amount }
        val totalDiscountsFinal = data.payments.sumOf { it.discount }
        val balance = data.order.total - totalPaidFinal - totalDiscountsFinal
        val hasPayments = data.payments.isNotEmpty()
        val boxH = if (hasPayments) 80f else 60f

        ensureSpace(boxH + 8f)

        // Right-aligned totals box (half width)
        val boxLeft = MR - 260f
        c.drawRoundRect(RectF(boxLeft, y, MR, y + boxH), 6f, 6f, bgTotal)

        val labelX = boxLeft + 14f
        val valueX = MR - 14f  // right edge for right-aligned values

        y += 16f
        c.drawText("Mano de Obra:", labelX, y, pTotalLabel)
        rightAlignAt(money(data.order.totalLabor), valueX, y, pTotalLabel)

        y += 16f
        c.drawText("Repuestos:", labelX, y, pTotalLabel)
        rightAlignAt(money(data.order.totalParts), valueX, y, pTotalLabel)

        y += 4f
        c.drawLine(labelX, y, valueX, y, pLine)
        y += 14f

        c.drawText("TOTAL:", labelX, y, pTotal)
        rightAlignAt(money(data.order.total), valueX, y, pTotal)

        if (hasPayments) {
            y += 16f
            val balColor = if (balance > 0.01) 0xFFD32F2F.toInt() else 0xFF388E3C.toInt()
            val pBal = paint(11f, balColor, bold = true)
            val balLabel = if (balance > 0.01) "Saldo Pendiente:" else "Pagado:"
            c.drawText(balLabel, labelX, y, pBal)
            rightAlignAt(money(balance), valueX, y, pBal)
        }

        y += 20f

        // ══════════════════════════════════════════
        //  PHOTOS (thumbnails)
        // ══════════════════════════════════════════
        if (data.photoPaths.isNotEmpty()) {
            ensureSpace(100f)
            sectionHeader("FOTOS DE LA ORDEN")

            val thumbSize = 80f
            val thumbGap = 8f
            var thumbX = ML

            data.photoPaths.forEach { path ->
                val file = File(path)
                if (!file.exists()) return@forEach

                // Check if we need to wrap to next row
                if (thumbX + thumbSize > MR) {
                    thumbX = ML
                    y += thumbSize + thumbGap
                    ensureSpace(thumbSize + thumbGap)
                }

                val bmp = decodeThumbnail(file, thumbSize.toInt())
                if (bmp != null) {
                    // Draw border
                    val borderPaint = Paint().apply {
                        color = COL_DIVIDER
                        style = Paint.Style.STROKE
                        strokeWidth = 0.8f
                        isAntiAlias = true
                    }
                    c.drawRoundRect(RectF(thumbX - 1f, y - 1f, thumbX + thumbSize + 1f, y + thumbSize + 1f), 4f, 4f, borderPaint)
                    // Draw image
                    c.drawBitmap(bmp, null, RectF(thumbX, y, thumbX + thumbSize, y + thumbSize), null)
                    bmp.recycle()
                }
                thumbX += thumbSize + thumbGap
            }

            y += thumbSize + SECTION_GAP
        }

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
        val plateSuffix = if (data.vehiclePlate.isNotBlank()) "_${data.vehiclePlate.replace(" ", "_")}" else ""
        val file = File(dir, "OT_${data.order.id}${plateSuffix}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()

        return file
    }

    // ── Funciones auxiliares ──────────────────────────────────────────

    private fun getLogoBitmap(context: Context, sizePx: Int): Bitmap? {
        return try {
            // Decode at high resolution then scale down for quality
            val opts = BitmapFactory.Options().apply { inScaled = false }
            val bmp = BitmapFactory.decodeResource(context.resources, R.drawable.servielecar_logo, opts)
                ?: BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher, opts)
                ?: return null
            Bitmap.createScaledBitmap(bmp, sizePx, sizePx, true)
        } catch (_: Exception) {
            null
        }
    }

    /** Decodifica una foto como miniatura eficiente usando muestreo reducido. */
    private fun decodeThumbnail(file: File, sizePx: Int): Bitmap? {
        return try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, opts)
            val w = opts.outWidth
            val h = opts.outHeight
            var inSample = 1
            while (w / inSample > sizePx * 2 || h / inSample > sizePx * 2) inSample *= 2
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = inSample }
            val bmp = BitmapFactory.decodeFile(file.absolutePath, decodeOpts) ?: return null
            Bitmap.createScaledBitmap(bmp, sizePx, sizePx, true)
        } catch (_: Exception) {
            null
        }
    }

    /** Mapea cada estado de orden a su color representativo para el badge del PDF. */
    private fun statusColor(status: OrderStatus): Int = when (status) {
        OrderStatus.RECIBIDO -> 0xFF2196F3.toInt()
        OrderStatus.EN_DIAGNOSTICO -> 0xFFFF9800.toInt()
        OrderStatus.EN_PROCESO -> 0xFF4CAF50.toInt()
        OrderStatus.EN_ESPERA_REPUESTO -> 0xFFF44336.toInt()
        OrderStatus.LISTO -> 0xFF8BC34A.toInt()
        OrderStatus.ENTREGADO -> 0xFF607D8B.toInt()
        OrderStatus.CANCELADO -> 0xFF9E9E9E.toInt()
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

    /** Divide texto en líneas que no excedan el ancho máximo dado. */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (maxWidth <= 0 || text.isEmpty()) return listOf(text)
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var cur = ""
        for (w in words) {
            val test = if (cur.isEmpty()) w else "$cur $w"
            if (paint.measureText(test) <= maxWidth) {
                cur = test
            } else {
                if (cur.isNotEmpty()) lines.add(cur)
                cur = w
            }
        }
        if (cur.isNotEmpty()) lines.add(cur)
        return lines.ifEmpty { listOf(text) }
    }

    private fun truncate(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var t = text
        while (t.length > 3 && paint.measureText("$t...") > maxWidth) t = t.dropLast(1)
        return "$t..."
    }
}
