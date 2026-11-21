package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.local.StudioProjectEntity
import com.example.engine.ImageEngine
import com.example.engine.PdfEngine
import com.example.engine.PdfInfo
import com.example.engine.PrintShareHelper
import com.example.engine.QrEccLevel
import com.example.engine.QrEngine
import com.example.studio.StudioViewModel
import com.example.ui.components.CheckerboardBitmapPreview
import com.example.ui.components.ColorSwatchPicker
import com.example.ui.components.ExportActionBar
import com.example.ui.components.StudioModuleScaffold
import com.example.ui.components.StudioSectionCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ---------------- MODULE 17: PRINT STUDIO ----------------

@Composable
fun PrintStudioScreen(
    viewModel: StudioViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var sourceImage by remember { mutableStateOf<Bitmap?>(null) }
    var composedSheet by remember { mutableStateOf<Bitmap?>(null) }
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfInfo by remember { mutableStateOf<PdfInfo?>(null) }
    var gridCopies by remember { mutableIntStateOf(1) }
    var isA4 by remember { mutableStateOf(true) }
    var landscape by remember { mutableStateOf(false) }
    var printCopies by remember { mutableIntStateOf(1) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedPdfUri = null
            pdfInfo = null
            scope.launch {
                sourceImage = withContext(Dispatchers.IO) { ImageEngine.decodeUri(context, uri, 1800) }
            }
        }
    }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            sourceImage = null
            composedSheet = null
            selectedPdfUri = uri
            scope.launch {
                pdfInfo = withContext(Dispatchers.IO) { PdfEngine.inspectPdf(context, uri) }
            }
        }
    }

    LaunchedEffect(sourceImage, gridCopies, isA4) {
        val src = sourceImage ?: return@LaunchedEffect
        composedSheet = withContext(Dispatchers.Default) {
            if (gridCopies == 1) src
            else ImageEngine.composePrintSheet(src, copies = gridCopies, isA4Sheet = isA4)
        }
    }

    StudioModuleScaffold(
        title = "Print Studio",
        subtitle = "A4 / 4R Multi-Up Photo & PDF Print Spooler",
        accentColor = Color(0xFF64748B),
        onBack = onBack,
        snackbarHostState = snackbar
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Print Photo")
                }
                OutlinedButton(
                    onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Print PDF")
                }
            }

            CheckerboardBitmapPreview(
                bitmap = composedSheet ?: pdfInfo?.firstPagePreview,
                placeholderText = "Select an image or PDF document to preview and print"
            )

            if (sourceImage != null) {
                StudioSectionCard(title = "Photo Print Sheet Options") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = isA4, onClick = { isA4 = true }, label = { Text("A4 Paper") })
                        FilterChip(selected = !isA4, onClick = { isA4 = false }, label = { Text("4R (4×6\")") })
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(1, 2, 4, 6, 8, 12)) { n ->
                            FilterChip(
                                selected = gridCopies == n,
                                onClick = { gridCopies = n },
                                label = { Text(if (n == 1) "Full Page (1-Up)" else "$n-Up Grid") }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Landscape Orientation")
                        Switch(checked = landscape, onCheckedChange = { landscape = it })
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(listOf(1, 2, 3, 5, 10)) { c ->
                            FilterChip(selected = printCopies == c, onClick = { printCopies = c }, label = { Text("$c Sheet(s)") })
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val img = composedSheet
                    val pdf = selectedPdfUri
                    when {
                        img != null -> PrintShareHelper.printBitmap(
                            context = context,
                            bitmap = img,
                            jobName = "Yunus Print Studio",
                            copies = printCopies,
                            landscape = landscape
                        )
                        pdf != null -> {
                            scope.launch {
                                val temp = File(context.cacheDir, "print_doc.pdf")
                                withContext(Dispatchers.IO) {
                                    context.contentResolver.openInputStream(pdf)?.use { inp ->
                                        temp.outputStream().use { out -> inp.copyTo(out) }
                                    }
                                }
                                PrintShareHelper.printPdfFile(context, temp, "Yunus Studio PDF")
                            }
                        }
                        else -> scope.launch { snackbar.showSnackbar("Select a photo or PDF first.") }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Print, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Send to Android Print Manager")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------------- MODULE 18: QR CODE GENERATOR ----------------

private enum class QrInputType(val label: String) {
    URL_TEXT("URL / Text"),
    WIFI("Wi-Fi Login"),
    VCARD("Contact vCard"),
    PHONE_SMS("Phone / bKash")
}

@Composable
fun QrGeneratorScreen(
    viewModel: StudioViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var inputType by remember { mutableStateOf(QrInputType.URL_TEXT) }
    var textValue by remember { mutableStateOf("https://yunusstudio.pro") }
    var wifiSsid by remember { mutableStateOf("YunusStudio_5G") }
    var wifiPass by remember { mutableStateOf("studio2026") }
    var vcardName by remember { mutableStateOf("Md. Yunus Ali") }
    var vcardPhone by remember { mutableStateOf("+8801711000000") }
    var vcardOrg by remember { mutableStateOf("Yunus Computer Studio Pro") }
    var fgColor by remember { mutableIntStateOf(AndroidColor.rgb(15, 23, 42)) }
    var bgColor by remember { mutableIntStateOf(AndroidColor.WHITE) }
    var rounded by remember { mutableStateOf(true) }
    var ecc by remember { mutableStateOf(QrEccLevel.M) }
    var centerBadge by remember { mutableStateOf("YUNUS") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val payload = remember(inputType, textValue, wifiSsid, wifiPass, vcardName, vcardPhone, vcardOrg) {
        when (inputType) {
            QrInputType.URL_TEXT -> textValue.ifBlank { "Yunus Studio" }
            QrInputType.WIFI -> "WIFI:T:WPA;S:$wifiSsid;P:$wifiPass;;"
            QrInputType.VCARD -> "BEGIN:VCARD\nVERSION:3.0\nFN:$vcardName\nORG:$vcardOrg\nTEL:$vcardPhone\nEND:VCARD"
            QrInputType.PHONE_SMS -> "tel:$vcardPhone"
        }
    }

    LaunchedEffect(payload, fgColor, bgColor, rounded, ecc, centerBadge) {
        qrBitmap = withContext(Dispatchers.Default) {
            QrEngine.renderBitmap(
                content = payload,
                pixelSize = 900,
                fgColor = fgColor,
                bgColor = bgColor,
                rounded = rounded,
                ecc = ecc,
                centerLabel = centerBadge
            )
        }
    }

    suspend fun writePng(): File? = withContext(Dispatchers.IO) {
        val bmp = qrBitmap ?: return@withContext null
        val f = File(viewModel.repository.getExportsDir(), "qr_${System.currentTimeMillis()}.png")
        ImageEngine.exportCompressedImage(bmp, f, Bitmap.CompressFormat.PNG, 100)
        f
    }

    StudioModuleScaffold(
        title = "QR Code Generator",
        subtitle = "Offline Reed-Solomon ISO/IEC 18004 QR Encoder",
        accentColor = Color(0xFF0EA5E9),
        onBack = onBack,
        snackbarHostState = snackbar
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CheckerboardBitmapPreview(bitmap = qrBitmap, placeholderText = "Generating QR…")

            StudioSectionCard(title = "QR Payload Type") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(QrInputType.entries) { t ->
                        FilterChip(selected = inputType == t, onClick = { inputType = t }, label = { Text(t.label) })
                    }
                }
                when (inputType) {
                    QrInputType.URL_TEXT -> OutlinedTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        label = { Text("Website URL or Custom Text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    QrInputType.WIFI -> {
                        OutlinedTextField(value = wifiSsid, onValueChange = { wifiSsid = it }, label = { Text("Wi-Fi SSID") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = wifiPass, onValueChange = { wifiPass = it }, label = { Text("Wi-Fi Password") }, modifier = Modifier.fillMaxWidth())
                    }
                    QrInputType.VCARD -> {
                        OutlinedTextField(value = vcardName, onValueChange = { vcardName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = vcardPhone, onValueChange = { vcardPhone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = vcardOrg, onValueChange = { vcardOrg = it }, label = { Text("Business / Studio") }, modifier = Modifier.fillMaxWidth())
                    }
                    QrInputType.PHONE_SMS -> OutlinedTextField(
                        value = vcardPhone,
                        onValueChange = { vcardPhone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            StudioSectionCard(title = "Style & Error Correction") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Rounded QR Modules")
                    Switch(checked = rounded, onCheckedChange = { rounded = it })
                }
                OutlinedTextField(
                    value = centerBadge,
                    onValueChange = { centerBadge = it.take(10) },
                    label = { Text("Center Label Badge (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Foreground Color", style = MaterialTheme.typography.bodySmall)
                ColorSwatchPicker(
                    colors = listOf(
                        "Slate" to AndroidColor.rgb(15, 23, 42),
                        "Black" to AndroidColor.BLACK,
                        "Teal" to AndroidColor.rgb(13, 148, 136),
                        "Indigo" to AndroidColor.rgb(67, 56, 202),
                        "Crimson" to AndroidColor.rgb(185, 28, 28)
                    ),
                    selectedColor = fgColor,
                    onSelect = { fgColor = it }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QrEccLevel.entries.forEach { lvl ->
                        FilterChip(selected = ecc == lvl, onClick = { ecc = lvl }, label = { Text("ECC ${lvl.name}") })
                    }
                }
            }

            ExportActionBar(
                onSaveProject = {
                    scope.launch {
                        val f = writePng()
                        viewModel.saveProject(
                            StudioProjectEntity(
                                moduleType = "QR_CODE",
                                title = payload.take(32),
                                payloadJson = payload,
                                previewPath = f?.absolutePath ?: ""
                            )
                        )
                        snackbar.showSnackbar("QR Code saved to Projects.")
                    }
                },
                onSaveDownloads = {
                    scope.launch {
                        val f = writePng() ?: return@launch
                        val res = PrintShareHelper.saveFileToDownloads(context, f, "image/png")
                        snackbar.showSnackbar(res.getOrElse { it.message ?: "Error" })
                    }
                },
                onShare = { scope.launch { writePng()?.let { PrintShareHelper.shareFile(context, it, "image/png") } } },
                onPrint = { qrBitmap?.let { PrintShareHelper.printBitmap(context, it, "QR Code") } }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
