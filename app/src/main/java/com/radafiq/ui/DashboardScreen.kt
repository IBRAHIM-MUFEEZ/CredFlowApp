package com.radafiq.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Divider
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.radafiq.data.models.CustomerTransaction
import com.radafiq.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.LocalTime

enum class DashboardTab {
    HOME, ACCOUNTS, CUSTOMERS, EMI_SCHEDULE
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
    DashboardTabItem(DashboardTab.EMI_SCHEDULE, "EMI", Icons.Default.CalendarMonth)
)

@Composable
fun DashboardScreen(
    navController: NavController,
    selectedAccountIds: Set<String>,
    onOpenSettings: () -> Unit,
    profileName: String,
    vm: MainViewModel = viewModel(),
    onOpenCustomer: (String) -> Unit = {},
    onOpenAccount: (String) -> Unit = {}
) {
    var currentScreen by rememberSaveable { mutableStateOf(DashboardTab.HOME) }
    val cards by vm.cards.collectAsState()
    val customers by vm.customers.collectAsState()
    val driveOperationMessage by vm.driveOperationMessage.collectAsState()

    // Only show accounts that have been used in at least one customer transaction
    val usedAccountIds = remember(customers) {
        customers.flatMap { it.transactions }.map { it.accountId }.toSet()
    }
    val visibleCards = remember(cards, usedAccountIds) {
        if (usedAccountIds.isEmpty()) emptyList()
        else cards.filter { it.id in usedAccountIds }
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
            val tabIndex = DashboardTabs.indexOfFirst { it.tab == currentScreen }
            AnimatedContent(
                targetState = currentScreen to tabIndex,
                transitionSpec = {
                    val (_, targetIdx) = targetState
                    val (_, initialIdx) = initialState
                    val forward = targetIdx > initialIdx
                    (slideInHorizontally(
                        animationSpec = tween(300),
                        initialOffsetX = { if (forward) it else -it }
                    ) + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(
                        animationSpec = tween(300),
                        targetOffsetX = { if (forward) -it else it }
                    ) + fadeOut(tween(200)))
                },
                label = "dashboard-tab"
            ) { (tab, _) ->
                when (tab) {
                    DashboardTab.HOME -> HomeScreen(
                        cards = visibleCards,
                        profileName = profileName,
                        driveOperationMessage = driveOperationMessage,
                        modifier = Modifier.padding(padding),
                        onOpenSettings = onOpenSettings
                    )

                    DashboardTab.ACCOUNTS -> AccountsScreen(
                        cards = visibleCards,
                        vm = vm,
                        modifier = Modifier.padding(padding),
                        onOpenSettings = onOpenSettings,
                        onOpenAccount = onOpenAccount
                    )

                    DashboardTab.CUSTOMERS -> CustomersScreen(
                        selectedAccountIds = selectedAccountIds,
                        vm = vm,
                        modifier = Modifier.padding(padding),
                        onOpenSettings = onOpenSettings,
                        onOpenCustomer = onOpenCustomer
                    )

                    DashboardTab.EMI_SCHEDULE -> EmiScheduleScreen(
                        customers = customers,
                        modifier = Modifier.padding(padding)
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
    driveOperationMessage: String?,
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

        if (!driveOperationMessage.isNullOrBlank()) {
            item {
                DriveOperationCard(message = driveOperationMessage)
            }
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
private fun DriveOperationCard(message: String) {
    FlowCard(accentColor = MaterialTheme.colorScheme.primary) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Google Drive Sync",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            StatusBadge(
                text = "In Progress",
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
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

// ── EMI Schedule Tab ─────────────────────────────────────────────────────────

private data class EmiGroupRow(
    val groupId: String,
    val customerName: String,
    val planName: String,
    val accountName: String,
    val emiIndex: Int,       // 0-based
    val emiTotal: Int,
    val amount: Double,
    val transactionDate: String,
    val dueDate: String,
    val isSettled: Boolean,
    val isPast: Boolean,     // date <= today (already in transactions)
    val isCurrent: Boolean   // date == today's month
)

@Composable
fun EmiScheduleScreen(
    customers: List<CustomerSummary>,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }

    // Collect every EMI instalment across all customers, only from groups
    // Collect every EMI instalment across all customers, grouped by plan.
    // Shows all EMI groups that have at least one instalment recorded.
    val allRows: List<EmiGroupRow> = remember(customers, today) {
        val result = mutableListOf<EmiGroupRow>()

        customers.forEach { customer ->
            val emiGroups = customer.transactions
                .filter { it.isEmi }
                .groupBy { t ->
                    // Fall back to planName+customer composite key if emiGroupId is blank
                    t.emiGroupId.ifBlank {
                        t.name.substringBefore(" — EMI").trim() + "_" + customer.name
                    }
                }

            emiGroups.forEach { (groupKey, instalments) ->
                val sorted = instalments.sortedBy { it.transactionDate }

                sorted.forEach { t ->
                    val date = runCatching { LocalDate.parse(t.transactionDate) }.getOrNull()
                    val isPast = date == null || !date.isAfter(today)
                    val isCurrent = date != null &&
                        date.year == today.year && date.monthValue == today.monthValue
                    val planName = t.name.substringBefore(" — EMI").trim()
                    result.add(
                        EmiGroupRow(
                            groupId = groupKey,
                            customerName = customer.name,
                            planName = planName,
                            accountName = t.accountName,
                            emiIndex = t.emiIndex,
                            emiTotal = t.emiTotal,
                            amount = t.amount,
                            transactionDate = t.transactionDate,
                            dueDate = t.dueDate,
                            isSettled = t.isSettled,
                            isPast = isPast,
                            isCurrent = isCurrent
                        )
                    )
                }
            }
        }

        // Sort: current month first, then future by date, then past by date desc
        result.sortWith(
            compareBy<EmiGroupRow> { if (it.isCurrent) 0 else if (!it.isPast) 1 else 2 }
                .thenBy { it.transactionDate }
                .thenBy { it.customerName }
        )
        result
    }

    // Group rows by EMI group for the amortization table view
    val groupedByPlan: List<Pair<String, List<EmiGroupRow>>> = remember(allRows) {
        allRows
            .groupBy { it.groupId }
            .entries
            .sortedBy { (_, rows) -> rows.first().transactionDate }
            .map { (groupId, rows) -> groupId to rows }
    }

    val totalUpcoming = remember(allRows) {
        allRows.filter { !it.isPast }.sumOf { it.amount }
    }
    val totalPaid = remember(allRows) {
        allRows.filter { it.isPast && it.isSettled }.sumOf { it.amount }
    }
    val totalPending = remember(allRows) {
        allRows.filter { it.isPast && !it.isSettled }.sumOf { it.amount }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PageHeader(
                title = "EMI Schedule",
                subtitle = "Amortization view of all active EMI plans across customers."
            )
        }

        if (allRows.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = "No EMI plans yet",
                        subtitle = "When a customer has an EMI transaction added, the full schedule appears here."
                    )
                }
            }
            return@LazyColumn
        }

        // Summary strip
        item {
            FlowCard(accentColor = MaterialTheme.colorScheme.primary) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricPill(
                        label = "Upcoming",
                        value = formatMoney(totalUpcoming),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricPill(
                        label = "Pending",
                        value = formatMoney(totalPending),
                        color = warningColor(),
                        modifier = Modifier.weight(1f)
                    )
                    MetricPill(
                        label = "Settled",
                        value = formatMoney(totalPaid),
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // One amortization card per EMI group
        groupedByPlan.forEachIndexed { index, (_, rows) ->
            val first = rows.first()
            val groupTotal = rows.sumOf { it.amount }
            val groupPaid = rows.filter { it.isPast && it.isSettled }.sumOf { it.amount }
            val groupPending = rows.filter { it.isPast && !it.isSettled }.sumOf { it.amount }
            val groupUpcoming = rows.filter { !it.isPast }.sumOf { it.amount }

            item(key = first.groupId.ifBlank { "emi_plan_$index" }) {
                EmiAmortizationCard(
                    customerName = first.customerName,
                    planName = first.planName,
                    accountName = first.accountName,
                    rows = rows,
                    groupTotal = groupTotal,
                    groupPaid = groupPaid,
                    groupPending = groupPending,
                    groupUpcoming = groupUpcoming,
                    today = today
                )
            }
        }
    }
}

@Composable
private fun EmiAmortizationCard(
    customerName: String,
    planName: String,
    accountName: String,
    rows: List<EmiGroupRow>,
    groupTotal: Double,
    groupPaid: Double,
    groupPending: Double,
    groupUpcoming: Double,
    today: LocalDate
) {
    // Key by groupId so each card's expanded state is independent and survives recomposition
    val groupId = rows.firstOrNull()?.groupId ?: planName
    var expanded by rememberSaveable(groupId) { mutableStateOf(true) }
    val progress = if (groupTotal > 0) ((groupPaid + groupPending) / groupTotal).toFloat().coerceIn(0f, 1f) else 0f
    val completedCount = rows.count { it.isPast }
    val totalCount = rows.size

    FlowCard(accentColor = MaterialTheme.colorScheme.primary) {
        // Header
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = planName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = customerName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = accountName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatMoney(groupTotal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$completedCount / $totalCount done",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            androidx.compose.material3.LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Mini summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EmiMiniStat("Pending", formatMoney(groupPending), warningColor(), Modifier.weight(1f))
                EmiMiniStat("Upcoming", formatMoney(groupUpcoming), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                EmiMiniStat("Settled", formatMoney(groupPaid), MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
            }

            // Expand/collapse hint
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.AccountBox else Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (expanded) "Tap to collapse" else "Tap to view schedule",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            // Table header
            EmiTableHeader()

            Divider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // Instalment rows
            rows.forEachIndexed { idx, row ->
                EmiTableRow(row = row, today = today)
                if (idx != rows.lastIndex) {
                    Divider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmiTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(24.dp)
        )
        Text(
            text = "Due Date",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.6f)
        )
        Text(
            text = "Amount",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.4f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
        Text(
            text = "Status",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun EmiTableRow(row: EmiGroupRow, today: LocalDate) {
    val statusColor = when {
        row.isSettled -> MaterialTheme.colorScheme.secondary
        row.isCurrent -> warningColor()
        row.isPast -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val statusLabel = when {
        row.isSettled -> "Paid"
        row.isCurrent -> "Due"
        row.isPast -> "Overdue"
        else -> "Upcoming"
    }
    val rowBg = when {
        row.isCurrent && !row.isSettled -> warningColor().copy(alpha = 0.07f)
        row.isPast && !row.isSettled -> MaterialTheme.colorScheme.error.copy(alpha = 0.05f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg, RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${row.emiIndex + 1}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (row.isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = statusColor,
            modifier = Modifier.width(24.dp)
        )
        Text(
            text = formatDisplayDate(row.transactionDate),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = formatMoney(row.amount),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.4f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .width(72.dp)
                .padding(start = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                maxLines = 1,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusColor.copy(alpha = 0.13f))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun EmiMiniStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
