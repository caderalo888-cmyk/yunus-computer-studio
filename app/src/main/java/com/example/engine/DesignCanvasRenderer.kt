package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.local.InvoiceEntity
import com.example.data.local.InvoiceItemCodec
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---- Graphic Design Studio Models ----

enum class DesignLayerKind { TEXT, RECT, CIRCLE, BADGE, LINE }

data class DesignLayer(
    val id: Long,
    val kind: DesignLayerKind,
    val text: String = "Studio Text",
    val xPercent: Float = 0.5f,
    val yPercent: Float = 0.5f,
    val widthPercent: Float = 0.38f,
    val heightPercent: Float = 0.14f,
    val fontSizeSp: Float = 44f,
    val colorHex: Int = Color.WHITE,
    val bold: Boolean = true,
    val rotationDeg: Float = 0f
)

enum class CanvasPreset(val label: String, val w: Int, val h: Int) {
    SQUARE_POST("Square 1:1", 1080, 1080),
    FLYER_A4("A4 Flyer", 1080, 1528),
    YOUTUBE_BANNER("Banner 16:9", 1600, 900),
    STORY_VERTICAL("Story 9:16", 900, 1600)
}

object GraphicProjectCodec {
    fun encode(preset: CanvasPreset, bg1: Int, bg2: Int?, layers: List<DesignLayer>): String {
        val root = JSONObject()
        root.put("preset", preset.name)
        root.put("bg1", bg1)
        if (bg2 != null) root.put("bg2", bg2)
        val arr = JSONArray()
        layers.forEach { l ->
            arr.put(
                JSONObject().apply {
                    put("id", l.id)
                    put("kind", l.kind.name)
                    put("text", l.text)
                    put("x", l.xPercent.toDouble())
                    put("y", l.yPercent.toDouble())
                    put("w", l.widthPercent.toDouble())
                    put("h", l.heightPercent.toDouble())
                    put("fs", l.fontSizeSp.toDouble())
                    put("col", l.colorHex)
                    put("bold", l.bold)
                    put("rot", l.rotationDeg.toDouble())
                }
            )
        }
        root.put("layers", arr)
        return root.toString()
    }
}

// ---- Wedding Card Models ----

enum class WeddingTheme(
    val label: String,
    val bgTop: Int,
    val bgBottom: Int,
    val accent: Int,
    val textColor: Int
) {
    ROYAL_MAROON("Royal Maroon & Gold", Color.rgb(69, 10, 26), Color.rgb(42, 4, 15), Color.rgb(245, 196, 81), Color.rgb(255, 248, 231)),
    EMERALD_FLORAL("Emerald & Pearl", Color.rgb(6, 62, 48), Color.rgb(4, 38, 30), Color.rgb(234, 179, 8), Color.rgb(240, 253, 244)),
    IVORY_CLASSIC("Ivory & Navy", Color.rgb(253, 248, 240), Color.rgb(245, 236, 220), Color.rgb(180, 125, 30), Color.rgb(20, 32, 62))
}

data class WeddingCardData(
    val theme: WeddingTheme = WeddingTheme.ROYAL_MAROON,
    val headerTitle: String = "Bismillahir Rahmanir Rahim",
    val occasionTitle: String = "Wedding Invitation",
    val groomName: String = "Tariqul Islam",
    val groomParents: String = "S/O Md. Yunus Ali & Fatema Begum",
    val brideName: String = "Nusrat Jahan",
    val brideParents: String = "D/O Abdul Karim & Amena Khatun",
    val dateLine: String = "Friday, 25 December 2026 • 7:30 PM",
    val venueLine: String = "Grand Celebration Hall, Dhaka",
    val rsvpLine: String = "With Warmest Regards • RSVP: 01700-000000"
)

// ---- Election Poster Models ----

enum class ElectionSymbol(val label: String) {
    BOAT("Boat"), SCALE("Scale"), BOOK("Book"), PEN("Pen"), SUN("Sun"), STAR("Star"), SHIELD("Shield")
}

enum class ElectionTheme(val label: String, val primary: Int, val secondary: Int, val accent: Int) {
    VICTORY_GREEN("Emerald & Crimson", Color.rgb(4, 96, 60), Color.rgb(185, 28, 28), Color.rgb(250, 204, 21)),
    NAVY_GOLD("Royal Navy & Gold", Color.rgb(15, 23, 42), Color.rgb(30, 64, 175), Color.rgb(245, 158, 11)),
    TEAL_BOLD("Civic Teal & Slate", Color.rgb(13, 148, 136), Color.rgb(15, 23, 42), Color.rgb(253, 224, 71))
}

