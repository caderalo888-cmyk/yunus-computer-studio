package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
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
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.engine.ImageEngine
import com.example.engine.PhotoPreset
import com.example.engine.PhotoPresets
import com.example.engine.PrintShareHelper
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

enum class IdPhotoMode(
    val title: String,
    val subtitle: String,
    val accent: Color,
    val presets: List<PhotoPreset>,
    val moduleKey: String
) {
    PASSPORT("Passport Photo Studio", "300 DPI Biometric Sizes & Print Sheets", Color(0xFF0EA5E9), PhotoPresets.passportPresets, "PASSPORT_PHOTO"),
    VISA("Visa Photo Studio", "Global Embassy & Visa Specs", Color(0xFF3B82F6), PhotoPresets.visaPresets, "VISA_PHOTO"),
    NID("NID Photo Studio", "Bangladesh NID, Smart Card & <100KB Portal", Color(0xFF06B6D4), PhotoPresets.nidPresets, "NID_PHOTO")
}

@Composable
fun IdPhotoStudioScreen(
    mode: IdPhotoMode,
    viewModel: StudioViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedPreset by remember { mutableStateOf(mode.presets.first()) }
    var replaceBg by remember { mutableStateOf(false) }
    var bgColor by remember { mutableIntStateOf(AndroidColor.WHITE) }
    var bgTolerance by remember { mutableFloatStateOf(36f) }
    var zoom by remember { mutableFloatStateOf(1.0f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var addBorder by remember { mutableStateOf(true) }
    var showPrintSheet by remember { mutableStateOf(false) }
    var sheetCopies by remember { mutableIntStateOf(8) }
    var isA4Sheet by remember { mutableStateOf(true) }
    var compressUnder100Kb by remember { mutableStateOf(mode == IdPhotoMode.NID) }
    var outputBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) { ImageEngine.decodeUri(context, uri, 1600) }
                if (bmp != null) sourceBitmap = bmp
                else snackbar.showSnackbar("Failed to load image.")
            }
        }
    }

    LaunchedEffect(sourceBitmap, selectedPreset, replaceBg, bgColor, bgTolerance, zoom, panX, panY, addBorder, showPrintSheet, sheetCopies, isA4Sheet) {
        val src = sourceBitmap ?: return@LaunchedEffect
        outputBitmap = withContext(Dispatchers.Default) {
            val single = ImageEngine.cropAndFormatIdPhoto(
                source = src,
                targetWidth = selectedPreset.widthPx,
                targetHeight = selectedPreset.heightPx,
                zoom = zoom,
                panX = panX,
                panY = panY,
                replaceBgColor = if (replaceBg) bgColor else null,
                bgTolerance = bgTolerance.toInt(),
                addThinBorder = addBorder
            )
            if (showPrintSheet) {
                ImageEngine.composePrintSheet(single, copies = sheetCopies, isA4Sheet = isA4Sheet)
            } else single
        }
    }

    fun writeExportFile(): File? {
        val bmp = outputBitmap ?: return null
        val ext = if (compressUnder100Kb && !showPrintSheet) "jpg" else "png"
        val file = File(viewModel.repository.getExportsDir(), "${mode.moduleKey.lowercase()}_${System.currentTimeMillis()}.$ext")
        val fmt = if (ext == "jpg") Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
        ImageEngine.exportCompressedImage(
            bitmap = bmp,
            outputFile = file,
            format = fmt,
            quality = 95,
            maxKb = if (compressUnder100Kb && !showPrintSheet) 98 else null
        )
        return file
    }

    StudioModuleScaffold(
        title = mode.title,
        subtitle = mode.subtitle,
        accentColor = mode.accent,
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
            Button(
                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth().testTag("pick_id_photo_button")
            ) {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (sourceBitmap == null) "Select Portrait Photo" else "Change Portrait Photo")
            }

            CheckerboardBitmapPreview(
                bitmap = outputBitmap,
                placeholderText = "Select a portrait photo to format ${selectedPreset.mmLabel} (${selectedPreset.widthPx}×${selectedPreset.heightPx} px)"
            )

            StudioSectionCard(title = "1. Standard Size Preset (${selectedPreset.mmLabel})") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(mode.presets) { preset ->
                        FilterChip(
                            selected = selectedPreset.id == preset.id,
                            onClick = { selectedPreset = preset },
                            label = { Text("${preset.title} (${preset.mmLabel})") }
                        )
                    }
                }
                Text(
                    "Output Dimensions: ${selectedPreset.widthPx} × ${selectedPreset.heightPx} px",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            StudioSectionCard(title = "2. Framing & Studio Background") {
                Text("Zoom: ${String.format("%.2f", zoom)}×", style = MaterialTheme.typography.labelLarge)
                Slider(value = zoom, onValueChange = { zoom = it }, valueRange = 0.8f..2.6f)

                Text("Horizontal / Vertical Adjust", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Pan X", style = MaterialTheme.typography.bodySmall)
                        Slider(value = panX, onValueChange = { panX = it }, valueRange = -1f..1f)
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Pan Y", style = MaterialTheme.typography.bodySmall)
                        Slider(value = panY, onValueChange = { panY = it }, valueRange = -1f..1f)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-Replace Background Color", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = replaceBg, onCheckedChange = { replaceBg = it })
                }

                if (replaceBg) {
                    ColorSwatchPicker(
                        colors = listOf(
                            "White" to AndroidColor.WHITE,
                            "StudioBlue" to AndroidColor.rgb(29, 78, 216),
                            "Sky" to AndroidColor.rgb(56, 189, 248),
                            "OffWhite" to AndroidColor.rgb(241, 245, 249),
                            "Red" to AndroidColor.rgb(185, 28, 28)
                        ),
                        selectedColor = bgColor,
                        onSelect = { bgColor = it }
                    )
                    Text("Edge Tolerance: ${bgTolerance.toInt()}", style = MaterialTheme.typography.bodySmall)
                    Slider(value = bgTolerance, onValueChange = { bgTolerance = it }, valueRange = 12f..85f)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cutting Border Outline", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = addBorder, onCheckedChange = { addBorder = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Portal Size Limit (<100 KB JPG)", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = compressUnder100Kb, onCheckedChange = { compressUnder100Kb = it })
                }
            }

            StudioSectionCard(
                title = "3. Print Sheet Layout (A4 / 4R)",
                trailing = {
                    OutlinedButton(onClick = { showPrintSheet = !showPrintSheet }) {
                        Icon(Icons.Rounded.GridOn, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (showPrintSheet) "Single View" else "Sheet View")
                    }
                }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = isA4Sheet, onClick = { isA4Sheet = true; showPrintSheet = true }, label = { Text("A4 Sheet") })
                    FilterChip(selected = !isA4Sheet, onClick = { isA4Sheet = false; showPrintSheet = true }, label = { Text("4R (4×6\")") })
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(2, 4, 6, 8, 12, 20)) { count ->
                        FilterChip(
                            selected = sheetCopies == count && showPrintSheet,
                            onClick = { sheetCopies = count; showPrintSheet = true },
                            label = { Text("$count Copies") }
                        )
                    }
                }
            }

            ExportActionBar(
                onSaveProject = {
                    scope.launch {
                        val file = withContext(Dispatchers.IO) { writeExportFile() }
                        if (file == null) {
                            snackbar.showSnackbar("Select a photo first.")
                            return@launch
                        }
                        viewModel.saveProject(
                            StudioProjectEntity(
                                moduleType = mode.moduleKey,
                                title = "${selectedPreset.title} (${if (showPrintSheet) "${sheetCopies}x Sheet" else "Single"})",
                                payloadJson = "{\"preset\":\"${selectedPreset.id}\",\"copies\":$sheetCopies}",
                                previewPath = file.absolutePath
                            )
                        )
                        snackbar.showSnackbar("Saved project locally (${file.length() / 1024} KB).")
                    }
                },
                onSaveDownloads = {
                    scope.launch {
                        val file = withContext(Dispatchers.IO) { writeExportFile() }
                        if (file == null) {
                            snackbar.showSnackbar("Select a photo first.")
                            return@launch
                        }
                        val mime = if (file.name.endsWith(".jpg")) "image/jpeg" else "image/png"
                        val res = withContext(Dispatchers.IO) { PrintShareHelper.saveFileToDownloads(context, file, mime) }
                        snackbar.showSnackbar(res.getOrElse { it.message ?: "Export failed" } + " (${file.length() / 1024} KB)")
                    }
                },
                onShare = {
                    scope.launch {
                        val file = withContext(Dispatchers.IO) { writeExportFile() }
                        if (file == null) snackbar.showSnackbar("Select a photo first.")
                        else {
                            val mime = if (file.name.endsWith(".jpg")) "image/jpeg" else "image/png"
                            PrintShareHelper.shareFile(context, file, mime)
                        }
                    }
                },
                onPrint = {
                    val bmp = outputBitmap
                    if (bmp == null) {
                        scope.launch { snackbar.showSnackbar("Select a photo first.") }
                    } else {
                        PrintShareHelper.printBitmap(context, bmp, "${mode.title} - ${selectedPreset.title}")
                    }
                }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
