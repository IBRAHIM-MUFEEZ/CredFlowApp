package com.radafiq.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.radafiq.data.models.AccountKind
import com.radafiq.data.models.AccountOption
import com.radafiq.data.models.CustomerSummary
import com.radafiq.data.models.CustomerTransaction
import com.radafiq.data.models.IndianAccountCatalog
import com.radafiq.viewmodel.MainViewModel
import java.time.LocalDate

private enum class CustomerViewMode(val label: String) {
    ACTIVE("Customers"),
    RECYCLE_BIN("Recycle Bin")
}

private enum class TransactionTypeFilter(val label: String) {
    ALL("All Transactions"),
    BANK_ACCOUNT("Bank Account"),
    CREDIT_CARD("Credit Card")
}

@Composable
fun CustomersScreen(
    selectedAccountIds: Set<String>,
    vm: MainViewModel = viewModel(),
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {}
) {
    val customers by vm.customers.collectAsState()
    val deletedCustomers by vm.deletedCustomers.collectAsState()
    var viewMode by remember { mutableStateOf(CustomerViewMode.ACTIVE) }
    var expandedCustomerId by rememberSaveable(viewMode) { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val allVisibleCustomers = if (viewMode == CustomerViewMode.ACTIVE) customers else deletedCustomers
    val visibleCustomers = if (searchQuery.isBlank()) allVisibleCustomers
    else allVisibleCustomers.filter { it.name.contains(searchQuery, ignoreCase = true) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PageHeader(
                title = viewMode.label,
                subtitle = if (viewMode == CustomerViewMode.ACTIVE) {
                    "Manage customer ledgers, due collections, and settled transactions."
                } else {
                    "Restore customers or remove old records forever."
                },
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                viewMode = if (viewMode == CustomerViewMode.ACTIVE) {
                                    CustomerViewMode.RECYCLE_BIN
                                } else {
                                    CustomerViewMode.ACTIVE
                                }
                            }
                        ) {
                            Text(
                                if (viewMode == CustomerViewMode.ACTIVE) "Recycle Bin" else "Customers"
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search customers") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (visibleCustomers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = if (viewMode == CustomerViewMode.ACTIVE) {
                            "No customers yet"
                        } else {
                            "Recycle bin is empty"
                        },
                        subtitle = if (viewMode == CustomerViewMode.ACTIVE) {
                            "Tap + to add your first customer ledger."
                        } else {
                            "Deleted customers will wait here before permanent removal."
                        }
                    )
                }
            }
        } else {
            items(visibleCustomers, key = { it.id }) { customer ->
                if (viewMode == CustomerViewMode.ACTIVE) {
                    CustomerCard(
                        customer = customer,
                        vm = vm,
                        selectedAccountIds = selectedAccountIds,
                        isExpanded = expandedCustomerId == customer.id,
                        onToggleExpanded = {
                            expandedCustomerId = if (expandedCustomerId == customer.id) {
                                null
                            } else {
                                customer.id
                            }
                        }
                    )
                } else {
                    DeletedCustomerCard(
                        customer = customer,
                        onRestore = { vm.restoreCustomer(customer.id) },
                        onDeleteForever = {
                            vm.permanentlyDeleteCustomer(
                                customerId = customer.id,
                                customerName = customer.name
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerCard(
    customer: CustomerSummary,
    vm: MainViewModel,
    selectedAccountIds: Set<String>,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    var showAddTransaction by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<CustomerTransaction?>(null) }
    var transactionFilter by remember(customer.id) { mutableStateOf(TransactionTypeFilter.ALL) }

    val filteredTransactions = customer.transactions.filter { transaction ->
        when (transactionFilter) {
            TransactionTypeFilter.ALL -> true
            TransactionTypeFilter.BANK_ACCOUNT -> transaction.accountKind == AccountKind.BANK_ACCOUNT
            TransactionTypeFilter.CREDIT_CARD -> transaction.accountKind == AccountKind.CREDIT_CARD
        }
    }

    FlowCard(
        accentColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(vertical = 4.dp)
            .animateContentSize()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onToggleExpanded)
                        .padding(4.dp)
                ) {
                    AdaptiveHeaderRow(
                        leading = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .width(48.dp)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                        .border(
                                            width = 1.dp,
                                            color = Color.White.copy(alpha = 0.55f),
                                            shape = RoundedCornerShape(24.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initialsFor(customer.name),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(modifier = Modifier.padding(start = 12.dp)) {
                                    Text(
                                        text = customer.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${customer.transactions.size} transaction(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        },
                        trailing = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Balance",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatMoney(customer.balance),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (customer.balance > 0.0) {
                                            warningColor()
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        vm.deleteCustomer(
                                            customerId = customer.id,
                                            customerName = customer.name
                                        )
                                    }
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete Customer")
                                }
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ResponsiveTwoPane(
                        first = { itemModifier ->
                            MetricPill(
                                label = "Used",
                                value = formatMoney(customer.totalAmount),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = itemModifier
                            )
                        },
                        second = { itemModifier ->
                            MetricPill(
                                label = "Customer Paid",
                                value = formatMoney(customer.creditDueAmount),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = itemModifier
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    AccentValueRow(
                        label = "Balance Remaining",
                        value = formatMoney(customer.balance),
                        color = if (customer.balance > 0.0) warningColor() else MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Manual paid ${formatMoney(customer.manualPaidAmount)} • Settled transactions ${formatMoney(customer.settledTransactionAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp)
                    )

                    Text(
                        text = "Tap to hide transaction details",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                CustomerSectionHeader(
                    title = "Transactions",
                    actionLabel = "Add",
                    onAction = { showAddTransaction = true }
                )

                TransactionTypeDropdown(
                    selectedFilter = transactionFilter,
                    onFilterSelected = { transactionFilter = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 8.dp)
                )

                if (filteredTransactions.isEmpty()) {
                    EmptyInlineState("No transactions for this selection.")
                } else {
                    filteredTransactions.forEach { transaction ->
                        TransactionRow(
                            transaction = transaction,
                            onEdit = { transactionToEdit = transaction },
                            onDelete = { vm.deleteTransaction(transaction.id) },
                            onSettledChange = { vm.toggleTransactionSettled(transaction.id, it) }
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onToggleExpanded)
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Due ${formatMoney(customer.balance)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (customer.balance > 0.0) {
                                warningColor()
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            textAlign = TextAlign.End
                        )
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showAddTransaction) {
        TransactionEditorDialog(
            customer = customer,
            selectedAccountIds = selectedAccountIds,
            transaction = null,
            onDismiss = { showAddTransaction = false },
            onSave = { transactionName, accountId, accountName, accountKind, amount, transactionDate ->
                vm.addTransaction(
                    customerId = customer.id,
                    transactionName = transactionName,
                    customerName = customer.name,
                    accountId = accountId,
                    accountName = accountName,
                    accountKind = accountKind,
                    amount = amount,
                    transactionDate = transactionDate
                )
                showAddTransaction = false
            }
        )
    }

    transactionToEdit?.let { transaction ->
        TransactionEditorDialog(
            customer = customer,
            selectedAccountIds = selectedAccountIds,
            transaction = transaction,
            onDismiss = { transactionToEdit = null },
            onSave = { transactionName, accountId, accountName, accountKind, amount, transactionDate ->
                vm.updateTransaction(
                    transactionId = transaction.id,
                    transactionName = transactionName,
                    accountId = accountId,
                    accountName = accountName,
                    accountKind = accountKind,
                    amount = amount,
                    transactionDate = transactionDate
                )
                transactionToEdit = null
            }
        )
    }

}

private fun initialsFor(name: String): String {
    val parts = name
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)

    return parts.joinToString(separator = "") { part ->
        part.first().uppercaseChar().toString()
    }.ifBlank { "RA" }
}

@Composable
private fun CustomerSectionHeader(
    title: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    AdaptiveHeaderRow(
        leading = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        },
        trailing = {
            TextButton(onClick = onAction) {
                Icon(Icons.Filled.Add, contentDescription = actionLabel)
                Spacer(modifier = Modifier.width(4.dp))
                Text(actionLabel)
            }
        }
    )
}

@Composable
private fun EmptyInlineState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(14.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DeletedCustomerCard(
    customer: CustomerSummary,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    FlowCard(accentColor = dangerColor()) {
        Column {
            Text(
                text = customer.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${customer.transactions.size} transaction(s) in recycle bin",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            ResponsiveTwoPane(
                first = { itemModifier ->
                    MetricPill(
                        label = "Used",
                        value = formatMoney(customer.totalAmount),
                        color = dangerColor(),
                        modifier = itemModifier
                    )
                },
                second = { itemModifier ->
                    MetricPill(
                        label = "Balance",
                        value = formatMoney(customer.balance),
                        color = warningColor(),
                        modifier = itemModifier
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            ResponsiveTwoPane(
                first = { itemModifier ->
                    OutlinedButton(
                        onClick = onRestore,
                        modifier = itemModifier
                    ) {
                        Icon(Icons.Filled.Restore, contentDescription = "Restore Customer")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore")
                    }
                },
                second = { itemModifier ->
                    OutlinedButton(
                        onClick = onDeleteForever,
                        modifier = itemModifier
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete Forever")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete Forever")
                    }
                }
            )
        }
    }
}

@Composable
fun TransactionRow(
    transaction: CustomerTransaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSettledChange: (Boolean) -> Unit
) {
    val lineThrough = if (transaction.isSettled) TextDecoration.LineThrough else TextDecoration.None
    val contentAlpha = if (transaction.isSettled) 0.56f else 1f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
    ) {
        AdaptiveHeaderRow(
            leading = {
                Column {
                    Text(
                        text = transaction.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        textDecoration = lineThrough,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${transaction.accountName} • ${transaction.transactionDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            trailing = {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatMoney(transaction.amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = lineThrough,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                        textAlign = TextAlign.End
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Checkbox(
                            checked = transaction.isSettled,
                            onCheckedChange = { checked -> onSettledChange(checked) }
                        )
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Transaction")
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete Transaction")
                        }
                    }
                }
            }
        )

        Text(
            text = if (transaction.isSettled) {
                "Paid and cleared${transaction.settledDate.takeIf { it.isNotBlank() }?.let { " on $it" }.orEmpty()}"
            } else {
                "Pending collection"
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (transaction.isSettled) MaterialTheme.colorScheme.primary else warningColor(),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun DueAmountDialog(
    customer: CustomerSummary,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var dueAmount by remember(customer.id) {
        mutableStateOf(customer.manualPaidAmount.takeIf { it > 0.0 }?.toString().orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Adjust Manual Paid Amount",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column {
                Text(
                    text = "Use this when you want to enter a manual paid adjustment outside the transaction checkbox list.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = dueAmount,
                    onValueChange = { dueAmount = it },
                    label = { Text("Manual Paid Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Text("₹") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(dueAmount) },
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

@Composable
fun TransactionEditorDialog(
    customer: CustomerSummary,
    selectedAccountIds: Set<String>,
    transaction: CustomerTransaction?,
    onDismiss: () -> Unit,
    onSave: (
        transactionName: String,
        accountId: String,
        accountName: String,
        accountKind: AccountKind,
        amount: String,
        transactionDate: String
    ) -> Unit
) {
    val availableKinds = remember(selectedAccountIds, transaction?.accountKind) {
        selectedAccountKinds(selectedAccountIds, transaction?.accountKind)
    }
    val defaultKind = remember(availableKinds) {
        when {
            availableKinds.contains(AccountKind.CREDIT_CARD) -> AccountKind.CREDIT_CARD
            availableKinds.isNotEmpty() -> availableKinds.first()
            else -> AccountKind.CREDIT_CARD
        }
    }
    var transactionName by remember(transaction?.id) {
        mutableStateOf(transaction?.name.orEmpty())
    }
    var selectedKind by remember(transaction?.id, availableKinds) {
        mutableStateOf(transaction?.accountKind ?: defaultKind)
    }
    var selectedAccountId by remember(transaction?.id) {
        mutableStateOf(transaction?.accountId.orEmpty())
    }
    var amountExpression by remember(transaction?.id) {
        mutableStateOf(transaction?.amount?.takeIf { it > 0.0 }?.toString().orEmpty())
    }
    var transactionDate by remember(transaction?.id) {
        mutableStateOf(
            transaction?.transactionDate?.ifBlank { LocalDate.now().toString() }
                ?: LocalDate.now().toString()
        )
    }

    val calculatedAmount = remember(amountExpression) {
        evaluateAmountExpression(amountExpression)
    }
    val accountOptions = remember(selectedKind, selectedAccountIds, transaction?.accountId) {
        selectedAccountOptions(
            accountKind = selectedKind,
            selectedAccountIds = selectedAccountIds,
            pinnedAccountId = transaction?.accountId
        )
    }
    val selectedAccount = accountOptions.firstOrNull { it.id == selectedAccountId }
        ?: accountOptions.firstOrNull()

    LaunchedEffect(availableKinds) {
        if (selectedKind !in availableKinds && availableKinds.isNotEmpty()) {
            selectedKind = defaultKind
        }
    }

    LaunchedEffect(selectedKind, accountOptions) {
        if (accountOptions.isNotEmpty() && selectedAccountId !in accountOptions.map { it.id }) {
            selectedAccountId = accountOptions.first().id
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (transaction == null) "Add Transaction" else "Edit Transaction",
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                OutlinedTextField(
                    value = transactionName,
                    onValueChange = { transactionName = it },
                    label = { Text("Transaction Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = transactionDate,
                    onValueChange = { transactionDate = it },
                    label = { Text("Transaction Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                if (availableKinds.isEmpty() || selectedAccount == null) {
                    Text(
                        text = "No accounts are enabled in Settings. Select at least one bank or credit card there to add transactions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                } else {
                    AccountKindDropdown(
                        options = availableKinds,
                        selectedKind = selectedKind,
                        onKindSelected = { selectedKind = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )

                    AccountOptionDropdown(
                        label = if (selectedKind == AccountKind.BANK_ACCOUNT) "Bank Account" else "Credit Card",
                        selectedOption = selectedAccount,
                        options = accountOptions,
                        onOptionSelected = { selectedAccountId = it.id },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 18.dp, vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Amount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (amountExpression.isBlank()) "₹0" else "₹$amountExpression",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 8.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = if (calculatedAmount != null) {
                        "= ${formatMoney(calculatedAmount)}"
                    } else {
                        "Enter an amount or arithmetic expression"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )

                CalculatorPad(
                    expression = amountExpression,
                    onExpressionChange = { amountExpression = it },
                    calculatedAmount = calculatedAmount
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = calculatedAmount ?: return@TextButton
                    val activeAccount = selectedAccount ?: return@TextButton
                    onSave(
                        transactionName,
                        activeAccount.id,
                        activeAccount.name,
                        selectedKind,
                        amount.toString(),
                        transactionDate
                    )
                },
                enabled = transactionName.isNotBlank() &&
                    calculatedAmount != null &&
                    selectedAccount != null &&
                    transactionDate.isNotBlank()
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

private fun selectedAccountKinds(
    selectedAccountIds: Set<String>,
    pinnedKind: AccountKind?
): List<AccountKind> {
    val selectedKinds = IndianAccountCatalog.availableKinds(selectedAccountIds).toMutableList()
    if (pinnedKind != null && pinnedKind !in selectedKinds) {
        selectedKinds.add(pinnedKind)
    }
    return AccountKind.values().filter { it in selectedKinds }
}

private fun selectedAccountOptions(
    accountKind: AccountKind,
    selectedAccountIds: Set<String>,
    pinnedAccountId: String?
): List<AccountOption> {
    val options = IndianAccountCatalog.optionsFor(accountKind, selectedAccountIds).toMutableList()
    val pinnedOption = pinnedAccountId?.let(IndianAccountCatalog::optionById)

    if (
        pinnedOption != null &&
        pinnedOption.accountKind == accountKind &&
        options.none { it.id == pinnedOption.id }
    ) {
        options.add(pinnedOption)
    }

    return options
}

@Composable
private fun CalculatorPad(
    expression: String,
    onExpressionChange: (String) -> Unit,
    calculatedAmount: Double?
) {
    val rows = listOf(
        listOf("7", "8", "9", "/"),
        listOf("4", "5", "6", "*"),
        listOf("1", "2", "3", "-"),
        listOf("0", ".", "=", "+"),
        listOf("CLR", "DEL", "(", ")")
    )

    rows.forEach { row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            row.forEach { label ->
                Button(
                    onClick = {
                        when (label) {
                            "CLR" -> onExpressionChange("")
                            "DEL" -> onExpressionChange(expression.dropLast(1))
                            "=" -> {
                                if (calculatedAmount != null) {
                                    onExpressionChange(trimAmount(calculatedAmount))
                                }
                            }
                            else -> onExpressionChange(expression + label)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.48f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    contentPadding = PaddingValues(vertical = 8.dp, horizontal = 2.dp)
                ) {
                    Text(label)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionTypeDropdown(
    selectedFilter: TransactionTypeFilter,
    onFilterSelected: (TransactionTypeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedFilter.label,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Transaction Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TransactionTypeFilter.values().forEach { filter ->
                DropdownMenuItem(
                    text = { Text(filter.label) },
                    onClick = {
                        expanded = false
                        onFilterSelected(filter)
                    }
                )
            }
        }
    }
}

private fun trimAmount(value: Double): String {
    return if (value % 1.0 == 0.0) {
        value.toLong().toString()
    } else {
        value.toString()
    }
}

private fun evaluateAmountExpression(expression: String): Double? {
    val parser = AmountExpressionParser(expression)
    return parser.parse()
}

private class AmountExpressionParser(
    private val source: String
) {
    private var index = 0

    fun parse(): Double? {
        return try {
            val value = parseExpression()
            skipSpaces()
            if (index == source.length && value.isFinite()) value else null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun parseExpression(): Double {
        var value = parseTerm()
        while (true) {
            skipSpaces()
            value = when {
                consume('+') -> value + parseTerm()
                consume('-') -> value - parseTerm()
                else -> return value
            }
        }
    }

    private fun parseTerm(): Double {
        var value = parseFactor()
        while (true) {
            skipSpaces()
            value = when {
                consume('*') -> value * parseFactor()
                consume('/') -> {
                    val divisor = parseFactor()
                    if (divisor == 0.0) throw IllegalArgumentException()
                    value / divisor
                }
                else -> return value
            }
        }
    }

    private fun parseFactor(): Double {
        skipSpaces()
        if (consume('+')) return parseFactor()
        if (consume('-')) return -parseFactor()

        if (consume('(')) {
            val value = parseExpression()
            if (!consume(')')) throw IllegalArgumentException()
            return value
        }

        return parseNumber()
    }

    private fun parseNumber(): Double {
        skipSpaces()
        val start = index
        while (index < source.length && (source[index].isDigit() || source[index] == '.')) {
            index++
        }
        if (start == index) throw IllegalArgumentException()
        return source.substring(start, index).toDoubleOrNull()
            ?: throw IllegalArgumentException()
    }

    private fun consume(char: Char): Boolean {
        skipSpaces()
        return if (index < source.length && source[index] == char) {
            index++
            true
        } else {
            false
        }
    }

    private fun skipSpaces() {
        while (index < source.length && source[index].isWhitespace()) {
            index++
        }
    }
}
