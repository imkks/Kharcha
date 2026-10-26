package com.kharcha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.DefaultAccountRepository
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.common.TransactionRepository
import com.kharcha.core.common.UserPreferences
import com.kharcha.core.designsystem.component.AddTransactionDialog
import com.kharcha.core.designsystem.theme.KharchaTheme
import com.kharcha.core.designsystem.theme.LocalPrivacyMode
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionFilter
import com.kharcha.core.model.TransactionType
import com.kharcha.feature.home.ui.HomeScreen
import com.kharcha.feature.insights.InsightsScreen
import com.kharcha.feature.onboarding.OnboardingRoute
import com.kharcha.feature.review.ui.ReviewQueueScreen
import com.kharcha.feature.settings.ui.MoreScreen
import com.kharcha.feature.transactions.ui.EntityTransactionsScreen
import com.kharcha.feature.transactions.ui.TransactionDetailScreen
import com.kharcha.feature.transactions.ui.TransactionsScreen
import com.kharcha.feature.transactions.ui.TransactionsSubTab
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
    val icon: ImageVector,
) {
    HOME(R.string.nav_home, Icons.Default.Home),
    TRANSACTIONS(R.string.nav_transactions, Icons.AutoMirrored.Filled.List),
    INSIGHTS(R.string.nav_insights, Icons.Default.Info),
    MORE(R.string.nav_more, Icons.Default.Settings),
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KharchaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    var isOnboardingCompleted by rememberSaveable {
                        mutableStateOf(userPreferences.isOnboardingCompleted)
                    }

                    if (!isOnboardingCompleted) {
                        OnboardingRoute(
                            onOnboardingComplete = {
                                userPreferences.isOnboardingCompleted = true
                                isOnboardingCompleted = true
                            },
                        )
                    } else {
                        MainAppShell(
                            transactionRepository = transactionRepository,
                            userPreferences = userPreferences,
                            onResetOnboarding = {
                                userPreferences.isOnboardingCompleted = false
                                isOnboardingCompleted = false
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppShell(
    transactionRepository: TransactionRepository,
    userPreferences: UserPreferences,
    onResetOnboarding: () -> Unit,
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestination.HOME) }
    val transactions by transactionRepository.getTransactions().collectAsState(initial = emptyList())
    val context = LocalContext.current
    val accountRepository = remember { DefaultAccountRepository.getInstance(context) }
    val accounts by accountRepository.getAccounts().collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showReviewQueue by rememberSaveable { mutableStateOf(false) }
    var selectedTransactionForDetail by remember { mutableStateOf<Transaction?>(null) }
    var selectedEntityForDetail by remember { mutableStateOf<SelectedEntity?>(null) }
    var showAddTransactionDialog by rememberSaveable { mutableStateOf(false) }
    var filterCategoryForTransactions by rememberSaveable { mutableStateOf<String?>(null) }
    var transactionsFilter by remember { mutableStateOf(TransactionFilter()) }
    var transactionsYearMonth by remember { mutableStateOf<YearMonth?>(YearMonth.now()) }
    var transactionsIsAllMonthsSelected by remember { mutableStateOf(false) }
    var transactionsSubTab by remember { mutableStateOf(TransactionsSubTab.TRANSACTIONS) }

    val showFab = currentDestination == AppDestination.HOME || currentDestination == AppDestination.TRANSACTIONS

    var isPrivacyMode by rememberSaveable { mutableStateOf(userPreferences.isPrivacyModeEnabled) }
    val onTogglePrivacyMode = {
        val newMode = !isPrivacyMode
        isPrivacyMode = newMode
        userPreferences.isPrivacyModeEnabled = newMode
    }

    CompositionLocalProvider(LocalPrivacyMode provides isPrivacyMode) {
        if (selectedTransactionForDetail != null) {
            BackHandler { selectedTransactionForDetail = null }
            val txn = selectedTransactionForDetail!!
            TransactionDetailScreen(
                transaction = txn,
                allTransactions = transactions,
                accounts = accounts,
                onBack = { selectedTransactionForDetail = null },
                onNavigateToTransaction = { txnId ->
                    selectedTransactionForDetail = transactions.find { it.id == txnId }
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
                        transactionRepository.updateTransaction(
                            toDelete.copy(type = TransactionType.Ignored, isReviewed = true),
                        )
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
        } else {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    NavigationBar {
                        AppDestination.entries.forEach { destination ->
                            NavigationBarItem(
                                selected = currentDestination == destination,
                                onClick = { currentDestination = destination },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = stringResource(id = destination.labelRes),
                                    )
                                },
                                label = { Text(text = stringResource(id = destination.labelRes)) },
                            )
                        }
                    }
                },
                floatingActionButton = {
                    if (showFab) {
                        FloatingActionButton(
                            onClick = { showAddTransactionDialog = true },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(id = R.string.fab_add_transaction),
                            )
                        }
                    }
                },
            ) { innerPadding ->
                BoxDestinationContent(
                    destination = currentDestination,
                    transactions = transactions,
                    userPreferences = userPreferences,
                    onNavigateToTransactions = { currentDestination = AppDestination.TRANSACTIONS },
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
                            transactionRepository.updateTransaction(
                                toDelete.copy(type = TransactionType.Ignored, isReviewed = true),
                            )
                        }
                    },
                    onRestoreTransaction = { toRestore ->
                        scope.launch {
                            transactionRepository.updateTransaction(toRestore)
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
                    onResetOnboarding = onResetOnboarding,
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
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }

    if (showReviewQueue) {
        val unreviewed = remember(transactions) { transactions.filter { !it.isReviewed } }
        ReviewQueueScreen(
            unreviewedTransactions = unreviewed,
            onCategorize = { txn, category, propagate ->
                val type =
                    if (category.equals("Transfer", ignoreCase = true)) {
                        TransactionType.Transfer
                    } else {
                        txn.type
                    }
                scope.launch {
                    if (propagate) {
                        val count =
                            transactionRepository.updateTransactionAndPropagateMerchant(
                                transaction = txn,
                                newCategory = category,
                                newType = type,
                                userPreferences = userPreferences,
                            )
                        val merchant = txn.merchantNormalised ?: txn.merchantRaw ?: "Merchant"
                        if (count > 1) {
                            snackbarHostState.showSnackbar("Updated $count transactions for $merchant")
                        }
                    } else {
                        transactionRepository.updateTransaction(
                            txn.copy(categorySource = category, type = type, isReviewed = true),
                        )
                    }
                }
            },
            onIgnore = { txn ->
                scope.launch {
                    transactionRepository.updateTransaction(
                        txn.copy(type = TransactionType.Ignored, isReviewed = true),
                    )
                }
            },
            onBack = { showReviewQueue = false },
        )
    }

    if (showAddTransactionDialog) {
        val txnAddedMsg = stringResource(id = R.string.txn_added)
        AddTransactionDialog(
            onSave = { newTxn ->
                scope.launch {
                    transactionRepository.insertTransaction(newTxn)
                    snackbarHostState.showSnackbar(txnAddedMsg)
                }
            },
            onDismiss = { showAddTransactionDialog = false },
        )
    }
}

@Composable
fun BoxDestinationContent(
    destination: AppDestination,
    transactions: List<Transaction>,
    userPreferences: UserPreferences,
    onNavigateToTransactions: () -> Unit,
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
    onResetOnboarding: () -> Unit,
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
    modifier: Modifier = Modifier,
) {
    when (destination) {
        AppDestination.HOME -> {
            HomeScreen(
                transactions = transactions,
                monthlyBudgetMinor = userPreferences.monthlyBudgetMinor,
                onSeeAllClick = onNavigateToTransactions,
                onReviewClick = onReviewClick,
                onTransactionClick = onTransactionClick,
                onCategoryClick = onCategoryClick,
                onPrivacyToggle = onPrivacyToggle,
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
            )
        }
        AppDestination.INSIGHTS -> {
            InsightsScreen(
                transactions = transactions,
                monthlyBudgetMinor = userPreferences.monthlyBudgetMinor,
                onUpdateMonthlyBudget = { newBudget -> userPreferences.monthlyBudgetMinor = newBudget },
                onTransactionClick = onTransactionClick,
                onCategoryClick = onCategoryClick,
                onMerchantClick = onMerchantClick,
                modifier = modifier,
            )
        }
        AppDestination.MORE -> {
            MoreScreen(
                transactions = transactions,
                userPreferences = userPreferences,
                onResetOnboarding = onResetOnboarding,
                onUpdateMonthlyBudget = { newBudget -> userPreferences.monthlyBudgetMinor = newBudget },
                modifier = modifier,
            )
        }
    }
}
