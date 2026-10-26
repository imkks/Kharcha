package com.kharcha.feature.home.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kharcha.core.common.DateTimeFormatterHelper
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.designsystem.R as DesignR
import com.kharcha.core.designsystem.component.AccountCardModel
import com.kharcha.core.designsystem.component.AccountCarousel
import com.kharcha.core.designsystem.component.CategoryBadge
import com.kharcha.core.designsystem.component.CreditCardFace
import com.kharcha.core.designsystem.component.EmptyStateCard
import com.kharcha.core.designsystem.component.PromoCard
import com.kharcha.core.designsystem.component.SpendRing
import com.kharcha.core.designsystem.component.TagChip
import com.kharcha.core.designsystem.theme.LocalPrivacyMode
import com.kharcha.core.designsystem.theme.financialColors
import com.kharcha.core.model.Account
import com.kharcha.core.model.AccountType
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import com.kharcha.feature.home.R
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

data class DueReminderItem(
    val id: Long,
    val frequency: String,
    val name: String,
    val amountMinor: Long,
)

/**
 * Axio-style Home Dashboard Screen (UI Specification Section 3.1 & 7.1).
 * Features:
 * - Avatar + "Hi Krishna" header with search and privacy toggle.
 * - Centered "Spent in [Month]" caption.
 * - 190dp circular progress ring with center "↗" amount, tappable to All Spends.
 * - Income and Set monthly budget split columns.
 * - Trends & Categories shortcut pills.
 * - Recent transactions card with pale-lime "+ Add" pill button.
 * - Continuous scroll feed:
 *   1. Dues & Reminders card with mini-cards and reminder toggle.
 *   2. Categories card with Unknown categorization and budget links.
 *   3. Accounts two-tone snapping carousel.
 *   4. Deposits promo card.
 *   5. Credit cards realistic card faces carousel.
 *   6. Splits card with crossed bills empty state.
 *   7. Tags wrap cloud with count badges.
 *   8. "Made with ❤ in India" footer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    transactions: List<Transaction> = emptyList(),
    accounts: List<Account> = emptyList(),
    monthlyBudgetMinor: Long = 0L,
    userName: String? = null,
    onSeeAllClick: () -> Unit = {},
    onReviewClick: () -> Unit = {},
    onTransactionClick: (Transaction) -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
    onPrivacyToggle: () -> Unit = {},
    onSetBudgetClick: () -> Unit = {},
    onAddTransactionClick: () -> Unit = {},
    onTrendsClick: () -> Unit = {},
    onCategoriesClick: () -> Unit = {},
    onWeeklySummaryClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onAccountClick: (Account) -> Unit = {},
    onTagClick: (String) -> Unit = {},
    onSplitClick: () -> Unit = {},
    onDepositClick: () -> Unit = {},
    onDuesClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val currentYearMonth = remember { YearMonth.now() }
    val monthDisplayName =
        remember(currentYearMonth) {
            currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        }

    val currentMonthTransactions =
        remember(transactions, currentYearMonth) {
            transactions.filter { tx ->
                val txYearMonth =
                    YearMonth.from(
                        Instant.ofEpochMilli(tx.dateTime).atZone(ZoneId.systemDefault()),
                    )
                txYearMonth == currentYearMonth
            }
        }

    val spentThisMonth =
        remember(currentMonthTransactions) {
            currentMonthTransactions
                .filter { it.type == TransactionType.Expense }
                .sumOf { it.amount }
        }

    val incomeThisMonth =
        remember(currentMonthTransactions) {
            currentMonthTransactions
                .filter { it.type == TransactionType.Income }
                .sumOf { it.amount }
        }

    val computedReviewCount =
        remember(transactions) {
            transactions.count { !it.isReviewed }
        }

    val recentTransactionsList =
        remember(transactions) {
            transactions.take(8)
        }

    val isPrivacy = LocalPrivacyMode.current
    val greetingUser = userName ?: stringResource(id = R.string.home_default_user)

    // Data models for Section 7.1 continuous scrolling feed
    val duesList =
        remember {
            listOf(
                DueReminderItem(1L, "Repeats Monthly · 01 Nov 26", "Netflix", 19900L),
                DueReminderItem(2L, "Repeats Monthly · 05 Nov 26", "Airtel Fiber", 117900L),
                DueReminderItem(3L, "Repeats Yearly · 15 Dec 26", "Term Insurance", 850000L),
            )
        }
    val reminderToggles = remember { mutableStateMapOf<Long, Boolean>() }
    val totalDuesMinor = remember(duesList) { duesList.sumOf { it.amountMinor } }

    val unknownSpend =
        remember(currentMonthTransactions) {
            currentMonthTransactions
                .filter { it.type == TransactionType.Expense && (it.categorySource == "Unknown" || it.categorySource == "Other" || !it.isReviewed) }
                .sumOf { it.amount }
        }

    val topSpendCategories =
        remember(currentMonthTransactions) {
            currentMonthTransactions
                .filter { it.type == TransactionType.Expense && it.categorySource != "Unknown" && it.categorySource != "Other" }
                .groupBy { it.categorySource ?: "Others" }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
                .toList()
                .sortedByDescending { it.second }
                .take(3)
        }

    val accountModels =
        remember(accounts, isPrivacy) {
            if (accounts.isNotEmpty()) {
                accounts.map { acc ->
                    AccountCardModel(
                        id = acc.id,
                        bankName = acc.bankName ?: acc.name,
                        accountNumberMasked = acc.last4?.let { "•••• $it" } ?: acc.name,
                        balanceFormatted =
                            if (isPrivacy) {
                                "₹ ••••••"
                            } else {
                                acc.currentBalance?.let { MoneyFormatter.formatMinorUnits(it) } ?: "₹0.00"
                            },
                        isWallet = acc.type == AccountType.Wallet,
                        lastTxnInfo = if (acc.type == AccountType.Wallet) "Active wallet" else null,
                    )
                }
            } else {
                listOf(
                    AccountCardModel(1L, "HDFC Bank", "•••• 1999", if (isPrivacy) "₹ ••••••" else "₹24,850.00"),
                    AccountCardModel(2L, "SBI", "•••• 4208", if (isPrivacy) "₹ ••••••" else "₹12,400.00"),
                    AccountCardModel(
                        3L,
                        "Amazon Pay",
                        "Digital Wallet",
                        if (isPrivacy) "₹ ••••••" else "₹850.00",
                        isWallet = true,
                        lastTxnInfo = "24 Oct · ₹120.00",
                    ),
                )
            }
        }

    val creditCards =
        remember(isPrivacy) {
            listOf(
                Triple("HDFC Bank", "XXXX XXXX XXXX 1999", if (isPrivacy) "₹ ••••••" else "₹18,450.00"),
                Triple("ICICI Bank", "XXXX XXXX XXXX 4007", if (isPrivacy) "₹ ••••••" else "₹13,746.00"),
            )
        }

    val tagsCloud =
        remember(transactions) {
            val counts = mutableMapOf<String, Int>()
            for (t in transactions) {
                val note = t.note ?: continue
                val matches = Regex("""#([A-Za-z0-9_]+)""").findAll(note)
                for (m in matches) {
                    val tag = m.groupValues[1]
                    counts[tag] = (counts[tag] ?: 0) + 1
                }
            }
            val defaults = listOf("Online", "Rent", "School", "Friends", "Family", "Office")
            for (d in defaults) {
                if (!counts.containsKey(d)) {
                    counts[d] = when (d) {
                        "Online" -> 22
                        "Rent" -> 1
                        "School" -> 4
                        "Friends" -> 12
                        "Family" -> 5
                        else -> 2
                    }
                }
            }
            counts.toList().sortedByDescending { it.second }
        }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.financialColors.cardBackground.copy(alpha = 0.35f),
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Header: Avatar + "Hi Krishna" + Actions (Search & Privacy)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Circular Green Avatar
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.financialColors.limeAccent,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = greetingUser.take(1).uppercase(Locale.ROOT),
                                color = MaterialTheme.financialColors.onPaleLimeAddButton,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(id = R.string.home_greeting, greetingUser),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.financialColors.textPrimary,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onPrivacyToggle) {
                        Icon(
                            painter =
                                painterResource(
                                    id =
                                        if (isPrivacy) {
                                            DesignR.drawable.ic_visibility_off
                                        } else {
                                            DesignR.drawable.ic_visibility
                                        },
                                ),
                            contentDescription =
                                stringResource(
                                    id =
                                        if (isPrivacy) {
                                            DesignR.string.action_show_amounts
                                        } else {
                                            DesignR.string.action_hide_amounts
                                        },
                                ),
                            tint = MaterialTheme.financialColors.textSecondary,
                        )
                    }
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(id = R.string.home_action_search),
                            tint = MaterialTheme.financialColors.textSecondary,
                        )
                    }
                }
            }

            // 2. Month Summary: "Spent in October" centered caption
            item {
                Text(
                    text = stringResource(id = R.string.home_spent_in_month, monthDisplayName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.financialColors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // 3. Hero Spend Ring (190dp, tappable to All Spends)
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    SpendRing(
                        spentMinor = spentThisMonth,
                        monthlyBudgetMinor = monthlyBudgetMinor,
                        onClick = onSeeAllClick,
                    )
                }
            }

            // 4. Sub-Ring Columns: Income | Set monthly budget
            item {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Left Column: Income
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(id = R.string.home_income_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.financialColors.textSecondary,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text =
                                if (isPrivacy) {
                                    "₹ ••••••"
                                } else {
                                    MoneyFormatter.formatMinorUnits(incomeThisMonth)
                                },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.financialColors.textPrimary,
                        )
                    }

                    VerticalDivider(
                        modifier = Modifier.height(34.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.financialColors.hairlineDivider,
                    )

                    // Right Column: Set monthly budget
                    Row(
                        modifier =
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSetBudgetClick() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Surface(
                            modifier = Modifier.size(24.dp),
                            shape = CircleShape,
                            color = MaterialTheme.financialColors.chartBlue,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = DesignR.drawable.ic_bar_chart),
                                    contentDescription = null,
                                    tint = MaterialTheme.financialColors.textPrimary,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            if (monthlyBudgetMinor > 0L) {
                                val remaining = (monthlyBudgetMinor - spentThisMonth).coerceAtLeast(0L)
                                Text(
                                    text =
                                        stringResource(
                                            id = R.string.home_budget_remaining,
                                            if (isPrivacy) "₹ ••••••" else MoneyFormatter.formatMinorUnits(remaining),
                                        ),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.financialColors.textPrimary,
                                )
                            } else {
                                Text(
                                    text = stringResource(id = R.string.home_set_monthly_budget),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.financialColors.textPrimary,
                                    lineHeight = 15.sp,
                                )
                            }
                        }
                    }
                }
            }

            // 5. Shortcut Chips: [ Trends ] and [ Categories ]
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Trends Chip
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = MaterialTheme.financialColors.cardBackground,
                        border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .clickable { onTrendsClick() },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(id = DesignR.drawable.ic_trending_up),
                                contentDescription = null,
                                tint = MaterialTheme.financialColors.limeAccent,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.home_shortcut_trends),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.financialColors.limeAccent,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Categories Chip
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = MaterialTheme.financialColors.cardBackground,
                        border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .clickable { onCategoriesClick() },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(id = DesignR.drawable.ic_category_shopping),
                                contentDescription = null,
                                tint = MaterialTheme.financialColors.limeAccent,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.home_shortcut_categories),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.financialColors.limeAccent,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Weekly Summary Chip
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = MaterialTheme.financialColors.cardBackground,
                        border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .clickable { onWeeklySummaryClick() },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = painterResource(id = DesignR.drawable.ic_calendar),
                                contentDescription = null,
                                tint = MaterialTheme.financialColors.limeAccent,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.home_shortcut_weekly_summary),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.financialColors.limeAccent,
                            )
                        }
                    }
                }
            }

            // 6. Review Card (surfaced if any items need review)
            if (computedReviewCount > 0) {
                item {
                    ReviewCard(
                        count = computedReviewCount,
                        onReviewClick = onReviewClick,
                    )
                }
            }

            // 7. Recent Transactions Card
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
                    ) {
                        // Header: Title on left, pale-lime "+ Add" pill button on right
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(id = R.string.home_recent_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.financialColors.textPrimary,
                            )
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = MaterialTheme.financialColors.paleLimeAddButton,
                                modifier =
                                    Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .clickable { onAddTransactionClick() },
                            ) {
                                Text(
                                    text = stringResource(id = R.string.home_recent_add_button),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.financialColors.onPaleLimeAddButton,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (recentTransactionsList.isEmpty()) {
                            EmptyHomeState()
                        } else {
                            recentTransactionsList.forEachIndexed { index, transaction ->
                                HomeTransactionRow(
                                    transaction = transaction,
                                    onClick = { onTransactionClick(transaction) },
                                )
                                if (index < recentTransactionsList.lastIndex) {
                                    HorizontalDivider(
                                        thickness = 1.dp,
                                        color = MaterialTheme.financialColors.hairlineDivider,
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 8. Dues & Reminders Card
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(id = R.string.home_dues_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.financialColors.textPrimary,
                            )
                            Box(
                                modifier =
                                    Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.financialColors.paleLimeAddButton)
                                        .clickable { onDuesClick() },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "+",
                                    color = MaterialTheme.financialColors.onPaleLimeAddButton,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Horizontally scrolling mini-cards
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(duesList, key = { it.id }) { due ->
                                val isReminderSet = reminderToggles[due.id] ?: false
                                val dueAmount =
                                    if (isPrivacy) {
                                        "₹ ••••••"
                                    } else {
                                        MoneyFormatter.formatMinorUnits(due.amountMinor)
                                    }

                                Card(
                                    modifier = Modifier.width(200.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors =
                                        CardDefaults.cardColors(
                                            containerColor = MaterialTheme.financialColors.raisedRowBackground,
                                        ),
                                    border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = due.frequency,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.financialColors.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = due.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.financialColors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = dueAmount,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.financialColors.textPrimary,
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(
                                                text = stringResource(id = R.string.home_dues_set_reminder),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.financialColors.textSecondary,
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Surface(
                                                    shape = RoundedCornerShape(100.dp),
                                                    color =
                                                        if (!isReminderSet) {
                                                            MaterialTheme.financialColors.cardBackground
                                                        } else {
                                                            Color.Transparent
                                                        },
                                                    border =
                                                        if (!isReminderSet) {
                                                            BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider)
                                                        } else {
                                                            null
                                                        },
                                                    modifier =
                                                        Modifier
                                                            .clip(RoundedCornerShape(100.dp))
                                                            .clickable { reminderToggles[due.id] = false },
                                                ) {
                                                    Text(
                                                        text = stringResource(id = R.string.home_dues_no),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color =
                                                            if (!isReminderSet) {
                                                                MaterialTheme.financialColors.textPrimary
                                                            } else {
                                                                MaterialTheme.financialColors.textSecondary
                                                            },
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    )
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(100.dp),
                                                    color =
                                                        if (isReminderSet) {
                                                            MaterialTheme.financialColors.limeAccent
                                                        } else {
                                                            Color.Transparent
                                                        },
                                                    border =
                                                        if (!isReminderSet) {
                                                            BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider)
                                                        } else {
                                                            null
                                                        },
                                                    modifier =
                                                        Modifier
                                                            .clip(RoundedCornerShape(100.dp))
                                                            .clickable { reminderToggles[due.id] = true },
                                                ) {
                                                    Text(
                                                        text = stringResource(id = R.string.home_dues_yes),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = if (isReminderSet) FontWeight.Bold else FontWeight.Normal,
                                                        color =
                                                            if (isReminderSet) {
                                                                Color.Black
                                                            } else {
                                                                MaterialTheme.financialColors.textSecondary
                                                            },
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.financialColors.hairlineDivider,
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onDuesClick() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val totalDuesFormatted =
                                if (isPrivacy) {
                                    "₹ ••••••"
                                } else {
                                    MoneyFormatter.formatMinorUnits(totalDuesMinor)
                                }
                            Text(
                                text = stringResource(id = R.string.home_dues_total, totalDuesFormatted),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.financialColors.textPrimary,
                            )
                            Icon(
                                painter = painterResource(id = DesignR.drawable.ic_chevron_right),
                                contentDescription = null,
                                tint = MaterialTheme.financialColors.textSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 9. Categories Card
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
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onCategoriesClick() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(id = R.string.home_categories_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.financialColors.textPrimary,
                            )
                            Icon(
                                painter = painterResource(id = DesignR.drawable.ic_chevron_right),
                                contentDescription = null,
                                tint = MaterialTheme.financialColors.textSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Unknown categorization row if any
                        if (unknownSpend > 0L) {
                            val unknownFormatted =
                                if (isPrivacy) "₹ ••••••" else MoneyFormatter.formatMinorUnits(unknownSpend)
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CategoryBadge(categoryName = "Unknown", size = 36.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Unknown",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.financialColors.textPrimary,
                                    )
                                    Text(
                                        text = unknownFormatted,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.financialColors.textSecondary,
                                    )
                                }
                                Text(
                                    text = stringResource(id = R.string.home_categories_categorise),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.financialColors.limeAccent,
                                    modifier = Modifier.clickable { onReviewClick() },
                                )
                            }
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.financialColors.hairlineDivider,
                            )
                        }

                        // Top categories
                        topSpendCategories.forEachIndexed { index, (catName, amount) ->
                            val amountFormatted =
                                if (isPrivacy) "₹ ••••••" else MoneyFormatter.formatMinorUnits(amount)
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable { onCategoryClick(catName) }
                                        .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CategoryBadge(categoryName = catName, size = 36.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = catName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.financialColors.textPrimary,
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(id = R.string.home_categories_set_budget),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.financialColors.limeAccent,
                                        modifier = Modifier.clickable { onSetBudgetClick() },
                                    )
                                }
                                Text(
                                    text = amountFormatted,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.financialColors.textPrimary,
                                )
                            }
                            if (index < topSpendCategories.lastIndex) {
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = MaterialTheme.financialColors.hairlineDivider,
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 10. Accounts Carousel
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(id = R.string.home_accounts_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.financialColors.textPrimary,
                        )
                        Icon(
                            painter = painterResource(id = DesignR.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.financialColors.textSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    AccountCarousel(
                        accounts = accountModels,
                        onAccountClick = { model ->
                            accounts.firstOrNull { it.id == model.id }?.let(onAccountClick)
                        },
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 11. Deposits Promo Card
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(id = R.string.home_deposits_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.financialColors.textPrimary,
                        )
                        Icon(
                            painter = painterResource(id = DesignR.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.financialColors.textSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    PromoCard(onActionClick = onDepositClick)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 12. Credit Cards Carousel
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(id = R.string.home_credit_cards_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.financialColors.textPrimary,
                        )
                        Icon(
                            painter = painterResource(id = DesignR.drawable.ic_chevron_right),
                            contentDescription = null,
                            tint = MaterialTheme.financialColors.textSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(creditCards) { card ->
                            CreditCardFace(
                                bankName = card.first,
                                cardNumberMasked = card.second,
                                outstandingFormatted = card.third,
                                billingCycle = "17 Oct - 16 Nov",
                                dueInfo = "09:27 AM • 2, Apr",
                                creditLimitFormatted = "₹1,79,000",
                                onRefreshClick = {},
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 13. Splits Empty State Card
            item {
                EmptyStateCard(
                    sectionTitle = stringResource(id = R.string.home_splits_title),
                    title = "Create group & split expenses",
                    subtitle = "Split your bills for room rent, hotel, outings, movies.",
                    actionLabel = "Create group",
                    onActionClick = onSplitClick,
                    onPlusClick = onSplitClick,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 14. Tags Cloud Card
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
                            text = stringResource(id = R.string.home_tags_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.financialColors.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            tagsCloud.forEach { (tag, count) ->
                                TagChip(
                                    tag = tag,
                                    count = count,
                                    onClick = { onTagClick(tag) },
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 15. Footer
            item {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(id = R.string.home_footer_made_in_india),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Individual row inside the Recent Transactions card.
 * Layout:
 * - Left: Category icon in rounded square.
 * - Middle: Merchant name (single line, ellipsis).
 * - Right: Amount with "↗" / "↙", date underneath ("Today, 9:46 pm" or "24 Oct").
 */
