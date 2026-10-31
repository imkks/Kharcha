package com.kharcha

import android.os.Bundle
import android.os.Build
import android.app.KeyguardManager
import android.view.WindowManager
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.kharcha.core.common.security.AuthenticatorPromptMode
import com.kharcha.core.common.security.authenticatorPromptMode
import androidx.fragment.app.FragmentActivity
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.kharcha.core.common.DefaultAccountRepository
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.common.TransactionRepository
import com.kharcha.core.common.UserPreferences
import com.kharcha.core.designsystem.R as DesignR
import com.kharcha.core.designsystem.component.AddTransactionDialog
import com.kharcha.core.designsystem.component.AxioBottomNavBar
import com.kharcha.core.designsystem.component.AxioNavItem
import com.kharcha.core.designsystem.component.KharchaPillSnackbar
import com.kharcha.core.designsystem.theme.KharchaTheme
import com.kharcha.core.designsystem.theme.LocalPrivacyMode
import com.kharcha.core.designsystem.theme.LocalPrivacyModeState
import com.kharcha.core.designsystem.theme.LocalIncomeHidden
import com.kharcha.core.designsystem.theme.LocalIncomeRevealed
import com.kharcha.core.designsystem.theme.LocalIncomeReveal
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.Account
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionFilter
import com.kharcha.core.model.TransactionType
import com.kharcha.engine.sms.ExpenseNotification
import com.kharcha.engine.sms.ImportWorker
import com.kharcha.feature.home.ui.HomeScreen
import com.kharcha.feature.insights.InsightsScreen
import com.kharcha.feature.onboarding.OnboardingRoute
import com.kharcha.feature.review.ui.ReviewQueueScreen
import com.kharcha.feature.settings.ui.MoreScreen
import com.kharcha.feature.transactions.R as TransactionsR
import com.kharcha.feature.transactions.ui.AccountDetailScreen
import com.kharcha.feature.transactions.ui.EntityTransactionsScreen
import com.kharcha.feature.transactions.ui.ReportUndetectedSmsScreen
import com.kharcha.feature.transactions.ui.SearchScreen
import com.kharcha.feature.transactions.ui.SetBudgetSheet
import com.kharcha.feature.transactions.ui.TransactionDetailScreen
import com.kharcha.feature.transactions.ui.TransactionsScreen
import com.kharcha.feature.transactions.ui.TransactionsSubTab
import com.kharcha.feature.transactions.ui.WeeklySummaryScreen
import com.kharcha.feature.transactions.ui.SplitRecordsScreen
import dagger.hilt.android.AndroidEntryPoint
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.launch

data class SelectedEntity(
    val title: String,
    val isMerchant: Boolean,
)

enum class AppDestination(
    val labelRes: Int,
    val iconRes: Int,
) {
    TRANSACTIONS(R.string.nav_transactions, DesignR.drawable.ic_category_transfer),
    HOME(R.string.nav_home, DesignR.drawable.ic_bar_chart),
    SPLITS(R.string.nav_splits, DesignR.drawable.ic_split),
    INSIGHTS(R.string.nav_insights, DesignR.drawable.ic_trending_up),
    MORE(R.string.nav_more, DesignR.drawable.ic_settings),
}

