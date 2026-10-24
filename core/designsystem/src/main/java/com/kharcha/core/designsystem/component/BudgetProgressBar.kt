package com.kharcha.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.R
import com.kharcha.core.designsystem.theme.LocalPrivacyMode
import com.kharcha.core.designsystem.theme.financialColors

enum class BudgetTrafficLightStatus {
    ON_TRACK,
    WARNING,
    EXCEEDED,
}

fun calculateBudgetStatus(spentMinor: Long, budgetMinor: Long): BudgetTrafficLightStatus {
    if (budgetMinor <= 0L) return BudgetTrafficLightStatus.EXCEEDED
    val fraction = spentMinor.toDouble() / budgetMinor.toDouble()
    return when {
        fraction >= 1.0 -> BudgetTrafficLightStatus.EXCEEDED
        fraction >= 0.80 -> BudgetTrafficLightStatus.WARNING
        else -> BudgetTrafficLightStatus.ON_TRACK
    }
}

@Composable
fun BudgetProgressBar(
    spentMinor: Long,
    budgetMinor: Long,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    showStatusSubtitle: Boolean = true,
    daysRemaining: Int? = null,
) {
    val status = remember(spentMinor, budgetMinor) {
        calculateBudgetStatus(spentMinor, budgetMinor)
    }

    val statusColor = when (status) {
        BudgetTrafficLightStatus.ON_TRACK -> MaterialTheme.financialColors.income
        BudgetTrafficLightStatus.WARNING -> MaterialTheme.financialColors.warning
        BudgetTrafficLightStatus.EXCEEDED -> MaterialTheme.financialColors.expense
    }

    val targetFraction = remember(spentMinor, budgetMinor) {
        if (budgetMinor <= 0L) 1f else (spentMinor.toFloat() / budgetMinor.toFloat()).coerceIn(0f, 1f)
    }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(targetFraction) {
        animProgress.snapTo(0f)
        animProgress.animateTo(targetFraction, animationSpec = tween(durationMillis = 650))
    }

    val isPrivacy = LocalPrivacyMode.current
    val remainingMinor = (budgetMinor - spentMinor).coerceAtLeast(0L)
    val excessMinor = (spentMinor - budgetMinor).coerceAtLeast(0L)

    val remainingDisplay = if (isPrivacy) "₹ ••••••" else MoneyFormatter.formatMinorUnits(remainingMinor)
    val excessDisplay = if (isPrivacy) "₹ ••••••" else MoneyFormatter.formatMinorUnits(excessMinor)

    Column(modifier = modifier.fillMaxWidth()) {
        // Track & Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animProgress.value)
                    .height(height)
                    .clip(RoundedCornerShape(height / 2))
                    .background(statusColor),
            )
        }

        if (showStatusSubtitle) {
            Spacer(modifier = Modifier.height(6.dp))
            val subtitleText = when (status) {
                BudgetTrafficLightStatus.EXCEEDED -> {
                    stringResource(id = R.string.budget_exceeded_by, excessDisplay)
                }
                BudgetTrafficLightStatus.WARNING -> {
                    stringResource(id = R.string.budget_left_pacing_high, remainingDisplay)
                }
                BudgetTrafficLightStatus.ON_TRACK -> {
                    if (daysRemaining != null) {
                        stringResource(id = R.string.budget_left_with_days, remainingDisplay, daysRemaining)
                    } else {
                        stringResource(id = R.string.budget_left_on_track, remainingDisplay)
                    }
                }
            }

            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodySmall,
                color = statusColor,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
