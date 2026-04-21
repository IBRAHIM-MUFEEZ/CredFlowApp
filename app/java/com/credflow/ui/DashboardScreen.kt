package com.credflow.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.credflow.viewmodel.MainViewModel

enum class DashboardScreen {
    HOME, ACCOUNTS, CUSTOMERS, ANALYTICS
}

@Composable
fun DashboardScreen(vm: MainViewModel = viewModel()) {

    var currentScreen by remember { mutableStateOf(DashboardScreen.HOME) }
    val cards by vm.cards.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentScreen == DashboardScreen.HOME,
                    onClick = { currentScreen = DashboardScreen.HOME },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = currentScreen == DashboardScreen.ACCOUNTS,
                    onClick = { currentScreen = DashboardScreen.ACCOUNTS },
                    icon = { Icon(Icons.Filled.List, contentDescription = "Accounts") },
                    label = { Text("Accounts") }
                )
                NavigationBarItem(
                    selected = currentScreen == DashboardScreen.CUSTOMERS,
                    onClick = { currentScreen = DashboardScreen.CUSTOMERS },
                    icon = { Icon(Icons.Filled.AccountBox, contentDescription = "Customers") },
                    label = { Text("Customers") }
                )
                NavigationBarItem(
                    selected = currentScreen == DashboardScreen.ANALYTICS,
                    onClick = { currentScreen = DashboardScreen.ANALYTICS },
                    icon = { Icon(Icons.Filled.BarChart, contentDescription = "Analytics") },
                    label = { Text("Analytics") }
                )
            }
        }
    ) { paddingValues ->
        when (currentScreen) {
            DashboardScreen.HOME -> HomeScreen(cards, Modifier.padding(paddingValues))
            DashboardScreen.ACCOUNTS -> AccountsScreen(vm)
            DashboardScreen.CUSTOMERS -> CustomersScreen(vm)
            DashboardScreen.ANALYTICS -> AnalyticsScreen(vm)
        }
    }
}

@Composable
fun HomeScreen(
    cards: List<com.credflow.data.models.CardSummary>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "💳 CredFlow Dashboard",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        if (cards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Column(
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text(
                        "Welcome to CredFlow!",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        "Start by adding your first account or card",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            Text(
                "Your Cards (${cards.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            cards.forEach {
                CreditCardItem(it.name, it.bill, it.pending, it.payable)
            }
        }
    }
}

@Composable
fun CreditCardItem(
    name: String,
    bill: Double,
    pending: Double,
    payable: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(text = name, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Text("Bill: ₹${String.format("%.2f", bill)}")
            Text("Pending: ₹${String.format("%.2f", pending)}")
            Text("Payable: ₹${String.format("%.2f", payable)}")
        }
    }
}