package com.credflow.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.credflow.data.models.CardSummary

@Composable
fun AnalyticsScreen(
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
                title = "Analytics",
                subtitle = "Inspect usage, payments, and due pressure across every account."
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

        Spacer(modifier = Modifier.height(10.dp))

        AccentValueRow(
            label = "Balance",
            value = formatMoney(totalBalance),
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

        cards.forEachIndexed { index, card ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(14.dp)
            ) {
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.bodyLarge,
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
                Spacer(modifier = Modifier.height(12.dp))
                AccentValueRow(
                    label = "Amount",
                    value = formatMoney(valueForCard(card)),
                    color = colorForCard(card)
                )
            }
            if (index != cards.lastIndex) {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}
