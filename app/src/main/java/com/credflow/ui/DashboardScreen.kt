package com.credflow.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.credflow.data.models.CardSummary
import com.credflow.viewmodel.MainViewModel
import androidx.compose.ui.graphics.vector.ImageVector

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
                            alwaysShowLabel = false,
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
                    DashboardTab.HOME -> HomeScreen(cards = cards, modifier = Modifier.padding(padding))
                    DashboardTab.ACCOUNTS -> AccountsScreen(
                        vm = vm,
                        modifier = Modifier.padding(padding)
                    )
                    DashboardTab.CUSTOMERS -> CustomersScreen(
                        vm = vm,
                        modifier = Modifier.padding(padding)
                    )
                    DashboardTab.ANALYTICS -> AnalyticsScreen(
                        vm = vm,
                        modifier = Modifier.padding(padding)
                    )
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
                subtitle = "Operational credit control for customers, accounts, and dues."
            )
        }

        item {
            HeroPanel(
                title = "Outstanding balance",
                amount = formatMoney(totalBalance),
                subtitle = "${cards.size} active account node(s) synchronized"
            )
        }

        item {
            ResponsiveTwoPane(
                first = { itemModifier ->
                    MetricPill(
                        label = "Used",
                        value = formatMoney(totalUsed),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = itemModifier
                    )
                },
                second = { itemModifier ->
                    MetricPill(
                        label = "Paid",
                        value = formatMoney(totalPaid),
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = itemModifier
                    )
                }
            )
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
                    Column {
                        Text(
                            text = card.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = card.accountKind.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

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
                                label = "Paid",
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
                }
            }
        }
    }
}
