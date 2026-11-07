package com.kharcha.feature.insights

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DailyTrendsCard(
    summary: DailyTrendsSummary,
    allTransactions: List<Transaction>,
    selectedDayOfMonth: Int,
    onSelectDayOfMonth: (Int) -> Unit,
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
                        text = stringResource(id = R.string.trends_grouping_day),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.financialColors.textPrimary,
                    )
                    Text(
                        text = stringResource(
                            id = R.string.trends_daily_avg_spend,
                            MoneyFormatter.formatMinorUnits(summary.averageDailySpendMinor),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.financialColors.textSecondary,
                    )
                }
                if (summary.dailySafeMinor > 0L) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.financialColors.limeAccent.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = stringResource(
                                id = R.string.trends_daily_safe_line,
                                MoneyFormatter.formatMinorUnits(summary.dailySafeMinor),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.financialColors.limeAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Planning metric chips: Zero spend days & remaining pace
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.financialColors.raisedRowBackground,
                    border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.financialColors.chartBlue),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.trends_daily_zero_spend_days, summary.zeroSpendDaysCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.financialColors.textSecondary,
                        )
                    }
                }

                if (summary.daysRemaining > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.financialColors.raisedRowBackground,
                        border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.financialColors.limeAccent),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${summary.daysRemaining} days left",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.financialColors.textSecondary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Option A: Horizontal Scrollable Daily Bar Chart
            DailyBarChart(
                buckets = buckets,
                dailySafeMinor = summary.dailySafeMinor,
                selectedDayOfMonth = selectedDayOfMonth,
                onSelectDay = onSelectDayOfMonth,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp),
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.financialColors.hairlineDivider)
            Spacer(modifier = Modifier.height(14.dp))

            // Selected Day Planning & Breakdown Card
            val selectedBucket = buckets.firstOrNull { it.dayOfMonth == selectedDayOfMonth } ?: buckets.first()
            val dayTransactions = remember(allTransactions, selectedBucket) {
                allTransactions.filter {
                    val epochDay = java.time.Instant.ofEpochMilli(it.dateTime)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                    epochDay == selectedBucket.date &&
                        it.type == TransactionType.Expense &&
                        it.isCounted
                }.sortedByDescending { it.dateTime }
            }

            val dateTitleFormatter = remember { DateTimeFormatter.ofPattern("d MMM (EEE)", Locale.ENGLISH) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = selectedBucket.date.format(dateTitleFormatter),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.financialColors.textPrimary,
                        )
                        if (selectedBucket.isToday) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.financialColors.limeAccent.copy(alpha = 0.2f),
                            ) {
                                Text(
                                    text = "Today",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.financialColors.limeAccent,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }

                    // Pacing indicator chip
                    val statusText = when {
                        selectedBucket.totalExpenseMinor == 0L -> stringResource(id = R.string.trends_daily_zero_spend)
                        selectedBucket.isOverSafeDaily -> stringResource(
                            id = R.string.trends_daily_over_budget,
                            MoneyFormatter.formatMinorUnits(selectedBucket.varianceMinor),
                        )
                        else -> stringResource(id = R.string.trends_daily_under_budget)
                    }
                    val statusColor = when {
                        selectedBucket.totalExpenseMinor == 0L -> MaterialTheme.financialColors.textSecondary
                        selectedBucket.isOverSafeDaily -> Color(0xFFEF5350)
                        else -> MaterialTheme.financialColors.limeAccent
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor,
                        fontWeight = FontWeight.Medium,
                    )
                }

                Text(
                    text = MoneyFormatter.formatMinorUnits(selectedBucket.totalExpenseMinor),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedBucket.isOverSafeDaily) Color(0xFFEF5350) else MaterialTheme.financialColors.textPrimary,
                )
            }

            if (dayTransactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    dayTransactions.forEach { tx ->
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
fun DailyBarChart(
    buckets: List<DailyTrendBucket>,
    dailySafeMinor: Long,
    selectedDayOfMonth: Int,
    onSelectDay: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(buckets) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 600))
    }

    val maxExpense = remember(buckets, dailySafeMinor) {
        maxOf(dailySafeMinor, buckets.maxOfOrNull { it.totalExpenseMinor } ?: 1L, 1L)
    }

    val scrollState = rememberScrollState()

    // Scroll to selected day or today on initial display
    LaunchedEffect(selectedDayOfMonth) {
        val targetIndex = (selectedDayOfMonth - 1).coerceIn(0, buckets.size - 1)
        val targetScroll = (targetIndex * 40 - 100).coerceAtLeast(0)
        scrollState.animateScrollTo(targetScroll)
    }

    val normalBarColor = MaterialTheme.financialColors.chartBlue
    val overBudgetBarColor = Color(0xFFEF5350)
    val highlightBorderColor = MaterialTheme.financialColors.limeAccent

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            buckets.forEach { bucket ->
                val isSelected = (bucket.dayOfMonth == selectedDayOfMonth)
                val targetHeightFraction = if (bucket.totalExpenseMinor > 0L) {
                    (bucket.totalExpenseMinor.toFloat() / maxExpense.toFloat()).coerceIn(0.06f, 1f)
                } else {
                    0.02f
                }
                val currentHeightFraction = targetHeightFraction * animProgress.value

                val barColor = when {
                    bucket.isFuture -> MaterialTheme.financialColors.hairlineDivider
                    bucket.isOverSafeDaily -> overBudgetBarColor
                    bucket.totalExpenseMinor == 0L -> MaterialTheme.financialColors.hairlineDivider
                    else -> normalBarColor
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(34.dp)
                        .clickable { onSelectDay(bucket.dayOfMonth) },
                ) {
                    // Daily amount compact indicator
                    Text(
                        text = if (bucket.totalExpenseMinor > 0L && !bucket.isFuture) {
                            formatDayCompactAmount(bucket.totalExpenseMinor)
                        } else {
                            ""
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (bucket.isOverSafeDaily) overBudgetBarColor else MaterialTheme.financialColors.textSecondary,
                        maxLines = 1,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Bar
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(105.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((105 * currentHeightFraction).dp),
                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                            color = if (isSelected) barColor else barColor.copy(alpha = if (bucket.isFuture) 0.3f else 0.7f),
                            border = if (isSelected) BorderStroke(1.5.dp, highlightBorderColor) else null,
                        ) {}
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Day of week / Day of month label
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isSelected) normalBarColor.copy(alpha = 0.2f) else if (bucket.isToday) MaterialTheme.financialColors.limeAccent.copy(alpha = 0.15f) else Color.Transparent,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "${bucket.dayOfMonth}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontWeight = if (isSelected || bucket.isToday) FontWeight.Bold else FontWeight.Medium,
                                color = if (bucket.isToday) MaterialTheme.financialColors.limeAccent else MaterialTheme.financialColors.textPrimary,
                            )
                            Text(
                                text = bucket.dayOfWeekLabel.take(1),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.financialColors.textSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDayCompactAmount(amountMinor: Long): String {
    val rupees = amountMinor / 100
    return when {
        rupees >= 1_000 -> String.format(Locale.ENGLISH, "₹%.1fk", rupees / 1_000.0)
        else -> "₹$rupees"
    }
}
