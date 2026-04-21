package com.credflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.credflow.data.models.CardSummary
import com.credflow.data.repository.FirebaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val repo = FirebaseRepository()

    private val _cards = MutableStateFlow<List<CardSummary>>(emptyList())
    val cards: StateFlow<List<CardSummary>> = _cards

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {

            val accounts = repo.getAccounts()
            val transactions = repo.getTransactions()
            val payments = repo.getPayments()

            val result = mutableListOf<CardSummary>()

            for (acc in accounts.documents) {

                val type = acc.getString("type") ?: ""
                if (type != "credit_card") continue

                val id = acc.id
                val name = acc.getString("name") ?: ""
                val bill = acc.getDouble("bill") ?: 0.0

                var totalGiven = 0.0
                var totalReceived = 0.0

                transactions.documents.forEach {
                    if (it.getString("accountId") == id)
                        totalGiven += it.getDouble("amount") ?: 0.0
                }

                payments.documents.forEach {
                    if (it.getString("accountId") == id)
                        totalReceived += it.getDouble("amount") ?: 0.0
                }

                val pending = totalGiven - totalReceived
                var payable = bill - pending
                if (payable < 0) payable = 0.0

                result.add(
                    CardSummary(id, name, bill, pending, payable)
                )
            }

            _cards.value = result
        }
    }
}