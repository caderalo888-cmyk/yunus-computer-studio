package com.example.studio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CustomerEntity
import com.example.data.local.InvoiceEntity
import com.example.data.local.StudioDatabase
import com.example.data.local.StudioProjectEntity
import com.example.data.local.StudioRepository
import com.example.data.local.TransactionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StudioViewModel(application: Application) : AndroidViewModel(application) {
    private val db = StudioDatabase.getInstance(application)
    val repository = StudioRepository(application, db)

    private val _darkTheme = MutableStateFlow(true)
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    val customers: StateFlow<List<CustomerEntity>> = repository.customers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val invoices: StateFlow<List<InvoiceEntity>> = repository.invoices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<TransactionEntity>> = repository.transactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projects: StateFlow<List<StudioProjectEntity>> = repository.projects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleTheme() {
        _darkTheme.value = !_darkTheme.value
    }

    fun saveCustomer(customer: CustomerEntity, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.saveCustomer(customer)
            onDone(id)
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch { repository.deleteCustomer(customer) }
    }

    fun saveInvoice(invoice: InvoiceEntity, recordIncome: Boolean = true, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.saveInvoice(invoice, recordIncome)
            onDone(id)
        }
    }

    fun deleteInvoice(invoice: InvoiceEntity) {
        viewModelScope.launch { repository.deleteInvoice(invoice) }
    }

    fun saveTransaction(tx: TransactionEntity, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.saveTransaction(tx)
            onDone(id)
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch { repository.deleteTransaction(tx) }
    }

    fun saveProject(project: StudioProjectEntity, onDone: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.saveProject(project)
            onDone(id)
        }
    }

    fun deleteProject(project: StudioProjectEntity) {
        viewModelScope.launch { repository.deleteProject(project) }
    }
}
