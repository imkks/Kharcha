package com.kharcha.feature.settings.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.CsvExportHelper
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.common.UserPreferences
import com.kharcha.core.designsystem.R as DesignR
import com.kharcha.core.designsystem.component.CategoryBadge
import com.kharcha.core.designsystem.component.DEFAULT_CATEGORIES
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.Account
import com.kharcha.core.model.AccountType
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
    accounts: List<Account> = emptyList(),
    onSaveAccount: (Account) -> Unit = {},
    onApplyMerchantRuleToTransactions: ((merchant: String, category: String) -> Unit)? = null,
) {
    val context = LocalContext.current
    var isPrivacyEnabled by remember { mutableStateOf(userPreferences.isPrivacyModeEnabled) }
    var currentBudget by remember { mutableStateOf(userPreferences.monthlyBudgetMinor) }

    var showBudgetDialog by remember { mutableStateOf(false) }
    var showRulesDialog by remember { mutableStateOf(false) }
    var showAddRuleDialog by remember { mutableStateOf(false) }
    var showCategoriesDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAccountsDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }

    var rulesRevision by remember { mutableIntStateOf(0) }
    val rules = remember(rulesRevision, showRulesDialog) { userPreferences.getMerchantCategoryRules() }
    val rulesCount = rules.size

    var categoriesRevision by remember { mutableIntStateOf(0) }
    val customCategories =
        remember(categoriesRevision, showCategoriesDialog) {
            userPreferences.getCustomCategories()
        }
    val customCategoriesCount = customCategories.size
    val allCategories = remember(customCategories) { (DEFAULT_CATEGORIES + customCategories).distinct() }

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
                        containerColor = MaterialTheme.financialColors.cardBackground,
                    ),
                border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Kharcha",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.settings_offline_title),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.financialColors.limeAccent,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(id = R.string.settings_offline_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.financialColors.textSecondary,
                    )
                }
            }
        }

        // 2. Accounts & Cards Section
        item {
            SettingsSectionHeader(title = stringResource(id = R.string.settings_section_accounts))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.financialColors.cardBackground,
                    ),
                border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
            ) {
                SettingsActionRow(
                    painter = painterResource(id = DesignR.drawable.ic_channel_card),
                    title = stringResource(id = R.string.settings_manage_accounts_title),
                    subtitle = stringResource(id = R.string.settings_manage_accounts_subtitle, accounts.size),
                    trailing = {
                        Icon(
                            painter = painterResource(id = DesignR.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.financialColors.textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    onClick = { showAccountsDialog = true },
                )
            }
        }

        // 3. General Section
        item {
            SettingsSectionHeader(title = stringResource(id = R.string.settings_section_general))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.financialColors.cardBackground,
                    ),
                border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
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
                                    tint = MaterialTheme.financialColors.limeAccent,
                                )
                            }
                        },
                        onClick = { showBudgetDialog = true },
                    )

                    HorizontalDivider(
                        color = MaterialTheme.financialColors.hairlineDivider,
                    )

                    // Currency Row
                    SettingsActionRow(
                        icon = Icons.Default.Check,
                        title = stringResource(id = R.string.settings_currency_title),
                        subtitle = stringResource(id = R.string.settings_currency_val),
                    )

                    HorizontalDivider(
                        color = MaterialTheme.financialColors.hairlineDivider,
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
                                colors =
                                    SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = MaterialTheme.financialColors.limeAccent,
                                        uncheckedThumbColor = MaterialTheme.financialColors.textSecondary,
                                        uncheckedTrackColor = Color(0xFF2E2E2E),
                                    ),
                            )
                        },
                        onClick = {
                            isPrivacyEnabled = !isPrivacyEnabled
                            userPreferences.isPrivacyModeEnabled = isPrivacyEnabled
                        },
                    )

                    HorizontalDivider(
                        color = MaterialTheme.financialColors.hairlineDivider,
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

        // 4. Organization & Rules Section
        item {
            SettingsSectionHeader(title = stringResource(id = R.string.settings_section_rules))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.financialColors.cardBackground,
                    ),
                border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
            ) {
                Column {
                    SettingsActionRow(
                        icon = Icons.Default.Info,
                        title = stringResource(id = R.string.settings_rules_title),
                        subtitle = stringResource(id = R.string.settings_rules_subtitle, rulesCount),
                        trailing = {
                            Icon(
                                painter = painterResource(id = DesignR.drawable.ic_chevron_right),
                                contentDescription = null,
                                tint = MaterialTheme.financialColors.textSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        onClick = { showRulesDialog = true },
                    )

                    HorizontalDivider(
                        color = MaterialTheme.financialColors.hairlineDivider,
                    )

                    SettingsActionRow(
                        painter = painterResource(id = DesignR.drawable.ic_category_shopping),
                        title = stringResource(id = R.string.settings_categories_title),
                        subtitle = stringResource(id = R.string.settings_categories_subtitle, customCategoriesCount),
                        trailing = {
                            Icon(
                                painter = painterResource(id = DesignR.drawable.ic_chevron_right),
                                contentDescription = null,
                                tint = MaterialTheme.financialColors.textSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        onClick = { showCategoriesDialog = true },
                    )
                }
            }
        }

        // 5. Data & Export Section
        item {
            SettingsSectionHeader(title = stringResource(id = R.string.settings_section_data))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.financialColors.cardBackground,
                    ),
                border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
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
                        color = MaterialTheme.financialColors.hairlineDivider,
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

        // 6. About Section
        item {
            SettingsSectionHeader(title = stringResource(id = R.string.settings_section_about))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.financialColors.cardBackground,
                    ),
                border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.settings_app_version),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Zero tracking • 100% on-device analytics • MIT License",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.financialColors.textSecondary,
                    )
                }
            }
        }
    }

    // --- DIALOGS ---

    // 1. Edit Monthly Budget Dialog
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
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.financialColors.limeAccent,
                            contentColor = MaterialTheme.financialColors.onLimeAccent,
                        ),
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

    // 2. Manage Accounts Dialog
    if (showAccountsDialog) {
        AlertDialog(
            onDismissRequest = { showAccountsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_manage_accounts_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = { showAddAccountDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(id = R.string.settings_add_account),
                            tint = MaterialTheme.financialColors.limeAccent,
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { showAddAccountDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.financialColors.paleLimeAddButton,
                                contentColor = MaterialTheme.financialColors.onPaleLimeAddButton,
                            ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.settings_add_account),
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (accounts.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.settings_accounts_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.financialColors.textSecondary,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(accounts, key = { it.id }) { acc ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.financialColors.cardBackground,
                                    border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        val iconRes =
                                            when (acc.type) {
                                                AccountType.CreditCard -> DesignR.drawable.ic_channel_card
                                                AccountType.Wallet -> DesignR.drawable.ic_channel_wallet
                                                AccountType.Cash -> DesignR.drawable.ic_bank
                                                else -> DesignR.drawable.ic_bank
                                            }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Icon(
                                                painter = painterResource(id = iconRes),
                                                contentDescription = null,
                                                tint = MaterialTheme.financialColors.limeAccent,
                                                modifier = Modifier.size(24.dp),
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = acc.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color.White,
                                                )
                                                val sub = "${acc.bankName ?: ""} ••${acc.last4 ?: "----"}"
                                                Text(
                                                    text = sub,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.financialColors.textSecondary,
                                                )
                                            }
                                        }
                                        Text(
                                            text = MoneyFormatter.formatMinorUnits(acc.currentBalance ?: 0L),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.financialColors.limeAccent,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountsDialog = false }) {
                    Text(text = stringResource(id = R.string.settings_close))
                }
            },
        )
    }

    // 3. Add Account Dialog
    if (showAddAccountDialog) {
        var accName by remember { mutableStateOf("") }
        var accBank by remember { mutableStateOf("") }
        var accLast4 by remember { mutableStateOf("") }
        var accBalanceInput by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf(AccountType.Savings) }

        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            title = {
                Text(
                    text = stringResource(id = R.string.settings_add_account),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = accName,
                        onValueChange = { accName = it },
                        label = { Text(text = stringResource(id = R.string.settings_account_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = accBank,
                        onValueChange = { accBank = it },
                        label = { Text(text = stringResource(id = R.string.settings_account_bank_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = accLast4,
                            onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) accLast4 = it },
                            label = { Text(text = stringResource(id = R.string.settings_account_last4_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = accBalanceInput,
                            onValueChange = { accBalanceInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text(text = stringResource(id = R.string.settings_account_balance_label)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Text(
                        text = stringResource(id = R.string.settings_account_type_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.financialColors.textSecondary,
                    )

                    AccountTypeSelector(
                        selectedType = selectedType,
                        onTypeSelected = { selectedType = it },
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedBalanceMinor = (accBalanceInput.toLongOrNull() ?: 0L) * 100L
                        val newAccount =
                            Account(
                                id = 0L,
                                name = accName.trim().ifBlank { accBank.trim().ifBlank { "Account" } },
                                bankName = accBank.trim().ifBlank { null },
                                last4 = accLast4.trim().ifBlank { null },
                                type = selectedType,
                                currentBalance = parsedBalanceMinor,
                                balanceUpdatedAt = System.currentTimeMillis(),
                            )
                        onSaveAccount(newAccount)
                        showAddAccountDialog = false
                    },
                    enabled = accName.isNotBlank() || accBank.isNotBlank(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.financialColors.limeAccent,
                            contentColor = MaterialTheme.financialColors.onLimeAccent,
                        ),
                ) {
                    Text(text = stringResource(id = R.string.settings_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAccountDialog = false }) {
                    Text(text = stringResource(id = R.string.settings_cancel))
                }
            },
        )
    }

    // 4. Merchant Category Rules Dialog
    if (showRulesDialog) {
        var searchQuery by remember { mutableStateOf("") }
        val filteredRules =
            remember(rules, searchQuery) {
                if (searchQuery.isBlank()) {
                    rules.entries.toList()
                } else {
                    rules.entries.filter { (m, c) ->
                        m.contains(searchQuery.trim(), ignoreCase = true) ||
                            c.contains(searchQuery.trim(), ignoreCase = true)
                    }
                }
            }

        AlertDialog(
            onDismissRequest = { showRulesDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_rules_dialog_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = { showAddRuleDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(id = R.string.settings_add_merchant_rule),
                            tint = MaterialTheme.financialColors.limeAccent,
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { showAddRuleDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.financialColors.paleLimeAddButton,
                                contentColor = MaterialTheme.financialColors.onPaleLimeAddButton,
                            ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.settings_add_merchant_rule),
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(text = stringResource(id = R.string.settings_rules_search_hint)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.financialColors.textSecondary,
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (filteredRules.isEmpty()) {
                        Text(
                            text =
                                if (searchQuery.isBlank()) {
                                    stringResource(id = R.string.settings_rules_empty)
                                } else {
                                    stringResource(id = R.string.settings_rules_no_match)
                                },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.financialColors.textSecondary,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(filteredRules, key = { it.key }) { (merchant, category) ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.financialColors.cardBackground,
                                    border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
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
                                            color = Color.White,
                                            modifier = Modifier.weight(1f),
                                        )
                                        CategoryBadge(categoryName = category, size = 28.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = {
                                                userPreferences.removeMerchantCategoryRule(merchant)
                                                rulesRevision++
                                            },
                                            modifier = Modifier.size(28.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = stringResource(id = R.string.settings_delete_rule_title),
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
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

    // 5. Add Merchant Rule Dialog
    if (showAddRuleDialog) {
        var merchantInput by remember { mutableStateOf("") }
        var selectedCat by remember { mutableStateOf(allCategories.firstOrNull() ?: "Shopping") }
        var applyToPast by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showAddRuleDialog = false },
            title = {
                Text(
                    text = stringResource(id = R.string.settings_add_merchant_rule),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = merchantInput,
                        onValueChange = { merchantInput = it },
                        label = { Text(text = stringResource(id = R.string.settings_merchant_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(
                        text = stringResource(id = R.string.settings_select_category_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.financialColors.textSecondary,
                    )

                    CategoryChipSelector(
                        categories = allCategories,
                        selectedCategory = selectedCat,
                        onSelectCategory = { selectedCat = it },
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { applyToPast = !applyToPast },
                    ) {
                        Checkbox(
                            checked = applyToPast,
                            onCheckedChange = { applyToPast = it },
                            colors =
                                CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.financialColors.limeAccent,
                                    checkmarkColor = MaterialTheme.financialColors.onLimeAccent,
                                ),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(id = R.string.settings_apply_rule_to_past),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val m = merchantInput.trim()
                        if (m.isNotBlank()) {
                            userPreferences.setMerchantCategory(m, selectedCat)
                            rulesRevision++
                            if (applyToPast) {
                                onApplyMerchantRuleToTransactions?.invoke(m, selectedCat)
                            }
                        }
                        showAddRuleDialog = false
                    },
                    enabled = merchantInput.isNotBlank(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.financialColors.limeAccent,
                            contentColor = MaterialTheme.financialColors.onLimeAccent,
                        ),
                ) {
                    Text(text = stringResource(id = R.string.settings_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRuleDialog = false }) {
                    Text(text = stringResource(id = R.string.settings_cancel))
                }
            },
        )
    }

    // 6. Manage Categories Dialog
    if (showCategoriesDialog) {
        AlertDialog(
            onDismissRequest = { showCategoriesDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_categories_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = { showAddCategoryDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(id = R.string.settings_add_category),
                            tint = MaterialTheme.financialColors.limeAccent,
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { showAddCategoryDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.financialColors.paleLimeAddButton,
                                contentColor = MaterialTheme.financialColors.onPaleLimeAddButton,
                            ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.settings_add_category),
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (customCategories.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.settings_categories_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.financialColors.textSecondary,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(customCategories.toList(), key = { it }) { cat ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.financialColors.cardBackground,
                                    border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            CategoryBadge(categoryName = cat, size = 28.dp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = cat,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White,
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                userPreferences.removeCustomCategory(cat)
                                                categoriesRevision++
                                            },
                                            modifier = Modifier.size(28.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = stringResource(id = R.string.settings_delete_category_title),
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoriesDialog = false }) {
                    Text(text = stringResource(id = R.string.settings_close))
                }
            },
        )
    }

    // 7. Add Category Dialog
    if (showAddCategoryDialog) {
        var newCatInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = {
                Text(
                    text = stringResource(id = R.string.settings_add_category),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                OutlinedTextField(
                    value = newCatInput,
                    onValueChange = { newCatInput = it },
                    label = { Text(text = stringResource(id = R.string.settings_category_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = newCatInput.trim()
                        if (trimmed.isNotBlank()) {
                            userPreferences.addCustomCategory(trimmed)
                            categoriesRevision++
                        }
                        showAddCategoryDialog = false
                    },
                    enabled = newCatInput.isNotBlank(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.financialColors.limeAccent,
                            contentColor = MaterialTheme.financialColors.onLimeAccent,
                        ),
                ) {
                    Text(text = stringResource(id = R.string.settings_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text(text = stringResource(id = R.string.settings_cancel))
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
        color = MaterialTheme.financialColors.limeAccent,
        modifier = modifier.padding(start = 4.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector? = null,
    painter: Painter? = null,
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
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.financialColors.textSecondary,
                    modifier = Modifier.size(22.dp),
                )
            } else if (painter != null) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = MaterialTheme.financialColors.textSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.financialColors.textSecondary,
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AccountTypeSelector(
    selectedType: AccountType,
    onTypeSelected: (AccountType) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val types =
            listOf(
                AccountType.Savings to R.string.settings_type_savings,
                AccountType.CreditCard to R.string.settings_type_credit_card,
                AccountType.Wallet to R.string.settings_type_wallet,
                AccountType.Cash to R.string.settings_type_cash,
                AccountType.Current to R.string.settings_type_current,
            )
        types.forEach { (type, labelRes) ->
            val isSelected = selectedType == type
            FilterChip(
                selected = isSelected,
                onClick = { onTypeSelected(type) },
                label = { Text(text = stringResource(id = labelRes)) },
                colors =
                    FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.financialColors.limeAccent,
                        selectedLabelColor = MaterialTheme.financialColors.onLimeAccent,
                    ),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryChipSelector(
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        categories.take(12).forEach { cat ->
            val isSelected = selectedCategory.equals(cat, ignoreCase = true)
            Surface(
                shape = RoundedCornerShape(100.dp),
                color =
                    if (isSelected) {
                        MaterialTheme.financialColors.limeAccent
                    } else {
                        MaterialTheme.financialColors.cardBackground
                    },
                border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .clickable { onSelectCategory(cat) },
            ) {
                Text(
                    text = cat,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color =
                        if (isSelected) {
                            MaterialTheme.financialColors.onLimeAccent
                        } else {
                            Color.White
                        },
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
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
