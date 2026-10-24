package com.kharcha.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.component.CategoryBadge
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    transactions: List<Transaction>,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedPeriod by remember { mutableStateOf(TimePeriod.THIS_MONTH) }

    val periodTxns =
        remember(transactions, selectedPeriod) {
            InsightsAnalyticsHelper.filterTransactionsByPeriod(transactions, selectedPeriod)
        }

    val expenses =
        remember(periodTxns) {
            periodTxns.filter { it.type == TransactionType.Expense }
        }

    val safeToSpend =
        remember(transactions) {
            InsightsAnalyticsHelper.calculateSafeToSpend(transactions)
        }

    val categoryShares =
        remember(expenses) {
            InsightsAnalyticsHelper.calculateCategoryShares(expenses)
        }

    val channelShares =
        remember(expenses) {
            InsightsAnalyticsHelper.calculateChannelShares(expenses)
        }

    val topMerchants =
        remember(expenses) {
            InsightsAnalyticsHelper.calculateTopMerchants(expenses)
        }

    val totalIncome =
        remember(periodTxns) {
            periodTxns.filter { it.type == TransactionType.Income }.sumOf { it.amount }
        }
    val totalExpense =
        remember(expenses) {
            expenses.sumOf { it.amount }
        }
    val netSavings = totalIncome - totalExpense

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.insights_title)) },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                PeriodSelectorRow(
                    selectedPeriod = selectedPeriod,
                    onSelectPeriod = { selectedPeriod = it },
                )
            }

            if (selectedPeriod == TimePeriod.THIS_MONTH) {
                item {
                    SafeToSpendCard(safeToSpend = safeToSpend)
                }
            }

            item {
                MonthlyFlowCard(
                    incomeMinor = totalIncome,
                    expenseMinor = totalExpense,
                    netSavingsMinor = netSavings,
                )
            }

            if (expenses.isEmpty()) {
                item {
                    EmptyInsightsCard()
                }
            } else {
                if (categoryShares.isNotEmpty()) {
                    item {
                        CategoryBreakdownCard(categoryShares = categoryShares)
                    }
                }

                if (channelShares.isNotEmpty()) {
                    item {
                        PaymentChannelBreakdownCard(channelShares = channelShares)
                    }
                }

                if (topMerchants.isNotEmpty()) {
                    item {
                        TopMerchantsCard(topMerchants = topMerchants)
                    }
                }
            }
        }
    }
}

@Composable
fun PeriodSelectorRow(
    selectedPeriod: TimePeriod,
    onSelectPeriod: (TimePeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            TimePeriod.THIS_MONTH to stringResource(id = R.string.insights_time_this_month),
            TimePeriod.LAST_MONTH to stringResource(id = R.string.insights_time_last_month),
            TimePeriod.ALL_TIME to stringResource(id = R.string.insights_time_all),
        ).forEach { (period, label) ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onSelectPeriod(period) },
                label = { Text(text = label) },
            )
        }
    }
}

@Composable
fun SafeToSpendCard(
    safeToSpend: SafeToSpendData,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.insights_safe_to_spend_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                PacingStatusBadge(status = safeToSpend.pacingStatus)
            }

            Spacer(modifier = Modifier.height(12.dp))

            val safeFormatted = MoneyFormatter.formatMinorUnits(safeToSpend.dailySafeMinor)
            Text(
                text = safeFormatted,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(id = R.string.insights_safe_daily_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(14.dp))

            val budgetFraction =
                if (safeToSpend.monthlyBudgetMinor > 0) {
                    (safeToSpend.totalSpentMinor.toFloat() / safeToSpend.monthlyBudgetMinor.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }

            LinearProgressIndicator(
                progress = { budgetFraction },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val dailyBurnStr = MoneyFormatter.formatMinorUnits(safeToSpend.currentDailyBurnMinor)
                Text(
                    text = stringResource(id = R.string.insights_daily_burn_rate, dailyBurnStr),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(id = R.string.insights_days_remaining, safeToSpend.daysRemaining),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun PacingStatusBadge(status: PacingStatus) {
    val (labelRes, color) =
        when (status) {
            PacingStatus.ON_TRACK ->
                R.string.insights_status_on_track to Color(0xFF2E7D32)
            PacingStatus.WARNING ->
                R.string.insights_status_warning to Color(0xFFED6C02)
            PacingStatus.EXCEEDED ->
                R.string.insights_status_exceeded to MaterialTheme.colorScheme.error
        }

    SuggestionChip(
        onClick = {},
        label = {
            Text(
                text = stringResource(id = labelRes),
                color = color,
                style = MaterialTheme.typography.labelSmall,
            )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = color.copy(alpha = 0.12f)),
        border = null,
    )
}

@Composable
fun MonthlyFlowCard(
    incomeMinor: Long,
    expenseMinor: Long,
    netSavingsMinor: Long,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.insights_summary_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                FlowSummaryItem(
                    title = stringResource(id = R.string.insights_summary_income),
                    amountMinor = incomeMinor,
                    color = Color(0xFF2E7D32),
                )
                FlowSummaryItem(
                    title = stringResource(id = R.string.insights_summary_expense),
                    amountMinor = expenseMinor,
                    color = MaterialTheme.colorScheme.error,
                )
                FlowSummaryItem(
                    title = stringResource(id = R.string.insights_summary_net),
                    amountMinor = netSavingsMinor,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
fun FlowSummaryItem(
    title: String,
    amountMinor: Long,
    color: Color,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = MoneyFormatter.formatMinorUnits(amountMinor),
            style = MaterialTheme.typography.titleMedium,
            color = color,
        )
    }
}

@Composable
fun CategoryBreakdownCard(
    categoryShares: List<CategorySpendShare>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.insights_category_breakdown_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(12.dp))

            categoryShares.take(6).forEach { share ->
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            CategoryBadge(categoryName = share.categoryName)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = share.categoryName,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        val formattedAmount = MoneyFormatter.formatMinorUnits(share.totalMinor)
                        val pctText = "${(share.percentage * 100).toInt()}%"
                        Text(
                            text = "$formattedAmount ($pctText)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { share.percentage },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentChannelBreakdownCard(
    channelShares: List<ChannelSpendShare>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.insights_payment_channel_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                channelShares.forEach { channelShare ->
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp),
                    ) {
                        Column {
                            Text(
                                text = channelShare.channelName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = MoneyFormatter.formatMinorUnits(channelShare.totalMinor),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = "${(channelShare.percentage * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopMerchantsCard(
    topMerchants: List<MerchantSpendShare>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.insights_top_merchants_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(12.dp))
            topMerchants.forEachIndexed { index, merchant ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = merchant.merchantName,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = MoneyFormatter.formatMinorUnits(merchant.totalMinor),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyInsightsCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.insights_empty_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.insights_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
