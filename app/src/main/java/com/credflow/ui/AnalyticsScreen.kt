package com.credflow.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.credflow.data.models.CardSummary
import com.credflow.viewmodel.MainViewModel

@Composable
fun AnalyticsScreen(
    vm: MainViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val cards by vm.cards.collectAsState()
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
                title = "Analytics",
                subtitle = "Understand account usage, payments, and remaining balances."
            )
        }

        if (cards.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.height(420.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = "No analytics yet",
                        subtitle = "Add customer transactions to unlock insights."
                    )
                }
            }
        } else {
            item {
                HeroPanel(
                    title = "Total balance",
                    amount = formatMoney(totalBalance),
                    subtitle = "Used ${formatMoney(totalUsed)} minus paid ${formatMoney(totalPaid)}"
                )
            }

            item {
                SummaryCard(cards)
            }

            item {
                BreakdownCard(
                    title = "Usage breakdown",
                    cards = cards,
                    valueForCard = { it.bill },
                    colorForCard = { accountAccent(it.accountKind) }
                )
            }

            item {
                BreakdownCard(
                    title = "Paid amounts",
                    cards = cards,
                    valueForCard = { it.pending },
                    colorForCard = { MaterialTheme.colorScheme.secondary }
                )
            }

            item {
                BreakdownCard(
                    title = "Outstanding balances",
                    cards = cards,
                    valueForCard = { it.payable },
                    colorForCard = { warningColor() }
                )
            }
        }
    }
}

@Composable
fun SummaryCard(cards: List<CardSummary>) {
    val totalUsed = cards.sumOf { it.bill }
    val totalPaid = cards.sumOf { it.pending }
    val totalBalance = cards.sumOf { it.payable }

    FlowCard(accentColor = MaterialTheme.colorScheme.primary) {
        Text(
            "Overall summary",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

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

        Spacer(modifier = Modifier.height(10.dp))

        StatusBadge(
            text = "Balance ${formatMoney(totalBalance)}",
            color = if (totalBalance > 0.0) warningColor() else MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun BreakdownCard(
    title: String,
    cards: List<CardSummary>,
    valueForCard: (CardSummary) -> Double,
    colorForCard: @Composable (CardSummary) -> androidx.compose.ui.graphics.Color
) {
    FlowCard(accentColor = MaterialTheme.colorScheme.secondary) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        cards.forEach { card ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = card.accountKind.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(
                    text = formatMoney(valueForCard(card)),
                    color = colorForCard(card)
                )
            }
        }
    }
}
