package com.kharcha.feature.home.ui

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.common.PeriodHelper
import com.kharcha.core.designsystem.R as DesignR
import com.kharcha.core.designsystem.component.AmountText
import com.kharcha.core.designsystem.component.DonutChart
import com.kharcha.core.designsystem.component.IncomeExpenseBarChart
import com.kharcha.core.designsystem.component.TransactionRow
import com.kharcha.core.designsystem.component.buildCategorySlices
import com.kharcha.core.designsystem.component.calculateMonthlyComparisonData
import com.kharcha.core.designsystem.theme.LocalPrivacyMode
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.TimePeriod
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import com.kharcha.feature.home.R
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    transactions: List<Transaction> = emptyList(),
    netAmount: Long? = null,
    totalIncome: Long? = null,
    totalExpense: Long? = null,
    needsReviewCount: Int? = null,
    recentTransactions: List<Transaction>? = null,
    monthlyBudgetMinor: Long = 30_000_00L,
    onSeeAllClick: () -> Unit = {},
    onReviewClick: () -> Unit = {},
    onTransactionClick: (Transaction) -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onPrivacyToggle: () -> Unit = {},
) {
    var periodIndex by rememberSaveable { mutableIntStateOf(0) }
    val periods = listOf(TimePeriod.THIS_MONTH, TimePeriod.LAST_MONTH, TimePeriod.ALL_TIME)
    val periodLabels =
        listOf(
            stringResource(id = R.string.home_period_this_month),
            stringResource(id = R.string.home_period_last_month),
            stringResource(id = R.string.home_period_all),
        )

    val selectedPeriod = periods[periodIndex % periods.size]
    val periodTransactions =
        remember(transactions, selectedPeriod) {
            if (transactions.isNotEmpty()) {
                PeriodHelper.filterTransactionsByPeriod(transactions, selectedPeriod)
            } else {
                emptyList()
            }
        }

    val computedIncome =
        remember(periodTransactions, totalIncome) {
            totalIncome
                ?: periodTransactions
                    .filter { it.type == TransactionType.Income }
                    .sumOf { it.amount }
        }
    val computedExpense =
        remember(periodTransactions, totalExpense) {
            totalExpense
                ?: periodTransactions
                    .filter { it.type == TransactionType.Expense }
                    .sumOf { it.amount }
        }
    val computedNet =
        remember(computedIncome, computedExpense, netAmount) {
            netAmount ?: (computedIncome - computedExpense)
        }
    val computedReviewCount =
        remember(transactions, needsReviewCount) {
            needsReviewCount ?: transactions.count { !it.isReviewed }
        }
    val computedRecentTransactions =
        remember(periodTransactions, transactions, recentTransactions) {
            recentTransactions
                ?: if (periodTransactions.isNotEmpty()) {
                    periodTransactions.take(10)
                } else {
                    transactions.take(10)
                }
        }

    val othersLabel = stringResource(id = R.string.home_spending_breakdown_others)
    val categorySlices =
        remember(periodTransactions, transactions, othersLabel) {
            val target = if (periodTransactions.isNotEmpty()) periodTransactions else transactions
            buildCategorySlices(target, othersLabel)
        }

    val monthlyComparisonData =
        remember(transactions) {
            calculateMonthlyComparisonData(transactions)
        }

    val today = LocalDate.now()
    val daysInMonth = today.lengthOfMonth()
    val currentDay = today.dayOfMonth
    val daysRemaining = (daysInMonth - currentDay + 1).coerceAtLeast(1)

    val safeToSpendUiModel =
        remember(computedExpense, monthlyBudgetMinor, daysRemaining, currentDay) {
            val remainingBudget = (monthlyBudgetMinor - computedExpense).coerceAtLeast(0L)
            val dailySafe = remainingBudget / daysRemaining
            val currentDailyBurn = if (currentDay > 0) computedExpense / currentDay else 0L

            val status =
                when {
                    computedExpense > monthlyBudgetMinor -> PacingStatus.EXCEEDED
                    dailySafe > 0 && currentDailyBurn > (dailySafe * 5 / 4) -> PacingStatus.WARNING
                    else -> PacingStatus.ON_TRACK
                }

            SafeToSpendUiModel(
                dailySafeMinor = dailySafe,
                currentDailyBurnMinor = currentDailyBurn,
                totalSpentMinor = computedExpense,
                monthlyBudgetMinor = monthlyBudgetMinor,
                daysRemaining = daysRemaining,
                pacingStatus = status,
            )
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.home_title)) },
                actions = {
                    val isPrivacy = LocalPrivacyMode.current
                    IconButton(onClick = onPrivacyToggle) {
                        Icon(
                            painter =
                                painterResource(
                                    id =
                                        if (isPrivacy) {
                                            DesignR.drawable.ic_visibility_off
                                        } else {
                                            DesignR.drawable.ic_visibility
                                        },
                                ),
                            contentDescription =
                                stringResource(
                                    id =
                                        if (isPrivacy) {
                                            DesignR.string.action_show_amounts
                                        } else {
                                            DesignR.string.action_hide_amounts
                                        },
                                ),
                        )
                    }
                    OutlinedButton(
                        onClick = { periodIndex = (periodIndex + 1) % periodLabels.size },
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text(text = periodLabels[periodIndex % periodLabels.size])
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SummaryCard(
                    netAmount = computedNet,
                    totalIncome = computedIncome,
                    totalExpense = computedExpense,
                )
            }

            if (selectedPeriod == TimePeriod.THIS_MONTH) {
                item {
                    SafeToSpendGaugeCard(data = safeToSpendUiModel)
                }
            }

            if (transactions.isNotEmpty()) {
                item {
                    IncomeExpenseBarChart(data = monthlyComparisonData)
                }
            }

            if (computedReviewCount > 0) {
                item {
                    ReviewCard(
                        count = computedReviewCount,
                        onReviewClick = onReviewClick,
                    )
                }
            }

            if (categorySlices.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(id = R.string.home_spending_breakdown_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DonutChart(
                        slices = categorySlices,
                        totalAmountMinor = computedExpense,
                        onCategoryClick = onCategoryClick,
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(id = R.string.home_recent_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    TextButton(onClick = onSeeAllClick) {
                        Text(text = stringResource(id = R.string.home_see_all))
                    }
                }
            }

            if (computedRecentTransactions.isEmpty()) {
                item {
                    EmptyHomeState()
                }
            } else {
                items(computedRecentTransactions) { txn ->
                    TransactionRow(
                        transaction = txn,
                        onClick = { onTransactionClick(txn) },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun SummaryCard(
    netAmount: Long,
    totalIncome: Long,
    totalExpense: Long,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.home_summary_net),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            val isPrivacy = LocalPrivacyMode.current
            val netDisplay =
                if (isPrivacy) "₹ ••••••" else MoneyFormatter.formatMinorUnits(netAmount)
            Text(
                text = netDisplay,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = DesignR.drawable.ic_trending_up),
                            contentDescription = null,
                            tint = MaterialTheme.financialColors.income,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(id = R.string.home_summary_income),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    AmountText(
                        amountMinor = totalIncome,
                        type = TransactionType.Income,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = DesignR.drawable.ic_trending_down),
                            contentDescription = null,
                            tint = MaterialTheme.financialColors.expense,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(id = R.string.home_summary_expense),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    AmountText(
                        amountMinor = totalExpense,
                        type = TransactionType.Expense,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewCard(
    count: Int,
    onReviewClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(id = R.string.home_review_title, count),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onReviewClick) {
                Text(text = stringResource(id = R.string.home_review_button))
            }
        }
    }
}

@Composable
fun EmptyHomeState(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(id = DesignR.drawable.ic_category_other),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(id = R.string.home_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(id = R.string.home_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
