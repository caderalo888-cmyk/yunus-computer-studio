package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class PhotoPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val widthPx: Int,
    val heightPx: Int,
    val mmLabel: String
)

object PhotoPresets {
    val passportPresets = listOf(
        PhotoPreset("pp_bd", "Bangladesh / Standard", "40 × 50 mm @ 300 DPI", 472, 591, "40×50 mm"),
        PhotoPreset("pp_us", "US / India 2×2\"", "51 × 51 mm @ 300 DPI", 600, 600, "51×51 mm"),
        PhotoPreset("pp_eu", "UK / EU Passport", "35 × 45 mm @ 300 DPI", 413, 531, "35×45 mm"),
        PhotoPreset("pp_stamp", "Stamp Size", "20 × 25 mm @ 300 DPI", 236, 295, "20×25 mm")
    )

    val visaPresets = listOf(
        PhotoPreset("visa_schengen", "Schengen Visa", "35 × 45 mm (White BG)", 413, 531, "35×45 mm"),
        PhotoPreset("visa_usa", "USA DS-160 Visa", "51 × 51 mm (2×2 in)", 600, 600, "51×51 mm"),
        PhotoPreset("visa_saudi", "Saudi / Gulf Visa", "40 × 60 mm (4×6 cm)", 472, 709, "40×60 mm"),
        PhotoPreset("visa_my", "Malaysia / Dubai", "35 × 50 mm (Blue/White)", 413, 591, "35×50 mm"),
        PhotoPreset("visa_ca", "Canada Visa", "50 × 70 mm @ 300 DPI", 591, 827, "50×70 mm")
    )

    val nidPresets = listOf(
        PhotoPreset("nid_bd", "Bangladesh NID", "40 × 50 mm Standard", 472, 591, "40×50 mm"),
        PhotoPreset("nid_smart", "Smart Card Photo", "25 × 30 mm Compact", 295, 354, "25×30 mm"),
        PhotoPreset("nid_tin", "TIN / Job Form", "300 × 300 px Digital", 300, 300, "300×300 px"),
        PhotoPreset("nid_bcs", "Govt / BCS Portal", "300 × 300 px (<100KB)", 300, 300, "300×300 px")
    )
}

object ImageEngine {

