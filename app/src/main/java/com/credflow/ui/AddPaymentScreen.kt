package com.credflow.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.credflow.data.models.AccountKind
import com.credflow.data.models.IndianAccountCatalog
import com.credflow.viewmodel.MainViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentScreen(
    onNavigateBack: () -> Unit = {}
) {
    val vm: MainViewModel = viewModel()

    var selectedKind by remember { mutableStateOf(AccountKind.CREDIT_CARD) }
    var selectedAccountId by remember {
        mutableStateOf(IndianAccountCatalog.creditCards.first().id)
    }
    var amount by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val today = LocalDate.now().toString()
    val accountOptions = remember(selectedKind) {
        IndianAccountCatalog.optionsFor(selectedKind)
    }
    val selectedAccount = accountOptions.firstOrNull { it.id == selectedAccountId }
        ?: accountOptions.first()

    LaunchedEffect(selectedKind) {
        selectedAccountId = IndianAccountCatalog.optionsFor(selectedKind).first().id
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Payment") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {

            Text(
                text = "Record Payment",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 20.dp)
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
                label = { Text("Payment Amount") },
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
