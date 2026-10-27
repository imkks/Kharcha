package com.kharcha.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kharcha.core.designsystem.R
import com.kharcha.core.designsystem.theme.financialColors

/**
 * Two-tone Account Card (~300dp wide) for the Home Accounts carousel.
 * Left half: pastel/bank tint with bank logo, bank name, and masked number.
 * Right half: darker surface with current balance and direction arrow.
 */
@Composable
fun AccountCard(
    bankName: String,
    accountNumberMasked: String,
    balanceFormatted: String,
    modifier: Modifier = Modifier,
    isWallet: Boolean = false,
    lastTxnInfo: String? = null,
    onClick: () -> Unit = {},
) {
    val cardRadius = 20.dp
    val hairline = MaterialTheme.financialColors.hairlineDivider
    val leftBg = if (isWallet) Color(0xFF1E2836) else Color(0xFF182A20)

    Card(
        modifier =
            modifier
                .width(300.dp)
                .height(130.dp)
                .clip(RoundedCornerShape(cardRadius))
                .clickable { onClick() },
        shape = RoundedCornerShape(cardRadius),
        border = BorderStroke(1.dp, hairline),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.financialColors.cardBackground,
            ),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left half: Bank branding
            Column(
                modifier =
                    Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .background(leftBg)
                        .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF263A2B)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter =
                            painterResource(
                                id = if (isWallet) R.drawable.ic_channel_wallet else R.drawable.ic_bank,
                            ),
                        contentDescription = null,
                        tint = MaterialTheme.financialColors.income,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Column {
                    Text(
                        text = bankName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = accountNumberMasked,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9E9E9E),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Right half: Balance & action
            Column(
                modifier =
                    Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.financialColors.hairlineDivider),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_outward),
                        contentDescription = null,
                        tint = MaterialTheme.financialColors.income,
                        modifier = Modifier.size(16.dp),
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = balanceFormatted,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                    )
                    if (lastTxnInfo != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = lastTxnInfo,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9E9E9E),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
