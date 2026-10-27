package com.kharcha.core.designsystem.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kharcha.core.common.DateTimeFormatterHelper
import com.kharcha.core.designsystem.R
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRow(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
) {
    val isCounted =
        transaction.type == TransactionType.Expense ||
            transaction.type == TransactionType.Income

    val merchantDisplay =
        transaction.merchantNormalised
            ?: transaction.merchantRaw
            ?: stringResource(id = R.string.unknown_merchant)

    val needsReviewLabel = stringResource(id = R.string.row_needs_review)
    val defaultSubtitle = stringResource(id = R.string.row_default_subtitle)

    val subtitle =
        buildString {
            transaction.categorySource?.let { append(it) }
            transaction.channel?.let {
                if (isNotEmpty()) append(" • ")
                append(it.name)
            }
            if (!transaction.isReviewed) {
                if (isNotEmpty()) append(" • ")
                append(needsReviewLabel)
            }
        }.ifEmpty { defaultSubtitle }

    val relativeTime = DateTimeFormatterHelper.formatRelativeTime(transaction.dateTime)

    val isOnline =
        remember(merchantDisplay) {
            val lower = merchantDisplay.lowercase()
            val onlineKeywords =
                listOf(
                    "swiggy", "blinkit", "zomato", "amazon", "flipkart", "uber", "ola", "zepto",
                    "hunger box", "hungerbox", "bigbasket", "bbdaily", "myntra", "dunzo", "instamart",
                )
            onlineKeywords.any { lower.contains(it) }
        }

    val rowModifier =
        if (!isCounted) {
            modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .alpha(0.45f)
                .blur(1.dp)
                .padding(vertical = 10.dp, horizontal = 4.dp)
        } else {
            modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .padding(vertical = 10.dp, horizontal = 4.dp)
        }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        if (isSelectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            CategoryBadge(categoryName = transaction.categorySource ?: merchantDisplay)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = merchantDisplay,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (isOnline) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.financialColors.onlineTagBackground,
                        ) {
                            Text(
                                text = "#Online",
                                style =
                                    MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                color = MaterialTheme.financialColors.onlineTagText,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    }
                    if (!isCounted) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        ) {
                            Text(
                                text = stringResource(id = R.string.row_not_counted),
                                style =
                                    MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                    ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            )
                        }
                    }
                    val parseConf = transaction.parseConfidence
                    if (parseConf != null && parseConf < 0.7f) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier =
                                Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.financialColors.warning),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            horizontalAlignment = Alignment.End,
        ) {
            AmountText(
                amountMinor = transaction.amount,
                type = transaction.type,
                style = MaterialTheme.typography.titleMedium,
            )
            if (relativeTime.isNotEmpty() || transaction.isReviewed) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (relativeTime.isNotEmpty()) {
                        Text(
                            text = relativeTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                        )
                    }
                    if (transaction.isReviewed) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.financialColors.income,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
        }
    }
}
