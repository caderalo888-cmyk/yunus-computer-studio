# Yunus Computer Studio Pro (Android Studio Edition)

A complete, offline-first **21-Module Creative, Print, Document, Audio & Studio Business Suite** built with **Kotlin, Jetpack Compose, Material 3, and Room SQLite** — with **zero paid APIs** and **100% local processing**.

## 21 Included Modules

1. **Dashboard** — Live studio KPIs, search, category filters, light/dark theme toggle, and adaptive NavigationBar / NavigationRail.
2. **Passport Photo Studio** — 40×50mm, 2×2", EU 35×45mm, Stamp sizes, studio backdrop replacement, A4/4R print sheet tiler.
3. **Visa Photo Studio** — Schengen, USA DS-160, Saudi/Gulf, Malaysia/Dubai, Canada embassy presets.
4. **NID Photo Studio** — Bangladesh NID, Smart Card, and <100 KB digital portal export.
5. **Photo Resize & HD Enhancement** — Pixel resize with aspect lock, target KB compression, 3×3 unsharp mask sharpening, auto-levels, brightness/contrast/saturation.
6. **Background Removal** — Edge-seeded flood-fill, chroma key (white/green), portrait oval cutout, touch eraser brush + undo, studio backdrops & transparent PNG export.
7. **PDF Tools** — Images-to-PDF, Merge PDFs, Split/Extract page ranges, PDF-to-Images (PNG), Compress, Grayscale & Watermark/Page Numbers.
8. **Word & Excel Tools** — Real OpenXML `.docx` & `.xlsx` generator/reader, `.csv` import/export, live spreadsheet formula evaluator (`=SUM`, `=AVG`, `=MIN`, `=MAX`, `=COUNT`).
9. **Graphic Design Studio** — Layered canvas editor with text, badges, shapes, gradients, rotation, and PNG/Print/Project export.
10. **Wedding Card Designer** — Royal Maroon, Emerald & Ivory templates with live high-res card rendering.
11. **Election Poster Designer** — Candidate photo frame, vector campaign symbols, manifesto & slogan layout.
12. **Visiting Card Designer** — 3.5×2" cards with embedded vCard QR code and 10-up A4 print sheet mode.
13. **CV Maker** — 3 ATS/Modern A4 resume templates with photo support and instant PDF export.
14. **Bangla Text to Speech** — Native `TextToSpeech` (`bn-BD` / `bn-IN` / `en-US`) with rate, pitch & WAV export.
15. **Dual Voice Audio Studio** — 2-speaker dialogue script player and combined `.wav` mixdown stitcher.
16. **Audio Editor** — Microphone recording (`AudioRecord`), audio import (`MediaCodec`), waveform visualizer, trim, gain, fade, echo, speed, reverse, normalize & WAV export.
17. **Print Studio** — Multi-up photo sheet & PDF printing via Android `PrintManager`.
18. **QR Code Generator** — Pure Kotlin ISO/IEC 18004 Reed-Solomon QR encoder (URL, Wi-Fi, vCard, Phone) with custom colors & center badge.
19. **Customer Management** — Local Room SQLite client directory with order totals.
20. **Billing & Receipt** — Itemized invoices with studio presets, bKash/Nagad/Cash tracking, auto-ledger sync, and printable PDF receipts.
21. **Income & Expense Management** — Real-time profit analytics, canvas ratio chart, ledger filtering, and PDF statement export.

## GitHub Upload Instructions

```bash
git init
git add .
git commit -m "Initial commit: Yunus Computer Studio Pro (21 Modules)"
git branch -M main
git remote add origin https://github.com/<YOUR_USERNAME>/yunus-computer-studio-pro.git
git push -u origin main
```

## Build Android APK Locally

```bash
gradle :app:assembleDebug
```
