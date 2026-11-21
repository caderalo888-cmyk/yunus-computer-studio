package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.CustomerEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.InvoiceItemCodec
import com.example.data.local.InvoiceLineItem
import com.example.data.local.TransactionEntity
import com.example.engine.DesignCanvasRenderer
import com.example.engine.OfficeDocEngine
import com.example.engine.PrintShareHelper
import com.example.studio.StudioViewModel
import com.example.ui.components.StudioModuleScaffold
import com.example.ui.components.StudioSectionCard
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioRose
import com.example.ui.theme.StudioTeal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------- MODULE 19: CUSTOMER MANAGEMENT ----------------

@Composable
fun CustomerManagementScreen(
    viewModel: StudioViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var editingCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val filtered = remember(customers, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) customers
        else customers.filter {
            it.name.lowercase().contains(q) || it.phone.lowercase().contains(q) || it.address.lowercase().contains(q)
        }
    }

    StudioModuleScaffold(
        title = "Customer Management",
        subtitle = "Client Directory & Order Totals",
        accentColor = Color(0xFF3B82F6),
        onBack = onBack,
        snackbarHostState = snackbar
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search name, phone, address…") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = { editingCustomer = null; showDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Customer")
                        Spacer(Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }

            if (filtered.isEmpty()) {
                item {
                    StudioSectionCard(title = "No Customers Found") {
                        Text(
                            "Tap 'Add' above to register studio clients for faster billing and tracking.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filtered, key = { it.id }) { cust ->
                    val custInvoices = invoices.filter {
                        it.customerId == cust.id || (cust.phone.isNotBlank() && it.customerPhone == cust.phone)
                    }
                    val totalSpent = custInvoices.sumOf { it.total }
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
                                Text(cust.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(cust.phone, fontFamily = MonoFontFamily, style = MaterialTheme.typography.bodySmall, color = StudioTeal)
                                if (cust.address.isNotBlank()) {
                                    Text(cust.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    "${custInvoices.size} bill(s) • ৳${String.format("%.0f", totalSpent)}",
                                    fontFamily = MonoFontFamily,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = StudioAmber
                                )
                            }
                            Row {
                                IconButton(onClick = { editingCustomer = cust; showDialog = true }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit Customer")
                                }
                                IconButton(onClick = {
                                    viewModel.deleteCustomer(cust)
                                    scope.launch { snackbar.showSnackbar("Deleted ${cust.name}") }
                                }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete Customer", tint = StudioRose)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        CustomerEditDialog(
            initial = editingCustomer,
            onDismiss = { showDialog = false },
            onSave = { entity ->
                viewModel.saveCustomer(entity)
                showDialog = false
                scope.launch { snackbar.showSnackbar("Saved customer ${entity.name}") }
            }
        )
    }
}

@Composable
private fun CustomerEditDialog(
    initial: CustomerEntity?,
    onDismiss: () -> Unit,
    onSave: (CustomerEntity) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var email by remember { mutableStateOf(initial?.email ?: "") }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Studio Customer" else "Edit Customer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it; errorMsg = "" }, label = { Text("Customer Name *") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it; errorMsg = "" }, label = { Text("Phone Number *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email (optional)") }, singleLine = true)
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, singleLine = true)
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") })
                if (errorMsg.isNotBlank()) Text(errorMsg, color = StudioRose, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isBlank() || phone.isBlank()) {
                    errorMsg = "Name and phone number are required."
                } else {
                    onSave(
                        CustomerEntity(
                            id = initial?.id ?: 0L,
                            name = name.trim(),
                            phone = phone.trim(),
                            email = email.trim(),
                            address = address.trim(),
                            notes = notes.trim(),
                            createdAt = initial?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ---------------- MODULE 20: BILLING AND RECEIPT ----------------

@Composable
fun BillingReceiptScreen(
    viewModel: StudioViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()

    var selectedCustomerId by remember { mutableStateOf<Long?>(null) }
    var customerName by remember { mutableStateOf("Walk-In Customer") }
    var customerPhone by remember { mutableStateOf("") }
    val lineItems = remember {
        mutableStateListOf(InvoiceLineItem("Passport Photo (8 pcs)", 1, 100.0))
    }
    var newItemDesc by remember { mutableStateOf("") }
    var newItemQty by remember { mutableStateOf("1") }
    var newItemRate by remember { mutableStateOf("") }
    var discountStr by remember { mutableStateOf("0") }
    var paidStr by remember { mutableStateOf("100") }
    var paymentMethod by remember { mutableStateOf("Cash") }

    val subtotal = lineItems.sumOf { it.lineTotal }
    val discount = discountStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val grandTotal = (subtotal - discount).coerceAtLeast(0.0)
    val paid = paidStr.toDoubleOrNull()?.coerceAtLeast(0.0) ?: grandTotal

    val presets = remember {
        listOf(
            InvoiceLineItem("Passport Photo (8 pcs)", 1, 100.0),
            InvoiceLineItem("NID Laminate", 1, 50.0),
            InvoiceLineItem("CV Design & Print", 1, 60.0),
            InvoiceLineItem("Visiting Card (100 pcs)", 1, 450.0),
            InvoiceLineItem("Color Print (A4)", 1, 15.0)
        )
    }

    StudioModuleScaffold(
        title = "Billing & Receipt",
        subtitle = "Itemized Studio Invoices & Printable PDF Receipts",
        accentColor = StudioAmber,
        onBack = onBack,
        snackbarHostState = snackbar
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StudioSectionCard(title = "1. Customer Details") {
                if (customers.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(customers) { c ->
                            FilterChip(
                                selected = selectedCustomerId == c.id,
                                onClick = {
                                    selectedCustomerId = c.id
                                    customerName = c.name
                                    customerPhone = c.phone
                                },
                                label = { Text(c.name) }
                            )
                        }
                    }
                }
                OutlinedTextField(value = customerName, onValueChange = { customerName = it }, label = { Text("Customer Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = customerPhone, onValueChange = { customerPhone = it }, label = { Text("Phone Number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
            }

            StudioSectionCard(title = "2. Quick Studio Presets & Line Items") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets) { p ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                lineItems.add(p)
                                val newTotal = (lineItems.sumOf { it.lineTotal } - discount).coerceAtLeast(0.0)
                                paidStr = newTotal.toInt().toString()
                            },
                            label = { Text("+ ${p.description} (৳${p.unitPrice.toInt()})") }
                        )
                    }
                }

                lineItems.forEachIndexed { idx, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                "${item.quantity} × ৳${String.format("%.0f", item.unitPrice)} = ৳${String.format("%.0f", item.lineTotal)}",
                                fontFamily = MonoFontFamily,
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioTeal
                            )
                        }
                        IconButton(onClick = { lineItems.removeAt(idx) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Remove Item", tint = StudioRose)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = newItemDesc, onValueChange = { newItemDesc = it }, label = { Text("Service") }, singleLine = true, modifier = Modifier.weight(2f))
                    OutlinedTextField(value = newItemQty, onValueChange = { newItemQty = it.filter { c -> c.isDigit() } }, label = { Text("Qty") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(0.8f))
                    OutlinedTextField(value = newItemRate, onValueChange = { newItemRate = it }, label = { Text("Rate") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedButton(
                    onClick = {
                        val qty = newItemQty.toIntOrNull()?.coerceAtLeast(1) ?: 1
                        val rate = newItemRate.toDoubleOrNull()
                        if (newItemDesc.isNotBlank() && rate != null && rate > 0) {
                            lineItems.add(InvoiceLineItem(newItemDesc.trim(), qty, rate))
                            newItemDesc = ""; newItemQty = "1"; newItemRate = ""
                            val newTotal = (lineItems.sumOf { it.lineTotal } - discount).coerceAtLeast(0.0)
                            paidStr = newTotal.toInt().toString()
                        } else {
                            scope.launch { snackbar.showSnackbar("Enter valid service name and rate.") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text("Add Custom Item")
                }
            }

            StudioSectionCard(title = "3. Payment & Receipt Summary") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = discountStr, onValueChange = { discountStr = it }, label = { Text("Discount (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = paidStr, onValueChange = { paidStr = it }, label = { Text("Paid Amount (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Cash", "bKash", "Nagad", "Card").forEach { pm ->
                        FilterChip(selected = paymentMethod == pm, onClick = { paymentMethod = pm }, label = { Text(pm) })
                    }
                }
                Text(
                    "Subtotal: ৳${String.format("%.2f", subtotal)}  |  Grand Total: ৳${String.format("%.2f", grandTotal)}  |  Due: ৳${String.format("%.2f", (grandTotal - paid).coerceAtLeast(0.0))}",
                    fontFamily = MonoFontFamily,
                    style = MaterialTheme.typography.titleSmall,
                    color = StudioAmber
                )
                Button(
                    onClick = {
                        if (lineItems.isEmpty() || customerName.isBlank()) {
                            scope.launch { snackbar.showSnackbar("Add customer name and at least 1 item.") }
                            return@Button
                        }
                        val status = when {
                            paid >= grandTotal -> "PAID"
                            paid > 0 -> "PARTIAL"
                            else -> "DUE"
                        }
                        val invNum = "YS-${(invoices.size + 1001)}"
                        val entity = InvoiceEntity(
                            invoiceNumber = invNum,
                            customerId = selectedCustomerId,
                            customerName = customerName.trim(),
                            customerPhone = customerPhone.trim(),
                            itemsJson = InvoiceItemCodec.encode(lineItems.toList()),
                            subtotal = subtotal,
                            discount = discount,
                            total = grandTotal,
                            paidAmount = paid,
                            paymentMethod = paymentMethod,
                            status = status
                        )
                        viewModel.saveInvoice(entity, recordIncome = true) {
                            scope.launch { snackbar.showSnackbar("Invoice $invNum saved & income posted to Ledger!") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Invoice & Record Income")
                }
            }

            StudioSectionCard(title = "Recent Invoices (${invoices.size})") {
                if (invoices.isEmpty()) {
                    Text("No invoices yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    invoices.take(10).forEach { inv ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("${inv.invoiceNumber} • ${inv.customerName}", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Total: ৳${String.format("%.0f", inv.total)} | ${inv.paymentMethod} | ${inv.status}",
                                    fontFamily = MonoFontFamily,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (inv.status == "PAID") StudioEmerald else StudioAmber
                                )
                            }
                            Row {
                                IconButton(onClick = {
                                    scope.launch {
                                        val pdf = File(viewModel.repository.getExportsDir(), "${inv.invoiceNumber}.pdf")
                                        withContext(Dispatchers.IO) { DesignCanvasRenderer.renderInvoiceReceiptPdf(inv, pdf) }
                                        PrintShareHelper.printPdfFile(context, pdf, "Invoice ${inv.invoiceNumber}")
                                    }
                                }) { Icon(Icons.Rounded.Print, contentDescription = "Print Receipt") }
                                IconButton(onClick = {
                                    scope.launch {
                                        val pdf = File(viewModel.repository.getExportsDir(), "${inv.invoiceNumber}.pdf")
                                        withContext(Dispatchers.IO) { DesignCanvasRenderer.renderInvoiceReceiptPdf(inv, pdf) }
                                        PrintShareHelper.shareFile(context, pdf, "application/pdf")
                                    }
                                }) { Icon(Icons.Rounded.Share, contentDescription = "Share Receipt PDF") }
                                IconButton(onClick = { viewModel.deleteInvoice(inv) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete Invoice", tint = StudioRose)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------------- MODULE 21: INCOME AND EXPENSE MANAGEMENT ----------------

@Composable
fun IncomeExpenseScreen(
    viewModel: StudioViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    var filterType by remember { mutableStateOf("ALL") }
    var txType by remember { mutableStateOf("INCOME") }
    var txCategory by remember { mutableStateOf("Photo & Print") }
    var txTitle by remember { mutableStateOf("") }
    var txAmount by remember { mutableStateOf("") }

    val totalIncome = remember(transactions) { transactions.filter { it.type == "INCOME" }.sumOf { it.amount } }
    val totalExpense = remember(transactions) { transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount } }
    val netProfit = totalIncome - totalExpense

    val filteredTx = remember(transactions, filterType) {
        if (filterType == "ALL") transactions else transactions.filter { it.type == filterType }
    }

    val fmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    StudioModuleScaffold(
        title = "Income & Expense",
        subtitle = "Studio Cashflow, Category Analytics & Statement PDF",
        accentColor = StudioEmerald,
        onBack = onBack,
        snackbarHostState = snackbar
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                LedgerStatCard("Income", "৳${String.format("%.0f", totalIncome)}", StudioEmerald, Modifier.weight(1f))
                LedgerStatCard("Expense", "৳${String.format("%.0f", totalExpense)}", StudioRose, Modifier.weight(1f))
                LedgerStatCard("Net Profit", "৳${String.format("%.0f", netProfit)}", if (netProfit >= 0) StudioTeal else StudioRose, Modifier.weight(1f))
            }

            // Visual Canvas Comparison Chart
            StudioSectionCard(title = "Income vs Expense Ratio") {
                val maxVal = maxOf(totalIncome, totalExpense, 1.0).toFloat()
                Canvas(modifier = Modifier.fillMaxWidth().height(72.dp)) {
                    val incW = (totalIncome.toFloat() / maxVal) * size.width
                    val expW = (totalExpense.toFloat() / maxVal) * size.width
                    drawRoundRect(
                        color = StudioEmerald,
                        topLeft = Offset(0f, 6f),
                        size = Size(incW.coerceAtLeast(8f), 24f),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                    drawRoundRect(
                        color = StudioRose,
                        topLeft = Offset(0f, 40f),
                        size = Size(expW.coerceAtLeast(8f), 24f),
                        cornerRadius = CornerRadius(10f, 10f)
                    )
                }
            }

            StudioSectionCard(title = "Add Transaction Entry") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = txType == "INCOME",
                        onClick = { txType = "INCOME"; txCategory = "Photo & Print" },
                        label = { Text("+ Income") }
                    )
                    FilterChip(
                        selected = txType == "EXPENSE",
                        onClick = { txType = "EXPENSE"; txCategory = "Paper & Ink" },
                        label = { Text("- Expense") }
                    )
                }
                val cats = if (txType == "INCOME") {
                    listOf("Photo & Print", "Design & CV", "Online Form / NID", "Audio / QR", "Other")
                } else {
                    listOf("Paper & Ink", "Rent & Electricity", "Equipment", "Internet", "Other")
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(cats) { c ->
                        FilterChip(selected = txCategory == c, onClick = { txCategory = c }, label = { Text(c) })
                    }
                }
                OutlinedTextField(value = txTitle, onValueChange = { txTitle = it }, label = { Text("Description") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = txAmount, onValueChange = { txAmount = it }, label = { Text("Amount (৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        val amt = txAmount.toDoubleOrNull()
                        if (txTitle.isBlank() || amt == null || amt <= 0) {
                            scope.launch { snackbar.showSnackbar("Enter valid description and positive amount.") }
                            return@Button
                        }
                        viewModel.saveTransaction(
                            TransactionEntity(type = txType, category = txCategory, title = txTitle.trim(), amount = amt)
                        ) {
                            txTitle = ""; txAmount = ""
                            scope.launch { snackbar.showSnackbar("Transaction recorded.") }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Record Entry")
                }
            }

            StudioSectionCard(
                title = "Ledger History (${filteredTx.size})",
                trailing = {
                    OutlinedButton(onClick = {
                        scope.launch {
                            val summary = buildString {
                                appendLine("Total Income: $totalIncome")
                                appendLine("Total Expense: $totalExpense")
                                appendLine("Net Profit: $netProfit")
                                appendLine("---")
                                transactions.forEach { t ->
                                    appendLine("${fmt.format(Date(t.dateMillis))} | ${t.type} | ${t.category} | ${t.title} | ${t.amount}")
                                }
                            }
                            val pdf = File(viewModel.repository.getExportsDir(), "ledger_statement_${System.currentTimeMillis()}.pdf")
                            withContext(Dispatchers.IO) { OfficeDocEngine.writeTextToPdf("Yunus Studio Ledger Statement", summary, pdf) }
                            PrintShareHelper.shareFile(context, pdf, "application/pdf")
                        }
                    }) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Statement PDF")
                    }
                }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL", "INCOME", "EXPENSE").forEach { f ->
                        FilterChip(selected = filterType == f, onClick = { filterType = f }, label = { Text(f) })
                    }
                }
                if (filteredTx.isEmpty()) {
                    Text("No entries recorded yet.", style = MaterialTheme.typography.bodySmall)
                } else {
                    filteredTx.forEach { tx ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(tx.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${tx.category} • ${fmt.format(Date(tx.dateMillis))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = (if (tx.type == "INCOME") "+৳" else "-৳") + String.format("%.0f", tx.amount),
                                    fontFamily = MonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = if (tx.type == "INCOME") StudioEmerald else StudioRose
                                )
                                IconButton(onClick = { viewModel.deleteTransaction(tx) }) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun LedgerStatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontFamily = MonoFontFamily, fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
