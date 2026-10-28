package com.kharcha.feature.transactions.ui

import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.R as DesignR
import com.kharcha.core.designsystem.component.CategoryBadge
import com.kharcha.core.designsystem.component.HalfRingGauge
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import com.kharcha.feature.transactions.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class UpcomingDue(
    val id: Long,
    val name: String,
    val dueDate: String,
    val amountMinor: Long,
)

/**
 * Weekly Summary Screen (UI Spec Section 7.7).
 * Opened from "This week" chip or Sunday summary trigger.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklySummaryScreen(
    transactions: List<Transaction>,
    monthlyBudgetMinor: Long = 0L,
    onBack: () -> Unit,
    onSetBudgetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var selectedRating by rememberSaveable { mutableStateOf<String?>(null) }

    val zoneId = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zoneId) }
    val weekStart = remember(today) { today.minusDays(today.dayOfWeek.value.toLong() - 1) }

    val thisWeekTransactions =
        remember(transactions, weekStart) {
            transactions.filter { tx ->
                val txDate = Instant.ofEpochMilli(tx.dateTime).atZone(zoneId).toLocalDate()
                !txDate.isBefore(weekStart) && !txDate.isAfter(today)
            }
        }

    val weekExpenseMinor =
        remember(thisWeekTransactions) {
            thisWeekTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }
        }

    // Weekly target pacing
    val weeklyBudgetMinor = if (monthlyBudgetMinor > 0L) monthlyBudgetMinor / 4L else 7500_00L
    val dailyPacingMinor = weeklyBudgetMinor / 7L
    val progressFraction =
        remember(weekExpenseMinor, weeklyBudgetMinor) {
            if (weeklyBudgetMinor > 0L) {
                (weekExpenseMinor.toFloat() / weeklyBudgetMinor.toFloat()).coerceIn(0f, 1f)
            } else {
                0.5f
            }
        }

    // Top 3 merchants for the week
    val topMerchants =
        remember(thisWeekTransactions, weekExpenseMinor) {
            thisWeekTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.merchantNormalised ?: it.merchantRaw ?: "Others" }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }
                .take(3)
                .map { (name, amount) ->
                    val percentage =
                        if (weekExpenseMinor > 0L) {
                            ((amount.toDouble() / weekExpenseMinor.toDouble()) * 100).toInt()
                        } else {
                            0
                        }
                    Triple(name, amount, percentage)
                }
        }

    val upcomingDues =
        remember {
            listOf(
                UpcomingDue(1L, "Netflix", "01 Nov", 199_00L),
                UpcomingDue(2L, "Airtel Fiber", "05 Nov", 1179_00L),
            )
        }

    val shareText =
        stringResource(
            id = R.string.weekly_summary_share_text,
            MoneyFormatter.formatMinorUnits(weekExpenseMinor),
            MoneyFormatter.formatMinorUnits(dailyPacingMinor),
        )

    fun shareSummary() {
        val sendIntent =
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
        val shareIntent = Intent.createChooser(sendIntent, "Share Weekly Summary")
        context.startActivity(shareIntent)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.weekly_summary_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.financialColors.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = DesignR.string.navigate_up),
                            tint = MaterialTheme.financialColors.textPrimary,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { shareSummary() }) {
                        Icon(
                            painter = painterResource(id = DesignR.drawable.ic_receipt_photo),
                            contentDescription = "Share",
                            tint = MaterialTheme.financialColors.textPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                    Button(
                        onClick = { shareSummary() },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.financialColors.limeAccent,
                                contentColor = MaterialTheme.financialColors.onPaleLimeAddButton,
                            ),
                        shape = RoundedCornerShape(100.dp),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.weekly_summary_share_button),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Hero Gauge
            item {
                HalfRingGauge(
                    spentAmountFormatted = MoneyFormatter.formatMinorUnits(weekExpenseMinor),
                    dailyPacingFormatted = stringResource(id = R.string.weekly_summary_daily_target, MoneyFormatter.formatMinorUnits(dailyPacingMinor)),
                    progressFraction = progressFraction,
                    onSetBudgetClick = onSetBudgetClick,
                )
            }

            // 2. Top Spends breakdown card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.financialColors.cardBackground,
                        ),
                    border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(id = R.string.weekly_summary_top_spends),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.financialColors.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (topMerchants.isEmpty()) {
                            Text(
                                text = "No spends recorded this week yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.financialColors.textSecondary,
                            )
                        } else {
                            topMerchants.forEachIndexed { index, (merchant, amount, pct) ->
                                Row(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        CategoryBadge(categoryName = merchant, size = 32.dp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = merchant,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.financialColors.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = "$pct% of week",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.financialColors.textSecondary,
                                            )
                                        }
                                    }
                                    Text(
                                        text = MoneyFormatter.formatMinorUnits(amount),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.financialColors.textPrimary,
                                    )
                                }
                                if (index < topMerchants.lastIndex) {
                                    HorizontalDivider(
                                        thickness = 1.dp,
                                        color = MaterialTheme.financialColors.hairlineDivider,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Dues coming up in next 7 days
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.financialColors.cardBackground,
                        ),
                    border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(id = R.string.weekly_summary_dues_coming_up),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.financialColors.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(upcomingDues) { due ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.financialColors.raisedRowBackground,
                                    border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                        Text(
                                            text = due.dueDate,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.financialColors.textSecondary,
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = due.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.financialColors.textPrimary,
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = MoneyFormatter.formatMinorUnits(due.amountMinor),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.financialColors.limeAccent,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Rate your week (😊 😐 😟)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.financialColors.cardBackground,
                        ),
                    border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(id = R.string.weekly_summary_rate_week),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.financialColors.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            listOf(
                                "😊" to "Great",
                                "😐" to "Okay",
                                "😟" to "High",
                            ).forEach { (emoji, label) ->
                                val isSelected = selectedRating == emoji
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color =
                                        if (isSelected) {
                                            MaterialTheme.financialColors.limeAccent.copy(alpha = 0.2f)
                                        } else {
                                            MaterialTheme.financialColors.raisedRowBackground
                                        },
                                    border =
                                        if (isSelected) {
                                            BorderStroke(1.5.dp, MaterialTheme.financialColors.limeAccent)
                                        } else {
                                            BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider)
                                        },
                                    modifier =
                                        Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .clickable { selectedRating = emoji },
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(text = emoji, fontSize = 24.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = MaterialTheme.financialColors.textPrimary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
