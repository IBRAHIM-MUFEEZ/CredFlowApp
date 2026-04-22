package com.credflow.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.credflow.data.models.AccountOption
import com.credflow.data.models.IndianAccountCatalog
import com.credflow.data.settings.AppSettingsState
import com.credflow.data.settings.AppThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsState: AppSettingsState,
    onThemeModeSelected: (AppThemeMode) -> Unit,
    onAccountSelectionChanged: (String, Boolean) -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    CredFlowBackground {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.12f),
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    PageHeader(
                        title = "Settings",
                        subtitle = "Choose the bank and credit card accounts you hold, then switch between light and dark mode."
                    )
                }

                item {
                    FlowCard(accentColor = MaterialTheme.colorScheme.secondary) {
                        Text(
                            text = "Appearance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        ResponsiveTwoPane(
                            first = { itemModifier ->
                                ThemeModeButton(
                                    label = AppThemeMode.LIGHT.label,
                                    selected = settingsState.themeMode == AppThemeMode.LIGHT,
                                    onClick = { onThemeModeSelected(AppThemeMode.LIGHT) },
                                    modifier = itemModifier
                                )
                            },
                            second = { itemModifier ->
                                ThemeModeButton(
                                    label = AppThemeMode.DARK.label,
                                    selected = settingsState.themeMode == AppThemeMode.DARK,
                                    onClick = { onThemeModeSelected(AppThemeMode.DARK) },
                                    modifier = itemModifier
                                )
                            }
                        )
                    }
                }

                item {
                    FlowCard(accentColor = MaterialTheme.colorScheme.primary) {
                        Text(
                            text = "Account visibility",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${settingsState.selectedAccountIds.size} account(s) selected. These choices will be used throughout the app.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Keep at least one account selected so transactions and payments always have a valid destination.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }

                item {
                    SettingsAccountSection(
                        title = "Bank Accounts",
                        options = IndianAccountCatalog.bankAccounts,
                        selectedAccountIds = settingsState.selectedAccountIds,
                        onAccountSelectionChanged = onAccountSelectionChanged
                    )
                }

                item {
                    SettingsAccountSection(
                        title = "Credit Cards",
                        options = IndianAccountCatalog.creditCards,
                        selectedAccountIds = settingsState.selectedAccountIds,
                        onAccountSelectionChanged = onAccountSelectionChanged
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeModeButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(label)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier
        ) {
            Text(label)
        }
    }
}

@Composable
private fun SettingsAccountSection(
    title: String,
    options: List<AccountOption>,
    selectedAccountIds: Set<String>,
    onAccountSelectionChanged: (String, Boolean) -> Unit
) {
    FlowCard(accentColor = MaterialTheme.colorScheme.primary) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(12.dp))

        options.forEachIndexed { index, option ->
            SettingsAccountRow(
                option = option,
                selected = option.id in selectedAccountIds,
                allowDeselection = option.id !in selectedAccountIds || selectedAccountIds.size > 1,
                onCheckedChange = { checked ->
                    onAccountSelectionChanged(option.id, checked)
                }
            )

            if (index != options.lastIndex) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun SettingsAccountRow(
    option: AccountOption,
    selected: Boolean,
    allowDeselection: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val canToggle = !selected || allowDeselection

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canToggle) { onCheckedChange(!selected) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = if (canToggle) onCheckedChange else null
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            Text(
                text = option.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = option.accountKind.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
