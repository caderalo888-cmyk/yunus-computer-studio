package com.example.engine

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.roundToInt

object OfficeDocEngine {

    private fun xmlEscape(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    fun writeDocx(title: String, body: String, outputFile: File): Result<File> = runCatching {
        val contentTypes = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
            </Types>
        """.trimIndent()

        val rels = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
            </Relationships>
        """.trimIndent()

        val docRels = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"/>
        """.trimIndent()

        val paragraphsXml = buildString {
            if (title.isNotBlank()) {
                append(
                    """
                    <w:p>
                      <w:pPr><w:spacing w:after="200"/></w:pPr>
                      <w:r>
                        <w:rPr><w:b/><w:sz w:val="36"/><w:color w:val="0F172A"/></w:rPr>
                        <w:t xml:space="preserve">${xmlEscape(title)}</w:t>
                      </w:r>
                    </w:p>
                    """.trimIndent()
                )
            }
            body.lines().forEach { line ->
                val isHeading = line.startsWith("# ")
                val clean = if (isHeading) line.removePrefix("# ") else line
                val rPr = if (isHeading) "<w:rPr><w:b/><w:sz w:val=\"28\"/></w:rPr>" else "<w:rPr><w:sz w:val=\"22\"/></w:rPr>"
                append(
                    """
                    <w:p>
                      <w:pPr><w:spacing w:after="120"/></w:pPr>
                      <w:r>$rPr<w:t xml:space="preserve">${xmlEscape(clean)}</w:t></w:r>
                    </w:p>
                    """.trimIndent()
                )
            }
        }

        val documentXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:body>
                $paragraphsXml
                <w:sectPr><w:pgSz w:w="11906" w:h="16838"/></w:sectPr>
              </w:body>
            </w:document>
        """.trimIndent()

        ZipOutputStream(FileOutputStream(outputFile)).use { zip ->
            fun put(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            put("[Content_Types].xml", contentTypes)
            put("_rels/.rels", rels)
            put("word/_rels/document.xml.rels", docRels)
            put("word/document.xml", documentXml)
        }
        outputFile
    }

    fun readDocxOrText(context: Context, uri: Uri): Result<String> = runCatching {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Unable to read file")
        // Check ZIP magic PK\x03\x04
        if (bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
            var docXmlBytes: ByteArray? = null
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        docXmlBytes = zis.readBytes()
                        break
                    }
                    entry = zis.nextEntry
                }
            }
            val xmlBytes = docXmlBytes ?: error("Not a valid DOCX word/document.xml archive")
            parseWordXml(xmlBytes)
        } else {
            String(bytes, Charsets.UTF_8)
        }
    }

    private fun parseWordXml(xmlBytes: ByteArray): String {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")
        val sb = StringBuilder()
        var event = parser.eventType
        var insideT = false
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name
                    if (name == "w:t" || name == "t") insideT = true
                    if (name == "w:tab" || name == "tab") sb.append("\t")
                    if (name == "w:br" || name == "br") sb.append("\n")
                }
                XmlPullParser.TEXT -> {
                    if (insideT) sb.append(parser.text ?: "")
                }
                XmlPullParser.END_TAG -> {
                    val name = parser.name
                    if (name == "w:t" || name == "t") insideT = false
                    if (name == "w:p" || name == "p") sb.append("\n")
                }
            }
            event = parser.next()
        }
        return sb.toString().trimEnd()
    }

    fun writeTextToPdf(title: String, body: String, outputFile: File): Result<File> = runCatching {
        val doc = PdfDocument()
        try {
            val pageW = 595
            val pageH = 842
            val margin = 44f
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.DKGRAY
                textSize = 11.5f
            }

            val wrappedLines = mutableListOf<String>()
            val maxLineW = pageW - margin * 2
            body.lines().forEach { rawLine ->
                if (rawLine.isBlank()) {
                    wrappedLines.add("")
                } else {
                    var rem = rawLine
                    while (rem.isNotEmpty()) {
                        val count = bodyPaint.breakText(rem, true, maxLineW, null).coerceAtLeast(1)
                        wrappedLines.add(rem.substring(0, count))
                        rem = rem.substring(count)
                    }
                }
            }

            var pageNum = 1
            var lineIdx = 0
            while (lineIdx < wrappedLines.size || pageNum == 1) {
                val page = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNum).create())
                val canvas: Canvas = page.canvas
                canvas.drawColor(Color.WHITE)
                var y = margin + 12f
                if (pageNum == 1 && title.isNotBlank()) {
                    canvas.drawText(title, margin, y, titlePaint)
                    y += 28f
                }
                while (lineIdx < wrappedLines.size && y < pageH - margin) {
                    canvas.drawText(wrappedLines[lineIdx], margin, y, bodyPaint)
                    y += 17f
                    lineIdx++
                }
                doc.finishPage(page)
                pageNum++
                if (lineIdx >= wrappedLines.size) break
            }
            FileOutputStream(outputFile).use { doc.writeTo(it) }
            outputFile
        } finally {
            doc.close()
        }
    }

    // ---- Excel / Spreadsheet Engine ----

    private fun colIndexToLetters(idx: Int): String {
        var n = idx
        val sb = StringBuilder()
        while (n >= 0) {
            sb.insert(0, ('A' + (n % 26)))
            n = n / 26 - 1
        }
        return sb.toString()
    }

    private fun lettersToColIndex(letters: String): Int {
        var res = 0
        for (ch in letters.uppercase()) {
            if (ch in 'A'..'Z') res = res * 26 + (ch - 'A' + 1)
        }
        return res - 1
    }

    fun evaluateCell(raw: String, grid: List<List<String>>): String {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("=")) return raw
        val expr = trimmed.substring(1).trim().uppercase()
        val openIdx = expr.indexOf('(')
        val closeIdx = expr.lastIndexOf(')')
        if (openIdx <= 0 || closeIdx <= openIdx) return raw
        val fn = expr.substring(0, openIdx).trim()
        val arg = expr.substring(openIdx + 1, closeIdx).trim()

        val values = mutableListOf<Double>()
        if (":" in arg) {
            val parts = arg.split(":")
            val start = parseCellRef(parts.getOrNull(0) ?: "")
            val end = parseCellRef(parts.getOrNull(1) ?: "")
            if (start != null && end != null) {
                for (r in minOf(start.first, end.first)..maxOf(start.first, end.first)) {
                    for (c in minOf(start.second, end.second)..maxOf(start.second, end.second)) {
                        val cellStr = grid.getOrNull(r)?.getOrNull(c) ?: ""
                        cellStr.toDoubleOrNull()?.let { values.add(it) }
                    }
                }
            }
        } else {
            arg.split(",").forEach { token ->
                val ref = parseCellRef(token.trim())
                if (ref != null) {
                    grid.getOrNull(ref.first)?.getOrNull(ref.second)?.toDoubleOrNull()?.let { values.add(it) }
                } else {
                    token.trim().toDoubleOrNull()?.let { values.add(it) }
                }
            }
        }

        if (values.isEmpty()) return "0"
        val res = when (fn) {
            "SUM" -> values.sum()
            "AVG", "AVERAGE" -> values.average()
            "MIN" -> values.minOrNull() ?: 0.0
            "MAX" -> values.maxOrNull() ?: 0.0
            "COUNT" -> values.size.toDouble()
            else -> return raw
        }
        return if (res % 1.0 == 0.0) res.roundToInt().toString() else String.format("%.2f", res)
    }

    private fun parseCellRef(ref: String): Pair<Int, Int>? {
        val clean = ref.trim().uppercase()
        val colPart = clean.takeWhile { it in 'A'..'Z' }
        val rowPart = clean.drop(colPart.length)
        if (colPart.isEmpty() || rowPart.isEmpty()) return null
        val row = rowPart.toIntOrNull()?.minus(1) ?: return null
        val col = lettersToColIndex(colPart)
        if (row < 0 || col < 0) return null
        return row to col
    }

    fun writeXlsx(grid: List<List<String>>, sheetName: String, outputFile: File): Result<File> = runCatching {
        val contentTypes = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
              <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
            </Types>
        """.trimIndent()

        val rels = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
            </Relationships>
        """.trimIndent()

        val wbRels = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
            </Relationships>
        """.trimIndent()

        val workbook = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                      xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
              <sheets>
                <sheet name="${xmlEscape(sheetName.ifBlank { "Sheet1" })}" sheetId="1" r:id="rId1"/>
              </sheets>
            </workbook>
        """.trimIndent()

        val rowsXml = buildString {
            grid.forEachIndexed { rIdx, row ->
                append("""<row r="${rIdx + 1}">""")
                row.forEachIndexed { cIdx, rawVal ->
                    val evaluated = evaluateCell(rawVal, grid)
                    if (evaluated.isNotEmpty()) {
                        val ref = "${colIndexToLetters(cIdx)}${rIdx + 1}"
                        val num = evaluated.toDoubleOrNull()
                        if (num != null) {
                            append("""<c r="$ref"><v>$evaluated</v></c>""")
                        } else {
                            append("""<c r="$ref" t="inlineStr"><is><t>${xmlEscape(evaluated)}</t></is></c>""")
                        }
                    }
                }
                append("</row>")
            }
        }

        val sheetXml = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheetData>$rowsXml</sheetData>
            </worksheet>
        """.trimIndent()

        ZipOutputStream(FileOutputStream(outputFile)).use { zip ->
            fun put(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            put("[Content_Types].xml", contentTypes)
            put("_rels/.rels", rels)
            put("xl/_rels/workbook.xml.rels", wbRels)
            put("xl/workbook.xml", workbook)
            put("xl/worksheets/sheet1.xml", sheetXml)
        }
        outputFile
    }

    fun writeCsv(grid: List<List<String>>, outputFile: File): Result<File> = runCatching {
        val text = grid.joinToString("\n") { row ->
            row.joinToString(",") { cell ->
                val v = evaluateCell(cell, grid)
                if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
                    "\"${v.replace("\"", "\"\"")}\""
                } else v
            }
        }
        outputFile.writeText(text, Charsets.UTF_8)
        outputFile
    }

    fun readCsvOrXlsx(context: Context, uri: Uri): Result<List<List<String>>> = runCatching {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Unable to read spreadsheet")
        if (bytes.size >= 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()) {
            var sharedStringsBytes: ByteArray? = null
            var sheetBytes: ByteArray? = null
            ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    when (entry.name) {
                        "xl/sharedStrings.xml" -> sharedStringsBytes = zis.readBytes()
                        "xl/worksheets/sheet1.xml" -> sheetBytes = zis.readBytes()
                    }
                    entry = zis.nextEntry
                }
            }
            val shared = sharedStringsBytes?.let { parseSharedStrings(it) } ?: emptyList()
            val sheet = sheetBytes ?: error("No sheet1.xml found inside XLSX")
            parseSheetXml(sheet, shared)
        } else {
            val csvText = String(bytes, Charsets.UTF_8)
            csvText.lines().filter { it.isNotBlank() }.map { line -> parseCsvLine(line) }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    cur.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (ch == ',' && !inQuotes) {
                result.add(cur.toString())
                cur.clear()
            } else {
                cur.append(ch)
            }
            i++
        }
        result.add(cur.toString())
        return result
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val list = mutableListOf<String>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
        var event = parser.eventType
        var inSi = false
        var inT = false
        val sb = StringBuilder()
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "si") { inSi = true; sb.clear() }
                    if (inSi && parser.name == "t") inT = true
                }
                XmlPullParser.TEXT -> if (inT) sb.append(parser.text ?: "")
                XmlPullParser.END_TAG -> {
                    if (parser.name == "t") inT = false
                    if (parser.name == "si") {
                        list.add(sb.toString())
                        inSi = false
                    }
                }
            }
            event = parser.next()
        }
        return list
    }

    private fun parseSheetXml(bytes: ByteArray, shared: List<String>): List<List<String>> {
        val rows = mutableListOf<MutableList<String>>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
        var event = parser.eventType
        var currentRow = mutableListOf<String>()
        var cellType = ""
        var cellRef = ""
        var inV = false
        var inT = false
        val valBuf = StringBuilder()

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "row" -> currentRow = mutableListOf()
                        "c" -> {
                            cellType = parser.getAttributeValue(null, "t") ?: ""
                            cellRef = parser.getAttributeValue(null, "r") ?: ""
                            valBuf.clear()
                        }
                        "v" -> inV = true
                        "t" -> inT = true
                    }
                }
                XmlPullParser.TEXT -> if (inV || inT) valBuf.append(parser.text ?: "")
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "v" -> inV = false
                        "t" -> inT = false
                        "c" -> {
                            val raw = valBuf.toString()
                            val value = if (cellType == "s") {
                                raw.toIntOrNull()?.let { shared.getOrNull(it) } ?: raw
                            } else raw
                            val colLetters = cellRef.takeWhile { it in 'A'..'Z' }
                            val colIdx = if (colLetters.isNotEmpty()) lettersToColIndex(colLetters) else currentRow.size
                            while (currentRow.size <= colIdx) currentRow.add("")
                            currentRow[colIdx] = value
                        }
                        "row" -> rows.add(currentRow)
                    }
                }
            }
            event = parser.next()
        }
        return rows
    }
}
