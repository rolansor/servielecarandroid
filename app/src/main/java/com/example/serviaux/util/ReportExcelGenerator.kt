/**
 * ReportExcelGenerator.kt - Exportación de reportes a Excel (.xlsx).
 *
 * Arma un workbook con [XlsxWriter] a partir de los datos del período
 * elegido en Reportes. Cada hoja es una variante seleccionable
 * ([ExcelSheet]): órdenes una a una, y agregados por mecánico, trabajo,
 * repuesto, tipo de vehículo y cliente.
 *
 * Los montos van como números crudos (no texto) para poder sumarlos en
 * Excel; las fechas como texto dd/MM/yyyy.
 */
package com.example.serviaux.util

import android.content.Context
import com.example.serviaux.data.entity.Customer
import com.example.serviaux.data.entity.Part
import com.example.serviaux.data.entity.ServiceLine
import com.example.serviaux.data.entity.User
import com.example.serviaux.data.entity.Vehicle
import com.example.serviaux.data.entity.WorkOrder
import com.example.serviaux.data.entity.WorkOrderMechanic
import com.example.serviaux.data.entity.WorkOrderPart
import com.example.serviaux.data.entity.WorkOrderPayment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Hojas disponibles del Excel; el usuario elige cuáles incluir. */
enum class ExcelSheet(val title: String) {
    ORDENES("Órdenes"),
    MECANICOS("Por mecánico"),
    TRABAJOS("Por trabajo"),
    REPUESTOS("Por repuesto"),
    TIPOS_VEHICULO("Por tipo de vehículo"),
    CLIENTES("Por cliente")
}

/** Datos crudos del período, ya filtrados a las órdenes del rango. */
data class ReportExportData(
    val orders: List<WorkOrder>,
    val vehiclesById: Map<Long, Vehicle>,
    val customersById: Map<Long, Customer>,
    val usersById: Map<Long, User>,
    val serviceLines: List<ServiceLine>,
    val orderParts: List<WorkOrderPart>,
    val payments: List<WorkOrderPayment>,
    val mechanics: List<WorkOrderMechanic>,
    val partsById: Map<Long, Part>
)

object ReportExcelGenerator {

