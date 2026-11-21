package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import kotlin.math.abs

enum class QrEccLevel(val formatBits: Int) {
    L(1), M(0), Q(3), H(2)
}

data class QrMatrix(val size: Int, val modules: BooleanArray) {
    fun get(x: Int, y: Int): Boolean = modules[y * size + x]
}

/**
 * Pure Kotlin ISO/IEC 18004 QR Code Encoder (Byte Mode, Versions 1..10, ECC L/M/Q/H).
 * No external API or paid library required.
 */
object QrEngine {

    private data class EccSpec(
        val totalCodewords: Int,
        val ecPerBlock: Int,
        val g1Blocks: Int,
        val g1Data: Int,
        val g2Blocks: Int = 0,
        val g2Data: Int = 0
    ) {
        val totalDataCodewords: Int get() = g1Blocks * g1Data + g2Blocks * g2Data
    }

    // Spec table for Versions 1..10 and ECC L, M, Q, H
    private val ECC_TABLE: Map<Pair<Int, QrEccLevel>, EccSpec> = mapOf(
        // V1
        (1 to QrEccLevel.L) to EccSpec(26, 7, 1, 19),
        (1 to QrEccLevel.M) to EccSpec(26, 10, 1, 16),
        (1 to QrEccLevel.Q) to EccSpec(26, 13, 1, 13),
        (1 to QrEccLevel.H) to EccSpec(26, 17, 1, 9),
        // V2
        (2 to QrEccLevel.L) to EccSpec(44, 10, 1, 34),
        (2 to QrEccLevel.M) to EccSpec(44, 16, 1, 28),
        (2 to QrEccLevel.Q) to EccSpec(44, 22, 1, 22),
        (2 to QrEccLevel.H) to EccSpec(44, 28, 1, 16),
        // V3
        (3 to QrEccLevel.L) to EccSpec(70, 15, 1, 55),
        (3 to QrEccLevel.M) to EccSpec(70, 26, 1, 44),
        (3 to QrEccLevel.Q) to EccSpec(70, 18, 2, 17),
        (3 to QrEccLevel.H) to EccSpec(70, 22, 2, 13),
        // V4
        (4 to QrEccLevel.L) to EccSpec(100, 20, 1, 80),
        (4 to QrEccLevel.M) to EccSpec(100, 18, 2, 32),
        (4 to QrEccLevel.Q) to EccSpec(100, 26, 2, 24),
        (4 to QrEccLevel.H) to EccSpec(100, 16, 4, 9),
        // V5
        (5 to QrEccLevel.L) to EccSpec(134, 26, 1, 108),
        (5 to QrEccLevel.M) to EccSpec(134, 24, 2, 43),
        (5 to QrEccLevel.Q) to EccSpec(134, 18, 2, 15, 2, 16),
        (5 to QrEccLevel.H) to EccSpec(134, 22, 2, 11, 2, 12),
        // V6
        (6 to QrEccLevel.L) to EccSpec(172, 18, 2, 68),
        (6 to QrEccLevel.M) to EccSpec(172, 16, 4, 27),
        (6 to QrEccLevel.Q) to EccSpec(172, 24, 4, 19),
        (6 to QrEccLevel.H) to EccSpec(172, 28, 4, 15),
        // V7
        (7 to QrEccLevel.L) to EccSpec(196, 20, 2, 78),
        (7 to QrEccLevel.M) to EccSpec(196, 18, 4, 31),
        (7 to QrEccLevel.Q) to EccSpec(196, 18, 2, 14, 4, 15),
        (7 to QrEccLevel.H) to EccSpec(196, 26, 4, 13, 1, 14),
        // V8
        (8 to QrEccLevel.L) to EccSpec(242, 24, 2, 97),
        (8 to QrEccLevel.M) to EccSpec(242, 22, 2, 38, 2, 39),
        (8 to QrEccLevel.Q) to EccSpec(242, 22, 4, 18, 2, 19),
        (8 to QrEccLevel.H) to EccSpec(242, 26, 4, 14, 2, 15),
        // V9
        (9 to QrEccLevel.L) to EccSpec(292, 30, 2, 116),
        (9 to QrEccLevel.M) to EccSpec(292, 22, 3, 36, 2, 37),
        (9 to QrEccLevel.Q) to EccSpec(292, 20, 4, 16, 4, 17),
        (9 to QrEccLevel.H) to EccSpec(292, 24, 4, 12, 4, 13),
        // V10
        (10 to QrEccLevel.L) to EccSpec(346, 18, 2, 68, 2, 69),
        (10 to QrEccLevel.M) to EccSpec(346, 26, 4, 43, 1, 44),
        (10 to QrEccLevel.Q) to EccSpec(346, 24, 6, 19, 2, 20),
        (10 to QrEccLevel.H) to EccSpec(346, 28, 6, 15, 2, 16)
    )

