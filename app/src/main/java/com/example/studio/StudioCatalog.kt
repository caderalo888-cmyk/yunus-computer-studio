package com.example.studio

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.ContactMail
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PersonPin
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.SpatialAudioOff
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class ModuleCategory(val label: String) {
    ALL("All (21)"),
    PHOTO_ID("Photo & ID"),
    DOCS_PDF("Docs & PDF"),
    DESIGN_PRINT("Design & Print"),
    AUDIO_QR("Audio & QR"),
    BUSINESS("Business")
}

object StudioRoutes {
    const val DASHBOARD = "dashboard"
    const val PASSPORT = "passport_photo"
    const val VISA = "visa_photo"
    const val NID = "nid_photo"
    const val PHOTO_ENHANCE = "photo_enhance"
    const val BG_REMOVAL = "bg_removal"
    const val PDF_TOOLS = "pdf_tools"
    const val OFFICE_TOOLS = "office_tools"
    const val GRAPHIC_DESIGN = "graphic_design"
    const val WEDDING_CARD = "wedding_card"
    const val ELECTION_POSTER = "election_poster"
    const val VISITING_CARD = "visiting_card"
    const val CV_MAKER = "cv_maker"
    const val BANGLA_TTS = "bangla_tts"
    const val DUAL_VOICE = "dual_voice"
    const val AUDIO_EDITOR = "audio_editor"
    const val PRINT_STUDIO = "print_studio"
    const val QR_GENERATOR = "qr_generator"
    const val CUSTOMERS = "customers"
    const val BILLING = "billing"
    const val LEDGER = "ledger"
}

data class StudioModuleItem(
    val number: Int,
    val route: String,
    val title: String,
    val subtitle: String,
    val category: ModuleCategory,
    val icon: ImageVector,
    val accentColor: Color
)

object StudioCatalog {
    val modules: List<StudioModuleItem> = listOf(
        StudioModuleItem(1, StudioRoutes.DASHBOARD, "Dashboard", "Studio overview, quick stats & tools", ModuleCategory.ALL, Icons.Rounded.Dashboard, Color(0xFF14B8A6)),
        StudioModuleItem(2, StudioRoutes.PASSPORT, "Passport Photo Studio", "40×50mm, 2×2\", A4 & 4R print sheet", ModuleCategory.PHOTO_ID, Icons.Rounded.PersonPin, Color(0xFF0EA5E9)),
        StudioModuleItem(3, StudioRoutes.VISA, "Visa Photo Studio", "Schengen, USA, Saudi, Malaysia specs", ModuleCategory.PHOTO_ID, Icons.Rounded.FlightTakeoff, Color(0xFF3B82F6)),
        StudioModuleItem(4, StudioRoutes.NID, "NID Photo Studio", "Bangladesh NID, Smart Card & <100KB", ModuleCategory.PHOTO_ID, Icons.Rounded.Badge, Color(0xFF06B6D4)),
        StudioModuleItem(5, StudioRoutes.PHOTO_ENHANCE, "Photo Resize & HD", "Sharpen, auto-levels, resize & target KB", ModuleCategory.PHOTO_ID, Icons.Rounded.AutoFixHigh, Color(0xFF10B981)),
        StudioModuleItem(6, StudioRoutes.BG_REMOVAL, "Background Removal", "Flood-fill, chroma key, brush & backdrop", ModuleCategory.PHOTO_ID, Icons.Rounded.ContentCut, Color(0xFFEC4899)),
        StudioModuleItem(7, StudioRoutes.PDF_TOOLS, "PDF Tools", "Images→PDF, Merge, Split, Compress", ModuleCategory.DOCS_PDF, Icons.Rounded.PictureAsPdf, Color(0xFFEF4444)),
        StudioModuleItem(8, StudioRoutes.OFFICE_TOOLS, "Word & Excel Tools", "Real DOCX & XLSX/CSV editor & export", ModuleCategory.DOCS_PDF, Icons.Rounded.TableChart, Color(0xFF22C55E)),
        StudioModuleItem(9, StudioRoutes.GRAPHIC_DESIGN, "Graphic Design Studio", "Layered poster, flyer & banner editor", ModuleCategory.DESIGN_PRINT, Icons.Rounded.Layers, Color(0xFF8B5CF6)),
        StudioModuleItem(10, StudioRoutes.WEDDING_CARD, "Wedding Card Designer", "Royal Gold, Emerald & Classic invites", ModuleCategory.DESIGN_PRINT, Icons.Rounded.Celebration, Color(0xFFF59E0B)),
        StudioModuleItem(11, StudioRoutes.ELECTION_POSTER, "Election Poster Designer", "Candidate photo, symbol & manifesto", ModuleCategory.DESIGN_PRINT, Icons.Rounded.Campaign, Color(0xFFF97316)),
        StudioModuleItem(12, StudioRoutes.VISITING_CARD, "Visiting Card Designer", "3.5×2\" cards with QR & 10-up A4 sheet", ModuleCategory.DESIGN_PRINT, Icons.Rounded.ContactMail, Color(0xFF6366F1)),
        StudioModuleItem(13, StudioRoutes.CV_MAKER, "CV Maker", "3 modern templates with instant A4 PDF", ModuleCategory.DOCS_PDF, Icons.Rounded.Description, Color(0xFF14B8A6)),
        StudioModuleItem(14, StudioRoutes.BANGLA_TTS, "Bangla Text to Speech", "bn-BD/en speech playback & WAV export", ModuleCategory.AUDIO_QR, Icons.Rounded.RecordVoiceOver, Color(0xFFA855F7)),
        StudioModuleItem(15, StudioRoutes.DUAL_VOICE, "Dual Voice Audio Studio", "2-speaker dialogue script & WAV mixdown", ModuleCategory.AUDIO_QR, Icons.Rounded.SpatialAudioOff, Color(0xFFD946EF)),
        StudioModuleItem(16, StudioRoutes.AUDIO_EDITOR, "Audio Editor", "Mic record, trim, fade, echo & WAV DSP", ModuleCategory.AUDIO_QR, Icons.Rounded.GraphicEq, Color(0xFF06B6D4)),
        StudioModuleItem(17, StudioRoutes.PRINT_STUDIO, "Print Studio", "A4/4R/Letter multi-up photo & PDF print", ModuleCategory.DESIGN_PRINT, Icons.Rounded.Print, Color(0xFF64748B)),
        StudioModuleItem(18, StudioRoutes.QR_GENERATOR, "QR Code Generator", "URL, Wi-Fi, vCard & Payment QR PNG/PDF", ModuleCategory.AUDIO_QR, Icons.Rounded.QrCode2, Color(0xFF0EA5E9)),
        StudioModuleItem(19, StudioRoutes.CUSTOMERS, "Customer Management", "Client directory, phone & invoice history", ModuleCategory.BUSINESS, Icons.Rounded.Groups, Color(0xFF3B82F6)),
        StudioModuleItem(20, StudioRoutes.BILLING, "Billing & Receipt", "Itemized invoices, bKash/Cash & PDF", ModuleCategory.BUSINESS, Icons.Rounded.ReceiptLong, Color(0xFFF59E0B)),
        StudioModuleItem(21, StudioRoutes.LEDGER, "Income & Expense", "Profit analytics, ledger & statement PDF", ModuleCategory.BUSINESS, Icons.Rounded.AccountBalanceWallet, Color(0xFF10B981))
    )
}
