/**
 * XlsxWriter.kt - Escritor mínimo de archivos .xlsx sin librerías externas.
 *
 * Un xlsx es un ZIP con XMLs del estándar Office Open XML. Este escritor
 * genera lo justo para que Excel/Sheets/WPS lo abran: tipos de contenido,
 * relaciones, workbook, un styles.xml mínimo (cabeceras en negrita) y una
 * hoja por cada `sheet()` declarado.
 *
 * Celdas soportadas: String (texto inline), Int/Long/Double (numéricas, de
 * modo que Excel puede sumarlas), null (celda vacía). La primera fila de
 * cada hoja se escribe en negrita (se asume cabecera).
 */
package com.example.serviaux.util

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class XlsxWriter {

    class Sheet internal constructor(val name: String) {
        internal val rows = mutableListOf<List<Any?>>()

        /** Agrega una fila; cada argumento es una celda (String, Int, Long, Double o null). */
        fun row(vararg cells: Any?) {
            rows.add(cells.toList())
        }
    }

    private val sheets = mutableListOf<Sheet>()

    fun sheet(name: String, block: Sheet.() -> Unit): XlsxWriter {
        sheets.add(Sheet(sanitizeSheetName(name)).apply(block))
        return this
    }

    fun writeTo(file: File) {
        require(sheets.isNotEmpty()) { "El workbook necesita al menos una hoja" }
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            fun entry(path: String, content: String) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }

            entry("[Content_Types].xml", contentTypesXml())
            entry("_rels/.rels", relsXml())
            entry("xl/workbook.xml", workbookXml())
            entry("xl/_rels/workbook.xml.rels", workbookRelsXml())
            entry("xl/styles.xml", STYLES_XML)
            sheets.forEachIndexed { index, sheet ->
                entry("xl/worksheets/sheet${index + 1}.xml", sheetXml(sheet))
            }
        }
    }

    // ── XMLs ────────────────────────────────────────────────────────────

    private fun contentTypesXml(): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
        append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
        append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
        append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
        append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
        sheets.indices.forEach { i ->
            append("<Override PartName=\"/xl/worksheets/sheet${i + 1}.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>")
        }
        append("</Types>")
    }

    private fun relsXml(): String =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
            "</Relationships>"

    private fun workbookXml(): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" ")
        append("xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">")
        append("<sheets>")
        sheets.forEachIndexed { i, sheet ->
            append("<sheet name=\"${escapeXml(sheet.name)}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>")
        }
        append("</sheets>")
        append("</workbook>")
    }

    private fun workbookRelsXml(): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">")
        sheets.indices.forEach { i ->
            append("<Relationship Id=\"rId${i + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet${i + 1}.xml\"/>")
        }
        append("<Relationship Id=\"rId${sheets.size + 1}\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>")
        append("</Relationships>")
    }

    private fun sheetXml(sheet: Sheet): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        append("<sheetData>")
        sheet.rows.forEachIndexed { rowIndex, cells ->
            append("<row>")
            val style = if (rowIndex == 0) " s=\"1\"" else ""
            cells.forEach { cell ->
                when (cell) {
                    null -> append("<c$style/>")
                    is Int, is Long -> append("<c$style><v>$cell</v></c>")
                    is Double -> append("<c$style><v>${formatDouble(cell)}</v></c>")
                    else -> append(
                        "<c$style t=\"inlineStr\"><is><t xml:space=\"preserve\">${escapeXml(cell.toString())}</t></is></c>"
                    )
                }
            }
            append("</row>")
        }
        append("</sheetData>")
        append("</worksheet>")
    }

    companion object {
        const val MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

        // Estilos mínimos: fuente normal (0) y negrita (1) para la fila de cabecera.
        private const val STYLES_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">" +
                "<fonts count=\"2\">" +
                "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
                "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
                "</fonts>" +
                "<fills count=\"2\">" +
                "<fill><patternFill patternType=\"none\"/></fill>" +
                "<fill><patternFill patternType=\"gray125\"/></fill>" +
                "</fills>" +
                "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
                "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
                "<cellXfs count=\"2\">" +
                "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
                "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>" +
                "</cellXfs>" +
                "</styleSheet>"

        /** Excel limita el nombre de hoja a 31 chars y prohíbe []:*?/\ */
        private fun sanitizeSheetName(name: String): String =
            name.replace(Regex("[\\[\\]:*?/\\\\]"), " ").trim().take(31).ifBlank { "Hoja" }

        private fun escapeXml(text: String): String = buildString(text.length) {
            for (ch in text) when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(ch)
            }
        }

        /** Montos con 2 decimales y punto decimal, sin notación científica. */
        private fun formatDouble(value: Double): String =
            String.format(java.util.Locale.US, "%.2f", value)
    }
}