private val bottomDestinations = listOf(
    AppDestination.TRANSACTIONS,
    AppDestination.HOME,
    AppDestination.SPLITS,
    AppDestination.INSIGHTS,
)

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private var appUnlockState: MutableState<Boolean>? = null
    private var incomeRevealState: MutableState<Boolean>? = null
    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (userPreferences.isAppLockEnabled || userPreferences.isIncomeHidden) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        setContent {
            KharchaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    var isOnboardingCompleted by rememberSaveable {
                        mutableStateOf(userPreferences.isOnboardingCompleted)
                    }
                    val appUnlockedState = rememberSaveable {
                        mutableStateOf(!userPreferences.isAppLockEnabled)
                    }
                    var appLockError by rememberSaveable { mutableStateOf(false) }
                    val incomeHiddenState = rememberSaveable { mutableStateOf(userPreferences.isIncomeHidden) }
                    val incomeRevealedState = rememberSaveable { mutableStateOf(false) }
                    val privacyModeState = rememberSaveable { mutableStateOf(userPreferences.isPrivacyModeEnabled) }
                    appUnlockState = appUnlockedState
                    incomeRevealState = incomeRevealedState
                    var isAppUnlocked by appUnlockedState
                    var isIncomeRevealed by incomeRevealedState

                    if (!isOnboardingCompleted) {
                        OnboardingRoute(
                            onOnboardingComplete = {
                                userPreferences.isOnboardingCompleted = true
                                isOnboardingCompleted = true
                            },
                        )
                    } else if (userPreferences.isAppLockEnabled && !isAppUnlocked) {
                        AppLockScreen(
                            hasError = appLockError,
                            onUnlock = {
                                authenticateWithBiometric(
                                    onAuthenticated = {
                                        isAppUnlocked = true
                                        appLockError = false
                                    },
                                    onError = { appLockError = true },
                                )
                            },
                        )
                    } else {
                        CompositionLocalProvider(
                            LocalIncomeHidden provides incomeHiddenState,
                            LocalIncomeRevealed provides incomeRevealedState,
                            LocalIncomeReveal provides {
                                if (!isIncomeRevealed) {
                                    authenticateWithBiometric(
                                        onAuthenticated = { isIncomeRevealed = true },
                                        onError = {},
                                    )
                                }
                            },
                            LocalPrivacyModeState provides privacyModeState,
                            LocalPrivacyMode provides privacyModeState.value,
                        ) {
                        MainAppShell(
                            transactionRepository = transactionRepository,
                            userPreferences = userPreferences,
                            notificationTransactionId =
                                intent.getLongExtra(ExpenseNotification.EXTRA_TRANSACTION_ID, -1L)
                                    .takeIf { it >= 0L },
                            notificationAction = intent.getStringExtra(ExpenseNotification.EXTRA_ACTION),
                            openWeeklySummary = intent.getBooleanExtra(SummaryNotificationWorker.EXTRA_OPEN_WEEKLY_SUMMARY, false),
                            onRescanSmsInbox = { windowCode ->
                                WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                                    "sms_inbox_rescan",
                                    ExistingWorkPolicy.KEEP,
                                    OneTimeWorkRequestBuilder<ImportWorker>()
                                        .setInputData(
                                            workDataOf(
                                                ImportWorker.KEY_FORCE_RESCAN to true,
                                                ImportWorker.KEY_SINCE_TIMESTAMP to ImportWorker.rescanSinceTimestamp(windowCode),
                                            ),
                                        ).build(),
                                )
                            },
                        )
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        incomeRevealState?.value = false
        if (userPreferences.isAppLockEnabled || userPreferences.isIncomeHidden) {
            appUnlockState?.value = false
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (userPreferences.isAppLockEnabled || userPreferences.isIncomeHidden) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private fun authenticateWithBiometric(
        onAuthenticated: () -> Unit,
        onError: () -> Unit,
    ) {
        try {
            val prompt = BiometricPrompt(
                this,
                mainExecutor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onAuthenticated()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        onError()
                    }
                },
            )
            val hasDeviceCredential = getSystemService(KeyguardManager::class.java)?.isDeviceSecure == true
            val authMode = authenticatorPromptMode(Build.VERSION.SDK_INT, hasDeviceCredential)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.app_lock_title))
                .setSubtitle(getString(R.string.app_lock_subtitle))
            if (authMode == AuthenticatorPromptMode.BIOMETRIC_WITH_DEVICE_CREDENTIAL) {
                promptInfo.setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                )
            } else if (authMode == AuthenticatorPromptMode.LEGACY_DEVICE_CREDENTIAL_FALLBACK) {
                @Suppress("DEPRECATION")
                promptInfo.setDeviceCredentialAllowed(true)
            } else {
                promptInfo
                    .setNegativeButtonText(getString(R.string.app_lock_cancel))
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            }
            prompt.authenticate(promptInfo.build())
        } catch (_: IllegalArgumentException) {
            onError()
        } catch (_: IllegalStateException) {
            onError()
        }
    }
}

@Composable
private fun AppLockScreen(hasError: Boolean, onUnlock: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        onUnlock()
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        if (hasError) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            ) {
                Text(text = stringResource(R.string.app_lock_error), style = MaterialTheme.typography.bodyMedium)
                androidx.compose.material3.Button(onClick = onUnlock) {
                    Text(text = stringResource(R.string.app_lock_retry))
                }
            }
        }
    }
}

