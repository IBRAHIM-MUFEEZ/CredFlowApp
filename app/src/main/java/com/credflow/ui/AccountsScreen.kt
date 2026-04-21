package com.credflow.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
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
                        AccountCard(
                            card = card,
                            onUpdateCreditDue = { amount, dueDate ->
                                vm.updateCreditCardDue(
                                    accountId = card.id,
                                    accountName = card.name,
                                    amount = amount,
                                    dueDate = dueDate
                                )
                            }
                        )
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
                        AccountCard(card = card)
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
fun AccountCard(
    card: CardSummary,
    onUpdateCreditDue: ((amount: String, dueDate: String) -> Unit)? = null
) {
    var showDueEditor by remember { mutableStateOf(false) }

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

            if (card.accountKind == AccountKind.CREDIT_CARD && onUpdateCreditDue != null) {
                TextButton(
                    onClick = { showDueEditor = true },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("Edit Card Due")
                }
            }

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

            if (card.accountKind == AccountKind.CREDIT_CARD) {
                CreditCardDueStatus(card)
            }
        }
    }

    if (showDueEditor && onUpdateCreditDue != null) {
        CreditCardDueDialog(
            card = card,
            onDismiss = { showDueEditor = false },
            onSave = { amount, dueDate ->
                onUpdateCreditDue(amount, dueDate)
                showDueEditor = false
            }
        )
    }
}

@Composable
fun CreditCardDueStatus(card: CardSummary) {
    val remainingDue = card.dueAmount - card.bill
    val message = when {
        card.dueAmount <= 0.0 -> "No card due amount set."
        remainingDue > 0.0 -> "You owe ₹${String.format("%.2f", remainingDue)} to this credit card."
        remainingDue < 0.0 -> "You have overpaid ₹${String.format("%.2f", kotlin.math.abs(remainingDue))} for this credit card."
        else -> "This credit card due is fully covered."
    }

    Spacer(modifier = Modifier.height(12.dp))

    Divider()

    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            text = "Credit Card Due",
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Due Amount: ₹${String.format("%.2f", card.dueAmount)}",
            style = MaterialTheme.typography.bodyMedium
        )
        if (card.dueDate.isNotBlank()) {
            Text(
                text = "Due Date: ${card.dueDate}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = "Customer Used: ₹${String.format("%.2f", card.bill)}",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun CreditCardDueDialog(
    card: CardSummary,
    onDismiss: () -> Unit,
    onSave: (amount: String, dueDate: String) -> Unit
) {
    var dueAmount by remember(card.id, card.dueAmount) {
        mutableStateOf(card.dueAmount.takeIf { it > 0.0 }?.toString().orEmpty())
    }
    var dueDate by remember(card.id, card.dueDate) {
        mutableStateOf(card.dueDate)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit ${card.name} Due") },
        text = {
            Column {
                OutlinedTextField(
                    value = dueAmount,
                    onValueChange = { dueAmount = it },
                    label = { Text("Credit Card Due Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Text("₹") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(dueAmount, dueDate) },
                enabled = dueAmount.toDoubleOrNull() != null
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
