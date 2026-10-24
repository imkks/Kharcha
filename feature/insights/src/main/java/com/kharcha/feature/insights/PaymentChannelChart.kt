package com.kharcha.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.theme.LocalPrivacyMode

fun getChannelColor(channelName: String): Color {
    return when (channelName.uppercase()) {
        "UPI" -> Color(0xFF0F766E)
        "CARD" -> Color(0xFF2563EB)
        "ATM" -> Color(0xFFD97706)
        "NEFT", "IMPS" -> Color(0xFF7C3AED)
        "WALLET" -> Color(0xFF059669)
        else -> Color(0xFF64748B)
    }
}

@Composable
fun PaymentChannelBreakdownCard(
    channelShares: List<ChannelSpendShare>,
    modifier: Modifier = Modifier,
) {
    if (channelShares.isEmpty()) return
    val isPrivacy = LocalPrivacyMode.current

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
            Text(
                text = stringResource(id = R.string.insights_payment_channel_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Proportional Multi-Segment Bar
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                channelShares.forEach { share ->
                    if (share.percentage > 0.005f) {
                        Box(
                            modifier =
                                Modifier
                                    .weight(share.percentage)
                                    .fillMaxHeight()
                                    .background(getChannelColor(share.channelName)),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Channel Rows List
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                channelShares.forEach { share ->
                    val color = getChannelColor(share.channelName)
                    val amountFormatted =
                        if (isPrivacy) "₹ ••••••" else MoneyFormatter.formatMinorUnits(share.totalMinor)

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
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
                                            .background(color),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = share.channelName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (share.transactionCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${share.transactionCount})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = amountFormatted,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = color.copy(alpha = 0.12f),
                                ) {
                                    Text(
                                        text = "${(share.percentage * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = color,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LinearProgressIndicator(
                            progress = { share.percentage },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                            color = color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
