package com.credflow.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.credflow.data.models.AccountKind
import com.credflow.data.models.CardSummary
import com.credflow.viewmodel.MainViewModel
import kotlin.math.abs

@Composable
fun AccountsScreen(
    vm: MainViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val cards by vm.cards.collectAsState()
    val creditCards = cards.filter { it.accountKind == AccountKind.CREDIT_CARD }
    val bankAccounts = cards.filter { it.accountKind == AccountKind.BANK_ACCOUNT }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PageHeader(
                title = "Accounts",
                subtitle = "Monitor banks, credit cards, dues, and usage."
            )
        }

        if (cards.isEmpty()) {
            item {
                Box(modifier = Modifier.height(420.dp)) {
                    EmptyState(
                        title = "No linked totals yet",
                        subtitle = "Customer transactions will appear here automatically."
                    )
                }
            }
        } else {
            item {
                AccountSectionTitle("Credit Cards")
            }

            if (creditCards.isEmpty()) {
                item {
                    Text(
                        text = "No credit card totals yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                    Text(
                        text = "No bank account totals yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(bankAccounts, key = { it.id }) { card ->
                    AccountCard(card = card)
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
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
    )
}

@Composable
fun AccountCard(
    card: CardSummary,
    onUpdateCreditDue: ((amount: String, dueDate: String) -> Unit)? = null
) {
    var showDueEditor by remember { mutableStateOf(false) }

    FlowCard(accentColor = accountAccent(card.accountKind)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = card.accountKind.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (card.accountKind == AccountKind.CREDIT_CARD && onUpdateCreditDue != null) {
                TextButton(onClick = { showDueEditor = true }) {
                    Text("Edit Due")
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricPill(
                label = "Used",
                value = formatMoney(card.bill),
                color = accountAccent(card.accountKind),
                modifier = Modifier.weight(1f)
            )
            MetricPill(
                label = "Paid",
                value = formatMoney(card.pending),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        StatusBadge(
            text = "Balance ${formatMoney(card.payable)}",
            color = accountAccent(card.accountKind)
        )

        if (card.accountKind == AccountKind.CREDIT_CARD) {
            CreditCardDueStatus(card)
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
    val statusColor = when {
        card.dueAmount <= 0.0 -> MaterialTheme.colorScheme.onSurfaceVariant
        remainingDue > 0.0 -> warningColor()
        remainingDue < 0.0 -> dangerColor()
        else -> MaterialTheme.colorScheme.primary
    }
    val message = when {
        card.dueAmount <= 0.0 -> "No card due amount set."
        remainingDue > 0.0 -> "You owe ${formatMoney(remainingDue)} to this credit card."
        remainingDue < 0.0 -> "You have overpaid ${formatMoney(abs(remainingDue))} for this credit card."
        else -> "This credit card due is fully covered."
    }

    Spacer(modifier = Modifier.height(14.dp))
    Divider(color = MaterialTheme.colorScheme.surfaceVariant)

    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(
            text = "Credit card due",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricPill(
                label = "Due",
                value = formatMoney(card.dueAmount),
                color = statusColor,
                modifier = Modifier.weight(1f)
            )
            MetricPill(
                label = "Customer used",
                value = formatMoney(card.bill),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
        if (card.dueDate.isNotBlank()) {
            Text(
                text = "Due date: ${card.dueDate}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = statusColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
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
        title = { Text("Edit ${card.name} due") },
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
