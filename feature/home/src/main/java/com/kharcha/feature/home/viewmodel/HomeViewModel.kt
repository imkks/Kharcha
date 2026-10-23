package com.kharcha.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kharcha.core.common.DateUtils
import com.kharcha.core.database.dao.TransactionDao
import com.kharcha.core.model.Category
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = false,
    val totalIncome: Long = 0,
    val totalExpense: Long = 0,
    val netAmount: Long = 0,
    val topCategories: List<CategoryTotal> = emptyList(),
    val monthlyTotals: List<MonthlyTotal> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val periodStart: Long = 0,
    val periodEnd: Long = 0,
    val periodOffset: Int = 0,
)

data class CategoryTotal(
    val category: Category?,
    val totalAmount: Long,
)

data class MonthlyTotal(
    val monthYear: String,
    val income: Long,
    val expense: Long,
)

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val transactionDao: TransactionDao,
    ) : ViewModel() {
        private val periodOffset = MutableStateFlow(0)

        // Can be observed from datastore later
        private val customMonthStart = MutableStateFlow(1)

        private val periodBounds =
            combine(periodOffset, customMonthStart) { offset, startDay ->
                val start = DateUtils.getStartOfMonth(offset, startDay)
                val end = DateUtils.getEndOfMonth(offset, startDay)
                Pair(start, end)
            }

        private val totalIncomeFlow =
            periodBounds.flatMapLatest { bounds ->
                transactionDao.getTotalForPeriod(TransactionType.Income, bounds.first, bounds.second)
            }

        private val totalExpenseFlow =
            periodBounds.flatMapLatest { bounds ->
                transactionDao.getTotalForPeriod(TransactionType.Expense, bounds.first, bounds.second)
            }

        private val topCategoriesFlow =
            periodBounds.flatMapLatest { bounds ->
                transactionDao.getExpensesGroupedByCategory(bounds.first, bounds.second).map { dbTotals ->
                    dbTotals.take(5).map { dbTotal ->
                        CategoryTotal(
                            category =
                                Category(
                                    id = dbTotal.categoryId ?: "unknown",
                                    name = "Unknown",
                                    parentId = null,
                                    icon = "",
                                    type = TransactionType.Expense,
                                ),
                            totalAmount = dbTotal.totalAmount,
                        )
                    }
                }
            }

        private val monthlyTotalsFlow =
            periodBounds.flatMapLatest { bounds ->
                // Actually, we want a fixed window for the bar chart, e.g. last 6 months regardless of period offset.
                // Or based on the end bound. Let's do last 6 months based on current time.
                val sixMonthsAgo = DateUtils.getStartOfMonth(-5, 1)
                val now = System.currentTimeMillis()
                transactionDao.getIncomeExpenseGroupedByMonth(sixMonthsAgo, now).map { dbTotals ->
                    dbTotals.map { MonthlyTotal(it.monthYear, it.totalIncome, it.totalExpense) }
                }
            }

        private val recentTransactionsFlow =
            periodBounds.flatMapLatest { bounds ->
                transactionDao.getRecentTransactions(bounds.first, bounds.second).map { entities ->
                    entities.map {
                        Transaction(
                            id = 0L,
                            accountId = 0L,
                            type = it.type,
                            amount = it.amountMinor,
                            currency = it.currency,
                            dateTime = it.timestamp,
                            merchantRaw = it.merchantName,
                        )
                    }
                }
            }

        val uiState: StateFlow<HomeUiState> =
            combine(
                periodBounds,
                totalIncomeFlow,
                totalExpenseFlow,
                topCategoriesFlow,
                monthlyTotalsFlow,
                recentTransactionsFlow,
            ) { args: Array<Any?> ->
                @Suppress("UNCHECKED_CAST")
                val bounds = args[0] as Pair<Long, Long>
                val income = args[1] as Long?
                val expense = args[2] as Long?

                @Suppress("UNCHECKED_CAST")
                val categories = args[3] as List<CategoryTotal>

                @Suppress("UNCHECKED_CAST")
                val monthly = args[4] as List<MonthlyTotal>

                @Suppress("UNCHECKED_CAST")
                val recent = args[5] as List<Transaction>
                val inc = income ?: 0L
                val exp = expense ?: 0L
                HomeUiState(
                    totalIncome = inc,
                    totalExpense = exp,
                    netAmount = inc - exp,
                    topCategories = categories,
                    monthlyTotals = monthly,
                    recentTransactions = recent,
                    periodStart = bounds.first,
                    periodEnd = bounds.second,
                    periodOffset = periodOffset.value,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = HomeUiState(isLoading = true),
            )

        fun onPreviousPeriod() {
            periodOffset.update { it - 1 }
        }

        fun onNextPeriod() {
            periodOffset.update { it + 1 }
        }
    }