@Composable
fun MainAppShell(
    transactionRepository: TransactionRepository,
    userPreferences: UserPreferences,
    onRescanSmsInbox: (String) -> Unit,
    notificationTransactionId: Long? = null,
    notificationAction: String? = null,
    openWeeklySummary: Boolean = false,
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestination.HOME) }
    val transactions by transactionRepository.getTransactions().collectAsState(initial = emptyList())
    val context = LocalContext.current
    val accountRepository = remember { DefaultAccountRepository.getInstance(context) }
    val accounts by accountRepository.getAccounts().collectAsState(initial = emptyList())

    LaunchedEffect(accounts.size, transactions.size) {
        if (accounts.isNotEmpty()) {
            val unlinkedIds = userPreferences.getUnlinkedAccountIds()
            accountRepository.autoLinkAccounts(transactions, unlinkedIds)
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showReviewQueue by rememberSaveable { mutableStateOf(false) }
    var selectedTransactionForDetail by remember { mutableStateOf<Transaction?>(null) }
    var categoryPickerTransactionId by remember { mutableStateOf<Long?>(null) }
    var selectedEntityForDetail by remember { mutableStateOf<SelectedEntity?>(null) }
    var selectedAccountForDetail by remember { mutableStateOf<Account?>(null) }
    var showAddTransactionDialog by rememberSaveable { mutableStateOf(false) }
    var showSearchScreen by rememberSaveable { mutableStateOf(false) }
    var showReportSmsScreen by rememberSaveable { mutableStateOf(false) }
    var showWeeklySummaryScreen by rememberSaveable { mutableStateOf(openWeeklySummary) }
    var showSetBudgetSheet by rememberSaveable { mutableStateOf(false) }
    var showSplitInfoDialog by rememberSaveable { mutableStateOf(false) }
    var filterCategoryForTransactions by rememberSaveable { mutableStateOf<String?>(null) }
    var transactionsFilter by remember { mutableStateOf(TransactionFilter()) }
    var transactionsYearMonth by remember { mutableStateOf<YearMonth?>(YearMonth.now()) }
    var transactionsIsAllMonthsSelected by remember { mutableStateOf(false) }
    var transactionsSubTab by remember { mutableStateOf(TransactionsSubTab.TRANSACTIONS) }
    var handledNotificationAction by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(notificationTransactionId, notificationAction, transactions) {
        val transactionId = notificationTransactionId ?: return@LaunchedEffect
        val action = notificationAction ?: ExpenseNotification.ACTION_OPEN
        val actionKey = "$transactionId:$action"
        if (actionKey == handledNotificationAction) return@LaunchedEffect

        val transaction = transactions.firstOrNull { it.id == transactionId } ?: return@LaunchedEffect
        handledNotificationAction = actionKey
        if (action == ExpenseNotification.ACTION_NOT_EXPENSE) {
            transactionRepository.updateTransaction(
                transaction.copy(
                    isCounted = false,
                    isReviewed = true,
                ),
            )
            NotificationManagerCompat.from(context).cancel((transactionId % Int.MAX_VALUE).toInt())
        } else {
            selectedTransactionForDetail = transaction
            if (action == ExpenseNotification.ACTION_EDIT_CATEGORY) {
                categoryPickerTransactionId = transactionId
            }
            if (action == ExpenseNotification.ACTION_SHOW_SMS) {
                currentDestination = AppDestination.TRANSACTIONS
            }
        }
    }

    val privacyModeState = LocalPrivacyModeState.current
    val isPrivacyMode = privacyModeState?.value ?: userPreferences.isPrivacyModeEnabled
    val onTogglePrivacyMode = {
        val newMode = !isPrivacyMode
        privacyModeState?.value = newMode
        userPreferences.isPrivacyModeEnabled = newMode
    }

    CompositionLocalProvider(LocalPrivacyMode provides isPrivacyMode) {
        BackHandler(enabled = showWeeklySummaryScreen) { showWeeklySummaryScreen = false }
        BackHandler(enabled = showSearchScreen) { showSearchScreen = false }
        BackHandler(enabled = showReportSmsScreen) { showReportSmsScreen = false }
        BackHandler(enabled = showReviewQueue) { showReviewQueue = false }
        BackHandler(enabled = showAddTransactionDialog) { showAddTransactionDialog = false }
        BackHandler(enabled = showSetBudgetSheet) { showSetBudgetSheet = false }
        BackHandler(enabled = showSplitInfoDialog) { showSplitInfoDialog = false }
        BackHandler(
            enabled = currentDestination != AppDestination.HOME &&
                selectedTransactionForDetail == null &&
                selectedEntityForDetail == null &&
                selectedAccountForDetail == null &&
                !showSearchScreen &&
                !showWeeklySummaryScreen &&
                !showReportSmsScreen &&
                !showReviewQueue &&
                !showAddTransactionDialog &&
                !showSetBudgetSheet,
        ) {
            currentDestination = AppDestination.HOME
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                snackbarHost = {
                    SnackbarHost(snackbarHostState) { data ->
                        KharchaPillSnackbar(snackbarData = data)
                    }
                },
                bottomBar = {
                    val navItems =
                        bottomDestinations.map { dest ->
                            AxioNavItem(
                                icon = painterResource(id = dest.iconRes),
                                contentDescription = stringResource(id = dest.labelRes),
                            )
                        }
                    val selectedIndex = bottomDestinations.indexOf(currentDestination)
                    AxioBottomNavBar(
                        selectedIndex = selectedIndex,
                        onItemSelected = { index ->
                            currentDestination = bottomDestinations[index]
                        },
                        items = navItems,
                    )
                },
            ) { innerPadding ->
                BoxDestinationContent(
                    modifier = Modifier.padding(innerPadding),
                    destination = currentDestination,
                    transactions = transactions,
                    accounts = accounts,
                    userPreferences = userPreferences,
                    onNavigateToTransactions = { currentDestination = AppDestination.TRANSACTIONS },
                    onNavigateToHome = { currentDestination = AppDestination.HOME },
                    onReviewClick = { showReviewQueue = true },
                    onTransactionClick = { txn -> selectedTransactionForDetail = txn },
                    onCategoryClick = { cat ->
                        selectedEntityForDetail = SelectedEntity(cat, isMerchant = false)
                    },
                    onMerchantClick = { merchant ->
                        selectedEntityForDetail = SelectedEntity(merchant, isMerchant = true)
                    },
                    filterCategory = filterCategoryForTransactions,
                    onClearCategoryFilter = { filterCategoryForTransactions = null },
                    onDeleteTransaction = { toDelete ->
                        scope.launch {
                            transactionRepository.deleteTransaction(toDelete.id)
                        }
                    },
                    onRestoreTransaction = { toRestore ->
                        scope.launch {
                            transactionRepository.insertTransaction(toRestore)
                        }
                    },
                    onReviewTransaction = { toReview ->
                        scope.launch {
                            transactionRepository.updateTransaction(toReview)
                        }
                    },
                    onUpdateTransactions = { updatedList ->
                        scope.launch {
                            transactionRepository.updateTransactions(updatedList)
                        }
                    },
                    onRescanSmsInbox = onRescanSmsInbox,
                    onPrivacyToggle = onTogglePrivacyMode,
                    onCategoriseUnknown = { showReviewQueue = true },
                    transactionsFilter = transactionsFilter,
                    onFilterChange = { transactionsFilter = it },
                    transactionsYearMonth = transactionsYearMonth,
                    onYearMonthChange = { transactionsYearMonth = it },
                    transactionsIsAllMonthsSelected = transactionsIsAllMonthsSelected,
                    onAllMonthsSelectedChange = { transactionsIsAllMonthsSelected = it },
                    transactionsSubTab = transactionsSubTab,
                    onSubTabChange = { transactionsSubTab = it },
                    onUpdateBudget = { category, budgetMinor ->
                        if (category == null) {
                            userPreferences.monthlyBudgetMinor = budgetMinor
                        } else {
                            userPreferences.setCategoryBudgetMinor(category, budgetMinor)
                        }
                    },
                    onSetBudgetClick = { showSetBudgetSheet = true },
                    onAddTransactionClick = { showAddTransactionDialog = true },
                    onTrendsClick = { currentDestination = AppDestination.INSIGHTS },
                    onCategoriesClick = {
                        transactionsSubTab = TransactionsSubTab.CATEGORIES
                        currentDestination = AppDestination.TRANSACTIONS
                    },
                    onWeeklySummaryClick = { showWeeklySummaryScreen = true },
                    onIncomeClick = {
                        filterCategoryForTransactions = null
                        transactionsFilter = TransactionFilter(types = setOf(TransactionType.Income))
                        transactionsYearMonth = YearMonth.now()
                        transactionsIsAllMonthsSelected = false
                        transactionsSubTab = TransactionsSubTab.TRANSACTIONS
                        currentDestination = AppDestination.TRANSACTIONS
                    },
                    onSearchClick = { showSearchScreen = true },
                    onTagClick = { tag ->
                        transactionsFilter = TransactionFilter(query = tag)
                        transactionsSubTab = TransactionsSubTab.TRANSACTIONS
                        currentDestination = AppDestination.TRANSACTIONS
                    },
                    onSaveAccount = { account ->
                        scope.launch {
                            accountRepository.saveAccount(account)
                            snackbarHostState.showSnackbar("Account saved")
                        }
                    },
                    onMergeAccounts = { primaryId, linkedIds ->
                        scope.launch {
                            val currentAccounts = accountRepository.getAccountsSync()
                            for (acc in currentAccounts) {
                                if (acc.id in linkedIds) {
                                    userPreferences.setAccountUnlinked(acc.id, false)
                                } else if (acc.linkedPrimaryAccountId == primaryId && acc.id !in linkedIds) {
                                    userPreferences.setAccountUnlinked(acc.id, true)
                                }
                            }
                            accountRepository.linkSubAccounts(primaryId, linkedIds)
                            snackbarHostState.showSnackbar("Accounts linked and grouped successfully")
                        }
                    },
                    onMergeDuplicateAccounts = { sourceId, targetId ->
                        scope.launch {
                            val success = accountRepository.mergeAccounts(sourceId, targetId, transactionRepository)
                            if (success) {
                                snackbarHostState.showSnackbar("Accounts merged successfully")
                            }
                        }
                    },
                    onReportSmsClick = { showReportSmsScreen = true },
                    onApplyMerchantRuleToTransactions = { merchant, category ->
                        scope.launch {
                            val matching =
                                transactions.filter {
                                    it.merchantNormalised?.equals(merchant, ignoreCase = true) == true ||
                                        it.merchantRaw?.equals(merchant, ignoreCase = true) == true
                                }
                            if (matching.isNotEmpty()) {
                                val updated = matching.map { it.copy(categorySource = category, isReviewed = true) }
                                transactionRepository.updateTransactions(updated)
                                snackbarHostState.showSnackbar("Applied $category to ${matching.size} transactions")
                            }
                        }
                    },
                    onAccountClick = { selectedAccountForDetail = it },
                    onProfileClick = { currentDestination = AppDestination.MORE },
                    onDuesClick = { currentDestination = AppDestination.INSIGHTS },
                    onNotificationSettingsChanged = {
                        SummaryNotificationWorker.scheduleAll(context, userPreferences)
                    },
                    onSplitClick = { showSplitInfoDialog = true },
                )
            }

            if (selectedTransactionForDetail != null) {
            BackHandler { selectedTransactionForDetail = null }
            val txn = selectedTransactionForDetail!!
            TransactionDetailScreen(
                transaction = txn,
                allTransactions = transactions,
                accounts = accounts,
                openCategoryPickerOnStart = categoryPickerTransactionId == txn.id,
                onBack = { selectedTransactionForDetail = null },
                onNavigateToTransaction = { txnId ->
                    selectedTransactionForDetail = transactions.find { it.id == txnId }
                },
                onNavigateToMerchant = { merchantName ->
                    selectedTransactionForDetail = null
                    selectedEntityForDetail = SelectedEntity(title = merchantName, isMerchant = true)
                },
                onLinkTransactions = { primaryId, secondaryId, isRefund ->
                    scope.launch {
                        transactionRepository.linkTransactions(primaryId, secondaryId, isRefund)
                        snackbarHostState.showSnackbar("Transactions linked")
                        selectedTransactionForDetail =
                            transactions.find { it.id == primaryId }?.copy(linkedTransactionId = secondaryId)
                    }
                },
                onUnlinkTransaction = { id ->
                    scope.launch {
                        transactionRepository.unlinkTransactions(id)
                        snackbarHostState.showSnackbar("Transactions unlinked")
                        selectedTransactionForDetail =
                            transactions.find { it.id == id }?.copy(linkedTransactionId = null)
                    }
                },
                onSave = { updated, propagate ->
                    scope.launch {
                        if (propagate) {
                            val newCategory = updated.categorySource ?: "Other"
                            val count =
                                transactionRepository.updateTransactionAndPropagateMerchant(
                                    transaction = updated,
                                    newCategory = newCategory,
                                    newType = updated.type,
                                    userPreferences = userPreferences,
                                )
                            val merchant = updated.merchantNormalised ?: updated.merchantRaw ?: "Merchant"
                            if (count > 1) {
                                snackbarHostState.showSnackbar("Updated $count transactions for $merchant")
                            } else {
                                snackbarHostState.showSnackbar("Transaction updated")
                            }
                        } else {
                            transactionRepository.updateTransaction(updated)
                            snackbarHostState.showSnackbar("Transaction updated")
                        }
                    }
                    selectedTransactionForDetail = null
                },
                onSaveTransactions = { updatedList, updateRule ->
                    scope.launch {
                        transactionRepository.updateTransactions(updatedList)
                        if (updateRule) {
                            val merchant =
                                updatedList.firstOrNull()?.merchantNormalised
                                    ?: updatedList.firstOrNull()?.merchantRaw
                            val category = updatedList.firstOrNull()?.categorySource
                            if (!merchant.isNullOrBlank() && !category.isNullOrBlank()) {
                                userPreferences.setMerchantCategory(merchant, category)
                            }
                        }
                        val count = updatedList.size
                        if (count > 1) {
                            snackbarHostState.showSnackbar("Updated $count transactions")
                        } else {
                            snackbarHostState.showSnackbar("Transaction updated")
                        }
                    }
                    selectedTransactionForDetail = null
                },
                onDelete = { toDelete ->
                    scope.launch {
                        transactionRepository.deleteTransaction(toDelete.id)
                        snackbarHostState.showSnackbar("Transaction deleted")
                    }
                    selectedTransactionForDetail = null
                },
            )
        } else if (selectedEntityForDetail != null) {
            val entity = selectedEntityForDetail!!
            BackHandler { selectedEntityForDetail = null }
            EntityTransactionsScreen(
                title = entity.title,
                isMerchant = entity.isMerchant,
                allTransactions = transactions,
                onBack = { selectedEntityForDetail = null },
                onTransactionClick = { selectedTransactionForDetail = it },
            )
        } else if (selectedAccountForDetail != null) {
            val acc = selectedAccountForDetail!!
            BackHandler { selectedAccountForDetail = null }
            AccountDetailScreen(
                account = acc,
                allAccounts = accounts,
                allTransactions = transactions,
                onBack = { selectedAccountForDetail = null },
                onTransactionClick = { selectedTransactionForDetail = it },
                onSaveAccount = { updatedAccount ->
                    scope.launch {
                        accountRepository.saveAccount(updatedAccount)
                        selectedAccountForDetail = updatedAccount
                    }
                },
                onMergeAccounts = { primaryId, linkedIds ->
                    scope.launch {
                        val currentAccounts = accountRepository.getAccountsSync()
                        for (a in currentAccounts) {
                            if (a.id in linkedIds) {
                                userPreferences.setAccountUnlinked(a.id, false)
                            } else if (a.linkedPrimaryAccountId == primaryId && a.id !in linkedIds) {
                                userPreferences.setAccountUnlinked(a.id, true)
                            }
                        }
                        accountRepository.linkSubAccounts(primaryId, linkedIds)
                    }
                },
                onAddTransactionClick = { showAddTransactionDialog = true },
                onSetBudgetClick = { showSetBudgetSheet = true },
            )
        }
    }

    if (showSetBudgetSheet) {
        SetBudgetSheet(
            currentBudgetMinor = userPreferences.monthlyBudgetMinor,
            onSaveBudget = { category, budgetMinor ->
                if (category == null) {
                    userPreferences.monthlyBudgetMinor = budgetMinor
                } else {
                    userPreferences.setCategoryBudgetMinor(category, budgetMinor)
                }
                showSetBudgetSheet = false
            },
            onDismiss = { showSetBudgetSheet = false },
        )
    }

    if (showReviewQueue) {
        val unreviewed = remember(transactions) { transactions.filter { !it.isReviewed } }
        ReviewQueueScreen(
            unreviewedTransactions = unreviewed,
            onCategorize = { txn, category, propagate ->
                scope.launch {
                    if (propagate) {
                        val count =
                            transactionRepository.updateTransactionAndPropagateMerchant(
                                transaction = txn,
                                newCategory = category,
                                newType = txn.type,
                                userPreferences = userPreferences,
                            )
                        val merchant = txn.merchantNormalised ?: txn.merchantRaw ?: "Merchant"
                        if (count > 1) {
                            snackbarHostState.showSnackbar("Updated $count transactions for $merchant")
                        }
                    } else {
                        val isTransferCat = category.equals("Transfer", ignoreCase = true) ||
                            category.equals("A/C Transfer", ignoreCase = true)
                        transactionRepository.updateTransaction(
                            txn.copy(
                                categorySource = category,
                                isCounted = !isTransferCat,
                                isReviewed = true,
                            ),
                        )
                    }
                }
            },
            onIgnore = { txn ->
                scope.launch {
                    transactionRepository.updateTransaction(
                        txn.copy(isCounted = false, isReviewed = true),
                    )
                }
            },
            onBack = { showReviewQueue = false },
        )
    }

    if (showAddTransactionDialog) {
        val txnAddedMsg = stringResource(id = R.string.txn_added)
        AddTransactionDialog(
            accounts = accounts,
            onSave = { newTxn ->
                scope.launch {
                    transactionRepository.insertTransaction(newTxn)
                    snackbarHostState.showSnackbar(txnAddedMsg)
                }
            },
            onDismiss = { showAddTransactionDialog = false },
        )
    }

    if (showSearchScreen) {
        SearchScreen(
            transactions = transactions,
            onTransactionClick = { txn ->
                selectedTransactionForDetail = txn
                showSearchScreen = false
            },
            onBack = { showSearchScreen = false },
            onAddPrefilledTransaction = { _ ->
                showSearchScreen = false
                showAddTransactionDialog = true
            },
        )
    }

    if (showReportSmsScreen) {
        val reportedMsg = stringResource(id = TransactionsR.string.report_sms_submitted)
        ReportUndetectedSmsScreen(
            onSubmitReport = { _, _, _ ->
                scope.launch {
                    snackbarHostState.showSnackbar(reportedMsg)
                }
            },
            onDismiss = { showReportSmsScreen = false },
        )
    }

    if (showWeeklySummaryScreen) {
        WeeklySummaryScreen(
            transactions = transactions,
            monthlyBudgetMinor = userPreferences.monthlyBudgetMinor,
            onBack = { showWeeklySummaryScreen = false },
            onSetBudgetClick = {
                showWeeklySummaryScreen = false
                showSetBudgetSheet = true
            },
        )
    }

    if (showSplitInfoDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSplitInfoDialog = false },
            title = { Text(text = "Split Expenses") },
            text = {
                Text(
                    text = "To split a bill, tap on any transaction from your Transactions list and tap 'Split Bill' to calculate individual shares and track group settlements.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        showSplitInfoDialog = false
                        currentDestination = AppDestination.TRANSACTIONS
                    },
                ) {
                    Text(text = "Go to Transactions")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showSplitInfoDialog = false },
                ) {
                    Text(text = "Close")
                }
            },
        )
    }
}
}

