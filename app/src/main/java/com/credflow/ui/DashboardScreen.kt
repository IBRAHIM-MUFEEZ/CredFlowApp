package com.credflow.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.credflow.viewmodel.MainViewModel
import com.credflow.data.models.CardSummary

enum class DashboardTab {
    HOME, ACCOUNTS, CUSTOMERS, ANALYTICS
}

@Composable
fun DashboardScreen(
    navController: NavController,
    vm: MainViewModel = viewModel()
) {

    var currentScreen by remember { mutableStateOf(DashboardTab.HOME) }
    val cards by vm.cards.collectAsState()

    Scaffold(

        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("addTransaction") }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        },

        bottomBar = {
            NavigationBar {

                NavigationBarItem(
                    selected = currentScreen == DashboardTab.HOME,
                    onClick = { currentScreen = DashboardTab.HOME },
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Home") }
                )

                NavigationBarItem(
                    selected = currentScreen == DashboardTab.ACCOUNTS,
                    onClick = { currentScreen = DashboardTab.ACCOUNTS },
                    icon = { Icon(Icons.Default.List, null) },
                    label = { Text("Accounts") }
                )

                NavigationBarItem(
                    selected = currentScreen == DashboardTab.CUSTOMERS,
                    onClick = { currentScreen = DashboardTab.CUSTOMERS },
                    icon = { Icon(Icons.Default.AccountBox, null) },
                    label = { Text("Customers") }
                )

                NavigationBarItem(
                    selected = currentScreen == DashboardTab.ANALYTICS,
                    onClick = { currentScreen = DashboardTab.ANALYTICS },
                    icon = { Icon(Icons.Default.BarChart, null) },
                    label = { Text("Analytics") }
                )
            }
        }

    ) { padding ->

        when (currentScreen) {
            DashboardTab.HOME -> HomeScreen(cards, Modifier.padding(padding))
            DashboardTab.ACCOUNTS -> AccountsScreen(vm)
            DashboardTab.CUSTOMERS -> CustomersScreen(vm)
            DashboardTab.ANALYTICS -> AnalyticsScreen(vm)
        }
    }
}

@Composable
fun HomeScreen(
    cards: List<CardSummary>,
    modifier: Modifier = Modifier
) {

    Column(modifier = modifier.padding(16.dp)) {

        Text("💳 CredFlow Dashboard", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        if (cards.isEmpty()) {
            Text("No cards yet. Add transactions to begin.")
        } else {
            cards.forEach {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(it.name)
                        Text("Bill: ₹${it.bill}")
                        Text("Pending: ₹${it.pending}")
                        Text("Payable: ₹${it.payable}")
                    }
                }
            }
        }
    }
}