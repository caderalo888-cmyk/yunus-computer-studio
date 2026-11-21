package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.StudioProjectEntity
import com.example.engine.ImageEngine
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
import kotlin.math.roundToInt

// ---------------- MODULE 5: PHOTO RESIZE & HD ENHANCEMENT ----------------

@Composable
fun PhotoEnhanceScreen(
    viewModel: StudioViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var widthInput by remember { mutableStateOf("1080") }
    var heightInput by remember { mutableStateOf("1080") }
    var lockAspect by remember { mutableStateOf(true) }
    var brightness by remember { mutableFloatStateOf(0f) }
    var contrast by remember { mutableFloatStateOf(1.08f) }
    var saturation by remember { mutableFloatStateOf(1.05f) }
    var sharpness by remember { mutableFloatStateOf(0.35f) }
    var autoLevels by remember { mutableStateOf(true) }
    var selectedFormat by remember { mutableStateOf("JPEG") }
    var targetKbInput by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) { ImageEngine.decodeUri(context, uri, 2000) }
                if (bmp != null) {
                    sourceBitmap = bmp
                    widthInput = bmp.width.toString()
                    heightInput = bmp.height.toString()
                } else snackbar.showSnackbar("Failed to decode image.")
            }
        }
    }

    LaunchedEffect(sourceBitmap, brightness, contrast, saturation, sharpness, autoLevels) {
        val src = sourceBitmap ?: return@LaunchedEffect
        previewBitmap = withContext(Dispatchers.Default) {
            val previewScaled = if (src.width > 960) {
                ImageEngine.resizeBitmap(src, 960, (960f * src.height / src.width).roundToInt())
            } else src
            ImageEngine.enhanceBitmap(previewScaled, brightness, contrast, saturation, sharpness, autoLevels)
        }
    }

    suspend fun buildFullOutput(): File? = withContext(Dispatchers.Default) {
        val src = sourceBitmap ?: return@withContext null
        val tw = widthInput.toIntOrNull()?.coerceIn(32, 4096) ?: src.width
        val th = heightInput.toIntOrNull()?.coerceIn(32, 4096) ?: src.height
        val resized = if (tw != src.width || th != src.height) ImageEngine.resizeBitmap(src, tw, th) else src
        val enhanced = ImageEngine.enhanceBitmap(resized, brightness, contrast, saturation, sharpness, autoLevels)
        val ext = when (selectedFormat) {
            "PNG" -> "png"
            "WEBP" -> "webp"
            else -> "jpg"
        }
        val fmt = when (selectedFormat) {
            "PNG" -> Bitmap.CompressFormat.PNG
            "WEBP" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.JPEG
        }
        val file = File(viewModel.repository.getExportsDir(), "enhanced_${System.currentTimeMillis()}.$ext")
        ImageEngine.exportCompressedImage(
            bitmap = enhanced,
            outputFile = file,
            format = fmt,
            quality = 94,
            maxKb = targetKbInput.toIntOrNull()?.takeIf { it >= 10 }
        )
        file
    }

    StudioModuleScaffold(
        title = "Photo Resize & HD",
        subtitle = "Resize, Unsharp Mask, Auto-Levels & Target KB",
        accentColor = Color(0xFF10B981),
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
                modifier = Modifier.fillMaxWidth().testTag("pick_enhance_photo")
            ) {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (sourceBitmap == null) "Select Photo to Enhance" else "Change Photo")
            }

            CheckerboardBitmapPreview(
                bitmap = previewBitmap,
                placeholderText = "Select an image to resize & apply HD sharpening"
            )

            StudioSectionCard(
                title = "HD Enhancement Filters",
                trailing = {
                    OutlinedButton(onClick = {
                        brightness = 0f; contrast = 1.0f; saturation = 1.0f; sharpness = 0f; autoLevels = false
                    }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Reset")
                    }
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.AutoFixHigh, contentDescription = null, tint = Color(0xFF10B981))
                        Text("Auto-Levels Histogram Stretch")
                    }
                    Switch(checked = autoLevels, onCheckedChange = { autoLevels = it })
                }

                Text("HD Sharpness (Unsharp Mask): ${(sharpness * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                Slider(value = sharpness, onValueChange = { sharpness = it }, valueRange = 0f..1f)

                Text("Brightness: ${brightness.toInt()}", style = MaterialTheme.typography.bodySmall)
                Slider(value = brightness, onValueChange = { brightness = it }, valueRange = -60f..60f)

                Text("Contrast: ${String.format("%.2f", contrast)}×", style = MaterialTheme.typography.bodySmall)
                Slider(value = contrast, onValueChange = { contrast = it }, valueRange = 0.6f..1.8f)

                Text("Saturation: ${String.format("%.2f", saturation)}×", style = MaterialTheme.typography.bodySmall)
                Slider(value = saturation, onValueChange = { saturation = it }, valueRange = 0f..1.8f)
            }

            StudioSectionCard(title = "Resize & Export Format") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = widthInput,
                        onValueChange = { v ->
                            widthInput = v.filter { it.isDigit() }
                            val w = widthInput.toIntOrNull()
                            val src = sourceBitmap
                            if (lockAspect && w != null && src != null && src.width > 0) {
                                heightInput = ((w.toFloat() / src.width) * src.height).roundToInt().toString()
                            }
                        },
                        label = { Text("Width (px)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { v ->
                            heightInput = v.filter { it.isDigit() }
                            val h = heightInput.toIntOrNull()
                            val src = sourceBitmap
                            if (lockAspect && h != null && src != null && src.height > 0) {
                                widthInput = ((h.toFloat() / src.height) * src.width).roundToInt().toString()
                            }
                        },
                        label = { Text("Height (px)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Lock Aspect Ratio", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = lockAspect, onCheckedChange = { lockAspect = it })
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("JPEG", "PNG", "WEBP").forEach { fmt ->
                        FilterChip(
                            selected = selectedFormat == fmt,
                            onClick = { selectedFormat = fmt },
                            label = { Text(fmt) }
                        )
                    }
                }

                OutlinedTextField(
                    value = targetKbInput,
                    onValueChange = { targetKbInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Optional Max File Size (KB) e.g. 100") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            ExportActionBar(
                onSaveProject = {
                    scope.launch {
                        val file = buildFullOutput()
                        if (file == null) { snackbar.showSnackbar("Select a photo first."); return@launch }
                        viewModel.saveProject(
                            StudioProjectEntity(
                                moduleType = "PHOTO_ENHANCE",
                                title = "HD ${widthInput}×${heightInput} ($selectedFormat)",
                                payloadJson = "{\"w\":$widthInput,\"h\":$heightInput}",
                                previewPath = file.absolutePath
                            )
                        )
                        snackbar.showSnackbar("Saved locally (${file.length() / 1024} KB).")
                    }
                },
                onSaveDownloads = {
                    scope.launch {
                        val file = buildFullOutput()
                        if (file == null) { snackbar.showSnackbar("Select a photo first."); return@launch }
                        val mime = when (selectedFormat) { "PNG" -> "image/png"; "WEBP" -> "image/webp"; else -> "image/jpeg" }
                        val res = withContext(Dispatchers.IO) { PrintShareHelper.saveFileToDownloads(context, file, mime) }
                        snackbar.showSnackbar(res.getOrElse { it.message ?: "Error" } + " (${file.length() / 1024} KB)")
                    }
                },
                onShare = {
                    scope.launch {
                        val file = buildFullOutput()
                        if (file == null) { snackbar.showSnackbar("Select a photo first."); return@launch }
                        val mime = when (selectedFormat) { "PNG" -> "image/png"; "WEBP" -> "image/webp"; else -> "image/jpeg" }
                        PrintShareHelper.shareFile(context, file, mime)
                    }
                },
                onPrint = {
                    val bmp = previewBitmap
                    if (bmp == null) scope.launch { snackbar.showSnackbar("Select a photo first.") }
                    else PrintShareHelper.printBitmap(context, bmp, "Enhanced Photo")
                }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------------- MODULE 6: BACKGROUND REMOVAL ----------------

private enum class BgRemovalMode(val label: String) {
    AUTO_EDGE("Auto Edge Flood-Fill"),
    CHROMA_WHITE("Remove White Screen"),
    CHROMA_GREEN("Remove Green Screen"),
    OVAL_PORTRAIT("Portrait Oval Cutout"),
    TOUCH_ERASE("Touch Brush Eraser")
}

@Composable
fun BackgroundRemovalScreen(
    viewModel: StudioViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cutoutBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val undoStack = remember { mutableStateListOf<Bitmap>() }
    var mode by remember { mutableStateOf(BgRemovalMode.AUTO_EDGE) }
    var tolerance by remember { mutableFloatStateOf(38f) }
    var feather by remember { mutableFloatStateOf(2f) }
    var backdropColor1 by remember { mutableStateOf<Int?>(null) }
    var backdropColor2 by remember { mutableStateOf<Int?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) { ImageEngine.decodeUri(context, uri, 1200) }
                if (bmp != null) {
                    sourceBitmap = bmp
                    undoStack.clear()
                    cutoutBitmap = withContext(Dispatchers.Default) {
                        ImageEngine.removeBackgroundFloodFill(bmp, tolerance.toInt(), feather.toInt())
                    }
                } else snackbar.showSnackbar("Failed to load photo.")
            }
        }
    }

    fun processCutout(seedX: Int? = null, seedY: Int? = null) {
        val src = sourceBitmap ?: return
        scope.launch {
            cutoutBitmap?.let { undoStack.add(it); if (undoStack.size > 6) undoStack.removeAt(0) }
            cutoutBitmap = withContext(Dispatchers.Default) {
                when (mode) {
                    BgRemovalMode.AUTO_EDGE -> ImageEngine.removeBackgroundFloodFill(src, tolerance.toInt(), feather.toInt(), seedX, seedY)
                    BgRemovalMode.CHROMA_WHITE -> ImageEngine.removeChromaColor(src, AndroidColor.WHITE, tolerance.toInt())
                    BgRemovalMode.CHROMA_GREEN -> ImageEngine.removeChromaColor(src, AndroidColor.rgb(0, 185, 65), tolerance.toInt() + 18)
                    BgRemovalMode.OVAL_PORTRAIT -> ImageEngine.applyPortraitOvalMask(src)
                    BgRemovalMode.TOUCH_ERASE -> cutoutBitmap ?: src.copy(Bitmap.Config.ARGB_8888, true)
                }
            }
        }
    }

    val displayBitmap = remember(cutoutBitmap, backdropColor1, backdropColor2) {
        cutoutBitmap?.let { ImageEngine.compositeWithBackdrop(it, backdropColor1, backdropColor2) }
    }

    suspend fun writeCutoutFile(): File? = withContext(Dispatchers.IO) {
        val bmp = displayBitmap ?: return@withContext null
        val file = File(viewModel.repository.getExportsDir(), "bg_removed_${System.currentTimeMillis()}.png")
        ImageEngine.exportCompressedImage(bmp, file, Bitmap.CompressFormat.PNG, 100)
        file
    }

    StudioModuleScaffold(
        title = "Background Removal",
        subtitle = "Flood-Fill, Chroma Key, Brush & Studio Backdrops",
        accentColor = Color(0xFFEC4899),
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
                modifier = Modifier.fillMaxWidth().testTag("pick_bg_remove_photo")
            ) {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (sourceBitmap == null) "Select Photo for Cutout" else "Change Photo")
            }

            // Interactive Touch-Capable Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                    .pointerInput(mode, cutoutBitmap) {
                        detectTapGestures { offset ->
                            val cur = cutoutBitmap ?: return@detectTapGestures
                            val nx = (offset.x / size.width).coerceIn(0f, 1f)
                            val ny = (offset.y / size.height).coerceIn(0f, 1f)
                            if (mode == BgRemovalMode.TOUCH_ERASE) {
                                undoStack.add(cur)
                                if (undoStack.size > 8) undoStack.removeAt(0)
                                cutoutBitmap = ImageEngine.eraseBrushAt(cur, nx, ny)
                            } else if (mode == BgRemovalMode.AUTO_EDGE) {
                                val src = sourceBitmap ?: return@detectTapGestures
                                processCutout(
                                    seedX = (nx * src.width).toInt(),
                                    seedY = (ny * src.height).toInt()
                                )
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (displayBitmap != null) {
                    Image(
                        bitmap = displayBitmap.asImageBitmap(),
                        contentDescription = "Cutout Preview",
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        "Pick an image • Tap preview in Brush/Edge mode to refine",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            StudioSectionCard(
                title = "Cutout Mode & Sensitivity",
                trailing = {
                    if (undoStack.isNotEmpty()) {
                        OutlinedButton(onClick = {
                            cutoutBitmap = undoStack.removeAt(undoStack.lastIndex)
                        }) {
                            Icon(Icons.Rounded.Undo, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Undo")
                        }
                    }
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BgRemovalMode.entries.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { m ->
                                FilterChip(
                                    selected = mode == m,
                                    onClick = {
                                        mode = m
                                        if (m != BgRemovalMode.TOUCH_ERASE) processCutout()
                                    },
                                    label = { Text(m.label) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                Text("Color Tolerance: ${tolerance.toInt()}", style = MaterialTheme.typography.bodySmall)
                Slider(value = tolerance, onValueChange = { tolerance = it }, valueRange = 10f..95f)

                Text("Edge Feathering: ${feather.toInt()} px", style = MaterialTheme.typography.bodySmall)
                Slider(value = feather, onValueChange = { feather = it }, valueRange = 0f..5f)

                Button(
                    onClick = { processCutout() },
                    enabled = sourceBitmap != null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply Cutout Processing")
                }
            }

            StudioSectionCard(title = "Replacement Backdrop") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = backdropColor1 == null,
                        onClick = { backdropColor1 = null; backdropColor2 = null },
                        label = { Text("Transparent PNG") }
                    )
                    FilterChip(
                        selected = backdropColor2 != null,
                        onClick = {
                            backdropColor1 = AndroidColor.rgb(15, 23, 42)
                            backdropColor2 = AndroidColor.rgb(13, 148, 136)
                        },
                        label = { Text("Teal Studio Gradient") }
                    )
                }
                ColorSwatchPicker(
                    colors = listOf(
                        "White" to AndroidColor.WHITE,
                        "Blue" to AndroidColor.rgb(29, 78, 216),
                        "Sky" to AndroidColor.rgb(56, 189, 248),
                        "Crimson" to AndroidColor.rgb(185, 28, 28),
                        "DarkSlate" to AndroidColor.rgb(15, 23, 42)
                    ),
                    selectedColor = backdropColor1 ?: -1,
                    onSelect = { backdropColor1 = it; backdropColor2 = null }
                )
            }

            ExportActionBar(
                onSaveProject = {
                    scope.launch {
                        val file = writeCutoutFile()
                        if (file == null) { snackbar.showSnackbar("Select a photo first."); return@launch }
                        viewModel.saveProject(
                            StudioProjectEntity(
                                moduleType = "BG_REMOVAL",
                                title = "Cutout (${mode.label})",
                                payloadJson = "{\"mode\":\"${mode.name}\"}",
                                previewPath = file.absolutePath
                            )
                        )
                        snackbar.showSnackbar("Saved PNG cutout locally.")
                    }
                },
                onSaveDownloads = {
                    scope.launch {
                        val file = writeCutoutFile()
                        if (file == null) { snackbar.showSnackbar("Select a photo first."); return@launch }
                        val res = PrintShareHelper.saveFileToDownloads(context, file, "image/png")
                        snackbar.showSnackbar(res.getOrElse { it.message ?: "Error" })
                    }
                },
                onShare = {
                    scope.launch {
                        val file = writeCutoutFile()
                        if (file == null) snackbar.showSnackbar("Select a photo first.")
                        else PrintShareHelper.shareFile(context, file, "image/png")
                    }
                },
                onPrint = {
                    val bmp = displayBitmap
                    if (bmp == null) scope.launch { snackbar.showSnackbar("Select a photo first.") }
                    else PrintShareHelper.printBitmap(context, bmp, "Background Cutout")
                }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
