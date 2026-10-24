package com.kharcha.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.theme.financialColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun SpendCalendarHeatmap(
    days: List<DaySpendIntensity>,
    modifier: Modifier = Modifier,
    onDaySelected: (DaySpendIntensity) -> Unit = {},
) {
    if (days.isEmpty()) return

    var selectedDay by remember { mutableStateOf(days.lastOrNull()) }
    val dayHeaders = listOf("M", "T", "W", "T", "F", "S", "S")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(id = R.string.insights_heatmap_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(id = R.string.insights_heatmap_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                dayHeaders.forEach { header ->
                    Text(
                        text = header,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val weeks = days.chunked(7)
            weeks.forEach { week ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    week.forEach { daySpend ->
                        val isSelected = selectedDay?.date == daySpend.date
                        HeatmapCell(
                            daySpend = daySpend,
                            isSelected = isSelected,
                            onClick = {
                                selectedDay = daySpend
                                onDaySelected(daySpend)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            selectedDay?.let { day ->
                val dateStr = day.date.format(DateTimeFormatter.ofPattern("EEE, d MMM"))
                val amountStr = MoneyFormatter.formatMinorUnits(day.totalExpenseMinor)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(
                            id = R.string.insights_heatmap_day_summary,
                            amountStr,
                            day.count,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeatmapCell(
    daySpend: DaySpendIntensity,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cellColor =
        when (daySpend.intensityLevel) {
            1 -> MaterialTheme.financialColors.heatmapLevel1
            2 -> MaterialTheme.financialColors.heatmapLevel2
            3 -> MaterialTheme.financialColors.heatmapLevel3
            4 -> MaterialTheme.financialColors.heatmapLevel4
            else -> MaterialTheme.financialColors.heatmapLevel0
        }

    val accessibilityDesc =
        stringResource(
            id = R.string.insights_heatmap_cell_desc,
            daySpend.date.dayOfMonth,
            MoneyFormatter.formatMinorUnits(daySpend.totalExpenseMinor),
        )

    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(4.dp))
                .background(cellColor)
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp),
                        )
                    } else {
                        Modifier
                    },
                ).clickable(onClick = onClick)
                .semantics { contentDescription = accessibilityDesc },
        contentAlignment = Alignment.Center,
    ) {
        if (daySpend.date == LocalDate.now()) {
            Box(
                modifier =
                    Modifier
                        .size(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White),
            )
        }
    }
}
