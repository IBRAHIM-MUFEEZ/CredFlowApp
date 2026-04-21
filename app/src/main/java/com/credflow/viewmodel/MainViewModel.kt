package com.credflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.credflow.data.models.AccountKind
import com.credflow.data.models.CardSummary
import com.credflow.data.models.CustomerSummary
import com.credflow.data.repository.FirebaseRepository
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

    init {
        repository.listenAllData { appData ->
            _cards.value = appData.accounts
            _customers.value = appData.customers
        }
    }

    fun addCustomer(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return

        viewModelScope.launch {
            repository.addCustomer(trimmedName)
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

    fun addTransaction(
        customerId: String,
        customerName: String,
        accountId: String,
        accountName: String,
        accountKind: AccountKind,
        amount: String,
        transactionDate: String,
        dueDate: String
    ) {
        val parsedAmount = amount.toDoubleOrNull() ?: return

        viewModelScope.launch {
            repository.addTransaction(
                customerId = customerId,
                accountId = accountId,
                accountName = accountName,
                accountKind = accountKind,
                customerName = customerName.trim(),
                amount = parsedAmount,
                transactionDate = transactionDate.ifBlank { LocalDate.now().toString() },
                dueDate = dueDate
            )
        }
    }

    fun updateTransaction(
        transactionId: String,
        accountId: String,
        accountName: String,
        accountKind: AccountKind,
        amount: String,
        transactionDate: String,
        dueDate: String
    ) {
        val parsedAmount = amount.toDoubleOrNull() ?: return

        viewModelScope.launch {
            repository.updateTransaction(
                transactionId = transactionId,
                accountId = accountId,
                accountName = accountName,
                accountKind = accountKind,
                amount = parsedAmount,
                transactionDate = transactionDate.ifBlank { LocalDate.now().toString() },
                dueDate = dueDate
            )
        }
    }

    fun deleteTransaction(transactionId: String) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
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
