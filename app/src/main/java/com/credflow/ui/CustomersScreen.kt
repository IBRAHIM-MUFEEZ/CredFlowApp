package com.credflow.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
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
    vm: MainViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val customers by vm.customers.collectAsState()
    val deletedCustomers by vm.deletedCustomers.collectAsState()
    var viewMode by remember { mutableStateOf(CustomerViewMode.ACTIVE) }

    val visibleCustomers = if (viewMode == CustomerViewMode.ACTIVE) {
        customers
    } else {
        deletedCustomers
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = viewMode.label,
                style = MaterialTheme.typography.headlineMedium
            )

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
                    if (viewMode == CustomerViewMode.ACTIVE) {
                        "Recycle Bin"
                    } else {
                        "Customers"
                    }
                )
            }
        }

        if (visibleCustomers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (viewMode == CustomerViewMode.ACTIVE) {
                        "No customers yet. Tap + to add one."
                    } else {
                        "Recycle bin is empty."
                    }
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = visibleCustomers,
                    key = { it.id }
                ) { customer ->
                    if (viewMode == CustomerViewMode.ACTIVE) {
                        CustomerCard(
                            customer = customer,
                            vm = vm
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
}

@Composable
fun CustomerCard(
    customer: CustomerSummary,
    vm: MainViewModel
) {
    var showAddTransaction by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<CustomerTransaction?>(null) }
    var showDueEditor by remember { mutableStateOf(false) }
    var transactionFilter by remember(customer.id) {
        mutableStateOf(TransactionTypeFilter.ALL)
    }

    val filteredTransactions = customer.transactions.filter { transaction ->
        when (transactionFilter) {
            TransactionTypeFilter.ALL -> true
            TransactionTypeFilter.BANK_ACCOUNT -> transaction.accountKind == AccountKind.BANK_ACCOUNT
            TransactionTypeFilter.CREDIT_CARD -> transaction.accountKind == AccountKind.CREDIT_CARD
        }
    }

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

                Row {
                    TextButton(onClick = { showDueEditor = true }) {
                        Text("Edit Due")
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

            TransactionTypeDropdown(
                selectedFilter = transactionFilter,
                onFilterSelected = { transactionFilter = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            if (filteredTransactions.isEmpty()) {
                Text(
                    text = "No transactions for this selection.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                filteredTransactions.forEach { transaction ->
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
fun DeletedCustomerCard(
    customer: CustomerSummary,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = customer.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${customer.transactions.size} transaction(s) in recycle bin",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AmountColumn("Used", customer.totalAmount)
                AmountColumn("Due Paid", customer.creditDueAmount)
                AmountColumn("Balance", customer.balance)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onRestore) {
                    Icon(Icons.Filled.Restore, contentDescription = "Restore Customer")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore")
                }
                TextButton(onClick = onDeleteForever) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete Forever")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete Forever")
                }
            }
        }
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
                text = transaction.name,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${transaction.accountName} - ${transaction.transactionDate}",
                style = MaterialTheme.typography.bodySmall
            )
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
        transactionName: String,
        accountId: String,
        accountName: String,
        accountKind: AccountKind,
        amount: String,
        transactionDate: String
    ) -> Unit
) {
    var transactionName by remember(transaction?.id) {
        mutableStateOf(transaction?.name.orEmpty())
    }
    var selectedKind by remember(transaction?.id) {
        mutableStateOf(transaction?.accountKind ?: AccountKind.CREDIT_CARD)
    }
    var selectedAccountId by remember(transaction?.id) {
        mutableStateOf(
            transaction?.accountId ?: IndianAccountCatalog.optionsFor(selectedKind).first().id
        )
    }
    var amountExpression by remember(transaction?.id) {
        mutableStateOf(transaction?.amount?.takeIf { it > 0.0 }?.toString().orEmpty())
    }
    var transactionDate by remember(transaction?.id) {
        mutableStateOf(transaction?.transactionDate?.ifBlank { LocalDate.now().toString() }
            ?: LocalDate.now().toString())
    }

    val calculatedAmount = remember(amountExpression) {
        evaluateAmountExpression(amountExpression)
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
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (transaction == null) "Add Transaction" else "Edit Transaction")
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
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = transactionName,
                    onValueChange = { transactionName = it },
                    label = { Text("Transaction Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = transactionDate,
                    onValueChange = { transactionDate = it },
                    label = { Text("Transaction Done Date (YYYY-MM-DD)") },
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
                    value = amountExpression,
                    onValueChange = { amountExpression = it },
                    label = { Text("Amount / Calculator") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    leadingIcon = { Text("₹") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                Text(
                    text = if (calculatedAmount != null) {
                        "= ₹${String.format("%.2f", calculatedAmount)}"
                    } else {
                        "Enter an amount or arithmetic expression"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
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
                    onSave(
                        transactionName,
                        selectedAccount.id,
                        selectedAccount.name,
                        selectedKind,
                        amount.toString(),
                        transactionDate
                    )
                },
                enabled = transactionName.isNotBlank() &&
                    calculatedAmount != null &&
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
                OutlinedButton(
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
