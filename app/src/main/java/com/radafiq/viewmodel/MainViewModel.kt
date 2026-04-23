package com.radafiq.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.radafiq.data.models.AccountKind
import com.radafiq.data.models.CardSummary
import com.radafiq.data.models.CustomerSummary
import com.radafiq.data.repository.FirebaseRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: FirebaseRepository = FirebaseRepository()
) : ViewModel() {

    private val _cards = MutableStateFlow<List<CardSummary>>(emptyList())
    val cards: StateFlow<List<CardSummary>> = _cards.asStateFlow()

    private val _customers = MutableStateFlow<List<CustomerSummary>>(emptyList())
    val customers: StateFlow<List<CustomerSummary>> = _customers.asStateFlow()

    private val _deletedCustomers = MutableStateFlow<List<CustomerSummary>>(emptyList())
    val deletedCustomers: StateFlow<List<CustomerSummary>> = _deletedCustomers.asStateFlow()

    init {
        repository.listenAllData { appData ->
            _cards.value = appData.accounts
            _customers.value = appData.customers
            _deletedCustomers.value = appData.deletedCustomers
        }
    }

    fun addCustomer(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return

        viewModelScope.launch {
            repository.addCustomer(trimmedName)
        }
    }

    fun deleteCustomer(
        customerId: String,
        customerName: String
    ) {
        viewModelScope.launch {
            repository.deleteCustomer(
                customerId = customerId,
                customerName = customerName
            )
        }
    }

    fun restoreCustomer(customerId: String) {
        viewModelScope.launch {
            repository.restoreCustomer(customerId)
        }
    }

    fun permanentlyDeleteCustomer(
        customerId: String,
        customerName: String
    ) {
        viewModelScope.launch {
            repository.permanentlyDeleteCustomer(
                customerId = customerId,
                customerName = customerName
            )
        }
    }

    fun updateCustomerDueAmount(
        customerId: String,
        customerName: String,
        amount: String
    ) {
        val parsedAmount = amount.toDoubleOrNull() ?: return

        viewModelScope.launch {
            repository.updateCustomerDueAmount(
                customerId = customerId,
                customerName = customerName,
                creditDueAmount = parsedAmount
            )
        }
    }

    fun updateCreditCardDue(
        accountId: String,
        accountName: String,
        amount: String,
        dueDate: String,
        remindersEnabled: Boolean,
        reminderEmail: String,
        reminderWhatsApp: String
    ) {
        val parsedAmount = amount.toDoubleOrNull() ?: return

        viewModelScope.launch {
            repository.updateCreditCardDue(
                accountId = accountId,
                accountName = accountName,
                dueAmount = parsedAmount,
                dueDate = dueDate,
                remindersEnabled = remindersEnabled,
                reminderEmail = reminderEmail.trim(),
                reminderWhatsApp = reminderWhatsApp.trim()
            )
        }
    }

    fun addTransaction(
        customerId: String,
        transactionName: String,
        customerName: String,
        accountId: String,
        accountName: String,
        accountKind: AccountKind,
        amount: String,
        transactionDate: String
    ) {
        val parsedAmount = amount.toDoubleOrNull() ?: return

        viewModelScope.launch {
            repository.addTransaction(
                customerId = customerId,
                transactionName = transactionName.trim(),
                accountId = accountId,
                accountName = accountName,
                accountKind = accountKind,
                customerName = customerName.trim(),
                amount = parsedAmount,
                transactionDate = transactionDate.ifBlank { LocalDate.now().toString() }
            )
        }
    }

    fun updateTransaction(
        transactionId: String,
        transactionName: String,
        accountId: String,
        accountName: String,
        accountKind: AccountKind,
        amount: String,
        transactionDate: String
    ) {
        val parsedAmount = amount.toDoubleOrNull() ?: return

        viewModelScope.launch {
            repository.updateTransaction(
                transactionId = transactionId,
                transactionName = transactionName.trim(),
                accountId = accountId,
                accountName = accountName,
                accountKind = accountKind,
                amount = parsedAmount,
                transactionDate = transactionDate.ifBlank { LocalDate.now().toString() }
            )
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
        }
    }

    fun toggleTransactionSettled(
        transactionId: String,
        isSettled: Boolean
    ) {
        viewModelScope.launch {
            repository.toggleTransactionSettled(
                transactionId = transactionId,
                isSettled = isSettled,
                settledDate = if (isSettled) LocalDate.now().toString() else ""
            )
        }
    }

    fun addPayment(
        accountId: String,
        accountName: String,
        accountKind: AccountKind,
        amount: String
    ) {
        val parsedAmount = amount.toDoubleOrNull() ?: return

        viewModelScope.launch {
            repository.addPayment(
                accountId = accountId,
                accountName = accountName,
                accountKind = accountKind,
                amount = parsedAmount,
                date = LocalDate.now().toString()
            )
        }
    }
}
