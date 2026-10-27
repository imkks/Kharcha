package com.kharcha.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.R as DesignR
import com.kharcha.core.designsystem.theme.LocalPrivacyMode
import com.kharcha.core.designsystem.theme.financialColors

/**
 * Axio-style large circular progress ring (UI Specification Section 3.1 & 4).
 * Features:
 * - 190dp outer diameter with a circular progress arc.
 * - Thin grey track when no budget is set.
 * - Fills with lime-to-amber progress as spend approaches monthly budget.
 * - Center displays "↗" direction glyph and large spent amount.
 * - Tappable to open "All spends".
 */
@Composable
fun SpendRing(
    spentMinor: Long,
    monthlyBudgetMinor: Long,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp,
    strokeWidth: Dp = 6.dp,
    onClick: (() -> Unit)? = null,
) {
    val isPrivacy = LocalPrivacyMode.current
    val progressFraction =
        remember(spentMinor, monthlyBudgetMinor) {
            if (monthlyBudgetMinor > 0L) {
                (spentMinor.toFloat() / monthlyBudgetMinor.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
        }

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(progressFraction) {
        animatedProgress.animateTo(
            targetValue = progressFraction,
            animationSpec = tween(durationMillis = 750),
        )
    }

    val trackColor = MaterialTheme.financialColors.hairlineDivider
    val limeColor = MaterialTheme.financialColors.limeAccent
    val warningColor = MaterialTheme.financialColors.warning
    val dangerColor = Color(0xFFF87171)

    val progressColor =
        when {
            monthlyBudgetMinor <= 0L -> limeColor
            spentMinor > monthlyBudgetMinor -> dangerColor
            spentMinor > (monthlyBudgetMinor * 8 / 10) -> warningColor
            else -> limeColor
        }

    val displayAmount =
        if (isPrivacy) {
            "₹ ••••••"
        } else {
            MoneyFormatter.formatMinorUnits(spentMinor)
        }

    Box(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable { onClick() }
                    } else {
                        Modifier
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(size - (strokeWidth * 2))) {
            val strokePx = strokeWidth.toPx()

            // Background circular track
            drawCircle(
                color = trackColor,
                style = Stroke(width = strokePx),
            )

            // Progress arc (starts at -90deg / top)
            if (animatedProgress.value > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress.value,
                    useCenter = false,
                    style =
                        Stroke(
                            width = strokePx,
                            cap = StrokeCap.Round,
                        ),
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Icon(
                painter = painterResource(id = DesignR.drawable.ic_arrow_outward),
                contentDescription = null,
                tint = MaterialTheme.financialColors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = displayAmount,
                color = MaterialTheme.financialColors.textPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 38.sp,
            )
        }
    }
}
