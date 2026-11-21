package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.StudioProjectEntity
import com.example.engine.OfficeDocEngine
import com.example.engine.PdfEngine
import com.example.engine.PdfInfo
import com.example.engine.PdfPageSizeMode
import com.example.engine.PrintShareHelper
import com.example.studio.StudioViewModel
import com.example.ui.components.CheckerboardBitmapPreview
import com.example.ui.components.StudioModuleScaffold
import com.example.ui.components.StudioSectionCard
import com.example.ui.theme.MonoFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ---------------- MODULE 7: PDF TOOLS ----------------

private enum class PdfToolTab(val label: String) {
    IMAGES_TO_PDF("Images → PDF"),
    MERGE("Merge PDFs"),
    SPLIT("Split Pages"),
    PDF_TO_IMAGES("PDF → Images"),
    COMPRESS_WM("Compress & Watermark")
}

@Composable
fun PdfToolsScreen(
    viewModel: StudioViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var activeTab by remember { mutableStateOf(PdfToolTab.IMAGES_TO_PDF) }
    val selectedImageUris = remember { mutableStateListOf<Uri>() }
    val mergePdfUris = remember { mutableStateListOf<Uri>() }
    var singlePdfUri by remember { mutableStateOf<Uri?>(null) }
    var singlePdfInfo by remember { mutableStateOf<PdfInfo?>(null) }
    var pageSizeMode by remember { mutableStateOf(PdfPageSizeMode.A4_PORTRAIT) }
    var pageRangeExpr by remember { mutableStateOf("1-2") }
    var compressScale by remember { mutableFloatStateOf(1.2f) }
    var grayscale by remember { mutableStateOf(false) }
    var watermarkText by remember { mutableStateOf("") }
    var addPageNumbers by remember { mutableStateOf(true) }
    var lastOutputFile by remember { mutableStateOf<File?>(null) }
    var lastOutputImages by remember { mutableStateOf<List<File>>(emptyList()) }
    var statusBanner by remember { mutableStateOf("") }

    val imageMultiPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(25)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImageUris.clear()
            selectedImageUris.addAll(uris)
        }
    }

    val pdfMultiPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            mergePdfUris.clear()
            mergePdfUris.addAll(uris)
        }
    }

    val singlePdfPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            singlePdfUri = uri
            scope.launch {
                singlePdfInfo = withContext(Dispatchers.IO) { PdfEngine.inspectPdf(context, uri) }
                singlePdfInfo?.let { info ->
                    pageRangeExpr = if (info.pageCount > 1) "1-${info.pageCount}" else "1"
                }
            }
        }
    }

    StudioModuleScaffold(
        title = "PDF Tools",
        subtitle = "Images→PDF, Merge, Split, Extract, Compress & Watermark",
        accentColor = Color(0xFFEF4444),
        onBack = onBack,
        snackbarHostState = snackbar
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PdfToolTab.entries) { tab ->
                    FilterChip(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab; statusBanner = "" },
                        label = { Text(tab.label) },
                        modifier = Modifier.testTag("pdf_tab_${tab.name}")
                    )
                }
            }

            when (activeTab) {
                PdfToolTab.IMAGES_TO_PDF -> {
                    StudioSectionCard(title = "Convert Multiple Images to PDF") {
                        Button(
                            onClick = { imageMultiPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (selectedImageUris.isEmpty()) "Select Images (Up to 25)" else "${selectedImageUris.size} Images Selected — Change")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PdfPageSizeMode.entries.forEach { m ->
                                FilterChip(selected = pageSizeMode == m, onClick = { pageSizeMode = m }, label = { Text(m.label) })
                            }
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    val out = File(viewModel.repository.getExportsDir(), "images_${System.currentTimeMillis()}.pdf")
                                    val res = withContext(Dispatchers.IO) {
                                        PdfEngine.imagesToPdf(context, selectedImageUris.toList(), out, pageSizeMode)
                                    }
                                    res.onSuccess {
                                        lastOutputFile = it
                                        statusBanner = "Created ${it.name} (${it.length() / 1024} KB)"
                                        snackbar.showSnackbar(statusBanner)
                                    }.onFailure {
                                        snackbar.showSnackbar(it.message ?: "PDF creation failed")
                                    }
                                }
                            },
                            enabled = selectedImageUris.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Generate Multi-Page PDF")
                        }
                    }
                }

                PdfToolTab.MERGE -> {
                    StudioSectionCard(title = "Merge 2+ PDFs into One") {
                        Button(
                            onClick = { pdfMultiPicker.launch(arrayOf("application/pdf")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (mergePdfUris.isEmpty()) "Select 2 or More PDFs" else "${mergePdfUris.size} PDFs Selected — Change")
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    val out = File(viewModel.repository.getExportsDir(), "merged_${System.currentTimeMillis()}.pdf")
                                    val res = withContext(Dispatchers.IO) {
                                        PdfEngine.mergePdfs(context, mergePdfUris.toList(), out)
                                    }
                                    res.onSuccess {
                                        lastOutputFile = it
                                        statusBanner = "Merged PDF ready (${it.length() / 1024} KB)"
                                        snackbar.showSnackbar(statusBanner)
                                    }.onFailure { snackbar.showSnackbar(it.message ?: "Merge failed") }
                                }
                            },
                            enabled = mergePdfUris.size >= 2,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Merge Selected PDFs")
                        }
                    }
                }

                PdfToolTab.SPLIT, PdfToolTab.PDF_TO_IMAGES, PdfToolTab.COMPRESS_WM -> {
                    StudioSectionCard(title = "Select Source PDF") {
                        Button(
                            onClick = { singlePdfPicker.launch(arrayOf("application/pdf")) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.UploadFile, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (singlePdfUri == null) "Pick PDF Document" else "Change Source PDF")
                        }
                        singlePdfInfo?.let { info ->
                            Text(
                                "Pages: ${info.pageCount}  •  Original Size: ${info.fileSizeBytes / 1024} KB",
                                fontFamily = MonoFontFamily,
                                style = MaterialTheme.typography.bodySmall
                            )
                            CheckerboardBitmapPreview(
                                bitmap = info.firstPagePreview,
                                placeholderText = "Page 1 Preview",
                                modifier = Modifier.height(180.dp)
                            )
                        }
                    }

                    if (activeTab == PdfToolTab.SPLIT) {
                        StudioSectionCard(title = "Extract Page Range") {
                            OutlinedTextField(
                                value = pageRangeExpr,
                                onValueChange = { pageRangeExpr = it },
                                label = { Text("Pages (e.g. 1-3, 5)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    val uri = singlePdfUri ?: return@Button
                                    scope.launch {
                                        val out = File(viewModel.repository.getExportsDir(), "split_${System.currentTimeMillis()}.pdf")
                                        val res = withContext(Dispatchers.IO) { PdfEngine.splitPdf(context, uri, pageRangeExpr, out) }
                                        res.onSuccess {
                                            lastOutputFile = it
                                            statusBanner = "Extracted pages ($pageRangeExpr) -> ${it.length() / 1024} KB"
                                            snackbar.showSnackbar(statusBanner)
                                        }.onFailure { snackbar.showSnackbar(it.message ?: "Split failed") }
                                    }
                                },
                                enabled = singlePdfUri != null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Split & Export PDF")
                            }
                        }
                    }

                    if (activeTab == PdfToolTab.PDF_TO_IMAGES) {
                        StudioSectionCard(title = "Extract Every Page as PNG") {
                            Button(
                                onClick = {
                                    val uri = singlePdfUri ?: return@Button
                                    scope.launch {
                                        val res = withContext(Dispatchers.IO) {
                                            PdfEngine.pdfToImages(context, uri, viewModel.repository.getExportsDir(), "pdf_${System.currentTimeMillis()}")
                                        }
                                        res.onSuccess { files ->
                                            lastOutputImages = files
                                            statusBanner = "Extracted ${files.size} PNG image(s)."
                                            snackbar.showSnackbar(statusBanner)
                                        }.onFailure { snackbar.showSnackbar(it.message ?: "Extraction failed") }
                                    }
                                },
                                enabled = singlePdfUri != null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Convert All Pages to PNG")
                            }
                            if (lastOutputImages.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = {
                                        PrintShareHelper.shareFile(context, lastOutputImages.first(), "image/png")
                                    }, modifier = Modifier.weight(1f)) {
                                        Text("Share Page 1")
                                    }
                                    Button(onClick = {
                                        scope.launch {
                                            var count = 0
                                            withContext(Dispatchers.IO) {
                                                lastOutputImages.forEach { f ->
                                                    if (PrintShareHelper.saveFileToDownloads(context, f, "image/png").isSuccess) count++
                                                }
                                            }
                                            snackbar.showSnackbar("Saved $count PNG(s) to Downloads/YunusStudio")
                                        }
                                    }, modifier = Modifier.weight(1f)) {
                                        Text("Save All PNGs")
                                    }
                                }
                            }
                        }
                    }

                    if (activeTab == PdfToolTab.COMPRESS_WM) {
                        StudioSectionCard(title = "Compress, Grayscale & Stamp Watermark") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = compressScale < 1.0f, onClick = { compressScale = 0.85f }, label = { Text("Compact (72dpi)") })
                                FilterChip(selected = compressScale in 1.0f..1.4f, onClick = { compressScale = 1.25f }, label = { Text("Balanced") })
                                FilterChip(selected = compressScale > 1.4f, onClick = { compressScale = 1.8f }, label = { Text("Crisp") })
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Convert to Grayscale (B&W Print)")
                                Switch(checked = grayscale, onCheckedChange = { grayscale = it })
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Add Page Numbers")
                                Switch(checked = addPageNumbers, onCheckedChange = { addPageNumbers = it })
                            }
                            OutlinedTextField(
                                value = watermarkText,
                                onValueChange = { watermarkText = it },
                                label = { Text("Optional Diagonal Watermark Text") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    val uri = singlePdfUri ?: return@Button
                                    scope.launch {
                                        val out = File(viewModel.repository.getExportsDir(), "processed_${System.currentTimeMillis()}.pdf")
                                        val res = withContext(Dispatchers.IO) {
                                            PdfEngine.compressAndWatermarkPdf(context, uri, out, compressScale, grayscale, watermarkText, addPageNumbers)
                                        }
                                        res.onSuccess {
                                            lastOutputFile = it
                                            val origKb = (singlePdfInfo?.fileSizeBytes ?: 0L) / 1024
                                            val newKb = it.length() / 1024
                                            statusBanner = "Done! Before: ${origKb}KB → After: ${newKb}KB"
                                            snackbar.showSnackbar(statusBanner)
                                        }.onFailure { snackbar.showSnackbar(it.message ?: "Processing failed") }
                                    }
                                },
                                enabled = singlePdfUri != null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Process & Export PDF")
                            }
                        }
                    }
                }
            }

            lastOutputFile?.let { file ->
                StudioSectionCard(title = "Output PDF Ready") {
                    if (statusBanner.isNotBlank()) Text(statusBanner, style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            scope.launch {
                                val res = withContext(Dispatchers.IO) {
                                    PrintShareHelper.saveFileToDownloads(context, file, "application/pdf")
                                }
                                snackbar.showSnackbar(res.getOrElse { it.message ?: "Error" })
                            }
                        }, modifier = Modifier.weight(1f)) {
                            Text("Save Downloads")
                        }
                        OutlinedButton(onClick = {
                            PrintShareHelper.shareFile(context, file, "application/pdf")
                        }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Share, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Share")
                        }
                        OutlinedButton(onClick = {
                            PrintShareHelper.printPdfFile(context, file, file.name)
                        }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Print, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Print")
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------------- MODULE 8: WORD & EXCEL TOOLS ----------------

@Composable
fun OfficeToolsScreen(
    viewModel: StudioViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var isExcelTab by remember { mutableStateOf(false) }

    // Word state
    var docTitle by remember { mutableStateOf("Official Studio Letter") }
    var docBody by remember {
        mutableStateOf(
            "# Subject: Service Quotation\n\nDear Sir/Madam,\nPlease find below our studio service quotation for printing, ID photography, and document binding.\n\nSincerely,\nYunus Computer Studio Pro"
        )
    }

    // Excel grid state (5 cols x 6 rows initially)
    val sheetRows = remember {
        mutableStateListOf(
            mutableStateListOf("Item", "Qty", "Rate", "Total"),
            mutableStateListOf("Passport Photo", "2", "100", "200"),
            mutableStateListOf("CV Print & PDF", "3", "40", "120"),
            mutableStateListOf("Lamination", "5", "30", "150"),
            mutableStateListOf("Grand Total", "", "", "=SUM(D2:D4)")
        )
    }
    var sheetName by remember { mutableStateOf("Studio_Sheet") }

    val wordImporter = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val res = withContext(Dispatchers.IO) { OfficeDocEngine.readDocxOrText(context, uri) }
                res.onSuccess {
                    docBody = it
                    snackbar.showSnackbar("Imported document text (${it.length} chars).")
                }.onFailure { snackbar.showSnackbar(it.message ?: "Import failed") }
            }
        }
    }

    val sheetImporter = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val res = withContext(Dispatchers.IO) { OfficeDocEngine.readCsvOrXlsx(context, uri) }
                res.onSuccess { rows ->
                    if (rows.isNotEmpty()) {
                        val maxCols = maxOf(4, rows.maxOf { it.size })
                        sheetRows.clear()
                        rows.forEach { r ->
                            val padded = MutableList(maxCols) { idx -> r.getOrElse(idx) { "" } }
                            sheetRows.add(padded.toMutableList() as androidx.compose.runtime.snapshots.SnapshotStateList<String>)
                        }
                        // Ensure SnapshotStateList compatibility cleanly:
                        val converted = rows.map { r ->
                            mutableStateListOf<String>().apply {
                                for (c in 0 until maxCols) add(r.getOrElse(c) { "" })
                            }
                        }
                        sheetRows.clear()
                        sheetRows.addAll(converted)
                        snackbar.showSnackbar("Loaded ${rows.size} rows into grid.")
                    }
                }.onFailure { snackbar.showSnackbar(it.message ?: "Spreadsheet import failed") }
            }
        }
    }

    StudioModuleScaffold(
        title = "Word & Excel Tools",
        subtitle = "Real DOCX Writer/Reader & XLSX/CSV Formula Grid",
        accentColor = Color(0xFF22C55E),
        onBack = onBack,
        snackbarHostState = snackbar
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !isExcelTab, onClick = { isExcelTab = false }, label = { Text("Word (.docx / .txt)") })
                FilterChip(selected = isExcelTab, onClick = { isExcelTab = true }, label = { Text("Excel (.xlsx / .csv)") })
            }

            if (!isExcelTab) {
                StudioSectionCard(
                    title = "Word Document Studio",
                    trailing = {
                        OutlinedButton(onClick = {
                            wordImporter.launch(
                                arrayOf(
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "text/plain"
                                )
                            )
                        }) {
                            Text("Import DOCX/TXT")
                        }
                    }
                ) {
                    OutlinedTextField(
                        value = docTitle,
                        onValueChange = { docTitle = it },
                        label = { Text("Document Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = docBody,
                        onValueChange = { docBody = it },
                        label = { Text("Document Body (prefix lines with '# ' for headings)") },
                        minLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                    val words = docBody.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.size
                    Text(
                        "Words: $words  •  Characters: ${docBody.length}",
                        fontFamily = MonoFontFamily,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            scope.launch {
                                val out = File(viewModel.repository.getExportsDir(), "${docTitle.ifBlank { "doc" }.replace(" ", "_")}.docx")
                                val res = withContext(Dispatchers.IO) { OfficeDocEngine.writeDocx(docTitle, docBody, out) }
                                res.onSuccess {
                                    PrintShareHelper.saveFileToDownloads(context, it, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                                    snackbar.showSnackbar("Exported real .docx (${it.length() / 1024} KB)")
                                    PrintShareHelper.shareFile(context, it, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                                }
                            }
                        }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Description, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Export DOCX")
                        }
                        OutlinedButton(onClick = {
                            scope.launch {
                                val out = File(viewModel.repository.getExportsDir(), "${docTitle.ifBlank { "doc" }.replace(" ", "_")}.pdf")
                                val res = withContext(Dispatchers.IO) { OfficeDocEngine.writeTextToPdf(docTitle, docBody, out) }
                                res.onSuccess {
                                    PrintShareHelper.printPdfFile(context, it, docTitle)
                                }
                            }
                        }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Print, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("PDF / Print")
                        }
                    }
                    OutlinedButton(onClick = {
                        viewModel.saveProject(
                            StudioProjectEntity(
                                moduleType = "WORD_DOC",
                                title = docTitle.ifBlank { "Untitled Doc" },
                                payloadJson = docBody
                            )
                        )
                        scope.launch { snackbar.showSnackbar("Document saved to Projects.") }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Save Document Project Locally")
                    }
                }
            } else {
                StudioSectionCard(
                    title = "Excel & Formula Grid",
                    trailing = {
                        OutlinedButton(onClick = {
                            sheetImporter.launch(
                                arrayOf(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "text/csv",
                                    "text/comma-separated-values"
                                )
                            )
                        }) {
                            Text("Import XLSX/CSV")
                        }
                    }
                ) {
                    OutlinedTextField(
                        value = sheetName,
                        onValueChange = { sheetName = it },
                        label = { Text("Workbook Sheet Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Supports formulas: =SUM(D2:D4), =AVG(...), =MIN(...), =MAX(...), =COUNT(...)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val snapshotGrid = sheetRows.map { it.toList() }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Column headers A, B, C...
                        val colCount = sheetRows.firstOrNull()?.size ?: 4
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Spacer(Modifier.width(34.dp))
                            for (c in 0 until colCount) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.width(110.dp)
                                ) {
                                    Text(
                                        text = ('A' + c).toString(),
                                        fontFamily = MonoFontFamily,
                                        modifier = Modifier.padding(6.dp),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                        sheetRows.forEachIndexed { rIdx, row ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.width(34.dp)
                                ) {
                                    Text(
                                        text = "${rIdx + 1}",
                                        fontFamily = MonoFontFamily,
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                row.forEachIndexed { cIdx, cellVal ->
                                    val evaluated = if (cellVal.trim().startsWith("=")) {
                                        "→ ${OfficeDocEngine.evaluateCell(cellVal, snapshotGrid)}"
                                    } else null
                                    OutlinedTextField(
                                        value = cellVal,
                                        onValueChange = { newVal -> sheetRows[rIdx][cIdx] = newVal },
                                        supportingText = evaluated?.let { { Text(it, fontFamily = MonoFontFamily) } },
                                        singleLine = true,
                                        modifier = Modifier.width(110.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            val cols = sheetRows.firstOrNull()?.size ?: 4
                            sheetRows.add(mutableStateListOf<String>().apply { repeat(cols) { add("") } })
                        }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Add Row")
                        }
                        OutlinedButton(onClick = {
                            sheetRows.forEach { it.add("") }
                        }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Add Col")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            scope.launch {
                                val out = File(viewModel.repository.getExportsDir(), "${sheetName.ifBlank { "sheet" }}.xlsx")
                                val res = withContext(Dispatchers.IO) {
                                    OfficeDocEngine.writeXlsx(sheetRows.map { it.toList() }, sheetName, out)
                                }
                                res.onSuccess {
                                    PrintShareHelper.saveFileToDownloads(context, it, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                    snackbar.showSnackbar("Exported real .xlsx workbook")
                                    PrintShareHelper.shareFile(context, it, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                                }
                            }
                        }, modifier = Modifier.weight(1f)) {
                            Text("Export XLSX")
                        }
                        OutlinedButton(onClick = {
                            scope.launch {
                                val out = File(viewModel.repository.getExportsDir(), "${sheetName.ifBlank { "sheet" }}.csv")
                                val res = withContext(Dispatchers.IO) {
                                    OfficeDocEngine.writeCsv(sheetRows.map { it.toList() }, out)
                                }
                                res.onSuccess {
                                    PrintShareHelper.saveFileToDownloads(context, it, "text/csv")
                                    snackbar.showSnackbar("Exported CSV to Downloads")
                                    PrintShareHelper.shareFile(context, it, "text/csv")
                                }
                            }
                        }, modifier = Modifier.weight(1f)) {
                            Text("Export CSV")
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
