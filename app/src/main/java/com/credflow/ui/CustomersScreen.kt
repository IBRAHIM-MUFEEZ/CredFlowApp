package com.credflow.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.credflow.data.models.AccountKind
import com.credflow.data.models.CustomerSummary
import com.credflow.data.models.CustomerTransaction
import com.credflow.data.models.IndianAccountCatalog
import com.credflow.viewmodel.MainViewModel
import java.time.LocalDate

@Composable
fun CustomersScreen(
    vm: MainViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val customers by vm.customers.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Customers",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (customers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No customers yet. Tap + to add one.")
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = customers,
                    key = { it.id }
                ) { customer ->
                    CustomerCard(
                        customer = customer,
                        vm = vm
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerCard(
    customer: CustomerSummary,
    vm: MainViewModel
) {
    var showAddTransaction by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<CustomerTransaction?>(null) }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${customer.transactions.size} transaction(s)",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                TextButton(onClick = { showDueEditor = true }) {
                    Text("Edit Due")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AmountColumn("Used", customer.totalAmount)
                AmountColumn("Due Paid", customer.creditDueAmount)
                AmountColumn("Balance", customer.balance)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.titleSmall
                )
                TextButton(onClick = { showAddTransaction = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Transaction")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }

            if (customer.transactions.isEmpty()) {
                Text(
                    text = "No transactions yet.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                customer.transactions.forEach { transaction ->
                    TransactionRow(
                        transaction = transaction,
                        onEdit = { transactionToEdit = transaction },
                        onDelete = { vm.deleteTransaction(transaction.id) }
                    )
                }
            }
        }
    }

    if (showAddTransaction) {
        TransactionEditorDialog(
            customer = customer,
            transaction = null,
            onDismiss = { showAddTransaction = false },
            onSave = { accountId, accountName, accountKind, amount, transactionDate, dueDate ->
                vm.addTransaction(
                    customerId = customer.id,
                    customerName = customer.name,
                    accountId = accountId,
                    accountName = accountName,
                    accountKind = accountKind,
                    amount = amount,
                    transactionDate = transactionDate,
                    dueDate = dueDate
                )
                showAddTransaction = false
            }
        )
    }

    transactionToEdit?.let { transaction ->
        TransactionEditorDialog(
            customer = customer,
            transaction = transaction,
            onDismiss = { transactionToEdit = null },
            onSave = { accountId, accountName, accountKind, amount, transactionDate, dueDate ->
                vm.updateTransaction(
                    transactionId = transaction.id,
                    accountId = accountId,
                    accountName = accountName,
                    accountKind = accountKind,
                    amount = amount,
                    transactionDate = transactionDate,
                    dueDate = dueDate
                )
                transactionToEdit = null
            }
        )
    }

    if (showDueEditor) {
        DueAmountDialog(
            customer = customer,
            onDismiss = { showDueEditor = false },
            onSave = { amount ->
                vm.updateCustomerDueAmount(
                    customerId = customer.id,
                    customerName = customer.name,
                    amount = amount
                )
                showDueEditor = false
            }
        )
    }
}

@Composable
fun AmountColumn(
    label: String,
    amount: Double
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = "₹${String.format("%.2f", amount)}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun TransactionRow(
    transaction: CustomerTransaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.accountName,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${transaction.accountKind.label} - ${transaction.transactionDate}",
                style = MaterialTheme.typography.bodySmall
            )
            if (
                transaction.accountKind == AccountKind.CREDIT_CARD &&
                transaction.dueDate.isNotBlank()
            ) {
                Text(
                    text = "Due: ${transaction.dueDate}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Text(
            text = "₹${String.format("%.2f", transaction.amount)}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        IconButton(onClick = onEdit) {
            Icon(Icons.Filled.Edit, contentDescription = "Edit Transaction")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "Delete Transaction")
        }
    }
}

@Composable
fun DueAmountDialog(
    customer: CustomerSummary,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var dueAmount by remember(customer.id) {
        mutableStateOf(customer.creditDueAmount.takeIf { it > 0.0 }?.toString().orEmpty())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Credit Due Amount") },
        text = {
            OutlinedTextField(
                value = dueAmount,
                onValueChange = { dueAmount = it },
                label = { Text("Due Amount Paid") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                leadingIcon = { Text("₹") },
                modifier = Modifier.fillMaxWidth()
            )
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
    transaction: CustomerTransaction?,
    onDismiss: () -> Unit,
    onSave: (
        accountId: String,
        accountName: String,
        accountKind: AccountKind,
        amount: String,
        transactionDate: String,
        dueDate: String
    ) -> Unit
) {
    var selectedKind by remember(transaction?.id) {
        mutableStateOf(transaction?.accountKind ?: AccountKind.CREDIT_CARD)
    }
    var selectedAccountId by remember(transaction?.id) {
        mutableStateOf(
            transaction?.accountId ?: IndianAccountCatalog.optionsFor(selectedKind).first().id
        )
    }
    var amount by remember(transaction?.id) {
        mutableStateOf(transaction?.amount?.takeIf { it > 0.0 }?.toString().orEmpty())
    }
    var transactionDate by remember(transaction?.id) {
        mutableStateOf(transaction?.transactionDate?.ifBlank { LocalDate.now().toString() }
            ?: LocalDate.now().toString())
    }
    var dueDate by remember(transaction?.id) {
        mutableStateOf(transaction?.dueDate.orEmpty())
    }

    val accountOptions = remember(selectedKind) {
        IndianAccountCatalog.optionsFor(selectedKind)
    }
    val selectedAccount = accountOptions.firstOrNull { it.id == selectedAccountId }
        ?: accountOptions.first()

    LaunchedEffect(selectedKind) {
        val options = IndianAccountCatalog.optionsFor(selectedKind)
        if (selectedAccountId !in options.map { it.id }) {
            selectedAccountId = options.first().id
        }
        if (selectedKind == AccountKind.BANK_ACCOUNT) {
            dueDate = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (transaction == null) "Add Transaction" else "Edit Transaction")
        },
        text = {
            Column {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = transactionDate,
                    onValueChange = { transactionDate = it },
                    label = { Text("Transaction Date (YYYY-MM-DD)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                AccountKindDropdown(
                    selectedKind = selectedKind,
                    onKindSelected = { selectedKind = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                AccountOptionDropdown(
                    label = if (selectedKind == AccountKind.BANK_ACCOUNT) {
                        "Bank Account"
                    } else {
                        "Credit Card"
                    },
                    selectedOption = selectedAccount,
                    options = accountOptions,
                    onOptionSelected = { selectedAccountId = it.id },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = { Text("₹") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                if (selectedKind == AccountKind.CREDIT_CARD) {
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Due Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        selectedAccount.id,
                        selectedAccount.name,
                        selectedKind,
                        amount,
                        transactionDate,
                        dueDate
                    )
                },
                enabled = amount.toDoubleOrNull() != null && transactionDate.isNotBlank()
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
