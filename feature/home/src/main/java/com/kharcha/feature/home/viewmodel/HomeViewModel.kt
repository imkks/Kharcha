package com.kharcha.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import com.kharcha.core.model.Category
import com.kharcha.core.model.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    constructor() : ViewModel() {
        private val _uiState = MutableStateFlow(HomeUiState())
        val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

        init {
            // Stub implementation, will be wired with repository
        }
    }
