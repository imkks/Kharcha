package com.kharcha.feature.settings.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.app.TimePickerDialog
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.flowOf
import com.kharcha.core.common.AccountAutoLinker
import com.kharcha.core.common.BankAccountMergeStore
import com.kharcha.core.common.CsvExportHelper
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.common.UserPreferences
import com.kharcha.core.designsystem.R as DesignR
import com.kharcha.core.designsystem.component.CategoryBadge
import com.kharcha.core.designsystem.component.DEFAULT_CREDIT_CATEGORIES
import com.kharcha.core.designsystem.component.DEFAULT_SPEND_CATEGORIES
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.designsystem.theme.LocalIncomeHidden
import com.kharcha.core.designsystem.theme.LocalIncomeRevealed
import com.kharcha.core.model.Account
import com.kharcha.core.model.AccountType
import com.kharcha.core.model.Transaction
import com.kharcha.feature.settings.R
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MoreScreen(
    transactions: List<Transaction>,
    userPreferences: UserPreferences,
    onRescanSmsInbox: (String) -> Unit,
    modifier: Modifier = Modifier,
    onUpdateMonthlyBudget: (Long) -> Unit = {},
    accounts: List<Account> = emptyList(),
    onSaveAccount: (Account) -> Unit = {},
    onMergeAccounts: ((primaryAccountId: Long, redundantAccountIds: Set<Long>) -> Unit)? = null,
    onMergeDuplicateAccounts: ((sourceAccountId: Long, targetAccountId: Long) -> Unit)? = null,
    onApplyMerchantRuleToTransactions: ((merchant: String, category: String) -> Unit)? = null,
    onWeeklySummaryClick: () -> Unit = {},
    onReportSmsClick: () -> Unit = {},
    onAccountClick: (Account) -> Unit = {},
    onTransactionClick: (Transaction) -> Unit = {},
    onNotificationSettingsChanged: () -> Unit = {},
) {
    val context = LocalContext.current
    var showPreferencesScreen by remember { mutableStateOf(false) }
    var showManageAccountsPage by remember { mutableStateOf(false) }
    var showLinkAccountsPage by remember { mutableStateOf(false) }

    // State for Dialogs and Sheets
    var showAccountsDialog by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var selectedAccountForSettings by remember { mutableStateOf<Account?>(null) }
    var editingAccount by remember { mutableStateOf<Account?>(null) }

    var showLinkAccountsSheet by remember { mutableStateOf(false) }
    var primaryAccountForLinking by remember { mutableStateOf<Account?>(null) }

    var showSpendCategoriesSheet by remember { mutableStateOf(false) }
    var showAddSpendCategoryDialog by remember { mutableStateOf(false) }

    var showCreditCategoriesSheet by remember { mutableStateOf(false) }
    var showAddCreditCategoryDialog by remember { mutableStateOf(false) }

    var showRulesDialog by remember { mutableStateOf(false) }
    var showAddRuleDialog by remember { mutableStateOf(false) }

    var showTransferThresholdDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showRemindersDialog by remember { mutableStateOf(false) }
    var showManageTagsSheet by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var showRescanSmsDialog by remember { mutableStateOf(false) }
    var selectedRescanWindow by rememberSaveable { mutableStateOf("90") }
    var currentUserName by remember { mutableStateOf(userPreferences.userName) }

    val rescanWorkInfoFlow = remember(context) {
        runCatching { WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(RESCAN_WORK_NAME) }
            .getOrDefault(flowOf(emptyList()))
    }
    val rescanWorkInfos by rescanWorkInfoFlow.collectAsState(initial = emptyList())
    val rescanWorkInfo = rescanWorkInfos.firstOrNull()

    var categoriesRevision by remember { mutableIntStateOf(0) }
    val customSpendCategories =
        remember(categoriesRevision, showSpendCategoriesSheet) {
            userPreferences.getCustomSpendCategories()
        }
    val allSpendCategories =
        remember(customSpendCategories) {
            (DEFAULT_SPEND_CATEGORIES + customSpendCategories).distinct()
        }

    val customCreditCategories =
        remember(categoriesRevision, showCreditCategoriesSheet) {
            userPreferences.getCustomCreditCategories()
        }
    val allCreditCategories =
        remember(customCreditCategories) {
            (DEFAULT_CREDIT_CATEGORIES + customCreditCategories).distinct()
        }

    var rulesRevision by remember { mutableIntStateOf(0) }
    val rules = remember(rulesRevision, showRulesDialog) { userPreferences.getMerchantCategoryRules() }

    var isMoneyManagerDefault by remember {
        mutableStateOf(userPreferences.isLaunchInMoneyManagerEnabled)
    }

    if (showManageAccountsPage) {
        BackHandler { showManageAccountsPage = false }
        ManageAccountsScreenView(
            accounts = accounts,
            onAccountClick = onAccountClick,
            onSaveAccount = onSaveAccount,
            onMergeDuplicateAccounts = onMergeDuplicateAccounts,
            onAddAccountClick = { showAddAccountDialog = true },
            onBack = { showManageAccountsPage = false },
            modifier = modifier,
        )
    } else if (showLinkAccountsPage) {
        BackHandler { showLinkAccountsPage = false }
        LinkAccountsScreenView(
            accounts = accounts,
            onMergeAccounts = onMergeAccounts,
            onLinkAccountForPrimary = { primary ->
                primaryAccountForLinking = primary
                showLinkAccountsSheet = true
            },
            onLinkNewAccountClick = {
                val primary = accounts.firstOrNull {
                    (it.type == AccountType.Savings || it.type == AccountType.Current) && it.linkedPrimaryAccountId == null
                } ?: accounts.firstOrNull { it.linkedPrimaryAccountId == null } ?: accounts.firstOrNull()
                if (primary != null) {
                    primaryAccountForLinking = primary
                    showLinkAccountsSheet = true
                }
            },
            onBack = { showLinkAccountsPage = false },
            modifier = modifier,
        )
    } else if (showPreferencesScreen) {
        BackHandler { showPreferencesScreen = false }
        PreferencesScreenView(
            accounts = accounts,
            userPreferences = userPreferences,
            isMoneyManagerDefault = isMoneyManagerDefault,
            onToggleMoneyManagerDefault = {
                isMoneyManagerDefault = it
                userPreferences.isLaunchInMoneyManagerEnabled = it
            },
            onManageAccountsClick = { showManageAccountsPage = true },
            onLinkAccountsClick = { showLinkAccountsPage = true },
            onSpendCategoriesClick = { showSpendCategoriesSheet = true },
            onCreditCategoriesClick = { showCreditCategoriesSheet = true },
            onRulesClick = { showRulesDialog = true },
            onMarkTransferClick = { showTransferThresholdDialog = true },
            onRemindersClick = { showRemindersDialog = true },
            onTagsClick = { showManageTagsSheet = true },
            onNotificationsClick = { showNotificationsDialog = true },
            onBackupClick = { showExportDialog = true },
            onSecurityClick = { showSecurityDialog = true },
            onRescanSmsClick = { showRescanSmsDialog = true },
            onBack = { showPreferencesScreen = false },
            modifier = modifier,
        )
    } else {
        // Main Account Screen (Screenshot 1)
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.settings_account_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                        ),
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = modifier.fillMaxSize(),
        ) { innerPadding ->
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 1. Profile Header Row
                item {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.financialColors.limeAccent,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = currentUserName.take(1).uppercase(Locale.ROOT).ifEmpty { "K" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.financialColors.onLimeAccent,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text =
                                currentUserName.ifBlank {
                                    stringResource(id = R.string.settings_user_name_missing)
                                },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }

                // 2. Contact Details Card
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
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Email row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(id = DesignR.drawable.ic_copy),
                                        contentDescription = null,
                                        tint = MaterialTheme.financialColors.textSecondary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = stringResource(id = R.string.settings_profile_details),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                    )
                                }
                                Text(
                                    text = stringResource(id = R.string.settings_edit),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.financialColors.limeAccent,
                                    modifier = Modifier.clickable { showEditProfileDialog = true },
                                )
                            }
                        }
                    }
                }

                // 3. Expense Tracking Section Header
                item {
                    Text(
                        text = stringResource(id = R.string.settings_expense_tracking_section),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                    )
                }

                // 4. Expense Tracking Actions Card
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
                            SettingsNavRow(
                                iconRes = DesignR.drawable.ic_settings,
                                iconTint = MaterialTheme.financialColors.chartBlue,
                                title = stringResource(id = R.string.settings_preferences_title),
                                onClick = { showPreferencesScreen = true },
                            )
                            HorizontalDivider(color = MaterialTheme.financialColors.hairlineDivider)
                            SettingsNavRow(
                                iconRes = DesignR.drawable.ic_bar_chart,
                                iconTint = MaterialTheme.financialColors.chartBlue,
                                title = stringResource(id = R.string.settings_weekly_summary_title),
                                onClick = onWeeklySummaryClick,
                            )
                            HorizontalDivider(color = MaterialTheme.financialColors.hairlineDivider)
                            SettingsNavRow(
                                iconRes = DesignR.drawable.ic_copy,
                                iconTint = MaterialTheme.financialColors.chartBlue,
                                title = stringResource(id = R.string.settings_export_statement_title),
                                onClick = { showExportDialog = true },
                            )
                            HorizontalDivider(color = MaterialTheme.financialColors.hairlineDivider)
                            SettingsNavRow(
                                iconRes = DesignR.drawable.ic_arrow_outward,
                                iconTint = MaterialTheme.financialColors.chartBlue,
                                title = stringResource(id = R.string.settings_report_sms_title),
                                onClick = onReportSmsClick,
                            )
                        }
                    }
                }
            }
        }
    }

    // --- SHEETS & DIALOGS ---

    // 1. Export Statement Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(text = stringResource(id = R.string.settings_export_statement_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val csv = CsvExportHelper.generateTransactionsCsv(transactions)
                            val filename = "kharcha_transactions_${System.currentTimeMillis()}.csv"
                            shareTextFile(context, csv, filename, "text/csv")
                            showExportDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.financialColors.cardBackground,
                                contentColor = Color.White,
                            ),
                    ) {
                        Text(text = stringResource(id = R.string.settings_export_csv_title))
                    }
                    Button(
                        onClick = {
                            val json = CsvExportHelper.generateLlmBatchExportJson(transactions)
                            val filename = "kharcha_llm_batch_${System.currentTimeMillis()}.json"
                            shareTextFile(context, json, filename, "application/json")
                            showExportDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.financialColors.cardBackground,
                                contentColor = Color.White,
                            ),
                    ) {
                        Text(text = stringResource(id = R.string.settings_export_llm_title))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(text = stringResource(id = R.string.settings_close))
                }
            },
        )
    }

    // 2. Link Accounts Bottom Sheet (Screenshot 3)
    if (showLinkAccountsSheet && primaryAccountForLinking != null) {
        var currentPrimary by remember(primaryAccountForLinking) { mutableStateOf(primaryAccountForLinking!!) }
        val availablePrimaries =
            remember(accounts) {
                val bankAccounts = accounts.filter {
                    (it.type == AccountType.Savings || it.type == AccountType.Current) && it.linkedPrimaryAccountId == null
                }
                bankAccounts.ifEmpty { accounts.filter { it.linkedPrimaryAccountId == null } }
            }
        val candidateAccounts =
            remember(accounts, currentPrimary) {
                val primaryBank = AccountAutoLinker.normalizeBank(currentPrimary.bankName)
                    ?: AccountAutoLinker.extractBankFromName(currentPrimary.name)
                accounts.filter { it.id != currentPrimary.id }
                    .sortedByDescending { candidate ->
                        val candidateBank = AccountAutoLinker.normalizeBank(candidate.bankName)
                            ?: AccountAutoLinker.extractBankFromName(candidate.name)
                        val isSameBank = primaryBank != null && candidateBank != null &&
                            primaryBank.equals(candidateBank, ignoreCase = true)
                        if (candidate.linkedPrimaryAccountId == currentPrimary.id) 2 else if (isSameBank) 1 else 0
                    }
            }
        var selectedSubAccountIds by remember(currentPrimary, candidateAccounts) {
            mutableStateOf(
                candidateAccounts.filter { it.linkedPrimaryAccountId == currentPrimary.id }.map { it.id }.toSet(),
            )
        }

        ModalBottomSheet(
            onDismissRequest = { showLinkAccountsSheet = false },
            containerColor = MaterialTheme.financialColors.cardBackground,
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
                    color = MaterialTheme.financialColors.hairlineDivider,
                    shape = RoundedCornerShape(2.dp),
                ) {
                    Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
                }
            },
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                        .padding(bottom = 24.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_link_accounts_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    TextButton(
                        onClick = {
                            val matchingIds =
                                candidateAccounts.filter {
                                    it.bankName?.equals(currentPrimary.bankName, ignoreCase = true) == true ||
                                        it.name.contains(currentPrimary.bankName ?: "", ignoreCase = true)
                                }.map { it.id }.toSet()
                            selectedSubAccountIds = selectedSubAccountIds + matchingIds
                        },
                    ) {
                        Text(
                            text = stringResource(id = R.string.settings_auto_link_title),
                            color = MaterialTheme.financialColors.limeAccent,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                val primaryLabel = "${currentPrimary.name} - ${currentPrimary.last4 ?: "••••"}"
                Text(
                    text = stringResource(id = R.string.settings_link_accounts_dialog_desc, primaryLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.financialColors.textSecondary,
                )

                if (availablePrimaries.size > 1) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = stringResource(id = R.string.settings_select_primary_bank),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.financialColors.textSecondary,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        availablePrimaries.forEach { bankAcc ->
                            val isSelected = bankAcc.id == currentPrimary.id
                            val bg =
                                if (isSelected) {
                                    MaterialTheme.financialColors.limeAccent
                                } else {
                                    MaterialTheme.financialColors.raisedRowBackground
                                }
                            val borderCol =
                                if (isSelected) {
                                    MaterialTheme.financialColors.limeAccent
                                } else {
                                    MaterialTheme.financialColors.hairlineDivider
                                }
                            Surface(
                                shape = CircleShape,
                                color = bg,
                                border = BorderStroke(1.dp, borderCol),
                                modifier = Modifier.clickable { currentPrimary = bankAcc },
                            ) {
                                val textCol =
                                    if (isSelected) {
                                        MaterialTheme.financialColors.onLimeAccent
                                    } else {
                                        MaterialTheme.financialColors.textPrimary
                                    }
                                Text(
                                    text = "${bankAcc.name} (${bankAcc.last4 ?: "••••"})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = textCol,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary account row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFC0392B),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = DesignR.drawable.ic_bank),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentPrimary.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            text = currentPrimary.last4 ?: "••••",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.financialColors.textSecondary,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.financialColors.limeAccent.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = "Primary Account",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.financialColors.limeAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }

                // Sub-accounts tree branch
                if (candidateAccounts.isNotEmpty()) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, top = 8.dp),
                    ) {
                        candidateAccounts.forEach { candidate ->
                            val isChecked = selectedSubAccountIds.contains(candidate.id)
                            val isSameBank =
                                candidate.bankName?.equals(currentPrimary.bankName, ignoreCase = true) == true ||
                                    candidate.name.contains(currentPrimary.bankName ?: "", ignoreCase = true)
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedSubAccountIds =
                                                if (isChecked) {
                                                    selectedSubAccountIds - candidate.id
                                                } else {
                                                    selectedSubAccountIds + candidate.id
                                                }
                                        }
                                        .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Tree line graphic
                                Canvas(modifier = Modifier.size(width = 24.dp, height = 36.dp)) {
                                    drawLine(
                                        color = Color(0xFF555555),
                                        start = Offset(0f, 0f),
                                        end = Offset(0f, size.height / 2),
                                        strokeWidth = 3f,
                                    )
                                    drawLine(
                                        color = Color(0xFF555555),
                                        start = Offset(0f, size.height / 2),
                                        end = Offset(size.width, size.height / 2),
                                        strokeWidth = 3f,
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                val isCc = candidate.type == AccountType.CreditCard
                                val iconRes =
                                    if (isCc) {
                                        DesignR.drawable.ic_category_credit
                                    } else {
                                        DesignR.drawable.ic_bank
                                    }
                                Surface(
                                    modifier = Modifier.size(34.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isCc) Color(0xFF1E3A8A) else Color(0xFFC0392B),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(id = iconRes),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = candidate.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = candidate.last4 ?: "XXXX",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.financialColors.textSecondary,
                                        )
                                        if (isChecked) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.financialColors.limeAccent.copy(alpha = 0.2f),
                                            ) {
                                                Text(
                                                    text = stringResource(id = R.string.settings_status_linked),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.financialColors.limeAccent,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                )
                                            }
                                        } else if (isSameBank) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.financialColors.chartBlue.copy(alpha = 0.2f),
                                            ) {
                                                Text(
                                                    text = stringResource(id = R.string.settings_suggested_match),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.financialColors.chartBlueHighlight,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                )
                                            }
                                        } else {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.financialColors.raisedRowBackground,
                                            ) {
                                                Text(
                                                    text = stringResource(id = R.string.settings_status_unlinked),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.financialColors.textSecondary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        selectedSubAccountIds =
                                            if (checked) {
                                                selectedSubAccountIds + candidate.id
                                            } else {
                                                selectedSubAccountIds - candidate.id
                                            }
                                    },
                                    colors =
                                        CheckboxDefaults.colors(
                                            checkedColor = MaterialTheme.financialColors.limeAccent,
                                            checkmarkColor = MaterialTheme.financialColors.onLimeAccent,
                                        ),
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons: Cancel and Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { showLinkAccountsSheet = false }) {
                        Text(
                            text = "✕ " + stringResource(id = R.string.settings_cancel),
                            color = MaterialTheme.financialColors.limeAccent,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Button(
                        onClick = {
                            onMergeAccounts?.invoke(currentPrimary.id, selectedSubAccountIds)
                            showLinkAccountsSheet = false
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.financialColors.limeAccent,
                                contentColor = MaterialTheme.financialColors.onLimeAccent,
                            ),
                        shape = RoundedCornerShape(100.dp),
                    ) {
                        Text(
                            text = "✓ " + stringResource(id = R.string.settings_save),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }

    // 3. Account Settings Bottom Sheet (Screenshot 2)
    if (selectedAccountForSettings != null) {
        val targetAccount = selectedAccountForSettings!!
        ModalBottomSheet(
            onDismissRequest = { selectedAccountForSettings = null },
            containerColor = MaterialTheme.financialColors.cardBackground,
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
                    color = MaterialTheme.financialColors.hairlineDivider,
                    shape = RoundedCornerShape(2.dp),
                ) {
                    Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
                }
            },
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                        .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.settings_account_settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 8.dp),
                )

                // 1. Expense Toggle Row
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val updated =
                                    targetAccount.copy(isCountedAsExpense = !targetAccount.isCountedAsExpense)
                                onSaveAccount(updated)
                                selectedAccountForSettings = updated
                            }
                            .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.settings_account_expense_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(id = R.string.settings_account_expense_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.financialColors.textSecondary,
                        )
                    }
                    Switch(
                        checked = targetAccount.isCountedAsExpense,
                        onCheckedChange = { isChecked ->
                            val updated = targetAccount.copy(isCountedAsExpense = isChecked)
                            onSaveAccount(updated)
                            selectedAccountForSettings = updated
                        },
                        colors =
                            SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.financialColors.limeAccent,
                            ),
                    )
                }

                HorizontalDivider(color = MaterialTheme.financialColors.hairlineDivider)

                // 2. Edit Account Row
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                editingAccount = targetAccount
                                selectedAccountForSettings = null
                            }
                            .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.settings_account_edit_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(id = R.string.settings_account_edit_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.financialColors.textSecondary,
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.financialColors.textSecondary,
                    )
                }

                HorizontalDivider(color = MaterialTheme.financialColors.hairlineDivider)

                // 3. Hide Account Row
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val updated = targetAccount.copy(isArchived = !targetAccount.isArchived)
                                onSaveAccount(updated)
                                selectedAccountForSettings = null
                            }
                            .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.settings_account_hide_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(id = R.string.settings_account_hide_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.financialColors.textSecondary,
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.financialColors.textSecondary,
                    )
                }

                HorizontalDivider(color = MaterialTheme.financialColors.hairlineDivider)

                // 4. Link / Unlink Sub-accounts Row
                if (targetAccount.linkedPrimaryAccountId != null) {
                    val parentBank = accounts.find { it.id == targetAccount.linkedPrimaryAccountId }
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val bankTitle = parentBank?.name ?: "Bank"
                            Text(
                                text = stringResource(id = R.string.settings_linked_to_format, bankTitle),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Card is currently linked to ${parentBank?.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.financialColors.textSecondary,
                            )
                        }
                        TextButton(
                            onClick = {
                                val unlinked = targetAccount.copy(linkedPrimaryAccountId = null)
                                onSaveAccount(unlinked)
                                selectedAccountForSettings = null
                            },
                        ) {
                            Text(
                                text = stringResource(id = R.string.settings_unlink_action),
                                color = MaterialTheme.financialColors.warning,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                } else {
                    val linkedChildren = accounts.filter { it.linkedPrimaryAccountId == targetAccount.id }
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    primaryAccountForLinking = targetAccount
                                    showLinkAccountsSheet = true
                                    selectedAccountForSettings = null
                                }
                                .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.settings_link_accounts_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text =
                                    if (linkedChildren.isNotEmpty()) {
                                        "${linkedChildren.size} credit card(s) linked"
                                    } else {
                                        "Link cards or sub-accounts"
                                    },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.financialColors.textSecondary,
                            )
                        }
                        Icon(
                            painter = painterResource(id = DesignR.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.financialColors.textSecondary,
                        )
                    }
                }
            }
        }
    }

    // 4. Edit Account Dialog
    if (editingAccount != null) {
        val target = editingAccount!!
        var editName by remember { mutableStateOf(target.name) }
        var editLast4 by remember { mutableStateOf(target.last4 ?: "") }

        AlertDialog(
            onDismissRequest = { editingAccount = null },
            title = { Text(text = stringResource(id = R.string.settings_account_edit_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(text = stringResource(id = R.string.settings_account_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = editLast4,
                        onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) editLast4 = it },
                        label = { Text(text = stringResource(id = R.string.settings_account_last4_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = target.copy(name = editName.trim(), last4 = editLast4.trim().ifBlank { null })
                        onSaveAccount(updated)
                        editingAccount = null
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
                TextButton(onClick = { editingAccount = null }) {
                    Text(text = stringResource(id = R.string.settings_cancel))
                }
            },
        )
    }

    // 5. Spend Categories Sheet
    if (showSpendCategoriesSheet) {
        CategoriesManagerSheet(
            title = stringResource(id = R.string.settings_spend_categories_title),
            allCategories = allSpendCategories,
            customCategories = customSpendCategories,
            onAddCategoryClick = { showAddSpendCategoryDialog = true },
            onDeleteCustomCategory = { cat ->
                userPreferences.removeCustomSpendCategory(cat)
                categoriesRevision++
            },
            onDismiss = { showSpendCategoriesSheet = false },
        )
    }

    // 6. Add Spend Category Dialog
    if (showAddSpendCategoryDialog) {
        var catInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddSpendCategoryDialog = false },
            title = { Text(text = stringResource(id = R.string.settings_add_category)) },
            text = {
                OutlinedTextField(
                    value = catInput,
                    onValueChange = { catInput = it },
                    label = { Text(text = stringResource(id = R.string.settings_category_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = catInput.trim()
                        if (trimmed.isNotBlank()) {
                            userPreferences.addCustomSpendCategory(trimmed)
                            categoriesRevision++
                        }
                        showAddSpendCategoryDialog = false
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
                TextButton(onClick = { showAddSpendCategoryDialog = false }) {
                    Text(text = stringResource(id = R.string.settings_cancel))
                }
            },
        )
    }

    // 7. Credit Categories Sheet
    if (showCreditCategoriesSheet) {
        CategoriesManagerSheet(
            title = stringResource(id = R.string.settings_credit_categories_title),
            allCategories = allCreditCategories,
            customCategories = customCreditCategories,
            onAddCategoryClick = { showAddCreditCategoryDialog = true },
            onDeleteCustomCategory = { cat ->
                userPreferences.removeCustomCreditCategory(cat)
                categoriesRevision++
            },
            onDismiss = { showCreditCategoriesSheet = false },
        )
    }

    // 8. Add Credit Category Dialog
    if (showAddCreditCategoryDialog) {
        var catInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCreditCategoryDialog = false },
            title = { Text(text = stringResource(id = R.string.settings_add_category)) },
            text = {
                OutlinedTextField(
                    value = catInput,
                    onValueChange = { catInput = it },
                    label = { Text(text = stringResource(id = R.string.settings_category_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = catInput.trim()
                        if (trimmed.isNotBlank()) {
                            userPreferences.addCustomCreditCategory(trimmed)
                            categoriesRevision++
                        }
                        showAddCreditCategoryDialog = false
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
                TextButton(onClick = { showAddCreditCategoryDialog = false }) {
                    Text(text = stringResource(id = R.string.settings_cancel))
                }
            },
        )
    }

    // 9. Manage Accounts Dialog
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
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
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
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedAccountForSettings = acc
                                            },
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
                                                val parent =
                                                    if (acc.linkedPrimaryAccountId != null) {
                                                        accounts.find { it.id == acc.linkedPrimaryAccountId }
                                                    } else {
                                                        null
                                                    }
                                                val linkedCount = accounts.count { it.linkedPrimaryAccountId == acc.id }
                                                val linkTag = when {
                                                    parent != null -> " • Linked to ${parent.name}"
                                                    linkedCount > 0 -> " • $linkedCount cards linked"
                                                    else -> ""
                                                }
                                                val sub = "${acc.bankName ?: ""} ••${acc.last4 ?: "----"}$linkTag"
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

    // 10. Add Account Dialog
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

    // 11. Mark as Transfer Threshold Dialog
    if (showTransferThresholdDialog) {
        var thresholdInput by remember {
            mutableStateOf((userPreferences.autoTransferThresholdPaise / 100L).toString())
        }
        AlertDialog(
            onDismissRequest = { showTransferThresholdDialog = false },
            title = { Text(text = stringResource(id = R.string.settings_mark_transfer_title)) },
            text = {
                OutlinedTextField(
                    value = thresholdInput,
                    onValueChange = { thresholdInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text(text = "Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = thresholdInput.toLongOrNull() ?: 10000L
                        userPreferences.autoTransferThresholdPaise = parsed * 100L
                        showTransferThresholdDialog = false
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
                TextButton(onClick = { showTransferThresholdDialog = false }) {
                    Text(text = stringResource(id = R.string.settings_cancel))
                }
            },
        )
    }

    if (showEditProfileDialog) {
        var nameInput by remember { mutableStateOf(currentUserName) }
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text(text = stringResource(id = R.string.settings_edit_profile_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text(text = stringResource(id = R.string.settings_user_name_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        userPreferences.userName = nameInput.trim()
                        currentUserName = userPreferences.userName
                        showEditProfileDialog = false
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
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text(text = stringResource(id = R.string.settings_cancel))
                }
            },
        )
    }

    if (showRemindersDialog) {
        var isDailyReminderEnabled by remember { mutableStateOf(true) }
        var isDuesReminderEnabled by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { showRemindersDialog = false },
            title = { Text(text = stringResource(id = R.string.settings_reminders_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.settings_reminders_daily_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                            Text(
                                text = stringResource(id = R.string.settings_reminders_daily_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.financialColors.textSecondary,
                            )
                        }
                        Switch(
                            checked = isDailyReminderEnabled,
                            onCheckedChange = { isDailyReminderEnabled = it },
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.financialColors.hairlineDivider)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.settings_reminders_dues_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                            Text(
                                text = stringResource(id = R.string.settings_reminders_dues_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.financialColors.textSecondary,
                            )
                        }
                        Switch(
                            checked = isDuesReminderEnabled,
                            onCheckedChange = { isDuesReminderEnabled = it },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRemindersDialog = false }) {
                    Text(text = stringResource(id = R.string.settings_close))
                }
            },
        )
    }

    if (showNotificationsDialog) {
        var isNotifsEnabled by remember { mutableStateOf(userPreferences.isExpenseNotificationsEnabled) }
        var hideSmsNotifications by remember { mutableStateOf(userPreferences.hideDefaultSmsNotifications) }
        var dailySummary by remember { mutableStateOf(userPreferences.isDailySummaryEnabled) }
        var dailyTime by remember { mutableIntStateOf(userPreferences.dailySummaryMinuteOfDay) }
        var weeklySummary by remember { mutableStateOf(userPreferences.isWeeklySummaryEnabled) }
        var weeklyTime by remember { mutableIntStateOf(userPreferences.weeklySummaryMinuteOfDay) }
        var billReminders by remember { mutableStateOf(userPreferences.isBillReminderEnabled) }
        val context = LocalContext.current
        val notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
        fun timeLabel(minutes: Int): String = String.format(Locale.getDefault(), "%02d:%02d", minutes / 60, minutes % 60)
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = { Text(text = stringResource(id = R.string.settings_notifications_reminders_title)) },
            text = {
                Column(
                    modifier = Modifier.height(520.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SettingsToggleRow(stringResource(R.string.settings_expense_notifications_title), stringResource(R.string.settings_expense_notifications_subtitle), isNotifsEnabled) {
                        isNotifsEnabled = it; userPreferences.isExpenseNotificationsEnabled = it
                    }
                    SettingsValueRow(
                        title = stringResource(R.string.settings_notification_permission_title),
                        value = stringResource(
                            if (notificationsAllowed) R.string.settings_notification_permission_allowed
                            else R.string.settings_notification_permission_blocked,
                        ),
                    ) {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                        )
                    }
                    SettingsToggleRow(stringResource(R.string.settings_hide_sms_title), stringResource(R.string.settings_hide_sms_desc), hideSmsNotifications) {
                        if (it) {
                            userPreferences.hideDefaultSmsNotifications = true
                            hideSmsNotifications = true
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        } else {
                            hideSmsNotifications = false
                            userPreferences.hideDefaultSmsNotifications = false
                        }
                    }
                    SettingsToggleRow(stringResource(R.string.settings_daily_summary_title), stringResource(R.string.settings_toggle_status, if (dailySummary) stringResource(R.string.settings_on) else stringResource(R.string.settings_off)), dailySummary) {
                        dailySummary = it; userPreferences.isDailySummaryEnabled = it; onNotificationSettingsChanged()
                    }
                    SettingsValueRow(stringResource(R.string.settings_daily_summary_time), timeLabel(dailyTime)) {
                        TimePickerDialog(context, { _, hour, minute -> dailyTime = hour * 60 + minute; userPreferences.dailySummaryMinuteOfDay = dailyTime; onNotificationSettingsChanged() }, dailyTime / 60, dailyTime % 60, false).show()
                    }
                    SettingsToggleRow(stringResource(R.string.settings_weekly_summary_title), stringResource(R.string.settings_toggle_status, if (weeklySummary) stringResource(R.string.settings_on) else stringResource(R.string.settings_off)), weeklySummary) {
                        weeklySummary = it; userPreferences.isWeeklySummaryEnabled = it; onNotificationSettingsChanged()
                    }
                    SettingsValueRow(stringResource(R.string.settings_weekly_summary_time), timeLabel(weeklyTime)) {
                        TimePickerDialog(context, { _, hour, minute -> weeklyTime = hour * 60 + minute; userPreferences.weeklySummaryMinuteOfDay = weeklyTime; onNotificationSettingsChanged() }, weeklyTime / 60, weeklyTime % 60, false).show()
                    }
                    SettingsValueRow(stringResource(R.string.settings_bill_reminders), stringResource(R.string.settings_bill_reminders_desc)) {
                        billReminders = !billReminders; userPreferences.isBillReminderEnabled = billReminders
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) {
                    Text(text = stringResource(id = R.string.settings_close))
                }
            },
        )
    }

    if (showSecurityDialog) {
        val incomeHiddenState = LocalIncomeHidden.current
        val incomeRevealedState = LocalIncomeRevealed.current
        var hideIncome by remember { mutableStateOf(userPreferences.isIncomeHidden) }
        var appLock by remember { mutableStateOf(userPreferences.isAppLockEnabled) }
        AlertDialog(
            onDismissRequest = { showSecurityDialog = false },
            title = { Text(text = stringResource(id = R.string.settings_security_dialog_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingsToggleRow(stringResource(R.string.settings_app_lock_title), stringResource(R.string.settings_app_lock_desc), appLock) {
                        if (!it) {
                            appLock = false
                            userPreferences.isAppLockEnabled = false
                        } else {
                            val activity = context.findFragmentActivity()
                            val available = BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
                            if (activity != null && available) {
                                val prompt = BiometricPrompt(activity, activity.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
                                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                        appLock = true
                                        userPreferences.isAppLockEnabled = true
                                    }
                                })
                                prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
                                    .setTitle(context.getString(R.string.settings_app_lock_title))
                                    .setSubtitle(context.getString(R.string.settings_app_lock_desc))
                                    .setNegativeButtonText(context.getString(R.string.settings_cancel))
                                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                                    .build())
                            }
                        }
                    }
                    SettingsToggleRow(stringResource(R.string.settings_hide_income_title), stringResource(R.string.settings_hide_income_desc), hideIncome) {
                        hideIncome = it
                        userPreferences.isIncomeHidden = it
                        incomeHiddenState?.value = it
                        if (!it) incomeRevealedState?.value = false
                    }
                    HorizontalDivider(color = MaterialTheme.financialColors.hairlineDivider)
                    Text(
                        text = stringResource(id = R.string.settings_security_offline_notice),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.financialColors.textSecondary,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSecurityDialog = false }) {
                    Text(text = stringResource(id = R.string.settings_close))
                }
            },
        )
    }

    if (showRescanSmsDialog) {
        val isRescanActive = rescanWorkInfo?.state == WorkInfo.State.ENQUEUED || rescanWorkInfo?.state == WorkInfo.State.RUNNING
        val progress = rescanWorkInfo?.progress?.getFloat("progress", 0f) ?: 0f
        val processed = rescanWorkInfo?.progress?.getInt("processedCount", 0) ?: 0
        AlertDialog(
            onDismissRequest = { showRescanSmsDialog = false },
            title = { Text(stringResource(R.string.settings_rescan_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.settings_rescan_window_prompt))
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("30", "90", "180", "all").forEach { code ->
                            val label = when (code) {
                                "30" -> stringResource(R.string.settings_rescan_window_30)
                                "90" -> stringResource(R.string.settings_rescan_window_90)
                                "180" -> stringResource(R.string.settings_rescan_window_180)
                                else -> stringResource(R.string.settings_rescan_window_all)
                            }
                            FilterChip(
                                selected = selectedRescanWindow == code,
                                onClick = { selectedRescanWindow = code },
                                label = { Text(label) },
                            )
                        }
                    }
                    val statusText = when (rescanWorkInfo?.state) {
                        WorkInfo.State.ENQUEUED -> stringResource(R.string.settings_rescan_queued)
                        WorkInfo.State.RUNNING -> stringResource(R.string.settings_rescan_progress, (progress * 100).toInt(), processed)
                        WorkInfo.State.SUCCEEDED -> stringResource(
                            R.string.settings_rescan_complete,
                            rescanWorkInfo.outputData.getInt("processed_count", processed),
                        )
                        WorkInfo.State.FAILED -> stringResource(R.string.settings_rescan_failed)
                        WorkInfo.State.CANCELLED -> stringResource(R.string.settings_rescan_cancelled)
                        else -> stringResource(R.string.settings_rescan_ready)
                    }
                    Text(statusText, color = MaterialTheme.financialColors.textSecondary)
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isRescanActive,
                    onClick = { onRescanSmsInbox(selectedRescanWindow) },
                ) {
                    Text(stringResource(if (isRescanActive) R.string.settings_rescan_queued_button else R.string.settings_rescan_start))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRescanSmsDialog = false }) {
                    Text(stringResource(R.string.settings_close))
                }
            },
        )
    }

    if (showManageTagsSheet) {
        val allTagsWithCounts = remember(transactions) {
            val counts = mutableMapOf<String, Int>()
            transactions.forEach { txn ->
                val note = txn.note ?: ""
                val regex = Regex("#[A-Za-z0-9_]+")
                regex.findAll(note).forEach { match ->
                    val tag = match.value
                    counts[tag] = (counts[tag] ?: 0) + 1
                }
            }
            val defaults = listOf("#rent", "Family", "Friends", "Office", "Online", "School")
            defaults.forEach { tag -> counts.putIfAbsent(tag, 0) }
            counts.toList().sortedByDescending { it.second }
        }

        ModalBottomSheet(
            onDismissRequest = { showManageTagsSheet = false },
            containerColor = MaterialTheme.financialColors.cardBackground,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.settings_tags_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                if (allTagsWithCounts.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.settings_tags_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.financialColors.textSecondary,
                    )
                } else {
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        allTagsWithCounts.forEach { (tag, count) ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.financialColors.raisedRowBackground,
                                border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                            ) {
                                Text(
                                    text = "$tag ($count)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val RESCAN_WORK_NAME = "sms_inbox_rescan"

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.financialColors.textSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsValueRow(title: String, value: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.financialColors.textSecondary)
    }
}

/**
 * Preferences Screen View (Screenshot 4).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreferencesScreenView(
    accounts: List<Account>,
    userPreferences: UserPreferences,
    isMoneyManagerDefault: Boolean,
    onToggleMoneyManagerDefault: (Boolean) -> Unit,
    onManageAccountsClick: () -> Unit,
    onLinkAccountsClick: () -> Unit,
    onSpendCategoriesClick: () -> Unit,
    onCreditCategoriesClick: () -> Unit,
    onRulesClick: () -> Unit,
    onMarkTransferClick: () -> Unit,
    onRemindersClick: () -> Unit = {},
    onTagsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onBackupClick: () -> Unit = {},
    onSecurityClick: () -> Unit = {},
    onRescanSmsClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_preferences_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.settings_close),
                            tint = Color.White,
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
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
                        SettingsGroupHeading(stringResource(R.string.settings_section_general))
                        // 1. Launch in Money Manager Row (Screenshot 4)
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleMoneyManagerDefault(!isMoneyManagerDefault) }
                                    .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    painter = painterResource(id = DesignR.drawable.ic_bar_chart),
                                    contentDescription = null,
                                    tint = MaterialTheme.financialColors.textSecondary,
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = stringResource(id = R.string.settings_launch_money_manager_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White,
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(id = R.string.settings_launch_money_manager_subtitle),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.financialColors.textSecondary,
                                    )
                                }
                            }
                            Switch(
                                checked = isMoneyManagerDefault,
                                onCheckedChange = onToggleMoneyManagerDefault,
                                colors =
                                    SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = MaterialTheme.financialColors.limeAccent,
                                    ),
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.financialColors.hairlineDivider)

                        SettingsGroupHeading(stringResource(R.string.settings_section_accounts))
                        // 2. Manage accounts
                        SettingsNavRow(
                            iconRes = DesignR.drawable.ic_bank,
                            title = stringResource(id = R.string.settings_manage_accounts_title),
                            onClick = onManageAccountsClick,
                        )

                        HorizontalDivider(color = MaterialTheme.financialColors.hairlineDivider)

                        // 3. Link accounts
                        SettingsNavRow(
                            iconRes = DesignR.drawable.ic_arrow_outward,
                            title = stringResource(id = R.string.settings_link_accounts_title),
                            subtitle = stringResource(id = R.string.settings_link_accounts_subtitle),
                            onClick = onLinkAccountsClick,
                        )

                        HorizontalDivider(color = MaterialTheme.financialColors.hairlineDivider)

                        SettingsGroupHeading(stringResource(R.string.settings_section_rules))
                        // 5. Spend categories
                        SettingsNavRow(
                            iconRes = DesignR.drawable.ic_arrow_outward,
                            title = stringResource(id = R.string.settings_spend_categories_title),
                            subtitle = stringResource(id = R.string.settings_categories_create_delete),
                            onClick = onSpendCategoriesClick,
                        )

                        HorizontalDivider(color = MaterialTheme.financialColors.hairlineDivider)

                        // 6. Credit categories
                        SettingsNavRow(
                            iconRes = DesignR.drawable.ic_trending_down,
                            title = stringResource(id = R.string.settings_credit_categories_title),
                            subtitle = stringResource(id = R.string.settings_categories_create_delete),
                            onClick = onCreditCategoriesClick,
                        )

                        HorizontalDivider(color = MaterialTheme.financialColors.hairlineDivider)

                        // 7. Tags
                        SettingsNavRow(
                            iconRes = DesignR.drawable.ic_label,
                            title = stringResource(id = R.string.settings_tags_title),
                            subtitle = stringResource(id = R.string.settings_tags_subtitle),
                            onClick = onTagsClick,
                        )

                        HorizontalDivider(color = MaterialTheme.financialColors.hairlineDivider)

                        // 8. Mark as transfer amount
                        val currentThresholdRupees = userPreferences.autoTransferThresholdPaise / 100L
                        SettingsNavRow(
                            iconRes = DesignR.drawable.ic_category_transfer,
                            title = stringResource(id = R.string.settings_mark_transfer_title),
                            subtitle = currentThresholdRupees.toString(),
                            onClick = onMarkTransferClick,
                        )

                        HorizontalDivider(color = MaterialTheme.financialColors.hairlineDivider)

                        SettingsGroupHeading(stringResource(R.string.settings_section_notifications))
                        // 9. Notifications & Reminders
                        SettingsNavRow(
                            iconRes = DesignR.drawable.ic_notes,
                            title = stringResource(id = R.string.settings_notifications_reminders_title),
                            onClick = onNotificationsClick,
                        )

                        HorizontalDivider(color = MaterialTheme.financialColors.hairlineDivider)

                        SettingsNavRow(
                            iconRes = DesignR.drawable.ic_copy,
                            title = stringResource(id = R.string.settings_manage_reminders_title),
                            onClick = onRemindersClick,
                        )

                        HorizontalDivider(color = MaterialTheme.financialColors.hairlineDivider)

                        SettingsGroupHeading(stringResource(R.string.settings_section_data))
                        // 10. Backup
                        SettingsNavRow(
                            iconRes = DesignR.drawable.ic_copy,
                            title = stringResource(id = R.string.settings_backup_title),
                            onClick = onBackupClick,
                        )

                        HorizontalDivider(color = MaterialTheme.financialColors.hairlineDivider)

                        SettingsGroupHeading(stringResource(R.string.settings_section_security))
                        // 11. Security
                        SettingsNavRow(
                            iconRes = DesignR.drawable.ic_channel_card,
                            title = stringResource(id = R.string.settings_security_title),
                            onClick = onSecurityClick,
                        )

                        HorizontalDivider(color = MaterialTheme.financialColors.hairlineDivider)

                        SettingsGroupHeading(stringResource(R.string.settings_section_sms))
                        // 12. Rescan SMS Inbox
                        SettingsNavRow(
                            icon = Icons.Default.Refresh,
                            title = stringResource(id = R.string.settings_rescan_title),
                            onClick = onRescanSmsClick,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable Categories Manager Sheet for Spend and Credit categories.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoriesManagerSheet(
    title: String,
    allCategories: List<String>,
    customCategories: Set<String>,
    onAddCategoryClick: () -> Unit,
    onDeleteCustomCategory: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.financialColors.cardBackground,
        dragHandle = {
            Surface(
                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp),
                color = MaterialTheme.financialColors.hairlineDivider,
                shape = RoundedCornerShape(2.dp),
            ) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp))
            }
        },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                IconButton(onClick = onAddCategoryClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(id = R.string.settings_add_category),
                        tint = MaterialTheme.financialColors.limeAccent,
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onAddCategoryClick,
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

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(allCategories, key = { it }) { cat ->
                    val isCustom = customCategories.contains(cat)
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryBadge(categoryName = cat, size = 28.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                )
                            }
                            if (isCustom) {
                                IconButton(
                                    onClick = { onDeleteCustomCategory(cat) },
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription =
                                            stringResource(id = R.string.settings_delete_category_title),
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
    }
}

@Composable
private fun SettingsGroupHeading(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.financialColors.textSecondary,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsNavRow(
    iconRes: Int? = null,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = iconTint ?: MaterialTheme.financialColors.textSecondary,
                    modifier = Modifier.size(22.dp),
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint ?: MaterialTheme.financialColors.textSecondary,
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
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.financialColors.textSecondary,
                    )
                }
            }
        }
        Icon(
            painter = painterResource(id = DesignR.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.financialColors.textSecondary,
            modifier = Modifier.size(18.dp),
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageAccountsScreenView(
    accounts: List<Account>,
    onAccountClick: (Account) -> Unit,
    onSaveAccount: (Account) -> Unit,
    onMergeDuplicateAccounts: ((sourceAccountId: Long, targetAccountId: Long) -> Unit)? = null,
    onAddAccountClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var duplicateToMerge by remember { mutableStateOf<Pair<Account, Account>?>(null) }
    val duplicateCandidates = remember(accounts) {
        BankAccountMergeStore.duplicatePairs(accounts)
    }

    if (duplicateToMerge != null) {
        val (source, target) = duplicateToMerge!!
        DuplicateAccountDialog(
            source = source,
            target = target,
            onMerge = {
                onMergeDuplicateAccounts?.invoke(source.id, target.id)
                duplicateToMerge = null
            },
            onDismiss = { duplicateToMerge = null },
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_manage_accounts_page_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.settings_close),
                            tint = Color.White,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAddAccountClick) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(id = R.string.settings_add_account),
                            tint = MaterialTheme.financialColors.limeAccent,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.financialColors.cardBackground,
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.financialColors.limeAccent,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(id = R.string.settings_manage_accounts_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.financialColors.textSecondary,
                        )
                    }
                }
            }

            if (duplicateCandidates.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (pair in duplicateCandidates) {
                            val source = pair.first
                            val target = pair.second
                            val sourceName = "${source.bankName ?: source.name} ••${source.last4 ?: ""}"
                            val targetName = "${target.bankName ?: target.name} ••${target.last4 ?: ""}"
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { duplicateToMerge = source to target },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.financialColors.limeAccent.copy(alpha = 0.1f),
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.financialColors.limeAccent.copy(alpha = 0.4f)),
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(id = R.string.duplicate_accounts_banner_title),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.financialColors.limeAccent,
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = stringResource(
                                                id = R.string.duplicate_accounts_banner_desc,
                                                sourceName,
                                                targetName,
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.financialColors.textSecondary,
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(id = R.string.duplicate_accounts_merge),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.financialColors.limeAccent,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (accounts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(id = R.string.settings_accounts_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.financialColors.textSecondary,
                        )
                    }
                }
            } else {
                items(accounts, key = { it.id }) { acc ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.financialColors.cardBackground,
                        border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAccountClick(acc) },
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val iconRes = when (acc.type) {
                                AccountType.CreditCard -> DesignR.drawable.ic_channel_card
                                AccountType.Wallet -> DesignR.drawable.ic_channel_wallet
                                else -> DesignR.drawable.ic_bank
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.financialColors.cardBackground,
                                    border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                                    modifier = Modifier.size(40.dp),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(id = iconRes),
                                            contentDescription = null,
                                            tint = MaterialTheme.financialColors.limeAccent,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = acc.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                    )
                                    val parent = if (acc.linkedPrimaryAccountId != null) {
                                        accounts.find { it.id == acc.linkedPrimaryAccountId }
                                    } else null
                                    val linkedCount = accounts.count { it.linkedPrimaryAccountId == acc.id }
                                    val linkTag = when {
                                        parent != null -> " • Linked to ${parent.name}"
                                        linkedCount > 0 -> " • $linkedCount cards linked"
                                        else -> ""
                                    }
                                    val sub = "${acc.bankName ?: ""} ••${acc.last4 ?: "----"}$linkTag"
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.financialColors.textSecondary,
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = MoneyFormatter.formatMinorUnits(acc.currentBalance ?: 0L),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.financialColors.limeAccent,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        onSaveAccount(acc.copy(isCountedAsExpense = !acc.isCountedAsExpense))
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (acc.isCountedAsExpense) {
                                                DesignR.drawable.ic_visibility
                                            } else {
                                                DesignR.drawable.ic_visibility_off
                                            },
                                        ),
                                        contentDescription = if (acc.isCountedAsExpense) {
                                            stringResource(id = R.string.settings_include_spends_toast)
                                        } else {
                                            stringResource(id = R.string.settings_exclude_spends_toast)
                                        },
                                        tint = if (acc.isCountedAsExpense) {
                                            MaterialTheme.financialColors.limeAccent
                                        } else {
                                            MaterialTheme.financialColors.textSecondary
                                        },
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkAccountsScreenView(
    accounts: List<Account>,
    onMergeAccounts: ((primaryAccountId: Long, redundantAccountIds: Set<Long>) -> Unit)?,
    onLinkAccountForPrimary: (Account) -> Unit,
    onLinkNewAccountClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val linkedPrimaryAccounts = remember(accounts) {
        accounts.filter { primary ->
            accounts.any { it.linkedPrimaryAccountId == primary.id }
        }
    }
    var expandedAccountIds by remember(linkedPrimaryAccounts) {
        mutableStateOf(linkedPrimaryAccounts.map { it.id }.toSet())
    }

    val unlinkedSuggestions = remember(accounts, linkedPrimaryAccounts) {
        val primaryBanks = accounts.filter {
            (it.type == AccountType.Savings || it.type == AccountType.Current) && it.linkedPrimaryAccountId == null
        }
        val unlinkedCards = accounts.filter {
            (it.type == AccountType.CreditCard || it.type == AccountType.Wallet) && it.linkedPrimaryAccountId == null
        }
        primaryBanks.mapNotNull { primary ->
            val pBank = AccountAutoLinker.normalizeBank(primary.bankName)
                ?: AccountAutoLinker.extractBankFromName(primary.name)
            val matchingCards = unlinkedCards.filter { card ->
                val cBank = AccountAutoLinker.normalizeBank(card.bankName)
                    ?: AccountAutoLinker.extractBankFromName(card.name)
                pBank != null && cBank != null && pBank.equals(cBank, ignoreCase = true)
            }
            if (matchingCards.isNotEmpty()) primary to matchingCards else null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_link_accounts_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.settings_close),
                            tint = Color.White,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLinkNewAccountClick) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(id = R.string.settings_link_new_account),
                            tint = MaterialTheme.financialColors.limeAccent,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (linkedPrimaryAccounts.isEmpty()) {
                if (unlinkedSuggestions.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.financialColors.cardBackground,
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.financialColors.limeAccent.copy(alpha = 0.5f)),
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.financialColors.limeAccent,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(id = R.string.settings_auto_link_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Found matching cards for your bank accounts. Link them to group transactions under your primary account.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.financialColors.textSecondary,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                unlinkedSuggestions.forEach { (primary, cards) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = primary.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White,
                                            )
                                            Text(
                                                text = "${cards.size} matching card(s): ${cards.joinToString { it.name }}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.financialColors.textSecondary,
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                onMergeAccounts?.invoke(primary.id, cards.map { it.id }.toSet())
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.financialColors.paleLimeAddButton,
                                                contentColor = MaterialTheme.financialColors.onPaleLimeAddButton,
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                        ) {
                                            Text(text = "Link", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.financialColors.cardBackground,
                            border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                            modifier = Modifier.size(64.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = DesignR.drawable.ic_channel_card),
                                    contentDescription = null,
                                    tint = MaterialTheme.financialColors.limeAccent,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                        Text(
                            text = stringResource(id = R.string.settings_no_linked_accounts_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = stringResource(id = R.string.settings_no_linked_accounts_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.financialColors.textSecondary,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onLinkNewAccountClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.financialColors.paleLimeAddButton,
                                contentColor = MaterialTheme.financialColors.onPaleLimeAddButton,
                            ),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.settings_link_new_account),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            } else {
                items(linkedPrimaryAccounts, key = { it.id }) { primary ->
                    val linkedSubAccounts = accounts.filter { it.linkedPrimaryAccountId == primary.id }
                    val isExpanded = expandedAccountIds.contains(primary.id)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.financialColors.cardBackground,
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Primary Account Header (Clickable to expand/collapse)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        expandedAccountIds = if (isExpanded) {
                                            expandedAccountIds - primary.id
                                        } else {
                                            expandedAccountIds + primary.id
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(
                                        painter = painterResource(id = DesignR.drawable.ic_bank),
                                        contentDescription = null,
                                        tint = MaterialTheme.financialColors.limeAccent,
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = primary.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                        )
                                        Text(
                                            text = "${primary.bankName ?: ""} ••${primary.last4 ?: "----"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.financialColors.textSecondary,
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.financialColors.limeAccent.copy(alpha = 0.15f),
                                        border =
                                            BorderStroke(
                                                1.dp,
                                                MaterialTheme.financialColors.limeAccent.copy(alpha = 0.3f),
                                            ),
                                    ) {
                                        Text(
                                            text = stringResource(
                                                id = R.string.settings_sub_accounts_linked_format,
                                                linkedSubAccounts.size,
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.financialColors.limeAccent,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = if (isExpanded) {
                                            Icons.Default.KeyboardArrowUp
                                        } else {
                                            Icons.Default.KeyboardArrowDown
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.financialColors.textSecondary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = MaterialTheme.financialColors.hairlineDivider)
                                Spacer(modifier = Modifier.height(8.dp))

                                // Tree-like structure of linked sub-accounts
                                linkedSubAccounts.forEach { sub ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(
                                                text = "└── ",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.financialColors.textSecondary,
                                                modifier = Modifier.padding(start = 8.dp),
                                            )
                                            Icon(
                                                painter = painterResource(
                                                    id = if (sub.type == AccountType.CreditCard) {
                                                        DesignR.drawable.ic_channel_card
                                                    } else {
                                                        DesignR.drawable.ic_channel_wallet
                                                    },
                                                ),
                                                contentDescription = null,
                                                tint = MaterialTheme.financialColors.limeAccent,
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = sub.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color.White,
                                                )
                                                Text(
                                                    text = "${sub.bankName ?: ""} ••${sub.last4 ?: "----"}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.financialColors.textSecondary,
                                                )
                                            }
                                        }

                                        Checkbox(
                                            checked = true,
                                            onCheckedChange = { checked ->
                                                if (!checked) {
                                                    val remainingIds = linkedSubAccounts
                                                        .filter { it.id != sub.id }
                                                        .map { it.id }
                                                        .toSet()
                                                    onMergeAccounts?.invoke(primary.id, remainingIds)
                                                }
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = MaterialTheme.financialColors.limeAccent,
                                                checkmarkColor = MaterialTheme.financialColors.onLimeAccent,
                                            ),
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onLinkAccountForPrimary(primary) }
                                        .padding(vertical = 6.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.financialColors.limeAccent,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(id = R.string.settings_link_more_cards),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.financialColors.limeAccent,
                                    )
                                }
                            }
                        }
                    }
                }
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
