package com.kharcha.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kharcha.core.designsystem.R
import com.kharcha.core.designsystem.theme.financialColors

/**
 * PromoCard component (Deposits / Investments).
 * Bordered dark card with title, 4 bullet benefits, money illustration, and a lime "Invest Now!" link.
 */
@Composable
fun PromoCard(
    modifier: Modifier = Modifier,
    onActionClick: () -> Unit = {},
) {
    val hairline = MaterialTheme.financialColors.hairlineDivider
    val lime = MaterialTheme.financialColors.income

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, hairline),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.financialColors.cardBackground,
            ),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.promo_fd_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val bullets =
                        listOf(
                            R.string.promo_fd_bullet1,
                            R.string.promo_fd_bullet2,
                            R.string.promo_fd_bullet3,
                            R.string.promo_fd_bullet4,
                        )

                    for (b in bullets) {
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(lime),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = b),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD1D5DB),
                                fontSize = 12.sp,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier =
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E3A24)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_category_investments),
                        contentDescription = null,
                        tint = lime,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier =
                    Modifier
                        .clickable { onActionClick() }
                        .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(id = R.string.promo_fd_action),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = lime,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_outward),
                    contentDescription = null,
                    tint = lime,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
