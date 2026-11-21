package com.example.ui.screens

import android.graphics.Bitmap
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
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.engine.CvData
import com.example.engine.CvTemplate
import com.example.engine.DesignCanvasRenderer
import com.example.engine.ElectionPosterData
import com.example.engine.ElectionSymbol
import com.example.engine.ElectionTheme
import com.example.engine.ImageEngine
import com.example.engine.PrintShareHelper
import com.example.engine.VisitingCardData
import com.example.engine.VisitingCardStyle
import com.example.engine.WeddingCardData
import com.example.engine.WeddingTheme
import com.example.studio.StudioViewModel
import com.example.ui.components.CheckerboardBitmapPreview
import com.example.ui.components.ExportActionBar
import com.example.ui.components.StudioModuleScaffold
import com.example.ui.components.StudioSectionCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ---------------- MODULE 10: WEDDING CARD DESIGNER ----------------

@Composable
fun WeddingCardScreen(
    viewModel: StudioViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var data by remember { mutableStateOf(WeddingCardData()) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(data) {
        preview = withContext(Dispatchers.Default) { DesignCanvasRenderer.renderWeddingCard(data) }
    }

    suspend fun writeFile(): File? = withContext(Dispatchers.IO) {
        val bmp = preview ?: return@withContext null
        val f = File(viewModel.repository.getExportsDir(), "wedding_card_${System.currentTimeMillis()}.png")
        ImageEngine.exportCompressedImage(bmp, f, Bitmap.CompressFormat.PNG, 100)
        f
    }

    StudioModuleScaffold(
        title = "Wedding Card Designer",
        subtitle = "Royal Gold, Emerald & Ivory Invitations",
        accentColor = Color(0xFFF59E0B),
        onBack = onBack,
        snackbarHostState = snackbar
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CheckerboardBitmapPreview(bitmap = preview, placeholderText = "Rendering card…", modifier = Modifier.height(310.dp))

            StudioSectionCard(title = "Card Theme & Details") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(WeddingTheme.entries) { t ->
                        FilterChip(selected = data.theme == t, onClick = { data = data.copy(theme = t) }, label = { Text(t.label) })
                    }
                }
                OutlinedTextField(value = data.headerTitle, onValueChange = { data = data.copy(headerTitle = it) }, label = { Text("Top Blessing / Header") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.occasionTitle, onValueChange = { data = data.copy(occasionTitle = it) }, label = { Text("Occasion Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.groomName, onValueChange = { data = data.copy(groomName = it) }, label = { Text("Groom Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.groomParents, onValueChange = { data = data.copy(groomParents = it) }, label = { Text("Groom Parents") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.brideName, onValueChange = { data = data.copy(brideName = it) }, label = { Text("Bride Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.brideParents, onValueChange = { data = data.copy(brideParents = it) }, label = { Text("Bride Parents") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.dateLine, onValueChange = { data = data.copy(dateLine = it) }, label = { Text("Date & Time") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.venueLine, onValueChange = { data = data.copy(venueLine = it) }, label = { Text("Venue Address") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.rsvpLine, onValueChange = { data = data.copy(rsvpLine = it) }, label = { Text("RSVP / Footer") }, modifier = Modifier.fillMaxWidth())
            }

            ExportActionBar(
                onSaveProject = {
                    scope.launch {
                        val file = writeFile()
                        viewModel.saveProject(
                            StudioProjectEntity(
                                moduleType = "WEDDING_CARD",
                                title = "${data.groomName} & ${data.brideName}",
                                payloadJson = "{\"theme\":\"${data.theme.name}\"}",
                                previewPath = file?.absolutePath ?: ""
                            )
                        )
                        snackbar.showSnackbar("Wedding card project saved.")
                    }
                },
                onSaveDownloads = {
                    scope.launch {
                        val f = writeFile() ?: return@launch
                        val res = PrintShareHelper.saveFileToDownloads(context, f, "image/png")
                        snackbar.showSnackbar(res.getOrElse { it.message ?: "Error" })
                    }
                },
                onShare = { scope.launch { writeFile()?.let { PrintShareHelper.shareFile(context, it, "image/png") } } },
                onPrint = { preview?.let { PrintShareHelper.printBitmap(context, it, "Wedding Card") } }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------------- MODULE 11: ELECTION POSTER DESIGNER ----------------

@Composable
fun ElectionPosterScreen(
    viewModel: StudioViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var data by remember { mutableStateOf(ElectionPosterData()) }
    var candidatePhoto by remember { mutableStateOf<Bitmap?>(null) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                candidatePhoto = withContext(Dispatchers.IO) { ImageEngine.decodeUri(context, uri, 900) }
            }
        }
    }

    LaunchedEffect(data, candidatePhoto) {
        preview = withContext(Dispatchers.Default) { DesignCanvasRenderer.renderElectionPoster(data, candidatePhoto) }
    }

    suspend fun writeFile(): File? = withContext(Dispatchers.IO) {
        val bmp = preview ?: return@withContext null
        val f = File(viewModel.repository.getExportsDir(), "election_poster_${System.currentTimeMillis()}.png")
        ImageEngine.exportCompressedImage(bmp, f, Bitmap.CompressFormat.PNG, 100)
        f
    }

    StudioModuleScaffold(
        title = "Election Poster Designer",
        subtitle = "Candidate Portrait, Symbol & Manifesto Poster",
        accentColor = Color(0xFFF97316),
        onBack = onBack,
        snackbarHostState = snackbar
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CheckerboardBitmapPreview(bitmap = preview, placeholderText = "Rendering poster…", modifier = Modifier.height(310.dp))

            Button(
                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (candidatePhoto == null) "Select Candidate Portrait" else "Change Candidate Photo")
            }

            StudioSectionCard(title = "Poster Palette & Election Symbol") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ElectionTheme.entries) { t ->
                        FilterChip(selected = data.theme == t, onClick = { data = data.copy(theme = t) }, label = { Text(t.label) })
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ElectionSymbol.entries) { s ->
                        FilterChip(selected = data.symbol == s, onClick = { data = data.copy(symbol = s) }, label = { Text(s.label) })
                    }
                }
                OutlinedTextField(value = data.topBanner, onValueChange = { data = data.copy(topBanner = it) }, label = { Text("Top Banner") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.candidateName, onValueChange = { data = data.copy(candidateName = it) }, label = { Text("Candidate Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.designation, onValueChange = { data = data.copy(designation = it) }, label = { Text("Position / Post") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.constituency, onValueChange = { data = data.copy(constituency = it) }, label = { Text("Ward / Constituency") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.slogan, onValueChange = { data = data.copy(slogan = it) }, label = { Text("Campaign Slogan") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.manifesto1, onValueChange = { data = data.copy(manifesto1 = it) }, label = { Text("Pledge 1") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.manifesto2, onValueChange = { data = data.copy(manifesto2 = it) }, label = { Text("Pledge 2") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.manifesto3, onValueChange = { data = data.copy(manifesto3 = it) }, label = { Text("Pledge 3") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.footerCredit, onValueChange = { data = data.copy(footerCredit = it) }, label = { Text("Campaign Committee Footer") }, modifier = Modifier.fillMaxWidth())
            }

            ExportActionBar(
                onSaveProject = {
                    scope.launch {
                        val file = writeFile()
                        viewModel.saveProject(
                            StudioProjectEntity(
                                moduleType = "ELECTION_POSTER",
                                title = "${data.candidateName} (${data.symbol.label})",
                                payloadJson = "{\"symbol\":\"${data.symbol.name}\"}",
                                previewPath = file?.absolutePath ?: ""
                            )
                        )
                        snackbar.showSnackbar("Election poster saved.")
                    }
                },
                onSaveDownloads = {
                    scope.launch {
                        val f = writeFile() ?: return@launch
                        val res = PrintShareHelper.saveFileToDownloads(context, f, "image/png")
                        snackbar.showSnackbar(res.getOrElse { it.message ?: "Error" })
                    }
                },
                onShare = { scope.launch { writeFile()?.let { PrintShareHelper.shareFile(context, it, "image/png") } } },
                onPrint = { preview?.let { PrintShareHelper.printBitmap(context, it, "Election Poster") } }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------------- MODULE 12: VISITING CARD DESIGNER ----------------

@Composable
fun VisitingCardScreen(
    viewModel: StudioViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var data by remember { mutableStateOf(VisitingCardData()) }
    var sheetMode10Up by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(data, sheetMode10Up) {
        preview = withContext(Dispatchers.Default) {
            val single = DesignCanvasRenderer.renderVisitingCard(data)
            if (sheetMode10Up) ImageEngine.composePrintSheet(single, copies = 10, isA4Sheet = true)
            else single
        }
    }

    suspend fun writeFile(): File? = withContext(Dispatchers.IO) {
        val bmp = preview ?: return@withContext null
        val f = File(viewModel.repository.getExportsDir(), "visiting_card_${System.currentTimeMillis()}.png")
        ImageEngine.exportCompressedImage(bmp, f, Bitmap.CompressFormat.PNG, 100)
        f
    }

    StudioModuleScaffold(
        title = "Visiting Card Designer",
        subtitle = "3.5×2\" Cards with Embedded vCard QR & 10-Up Sheet",
        accentColor = Color(0xFF6366F1),
        onBack = onBack,
        snackbarHostState = snackbar
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CheckerboardBitmapPreview(bitmap = preview, placeholderText = "Rendering card…")

            StudioSectionCard(title = "Card Style & Business Info") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(VisitingCardStyle.entries) { s ->
                        FilterChip(selected = data.style == s, onClick = { data = data.copy(style = s) }, label = { Text(s.label) })
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Embed Real vCard QR Code")
                    Switch(checked = data.includeQr, onCheckedChange = { data = data.copy(includeQr = it) })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("10-Up A4 Print Sheet View")
                    Switch(checked = sheetMode10Up, onCheckedChange = { sheetMode10Up = it })
                }
                OutlinedTextField(value = data.companyName, onValueChange = { data = data.copy(companyName = it) }, label = { Text("Company / Studio Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.tagline, onValueChange = { data = data.copy(tagline = it) }, label = { Text("Tagline") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.personName, onValueChange = { data = data.copy(personName = it) }, label = { Text("Person Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.jobTitle, onValueChange = { data = data.copy(jobTitle = it) }, label = { Text("Designation") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.phone, onValueChange = { data = data.copy(phone = it) }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.email, onValueChange = { data = data.copy(email = it) }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.website, onValueChange = { data = data.copy(website = it) }, label = { Text("Website") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.address, onValueChange = { data = data.copy(address = it) }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
            }

            ExportActionBar(
                onSaveProject = {
                    scope.launch {
                        val file = writeFile()
                        viewModel.saveProject(
                            StudioProjectEntity(
                                moduleType = "VISITING_CARD",
                                title = "${data.personName} - ${data.companyName}",
                                payloadJson = "{\"style\":\"${data.style.name}\"}",
                                previewPath = file?.absolutePath ?: ""
                            )
                        )
                        snackbar.showSnackbar("Visiting card saved.")
                    }
                },
                onSaveDownloads = {
                    scope.launch {
                        val f = writeFile() ?: return@launch
                        val res = PrintShareHelper.saveFileToDownloads(context, f, "image/png")
                        snackbar.showSnackbar(res.getOrElse { it.message ?: "Error" })
                    }
                },
                onShare = { scope.launch { writeFile()?.let { PrintShareHelper.shareFile(context, it, "image/png") } } },
                onPrint = { preview?.let { PrintShareHelper.printBitmap(context, it, "Visiting Card") } }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------------- MODULE 13: CV MAKER ----------------

@Composable
fun CvMakerScreen(
    viewModel: StudioViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var data by remember { mutableStateOf(CvData()) }
    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var preview by remember { mutableStateOf<Bitmap?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch { photo = withContext(Dispatchers.IO) { ImageEngine.decodeUri(context, uri, 800) } }
        }
    }

    LaunchedEffect(data, photo) {
        preview = withContext(Dispatchers.Default) {
            DesignCanvasRenderer.renderCvToBitmapAndPdf(data, photo, pdfOutFile = null)
        }
    }

    suspend fun generateCvPdf(): File = withContext(Dispatchers.IO) {
        val pdfFile = File(viewModel.repository.getExportsDir(), "CV_${data.fullName.replace(" ", "_")}_${System.currentTimeMillis()}.pdf")
        DesignCanvasRenderer.renderCvToBitmapAndPdf(data, photo, pdfOutFile = pdfFile)
        pdfFile
    }

    StudioModuleScaffold(
        title = "CV Maker",
        subtitle = "3 Professional A4 Resume Templates & Instant PDF",
        accentColor = Color(0xFF14B8A6),
        onBack = onBack,
        snackbarHostState = snackbar
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CheckerboardBitmapPreview(bitmap = preview, placeholderText = "Rendering A4 CV…", modifier = Modifier.height(310.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (photo == null) "Add CV Photo" else "Change Photo")
                }
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val pdf = generateCvPdf()
                            val res = withContext(Dispatchers.IO) {
                                PrintShareHelper.saveFileToDownloads(context, pdf, "application/pdf")
                            }
                            snackbar.showSnackbar(res.getOrElse { it.message ?: "Error" })
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Export PDF")
                }
            }

            StudioSectionCard(title = "Template & Resume Fields") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CvTemplate.entries) { t ->
                        FilterChip(selected = data.template == t, onClick = { data = data.copy(template = t) }, label = { Text(t.label) })
                    }
                }
                OutlinedTextField(value = data.fullName, onValueChange = { data = data.copy(fullName = it) }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.roleTitle, onValueChange = { data = data.copy(roleTitle = it) }, label = { Text("Target Role / Title") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = data.phone, onValueChange = { data = data.copy(phone = it) }, label = { Text("Phone") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = data.email, onValueChange = { data = data.copy(email = it) }, label = { Text("Email") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = data.address, onValueChange = { data = data.copy(address = it) }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.summary, onValueChange = { data = data.copy(summary = it) }, label = { Text("Profile Summary") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.experience, onValueChange = { data = data.copy(experience = it) }, label = { Text("Work Experience") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.education, onValueChange = { data = data.copy(education = it) }, label = { Text("Education") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.skills, onValueChange = { data = data.copy(skills = it) }, label = { Text("Skills (comma-separated)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.languages, onValueChange = { data = data.copy(languages = it) }, label = { Text("Languages") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = data.reference, onValueChange = { data = data.copy(reference = it) }, label = { Text("References") }, modifier = Modifier.fillMaxWidth())
            }

            ExportActionBar(
                onSaveProject = {
                    scope.launch {
                        val pdf = generateCvPdf()
                        viewModel.saveProject(
                            StudioProjectEntity(
                                moduleType = "CV_MAKER",
                                title = "CV - ${data.fullName}",
                                payloadJson = "{\"template\":\"${data.template.name}\"}",
                                previewPath = pdf.absolutePath
                            )
                        )
                        snackbar.showSnackbar("CV saved locally as PDF project.")
                    }
                },
                onSaveDownloads = {
                    scope.launch {
                        val pdf = generateCvPdf()
                        val res = PrintShareHelper.saveFileToDownloads(context, pdf, "application/pdf")
                        snackbar.showSnackbar(res.getOrElse { it.message ?: "Error" })
                    }
                },
                onShare = {
                    scope.launch {
                        val pdf = generateCvPdf()
                        PrintShareHelper.shareFile(context, pdf, "application/pdf")
                    }
                },
                onPrint = {
                    scope.launch {
                        val pdf = generateCvPdf()
                        PrintShareHelper.printPdfFile(context, pdf, "CV - ${data.fullName}")
                    }
                }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
