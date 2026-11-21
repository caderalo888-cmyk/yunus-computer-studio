package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.CustomerEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.StudioProjectEntity
import com.example.data.local.TransactionEntity
import com.example.engine.PrintShareHelper
import com.example.studio.ModuleCategory
import com.example.studio.StudioCatalog
import com.example.studio.StudioModuleItem
import com.example.studio.StudioRoutes
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioRose
import com.example.ui.theme.StudioTeal
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardTabScreen(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    customers: List<CustomerEntity>,
    invoices: List<InvoiceEntity>,
    transactions: List<TransactionEntity>,
    projects: List<StudioProjectEntity>,
    onOpenModule: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ModuleCategory.ALL) }

    val toolModules = remember { StudioCatalog.modules.filter { it.route != StudioRoutes.DASHBOARD } }
    val filteredModules = remember(searchQuery, selectedCategory) {
        toolModules.filter { m ->
            val catMatch = selectedCategory == ModuleCategory.ALL || m.category == selectedCategory
            val q = searchQuery.trim().lowercase()
            val textMatch = q.isEmpty() ||
                m.title.lowercase().contains(q) ||
                m.subtitle.lowercase().contains(q) ||
                m.number.toString() == q
            catMatch && textMatch
        }
    }

    val totalIncome = remember(transactions) {
        transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = if (maxWidth > 720.dp) 3 else 2

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Header Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Box(modifier = Modifier.height(176.dp).fillMaxWidth()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_studio_hero),
                            contentDescription = "Yunus Computer Studio Pro Hero Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xBB090E1A),
                                            Color(0xEE0F172A)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = StudioTeal.copy(alpha = 0.20f),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, StudioTeal.copy(alpha = 0.55f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.VerifiedUser,
                                            contentDescription = null,
                                            tint = StudioTeal,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "OFFLINE STUDIO • 21 MODULES",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = onToggleTheme,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .testTag("theme_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                        contentDescription = "Toggle Dark or Light Theme",
                                        tint = StudioAmber
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Yunus Computer Studio Pro",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Photo ID • PDF & Office • Design & Print • Bangla TTS & Audio • Business",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1),
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }

            // Live Studio KPI Strip
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DashboardKpiPill(
                        label = "Clients",
                        value = customers.size.toString(),
                        accent = Color(0xFF38BDF8),
                        modifier = Modifier.weight(1f).clickable { onOpenModule(StudioRoutes.CUSTOMERS) }
                    )
                    DashboardKpiPill(
                        label = "Invoices",
                        value = invoices.size.toString(),
                        accent = StudioAmber,
                        modifier = Modifier.weight(1f).clickable { onOpenModule(StudioRoutes.BILLING) }
                    )
                    DashboardKpiPill(
                        label = "Net",
                        value = "৳${(totalIncome - totalExpense).toInt()}",
                        accent = if (totalIncome >= totalExpense) StudioEmerald else StudioRose,
                        modifier = Modifier.weight(1f).clickable { onOpenModule(StudioRoutes.LEDGER) }
                    )
                    DashboardKpiPill(
                        label = "Saved",
                        value = projects.size.toString(),
                        accent = StudioTeal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Search Field
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search 21 tools (e.g. Passport, PDF, Bangla, QR, CV)…") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_search_input")
                )
            }

            // Category Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ModuleCategory.entries) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category.label) },
                            modifier = Modifier.testTag("category_chip_${category.name}")
                        )
                    }
                }
            }

            // Module Grid Rows
            val chunked = filteredModules.chunked(columns)
            items(chunked) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { module ->
                        ModuleLauncherCard(
                            module = module,
                            onClick = { onOpenModule(module.route) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(columns - rowItems.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun DashboardKpiPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ModuleLauncherCard(
    module: StudioModuleItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(142.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("module_card_${module.route}"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(module.accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = module.icon,
                        contentDescription = module.title,
                        tint = module.accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "#${module.number.toString().padStart(2, '0')}",
                        fontFamily = MonoFontFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
            Column {
                Text(
                    text = module.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = module.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SavedProjectsTabScreen(
    projects: List<StudioProjectEntity>,
    onDeleteProject: (StudioProjectEntity) -> Unit,
    onOpenModule: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fmt = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Saved Studio Projects & Exports", style = MaterialTheme.typography.headlineSmall)
            Text(
                "All projects and outputs are stored locally on your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (projects.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = StudioTeal
                        )
                        Text("No Saved Projects Yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Tap 'Save' inside any photo, card, poster, QR, or CV studio tool to keep local project records here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(projects, key = { it.id }) { proj ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                color = StudioTeal.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = proj.moduleType,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = StudioTeal,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(proj.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                fmt.format(Date(proj.updatedAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row {
                            if (proj.previewPath.isNotBlank() && File(proj.previewPath).exists()) {
                                IconButton(onClick = {
                                    val f = File(proj.previewPath)
                                    val mime = when {
                                        f.name.endsWith(".pdf") -> "application/pdf"
                                        f.name.endsWith(".wav") -> "audio/wav"
                                        else -> "image/png"
                                    }
                                    PrintShareHelper.shareFile(context, f, mime)
                                }) {
                                    Icon(Icons.Rounded.Share, contentDescription = "Share Saved Export")
                                }
                            }
                            IconButton(onClick = { onDeleteProject(proj) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete Project", tint = StudioRose)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BusinessHubTabScreen(
    customersCount: Int,
    invoicesCount: Int,
    netBalance: Double,
    onOpenModule: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Studio Business Suite", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Manage walk-in studio clients, itemized receipts, and daily income/expense ledger.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            BusinessShortcutCard(
                title = "19. Customer Management",
                subtitle = "$customersCount saved customers • Phone, address & order tracking",
                icon = Icons.Rounded.Groups,
                accent = Color(0xFF3B82F6),
                onClick = { onOpenModule(StudioRoutes.CUSTOMERS) }
            )
        }
        item {
            BusinessShortcutCard(
                title = "20. Billing & Receipt",
                subtitle = "$invoicesCount generated invoices • Quick presets & printable PDF receipts",
                icon = Icons.Rounded.ReceiptLong,
                accent = StudioAmber,
                onClick = { onOpenModule(StudioRoutes.BILLING) }
            )
        }
        item {
            BusinessShortcutCard(
                title = "21. Income & Expense Management",
                subtitle = "Net Balance: ৳${String.format("%.2f", netBalance)} • Ledger analytics & statement export",
                icon = Icons.Rounded.AccountBalanceWallet,
                accent = StudioEmerald,
                onClick = { onOpenModule(StudioRoutes.LEDGER) }
            )
        }
    }
}

@Composable
private fun BusinessShortcutCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun StudioInfoTabScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Studio Architecture & Offline Engine", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("100% Free & Offline Native Processing", style = MaterialTheme.typography.titleMedium, color = StudioTeal)
                    Text(
                        "• Photo & ID Engines: Native Android Bitmap, EXIF auto-orient, 3×3 unsharp-mask convolution, BFS flood-fill & chroma background removal, A4/4R print sheet tiler.\n" +
                            "• PDF & Office: Native PdfDocument & PdfRenderer + pure Kotlin OpenXML (.docx & .xlsx) ZIP/XML builder & parser.\n" +
                            "• Audio & Bangla TTS: Android TextToSpeech with WAV file synthesis + dual-voice PCM WAV stitcher + AudioRecord & DSP effects.\n" +
                            "• QR & Business: Pure Kotlin ISO/IEC 18004 Reed-Solomon QR encoder + Room SQLite persistence + Android PrintManager.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("GitHub & Build Notes", style = MaterialTheme.typography.titleMedium, color = StudioAmber)
                    Text(
                        "All source code is structured cleanly in modular packages (`engine`, `data.local`, `studio`, `ui.screens`) with zero hardcoded secrets or paid API keys. Check README.md in the project root for GitHub push, Android APK export, and companion deployment instructions.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
