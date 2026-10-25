package com.kharcha.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable shimmer brush modifier for loading placeholder states.
 * Respects theme tokens and dark/light modes.
 * Follows UI Spec Section 6 and Research Recommendation #9.
 */
fun Modifier.shimmerEffect(
    shape: Shape = RoundedCornerShape(4.dp),
    baseColor: Color? = null,
    highlightColor: Color? = null,
): Modifier =
    composed {
        val transition = rememberInfiniteTransition(label = "ShimmerTransition")
        val translateAnim =
            transition.animateFloat(
                initialValue = 0f,
                targetValue = 1200f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = 1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                label = "ShimmerTranslate",
            )

        val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
        val base = baseColor ?: surfaceVariant.copy(alpha = 0.6f)
        val highlight = highlightColor ?: surfaceVariant.copy(alpha = 0.2f)

        val brush =
            Brush.linearGradient(
                colors = listOf(base, highlight, base),
                start = Offset(translateAnim.value - 400f, translateAnim.value - 400f),
                end = Offset(translateAnim.value, translateAnim.value),
            )

        this
            .clip(shape)
            .background(brush = brush)
    }

@Composable
fun TransactionRowSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Category icon circle
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .shimmerEffect(shape = CircleShape),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            // Merchant title bar
            Box(
                modifier =
                    Modifier
                        .width(130.dp)
                        .height(16.dp)
                        .shimmerEffect(shape = RoundedCornerShape(4.dp)),
            )
            Spacer(modifier = Modifier.height(6.dp))
            // Category & metadata subtitle bar
            Box(
                modifier =
                    Modifier
                        .width(90.dp)
                        .height(12.dp)
                        .shimmerEffect(shape = RoundedCornerShape(4.dp)),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        // Amount bar
        Box(
            modifier =
                Modifier
                    .width(65.dp)
                    .height(18.dp)
                    .shimmerEffect(shape = RoundedCornerShape(4.dp)),
        )
    }
}

@Composable
fun SummaryCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            ),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Label
            Box(
                modifier =
                    Modifier
                        .width(80.dp)
                        .height(12.dp)
                        .shimmerEffect(shape = RoundedCornerShape(4.dp)),
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Hero Amount
            Box(
                modifier =
                    Modifier
                        .width(160.dp)
                        .height(32.dp)
                        .shimmerEffect(shape = RoundedCornerShape(6.dp)),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier =
                            Modifier
                                .width(70.dp)
                                .height(12.dp)
                                .shimmerEffect(shape = RoundedCornerShape(4.dp)),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier =
                            Modifier
                                .width(80.dp)
                                .height(16.dp)
                                .shimmerEffect(shape = RoundedCornerShape(4.dp)),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier =
                            Modifier
                                .width(70.dp)
                                .height(12.dp)
                                .shimmerEffect(shape = RoundedCornerShape(4.dp)),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier =
                            Modifier
                                .width(80.dp)
                                .height(16.dp)
                                .shimmerEffect(shape = RoundedCornerShape(4.dp)),
                    )
                }
            }
        }
    }
}

@Composable
fun CardSkeleton(
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    shape: Shape = RoundedCornerShape(16.dp),
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(height)
                .shimmerEffect(shape = shape),
    )
}
