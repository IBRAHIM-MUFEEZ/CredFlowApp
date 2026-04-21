package com.credflow.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.credflow.data.models.CardSummary
import com.credflow.viewmodel.MainViewModel

enum class DashboardTab {
    HOME, ACCOUNTS, CUSTOMERS, ANALYTICS
}

private data class DashboardTabItem(
    val tab: DashboardTab,
    val label: String,
    val icon: ImageVector
)

private val DashboardTabs = listOf(
    DashboardTabItem(DashboardTab.HOME, "Home", Icons.Default.Home),
    DashboardTabItem(DashboardTab.ACCOUNTS, "Accounts", Icons.Default.CreditCard),
    DashboardTabItem(DashboardTab.CUSTOMERS, "Customers", Icons.Default.AccountBox),
    DashboardTabItem(DashboardTab.ANALYTICS, "Analytics", Icons.Default.BarChart)
)

@Composable
fun DashboardScreen(
    navController: NavController,
    vm: MainViewModel = viewModel()
) {
    var currentScreen by rememberSaveable { mutableStateOf(DashboardTab.HOME) }
    val cards by vm.cards.collectAsState()

    CredFlowBackground {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            floatingActionButton = {
                if (currentScreen == DashboardTab.CUSTOMERS) {
                    FloatingActionButton(
                        onClick = { navController.navigate("addTransaction") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Customer")
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    DashboardTabs.forEach { item ->
                        NavigationBarItem(
                            selected = currentScreen == item.tab,
                            onClick = { currentScreen = item.tab },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Crossfade(
                targetState = currentScreen,
                animationSpec = tween(durationMillis = 260),
                label = "dashboard-tab"
            ) { tab ->
                when (tab) {
                    DashboardTab.HOME -> HomeScreen(cards, Modifier.padding(padding))
                    DashboardTab.ACCOUNTS -> AccountsScreen(vm, Modifier.padding(padding))
                    DashboardTab.CUSTOMERS -> CustomersScreen(vm, Modifier.padding(padding))
                    DashboardTab.ANALYTICS -> AnalyticsScreen(vm, Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    cards: List<CardSummary>,
    modifier: Modifier = Modifier
) {
    val totalUsed = cards.sumOf { it.bill }
    val totalPaid = cards.sumOf { it.pending }
    val totalBalance = cards.sumOf { it.payable }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PageHeader(
                title = "CredFlow",
                subtitle = "Track customers, cards, banks, and dues in one flow."
            )
        }

        item {
            HeroPanel(
                title = "Outstanding balance",
                amount = formatMoney(totalBalance),
                subtitle = "${cards.size} linked account(s)"
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricPill(
                    label = "Used",
                    value = formatMoney(totalUsed),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricPill(
                    label = "Paid",
                    value = formatMoney(totalPaid),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (cards.isEmpty()) {
            item {
                EmptyState(
                    title = "Your flow is ready",
                    subtitle = "Add customers from the Customers tab to start tracking usage."
                )
            }
        } else {
            item {
                Text(
                    text = "Account activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(cards, key = { it.id }) { card ->
                FlowCard(accentColor = accountAccent(card.accountKind)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = card.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = card.accountKind.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusBadge(
                            text = formatMoney(card.payable),
                            color = accountAccent(card.accountKind)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        CompactMetric("Used", formatMoney(card.bill))
                        Spacer(modifier = Modifier.width(8.dp))
                        CompactMetric("Paid", formatMoney(card.pending))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactMetric(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
