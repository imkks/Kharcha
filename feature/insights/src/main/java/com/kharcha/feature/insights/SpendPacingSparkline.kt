package com.kharcha.feature.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

fun calculateCumulativeSpendByDay(
    transactions: List<Transaction>,
    referenceDate: LocalDate = LocalDate.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<Long> {
    val monthStart =
        referenceDate
            .with(TemporalAdjusters.firstDayOfMonth())
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    val currentDay = referenceDate.dayOfMonth

    val currentMonthExpenses =
        transactions.filter {
            it.type == TransactionType.Expense && it.dateTime >= monthStart
        }

    val spendByDay = LongArray(currentDay + 1)
    currentMonthExpenses.forEach { txn ->
        val day = Instant.ofEpochMilli(txn.dateTime).atZone(zoneId).dayOfMonth
        if (day in 1..currentDay) {
            spendByDay[day] += txn.amount
        }
    }

    val cumulative = ArrayList<Long>(currentDay)
    var runningTotal = 0L
    for (d in 1..currentDay) {
        runningTotal += spendByDay[d]
        cumulative.add(runningTotal)
    }

    return cumulative
}

@Composable
fun SpendPacingSparkline(
    cumulativeSpend: List<Long>,
    monthlyBudgetMinor: Long,
    totalDaysInMonth: Int,
    currentDay: Int,
    modifier: Modifier = Modifier,
    height: Dp = 130.dp,
) {
    if (cumulativeSpend.isEmpty() || totalDaysInMonth <= 0) return

    val currentSpend = cumulativeSpend.lastOrNull() ?: 0L
    val idealPaceToday =
        if (totalDaysInMonth > 0) {
            (monthlyBudgetMinor * currentDay.toLong()) / totalDaysInMonth.toLong()
        } else {
            0L
        }

    val isRunningHot = currentSpend > idealPaceToday
    val lineColor =
        if (isRunningHot) {
            MaterialTheme.financialColors.warning
        } else {
            MaterialTheme.financialColors.income
        }

    val maxVal =
        remember(cumulativeSpend, monthlyBudgetMinor) {
            maxOf(monthlyBudgetMinor, cumulativeSpend.maxOrNull() ?: 0L, 1L)
        }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.budget_sparkline_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(id = R.string.budget_sparkline_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LegendItem(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        label = stringResource(id = R.string.budget_sparkline_target_legend),
                    )
                    LegendItem(
                        color = lineColor,
                        label = stringResource(id = R.string.budget_sparkline_actual_legend),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sparkline Canvas
            val outlineVariant = MaterialTheme.colorScheme.outlineVariant
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(height),
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(height)) {
                    val w = size.width
                    val h = size.height

                    // 1. Draw Target Linear Pace Line (Dashed)
                    val targetStartY = h
                    val targetEndY = h - ((monthlyBudgetMinor.toFloat() / maxVal.toFloat()) * h)
                    drawLine(
                        color = outlineVariant,
                        start = Offset(0f, targetStartY),
                        end = Offset(w, targetEndY),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
                        cap = StrokeCap.Round,
                    )

                    // 2. Draw Actual Cumulative Spend Path
                    val stepX = w / totalDaysInMonth.toFloat()
                    val path = Path()
                    val fillPath = Path()

                    fillPath.moveTo(0f, h)

                    cumulativeSpend.forEachIndexed { idx, spend ->
                        val x = (idx + 1) * stepX
                        val frac = (spend.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                        val y = h - (frac * h)

                        if (idx == 0) {
                            path.moveTo(0f, h)
                            path.lineTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                        fillPath.lineTo(x, y)
                    }

                    val lastX = cumulativeSpend.size * stepX
                    fillPath.lineTo(lastX, h)
                    fillPath.close()

                    // Gradient fill underneath
                    drawPath(
                        path = fillPath,
                        brush =
                            Brush.verticalGradient(
                                colors = listOf(lineColor.copy(alpha = 0.25f), Color.Transparent),
                                startY = 0f,
                                endY = h,
                            ),
                    )

                    // Line stroke
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
                    )

                    // Dot at current day
                    val currentFrac = (currentSpend.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                    val currentY = h - (currentFrac * h)
                    drawCircle(
                        color = lineColor,
                        radius = 4.5.dp.toPx(),
                        center = Offset(lastX, currentY),
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // X-Axis Day Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(id = R.string.budget_sparkline_day_start),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(id = R.string.budget_sparkline_today),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(id = R.string.budget_sparkline_day_end, totalDaysInMonth),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LegendItem(
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
