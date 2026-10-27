package com.kharcha.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kharcha.core.designsystem.R
import com.kharcha.core.designsystem.theme.financialColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Speedometer gauge component used in the Budget Planner header.
 * Renders an arc gauge in chart blue (#4B4FE6) with accent particles at top right,
 * followed by a motivational heading and caption.
 */
@Composable
fun BudgetGauge(
    modifier: Modifier = Modifier,
    fillFraction: Float = 0.65f,
) {
    val chartBlue = MaterialTheme.financialColors.chartBlue
    val highlightBlue = MaterialTheme.financialColors.chartBlueHighlight
    val trackColor = MaterialTheme.financialColors.hairlineDivider

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.financialColors.cardBackground,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(160.dp, 100.dp)
                        .padding(top = 8.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    val strokeWidth = 14.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                    // Background track (180 to 360 degrees, i.e. top half semicircle)
                    drawArc(
                        color = trackColor,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )

                    // Active gauge progress arc
                    val sweep = (180f * fillFraction.coerceIn(0.05f, 1f))
                    drawArc(
                        color = chartBlue,
                        startAngle = 180f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )

                    // Pointer needle dot at end of active progress
                    val rad = Math.toRadians((180.0 + sweep).toDouble())
                    val radius = (size.width - strokeWidth) / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dotX = center.x + (radius * cos(rad)).toFloat()
                    val dotY = center.y + (radius * sin(rad)).toFloat()

                    drawCircle(
                        color = Color.White,
                        radius = 6.dp.toPx(),
                        center = Offset(dotX, dotY),
                    )

                    // Star / particle accents at top right
                    drawCircle(
                        color = highlightBlue,
                        radius = 2.5.dp.toPx(),
                        center = Offset(size.width * 0.88f, size.height * 0.12f),
                    )
                    drawCircle(
                        color = chartBlue,
                        radius = 1.5.dp.toPx(),
                        center = Offset(size.width * 0.95f, size.height * 0.25f),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(id = R.string.budget_gauge_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(id = R.string.budget_gauge_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9E9E9E),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}