data class ElectionPosterData(
    val theme: ElectionTheme = ElectionTheme.VICTORY_GREEN,
    val symbol: ElectionSymbol = ElectionSymbol.BOOK,
    val topBanner: String = "VOTE FOR DEVELOPMENT & INTEGRITY",
    val candidateName: String = "MD. YUNUS MIA",
    val designation: String = "Candidate for Ward Councillor",
    val constituency: String = "Ward No. 07 • Municipal Election 2026",
    val slogan: String = "Education • Clean Roads • Youth Employment",
    val manifesto1: String = "1. Digital Citizen Services & Free Wi-Fi Zones",
    val manifesto2: String = "2. Safe Roads, Street Lights & Clean Drainage",
    val manifesto3: String = "3. Youth Computer Training & Job Support",
    val footerCredit: String = "Campaign Committee • Ward 07"
)

// ---- Visiting Card Models ----

enum class VisitingCardStyle(val label: String, val bg: Int, val accent: Int, val text: Int) {
    EXECUTIVE_DARK("Executive Slate", Color.rgb(15, 23, 42), Color.rgb(20, 184, 166), Color.WHITE),
    AMBER_LUXE("Obsidian & Gold", Color.rgb(24, 24, 27), Color.rgb(245, 158, 11), Color.rgb(250, 250, 249)),
    MINIMAL_PAPER("Classic White", Color.WHITE, Color.rgb(13, 148, 136), Color.rgb(15, 23, 42))
}

data class VisitingCardData(
    val style: VisitingCardStyle = VisitingCardStyle.EXECUTIVE_DARK,
    val companyName: String = "YUNUS COMPUTER STUDIO",
    val tagline: String = "Photo • Print • Design • Digital Service",
    val personName: String = "Md. Yunus Ali",
    val jobTitle: String = "Founder & Creative Director",
    val phone: String = "+880 1711-000000",
    val email: String = "yunusstudio@example.com",
    val website: String = "www.yunusstudio.pro",
    val address: String = "Station Road, Sadar Market",
    val includeQr: Boolean = true
)

// ---- CV Maker Models ----

enum class CvTemplate(val label: String) {
    MODERN_TEAL("Modern Teal Sidebar"),
    CLASSIC_EXEC("Classic Executive"),
    MINIMAL_ACADEMIC("Minimal Clean")
}

data class CvData(
    val template: CvTemplate = CvTemplate.MODERN_TEAL,
    val fullName: String = "MD. YUNUS ALI",
    val roleTitle: String = "Graphic Designer & Computer Operator",
    val phone: String = "+880 1711-000000",
    val email: String = "yunus.pro@example.com",
    val address: String = "Dhaka, Bangladesh",
    val summary: String = "Detail-oriented creative studio specialist with 5+ years of experience in photo editing, document layout, DTP printing, Bangla typing, and digital design.",
    val experience: String = "Senior Studio Operator — Yunus Computer Studio (2022–Present)\n• Managed passport/visa photo printing, NID services, and commercial card layouts.\n\nDTP & Graphics Assistant — City Press (2020–2022)\n• Designed election posters, wedding cards, and formatted corporate PDFs.",
    val education: String = "B.Sc. in Computer Science — National University (2020)\nHSC (Science) — Govt. Science College (2016)",
    val skills: String = "Photo Retouching, Passport/ID Print, Bangla Typing, MS Word & Excel, PDF Publishing, Graphic Design, Audio Editing",
    val languages: String = "Bangla (Native), English (Professional)",
    val reference: String = "Available upon request."
)

object DesignCanvasRenderer {