    private val ALIGNMENT_POS = mapOf(
        1 to intArrayOf(),
        2 to intArrayOf(6, 18),
        3 to intArrayOf(6, 22),
        4 to intArrayOf(6, 26),
        5 to intArrayOf(6, 30),
        6 to intArrayOf(6, 34),
        7 to intArrayOf(6, 22, 38),
        8 to intArrayOf(6, 24, 42),
        9 to intArrayOf(6, 26, 46),
        10 to intArrayOf(6, 28, 50)
    )

    private val GF_EXP = IntArray(512)
    private val GF_LOG = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            GF_EXP[i] = x
            GF_LOG[x] = i
            x = x shl 1
            if (x and 0x100 != 0) x = x xor 0x11D
        }
        for (i in 255 until 512) {
            GF_EXP[i] = GF_EXP[i - 255]
        }
    }

    private fun gfMul(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return GF_EXP[GF_LOG[a] + GF_LOG[b]]
    }

    private fun rsGenerator(degree: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until degree) {
            val next = IntArray(poly.size + 1)
            val factor = GF_EXP[i]
            for (j in poly.indices) {
                next[j] = next[j] xor poly[j]
                next[j + 1] = next[j + 1] xor gfMul(poly[j], factor)
            }
            poly = next
        }
        return poly
    }

    private fun rsComputeEcc(data: IntArray, ecLen: Int): IntArray {
        val gen = rsGenerator(ecLen)
        val res = IntArray(ecLen)
        for (b in data) {
            val factor = b xor res[0]
            System.arraycopy(res, 1, res, 0, ecLen - 1)
            res[ecLen - 1] = 0
            for (j in 0 until ecLen) {
                res[j] = res[j] xor gfMul(gen[j + 1], factor)
            }
        }
        return res
    }

    fun encode(text: String, ecc: QrEccLevel = QrEccLevel.M): QrMatrix {
        val bytes = text.toByteArray(Charsets.UTF_8)
        var selectedVer = -1
        var selectedSpec: EccSpec? = null
        for (ver in 1..10) {
            val spec = ECC_TABLE[ver to ecc] ?: continue
            val headerBits = 4 + if (ver <= 9) 8 else 16
            val capacityBits = spec.totalDataCodewords * 8
            if (headerBits + bytes.size * 8 <= capacityBits) {
                selectedVer = ver
                selectedSpec = spec
                break
            }
        }
        if (selectedVer == -1) {
            // Fall back to L if content is long, or truncate safely
            selectedVer = 10
            selectedSpec = ECC_TABLE[10 to QrEccLevel.L]!!
        }
        val spec = selectedSpec!!
        val ver = selectedVer
        val maxBytes = (spec.totalDataCodewords * 8 - (4 + if (ver <= 9) 8 else 16)) / 8
        val payload = if (bytes.size > maxBytes) bytes.copyOf(maxBytes) else bytes

        // Build bitstream
        val bits = ArrayList<Int>(spec.totalDataCodewords * 8)
        fun appendBits(value: Int, count: Int) {
            for (i in count - 1 downTo 0) bits.add((value ushr i) and 1)
        }
        appendBits(0b0100, 4) // Byte mode
        appendBits(payload.size, if (ver <= 9) 8 else 16)
        for (b in payload) appendBits(b.toInt() and 0xFF, 8)

        val totalDataBits = spec.totalDataCodewords * 8
        val terminator = minOf(4, totalDataBits - bits.size)
        repeat(terminator) { bits.add(0) }
        while (bits.size % 8 != 0) bits.add(0)

        var padToggle = true
        while (bits.size < totalDataBits) {
            appendBits(if (padToggle) 0xEC else 0x11, 8)
            padToggle = !padToggle
        }

        val dataBytes = IntArray(spec.totalDataCodewords) { idx ->
            var v = 0
            for (b in 0 until 8) v = (v shl 1) or bits[idx * 8 + b]
            v
        }

        // Split into blocks and compute ECC
        val dataBlocks = ArrayList<IntArray>()
        val ecBlocks = ArrayList<IntArray>()
        var offset = 0
        repeat(spec.g1Blocks) {
            val block = dataBytes.copyOfRange(offset, offset + spec.g1Data)
            offset += spec.g1Data
            dataBlocks.add(block)
            ecBlocks.add(rsComputeEcc(block, spec.ecPerBlock))
        }
        repeat(spec.g2Blocks) {
            val block = dataBytes.copyOfRange(offset, offset + spec.g2Data)
            offset += spec.g2Data
            dataBlocks.add(block)
            ecBlocks.add(rsComputeEcc(block, spec.ecPerBlock))
        }

        val interleaved = ArrayList<Int>(spec.totalCodewords)
        val maxDataLen = maxOf(spec.g1Data, spec.g2Data)
        for (i in 0 until maxDataLen) {
            for (blk in dataBlocks) if (i < blk.size) interleaved.add(blk[i])
        }
        for (i in 0 until spec.ecPerBlock) {
            for (blk in ecBlocks) interleaved.add(blk[i])
        }

        val size = 17 + 4 * ver
        val modules = BooleanArray(size * size)
        val isFunction = BooleanArray(size * size)

        fun setFn(x: Int, y: Int, dark: Boolean) {
            if (x in 0 until size && y in 0 until size) {
                modules[y * size + x] = dark
                isFunction[y * size + x] = true
            }
        }

        // Finder patterns + separators
        fun drawFinder(cx: Int, cy: Int) {
            for (dy in -4..4) {
                for (dx in -4..4) {
                    val xx = cx + dx
                    val yy = cy + dy
                    if (xx !in 0 until size || yy !in 0 until size) continue
                    val dist = maxOf(abs(dx), abs(dy))
                    val dark = dist != 2 && dist != 4
                    setFn(xx, yy, dark)
                }
            }
        }
        drawFinder(3, 3)
        drawFinder(size - 4, 3)
        drawFinder(3, size - 4)

        // Timing patterns
        for (i in 8 until size - 8) {
            val dark = i % 2 == 0
            setFn(i, 6, dark)
            setFn(6, i, dark)
        }

        // Alignment patterns
        val align = ALIGNMENT_POS[ver] ?: intArrayOf()
        for (ay in align) {
            for (ax in align) {
                if (isFunction[ay * size + ax]) continue
                for (dy in -2..2) {
                    for (dx in -2..2) {
                        val dist = maxOf(abs(dx), abs(dy))
                        setFn(ax + dx, ay + dy, dist != 1)
                    }
                }
            }
        }

        // Dark module
        setFn(8, 4 * ver + 9, true)

        // Reserve format areas
        for (i in 0..8) {
            if (i != 6) {
                isFunction[8 * size + i] = true
                isFunction[i * size + 8] = true
            }
        }
        for (i in size - 8 until size) {
            isFunction[8 * size + i] = true
            isFunction[i * size + 8] = true
        }

        // Reserve & write version info if ver >= 7
        if (ver >= 7) {
            var rem = ver
            repeat(12) { rem = (rem shl 1) xor ((rem ushr 11) * 0x1F25) }
            val vBits = (ver shl 12) or rem
            for (i in 0 until 18) {
                val dark = ((vBits ushr i) and 1) != 0
                val a = size - 11 + (i % 3)
                val b = i / 3
                setFn(a, b, dark)
                setFn(b, a, dark)
            }
        }

        // Place data bits in zig-zag
        val codeBits = ArrayList<Boolean>(interleaved.size * 8 + 7)
        for (cw in interleaved) {
            for (b in 7 downTo 0) codeBits.add(((cw ushr b) and 1) != 0)
        }
        var bitIdx = 0
        var right = size - 1
        while (right >= 1) {
            if (right == 6) right = 5
            for (vert in 0 until size) {
                for (j in 0 until 2) {
                    val x = right - j
                    val upward = ((right + 1) and 2) == 0
                    val y = if (upward) size - 1 - vert else vert
                    if (!isFunction[y * size + x] && bitIdx < codeBits.size) {
                        modules[y * size + x] = codeBits[bitIdx++]
                    }
                }
            }
            right -= 2
        }

        // Evaluate 8 mask patterns and choose lowest penalty
        var bestMask = 0
        var bestPenalty = Int.MAX_VALUE
        val baseModules = modules.copyOf()

        for (mask in 0 until 8) {
            val candidate = baseModules.copyOf()
            for (y in 0 until size) {
                for (x in 0 until size) {
                    if (isFunction[y * size + x]) continue
                    val invert = when (mask) {
                        0 -> (x + y) % 2 == 0
                        1 -> y % 2 == 0
                        2 -> x % 3 == 0
                        3 -> (x + y) % 3 == 0
                        4 -> (y / 2 + x / 3) % 2 == 0
                        5 -> (x * y) % 2 + (x * y) % 3 == 0
                        6 -> ((x * y) % 2 + (x * y) % 3) % 2 == 0
                        else -> ((x + y) % 2 + (x * y) % 3) % 2 == 0
                    }
                    if (invert) candidate[y * size + x] = !candidate[y * size + x]
                }
            }
            writeFormatBits(candidate, size, ecc, mask)
            val penalty = computePenalty(candidate, size)
            if (penalty < bestPenalty) {
                bestPenalty = penalty
                bestMask = mask
            }
        }

        // Apply best mask
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (isFunction[y * size + x]) continue
                val invert = when (bestMask) {
                    0 -> (x + y) % 2 == 0
                    1 -> y % 2 == 0
                    2 -> x % 3 == 0
                    3 -> (x + y) % 3 == 0
                    4 -> (y / 2 + x / 3) % 2 == 0
                    5 -> (x * y) % 2 + (x * y) % 3 == 0
                    6 -> ((x * y) % 2 + (x * y) % 3) % 2 == 0
                    else -> ((x + y) % 2 + (x * y) % 3) % 2 == 0
                }
                if (invert) modules[y * size + x] = !modules[y * size + x]
            }
        }
        writeFormatBits(modules, size, ecc, bestMask)
        return QrMatrix(size, modules)
    }

    private fun writeFormatBits(modules: BooleanArray, size: Int, ecc: QrEccLevel, mask: Int) {
        val data = (ecc.formatBits shl 3) or mask
        var rem = data
        repeat(10) { rem = (rem shl 1) xor ((rem ushr 9) * 0x537) }
        val bits = ((data shl 10) or rem) xor 0x5412

        for (i in 0..5) modules[i * size + 8] = ((bits ushr i) and 1) != 0
        modules[7 * size + 8] = ((bits ushr 6) and 1) != 0
        modules[8 * size + 8] = ((bits ushr 7) and 1) != 0
        modules[8 * size + 7] = ((bits ushr 8) and 1) != 0
        for (i in 9 until 15) modules[8 * size + (14 - i)] = ((bits ushr i) and 1) != 0

        for (i in 0 until 8) modules[8 * size + (size - 1 - i)] = ((bits ushr i) and 1) != 0
        for (i in 8 until 15) modules[(size - 15 + i) * size + 8] = ((bits ushr i) and 1) != 0
    }

    private fun computePenalty(m: BooleanArray, s: Int): Int {
        var penalty = 0
        for (y in 0 until s) {
            var run = 1
            for (x in 1 until s) {
                if (m[y * s + x] == m[y * s + x - 1]) run++ else {
                    if (run >= 5) penalty += 3 + (run - 5)
                    run = 1
                }
            }
            if (run >= 5) penalty += 3 + (run - 5)
        }
        for (x in 0 until s) {
            var run = 1
            for (y in 1 until s) {
                if (m[y * s + x] == m[(y - 1) * s + x]) run++ else {
                    if (run >= 5) penalty += 3 + (run - 5)
                    run = 1
                }
            }
            if (run >= 5) penalty += 3 + (run - 5)
        }
        for (y in 0 until s - 1) {
            for (x in 0 until s - 1) {
                val c = m[y * s + x]
                if (c == m[y * s + x + 1] && c == m[(y + 1) * s + x] && c == m[(y + 1) * s + x + 1]) {
                    penalty += 3
                }
            }
        }
        return penalty
    }

    fun renderBitmap(
        content: String,
        pixelSize: Int = 900,
        fgColor: Int = Color.BLACK,
        bgColor: Int = Color.WHITE,
        rounded: Boolean = false,
        ecc: QrEccLevel = QrEccLevel.M,
        centerLabel: String = ""
    ): Bitmap {
        val matrix = encode(content, if (centerLabel.isNotBlank()) QrEccLevel.H else ecc)
        val bmp = Bitmap.createBitmap(pixelSize, pixelSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(bgColor)

        val quiet = 2
        val totalModules = matrix.size + quiet * 2
        val cell = pixelSize.toFloat() / totalModules
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fgColor }

        for (y in 0 until matrix.size) {
            for (x in 0 until matrix.size) {
                if (matrix.get(x, y)) {
                    val left = (x + quiet) * cell
                    val top = (y + quiet) * cell
                    if (rounded) {
                        canvas.drawRoundRect(
                            RectF(left + cell * 0.06f, top + cell * 0.06f, left + cell * 0.94f, top + cell * 0.94f),
                            cell * 0.35f,
                            cell * 0.35f,
                            paint
                        )
                    } else {
                        canvas.drawRect(left, top, left + cell + 0.4f, top + cell + 0.4f, paint)
                    }
                }
            }
        }

        if (centerLabel.isNotBlank()) {
            val badgeW = pixelSize * 0.28f
            val badgeH = pixelSize * 0.11f
            val cx = pixelSize / 2f
            val cy = pixelSize / 2f
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = fgColor
                style = Paint.Style.STROKE
                strokeWidth = pixelSize * 0.008f
            }
            val rect = RectF(cx - badgeW / 2, cy - badgeH / 2, cx + badgeW / 2, cy + badgeH / 2)
            canvas.drawRoundRect(rect, badgeH * 0.3f, badgeH * 0.3f, bgPaint)
            canvas.drawRoundRect(rect, badgeH * 0.3f, badgeH * 0.3f, borderPaint)

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = fgColor
                textSize = badgeH * 0.52f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val fm = textPaint.fontMetrics
            val textY = cy - (fm.ascent + fm.descent) / 2f
            canvas.drawText(centerLabel.take(10), cx, textY, textPaint)
        }

        return bmp
    }
}