@Composable
fun BoxDestinationContent(
    destination: AppDestination,
    transactions: List<Transaction>,
    accounts: List<Account> = emptyList(),
    userPreferences: UserPreferences,
    onNavigateToTransactions: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onReviewClick: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    onCategoryClick: (String) -> Unit = {},
    onMerchantClick: (String) -> Unit = {},
    filterCategory: String? = null,
    onClearCategoryFilter: () -> Unit = {},
    onDeleteTransaction: (Transaction) -> Unit = {},
    onRestoreTransaction: (Transaction) -> Unit = {},
    onReviewTransaction: (Transaction) -> Unit = {},
    onUpdateTransactions: (List<Transaction>) -> Unit = {},
    onRescanSmsInbox: (String) -> Unit,
    onPrivacyToggle: () -> Unit,
    onCategoriseUnknown: () -> Unit = {},
    transactionsFilter: TransactionFilter = TransactionFilter(),
    onFilterChange: (TransactionFilter) -> Unit = {},
    transactionsYearMonth: YearMonth? = YearMonth.now(),
    onYearMonthChange: (YearMonth?) -> Unit = {},
    transactionsIsAllMonthsSelected: Boolean = false,
    onAllMonthsSelectedChange: (Boolean) -> Unit = {},
    transactionsSubTab: TransactionsSubTab = TransactionsSubTab.TRANSACTIONS,
    onSubTabChange: (TransactionsSubTab) -> Unit = {},
    onUpdateBudget: (String?, Long) -> Unit = { _, _ -> },
    onSetBudgetClick: () -> Unit = {},
    onAddTransactionClick: () -> Unit = {},
    onTrendsClick: () -> Unit = {},
    onCategoriesClick: () -> Unit = {},
    onIncomeClick: () -> Unit = {},
    onWeeklySummaryClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onTagClick: (String) -> Unit = {},
    onAccountClick: (Account) -> Unit = {},
    onSplitClick: () -> Unit = {},
    onDepositClick: () -> Unit = {},
    onDuesClick: () -> Unit = {},
    onNotificationSettingsChanged: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSaveAccount: (Account) -> Unit = {},
    onMergeAccounts: ((Long, Set<Long>) -> Unit)? = null,
    onMergeDuplicateAccounts: ((Long, Long) -> Unit)? = null,
    onReportSmsClick: () -> Unit = {},
    onApplyMerchantRuleToTransactions: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    when (destination) {
        AppDestination.HOME -> {
            HomeScreen(
                transactions = transactions,
                accounts = accounts,
                userName = userPreferences.userName,
                monthlyBudgetMinor = userPreferences.monthlyBudgetMinor,
                onSeeAllClick = onNavigateToTransactions,
                onIncomeClick = onIncomeClick,
                onReviewClick = onReviewClick,
                onTransactionClick = onTransactionClick,
                onCategoryClick = onCategoryClick,
                onPrivacyToggle = onPrivacyToggle,
                onSetBudgetClick = onSetBudgetClick,
                onAddTransactionClick = onAddTransactionClick,
                onTrendsClick = onTrendsClick,
                onCategoriesClick = onCategoriesClick,
                onWeeklySummaryClick = onWeeklySummaryClick,
                onSearchClick = onSearchClick,
                onAccountClick = onAccountClick,
                onTagClick = onTagClick,
                onSplitClick = onSplitClick,
                onDepositClick = onDepositClick,
                onDuesClick = onDuesClick,
                onProfileClick = onProfileClick,
                modifier = modifier,
            )
        }
        AppDestination.TRANSACTIONS -> {
            TransactionsScreen(
                transactions = transactions,
                accounts = accounts,
                onTransactionClick = onTransactionClick,
                onPrivacyToggle = onPrivacyToggle,
                initialCategory = filterCategory,
                onClearInitialCategory = onClearCategoryFilter,
                onDeleteTransaction = onDeleteTransaction,
                onRestoreTransaction = onRestoreTransaction,
                onReviewTransaction = onReviewTransaction,
                onUpdateTransactions = onUpdateTransactions,
                monthlyBudgetMinor = userPreferences.monthlyBudgetMinor,
                categoryBudgets = userPreferences.getAllCategoryBudgets(),
                onUpdateBudget = onUpdateBudget,
                onCategoriseUnknown = onCategoriseUnknown,
                filter = transactionsFilter,
                onFilterChange = onFilterChange,
                selectedYearMonthState = transactionsYearMonth,
                onYearMonthChange = onYearMonthChange,
                isAllMonthsSelectedState = transactionsIsAllMonthsSelected,
                onAllMonthsSelectedChange = onAllMonthsSelectedChange,
                subTabState = transactionsSubTab,
                onSubTabChange = onSubTabChange,
                onCategoryClick = onCategoryClick,
                onMerchantClick = onMerchantClick,
                modifier = modifier,
            )
        }
        AppDestination.INSIGHTS -> {
            InsightsScreen(
                transactions = transactions,
                accounts = accounts,
                monthlyBudgetMinor = userPreferences.monthlyBudgetMinor,
                categoryBudgets = userPreferences.getAllCategoryBudgets(),
                onUpdateMonthlyBudget = { newBudget -> userPreferences.monthlyBudgetMinor = newBudget },
                onUpdateCategoryBudget = { cat, minor -> userPreferences.setCategoryBudgetMinor(cat, minor) },
                onTransactionClick = onTransactionClick,
                onCategoryClick = onCategoryClick,
                onMerchantClick = onMerchantClick,
                onBack = onNavigateToHome,
                onReviewMonthClick = { ym ->
                    onYearMonthChange(ym)
                    onSubTabChange(TransactionsSubTab.TRANSACTIONS)
                    onNavigateToTransactions()
                },
                modifier = modifier,
            )
        }
        AppDestination.SPLITS -> {
            SplitRecordsScreen(
                transactions = transactions,
                onTransactionClick = onTransactionClick,
                modifier = modifier,
            )
        }
        AppDestination.MORE -> {
            MoreScreen(
                transactions = transactions,
                accounts = accounts,
                userPreferences = userPreferences,
                onRescanSmsInbox = onRescanSmsInbox,
                onUpdateMonthlyBudget = { newBudget -> userPreferences.monthlyBudgetMinor = newBudget },
                onSaveAccount = onSaveAccount,
                onMergeAccounts = onMergeAccounts,
                onMergeDuplicateAccounts = onMergeDuplicateAccounts,
                onApplyMerchantRuleToTransactions = onApplyMerchantRuleToTransactions,
                onWeeklySummaryClick = onWeeklySummaryClick,
                onReportSmsClick = onReportSmsClick,
                onAccountClick = onAccountClick,
                onTransactionClick = onTransactionClick,
                onNotificationSettingsChanged = onNotificationSettingsChanged,
                modifier = modifier,
            )
        }
    }
}
