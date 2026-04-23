package com.radafiq.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.radafiq.data.models.AccountKind
import com.radafiq.data.models.IndianAccountCatalog
import com.radafiq.viewmodel.MainViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentScreen(
    selectedAccountIds: Set<String>,
    onNavigateBack: () -> Unit = {}
) {
    val vm: MainViewModel = viewModel()
    val availableKinds = remember(selectedAccountIds) {
        IndianAccountCatalog.availableKinds(selectedAccountIds)
    }
    val defaultKind = remember(availableKinds) {
        when {
            availableKinds.contains(AccountKind.CREDIT_CARD) -> AccountKind.CREDIT_CARD
            availableKinds.isNotEmpty() -> availableKinds.first()
            else -> AccountKind.CREDIT_CARD
        }
    }

    var selectedKind by remember(selectedAccountIds) { mutableStateOf(defaultKind) }
    var selectedAccountId by remember {
        mutableStateOf("")
    }
    var amount by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val today = LocalDate.now().toString()
    val accountOptions = remember(selectedKind, selectedAccountIds) {
        IndianAccountCatalog.optionsFor(selectedKind, selectedAccountIds)
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

    RadafiqBackground {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.12f),
            topBar = {
                TopAppBar(
                    title = { Text("Add Payment") },
                    navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.12f),
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { paddingValues ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                PageHeader(
                    title = "Record payment",
                    subtitle = "Apply a payment to a bank account or credit card ledger."
                )

                Spacer(modifier = Modifier.height(16.dp))

                FlowCard(accentColor = MaterialTheme.colorScheme.secondary) {
                    if (availableKinds.isEmpty() || selectedAccount == null) {
                        Text(
                            text = "No accounts are enabled in Settings. Add at least one bank or credit card there to record payments.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            label = { Text("Payment Amount") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            leadingIcon = { Text("₹") }
                        )

                        OutlinedTextField(
                            value = today,
                            onValueChange = {},
                            label = { Text("Payment Date") },
                            singleLine = true,
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp)
                        )

                        Button(
                            onClick = {
                                isLoading = true

                                vm.addPayment(
                                    accountId = selectedAccount.id,
                                    accountName = selectedAccount.name,
                                    accountKind = selectedKind,
                                    amount = amount
                                )

                                isLoading = false
                                onNavigateBack()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            enabled = amount.toDoubleOrNull() != null
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Save Payment")
                            }
                        }
                    }
                }
            }
        }
    }
}