    fun decodeUri(context: Context, uri: Uri, maxDim: Int = 1800): Bitmap? {
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val origW = bounds.outWidth
            val origH = bounds.outHeight
            if (origW <= 0 || origH <= 0) return null

            var sample = 1
            while (origW / sample > maxDim || origH / sample > maxDim) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return null

            val rotation = context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f

            if (rotation == 0f) decoded
            else {
                val matrix = Matrix().apply { postRotate(rotation) }
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            }
        }.getOrNull()
    }

    /**
     * Crops source bitmap to exact target aspect and size with pan/zoom and optional studio border.
     */
    fun cropAndFormatIdPhoto(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        zoom: Float = 1f,
        panX: Float = 0f,
        panY: Float = 0f,
        replaceBgColor: Int? = null,
        bgTolerance: Int = 38,
        addThinBorder: Boolean = true
    ): Bitmap {
        val out = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        canvas.drawColor(replaceBgColor ?: Color.WHITE)

        val processedSource = if (replaceBgColor != null) {
            val cutout = removeBackgroundFloodFill(source, tolerance = bgTolerance, feather = 2)
            val flattened = Bitmap.createBitmap(cutout.width, cutout.height, Bitmap.Config.ARGB_8888)
            val fc = Canvas(flattened)
            fc.drawColor(replaceBgColor)
            fc.drawBitmap(cutout, 0f, 0f, null)
            flattened
        } else {
            source
        }

        val scaleBase = max(
            targetWidth.toFloat() / processedSource.width,
            targetHeight.toFloat() / processedSource.height
        )
        val scale = scaleBase * zoom.coerceIn(0.8f, 3.0f)
        val drawW = processedSource.width * scale
        val drawH = processedSource.height * scale
        val left = (targetWidth - drawW) / 2f + panX * targetWidth * 0.35f
        val top = (targetHeight - drawH) / 2f + panY * targetHeight * 0.35f

        val dstRect = RectF(left, top, left + drawW, top + drawH)
        canvas.drawBitmap(processedSource, null, dstRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))

        if (addThinBorder) {
            val borderPaint = Paint().apply {
                color = Color.argb(90, 80, 90, 110)
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRect(1f, 1f, targetWidth - 1f, targetHeight - 1f, borderPaint)
        }
        return out
    }

    /**
     * Tiles an ID photo across an A4 (1240x1754) or 4R (1200x1800) print sheet with crop marks.
     */
    fun composePrintSheet(
        photo: Bitmap,
        copies: Int,
        isA4Sheet: Boolean = true,
        showCutMarks: Boolean = true
    ): Bitmap {
        val sheetW = if (isA4Sheet) 1240 else 1200
        val sheetH = if (isA4Sheet) 1754 else 1800
        val sheet = Bitmap.createBitmap(sheetW, sheetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)
        canvas.drawColor(Color.WHITE)

        val margin = 44
        val gap = 22
        val aspect = photo.height.toFloat() / photo.width.toFloat()
        val cols = when {
            copies <= 2 -> 2
            copies <= 6 -> 3
            copies <= 12 -> 4
            else -> 5
        }
        val cellW = ((sheetW - margin * 2 - gap * (cols - 1)) / cols).coerceAtMost(
            if (isA4Sheet) 260 else 320
        )
        val cellH = (cellW * aspect).roundToInt()

        val cutPaint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 1.5f
        }
        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        var placed = 0
        var row = 0
        while (placed < copies) {
            val top = margin + row * (cellH + gap)
            if (top + cellH > sheetH - margin) break
            for (col in 0 until cols) {
                if (placed >= copies) break
                val left = margin + col * (cellW + gap)
                val dst = Rect(left, top, left + cellW, top + cellH)
                canvas.drawBitmap(photo, null, dst, bitmapPaint)
                if (showCutMarks) {
                    val mark = 10f
                    // Top-left & bottom-right subtle crop ticks
                    canvas.drawLine(left - mark, top.toFloat(), left.toFloat(), top.toFloat(), cutPaint)
                    canvas.drawLine(left.toFloat(), top - mark, left.toFloat(), top.toFloat(), cutPaint)
                    canvas.drawLine((left + cellW).toFloat(), top.toFloat(), left + cellW + mark, top.toFloat(), cutPaint)
                    canvas.drawLine((left + cellW).toFloat(), top + cellH.toFloat(), (left + cellW).toFloat(), top + cellH + mark, cutPaint)
                }
                placed++
            }
            row++
        }
        return sheet
    }

    /**
     * Real brightness, contrast, saturation, auto-levels, and 3x3 unsharp-mask HD sharpening.
     */
    fun enhanceBitmap(
        source: Bitmap,
        brightness: Float = 0f,   // -100..100
        contrast: Float = 1f,     // 0.5..2.0
        saturation: Float = 1f,   // 0..2.0
        sharpness: Float = 0f,    // 0..1.0
        autoLevels: Boolean = false
    ): Bitmap {
        val working = if (autoLevels) applyAutoLevels(source) else source.copy(Bitmap.Config.ARGB_8888, true)

        val colorBitmap = Bitmap.createBitmap(working.width, working.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(colorBitmap)

        val satMatrix = ColorMatrix().apply { setSaturation(saturation) }
        val c = contrast
        val t = (1f - c) * 128f + brightness * 1.25f
        val bcMatrix = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
        )
        satMatrix.postConcat(bcMatrix)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(satMatrix)
        }
        canvas.drawBitmap(working, 0f, 0f, paint)

        return if (sharpness > 0.05f) {
            applyUnsharpKernel(colorBitmap, sharpness)
        } else {
            colorBitmap
        }
    }

    private fun applyAutoLevels(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        var minL = 255
        var maxL = 0
        for (p in pixels) {
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val lum = (r * 77 + g * 150 + b * 29) shr 8
            if (lum < minL) minL = lum
            if (lum > maxL) maxL = lum
        }
        val span = (maxL - minL).coerceAtLeast(16)
        if (span > 235 && minL < 12) return src.copy(Bitmap.Config.ARGB_8888, true)

        val scale = 255f / span
        for (i in pixels.indices) {
            val p = pixels[i]
            val a = (p ushr 24) and 0xFF
            val r = (((p shr 16) and 0xFF) - minL) * scale
            val g = (((p shr 8) and 0xFF) - minL) * scale
            val b = ((p and 0xFF) - minL) * scale
            pixels[i] = (a shl 24) or
                (r.roundToInt().coerceIn(0, 255) shl 16) or
                (g.roundToInt().coerceIn(0, 255) shl 8) or
                b.roundToInt().coerceIn(0, 255)
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun applyUnsharpKernel(src: Bitmap, amount: Float): Bitmap {
        val w = src.width
        val h = src.height
        if (w < 3 || h < 3) return src
        val input = IntArray(w * h)
        val output = IntArray(w * h)
        src.getPixels(input, 0, w, 0, 0, w, h)
        System.arraycopy(input, 0, output, 0, input.size)

        val k = amount.coerceIn(0f, 1.2f) * 0.85f
        val centerWeight = 1f + 4f * k
        val edgeWeight = -k

        for (y in 1 until h - 1) {
            val row = y * w
            for (x in 1 until w - 1) {
                val idx = row + x
                val c = input[idx]
                val u = input[idx - w]
                val d = input[idx + w]
                val l = input[idx - 1]
                val r = input[idx + 1]

                val a = (c ushr 24) and 0xFF
                val rr = ((c shr 16 and 0xFF) * centerWeight +
                    ((u shr 16 and 0xFF) + (d shr 16 and 0xFF) + (l shr 16 and 0xFF) + (r shr 16 and 0xFF)) * edgeWeight)
                    .roundToInt().coerceIn(0, 255)
                val gg = ((c shr 8 and 0xFF) * centerWeight +
                    ((u shr 8 and 0xFF) + (d shr 8 and 0xFF) + (l shr 8 and 0xFF) + (r shr 8 and 0xFF)) * edgeWeight)
                    .roundToInt().coerceIn(0, 255)
                val bb = ((c and 0xFF) * centerWeight +
                    ((u and 0xFF) + (d and 0xFF) + (l and 0xFF) + (r and 0xFF)) * edgeWeight)
                    .roundToInt().coerceIn(0, 255)
                output[idx] = (a shl 24) or (rr shl 16) or (gg shl 8) or bb
            }
        }
        return Bitmap.createBitmap(output, w, h, Bitmap.Config.ARGB_8888)
    }

    fun resizeBitmap(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        return Bitmap.createScaledBitmap(source, targetWidth.coerceAtLeast(16), targetHeight.coerceAtLeast(16), true)
    }

    /**
     * Compresses bitmap to target file, optionally fitting within maxKb via binary-search quality.
     */
    fun exportCompressedImage(
        bitmap: Bitmap,
        outputFile: File,
        format: Bitmap.CompressFormat,
        quality: Int = 92,
        maxKb: Int? = null
    ): Long {
        if (maxKb != null && format != Bitmap.CompressFormat.PNG) {
            val maxBytes = maxKb * 1024
            var low = 10
            var high = quality.coerceIn(20, 100)
            var bestBytes: ByteArray? = null
            while (low <= high) {
                val mid = (low + high) / 2
                val baos = ByteArrayOutputStream()
                bitmap.compress(format, mid, baos)
                val data = baos.toByteArray()
                if (data.size <= maxBytes) {
                    bestBytes = data
                    low = mid + 1
                } else {
                    high = mid - 1
                }
            }
            val finalBytes = bestBytes ?: ByteArrayOutputStream().also {
                bitmap.compress(format, 10, it)
            }.toByteArray()
            FileOutputStream(outputFile).use { it.write(finalBytes) }
            return outputFile.length()
        } else {
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(format, quality.coerceIn(10, 100), out)
            }
            return outputFile.length()
        }
    }

    /**
     * Real edge-seeded BFS flood-fill background removal with tolerance and soft alpha feathering.
     */
    fun removeBackgroundFloodFill(
        source: Bitmap,
        tolerance: Int = 36,
        feather: Int = 2,
        seedX: Int? = null,
        seedY: Int? = null
    ): Bitmap {
        val maxProcessDim = 900
        val scale = if (max(source.width, source.height) > maxProcessDim) {
            maxProcessDim.toFloat() / max(source.width, source.height)
        } else 1f
        val bmp = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                source,
                (source.width * scale).roundToInt(),
                (source.height * scale).roundToInt(),
                true
            )
        } else {
            source.copy(Bitmap.Config.ARGB_8888, true)
        }

        val w = bmp.width
        val h = bmp.height
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)

        val visited = BooleanArray(w * h)
        val queue = IntArray(w * h)
        var head = 0
        var tail = 0

        fun colorDist(p1: Int, r2: Int, g2: Int, b2: Int): Double {
            val r1 = (p1 shr 16) and 0xFF
            val g1 = (p1 shr 8) and 0xFF
            val b1 = p1 and 0xFF
            val dr = r1 - r2
            val dg = g1 - g2
            val db = b1 - b2
            return sqrt(0.3 * dr * dr + 0.59 * dg * dg + 0.11 * db * db)
        }

        if (seedX != null && seedY != null) {
            val sx = seedX.coerceIn(0, w - 1)
            val sy = seedY.coerceIn(0, h - 1)
            val idx = sy * w + sx
            visited[idx] = true
            queue[tail++] = idx
        } else {
            for (x in 0 until w) {
                val top = x
                val bot = (h - 1) * w + x
                if (!visited[top]) { visited[top] = true; queue[tail++] = top }
                if (!visited[bot]) { visited[bot] = true; queue[tail++] = bot }
            }
            for (y in 0 until h) {
                val left = y * w
                val right = y * w + (w - 1)
                if (!visited[left]) { visited[left] = true; queue[tail++] = left }
                if (!visited[right]) { visited[right] = true; queue[tail++] = right }
            }
        }

        // Reference color from seed or average border
        val refPixel = if (seedX != null && seedY != null) {
            pixels[seedY.coerceIn(0, h - 1) * w + seedX.coerceIn(0, w - 1)]
        } else {
            pixels[0]
        }
        val refR = (refPixel shr 16) and 0xFF
        val refG = (refPixel shr 8) and 0xFF
        val refB = refPixel and 0xFF

        val tol = tolerance.toDouble()
        val isBg = BooleanArray(w * h)

        while (head < tail) {
            val curr = queue[head++]
            val px = pixels[curr]
            val distFromRef = colorDist(px, refR, refG, refB)
            if (distFromRef > tol * 1.65) continue

            isBg[curr] = true
            val cx = curr % w
            val cy = curr / w
            val pr = (px shr 16) and 0xFF
            val pg = (px shr 8) and 0xFF
            val pb = px and 0xFF

            val neighbors = intArrayOf(
                if (cx > 0) curr - 1 else -1,
                if (cx < w - 1) curr + 1 else -1,
                if (cy > 0) curr - w else -1,
                if (cy < h - 1) curr + w else -1
            )
            for (n in neighbors) {
                if (n >= 0 && !visited[n]) {
                    visited[n] = true
                    val nDistLocal = colorDist(pixels[n], pr, pg, pb)
                    val nDistRef = colorDist(pixels[n], refR, refG, refB)
                    if (nDistLocal <= tol * 0.75 || nDistRef <= tol) {
                        queue[tail++] = n
                    }
                }
            }
        }

        // Apply transparency and optional edge feathering
        for (i in pixels.indices) {
            if (isBg[i]) {
                pixels[i] = pixels[i] and 0x00FFFFFF
            } else if (feather > 0) {
                val x = i % w
                val y = i / w
                var nearBg = false
                for (dy in -feather..feather) {
                    for (dx in -feather..feather) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx in 0 until w && ny in 0 until h && isBg[ny * w + nx]) {
                            nearBg = true
                            break
                        }
                    }
                    if (nearBg) break
                }
                if (nearBg) {
                    val alpha = 150
                    pixels[i] = (alpha shl 24) or (pixels[i] and 0x00FFFFFF)
                }
            }
        }

        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }

    fun removeChromaColor(source: Bitmap, targetColor: Int, tolerance: Int = 48): Bitmap {
        val w = source.width
        val h = source.height
        val pixels = IntArray(w * h)
        source.getPixels(pixels, 0, w, 0, 0, w, h)
        val tr = Color.red(targetColor)
        val tg = Color.green(targetColor)
        val tb = Color.blue(targetColor)
        val tol = tolerance.toDouble()
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val dist = sqrt(0.3 * (r - tr) * (r - tr) + 0.59 * (g - tg) * (g - tg) + 0.11 * (b - tb) * (b - tb))
            if (dist < tol) {
                pixels[i] = p and 0x00FFFFFF
            } else if (dist < tol * 1.25) {
                val alpha = (((dist - tol) / (tol * 0.25)) * 255).roundToInt().coerceIn(0, 255)
                pixels[i] = (alpha shl 24) or (p and 0x00FFFFFF)
            }
        }
        return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
    }

    fun applyPortraitOvalMask(source: Bitmap): Bitmap {
        val out = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val path = Path().apply {
            addOval(
                RectF(
                    source.width * 0.10f,
                    source.height * 0.05f,
                    source.width * 0.90f,
                    source.height * 0.95f
                ),
                Path.Direction.CW
            )
        }
        canvas.clipPath(path)
        canvas.drawBitmap(source, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
        return out
    }

    fun compositeWithBackdrop(foreground: Bitmap, color1: Int?, color2: Int? = null): Bitmap {
        if (color1 == null) return foreground
        val out = Bitmap.createBitmap(foreground.width, foreground.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        if (color2 != null) {
            val paint = Paint().apply {
                shader = LinearGradient(
                    0f, 0f, 0f, foreground.height.toFloat(),
                    color1, color2, Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, foreground.width.toFloat(), foreground.height.toFloat(), paint)
        } else {
            canvas.drawColor(color1)
        }
        canvas.drawBitmap(foreground, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG))
        return out
    }

    fun eraseBrushAt(current: Bitmap, normX: Float, normY: Float, radiusFraction: Float = 0.045f): Bitmap {
        val copy = current.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(copy)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        val r = min(copy.width, copy.height) * radiusFraction
        canvas.drawCircle(normX * copy.width, normY * copy.height, r, paint)
        return copy
    }
}
