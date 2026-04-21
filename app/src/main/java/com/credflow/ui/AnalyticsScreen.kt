package com.credflow.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.credflow.viewmodel.MainViewModel
import com.credflow.data.models.CardSummary
@Composable
fun AnalyticsScreen(vm: MainViewModel = viewModel()) {

    val cards by vm.cards.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "📊 Analytics",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        if (cards.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No data available. Add some transactions to see analytics.")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SummaryCard(cards)
                }

                item {
                    BillBreakdownCard(cards)
                }

                item {
                    PendingAmountCard(cards)
                }

                item {
                    PayableAmountCard(cards)
                }
            }
        }
    }
}

@Composable
fun SummaryCard(cards: List<CardSummary>) {
    val totalBill = cards.sumOf { it.bill }
    val totalPending = cards.sumOf { it.pending }
    val totalPayable = cards.sumOf { it.payable }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Overall Summary",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            StatRow("Total Bill Amount", "₹${String.format("%.2f", totalBill)}")
            Spacer(modifier = Modifier.height(8.dp))
            StatRow("Total Pending", "₹${String.format("%.2f", totalPending)}")
            Spacer(modifier = Modifier.height(8.dp))
            StatRow("Total Payable", "₹${String.format("%.2f", totalPayable)}")
        }
    }
}

@Composable
fun BillBreakdownCard(cards: List<CardSummary>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Bill Breakdown",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            cards.forEach { card ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(card.name)
                    Text("₹${String.format("%.2f", card.bill)}")
                }
            }
        }
    }
}

@Composable
fun PendingAmountCard(cards: List<CardSummary>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Pending Amounts",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            cards.forEach { card ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(card.name)
                    Text("₹${String.format("%.2f", card.pending)}")
                }
            }
        }
    }
}

@Composable
fun PayableAmountCard(cards: List<CardSummary>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Payable Amounts",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            cards.forEach { card ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(card.name)
                    Text("₹${String.format("%.2f", card.payable)}")
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}