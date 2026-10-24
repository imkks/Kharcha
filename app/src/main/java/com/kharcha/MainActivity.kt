package com.kharcha

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.kharcha.core.common.CsvExportHelper
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.common.TransactionRepository
import com.kharcha.core.common.UserPreferences
import com.kharcha.core.designsystem.component.AddTransactionDialog
import com.kharcha.core.designsystem.component.ReviewQueueDialog
import com.kharcha.core.designsystem.component.TransactionDetailDialog
import com.kharcha.core.designsystem.theme.KharchaTheme
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import com.kharcha.feature.home.ui.HomeScreen
import com.kharcha.feature.insights.InsightsScreen
import com.kharcha.feature.onboarding.OnboardingRoute
import com.kharcha.feature.transactions.ui.TransactionsScreen
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.launch

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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showReviewQueue by rememberSaveable { mutableStateOf(false) }
    var selectedTransactionForDetail by remember { mutableStateOf<Transaction?>(null) }
    var showAddTransactionDialog by rememberSaveable { mutableStateOf(false) }

    val showFab = currentDestination == AppDestination.HOME || currentDestination == AppDestination.TRANSACTIONS

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
            onResetOnboarding = onResetOnboarding,
            modifier = Modifier.padding(innerPadding),
        )
    }

    if (showReviewQueue) {
        val unreviewed = transactions.filter { !it.isReviewed }
        ReviewQueueDialog(
            unreviewedTransactions = unreviewed,
            onCategorize = { txn, category ->
                val type =
                    if (category.equals("Transfer", ignoreCase = true)) {
                        TransactionType.Transfer
                    } else {
                        txn.type
                    }
                scope.launch {
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
                }
            },
            onIgnore = { txn ->
                scope.launch {
                    transactionRepository.updateTransaction(
                        txn.copy(type = TransactionType.Ignored, isReviewed = true),
                    )
                }
            },
            onDismiss = { showReviewQueue = false },
        )
    }

    selectedTransactionForDetail?.let { txn ->
        TransactionDetailDialog(
            transaction = txn,
            onSave = { updated ->
                scope.launch {
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
                    }
                }
                selectedTransactionForDetail = null
            },
            onDismiss = { selectedTransactionForDetail = null },
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
    onResetOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (destination) {
        AppDestination.HOME -> {
            val totalIncome =
                remember(transactions) {
                    transactions.filter { it.type == TransactionType.Income }.sumOf { it.amount }
                }
            val totalExpense =
                remember(transactions) {
                    transactions.filter { it.type == TransactionType.Expense }.sumOf { it.amount }
                }
            val netAmount = totalIncome - totalExpense
            val needsReviewCount =
                remember(transactions) {
                    transactions.count { !it.isReviewed }
                }

            HomeScreen(
                netAmount = netAmount,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                needsReviewCount = needsReviewCount,
                recentTransactions = transactions.take(10),
                monthlyBudgetMinor = userPreferences.monthlyBudgetMinor,
                onSeeAllClick = onNavigateToTransactions,
                onReviewClick = onReviewClick,
                onTransactionClick = onTransactionClick,
            )
        }
        AppDestination.TRANSACTIONS -> {
            TransactionsScreen(
                transactions = transactions,
                onTransactionClick = onTransactionClick,
            )
        }
        AppDestination.INSIGHTS -> {
            InsightsScreen(
                transactions = transactions,
                onTransactionClick = onTransactionClick,
                modifier = modifier,
            )
        }
        AppDestination.MORE -> {
            MoreSettingsScreen(
                transactions = transactions,
                userPreferences = userPreferences,
                onResetOnboarding = onResetOnboarding,
                modifier = modifier,
            )
        }
    }
}

@Composable
fun MoreSettingsScreen(
    transactions: List<Transaction>,
    userPreferences: UserPreferences,
    onResetOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val rulesCount = userPreferences.getMerchantCategoryRules().size
    val unreviewedCount =
        transactions.count {
            !it.isReviewed || it.categorySource == "Other" || (it.parseConfidence ?: 1.0f) < 0.8f
        }

    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = stringResource(id = R.string.nav_more),
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.more_privacy_badge),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (rulesCount > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(id = R.string.more_rules_count, rulesCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.more_budget_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = MoneyFormatter.formatMinorUnits(userPreferences.monthlyBudgetMinor),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.more_budget_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.more_export_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.more_export_csv_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val csv = CsvExportHelper.generateTransactionsCsv(transactions)
                            val filename = "kharcha_transactions_${System.currentTimeMillis()}.csv"
                            shareTextFile(context, csv, filename, "text/csv")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(id = R.string.more_export_csv_btn, transactions.size))
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(id = R.string.more_export_llm_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.more_export_llm_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            val json = CsvExportHelper.generateLlmBatchExportJson(transactions)
                            val filename = "kharcha_llm_batch_${System.currentTimeMillis()}.json"
                            shareTextFile(context, json, filename, "application/json")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = stringResource(id = R.string.more_export_llm_btn, unreviewedCount))
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.more_rerun_onboarding),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.more_rerun_onboarding_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = onResetOnboarding) {
                        Text(text = stringResource(id = R.string.more_rerun_onboarding))
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
