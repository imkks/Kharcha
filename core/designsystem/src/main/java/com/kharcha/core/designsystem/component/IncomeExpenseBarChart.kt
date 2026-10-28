package com.kharcha.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.R
import com.kharcha.core.designsystem.theme.LocalPrivacyMode
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class MonthlyComparisonBarData(
    val monthLabel: String,
    val yearMonth: YearMonth,
    val incomeMinor: Long,
    val expenseMinor: Long,
)

/**
 * Calculates monthly income and expense totals for the past [monthsCount] months.
 */
fun calculateMonthlyComparisonData(
    transactions: List<Transaction>,
    monthsCount: Int = 6,
    referenceDate: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<MonthlyComparisonBarData> {
    if (monthsCount <= 0) return emptyList()

    val currentYm = YearMonth.from(referenceDate)
    val startYm = currentYm.minusMonths((monthsCount - 1).toLong())
    val formatter = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())

    val results = mutableListOf<MonthlyComparisonBarData>()
    var cursor = startYm
    while (!cursor.isAfter(currentYm)) {
        val monthStart =
            cursor
                .atDay(1)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
        val monthEnd =
            cursor
                .atEndOfMonth()
                .atTime(23, 59, 59, 999_000_000)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()

        val txnsInMonth = transactions.filter { it.dateTime in monthStart..monthEnd }
        val income =
            txnsInMonth
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }
        val expense =
            txnsInMonth
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

        results.add(
            MonthlyComparisonBarData(
                monthLabel = cursor.format(formatter),
                yearMonth = cursor,
                incomeMinor = income,
                expenseMinor = expense,
            ),
        )
        cursor = cursor.plusMonths(1)
    }

    return results
}

@Composable
fun IncomeExpenseBarChart(
    data: List<MonthlyComparisonBarData>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 150.dp,
    onMonthSelected: ((MonthlyComparisonBarData) -> Unit)? = null,
) {
    if (data.isEmpty()) return

    val isPrivacy = LocalPrivacyMode.current
    var selectedIndex by remember { mutableStateOf(data.lastIndex) }
    val selectedData = data.getOrNull(selectedIndex) ?: data.last()

    val maxAmount =
        remember(data) {
            data.maxOfOrNull { maxOf(it.incomeMinor, it.expenseMinor) }?.coerceAtLeast(1L) ?: 1L
        }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 700))
    }

    val incomeColor = MaterialTheme.financialColors.income
    val expenseColor = MaterialTheme.financialColors.expense
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = surfaceVariantColor.copy(alpha = 0.5f),
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Title & Legends
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.chart_income_vs_expense_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LegendDot(color = incomeColor, label = stringResource(id = R.string.chart_income_legend))
                    LegendDot(color = expenseColor, label = stringResource(id = R.string.chart_expense_legend))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Selected Month Detail Strip
            SelectedMonthDetailStrip(
                data = selectedData,
                isPrivacy = isPrivacy,
                incomeColor = incomeColor,
                expenseColor = expenseColor,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Dual Bar Canvas
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(chartHeight)
                        .pointerInput(data) {
                            detectTapGestures { offset ->
                                val slotWidth = size.width / data.size
                                val index = (offset.x / slotWidth).toInt().coerceIn(0, data.lastIndex)
                                selectedIndex = index
                                onMonthSelected?.invoke(data[index])
                            }
                        },
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val slotWidth = canvasWidth / data.size
                    val barWidth = (slotWidth * 0.28f).coerceAtLeast(6f)
                    val cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)

                    // Baseline indicator
                    drawLine(
                        color = outlineVariantColor,
                        start = Offset(0f, canvasHeight),
                        end = Offset(canvasWidth, canvasHeight),
                        strokeWidth = 1.dp.toPx(),
                    )

                    data.forEachIndexed { index, item ->
                        val slotCenterX = index * slotWidth + slotWidth / 2f
                        val progress = animProgress.value

                        // Income Bar (Left)
                        val incomeFrac = (item.incomeMinor.toFloat() / maxAmount.toFloat()).coerceIn(0f, 1f)
                        val incomeBarHeight = incomeFrac * canvasHeight * progress
                        if (incomeBarHeight > 0f) {
                            val incomeLeft = slotCenterX - barWidth - 2.dp.toPx()
                            drawRoundRect(
                                color = incomeColor,
                                topLeft = Offset(incomeLeft, canvasHeight - incomeBarHeight),
                                size = Size(barWidth, incomeBarHeight),
                                cornerRadius = cornerRadius,
                            )
                        }

                        // Expense Bar (Right)
                        val expenseFrac = (item.expenseMinor.toFloat() / maxAmount.toFloat()).coerceIn(0f, 1f)
                        val expenseBarHeight = expenseFrac * canvasHeight * progress
                        if (expenseBarHeight > 0f) {
                            val expenseLeft = slotCenterX + 2.dp.toPx()
                            drawRoundRect(
                                color = expenseColor,
                                topLeft = Offset(expenseLeft, canvasHeight - expenseBarHeight),
                                size = Size(barWidth, expenseBarHeight),
                                cornerRadius = cornerRadius,
                            )
                        }

                        // Selected Slot Indicator Highlight
                        if (index == selectedIndex) {
                            drawRoundRect(
                                color = outlineVariantColor.copy(alpha = 0.35f),
                                topLeft = Offset(index * slotWidth + 2.dp.toPx(), 0f),
                                size = Size(slotWidth - 4.dp.toPx(), canvasHeight),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Month Labels Row
            Row(modifier = Modifier.fillMaxWidth()) {
                data.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex
                    Text(
                        text = item.monthLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color =
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SelectedMonthDetailStrip(
    data: MonthlyComparisonBarData,
    isPrivacy: Boolean,
    incomeColor: Color,
    expenseColor: Color,
    modifier: Modifier = Modifier,
) {
    val net = data.incomeMinor - data.expenseMinor
    val incomeFormatted = if (isPrivacy) "₹ ••••••" else MoneyFormatter.formatMinorUnits(data.incomeMinor)
    val expenseFormatted = if (isPrivacy) "₹ ••••••" else MoneyFormatter.formatMinorUnits(data.expenseMinor)
    val netFormatted =
        if (isPrivacy) {
            "₹ ••••••"
        } else {
            (if (net >= 0) "+" else "") + MoneyFormatter.formatMinorUnits(net)
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = stringResource(id = R.string.chart_income_legend),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = incomeFormatted,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = incomeColor,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(id = R.string.chart_expense_legend),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = expenseFormatted,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = expenseColor,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = stringResource(id = R.string.chart_net_legend),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = netFormatted,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color =
                    if (net >= 0) {
                        incomeColor
                    } else {
                        expenseColor
                    },
            )
        }
    }
}
