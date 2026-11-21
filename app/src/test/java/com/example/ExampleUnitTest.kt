package com.example

import com.example.engine.OfficeDocEngine
import com.example.engine.PdfEngine
import com.example.engine.QrEccLevel
import com.example.engine.QrEngine
import com.example.studio.StudioCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExampleUnitTest {

    @Test
    fun catalogHasAll21Modules() {
        assertEquals(21, StudioCatalog.modules.size)
        assertEquals((1..21).toList(), StudioCatalog.modules.map { it.number })
    }

    @Test
    fun qrEncoderProducesValidFinderAndMatrix() {
        val qr = QrEngine.encode("https://yunusstudio.pro", QrEccLevel.M)
        assertTrue(qr.size >= 21)
        // Top-left finder pattern center (3,3) must be dark, (1,1) white
        assertTrue(qr.get(3, 3))
        assertTrue(!qr.get(1, 1))
        assertTrue(qr.get(0, 0))
        // Top-right finder pattern center
        assertTrue(qr.get(qr.size - 4, 3))
    }

    @Test
    fun officeFormulaEvaluatorWorks() {
        val grid = listOf(
            listOf("Item", "Amount"),
            listOf("Photo", "100"),
            listOf("Print", "50"),
            listOf("CV", "40")
        )
        assertEquals("190", OfficeDocEngine.evaluateCell("=SUM(B2:B4)", grid))
        assertEquals("63.33", OfficeDocEngine.evaluateCell("=AVG(B2:B4)", grid))
        assertEquals("40", OfficeDocEngine.evaluateCell("=MIN(B2:B4)", grid))
        assertEquals("100", OfficeDocEngine.evaluateCell("=MAX(B2:B4)", grid))
    }

    @Test
    fun docxAndXlsxArchivesAreGenerated() {
        val docx = File.createTempFile("test_doc", ".docx")
        val xlsx = File.createTempFile("test_sheet", ".xlsx")
        try {
            val resDoc = OfficeDocEngine.writeDocx("Title", "Hello\nWorld", docx)
            assertTrue(resDoc.isSuccess && docx.length() > 100)
            val resXlsx = OfficeDocEngine.writeXlsx(
                listOf(listOf("A", "10"), listOf("B", "20"), listOf("Sum", "=SUM(B1:B2)")),
                "Sheet1",
                xlsx
            )
            assertTrue(resXlsx.isSuccess && xlsx.length() > 100)
        } finally {
            docx.delete()
            xlsx.delete()
        }
    }

    @Test
    fun pdfPageRangeParserHandlesRanges() {
        val pages = PdfEngine.parsePageRanges("1-3, 5, 99", maxPage = 6)
        assertEquals(listOf(0, 1, 2, 4), pages)
    }
}
