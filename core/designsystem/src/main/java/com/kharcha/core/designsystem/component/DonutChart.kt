package com.kharcha.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.R
import com.kharcha.core.designsystem.theme.CategoryColors
import com.kharcha.core.designsystem.theme.LocalPrivacyMode
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sqrt

data class DonutSliceData(
    val categoryName: String,
    val amountMinor: Long,
    val color: Color,
    val percentage: Float, // 0..100
)

/**
 * Builds top 5 categories + Others slice data from transaction list.
 */
fun buildCategorySlices(
    transactions: List<Transaction>,
    otherLabel: String = "Others",
): List<DonutSliceData> {
    val expenses = transactions.filter { it.type == TransactionType.Expense && it.amount > 0 }
    val totalExpense = expenses.sumOf { it.amount }
    if (totalExpense <= 0L) return emptyList()

    val grouped =
        expenses
            .groupBy { it.categorySource?.trim().takeUnless { s -> s.isNullOrEmpty() } ?: otherLabel }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }
            .toList()
            .sortedWith(
                compareByDescending<Pair<String, Long>> { it.second }
                    .thenBy { it.first == otherLabel }
                    .thenBy { it.first },
            )

    val top5 = grouped.take(5)
    val remainder = grouped.drop(5)
    val remainderSum = remainder.sumOf { it.second }

    val slices = mutableListOf<DonutSliceData>()
    top5.forEach { (category, amount) ->
        val pct = (amount.toDouble() / totalExpense.toDouble() * 100).toFloat()
        slices.add(
            DonutSliceData(
                categoryName = category,
                amountMinor = amount,
                color = CategoryColors.forCategory(category).color,
                percentage = pct,
            ),
        )
    }

    if (remainderSum > 0L) {
        val remainderPct = (remainderSum.toDouble() / totalExpense.toDouble() * 100).toFloat()
        slices.add(
            DonutSliceData(
                categoryName = otherLabel,
                amountMinor = remainderSum,
                color = CategoryColors.forCategory(null).color,
                percentage = remainderPct,
            ),
        )
    }

    return slices
}

@Composable
fun DonutChart(
    slices: List<DonutSliceData>,
    totalAmountMinor: Long,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    chartSize: Dp = 190.dp,
    strokeWidth: Dp = 28.dp,
) {
    if (slices.isEmpty() || totalAmountMinor <= 0L) return

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    val selectedSlice = slices.find { it.categoryName == selectedCategory }
    val isPrivacy = LocalPrivacyMode.current

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(durationMillis = 750))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Chart with Center Content
            Box(
                modifier = Modifier.size(chartSize),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(
                    modifier =
                        Modifier
                            .size(chartSize)
                            .pointerInput(slices) {
                                detectTapGestures { tapOffset ->
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val dx = tapOffset.x - center.x
                                    val dy = tapOffset.y - center.y
                                    val distance = sqrt(dx * dx + dy * dy)
                                    val outerRadius = size.width / 2f
                                    val innerRadius = outerRadius - strokeWidth.toPx()

                                    if (distance in innerRadius..outerRadius) {
                                        var angle = (atan2(dy, dx) * 180f / PI.toFloat()) + 90f
                                        if (angle < 0f) angle += 360f

                                        var accumulatedAngle = 0f
                                        for (slice in slices) {
                                            val sweep = (slice.percentage / 100f) * 360f
                                            if (angle in accumulatedAngle..(accumulatedAngle + sweep)) {
                                                if (selectedCategory == slice.categoryName) {
                                                    onCategoryClick(slice.categoryName)
                                                } else {
                                                    selectedCategory = slice.categoryName
                                                }
                                                break
                                            }
                                            accumulatedAngle += sweep
                                        }
                                    } else if (distance < innerRadius) {
                                        selectedCategory?.let { onCategoryClick(it) }
                                    }
                                }
                            },
                ) {
                    val strokePx = strokeWidth.toPx()
                    val arcSize = Size(size.width - strokePx, size.height - strokePx)
                    val arcOffset = Offset(strokePx / 2f, strokePx / 2f)

                    var startAngle = -90f
                    slices.forEach { slice ->
                        val sweepAngle = (slice.percentage / 100f) * 360f * animProgress.value
                        val isSelected = slice.categoryName == selectedCategory
                        drawArc(
                            color = slice.color,
                            startAngle = startAngle,
                            sweepAngle = (sweepAngle - 2f).coerceAtLeast(0.5f),
                            useCenter = false,
                            topLeft = arcOffset,
                            size = arcSize,
                            style = Stroke(width = if (isSelected) strokePx + 6f else strokePx, cap = StrokeCap.Round),
                        )
                        startAngle += sweepAngle
                    }
                }

                // Center Label & Amount
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp),
                ) {
                    val label = selectedSlice?.categoryName ?: stringResource(id = R.string.donut_total_expenses)
                    val amountMinor = selectedSlice?.amountMinor ?: totalAmountMinor
                    val formatted =
                        if (isPrivacy) "₹ ••••••" else MoneyFormatter.formatMinorUnits(amountMinor)

                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    selectedSlice?.let {
                        Text(
                            text = String.format("%.1f%%", it.percentage),
                            style = MaterialTheme.typography.labelSmall,
                            color = it.color,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Category Legend
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                slices.forEach { slice ->
                    val isSelected = slice.categoryName == selectedCategory
                    val formattedAmount =
                        if (isPrivacy) "₹ ••••••" else MoneyFormatter.formatMinorUnits(slice.amountMinor)

                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (selectedCategory == slice.categoryName) {
                                        onCategoryClick(slice.categoryName)
                                    } else {
                                        selectedCategory = slice.categoryName
                                    }
                                },
                        color =
                            if (isSelected) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                Color.Transparent
                            },
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(slice.color),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = slice.categoryName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = formattedAmount,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = String.format("%.0f%%", slice.percentage),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(36.dp),
                                        textAlign = TextAlign.End,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { slice.percentage / 100f },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                color = slice.color,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
