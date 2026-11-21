package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.example.engine.CanvasPreset
import com.example.engine.DesignCanvasRenderer
import com.example.engine.DesignLayer
import com.example.engine.DesignLayerKind
import com.example.engine.GraphicProjectCodec
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

@Composable
fun GraphicDesignStudioScreen(
    viewModel: StudioViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var preset by remember { mutableStateOf(CanvasPreset.SQUARE_POST) }
    var bg1 by remember { mutableIntStateOf(AndroidColor.rgb(15, 23, 42)) }
    var bg2 by remember { mutableStateOf<Int?>(AndroidColor.rgb(13, 148, 136)) }
    var nextId by remember { mutableIntStateOf(10) }

    val layers = remember {
        mutableStateListOf(
            DesignLayer(1L, DesignLayerKind.RECT, "", 0.5f, 0.5f, 0.86f, 0.86f, colorHex = AndroidColor.argb(40, 255, 255, 255)),
            DesignLayer(2L, DesignLayerKind.BADGE, "SPECIAL STUDIO OFFER", 0.5f, 0.22f, 0.55f, 0.10f, fontSizeSp = 22f, colorHex = AndroidColor.rgb(245, 158, 11)),
            DesignLayer(3L, DesignLayerKind.TEXT, "YUNUS COMPUTER\nSTUDIO PRO", 0.5f, 0.48f, 0.8f, 0.25f, fontSizeSp = 44f, colorHex = AndroidColor.WHITE, bold = true),
            DesignLayer(4L, DesignLayerKind.LINE, "", 0.5f, 0.68f, 0.55f, 0.02f, colorHex = AndroidColor.rgb(45, 212, 191)),
            DesignLayer(5L, DesignLayerKind.TEXT, "Photo • Print • Design • Digital Service", 0.5f, 0.77f, 0.8f, 0.12f, fontSizeSp = 22f, colorHex = AndroidColor.rgb(226, 232, 240), bold = false)
        )
    }
    var selectedLayerId by remember { mutableStateOf(3L) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val selectedIndex = layers.indexOfFirst { it.id == selectedLayerId }
    val selectedLayer = layers.getOrNull(selectedIndex)

    LaunchedEffect(preset, bg1, bg2, layers.toList()) {
        previewBitmap = withContext(Dispatchers.Default) {
            DesignCanvasRenderer.renderGraphicStudio(preset, bg1, bg2, layers.toList())
        }
    }

    suspend fun writePng(): File? = withContext(Dispatchers.IO) {
        val bmp = previewBitmap ?: return@withContext null
        val file = File(viewModel.repository.getExportsDir(), "graphic_${System.currentTimeMillis()}.png")
        ImageEngine.exportCompressedImage(bmp, file, Bitmap.CompressFormat.PNG, 100)
        file
    }

    StudioModuleScaffold(
        title = "Graphic Design Studio",
        subtitle = "Layered Poster, Banner & Social Flyer Editor",
        accentColor = Color(0xFF8B5CF6),
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
            CheckerboardBitmapPreview(bitmap = previewBitmap, placeholderText = "Rendering canvas…")

            StudioSectionCard(title = "Canvas Size & Background") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CanvasPreset.entries) { p ->
                        FilterChip(selected = preset == p, onClick = { preset = p }, label = { Text(p.label) })
                    }
                }
                ColorSwatchPicker(
                    colors = listOf(
                        "Slate" to AndroidColor.rgb(15, 23, 42),
                        "Teal" to AndroidColor.rgb(13, 148, 136),
                        "Indigo" to AndroidColor.rgb(67, 56, 202),
                        "Crimson" to AndroidColor.rgb(153, 27, 27),
                        "White" to AndroidColor.WHITE
                    ),
                    selectedColor = bg1,
                    onSelect = { bg1 = it }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dual-Tone Gradient Backdrop")
                    Switch(
                        checked = bg2 != null,
                        onCheckedChange = { on -> bg2 = if (on) AndroidColor.rgb(13, 148, 136) else null }
                    )
                }
            }

            StudioSectionCard(title = "Layers (${layers.size})") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = {
                        val id = nextId++.toLong()
                        layers.add(DesignLayer(id, DesignLayerKind.TEXT, "New Heading", 0.5f, 0.5f, fontSizeSp = 36f, colorHex = AndroidColor.WHITE))
                        selectedLayerId = id
                    }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Text("Text")
                    }
                    OutlinedButton(onClick = {
                        val id = nextId++.toLong()
                        layers.add(DesignLayer(id, DesignLayerKind.BADGE, "NEW BADGE", 0.5f, 0.35f, 0.48f, 0.10f, fontSizeSp = 22f, colorHex = AndroidColor.rgb(245, 158, 11)))
                        selectedLayerId = id
                    }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Text("Badge")
                    }
                    OutlinedButton(onClick = {
                        val id = nextId++.toLong()
                        layers.add(DesignLayer(id, DesignLayerKind.CIRCLE, "", 0.5f, 0.5f, 0.25f, 0.25f, colorHex = AndroidColor.argb(110, 45, 212, 191)))
                        selectedLayerId = id
                    }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Text("Circle")
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(layers) { layer ->
                        FilterChip(
                            selected = layer.id == selectedLayerId,
                            onClick = { selectedLayerId = layer.id },
                            label = {
                                Text(
                                    when (layer.kind) {
                                        DesignLayerKind.TEXT, DesignLayerKind.BADGE -> layer.text.take(12).ifBlank { layer.kind.name }
                                        else -> layer.kind.name
                                    }
                                )
                            }
                        )
                    }
                }

                if (selectedLayer != null && selectedIndex >= 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Edit Selected: ${selectedLayer.kind.name}", style = MaterialTheme.typography.titleSmall)
                        IconButton(onClick = {
                            layers.removeAt(selectedIndex)
                            selectedLayerId = layers.lastOrNull()?.id ?: -1L
                        }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete Layer", tint = Color(0xFFF43F5E))
                        }
                    }
                    if (selectedLayer.kind == DesignLayerKind.TEXT || selectedLayer.kind == DesignLayerKind.BADGE) {
                        OutlinedTextField(
                            value = selectedLayer.text,
                            onValueChange = { txt -> layers[selectedIndex] = selectedLayer.copy(text = txt) },
                            label = { Text("Layer Text") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Font Size: ${selectedLayer.fontSizeSp.toInt()}", style = MaterialTheme.typography.bodySmall)
                        Slider(
                            value = selectedLayer.fontSizeSp,
                            onValueChange = { layers[selectedIndex] = selectedLayer.copy(fontSizeSp = it) },
                            valueRange = 14f..72f
                        )
                    }
                    Text("Position X / Y", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Slider(
                            value = selectedLayer.xPercent,
                            onValueChange = { layers[selectedIndex] = selectedLayer.copy(xPercent = it) },
                            valueRange = 0.05f..0.95f,
                            modifier = Modifier.weight(1f)
                        )
                        Slider(
                            value = selectedLayer.yPercent,
                            onValueChange = { layers[selectedIndex] = selectedLayer.copy(yPercent = it) },
                            valueRange = 0.05f..0.95f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text("Rotation: ${selectedLayer.rotationDeg.toInt()}°", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = selectedLayer.rotationDeg,
                        onValueChange = { layers[selectedIndex] = selectedLayer.copy(rotationDeg = it) },
                        valueRange = -180f..180f
                    )
                    ColorSwatchPicker(
                        colors = listOf(
                            "White" to AndroidColor.WHITE,
                            "Amber" to AndroidColor.rgb(245, 158, 11),
                            "Teal" to AndroidColor.rgb(45, 212, 191),
                            "Rose" to AndroidColor.rgb(244, 63, 94),
                            "Dark" to AndroidColor.rgb(15, 23, 42)
                        ),
                        selectedColor = selectedLayer.colorHex,
                        onSelect = { c -> layers[selectedIndex] = selectedLayer.copy(colorHex = c) }
                    )
                }
            }

            ExportActionBar(
                onSaveProject = {
                    scope.launch {
                        val file = writePng()
                        val json = GraphicProjectCodec.encode(preset, bg1, bg2, layers.toList())
                        viewModel.saveProject(
                            StudioProjectEntity(
                                moduleType = "GRAPHIC_DESIGN",
                                title = "Graphic (${preset.label})",
                                payloadJson = json,
                                previewPath = file?.absolutePath ?: ""
                            )
                        )
                        snackbar.showSnackbar("Graphic design project saved locally.")
                    }
                },
                onSaveDownloads = {
                    scope.launch {
                        val file = writePng() ?: return@launch
                        val res = PrintShareHelper.saveFileToDownloads(context, file, "image/png")
                        snackbar.showSnackbar(res.getOrElse { it.message ?: "Error" })
                    }
                },
                onShare = {
                    scope.launch {
                        val file = writePng() ?: return@launch
                        PrintShareHelper.shareFile(context, file, "image/png")
                    }
                },
                onPrint = {
                    previewBitmap?.let { PrintShareHelper.printBitmap(context, it, "Graphic Design") }
                }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
