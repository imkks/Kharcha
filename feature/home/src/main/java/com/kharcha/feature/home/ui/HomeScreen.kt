package com.kharcha.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kharcha.feature.home.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToCategory: (String) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    val periodName = if (uiState.periodOffset == 0) "Current Month" else "Offset ${uiState.periodOffset}"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kharcha") },
                actions = {
                    PeriodSelector(
                        periodName = periodName,
                        onPrevious = { viewModel.onPreviousPeriod() },
                        onNext = { viewModel.onNextPeriod() },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            SummaryCard(
                netAmount = uiState.netAmount,
                totalIncome = uiState.totalIncome,
                totalExpense = uiState.totalExpense,
            )

            Text("Top Categories", style = MaterialTheme.typography.titleMedium)
            CategoryDonutChart(categories = uiState.topCategories) { categoryId ->
                onNavigateToCategory(categoryId)
            }

            Text("Income vs Expense", style = MaterialTheme.typography.titleMedium)
            IncomeExpenseBarChart(monthlyTotals = uiState.monthlyTotals)

            RecentTransactions(
                transactions = uiState.recentTransactions,
                onSeeAll = onNavigateToTransactions,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
