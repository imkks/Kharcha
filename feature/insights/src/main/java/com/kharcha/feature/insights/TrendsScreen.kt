package com.kharcha.feature.insights

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.R as DesignR
import com.kharcha.core.designsystem.component.IncomeExpenseBarChart
import com.kharcha.core.designsystem.component.SelectAccountSheet
import com.kharcha.core.designsystem.component.StickyActionPill
import com.kharcha.core.designsystem.component.calculateMonthlyComparisonData
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.designsystem.theme.LocalIncomeHidden
import com.kharcha.core.designsystem.theme.LocalIncomeRevealed
import com.kharcha.core.designsystem.theme.LocalIncomeReveal
import com.kharcha.core.model.Account
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import com.kharcha.core.model.isCounted
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class TrendsGrouping(val labelRes: Int) {
    BY_MONTH(R.string.trends_grouping_month),
    BY_CATEGORY(R.string.trends_grouping_category),
    BY_WEEK(R.string.trends_grouping_week),
}

/**
 * Trends Screen (UI Spec Section 7.2).
 * Features:
 * - App bar: Back arrow, "Trends by month ▾" switcher, filter icon.
 * - 6-month bar chart in blue with baseline green dots and selected month highlight.
 * - Month summary bar with selected month and "All accounts ▾" selector.
 * - Spends tile (always visible) & Income tile (strictly eye-toggled hide/reveal).
 * - "Set monthly budget" tile.
 * - Fixed floating "Review [Month]" action pill at bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsScreen(
    transactions: List<Transaction>,
    accounts: List<Account> = emptyList(),
    monthlyBudgetMinor: Long = 0L,
    onBack: () -> Unit = {},
    onSetMonthlyBudgetClick: () -> Unit = {},
    onReviewMonthClick: (YearMonth) -> Unit = {},
    onFilterClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedGrouping by rememberSaveable { mutableStateOf(TrendsGrouping.BY_MONTH) }
    var showGroupingMenu by remember { mutableStateOf(false) }

    var selectedYearMonth by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var showAccountSheet by remember { mutableStateOf(false) }

    // User Rule: the eye icon hide/reveal toggle is strictly for income (spends is always visible)
    val incomeHiddenBySetting = LocalIncomeHidden.current?.value == true && LocalIncomeRevealed.current?.value != true
    var isIncomeHidden by rememberSaveable { mutableStateOf(false) }
    val shouldHideIncome = isIncomeHidden || incomeHiddenBySetting
    val revealIncome = LocalIncomeReveal.current

    val accountFilteredTransactions =
        remember(transactions, selectedAccount) {
            if (selectedAccount != null) {
                transactions.filter { it.accountId == selectedAccount!!.id }
            } else {
                transactions
            }
        }

    val monthTransactions =
        remember(accountFilteredTransactions, selectedYearMonth) {
            accountFilteredTransactions.filter { tx ->
                val ym = YearMonth.from(Instant.ofEpochMilli(tx.dateTime).atZone(ZoneId.systemDefault()))
                ym == selectedYearMonth
            }
        }

    val selectedMonthSpendsMinor =
        remember(monthTransactions) {
            monthTransactions
                .filter { it.type == TransactionType.Expense && it.isCounted }
                .sumOf { it.amount }
        }

    val selectedMonthIncomeMinor =
        remember(monthTransactions) {
            monthTransactions
                .filter { it.type == TransactionType.Income && it.isCounted }
                .sumOf { it.amount }
        }

    val monthLabelFormatter = remember { DateTimeFormatter.ofPattern("MMM''yy", Locale.ENGLISH) }
    val monthLabel = remember(selectedYearMonth) { selectedYearMonth.format(monthLabelFormatter) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showGroupingMenu = true },
                        ) {
                            Text(
                                text = stringResource(id = selectedGrouping.labelRes) + " ▾",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.financialColors.textPrimary,
                            )
                        }
                        DropdownMenu(
                            expanded = showGroupingMenu,
                            onDismissRequest = { showGroupingMenu = false },
                            modifier = Modifier.background(MaterialTheme.financialColors.cardBackground),
                        ) {
                            TrendsGrouping.entries.forEach { grouping ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(id = grouping.labelRes),
                                            color = MaterialTheme.financialColors.textPrimary,
                                            fontWeight = if (selectedGrouping == grouping) FontWeight.Bold else FontWeight.Normal,
                                        )
                                    },
                                    onClick = {
                                        selectedGrouping = grouping
                                        showGroupingMenu = false
                                    },
                                )
                            }
                        }
                    }
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
                    IconButton(onClick = onFilterClick) {
                        Icon(
                            painter = painterResource(id = DesignR.drawable.ic_category_other),
                            contentDescription = stringResource(id = R.string.trends_filter_title),
                            tint = MaterialTheme.financialColors.textPrimary,
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
            StickyActionPill(
                label = stringResource(id = R.string.trends_review_action, monthLabel),
                onClick = { onReviewMonthClick(selectedYearMonth) },
            )
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
            // 1. Chart Section
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
                        val chartData =
                            remember(accountFilteredTransactions) {
                                calculateMonthlyComparisonData(accountFilteredTransactions, monthsCount = 6)
                            }
                        IncomeExpenseBarChart(
                            data = chartData,
                            onMonthSelected = { item -> selectedYearMonth = item.yearMonth },
                        )
                    }
                }
            }

            // 2. Month summary bar: [ Oct'26 ] ... [ All accounts ▾ ]
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.financialColors.raisedRowBackground,
                    border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = monthLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.financialColors.textPrimary,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showAccountSheet = true },
                        ) {
                            val accName = selectedAccount?.let { it.bankName ?: it.name } ?: stringResource(id = R.string.trends_all_accounts)
                            Text(
                                text = accName,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.financialColors.limeAccent,
                                modifier = if (incomeHiddenBySetting) Modifier.clickable { revealIncome?.invoke() } else Modifier,
                            )
                        }
                    }
                }
            }

            // 3. Two Tiles: Spends (always visible) and Income (strictly eye toggled)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Spends Tile (Blue tinted, always visible)
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = Color(0xFF192233),
                            ),
                        border = BorderStroke(1.dp, MaterialTheme.financialColors.chartBlue.copy(alpha = 0.3f)),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(id = R.string.trends_spends_tile),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.financialColors.textSecondary,
                                )
                                Icon(
                                    painter = painterResource(id = DesignR.drawable.ic_trending_down),
                                    contentDescription = null,
                                    tint = MaterialTheme.financialColors.chartBlue,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = MoneyFormatter.formatMinorUnits(selectedMonthSpendsMinor),
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.financialColors.textPrimary,
                            )
                        }
                    }

                    // Income Tile (Green tinted, eye icon toggles strictly for income)
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors =
                            CardDefaults.cardColors(
                                containerColor = Color(0xFF142918),
                            ),
                        border = BorderStroke(1.dp, MaterialTheme.financialColors.limeAccent.copy(alpha = 0.3f)),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = stringResource(id = R.string.trends_income_tile),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.financialColors.textSecondary,
                                )
                                IconButton(
                                    onClick = { if (!incomeHiddenBySetting) isIncomeHidden = !isIncomeHidden },
                                    modifier = Modifier.size(20.dp),
                                ) {
                                    Icon(
                                        painter =
                                            painterResource(
                                                id =
                                                    if (shouldHideIncome) {
                                                        DesignR.drawable.ic_visibility_off
                                                    } else {
                                                        DesignR.drawable.ic_visibility
                                                    },
                                            ),
                                        contentDescription = stringResource(id = R.string.trends_toggle_income),
                                        tint = MaterialTheme.financialColors.limeAccent,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text =
                                    if (shouldHideIncome) {
                                        "₹ ••••••"
                                    } else {
                                        MoneyFormatter.formatMinorUnits(selectedMonthIncomeMinor)
                                    },
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.financialColors.limeAccent,
                            )
                        }
                    }
                }
            }

            // 4. Third Tile: "Set monthly budget"
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.financialColors.cardBackground,
                    border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSetMonthlyBudgetClick() },
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.financialColors.chartBlue.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(id = DesignR.drawable.ic_bar_chart),
                                    contentDescription = null,
                                    tint = MaterialTheme.financialColors.chartBlue,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(id = R.string.trends_set_monthly_budget),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.financialColors.textPrimary,
                                )
                                if (monthlyBudgetMinor > 0L) {
                                    Text(
                                        text = MoneyFormatter.formatMinorUnits(monthlyBudgetMinor),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.financialColors.textSecondary,
                                    )
                                }
                            }
                        }
                        Icon(
                            painter = painterResource(id = DesignR.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.financialColors.textSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(50.dp))
            }
        }
    }

    if (showAccountSheet) {
        SelectAccountSheet(
            accounts = accounts,
            selectedAccountId = selectedAccount?.id,
            onAccountSelected = { acc ->
                selectedAccount = acc
                showAccountSheet = false
            },
            onDismiss = { showAccountSheet = false },
        )
    }
}
