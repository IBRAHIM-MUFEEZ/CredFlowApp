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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.credflow.data.models.AccountKind
import com.credflow.data.models.CardSummary
import com.credflow.reminders.DueReminderScheduler
import com.credflow.viewmodel.MainViewModel
import kotlin.math.abs

@Composable
fun AccountsScreen(
    cards: List<CardSummary>,
    vm: MainViewModel = viewModel(),
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val dueReminderScheduler = remember(context) { DueReminderScheduler(context) }
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
                subtitle = "Monitor banks, credit cards, dues, and usage.",
                trailing = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
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
                        onAdjustPaid = { amount ->
                            vm.addPayment(
                                accountId = card.id,
                                accountName = card.name,
                                accountKind = card.accountKind,
                                amount = amount
                            )
                        },
                        onUpdateCreditDue = { amount, dueDate, remindersEnabled, reminderEmail, reminderWhatsApp ->
                            vm.updateCreditCardDue(
                                accountId = card.id,
                                accountName = card.name,
                                amount = amount,
                                dueDate = dueDate,
                                remindersEnabled = remindersEnabled,
                                reminderEmail = reminderEmail,
                                reminderWhatsApp = reminderWhatsApp
                            )
                            dueReminderScheduler.syncDueReminders(
                                accountId = card.id,
                                accountName = card.name,
                                dueDate = dueDate,
                                enabled = remindersEnabled
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
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
    )
}

@Composable
fun AccountCard(
    card: CardSummary,
    onAdjustPaid: ((String) -> Unit)? = null,
    onUpdateCreditDue: ((amount: String, dueDate: String, remindersEnabled: Boolean, reminderEmail: String, reminderWhatsApp: String) -> Unit)? = null
) {
    var showPaidEditor by remember { mutableStateOf(false) }
    var showDueEditor by remember { mutableStateOf(false) }

    FlowCard(accentColor = accountAccent(card.accountKind)) {
        AdaptiveHeaderRow(
            leading = {
                Column {
                    Text(
                        text = card.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = card.accountKind.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            trailing = {
                if (card.accountKind == AccountKind.CREDIT_CARD && onUpdateCreditDue != null) {
                    Column(
                        horizontalAlignment = androidx.compose.ui.Alignment.End
                    ) {
                        Text(
                            text = formatMoney(card.payable),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (onAdjustPaid != null) {
                                TextButton(onClick = { showPaidEditor = true }) {
                                    Text("Adjust Paid")
                                }
                            }
                            TextButton(onClick = { showDueEditor = true }) {
                                Text("Edit Due")
                            }
                        }
                    }
                } else {
                    Text(
                        text = formatMoney(card.payable),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        ResponsiveTwoPane(
            first = { itemModifier ->
                MetricPill(
                    label = "Used",
                    value = formatMoney(card.bill),
                    color = accountAccent(card.accountKind),
                    modifier = itemModifier
                )
            },
            second = { itemModifier ->
                MetricPill(
                    label = "Personal Paid",
                    value = formatMoney(card.pending),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = itemModifier
                )
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        AccentValueRow(
            label = "Balance",
            value = formatMoney(card.payable),
            color = accountAccent(card.accountKind)
        )

        if (card.accountKind == AccountKind.CREDIT_CARD) {
            CreditCardDueStatus(card)
        }
    }

    if (showPaidEditor && onAdjustPaid != null) {
        CreditCardPaidDialog(
            card = card,
            onDismiss = { showPaidEditor = false },
            onSave = { amount ->
                onAdjustPaid(amount)
                showPaidEditor = false
            }
        )
    }

    if (showDueEditor && onUpdateCreditDue != null) {
        CreditCardDueDialog(
            card = card,
            onDismiss = { showDueEditor = false },
            onSave = { amount, dueDate, remindersEnabled, reminderEmail, reminderWhatsApp ->
                onUpdateCreditDue(amount, dueDate, remindersEnabled, reminderEmail, reminderWhatsApp)
                showDueEditor = false
            }
        )
    }
}

@Composable
fun CreditCardPaidDialog(
    card: CardSummary,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var paidAmount by remember(card.id) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Adjust paid for ${card.name}",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column {
                Text(
                    text = "Add the extra paid amount you want to record for this credit card.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = paidAmount,
                    onValueChange = { paidAmount = it },
                    label = { Text("Paid Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Text("₹") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(paidAmount) },
                enabled = (paidAmount.toDoubleOrNull() ?: 0.0) > 0.0
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

@Composable
fun CreditCardDueStatus(card: CardSummary) {
    val remainingDue = card.dueAmount - card.pending
    val statusColor = when {
        card.dueAmount <= 0.0 -> MaterialTheme.colorScheme.onSurfaceVariant
        remainingDue > 0.0 -> warningColor()
        remainingDue < 0.0 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
    val message = when {
        card.dueAmount <= 0.0 -> "No card due amount set."
        remainingDue > 0.0 -> "You still need to pay ${formatMoney(remainingDue)} on this credit card."
        remainingDue < 0.0 -> "You have overpaid ${formatMoney(abs(remainingDue))} on this credit card."
        else -> "This credit card due is fully covered."
    }
    val balanceLabel = when {
        card.dueAmount <= 0.0 -> "Due status"
        remainingDue < 0.0 -> "Overpaid"
        else -> "Remaining due"
    }
    val balanceValue = when {
        card.dueAmount <= 0.0 -> formatMoney(0.0)
        remainingDue < 0.0 -> formatMoney(abs(remainingDue))
        else -> formatMoney(remainingDue)
    }
    val progress = when {
        card.dueAmount <= 0.0 -> 0f
        card.pending >= card.dueAmount -> 1f
        else -> (card.pending / card.dueAmount).toFloat().coerceIn(0f, 1f)
    }

    Spacer(modifier = Modifier.height(16.dp))
    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.42f))

    Column(modifier = Modifier.padding(top = 12.dp)) {
        AdaptiveHeaderRow(
            leading = {
                Column {
                    Text(
                        text = "Credit card due",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (card.dueDate.isBlank()) "Add a due date to track this card." else "Due ${card.dueDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            },
            trailing = {
                StatusBadge(
                    text = when {
                        card.dueAmount <= 0.0 -> "Unset"
                        remainingDue < 0.0 -> "Overpaid"
                        remainingDue > 0.0 -> "Pending"
                        else -> "Covered"
                    },
                    color = statusColor
                )
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        ResponsiveTwoPane(
            first = { itemModifier ->
                MetricPill(
                    label = "Due",
                    value = formatMoney(card.dueAmount),
                    color = statusColor,
                    modifier = itemModifier
                )
            },
            second = { itemModifier ->
                MetricPill(
                    label = "Personal Paid",
                    value = formatMoney(card.pending),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = itemModifier
                )
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(999.dp)),
            color = statusColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        AccentValueRow(
            label = balanceLabel,
            value = balanceValue,
            color = statusColor
        )

        Text(
            text = "Customer used for this card: ${formatMoney(card.bill)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (card.remindersEnabled) {
            Text(
                text = "Daily reminders are enabled from 5 days before the due date.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
            if (card.reminderEmail.isNotBlank()) {
                Text(
                    text = "Email target: ${card.reminderEmail}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (card.reminderWhatsApp.isNotBlank()) {
                Text(
                    text = "WhatsApp target: ${card.reminderWhatsApp}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
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
    onSave: (amount: String, dueDate: String, remindersEnabled: Boolean, reminderEmail: String, reminderWhatsApp: String) -> Unit
) {
    var dueAmount by remember(card.id, card.dueAmount) {
        mutableStateOf(card.dueAmount.takeIf { it > 0.0 }?.toString().orEmpty())
    }
    var dueDate by remember(card.id, card.dueDate) {
        mutableStateOf(card.dueDate)
    }
    var remindersEnabled by remember(card.id, card.remindersEnabled) {
        mutableStateOf(card.remindersEnabled)
    }
    var reminderEmail by remember(card.id, card.reminderEmail) {
        mutableStateOf(card.reminderEmail)
    }
    var reminderWhatsApp by remember(card.id, card.reminderWhatsApp) {
        mutableStateOf(card.reminderWhatsApp)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit ${card.name} due",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = dueAmount,
                    onValueChange = { dueAmount = it },
                    label = { Text("Credit Card Due Amount") },
                    singleLine = true,
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
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily due reminders",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "One reminder per day from 5 days before the due date until the due date.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = remindersEnabled,
                        onCheckedChange = { remindersEnabled = it }
                    )
                }
                if (remindersEnabled) {
                    OutlinedTextField(
                        value = reminderEmail,
                        onValueChange = { reminderEmail = it },
                        label = { Text("Reminder Email") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = reminderWhatsApp,
                        onValueChange = { reminderWhatsApp = it },
                        label = { Text("WhatsApp Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        dueAmount,
                        dueDate,
                        remindersEnabled,
                        reminderEmail,
                        reminderWhatsApp
                    )
                },
                enabled = dueAmount.toDoubleOrNull() != null && dueDate.isNotBlank()
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
