package com.kharcha.feature.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kharcha.core.common.DefaultTransactionRepository
import com.kharcha.core.common.TransactionRepository
import com.kharcha.core.model.Direction
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import com.kharcha.engine.parser.GenericFallbackParser
import com.kharcha.engine.parser.NoiseFilter
import com.kharcha.engine.sms.Deduplicator
import com.kharcha.engine.sms.ImportWorker
import com.kharcha.engine.sms.InboxReader
import com.kharcha.engine.sms.SenderFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val currentState: OnboardingState = OnboardingState.WELCOME,
    val isPermissionDenied: Boolean = false,
    val progress: Float = 0.0f,
    val transactionCount: Int = 0,
    val isImportComplete: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel
    @Inject
    constructor(
        private val inboxReader: InboxReader,
        private val senderFilter: SenderFilter,
        private val deduplicator: Deduplicator,
        private val transactionRepository: TransactionRepository,
        @ApplicationContext private val context: Context,
    ) : ViewModel() {
        var ioDispatcher: CoroutineDispatcher = Dispatchers.IO

        constructor(
            inboxReader: InboxReader,
            senderFilter: SenderFilter,
            deduplicator: Deduplicator,
            transactionRepository: TransactionRepository,
            context: Context,
            ioDispatcher: CoroutineDispatcher,
        ) : this(inboxReader, senderFilter, deduplicator, transactionRepository, context) {
            this.ioDispatcher = ioDispatcher
        }

        constructor(
            inboxReader: InboxReader,
            senderFilter: SenderFilter,
            deduplicator: Deduplicator,
            context: Context,
            ioDispatcher: CoroutineDispatcher,
        ) : this(
            inboxReader,
            senderFilter,
            deduplicator,
            DefaultTransactionRepository.getInstance(context),
            context,
            ioDispatcher,
        )

        private val _uiState = MutableStateFlow(OnboardingUiState())
        val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

        private val fallbackParser = GenericFallbackParser()

        fun moveToState(state: OnboardingState) {
            _uiState.update { it.copy(currentState = state) }
        }

        fun onPermissionResult(isGranted: Boolean) {
            if (isGranted) {
                _uiState.update { it.copy(currentState = OnboardingState.SETUP, isPermissionDenied = false) }
            } else {
                _uiState.update { it.copy(isPermissionDenied = true) }
            }
        }

        fun startImport(
            window: String,
            storeRaw: Boolean,
        ) {
            _uiState.update {
                it.copy(
                    currentState = OnboardingState.IMPORT_PROGRESS,
                    progress = 0.0f,
                    transactionCount = 0,
                    isImportComplete = false,
                )
            }

            viewModelScope.launch(ioDispatcher) {
                val now = System.currentTimeMillis()
                val sinceTimestamp =
                    when (window) {
                        "30 days" -> now - 30L * 24 * 60 * 60 * 1000L
                        "90 days" -> now - 90L * 24 * 60 * 60 * 1000L
                        "6 months" -> now - 180L * 24 * 60 * 60 * 1000L
                        else -> 0L
                    }

                val totalCount = inboxReader.getSmsCount(sinceTimestamp)
                if (totalCount <= 0) {
                    _uiState.update { it.copy(progress = 1.0f, isImportComplete = true) }
                    enqueueBackgroundWorker()
                    return@launch
                }

                val batchSize = 100
                var lastProcessedId = 0L
                var processedCount = 0
                var foundCount = 0
                val seenHashes = mutableSetOf<String>()

                while (true) {
                    val batch = inboxReader.readSmsBatch(lastProcessedId, batchSize, sinceTimestamp)
                    if (batch.isEmpty()) break

                    val batchTxns = mutableListOf<Transaction>()

                    for (sms in batch) {
                        lastProcessedId = maxOf(lastProcessedId, sms.id)
                        processedCount++

                        val hash = deduplicator.generateHash(sms.sender, sms.body, sms.receivedAt)
                        if (!seenHashes.add(hash)) {
                            continue
                        }

                        if (!senderFilter.isFinancialSender(sms.sender)) {
                            continue
                        }

                        if (NoiseFilter.filter(sms) != null) {
                            continue
                        }

                        val parsedTxn = fallbackParser.parse(sms)
                        if (parsedTxn != null) {
                            foundCount++
                            val isCredit = parsedTxn.direction == Direction.CREDIT
                            val type = if (isCredit) TransactionType.Income else TransactionType.Expense
                            val rawMerchant = parsedTxn.merchantRaw ?: parsedTxn.upiId
                            val normMerchant =
                                cleanMerchantDisplay(rawMerchant, parsedTxn.bankName, parsedTxn.channel?.name)
                            val category = guessCategory(normMerchant, isCredit)
                            val isHighConf = (parsedTxn.confidence >= 0.8f) && (rawMerchant != null)

                            val txn =
                                Transaction(
                                    id = sms.id,
                                    smsHash = hash,
                                    accountId = 1L,
                                    type = type,
                                    amount = parsedTxn.amountMinor,
                                    currency = parsedTxn.currency,
                                    dateTime = parsedTxn.dateTime ?: sms.receivedAt,
                                    merchantRaw = rawMerchant ?: normMerchant,
                                    merchantNormalised = normMerchant,
                                    categorySource = category,
                                    channel = parsedTxn.channel,
                                    refNumber = parsedTxn.refNumber,
                                    upiId = parsedTxn.upiId,
                                    balanceAfter = parsedTxn.balanceAfterMinor,
                                    parseConfidence = parsedTxn.confidence,
                                    isReviewed = isHighConf && category != "General",
                                )
                            batchTxns.add(txn)
                        }
                    }

                    if (batchTxns.isNotEmpty()) {
                        transactionRepository.insertTransactions(batchTxns)
                    }

                    val currentProgress = (processedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
                    _uiState.update {
                        it.copy(
                            progress = currentProgress,
                            transactionCount = foundCount,
                        )
                    }
                }

                _uiState.update {
                    it.copy(
                        progress = 1.0f,
                        transactionCount = foundCount,
                        isImportComplete = true,
                    )
                }

                enqueueBackgroundWorker()
            }
        }

        @Suppress("TooGenericExceptionCaught")
        private fun enqueueBackgroundWorker() {
            try {
                val workRequest = OneTimeWorkRequestBuilder<ImportWorker>().build()
                WorkManager.getInstance(context).enqueue(workRequest)
            } catch (_: Throwable) {
                // Ignore if WorkManager fails in tests or headless mode
            }
        }

        private fun cleanMerchantDisplay(raw: String?, bank: String?, channel: String?): String {
            if (raw.isNullOrBlank()) {
                return if (!bank.isNullOrBlank()) "$bank ${channel ?: "Txn"}" else "Transaction"
            }
            val withoutHandle = raw.substringBefore("@")
            val cleaned = withoutHandle.replace(Regex("[^a-zA-Z0-9 ]"), " ").trim()
            val words = cleaned.split(Regex("\\s+")).filter { it.isNotBlank() }
            val filtered =
                words.filter { word ->
                    word.uppercase() !in setOf("PVT", "LTD", "LIMITED", "PRIVATE", "INC", "CORP", "VPA", "UPI", "POS")
                }
            val finalWords = if (filtered.isNotEmpty()) filtered else words
            return finalWords.take(4).joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
        }

        private fun guessCategory(merchant: String, isCredit: Boolean): String {
            if (isCredit) return "Income"
            val m = merchant.lowercase()
            return when {
                m.contains("swiggy") || m.contains("zomato") || m.contains("mcdonald") ||
                    m.contains("kfc") || m.contains("domino") || m.contains("pizza") ||
                    m.contains("starbucks") || m.contains("coffee") || m.contains("chai") ||
                    m.contains("burger") || m.contains("subway") || m.contains("restaurant") -> "Food & Dining"
                m.contains("blinkit") || m.contains("zepto") || m.contains("instamart") ||
                    m.contains("bigbasket") || m.contains("dmart") || m.contains("grocery") ||
                    m.contains("supermarket") || m.contains("reliance fresh") -> "Groceries"
                m.contains("amazon") || m.contains("flipkart") || m.contains("myntra") ||
                    m.contains("ajio") || m.contains("meesho") || m.contains("tata cliq") ||
                    m.contains("retail") || m.contains("store") -> "Shopping"
                m.contains("uber") || m.contains("ola") || m.contains("rapido") ||
                    m.contains("metro") || m.contains("irctc") || m.contains("petrol") ||
                    m.contains("fuel") || m.contains("hpcl") || m.contains("bpcl") ||
                    m.contains("ioc") || m.contains("railway") -> "Transport"
                m.contains("airtel") || m.contains("jio") || m.contains("vi ") ||
                    m.contains("bescom") || m.contains("electricity") || m.contains("water") ||
                    m.contains("gas") || m.contains("broadband") || m.contains("bill") -> "Bills & Utilities"
                m.contains("netflix") || m.contains("spotify") || m.contains("prime") ||
                    m.contains("hotstar") || m.contains("bookmyshow") || m.contains("cinema") ||
                    m.contains("pvr") || m.contains("inox") -> "Entertainment"
                m.contains("apollo") || m.contains("pharmacy") || m.contains("1mg") ||
                    m.contains("pharmeasy") || m.contains("hospital") || m.contains("clinic") ||
                    m.contains("medplus") -> "Health"
                m.contains("zerodha") || m.contains("groww") || m.contains("mutual") ||
                    m.contains("sip") || m.contains("indmoney") || m.contains("upstox") -> "Investments"
                else -> "General"
            }
        }
    }
