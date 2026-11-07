package com.kharcha.feature.insights

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.R as DesignR
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType

@Composable
fun WeeklyTrendsCard(
    summary: WeeklyTrendsSummary,
    allTransactions: List<Transaction>,
    selectedWeekIndex: Int,
    onSelectWeek: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val buckets = summary.buckets
    if (buckets.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.financialColors.cardBackground,
        ),
        border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Header stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.trends_grouping_week),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.financialColors.textPrimary,
                    )
                    Text(
                        text = stringResource(
                            id = R.string.trends_week_avg_label,
                            MoneyFormatter.formatMinorUnits(summary.averageWeeklyExpenseMinor),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.financialColors.textSecondary,
                    )
                }
                if (summary.targetWeeklyBudgetMinor > 0L) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.financialColors.chartBlue.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = stringResource(
                                id = R.string.trends_week_budget_pace,
                                MoneyFormatter.formatMinorUnits(summary.targetWeeklyBudgetMinor),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.financialColors.chartBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Canvas Bar Chart for each week
            WeeklyBarChart(
                buckets = buckets,
                selectedWeekIndex = selectedWeekIndex,
                onSelectWeek = onSelectWeek,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.financialColors.hairlineDivider)
            Spacer(modifier = Modifier.height(14.dp))

            // Selected Week Detail Section
            val selectedBucket = buckets.firstOrNull { it.weekIndex == selectedWeekIndex } ?: buckets.first()
            val weekTransactions = remember(allTransactions, selectedBucket) {
                allTransactions.filter {
                    it.dateTime in selectedBucket.startEpochMs..selectedBucket.endEpochMs &&
                        it.type == TransactionType.Expense &&
                        it.isCounted
                }.sortedByDescending { it.dateTime }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = selectedBucket.label + " • " + selectedBucket.dateRangeLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.financialColors.textPrimary,
                    )
                    Text(
                        text = "${selectedBucket.transactionCount} transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.financialColors.textSecondary,
                    )
                }
                Text(
                    text = MoneyFormatter.formatMinorUnits(selectedBucket.totalExpenseMinor),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.financialColors.chartBlue,
                )
            }

            if (weekTransactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    weekTransactions.take(5).forEach { tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.financialColors.raisedRowBackground)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.merchantNormalised ?: tx.merchantRaw ?: "Expense",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.financialColors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = tx.categorySource ?: "Other",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.financialColors.textSecondary,
                                )
                            }
                            Text(
                                text = MoneyFormatter.formatMinorUnits(tx.amount),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.financialColors.textPrimary,
                            )
                        }
                    }
                    if (weekTransactions.size > 5) {
                        Text(
                            text = "+${weekTransactions.size - 5} more transactions",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.financialColors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(id = R.string.trends_no_transactions),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.financialColors.textSecondary,
                )
            }
        }
    }
}

@Composable
fun WeeklyBarChart(
    buckets: List<WeeklyTrendBucket>,
    selectedWeekIndex: Int,
    onSelectWeek: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(buckets) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 600))
    }

    val maxExpense = remember(buckets) {
        maxOf(1L, buckets.maxOfOrNull { it.totalExpenseMinor } ?: 1L)
    }

    val activeBarColor = MaterialTheme.financialColors.chartBlue
    val unselectedBarColor = MaterialTheme.financialColors.chartBlue.copy(alpha = 0.45f)
    val highlightBorderColor = MaterialTheme.financialColors.limeAccent

    Box(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom,
        ) {
            buckets.forEach { bucket ->
                val isSelected = (bucket.weekIndex == selectedWeekIndex)
                val targetHeightFraction = (bucket.totalExpenseMinor.toFloat() / maxExpense.toFloat()).coerceIn(0.04f, 1f)
                val currentHeightFraction = targetHeightFraction * animProgress.value

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectWeek(bucket.weekIndex) }
                        .padding(horizontal = 4.dp),
                ) {
                    // Amount label
                    Text(
                        text = if (bucket.totalExpenseMinor > 0L) {
                            formatCompactCurrency(bucket.totalExpenseMinor)
                        } else {
                            "₹0"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) activeBarColor else MaterialTheme.financialColors.textSecondary,
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Bar container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(110.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((110 * currentHeightFraction).dp),
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                            color = if (isSelected) activeBarColor else unselectedBarColor,
                            border = if (isSelected) BorderStroke(1.5.dp, highlightBorderColor) else null,
                        ) {}
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Week label
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) activeBarColor.copy(alpha = 0.2f) else Color.Transparent,
                    ) {
                        Text(
                            text = "W${bucket.weekIndex}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) activeBarColor else MaterialTheme.financialColors.textSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun formatCompactCurrency(amountMinor: Long): String {
    val rupees = amountMinor / 100
    return when {
        rupees >= 100_000 -> String.format(java.util.Locale.ENGLISH, "₹%.1fL", rupees / 100_000.0)
        rupees >= 1_000 -> String.format(java.util.Locale.ENGLISH, "₹%.1fk", rupees / 1_000.0)
        else -> "₹$rupees"
    }
}
