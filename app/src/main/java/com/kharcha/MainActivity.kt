package com.kharcha

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.TransactionRepository
import com.kharcha.core.designsystem.component.ReviewQueueDialog
import com.kharcha.core.designsystem.component.TransactionDetailDialog
import com.kharcha.core.designsystem.theme.KharchaTheme
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import com.kharcha.feature.home.ui.HomeScreen
import com.kharcha.feature.onboarding.OnboardingRoute
import com.kharcha.feature.transactions.ui.TransactionsScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppDestination(
    val labelRes: Int,
    val icon: ImageVector,
) {
    HOME(R.string.nav_home, Icons.Default.Home),
    TRANSACTIONS(R.string.nav_transactions, Icons.Default.List),
    INSIGHTS(R.string.nav_insights, Icons.Default.Info),
    MORE(R.string.nav_more, Icons.Default.Settings),
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var transactionRepository: TransactionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KharchaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    var isOnboardingCompleted by rememberSaveable { mutableStateOf(false) }

                    if (!isOnboardingCompleted) {
                        OnboardingRoute(
                            onOnboardingComplete = { isOnboardingCompleted = true },
                        )
                    } else {
                        MainAppShell(transactionRepository = transactionRepository)
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppShell(transactionRepository: TransactionRepository) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestination.HOME) }
    val transactions by transactionRepository.getTransactions().collectAsState(initial = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val addTxnComingSoon = stringResource(id = R.string.add_transaction_coming_soon)
    var showReviewQueue by rememberSaveable { mutableStateOf(false) }
    var selectedTransactionForDetail by remember { mutableStateOf<Transaction?>(null) }

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
                    onClick = {
                        scope.launch {
                            snackbarHostState.showSnackbar(addTxnComingSoon)
                        }
                    },
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
            onNavigateToTransactions = { currentDestination = AppDestination.TRANSACTIONS },
            onReviewClick = { showReviewQueue = true },
            onTransactionClick = { txn -> selectedTransactionForDetail = txn },
            modifier = Modifier.padding(innerPadding),
        )
    }

    if (showReviewQueue) {
        val unreviewed = transactions.filter { !it.isReviewed }
        ReviewQueueDialog(
            unreviewedTransactions = unreviewed,
            onCategorize = { txn, category ->
                scope.launch {
                    transactionRepository.updateTransaction(
                        txn.copy(categorySource = category, isReviewed = true),
                    )
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
                    transactionRepository.updateTransaction(updated)
                }
                selectedTransactionForDetail = null
            },
            onDismiss = { selectedTransactionForDetail = null },
        )
    }
}

@Composable
fun BoxDestinationContent(
    destination: AppDestination,
    transactions: List<Transaction>,
    onNavigateToTransactions: () -> Unit,
    onReviewClick: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
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
            PlaceholderDestination(
                title = stringResource(id = R.string.insights_coming_soon),
                subtitle = stringResource(id = R.string.insights_subtitle),
                modifier = modifier,
            )
        }
        AppDestination.MORE -> {
            PlaceholderDestination(
                title = stringResource(id = R.string.more_coming_soon),
                subtitle = stringResource(id = R.string.more_subtitle),
                modifier = modifier,
            )
        }
    }
}

@Composable
fun PlaceholderDestination(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
