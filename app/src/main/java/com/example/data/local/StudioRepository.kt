package com.example.data.local

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.io.File

class StudioRepository(
    private val context: Context,
    private val db: StudioDatabase
) {
    val customers: Flow<List<CustomerEntity>> = db.customerDao().observeAll()
    val invoices: Flow<List<InvoiceEntity>> = db.invoiceDao().observeAll()
    val transactions: Flow<List<TransactionEntity>> = db.transactionDao().observeAll()
    val projects: Flow<List<StudioProjectEntity>> = db.studioProjectDao().observeAll()

    fun projectsForModule(moduleType: String): Flow<List<StudioProjectEntity>> =
        db.studioProjectDao().observeByModule(moduleType)

    suspend fun saveCustomer(customer: CustomerEntity): Long {
        return if (customer.id == 0L) db.customerDao().insert(customer)
        else {
            db.customerDao().update(customer)
            customer.id
        }
    }

    suspend fun deleteCustomer(customer: CustomerEntity) = db.customerDao().delete(customer)

    suspend fun saveInvoice(invoice: InvoiceEntity, recordIncomeIfPaid: Boolean = true): Long {
        val id = db.invoiceDao().insert(invoice)
        if (recordIncomeIfPaid && invoice.paidAmount > 0.0) {
            db.transactionDao().insert(
                TransactionEntity(
                    type = "INCOME",
                    category = "Billing & Print",
                    title = "Invoice ${invoice.invoiceNumber} (${invoice.customerName})",
                    amount = invoice.paidAmount,
                    notes = "Payment: ${invoice.paymentMethod}",
                    dateMillis = invoice.createdAt
                )
            )
        }
        return id
    }

    suspend fun deleteInvoice(invoice: InvoiceEntity) = db.invoiceDao().delete(invoice)

    suspend fun saveTransaction(tx: TransactionEntity): Long = db.transactionDao().insert(tx)

    suspend fun deleteTransaction(tx: TransactionEntity) = db.transactionDao().delete(tx)

    suspend fun saveProject(project: StudioProjectEntity): Long = db.studioProjectDao().insert(project)

    suspend fun deleteProject(project: StudioProjectEntity) {
        if (project.previewPath.isNotBlank()) {
            runCatching { File(project.previewPath).delete() }
        }
        db.studioProjectDao().delete(project)
    }

    fun getExportsDir(): File = File(context.filesDir, "exports").apply { mkdirs() }

    fun getProjectsDir(): File = File(context.filesDir, "projects").apply { mkdirs() }
}