    fun renderGraphicStudio(
        preset: CanvasPreset,
        bg1: Int,
        bg2: Int?,
        layers: List<DesignLayer>
    ): Bitmap {
        val bmp = Bitmap.createBitmap(preset.w, preset.h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        if (bg2 != null) {
            val bgPaint = Paint().apply {
                shader = LinearGradient(0f, 0f, preset.w.toFloat(), preset.h.toFloat(), bg1, bg2, Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, preset.w.toFloat(), preset.h.toFloat(), bgPaint)
        } else {
            canvas.drawColor(bg1)
        }

        for (layer in layers) {
            val cx = layer.xPercent * preset.w
            val cy = layer.yPercent * preset.h
            val lw = layer.widthPercent * preset.w
            val lh = layer.heightPercent * preset.h

            canvas.save()
            canvas.rotate(layer.rotationDeg, cx, cy)
            when (layer.kind) {
                DesignLayerKind.RECT -> {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = layer.colorHex }
                    canvas.drawRoundRect(
                        RectF(cx - lw / 2, cy - lh / 2, cx + lw / 2, cy + lh / 2),
                        24f, 24f, p
                    )
                }
                DesignLayerKind.CIRCLE -> {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = layer.colorHex }
                    canvas.drawCircle(cx, cy, minOf(lw, lh) / 2f, p)
                }
                DesignLayerKind.LINE -> {
                    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = layer.colorHex
                        strokeWidth = 10f
                        strokeCap = Paint.Cap.ROUND
                    }
                    canvas.drawLine(cx - lw / 2, cy, cx + lw / 2, cy, p)
                }
                DesignLayerKind.BADGE -> {
                    val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = layer.colorHex }
                    val rect = RectF(cx - lw / 2, cy - lh / 2, cx + lw / 2, cy + lh / 2)
                    canvas.drawRoundRect(rect, lh / 2f, lh / 2f, bg)
                    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = if (isColorDark(layer.colorHex)) Color.WHITE else Color.rgb(15, 23, 42)
                        textSize = layer.fontSizeSp * 1.4f
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                    val fm = tp.fontMetrics
                    canvas.drawText(layer.text, cx, cy - (fm.ascent + fm.descent) / 2f, tp)
                }
                DesignLayerKind.TEXT -> {
                    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = layer.colorHex
                        textSize = layer.fontSizeSp * 1.6f
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.create(
                            Typeface.DEFAULT,
                            if (layer.bold) Typeface.BOLD else Typeface.NORMAL
                        )
                    }
                    drawMultilineCentered(canvas, layer.text, cx, cy, tp, preset.w * 0.85f)
                }
            }
            canvas.restore()
        }
        return bmp
    }

    fun renderWeddingCard(data: WeddingCardData): Bitmap {
        val w = 1080
        val h = 1520
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val t = data.theme

        val bgPaint = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, h.toFloat(), t.bgTop, t.bgBottom, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Outer & inner ornamental borders
        val outerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = t.accent
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }
        val innerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = t.accent
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            pathEffect = DashPathEffect(floatArrayOf(16f, 8f), 0f)
        }
        canvas.drawRoundRect(RectF(44f, 44f, w - 44f, h - 44f), 28f, 28f, outerBorder)
        canvas.drawRoundRect(RectF(66f, 66f, w - 66f, h - 66f), 20f, 20f, innerBorder)

        // Corner diamond ornaments
        val fillAccent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = t.accent }
        listOf(84f to 84f, w - 84f to 84f, 84f to h - 84f, w - 84f to h - 84f).forEach { (cx, cy) ->
            val path = Path().apply {
                moveTo(cx, cy - 18f)
                lineTo(cx + 18f, cy)
                lineTo(cx, cy + 18f)
                lineTo(cx - 18f, cy)
                close()
            }
            canvas.drawPath(path, fillAccent)
        }

        // Top Arch / Emblem
        val archPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = t.accent
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawArc(RectF(w / 2f - 130f, 110f, w / 2f + 130f, 250f), 180f, 180f, false, archPaint)

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = t.accent
            textSize = 34f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }
        canvas.drawText(data.headerTitle, w / 2f, 215f, headerPaint)

        val occasionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = t.textColor
            textSize = 48f
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.08f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText(data.occasionTitle.uppercase(), w / 2f, 320f, occasionPaint)

        // Divider line
        canvas.drawLine(w * 0.25f, 360f, w * 0.75f, 360f, outerBorder)

        val subText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = t.textColor
            alpha = 210
            textSize = 28f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Together with their families", w / 2f, 435f, subText)

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = t.accent
            textSize = 72f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        canvas.drawText(data.groomName, w / 2f, 555f, namePaint)
        canvas.drawText(data.groomParents, w / 2f, 610f, subText)

        val ampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = t.textColor
            textSize = 54f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }
        canvas.drawText("&", w / 2f, 705f, ampPaint)

        canvas.drawText(data.brideName, w / 2f, 815f, namePaint)
        canvas.drawText(data.brideParents, w / 2f, 870f, subText)

        // Detail Box
        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(40, 255, 255, 255)
        }
        canvas.drawRoundRect(RectF(130f, 960f, w - 130f, 1270f), 24f, 24f, boxPaint)
        canvas.drawRoundRect(RectF(130f, 960f, w - 130f, 1270f), 24f, 24f, innerBorder)

        val detailTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = t.accent
            textSize = 36f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(data.dateLine, w / 2f, 1060f, detailTitle)

        val venuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = t.textColor
            textSize = 34f
            textAlign = Paint.Align.CENTER
        }
        drawMultilineCentered(canvas, data.venueLine, w / 2f, 1155f, venuePaint, w - 300f)

        canvas.drawText(data.rsvpLine, w / 2f, 1385f, subText)
        return bmp
    }

    fun renderElectionPoster(data: ElectionPosterData, candidatePhoto: Bitmap?): Bitmap {
        val w = 1080
        val h = 1520
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val t = data.theme

        // Background
        val bg = Paint().apply {
            shader = LinearGradient(0f, 0f, 0f, h.toFloat(), t.primary, Color.rgb(10, 15, 28), Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bg)

        // Top ribbon banner
        val ribbonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = t.secondary }
        canvas.drawRect(0f, 0f, w.toFloat(), 150f, ribbonPaint)
        val bannerText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 38f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(data.topBanner.uppercase(), w / 2f, 92f, bannerText)

        // Candidate photo frame (left-center) & Election Symbol Badge (right)
        val photoRect = RectF(90f, 210f, 590f, 770f)
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRoundRect(photoRect, 28f, 28f, framePaint)
        if (candidatePhoto != null) {
            canvas.save()
            val clipPath = Path().apply { addRoundRect(RectF(102f, 222f, 578f, 758f), 20f, 20f, Path.Direction.CW) }
            canvas.clipPath(clipPath)
            val scale = maxOf(476f / candidatePhoto.width, 536f / candidatePhoto.height)
            val dw = candidatePhoto.width * scale
            val dh = candidatePhoto.height * scale
            canvas.drawBitmap(
                candidatePhoto,
                null,
                RectF(340f - dw / 2f, 490f - dh / 2f, 340f + dw / 2f, 490f + dh / 2f),
                Paint(Paint.FILTER_BITMAP_FLAG)
            )
            canvas.restore()
        } else {
            val phPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(203, 213, 225)
                textSize = 30f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("CANDIDATE PHOTO", 340f, 495f, phPaint)
        }

        // Symbol badge
        val symCx = 820f
        val symCy = 460f
        val symBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val symBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = t.accent
            style = Paint.Style.STROKE
            strokeWidth = 12f
        }
        canvas.drawCircle(symCx, symCy, 165f, symBg)
        canvas.drawCircle(symCx, symCy, 165f, symBorder)
        drawElectionSymbol(canvas, data.symbol, symCx, symCy, 105f, t.primary)

        val symCaption = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = t.accent
            textSize = 36f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("SYMBOL: ${data.symbol.label.uppercase()}", symCx, 685f, symCaption)

        // Candidate Name Block
        val nameBox = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = t.accent }
        canvas.drawRoundRect(RectF(70f, 820f, w - 70f, 965f), 20f, 20f, nameBox)
        val candPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 68f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(data.candidateName.uppercase(), w / 2f, 916f, candPaint)

        val rolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 40f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(data.designation, w / 2f, 1035f, rolePaint)

        val constPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 232, 240)
            textSize = 32f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(data.constituency, w / 2f, 1090f, constPaint)

        // Manifesto card
        val manBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(60, 255, 255, 255) }
        canvas.drawRoundRect(RectF(70f, 1140f, w - 70f, 1390f), 22f, 22f, manBg)
        val sloganPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = t.accent
            textSize = 33f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(data.slogan, w / 2f, 1200f, sloganPaint)

        val itemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 28f
            textAlign = Paint.Align.LEFT
        }
        canvas.drawText(data.manifesto1, 110f, 1258f, itemPaint)
        canvas.drawText(data.manifesto2, 110f, 1304f, itemPaint)
        canvas.drawText(data.manifesto3, 110f, 1350f, itemPaint)

        // Footer
        canvas.drawRect(0f, h - 90f, w.toFloat(), h.toFloat(), ribbonPaint)
        val footPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 30f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(data.footerCredit, w / 2f, h - 34f, footPaint)
        return bmp
    }

    private fun drawElectionSymbol(canvas: Canvas, symbol: ElectionSymbol, cx: Float, cy: Float, r: Float, color: Int) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 10f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        when (symbol) {
            ElectionSymbol.BOAT -> {
                val hull = Path().apply {
                    moveTo(cx - r * 0.9f, cy + r * 0.1f)
                    lineTo(cx + r * 0.9f, cy + r * 0.1f)
                    lineTo(cx + r * 0.55f, cy + r * 0.55f)
                    lineTo(cx - r * 0.55f, cy + r * 0.55f)
                    close()
                }
                canvas.drawPath(hull, p)
                val sail = Path().apply {
                    moveTo(cx - r * 0.05f, cy + r * 0.05f)
                    lineTo(cx - r * 0.05f, cy - r * 0.8f)
                    lineTo(cx + r * 0.65f, cy + r * 0.02f)
                    close()
                }
                canvas.drawPath(sail, p)
            }
            ElectionSymbol.BOOK -> {
                canvas.drawRoundRect(RectF(cx - r * 0.7f, cy - r * 0.55f, cx - r * 0.04f, cy + r * 0.55f), 10f, 10f, stroke)
                canvas.drawRoundRect(RectF(cx + r * 0.04f, cy - r * 0.55f, cx + r * 0.7f, cy + r * 0.55f), 10f, 10f, stroke)
                canvas.drawLine(cx, cy - r * 0.5f, cx, cy + r * 0.55f, stroke)
            }
            ElectionSymbol.SCALE -> {
                canvas.drawLine(cx, cy - r * 0.7f, cx, cy + r * 0.65f, stroke)
                canvas.drawLine(cx - r * 0.7f, cy - r * 0.45f, cx + r * 0.7f, cy - r * 0.45f, stroke)
                canvas.drawLine(cx - r * 0.4f, cy + r * 0.65f, cx + r * 0.4f, cy + r * 0.65f, stroke)
                canvas.drawArc(RectF(cx - r * 0.9f, cy - r * 0.05f, cx - r * 0.3f, cy + r * 0.35f), 0f, 180f, true, p)
                canvas.drawArc(RectF(cx + r * 0.3f, cy - r * 0.05f, cx + r * 0.9f, cy + r * 0.35f), 0f, 180f, true, p)
            }
            ElectionSymbol.PEN -> {
                val nib = Path().apply {
                    moveTo(cx, cy - r * 0.85f)
                    lineTo(cx + r * 0.42f, cy - r * 0.05f)
                    lineTo(cx + r * 0.28f, cy + r * 0.7f)
                    lineTo(cx - r * 0.28f, cy + r * 0.7f)
                    lineTo(cx - r * 0.42f, cy - r * 0.05f)
                    close()
                }
                canvas.drawPath(nib, p)
            }
            ElectionSymbol.SUN -> {
                canvas.drawCircle(cx, cy, r * 0.42f, p)
                for (a in 0 until 360 step 30) {
                    val rad = Math.toRadians(a.toDouble())
                    canvas.drawLine(
                        cx + (r * 0.56f * kotlin.math.cos(rad)).toFloat(),
                        cy + (r * 0.56f * kotlin.math.sin(rad)).toFloat(),
                        cx + (r * 0.85f * kotlin.math.cos(rad)).toFloat(),
                        cy + (r * 0.85f * kotlin.math.sin(rad)).toFloat(),
                        stroke
                    )
                }
            }
            ElectionSymbol.STAR -> {
                val path = Path()
                for (i in 0 until 10) {
                    val rad = Math.toRadians((i * 36 - 90).toDouble())
                    val dist = if (i % 2 == 0) r * 0.85f else r * 0.38f
                    val px = cx + (dist * kotlin.math.cos(rad)).toFloat()
                    val py = cy + (dist * kotlin.math.sin(rad)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                canvas.drawPath(path, p)
            }
            ElectionSymbol.SHIELD -> {
                val shield = Path().apply {
                    moveTo(cx, cy - r * 0.8f)
                    lineTo(cx + r * 0.7f, cy - r * 0.45f)
                    lineTo(cx + r * 0.55f, cy + r * 0.35f)
                    lineTo(cx, cy + r * 0.85f)
                    lineTo(cx - r * 0.55f, cy + r * 0.35f)
                    lineTo(cx - r * 0.7f, cy - r * 0.45f)
                    close()
                }
                canvas.drawPath(shield, p)
            }
        }
    }

    fun renderVisitingCard(data: VisitingCardData): Bitmap {
        val w = 1050
        val h = 600
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val s = data.style
        canvas.drawColor(s.bg)

        // Left accent stripe
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = s.accent }
        canvas.drawRect(0f, 0f, 26f, h.toFloat(), accentPaint)
        canvas.drawRect(26f, h - 14f, w.toFloat(), h.toFloat(), accentPaint)

        // Company & Tagline
        val compPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = s.accent
            textSize = 36f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.06f
        }
        canvas.drawText(data.companyName, 74f, 92f, compPaint)

        val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = s.text
            alpha = 190
            textSize = 22f
        }
        canvas.drawText(data.tagline, 74f, 128f, tagPaint)

        // Person Name & Designation
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = s.text
            textSize = 48f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(data.personName, 74f, 245f, namePaint)

        val rolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = s.accent
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(data.jobTitle, 74f, 288f, rolePaint)

        // Contact lines
        val infoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = s.text
            textSize = 24f
        }
        canvas.drawText("Tel: ${data.phone}", 74f, 380f, infoPaint)
        canvas.drawText("Email: ${data.email}", 74f, 422f, infoPaint)
        canvas.drawText("Web: ${data.website}", 74f, 464f, infoPaint)
        canvas.drawText("Addr: ${data.address}", 74f, 506f, infoPaint)

        if (data.includeQr) {
            val vcard = "BEGIN:VCARD\nVERSION:3.0\nFN:${data.personName}\nORG:${data.companyName}\nTEL:${data.phone}\nEMAIL:${data.email}\nURL:${data.website}\nEND:VCARD"
            val qrBmp = QrEngine.renderBitmap(
                content = vcard,
                pixelSize = 230,
                fgColor = Color.rgb(15, 23, 42),
                bgColor = Color.WHITE
            )
            val qrFrame = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            canvas.drawRoundRect(RectF(756f, 185f, 998f, 427f), 16f, 16f, qrFrame)
            canvas.drawBitmap(qrBmp, 762f, 191f, null)
        }
        return bmp
    }

    fun renderCvToBitmapAndPdf(
        data: CvData,
        photo: Bitmap?,
        pdfOutFile: File? = null
    ): Bitmap {
        val w = 1190
        val h = 1684 // A4 @ ~144 DPI
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val isSidebar = data.template == CvTemplate.MODERN_TEAL
        val accent = when (data.template) {
            CvTemplate.MODERN_TEAL -> Color.rgb(13, 148, 136)
            CvTemplate.CLASSIC_EXEC -> Color.rgb(30, 41, 59)
            CvTemplate.MINIMAL_ACADEMIC -> Color.rgb(79, 70, 229)
        }

        if (isSidebar) {
            val sidePaint = Paint().apply { color = Color.rgb(15, 23, 42) }
            canvas.drawRect(0f, 0f, 380f, h.toFloat(), sidePaint)

            if (photo != null) {
                canvas.save()
                val path = Path().apply { addCircle(190f, 170f, 110f, Path.Direction.CW) }
                canvas.clipPath(path)
                val sc = maxOf(220f / photo.width, 220f / photo.height)
                val dw = photo.width * sc
                val dh = photo.height * sc
                canvas.drawBitmap(photo, null, RectF(190f - dw / 2, 170f - dh / 2, 190f + dw / 2, 170f + dh / 2), Paint(Paint.FILTER_BITMAP_FLAG))
                canvas.restore()
            }

            val sideHeader = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(45, 212, 191)
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val sideBody = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(226, 232, 240)
                textSize = 22f
            }
            var sy = if (photo != null) 340f else 110f
            canvas.drawText("CONTACT", 44f, sy, sideHeader); sy += 38f
            sy = drawWrappedLeft(canvas, "${data.phone}\n${data.email}\n${data.address}", 44f, sy, 300f, sideBody, 30f) + 34f
            canvas.drawText("SKILLS", 44f, sy, sideHeader); sy += 38f
            data.skills.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { sk ->
                sy = drawWrappedLeft(canvas, "• $sk", 44f, sy, 300f, sideBody, 30f)
            }
            sy += 28f
            canvas.drawText("LANGUAGES", 44f, sy, sideHeader); sy += 38f
            drawWrappedLeft(canvas, data.languages, 44f, sy, 300f, sideBody, 30f)

            // Right column
            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 56f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val rolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = accent
                textSize = 30f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(data.fullName, 430f, 130f, namePaint)
            canvas.drawText(data.roleTitle, 430f, 176f, rolePaint)
            canvas.drawLine(430f, 208f, w - 50f, 208f, Paint().apply { color = accent; strokeWidth = 4f })

            drawCvSections(canvas, 430f, 260f, w - 490f, accent, data)
        } else {
            // Classic / Minimal top banner
            val topBg = Paint().apply { color = accent }
            canvas.drawRect(0f, 0f, w.toFloat(), 240f, topBg)
            val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 54f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val rolePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(226, 232, 240)
                textSize = 28f
            }
            canvas.drawText(data.fullName, 64f, 108f, namePaint)
            canvas.drawText("${data.roleTitle}  |  ${data.phone}  |  ${data.email}", 64f, 158f, rolePaint)
            canvas.drawText(data.address, 64f, 198f, rolePaint)

            if (photo != null) {
                canvas.save()
                val rect = RectF(w - 240f, 35f, w - 65f, 210f)
                val p = Path().apply { addRoundRect(rect, 18f, 18f, Path.Direction.CW) }
                canvas.clipPath(p)
                canvas.drawBitmap(photo, null, rect, Paint(Paint.FILTER_BITMAP_FLAG))
                canvas.restore()
            }

            var y = drawCvSections(canvas, 64f, 300f, w - 128f, accent, data)
            val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; textSize = 26f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(51, 65, 85); textSize = 22f }
            canvas.drawText("SKILLS & LANGUAGES", 64f, y, secPaint); y += 36f
            drawWrappedLeft(canvas, "${data.skills}\nLanguages: ${data.languages}", 64f, y, w - 128f, bodyPaint, 30f)
        }

        if (pdfOutFile != null) {
            val doc = PdfDocument()
            try {
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                val page = doc.startPage(pageInfo)
                page.canvas.drawBitmap(bmp, null, Rect(0, 0, 595, 842), Paint(Paint.FILTER_BITMAP_FLAG))
                doc.finishPage(page)
                FileOutputStream(pdfOutFile).use { doc.writeTo(it) }
            } finally {
                doc.close()
            }
        }
        return bmp
    }

    private fun drawCvSections(
        canvas: Canvas,
        x: Float,
        startY: Float,
        maxW: Float,
        accent: Int,
        data: CvData
    ): Float {
        val secPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            textSize = 27f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(51, 65, 85)
            textSize = 23f
        }
        var y = startY
        canvas.drawText("PROFILE SUMMARY", x, y, secPaint); y += 36f
        y = drawWrappedLeft(canvas, data.summary, x, y, maxW, bodyPaint, 31f) + 32f

        canvas.drawText("EXPERIENCE", x, y, secPaint); y += 36f
        y = drawWrappedLeft(canvas, data.experience, x, y, maxW, bodyPaint, 31f) + 32f

        canvas.drawText("EDUCATION", x, y, secPaint); y += 36f
        y = drawWrappedLeft(canvas, data.education, x, y, maxW, bodyPaint, 31f) + 32f

        canvas.drawText("REFERENCE", x, y, secPaint); y += 36f
        y = drawWrappedLeft(canvas, data.reference, x, y, maxW, bodyPaint, 31f) + 26f
        return y
    }

    fun renderInvoiceReceiptPdf(invoice: InvoiceEntity, outFile: File): Result<File> = runCatching {
        val doc = PdfDocument()
        try {
            val w = 595
            val h = 842
            val page = doc.startPage(PdfDocument.PageInfo.Builder(w, h, 1).create())
            val c = page.canvas
            c.drawColor(Color.WHITE)

            val headerBg = Paint().apply { color = Color.rgb(15, 23, 42) }
            c.drawRect(0f, 0f, w.toFloat(), 115f, headerBg)

            val studioPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(45, 212, 191)
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(203, 213, 225)
                textSize = 11f
            }
            c.drawText("YUNUS COMPUTER STUDIO PRO", 36f, 48f, studioPaint)
            c.drawText("Photo • Print • Design • Digital Service Receipt", 36f, 70f, subPaint)

            val invBadge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 16f
                textAlign = Paint.Align.RIGHT
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            c.drawText("INVOICE #${invoice.invoiceNumber}", w - 36f, 50f, invBadge)
            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(invoice.createdAt))
            c.drawText(dateStr, w - 36f, 72f, Paint(subPaint).apply { textAlign = Paint.Align.RIGHT })

            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(51, 65, 85)
                textSize = 12f
            }
            c.drawText("Billed To: ${invoice.customerName}", 36f, 152f, labelPaint)
            if (invoice.customerPhone.isNotBlank()) {
                c.drawText("Phone: ${invoice.customerPhone}", 36f, 172f, bodyPaint)
            }
            c.drawText("Payment: ${invoice.paymentMethod}  |  Status: ${invoice.status}", w - 36f, 152f, Paint(labelPaint).apply { textAlign = Paint.Align.RIGHT })

            // Table header
            val tableTop = 205f
            c.drawRect(36f, tableTop, w - 36f, tableTop + 28f, Paint().apply { color = Color.rgb(241, 245, 249) })
            c.drawText("Item Description", 46f, tableTop + 19f, labelPaint)
            c.drawText("Qty", 365f, tableTop + 19f, labelPaint)
            c.drawText("Rate", 425f, tableTop + 19f, labelPaint)
            c.drawText("Amount", w - 46f, tableTop + 19f, Paint(labelPaint).apply { textAlign = Paint.Align.RIGHT })

            val items = InvoiceItemCodec.decode(invoice.itemsJson)
            var y = tableTop + 50f
            items.forEach { item ->
                c.drawText(item.description.take(42), 46f, y, bodyPaint)
                c.drawText(item.quantity.toString(), 370f, y, bodyPaint)
                c.drawText(String.format("%.2f", item.unitPrice), 425f, y, bodyPaint)
                c.drawText(String.format("%.2f", item.lineTotal), w - 46f, y, Paint(bodyPaint).apply { textAlign = Paint.Align.RIGHT })
                c.drawLine(36f, y + 10f, w - 36f, y + 10f, Paint().apply { color = Color.rgb(226, 232, 240) })
                y += 28f
            }

            y += 18f
            val rightLabel = Paint(bodyPaint).apply { textAlign = Paint.Align.RIGHT }
            val rightBold = Paint(labelPaint).apply { textAlign = Paint.Align.RIGHT; textSize = 15f }
            c.drawText("Subtotal:  ${String.format("%.2f", invoice.subtotal)}", w - 46f, y, rightLabel); y += 22f
            c.drawText("Discount:  -${String.format("%.2f", invoice.discount)}", w - 46f, y, rightLabel); y += 26f
            c.drawText("Grand Total:  ${String.format("%.2f", invoice.total)}", w - 46f, y, rightBold); y += 22f
            c.drawText("Paid:  ${String.format("%.2f", invoice.paidAmount)}", w - 46f, y, rightLabel); y += 22f
            val due = (invoice.total - invoice.paidAmount).coerceAtLeast(0.0)
            c.drawText("Due Balance:  ${String.format("%.2f", due)}", w - 46f, y, rightBold)

            c.drawText(
                "Thank you for choosing Yunus Computer Studio Pro!",
                w / 2f,
                h - 42f,
                Paint(subPaint).apply { color = Color.GRAY; textAlign = Paint.Align.CENTER }
            )
            doc.finishPage(page)
            FileOutputStream(outFile).use { doc.writeTo(it) }
            outFile
        } finally {
            doc.close()
        }
    }

    private fun drawWrappedLeft(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint,
        lineHeight: Float
    ): Float {
        var y = startY
        text.lines().forEach { raw ->
            if (raw.isBlank()) {
                y += lineHeight * 0.6f
            } else {
                var rem = raw
                while (rem.isNotEmpty()) {
                    val count = paint.breakText(rem, true, maxWidth, null).coerceAtLeast(1)
                    // Try word boundary
                    val slice = if (count < rem.length) {
                        val lastSpace = rem.substring(0, count).lastIndexOf(' ')
                        if (lastSpace > 4) lastSpace else count
                    } else count
                    canvas.drawText(rem.substring(0, slice).trim(), x, y, paint)
                    y += lineHeight
                    rem = rem.substring(slice).trimStart()
                }
            }
        }
        return y
    }

    private fun drawMultilineCentered(
        canvas: Canvas,
        text: String,
        cx: Float,
        cy: Float,
        paint: Paint,
        maxWidth: Float
    ) {
        val lines = mutableListOf<String>()
        text.lines().forEach { raw ->
            var rem = raw
            while (rem.isNotEmpty()) {
                val count = paint.breakText(rem, true, maxWidth, null).coerceAtLeast(1)
                lines.add(rem.substring(0, count))
                rem = rem.substring(count)
            }
        }
        val lh = paint.textSize * 1.25f
        var y = cy - ((lines.size - 1) * lh) / 2f
        lines.forEach {
            canvas.drawText(it, cx, y, paint)
            y += lh
        }
    }

    private fun isColorDark(color: Int): Boolean {
        val lum = 0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)
        return lum < 140
    }
}
