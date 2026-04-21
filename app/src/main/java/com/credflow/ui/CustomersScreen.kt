package com.credflow.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.credflow.viewmodel.MainViewModel

@Composable
fun CustomersScreen(vm: MainViewModel = viewModel()) {

    val cards by vm.cards.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Navigate to add customer */ }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Customer")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "👥 Customers",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (cards.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No customers yet. Add one to get started!")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cards) { card ->
                        CustomerCard(card)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerCard(card: com.credflow.data.models.CardSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "ID: ${card.id.take(8)}...",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Badge(
                    modifier = Modifier.align(Alignment.Top)
                ) {
                    Text(
                        if (card.payable > 0) "Owes ₹${String.format("%.0f", card.payable)}"
                        else "Settled",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Total Bill",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        "₹${String.format("%.2f", card.bill)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column {
                    Text(
                        "Pending",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        "₹${String.format("%.2f", card.pending)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column {
                    Text(
                        "Payable",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        "₹${String.format("%.2f", card.payable)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}