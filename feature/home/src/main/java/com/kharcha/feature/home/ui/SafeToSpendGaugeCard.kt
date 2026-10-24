package com.kharcha.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.feature.home.R

enum class PacingStatus {
    ON_TRACK,
    WARNING,
    EXCEEDED,
}

data class SafeToSpendUiModel(
    val dailySafeMinor: Long,
    val currentDailyBurnMinor: Long,
    val totalSpentMinor: Long,
    val monthlyBudgetMinor: Long,
    val daysRemaining: Int,
    val pacingStatus: PacingStatus,
)

private data class PacingChipConfig(
    val labelRes: Int,
    val icon: ImageVector,
    val textColor: Color,
    val containerColor: Color,
)

@Composable
fun SafeToSpendGaugeCard(
    data: SafeToSpendUiModel,
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
                    text = stringResource(id = R.string.home_safe_to_spend_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                PacingStatusPill(status = data.pacingStatus)
            }

            Spacer(modifier = Modifier.height(12.dp))

            val safeFormatted = MoneyFormatter.formatMinorUnits(data.dailySafeMinor)
            Text(
                text = safeFormatted,
                style = MaterialTheme.typography.displaySmall,
                color =
                    when (data.pacingStatus) {
                        PacingStatus.ON_TRACK -> MaterialTheme.financialColors.income
                        PacingStatus.WARNING -> MaterialTheme.financialColors.warning
                        PacingStatus.EXCEEDED -> MaterialTheme.colorScheme.error
                    },
            )
            Text(
                text = stringResource(id = R.string.home_safe_daily_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            val budgetFraction =
                if (data.monthlyBudgetMinor > 0) {
                    (data.totalSpentMinor.toFloat() / data.monthlyBudgetMinor.toFloat()).coerceIn(0f, 1f)
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
                color =
                    when (data.pacingStatus) {
                        PacingStatus.ON_TRACK -> MaterialTheme.colorScheme.primary
                        PacingStatus.WARNING -> MaterialTheme.financialColors.warning
                        PacingStatus.EXCEEDED -> MaterialTheme.colorScheme.error
                    },
                trackColor = MaterialTheme.colorScheme.surface,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val dailyBurnStr = MoneyFormatter.formatMinorUnits(data.currentDailyBurnMinor)
                Text(
                    text = stringResource(id = R.string.home_daily_burn_rate, dailyBurnStr),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(id = R.string.home_days_remaining, data.daysRemaining),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun PacingStatusPill(status: PacingStatus) {
    val config =
        when (status) {
            PacingStatus.ON_TRACK ->
                PacingChipConfig(
                    labelRes = R.string.home_status_on_track,
                    icon = Icons.Default.CheckCircle,
                    textColor = MaterialTheme.financialColors.income,
                    containerColor = MaterialTheme.financialColors.incomeContainer,
                )
            PacingStatus.WARNING ->
                PacingChipConfig(
                    labelRes = R.string.home_status_warning,
                    icon = Icons.Default.Warning,
                    textColor = MaterialTheme.financialColors.warning,
                    containerColor = MaterialTheme.financialColors.warningContainer,
                )
            PacingStatus.EXCEEDED ->
                PacingChipConfig(
                    labelRes = R.string.home_status_exceeded,
                    icon = Icons.Default.Warning,
                    textColor = MaterialTheme.colorScheme.error,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                )
        }

    SuggestionChip(
        onClick = {},
        icon = {
            Icon(
                imageVector = config.icon,
                contentDescription = null,
                tint = config.textColor,
                modifier = Modifier.size(16.dp),
            )
        },
        label = {
            Text(
                text = stringResource(id = config.labelRes),
                color = config.textColor,
                style = MaterialTheme.typography.labelSmall,
            )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = config.containerColor),
        border = null,
    )
}
