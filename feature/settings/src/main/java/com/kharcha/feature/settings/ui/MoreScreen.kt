package com.kharcha.feature.settings.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.CsvExportHelper
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.common.UserPreferences
import com.kharcha.core.designsystem.component.CategoryBadge
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.Transaction
import com.kharcha.feature.settings.R
import java.io.File

@Composable
fun MoreScreen(
    transactions: List<Transaction>,
    userPreferences: UserPreferences,
    onResetOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
    onUpdateMonthlyBudget: (Long) -> Unit = {},
) {
    val context = LocalContext.current
    var isPrivacyEnabled by remember { mutableStateOf(userPreferences.isPrivacyModeEnabled) }
    var currentBudget by remember { mutableStateOf(userPreferences.monthlyBudgetMinor) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showRulesDialog by remember { mutableStateOf(false) }

    val rules = remember(showRulesDialog) { userPreferences.getMerchantCategoryRules() }
    val rulesCount = rules.size
    val unreviewedCount =
        remember(transactions) {
            transactions.count {
                !it.isReviewed || it.categorySource == "Other" || (it.parseConfidence ?: 1.0f) < 0.8f
            }
        }

    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Text(
                text = stringResource(id = R.string.settings_screen_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        // 1. App Banner Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Kharcha",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.settings_offline_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(id = R.string.settings_offline_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    )
                }
            }
        }

        // 2. General Section
        item {
            SettingsSectionHeader(title = stringResource(id = R.string.settings_section_general))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                Column {
                    // Monthly Budget Row
                    SettingsActionRow(
                        icon = Icons.Default.Edit,
                        title = stringResource(id = R.string.settings_budget_title),
                        subtitle = MoneyFormatter.formatMinorUnits(currentBudget),
                        trailing = {
                            IconButton(onClick = { showBudgetDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        },
                        onClick = { showBudgetDialog = true },
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )

                    // Currency Row
                    SettingsActionRow(
                        icon = Icons.Default.Check,
                        title = stringResource(id = R.string.settings_currency_title),
                        subtitle = stringResource(id = R.string.settings_currency_val),
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )

                    // Privacy Mode Toggle Row
                    SettingsActionRow(
                        icon = Icons.Default.Lock,
                        title = stringResource(id = R.string.settings_privacy_mode_title),
                        subtitle = stringResource(id = R.string.settings_privacy_mode_subtitle),
                        trailing = {
                            Switch(
                                checked = isPrivacyEnabled,
                                onCheckedChange = { isChecked ->
                                    isPrivacyEnabled = isChecked
                                    userPreferences.isPrivacyModeEnabled = isChecked
                                },
                            )
                        },
                        onClick = {
                            isPrivacyEnabled = !isPrivacyEnabled
                            userPreferences.isPrivacyModeEnabled = isPrivacyEnabled
                        },
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )

                    // Re-scan SMS Inbox Row
                    SettingsActionRow(
                        icon = Icons.Default.Refresh,
                        title = stringResource(id = R.string.settings_rescan_title),
                        subtitle = stringResource(id = R.string.settings_rescan_subtitle),
                        onClick = onResetOnboarding,
                    )
                }
            }
        }

        // 3. Organization & Rules Section
        item {
            SettingsSectionHeader(title = stringResource(id = R.string.settings_section_rules))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                SettingsActionRow(
                    icon = Icons.Default.Info,
                    title = stringResource(id = R.string.settings_rules_title),
                    subtitle = stringResource(id = R.string.settings_rules_subtitle, rulesCount),
                    onClick = { showRulesDialog = true },
                )
            }
        }

        // 4. Data & Export Section
        item {
            SettingsSectionHeader(title = stringResource(id = R.string.settings_section_data))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                Column {
                    SettingsActionRow(
                        icon = Icons.Default.Share,
                        title = stringResource(id = R.string.settings_export_csv_title),
                        subtitle =
                            stringResource(
                                id = R.string.settings_export_csv_subtitle,
                                transactions.size,
                            ),
                        onClick = {
                            val csv = CsvExportHelper.generateTransactionsCsv(transactions)
                            val filename = "kharcha_transactions_${System.currentTimeMillis()}.csv"
                            shareTextFile(context, csv, filename, "text/csv")
                        },
                    )

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )

                    SettingsActionRow(
                        icon = Icons.Default.Share,
                        title = stringResource(id = R.string.settings_export_llm_title),
                        subtitle =
                            stringResource(
                                id = R.string.settings_export_llm_subtitle,
                                unreviewedCount,
                            ),
                        onClick = {
                            val json = CsvExportHelper.generateLlmBatchExportJson(transactions)
                            val filename = "kharcha_llm_batch_${System.currentTimeMillis()}.json"
                            shareTextFile(context, json, filename, "application/json")
                        },
                    )
                }
            }
        }

        // 5. About Section
        item {
            SettingsSectionHeader(title = stringResource(id = R.string.settings_section_about))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.settings_app_version),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Zero tracking • 100% on-device analytics • MIT License",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showBudgetDialog) {
        var budgetInput by remember {
            mutableStateOf((currentBudget / 100L).toString())
        }
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = {
                Text(text = stringResource(id = R.string.settings_budget_edit_dialog_title))
            },
            text = {
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text(text = "Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = budgetInput.toLongOrNull() ?: (currentBudget / 100L)
                        val newMinor = parsed * 100L
                        currentBudget = newMinor
                        userPreferences.monthlyBudgetMinor = newMinor
                        onUpdateMonthlyBudget(newMinor)
                        showBudgetDialog = false
                    },
                ) {
                    Text(text = stringResource(id = R.string.settings_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text(text = stringResource(id = R.string.settings_cancel))
                }
            },
        )
    }

    if (showRulesDialog) {
        AlertDialog(
            onDismissRequest = { showRulesDialog = false },
            title = {
                Text(text = stringResource(id = R.string.settings_rules_dialog_title))
            },
            text = {
                if (rules.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.settings_rules_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(rules.entries.toList()) { (merchant, category) ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = merchant.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f),
                                    )
                                    CategoryBadge(categoryName = category, size = 28.dp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRulesDialog = false }) {
                    Text(text = stringResource(id = R.string.settings_close))
                }
            },
        )
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 4.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val rowModifier =
        if (onClick != null) {
            modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp)
        } else {
            modifier
                .fillMaxWidth()
                .padding(16.dp)
        }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

private fun shareTextFile(
    context: Context,
    content: String,
    fileName: String,
    mimeType: String,
) {
    try {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val file = File(exportDir, fileName)
        file.writeText(content)

        val uri =
            androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )

        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        context.startActivity(Intent.createChooser(intent, "Export $fileName"))
    } catch (_: Exception) {
        val fallbackIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
            }
        context.startActivity(Intent.createChooser(fallbackIntent, "Export $fileName"))
    }
}
