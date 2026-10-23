package com.kharcha.feature.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kharcha.core.database.repository.RuleRepository
import com.kharcha.core.database.repository.TransactionRepository
import com.kharcha.core.model.CategorySuggestion
import com.kharcha.core.model.Transaction
import com.kharcha.engine.categorizer.CategorizationLearner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewItemUiState(
    val transaction: Transaction,
    val topSuggestions: List<CategorySuggestion>,
)

data class ReviewQueueUiState(
    val items: List<ReviewItemUiState> = emptyList(),
    val remainingCount: Int = 0,
)

@HiltViewModel
class ReviewQueueViewModel
    @Inject
    constructor(
        private val transactionRepository: TransactionRepository,
        private val categorizationLearner: CategorizationLearner,
        private val ruleRepository: RuleRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ReviewQueueUiState())
        val uiState: StateFlow<ReviewQueueUiState> = _uiState.asStateFlow()

        private val skippedTransactionIds = mutableSetOf<String>()

        init {
            viewModelScope.launch {
                transactionRepository.getUnreviewedTransactions().collect { transactions ->
                    val reviewItems =
                        transactions
                            .filterNot { skippedTransactionIds.contains(it.id) }
                            .map { tx ->
                                ReviewItemUiState(
                                    transaction = tx,
                                    topSuggestions = emptyList(),
                                )
                            }
                    _uiState.value =
                        ReviewQueueUiState(
                            items = reviewItems,
                            remainingCount = reviewItems.size,
                        )
                }
            }
        }

        fun acceptCategory(
            transactionId: String,
            categoryId: String,
            applyToPast: Boolean,
        ) {
            val item = _uiState.value.items.find { it.transaction.id == transactionId } ?: return

            viewModelScope.launch {
                transactionRepository.updateTransactionCategory(transactionId, categoryId)

                if (applyToPast) {
                    categorizationLearner.applyCorrection(
                        transactionId = transactionId,
                        merchant = item.transaction.merchant,
                        categoryId = categoryId,
                        applyToPast = applyToPast,
                    )?.let { rule ->
                        ruleRepository.insertRule(rule)
                        transactionRepository.updateTransactionsByMerchant(rule.merchantPattern, rule.targetCategoryId)
                    }
                }
            }
        }

        fun markIgnored(transactionId: String) {
            viewModelScope.launch {
                transactionRepository.markTransactionAsIgnored(transactionId)
            }
        }

        fun skip(transactionId: String) {
            skippedTransactionIds.add(transactionId)
            _uiState.update { currentState ->
                val updatedItems = currentState.items.filterNot { it.transaction.id == transactionId }
                currentState.copy(
                    items = updatedItems,
                    remainingCount = updatedItems.size,
                )
            }
        }
    }