@Composable
fun HomeTransactionRow(
    transaction: Transaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPrivacy = LocalPrivacyMode.current
    val isDebit = transaction.type == TransactionType.Expense
    val arrowGlyph = if (isDebit) "↗" else "↙"
    val amountFormatted =
        if (isPrivacy) {
            "₹ ••••••"
        } else {
            MoneyFormatter.formatMinorUnits(transaction.amount)
        }

    val relativeDate =
        remember(transaction.dateTime) {
            DateTimeFormatterHelper.formatRelativeTime(transaction.dateTime)
        }

    val merchantName =
        transaction.merchantNormalised
            ?: transaction.merchantRaw
            ?: transaction.channel?.name
            ?: stringResource(id = DesignR.string.unknown_merchant)

    val category = transaction.categorySource ?: "Others"

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(
            categoryName = category,
            size = 38.dp,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = merchantName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.financialColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = category,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.financialColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$arrowGlyph $amountFormatted",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color =
                        if (isDebit) {
                            MaterialTheme.financialColors.textPrimary
                        } else {
                            MaterialTheme.financialColors.limeAccent
                        },
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = relativeDate,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.financialColors.textSecondary,
            )
        }
    }
}

@Composable
fun ReviewCard(
    count: Int,
    onReviewClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.financialColors.cardBackground,
            ),
        border = BorderStroke(1.dp, MaterialTheme.financialColors.hairlineDivider),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.financialColors.limeAccent,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(id = R.string.home_review_title, count),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.financialColors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = MaterialTheme.financialColors.cardBackground,
                border = BorderStroke(1.dp, MaterialTheme.financialColors.limeAccent),
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .clickable { onReviewClick() },
            ) {
                Text(
                    text = stringResource(id = R.string.home_review_button),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.financialColors.limeAccent,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
fun EmptyHomeState(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(id = DesignR.drawable.ic_category_other),
            contentDescription = null,
            tint = MaterialTheme.financialColors.textSecondary,
            modifier = Modifier.size(40.dp),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(id = R.string.home_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.financialColors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(id = R.string.home_empty_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.financialColors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}
