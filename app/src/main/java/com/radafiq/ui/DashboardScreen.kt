package com.radafiq.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.radafiq.data.models.AccountKind
import com.radafiq.data.models.CardSummary
import com.radafiq.data.models.CustomerSummary
import com.radafiq.viewmodel.MainViewModel
import java.time.LocalTime

enum class DashboardTab {
    HOME, ACCOUNTS, CUSTOMERS
}

private data class DashboardTabItem(
    val tab: DashboardTab,
    val label: String,
    val icon: ImageVector
)

private val DashboardTabs = listOf(
    DashboardTabItem(DashboardTab.HOME, "Home", Icons.Default.Home),
    DashboardTabItem(DashboardTab.ACCOUNTS, "Accounts", Icons.Default.CreditCard),
    DashboardTabItem(DashboardTab.CUSTOMERS, "Customers", Icons.Default.AccountBox)
)

@Composable
fun DashboardScreen(
    navController: NavController,
    selectedAccountIds: Set<String>,
    onOpenSettings: () -> Unit,
    profileName: String,
    vm: MainViewModel = viewModel()
) {
    var currentScreen by rememberSaveable { mutableStateOf(DashboardTab.HOME) }
    val cards by vm.cards.collectAsState()
    val visibleCards = remember(cards, selectedAccountIds) {
        cards.filter { it.id in selectedAccountIds }
    }

    RadafiqBackground {
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                if (currentScreen == DashboardTab.CUSTOMERS) {
                    ExtendedFloatingActionButton(
                        onClick = { navController.navigate("addTransaction") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        icon = { Icon(Icons.Default.Add, contentDescription = "Add Customer") },
                        text = { Text("Add Customer") }
                    )
                }
            },
            bottomBar = {
                DashboardBottomBar(
                    currentScreen = currentScreen,
                    onTabSelected = { currentScreen = it }
                )
            }
        ) { padding ->
            Crossfade(
                targetState = currentScreen,
                animationSpec = tween(durationMillis = 260),
                label = "dashboard-tab"
            ) { tab ->
                when (tab) {
                    DashboardTab.HOME -> HomeScreen(
                        cards = visibleCards,
                        profileName = profileName,
                        modifier = Modifier.padding(padding),
                        onOpenSettings = onOpenSettings
                    )

                    DashboardTab.ACCOUNTS -> AccountsScreen(
                        cards = visibleCards,
                        vm = vm,
                        modifier = Modifier.padding(padding),
                        onOpenSettings = onOpenSettings
                    )

                    DashboardTab.CUSTOMERS -> CustomersScreen(
                        selectedAccountIds = selectedAccountIds,
                        vm = vm,
                        modifier = Modifier.padding(padding),
                        onOpenSettings = onOpenSettings
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardBottomBar(
    currentScreen: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.74f))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                )
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DashboardTabs.forEach { item ->
                val selected = currentScreen == item.tab
                val containerColor = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    Color.Transparent
                }
                val contentColor = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(containerColor)
                        .clickable { onTabSelected(item.tab) }
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = contentColor
                    )
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    cards: List<CardSummary>,
    profileName: String,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit
) {
    val totalUsed = cards.sumOf { it.bill }
    val totalPaid = cards.sumOf { it.pending }
    val totalBalance = cards.sumOf { it.payable }
    val availableKinds = remember(cards) { cards.map { it.accountKind }.distinct() }
    var selectedActivityKindName by rememberSaveable { mutableStateOf("ALL") }
    var isFilterOpen by rememberSaveable { mutableStateOf(false) }

    val activityCards = when (selectedActivityKindName) {
        "ALL" -> cards
        else -> cards.filter { it.accountKind.name == selectedActivityKindName }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            HomeGreetingHeader(
                profileName = profileName,
                onOpenSettings = onOpenSettings
            )
        }

        item {
            HeroPanel(
                title = "Outstanding Balance",
                amount = formatMoney(totalBalance),
                subtitle = "${cards.size} active account(s) currently contributing to your ledger flow."
            )
        }

        item {
            ResponsiveTwoPane(
                first = { itemModifier ->
                    MetricPill(
                        label = "Total Used",
                        value = formatMoney(totalUsed),
                        color = warningColor(),
                        modifier = itemModifier
                    )
                },
                second = { itemModifier ->
                    MetricPill(
                        label = "Total Paid",
                        value = formatMoney(totalPaid),
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = itemModifier
                    )
                }
            )
        }

        item {
            AdaptiveHeaderRow(
                leading = {
                    Column {
                        Text(
                            text = "Account Activity",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Styled from the supplied design, adapted to your account data.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                },
                trailing = {
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(999.dp)
                                )
                                .clickable { isFilterOpen = true }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = when (selectedActivityKindName) {
                                    "ALL" -> "All Accounts"
                                    else -> availableKinds.firstOrNull { it.name == selectedActivityKindName }?.label
                                        ?: "All Accounts"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select account filter",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = isFilterOpen,
                            onDismissRequest = { isFilterOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Accounts") },
                                onClick = {
                                    selectedActivityKindName = "ALL"
                                    isFilterOpen = false
                                }
                            )
                            availableKinds.forEach { kind ->
                                DropdownMenuItem(
                                    text = { Text(kind.label) },
                                    onClick = {
                                        selectedActivityKindName = kind.name
                                        isFilterOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }

        if (activityCards.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = "No activity yet",
                        subtitle = "As customers and payments are recorded, the adapted activity cards will appear here."
                    )
                }
            }
        } else {
            items(activityCards.sortedByDescending { it.payable }.take(6), key = { it.id }) { card ->
                HomeActivityCard(card = card)
            }
        }
    }
}

@Composable
private fun HomeGreetingHeader(
    profileName: String,
    onOpenSettings: () -> Unit
) {
    val greeting = remember {
        when (LocalTime.now().hour) {
            in 0..11 -> "Good Morning,"
            in 12..16 -> "Good Afternoon,"
            else -> "Good Evening,"
        }
    }
    val resolvedProfileName = profileName.trim().ifBlank { "Your Profile" }
    val initials = remember(resolvedProfileName) {
        resolvedProfileName
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString(separator = "") { it.first().uppercaseChar().toString() }
            .ifBlank { "RA" }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = greeting,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = resolvedProfileName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
        ) {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun HomeActivityCard(card: CardSummary) {
    val isOutgoing = card.accountKind == AccountKind.CREDIT_CARD
    val accentColor = if (isOutgoing) warningColor() else MaterialTheme.colorScheme.secondary
    val amountText = if (isOutgoing) {
        "-${formatMoney(card.payable)}"
    } else {
        "+${formatMoney(card.payable)}"
    }
    val supportingText = buildString {
        append(card.accountKind.label)
        if (card.dueDate.isNotBlank()) {
            append(" / Due ")
            append(card.dueDate)
        }
    }

    FlowCard(accentColor = accountAccent(card.accountKind)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isOutgoing) Icons.Default.NorthEast else Icons.Default.SouthWest,
                        contentDescription = null,
                        tint = accentColor
                    )
                }

                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = card.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = amountText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
        }
    }
}