    private val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale("es"))
    private val fileFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun generate(
        context: Context,
        from: Long,
        to: Long,
        data: ReportExportData,
        sheets: Set<ExcelSheet>
    ): File {
        val writer = XlsxWriter()

        // Lo pagado "efectivo" de una orden incluye el descuento del pago:
        // ambos reducen el saldo (mismo criterio que la pantalla de la orden).
        val paidByOrder = data.payments.groupBy { it.workOrderId }
            .mapValues { (_, pays) -> pays.sumOf { it.amount } }
        val paidEffectiveByOrder = data.payments.groupBy { it.workOrderId }
            .mapValues { (_, pays) -> pays.sumOf { it.amount + it.discount } }

        ExcelSheet.entries.filter { it in sheets }.forEach { sheet ->
            when (sheet) {
                ExcelSheet.ORDENES -> ordersSheet(writer, data, paidByOrder, paidEffectiveByOrder)
                ExcelSheet.MECANICOS -> mechanicsSheet(writer, data)
                ExcelSheet.TRABAJOS -> jobsSheet(writer, data)
                ExcelSheet.REPUESTOS -> partsSheet(writer, data)
                ExcelSheet.TIPOS_VEHICULO -> vehicleTypesSheet(writer, data)
                ExcelSheet.CLIENTES -> customersSheet(writer, data, paidEffectiveByOrder)
            }
        }

        val dir = File(context.filesDir, "reports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(
            dir,
            "reporte_serviaux_${fileFmt.format(Date(from))}_a_${fileFmt.format(Date(to))}.xlsx"
        )
        writer.writeTo(file)
        return file
    }

    // ── Hojas ───────────────────────────────────────────────────────────

    private fun ordersSheet(
        writer: XlsxWriter,
        data: ReportExportData,
        paidByOrder: Map<Long, Double>,
        paidEffectiveByOrder: Map<Long, Double>
    ) {
        val mechanicsByOrder = data.mechanics.groupBy { it.workOrderId }
        writer.sheet(ExcelSheet.ORDENES.title) {
            row(
                "Nº orden", "Fecha", "Placa", "Vehículo", "Tipo de vehículo", "Cliente",
                "Estado", "Mecánicos", "Mano de obra", "Repuestos", "Extras",
                "Total", "Pagado", "Saldo"
            )
            data.orders.forEach { order ->
                val vehicle = data.vehiclesById[order.vehicleId]
                val customer = data.customersById[order.customerId]
                val mechNames = mechanicsByOrder[order.id].orEmpty()
                    .mapNotNull { data.usersById[it.mechanicId]?.name }
                    .joinToString(", ")
                val saldo = order.total - (paidEffectiveByOrder[order.id] ?: 0.0)
                row(
                    order.id,
                    dateFmt.format(Date(order.createdAt)),
                    vehicle?.plate ?: "",
                    vehicle?.let {
                        listOfNotNull(it.brand, it.model, it.year?.toString()).joinToString(" ")
                    } ?: "",
                    vehicle?.vehicleType ?: "",
                    customer?.fullName ?: "",
                    order.status.displayName,
                    mechNames,
                    order.totalLabor,
                    order.totalParts,
                    order.totalExtras,
                    order.total,
                    paidByOrder[order.id] ?: 0.0,
                    saldo
                )
            }
            row(
                "TOTAL", null, null, null, null, null, null, null,
                data.orders.sumOf { it.totalLabor },
                data.orders.sumOf { it.totalParts },
                data.orders.sumOf { it.totalExtras },
                data.orders.sumOf { it.total },
                data.orders.sumOf { paidByOrder[it.id] ?: 0.0 },
                data.orders.sumOf { it.total - (paidEffectiveByOrder[it.id] ?: 0.0) }
            )
        }
    }

    private fun mechanicsSheet(writer: XlsxWriter, data: ReportExportData) {
        val ordersById = data.orders.associateBy { it.id }
        val byMechanic = data.mechanics.groupBy { it.mechanicId }
        writer.sheet(ExcelSheet.MECANICOS.title) {
            row(
                "Mecánico", "Órdenes", "Mano de obra de sus órdenes",
                "Comisión generada", "Comisión pagada", "Comisión pendiente"
            )
            byMechanic.entries
                .sortedByDescending { (_, rows) -> rows.sumOf { it.commissionAmount } }
                .forEach { (mechanicId, rows) ->
                    val orderIds = rows.map { it.workOrderId }.distinct()
                    val labor = orderIds.sumOf { ordersById[it]?.totalLabor ?: 0.0 }
                    val generated = rows.sumOf { it.commissionAmount }
                    val paid = rows.filter { it.commissionPaid }.sumOf { it.commissionAmount }
                    row(
                        data.usersById[mechanicId]?.name ?: "Mecánico #$mechanicId",
                        orderIds.size,
                        labor,
                        generated,
                        paid,
                        generated - paid
                    )
                }
            row(
                "TOTAL",
                null,
                null,
                data.mechanics.sumOf { it.commissionAmount },
                data.mechanics.filter { it.commissionPaid }.sumOf { it.commissionAmount },
                data.mechanics.filter { !it.commissionPaid }.sumOf { it.commissionAmount }
            )
        }
    }

    private fun jobsSheet(writer: XlsxWriter, data: ReportExportData) {
        val grouped = data.serviceLines.groupBy { it.description.trim().uppercase(Locale("es")) }
        writer.sheet(ExcelSheet.TRABAJOS.title) {
            row("Trabajo", "Veces realizado", "Total facturado")
            grouped.entries
                .sortedByDescending { (_, lines) -> lines.sumOf { it.laborCost - it.discount } }
                .forEach { (description, lines) ->
                    row(description, lines.size, lines.sumOf { it.laborCost - it.discount })
                }
            row("TOTAL", data.serviceLines.size,
                data.serviceLines.sumOf { it.laborCost - it.discount })
        }
    }

    private fun partsSheet(writer: XlsxWriter, data: ReportExportData) {
        val grouped = data.orderParts.groupBy { it.partId }
        writer.sheet(ExcelSheet.REPUESTOS.title) {
            row("Repuesto", "Código", "Cantidad vendida", "Total facturado")
            grouped.entries
                .sortedByDescending { (_, rows) -> rows.sumOf { it.subtotal - it.discount } }
                .forEach { (partId, rows) ->
                    val part = data.partsById[partId]
                    row(
                        part?.name ?: "Repuesto #$partId",
                        part?.code ?: "",
                        rows.sumOf { it.quantity },
                        rows.sumOf { it.subtotal - it.discount }
                    )
                }
            row("TOTAL", null,
                data.orderParts.sumOf { it.quantity },
                data.orderParts.sumOf { it.subtotal - it.discount })
        }
    }

    private fun vehicleTypesSheet(writer: XlsxWriter, data: ReportExportData) {
        val grouped = data.orders.groupBy {
            data.vehiclesById[it.vehicleId]?.vehicleType?.takeIf { t -> t.isNotBlank() }
                ?: "Sin tipo"
        }
        writer.sheet(ExcelSheet.TIPOS_VEHICULO.title) {
            row("Tipo de vehículo", "Órdenes", "Total facturado")
            grouped.entries
                .sortedByDescending { (_, orders) -> orders.sumOf { it.total } }
                .forEach { (type, orders) ->
                    row(type, orders.size, orders.sumOf { it.total })
                }
            row("TOTAL", data.orders.size, data.orders.sumOf { it.total })
        }
    }

    private fun customersSheet(
        writer: XlsxWriter,
        data: ReportExportData,
        paidEffectiveByOrder: Map<Long, Double>
    ) {
        val grouped = data.orders.groupBy { it.customerId }
        writer.sheet(ExcelSheet.CLIENTES.title) {
            row("Cliente", "Órdenes", "Total facturado", "Saldo pendiente")
            grouped.entries
                .sortedByDescending { (_, orders) -> orders.sumOf { it.total } }
                .forEach { (customerId, orders) ->
                    val saldo = orders.sumOf { it.total - (paidEffectiveByOrder[it.id] ?: 0.0) }
                    row(
                        data.customersById[customerId]?.fullName ?: "Cliente #$customerId",
                        orders.size,
                        orders.sumOf { it.total },
                        saldo
                    )
                }
            row(
                "TOTAL",
                data.orders.size,
                data.orders.sumOf { it.total },
                data.orders.sumOf { it.total - (paidEffectiveByOrder[it.id] ?: 0.0) }
            )
        }
    }
}
