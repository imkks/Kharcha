package com.kharcha.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.component.CategoryBadge
import com.kharcha.core.designsystem.theme.financialColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun RecurringSubscriptionsSection(
    subscriptions: List<SubscriptionItemUiModel>,
    modifier: Modifier = Modifier,
) {
    if (subscriptions.isEmpty()) return

    val totalMonthlyProjected = subscriptions.sumOf { it.amountMinor }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(id = R.string.insights_recurring_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text =
                            stringResource(
                                id = R.string.insights_recurring_monthly_total,
                                MoneyFormatter.formatMinorUnits(totalMonthlyProjected),
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            subscriptions.forEach { sub ->
                SubscriptionRowItem(item = sub)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun SubscriptionRowItem(item: SubscriptionItemUiModel) {
    val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), item.nextChargeDate)
    val relativeDateStr =
        when {
            daysUntil == 0L -> stringResource(id = R.string.insights_due_today)
            daysUntil == 1L -> stringResource(id = R.string.insights_due_tomorrow)
            daysUntil > 1L -> stringResource(id = R.string.insights_due_in_days, daysUntil)
            else -> item.nextChargeDate.format(DateTimeFormatter.ofPattern("d MMM"))
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(categoryName = item.categoryName, size = 36.dp)
        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.merchantName,
                style = MaterialTheme.typography.titleSmall,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = relativeDateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                item.priceDiffMinor?.let { diff ->
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text =
                            stringResource(
                                id = R.string.insights_price_hike_alert,
                                MoneyFormatter.formatMinorUnits(diff),
                            ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.financialColors.warning,
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = MoneyFormatter.formatMinorUnits(item.amountMinor),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        text = item.cadenceLabel,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                colors =
                    SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                border = null,
                modifier = Modifier.height(24.dp),
            )
        }
    }
}
