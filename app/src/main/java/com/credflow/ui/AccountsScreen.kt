package com.credflow.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.credflow.data.models.AccountKind
import com.credflow.data.models.CardSummary
import com.credflow.viewmodel.MainViewModel

@Composable
fun AccountsScreen(
    vm: MainViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val cards by vm.cards.collectAsState()
    val creditCards = cards.filter { it.accountKind == AccountKind.CREDIT_CARD }
    val bankAccounts = cards.filter { it.accountKind == AccountKind.BANK_ACCOUNT }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Accounts",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (cards.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No account totals yet. Add a customer entry to begin.")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    AccountSectionTitle("Credit Cards")
                }

                if (creditCards.isEmpty()) {
                    item {
                        Text("No credit card totals yet.")
                    }
                } else {
                    items(creditCards, key = { it.id }) { card ->
                        AccountCard(card)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    AccountSectionTitle("Bank Accounts")
                }

                if (bankAccounts.isEmpty()) {
                    item {
                        Text("No bank account totals yet.")
                    }
                } else {
                    items(bankAccounts, key = { it.id }) { card ->
                        AccountCard(card)
                    }
                }
            }
        }
    }
}

@Composable
fun AccountSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
    )
}

@Composable
fun AccountCard(card: CardSummary) {
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
            Text(
                text = card.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = card.accountKind.label,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Total Used",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        "₹${String.format("%.2f", card.bill)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column {
                    Text(
                        "Paid",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        "₹${String.format("%.2f", card.pending)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column {
                    Text(
                        "Balance",
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
