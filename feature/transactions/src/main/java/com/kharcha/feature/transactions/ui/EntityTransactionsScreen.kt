package com.kharcha.feature.transactions.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.R as DesignR
import com.kharcha.core.designsystem.component.CategoryBadge
import com.kharcha.core.designsystem.theme.CategoryColors
import com.kharcha.core.designsystem.theme.LocalPrivacyMode
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.Channel
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import com.kharcha.core.model.isCounted
import com.kharcha.feature.transactions.R
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class MonthSpendBarData(
    val yearMonth: YearMonth,
    val monthLabel: String,
    val spendMinor: Long,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntityTransactionsScreen(
    title: String,
    isMerchant: Boolean,
    allTransactions: List<Transaction>,
    onBack: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val zoneId = ZoneId.systemDefault()
    val isPrivacy = LocalPrivacyMode.current

    val entityTransactions =
        remember(allTransactions, title, isMerchant) {
            if (isMerchant) {
                allTransactions.filter { txn ->
                    val norm = txn.merchantNormalised
                    val raw = txn.merchantRaw
                    norm.equals(title, ignoreCase = true) ||
                        (norm != null && title.contains(norm, ignoreCase = true)) ||
                        raw?.contains(title, ignoreCase = true) == true ||
                        (raw != null && title.contains(raw, ignoreCase = true))
                }
            } else {
                allTransactions.filter { txn ->
                    txn.categorySource.equals(title, ignoreCase = true)
                }
            }.sortedByDescending { it.dateTime }
        }

    val latestDate =
        remember(entityTransactions) {
            val maxEpoch = entityTransactions.maxOfOrNull { it.dateTime }
            if (maxEpoch != null) {
                Instant.ofEpochMilli(maxEpoch).atZone(zoneId).toLocalDate()
            } else {
                LocalDate.now()
            }
        }

    val monthlyBarData =
        remember(entityTransactions, latestDate) {
            calculateEntityMonthlySpend(entityTransactions, latestDate, monthsCount = 7, zoneId)
        }

    var selectedMonth by rememberSaveable {
        mutableStateOf<YearMonth?>(YearMonth.from(latestDate))
    }

    val filteredTransactions =
        remember(entityTransactions, selectedMonth) {
            if (selectedMonth == null) {
                entityTransactions
            } else {
                entityTransactions.filter { txn ->
                    val date = Instant.ofEpochMilli(txn.dateTime).atZone(zoneId).toLocalDate()
                    YearMonth.from(date) == selectedMonth
                }
            }
        }

    val subtitle =
        if (selectedMonth != null) {
            selectedMonth!!.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))
        } else {
            stringResource(id = R.string.entity_screen_all_time)
        }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF9E9E9E),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = DesignR.string.txn_detail_close),
                            tint = Color.White,
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Top Bar Chart: 7 Months Spending
            item {
                EntityMonthlyBarChart(
                    data = monthlyBarData,
                    selectedMonth = selectedMonth,
                    onSelectMonth = { ym ->
                        selectedMonth = if (selectedMonth == ym) null else ym
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            // 2. Transaction List Container
            item {
                if (filteredTransactions.isEmpty()) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(id = R.string.entity_screen_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text =
                                    stringResource(
                                        id = R.string.entity_screen_empty_subtitle,
                                        title,
                                        subtitle,
                                    ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF9E9E9E),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    Card(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = MaterialTheme.financialColors.cardBackground,
                            ),
                        border =
                            BorderStroke(
                                1.dp,
                                MaterialTheme.financialColors.hairlineDivider,
                            ),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            filteredTransactions.forEachIndexed { index, txn ->
                                EntityTransactionRow(
                                    transaction = txn,
                                    showMerchantAsTitle = !isMerchant,
                                    isPrivacy = isPrivacy,
                                    onClick = { onTransactionClick(txn) },
                                )
                                if (index < filteredTransactions.size - 1) {
                                    HorizontalDivider(
                                        color = MaterialTheme.financialColors.hairlineDivider,
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EntityMonthlyBarChart(
    data: List<MonthSpendBarData>,
    selectedMonth: YearMonth?,
    onSelectMonth: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (data.isEmpty()) return

    val maxSpend = remember(data) { data.maxOfOrNull { it.spendMinor }?.coerceAtLeast(1L) ?: 1L }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        data.forEach { item ->
            val isSelected = selectedMonth == item.yearMonth
            val heightFraction = (item.spendMinor.toFloat() / maxSpend.toFloat()).coerceIn(0.04f, 1f)
            val amountLabel = MoneyFormatter.formatCompactAmount(item.spendMinor)

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) {
                                MaterialTheme.financialColors.cardBackground.copy(alpha = 0.8f)
                            } else {
                                Color.Transparent
                            },
                        )
                        .clickable { onSelectMonth(item.yearMonth) }
                        .padding(vertical = 8.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                // Formatted Amount above the bar
                Text(
                    text = amountLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color =
                        if (isSelected) {
                            Color.White
                        } else {
                            Color(0xFF9E9E9E)
                        },
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Vertical Blue Bar Pill (#4B4FE6 / #7B7EF6)
                Box(
                    modifier =
                        Modifier
                            .width(20.dp)
                            .height(140.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    val barHeight = (140.dp * heightFraction).coerceAtLeast(6.dp)
                    Box(
                        modifier =
                            Modifier
                                .width(20.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) {
                                        MaterialTheme.financialColors.chartBlueHighlight
                                    } else if (item.spendMinor > 0L) {
                                        MaterialTheme.financialColors.chartBlue
                                    } else {
                                        MaterialTheme.financialColors.chartBlue.copy(alpha = 0.2f)
                                    },
                                ),
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Month Label
                Text(
                    text = item.monthLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color =
                        if (isSelected) {
                            Color.White
                        } else {
                            Color(0xFF9E9E9E)
                        },
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun EntityTransactionRow(
    transaction: Transaction,
    showMerchantAsTitle: Boolean,
    isPrivacy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val categoryName = transaction.categorySource ?: "Other"
    val merchantDisplay =
        transaction.merchantNormalised
            ?: transaction.merchantRaw
            ?: stringResource(id = R.string.detail_original_sms)

    val title = if (showMerchantAsTitle) merchantDisplay else categoryName
    val zoneId = ZoneId.systemDefault()
    val dateFormatted =
        remember(transaction.dateTime) {
            val date = Instant.ofEpochMilli(transaction.dateTime).atZone(zoneId).toLocalDate()
            date.format(DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault()))
        }

    val note = transaction.note
    val tagText =
        remember(note, transaction.channel) {
            when {
                note != null && note.contains("#") -> {
                    note.split("\\s+".toRegex()).firstOrNull { it.startsWith("#") } ?: "#Online"
                }
                transaction.channel in listOf(Channel.UPI, Channel.NEFT, Channel.IMPS, Channel.CARD) -> "#Online"
                else -> "#Offline"
            }
        }

    val amountFormatted =
        if (isPrivacy) "₹ ••••••" else MoneyFormatter.formatMinorUnits(transaction.amount)

    val isCounted = transaction.isCounted

    val rowModifier =
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (!isCounted) {
                    Modifier.alpha(0.72f)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Icon Badge
        CategoryBadge(categoryName = categoryName, size = 40.dp)

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Tag
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.financialColors.onlineTagBackground)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = tagText,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.financialColors.onlineTagText,
                    )
                }
                if (!isCounted) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.row_not_counted),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Amount & Date
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = amountFormatted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(id = DesignR.drawable.ic_arrow_outward),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dateFormatted,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9E9E9E),
            )
        }
    }
}

fun calculateEntityMonthlySpend(
    transactions: List<Transaction>,
    referenceDate: LocalDate,
    monthsCount: Int = 7,
    zoneId: ZoneId = ZoneId.systemDefault(),
): List<MonthSpendBarData> {
    if (monthsCount <= 0) return emptyList()

    val currentYm = YearMonth.from(referenceDate)
    val startYm = currentYm.minusMonths((monthsCount - 1).toLong())
    val formatter = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())

    val results = mutableListOf<MonthSpendBarData>()
    var cursor = startYm
    while (!cursor.isAfter(currentYm)) {
        val monthStart = cursor.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val monthEnd =
            cursor
                .atEndOfMonth()
                .atTime(23, 59, 59, 999_000_000)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()

        val spend =
            transactions
                .filter { it.dateTime in monthStart..monthEnd && it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

        results.add(
            MonthSpendBarData(
                yearMonth = cursor,
                monthLabel = cursor.format(formatter),
                spendMinor = spend,
            ),
        )
        cursor = cursor.plusMonths(1)
    }
    return results
}
