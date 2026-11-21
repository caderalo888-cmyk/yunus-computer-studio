package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

data class PdfInfo(
    val pageCount: Int,
    val fileSizeBytes: Long,
    val firstPagePreview: Bitmap?
)

enum class PdfPageSizeMode(val label: String) {
    A4_PORTRAIT("A4 Portrait"),
    LETTER("US Letter"),
    FIT_IMAGE("Fit to Image")
}

object PdfEngine {

    fun inspectPdf(context: Context, uri: Uri): PdfInfo? {
        return runCatching {
            val tempFile = copyUriToTemp(context, uri, "inspect.pdf") ?: return null
            val size = tempFile.length()
            ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val count = renderer.pageCount
                    val preview = if (count > 0) {
                        renderer.openPage(0).use { page ->
                            val scale = 600f / page.width.coerceAtLeast(1)
                            val bmp = Bitmap.createBitmap(
                                (page.width * scale).roundToInt().coerceAtLeast(100),
                                (page.height * scale).roundToInt().coerceAtLeast(100),
                                Bitmap.Config.ARGB_8888
                            )
                            val c = Canvas(bmp)
                            c.drawColor(Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            bmp
                        }
                    } else null
                    PdfInfo(count, size, preview)
                }
            }
        }.getOrNull()
    }

    fun imagesToPdf(
        context: Context,
        imageUris: List<Uri>,
        outputFile: File,
        pageSizeMode: PdfPageSizeMode = PdfPageSizeMode.A4_PORTRAIT,
        marginPx: Int = 28
    ): Result<File> = runCatching {
        require(imageUris.isNotEmpty()) { "Select at least one image." }
        val doc = PdfDocument()
        try {
            imageUris.forEachIndexed { index, uri ->
                val bmp = ImageEngine.decodeUri(context, uri, maxDim = 1600)
                    ?: error("Failed to decode image #${index + 1}")
                val (pageW, pageH) = when (pageSizeMode) {
                    PdfPageSizeMode.A4_PORTRAIT -> 595 to 842
                    PdfPageSizeMode.LETTER -> 612 to 792
                    PdfPageSizeMode.FIT_IMAGE -> bmp.width.coerceIn(300, 1400) to
                        ((bmp.height.toFloat() / bmp.width) * bmp.width.coerceIn(300, 1400)).roundToInt()
                }
                val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, index + 1).create()
                val page = doc.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)
                val availW = (pageW - marginPx * 2).coerceAtLeast(100)
                val availH = (pageH - marginPx * 2).coerceAtLeast(100)
                val scale = minOf(availW.toFloat() / bmp.width, availH.toFloat() / bmp.height)
                val drawW = (bmp.width * scale).roundToInt()
                val drawH = (bmp.height * scale).roundToInt()
                val left = (pageW - drawW) / 2
                val top = (pageH - drawH) / 2
                canvas.drawBitmap(
                    bmp,
                    null,
                    Rect(left, top, left + drawW, top + drawH),
                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                )
                doc.finishPage(page)
            }
            FileOutputStream(outputFile).use { doc.writeTo(it) }
            outputFile
        } finally {
            doc.close()
        }
    }

    fun mergePdfs(context: Context, pdfUris: List<Uri>, outputFile: File): Result<File> = runCatching {
        require(pdfUris.size >= 2) { "Select at least 2 PDF files to merge." }
        val outDoc = PdfDocument()
        var pageNum = 1
        try {
            pdfUris.forEachIndexed { idx, uri ->
                val temp = copyUriToTemp(context, uri, "merge_$idx.pdf")
                    ?: error("Cannot read PDF #${idx + 1}")
                ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        for (i in 0 until renderer.pageCount) {
                            renderer.openPage(i).use { srcPage ->
                                val scale = 2.0f // 144 DPI crisp render
                                val bmp = Bitmap.createBitmap(
                                    (srcPage.width * scale).roundToInt(),
                                    (srcPage.height * scale).roundToInt(),
                                    Bitmap.Config.ARGB_8888
                                )
                                Canvas(bmp).drawColor(Color.WHITE)
                                srcPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                                val pageInfo = PdfDocument.PageInfo.Builder(srcPage.width, srcPage.height, pageNum++).create()
                                val dstPage = outDoc.startPage(pageInfo)
                                dstPage.canvas.drawBitmap(
                                    bmp,
                                    null,
                                    Rect(0, 0, srcPage.width, srcPage.height),
                                    Paint(Paint.FILTER_BITMAP_FLAG)
                                )
                                outDoc.finishPage(dstPage)
                            }
                        }
                    }
                }
            }
            FileOutputStream(outputFile).use { outDoc.writeTo(it) }
            outputFile
        } finally {
            outDoc.close()
        }
    }

    fun splitPdf(
        context: Context,
        pdfUri: Uri,
        pageRangeExpr: String,
        outputFile: File
    ): Result<File> = runCatching {
        val temp = copyUriToTemp(context, pdfUri, "split_src.pdf") ?: error("Unable to open PDF")
        val outDoc = PdfDocument()
        try {
            ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val indices = parsePageRanges(pageRangeExpr, renderer.pageCount)
                    require(indices.isNotEmpty()) { "Invalid page range. Example: 1-3, 5" }
                    indices.forEachIndexed { outIdx, pageZeroIdx ->
                        renderer.openPage(pageZeroIdx).use { srcPage ->
                            val scale = 2.0f
                            val bmp = Bitmap.createBitmap(
                                (srcPage.width * scale).roundToInt(),
                                (srcPage.height * scale).roundToInt(),
                                Bitmap.Config.ARGB_8888
                            )
                            Canvas(bmp).drawColor(Color.WHITE)
                            srcPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                            val pageInfo = PdfDocument.PageInfo.Builder(srcPage.width, srcPage.height, outIdx + 1).create()
                            val dstPage = outDoc.startPage(pageInfo)
                            dstPage.canvas.drawBitmap(
                                bmp,
                                null,
                                Rect(0, 0, srcPage.width, srcPage.height),
                                Paint(Paint.FILTER_BITMAP_FLAG)
                            )
                            outDoc.finishPage(dstPage)
                        }
                    }
                }
            }
            FileOutputStream(outputFile).use { outDoc.writeTo(it) }
            outputFile
        } finally {
            outDoc.close()
        }
    }

    fun pdfToImages(
        context: Context,
        pdfUri: Uri,
        outputDir: File,
        prefix: String
    ): Result<List<File>> = runCatching {
        val temp = copyUriToTemp(context, pdfUri, "pdf2img.pdf") ?: error("Unable to open PDF")
        val created = mutableListOf<File>()
        ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { page ->
                        val scale = 2.2f
                        val bmp = Bitmap.createBitmap(
                            (page.width * scale).roundToInt(),
                            (page.height * scale).roundToInt(),
                            Bitmap.Config.ARGB_8888
                        )
                        Canvas(bmp).drawColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val file = File(outputDir, "${prefix}_page_${i + 1}.png")
                        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        created.add(file)
                    }
                }
            }
        }
        created
    }

    fun compressAndWatermarkPdf(
        context: Context,
        pdfUri: Uri,
        outputFile: File,
        renderScale: Float = 1.25f,
        grayscale: Boolean = false,
        watermarkText: String = "",
        addPageNumbers: Boolean = false
    ): Result<File> = runCatching {
        val temp = copyUriToTemp(context, pdfUri, "proc_src.pdf") ?: error("Unable to open PDF")
        val outDoc = PdfDocument()
        try {
            ParcelFileDescriptor.open(temp, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val total = renderer.pageCount
                    for (i in 0 until total) {
                        renderer.openPage(i).use { srcPage ->
                            val bmp = Bitmap.createBitmap(
                                (srcPage.width * renderScale).roundToInt().coerceAtLeast(100),
                                (srcPage.height * renderScale).roundToInt().coerceAtLeast(100),
                                Bitmap.Config.ARGB_8888
                            )
                            Canvas(bmp).drawColor(Color.WHITE)
                            srcPage.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                            val pageInfo = PdfDocument.PageInfo.Builder(srcPage.width, srcPage.height, i + 1).create()
                            val dstPage = outDoc.startPage(pageInfo)
                            val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                                if (grayscale) {
                                    colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                                }
                            }
                            dstPage.canvas.drawBitmap(
                                bmp,
                                null,
                                Rect(0, 0, srcPage.width, srcPage.height),
                                paint
                            )
                            if (watermarkText.isNotBlank()) {
                                val wmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = Color.argb(65, 180, 30, 30)
                                    textSize = (srcPage.width * 0.08f).coerceIn(18f, 52f)
                                    textAlign = Paint.Align.CENTER
                                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                }
                                dstPage.canvas.save()
                                dstPage.canvas.rotate(-35f, srcPage.width / 2f, srcPage.height / 2f)
                                dstPage.canvas.drawText(watermarkText, srcPage.width / 2f, srcPage.height / 2f, wmPaint)
                                dstPage.canvas.restore()
                            }
                            if (addPageNumbers) {
                                val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    color = Color.DKGRAY
                                    textSize = 11f
                                    textAlign = Paint.Align.RIGHT
                                }
                                dstPage.canvas.drawText(
                                    "Page ${i + 1} of $total",
                                    srcPage.width - 24f,
                                    srcPage.height - 16f,
                                    numPaint
                                )
                            }
                            outDoc.finishPage(dstPage)
                        }
                    }
                }
            }
            FileOutputStream(outputFile).use { outDoc.writeTo(it) }
            outputFile
        } finally {
            outDoc.close()
        }
    }

    fun parsePageRanges(expr: String, maxPage: Int): List<Int> {
        val result = linkedSetOf<Int>()
        val parts = expr.split(",")
        for (raw in parts) {
            val p = raw.trim()
            if (p.isEmpty()) continue
            if ("-" in p) {
                val bounds = p.split("-")
                val start = bounds.getOrNull(0)?.trim()?.toIntOrNull() ?: continue
                val end = bounds.getOrNull(1)?.trim()?.toIntOrNull() ?: continue
                for (num in minOf(start, end)..maxOf(start, end)) {
                    if (num in 1..maxPage) result.add(num - 1)
                }
            } else {
                val num = p.toIntOrNull() ?: continue
                if (num in 1..maxPage) result.add(num - 1)
            }
        }
        return result.toList()
    }

    private fun copyUriToTemp(context: Context, uri: Uri, name: String): File? {
        return runCatching {
            val file = File(context.cacheDir, name)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            } ?: return null
            file
        }.getOrNull()
    }
}
