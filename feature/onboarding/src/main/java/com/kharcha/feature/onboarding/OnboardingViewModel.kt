package com.kharcha.feature.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.kharcha.core.common.AccountRepository
import com.kharcha.core.common.DefaultAccountRepository
import com.kharcha.core.common.DefaultTransactionRepository
import com.kharcha.core.common.DefaultUserPreferences
import com.kharcha.core.common.TransactionClassifierHelper
import com.kharcha.core.common.TransactionRepository
import com.kharcha.core.model.Channel
import com.kharcha.core.model.Direction
import com.kharcha.core.model.ParseResult
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import com.kharcha.engine.parser.NoiseFilter
import com.kharcha.engine.parser.TemplateLoader
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
        var accountRepository: AccountRepository? = null

        constructor(
            inboxReader: InboxReader,
            senderFilter: SenderFilter,
            deduplicator: Deduplicator,
            transactionRepository: TransactionRepository,
            context: Context,
            ioDispatcher: CoroutineDispatcher,
            accountRepository: AccountRepository? = null,
        ) : this(inboxReader, senderFilter, deduplicator, transactionRepository, context) {
            this.ioDispatcher = ioDispatcher
            this.accountRepository = accountRepository
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

        private val parser by lazy {
            try {
                TemplateLoader.createParser(context)
            } catch (_: Throwable) {
                TemplateLoader.createParser(null)
            }
        }

        fun moveToState(state: OnboardingState) {
            _uiState.update { it.copy(currentState = state) }
        }

        fun saveProfile(
            name: String,
            phone: String,
        ) {
            DefaultUserPreferences(context).apply {
                userName = name.trim()
                userPhone = phone.trim()
            }
            moveToState(OnboardingState.PERMISSION)
        }

        fun onPermissionResult(
            isGranted: Boolean,
            window: String = "90 days",
        ) {
            if (isGranted) {
                _uiState.update { it.copy(currentState = OnboardingState.IMPORT_PROGRESS, isPermissionDenied = false) }
                startImport(window = window, storeRaw = false)
            } else {
                _uiState.update { it.copy(isPermissionDenied = true) }
            }
        }

        fun startImport(
            window: String = "90 days",
            storeRaw: Boolean = false,
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
                val seenBodyKeys = mutableSetOf<String>()
                val userRules = DefaultUserPreferences(context).getMerchantCategoryRules()

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

                        val bodyKey = "${sms.sender}|${sms.body.trim().replace(Regex("\\s+"), " ")}"
                        if (!seenBodyKeys.add(bodyKey)) {
                            continue
                        }

                        if (!senderFilter.isFinancialSender(sms.sender)) {
                            continue
                        }

                        if (NoiseFilter.filter(sms) != null) {
                            continue
                        }

                        val parseResult = parser.parse(sms)
                        if (parseResult is ParseResult.Success) {
                            val parsedTxn = parseResult.txn

                            foundCount++
                            val isCredit = parsedTxn.direction == Direction.CREDIT
                            val rawMerchant = parsedTxn.merchantRaw ?: parsedTxn.upiId
                            val normMerchant =
                                TransactionClassifierHelper.cleanMerchantDisplay(
                                    rawMerchant,
                                    parsedTxn.bankName,
                                    parsedTxn.channel?.name,
                                )
                            val type =
                                TransactionClassifierHelper.classifyType(
                                    direction = parsedTxn.direction,
                                    body = sms.body,
                                    merchant = normMerchant,
                                    amountMinor = parsedTxn.amountMinor,
                                    channel = parsedTxn.channel,
                                )
                            val isCardBill = TransactionClassifierHelper.isCreditCardBillPayment(sms.body, normMerchant)
                            val targetCardLast4 = if (isCardBill) TransactionClassifierHelper.extractTargetCardLast4(sms.body) else null
                            val resolvedAccountLast4 = targetCardLast4 ?: parsedTxn.accountLast4
                            val resolvedChannel = if (isCardBill) Channel.CARD else parsedTxn.channel
                            val isTransferNarrative = TransactionClassifierHelper.isTransferNarrative(
                                body = sms.body,
                                merchant = normMerchant,
                                amountMinor = parsedTxn.amountMinor,
                                channel = parsedTxn.channel,
                            )
                            val normLower = normMerchant.lowercase().trim()
                            val isUserCategorized = userRules.containsKey(normLower)
                            val category =
                                TransactionClassifierHelper.guessCategory(
                                    normMerchant,
                                    isCredit,
                                    isTransfer = isTransferNarrative,
                                    body = sms.body,
                                    userRules = userRules,
                                )
                            val isTransfer = !isCardBill && (category.equals("Transfer", ignoreCase = true) || category.equals("A/C Transfer", ignoreCase = true))
                            val isCounted = !isCardBill && !isTransfer
                            val isHighConf = (parsedTxn.confidence >= 0.8f) && (rawMerchant != null)
                            val isReviewed = isUserCategorized || (isHighConf && category != "Other")

                            val accountId =
                                try {
                                    val accountRepo =
                                        accountRepository ?: DefaultAccountRepository.getInstance(context)
                                    val account =
                                        accountRepo.resolveOrCreateAccount(
                                            bankName = parsedTxn.bankName,
                                            last4 = resolvedAccountLast4,
                                            channel = resolvedChannel,
                                            balanceMinor = parsedTxn.balanceAfterMinor,
                                            timestamp = parsedTxn.dateTime ?: sms.receivedAt,
                                            smsBody = sms.body,
                                        )
                                    account.id
                                } catch (_: Throwable) {
                                    1L
                                }

                            val txn =
                                Transaction(
                                    id = sms.id,
                                    smsHash = hash,
                                    accountId = accountId,
                                    type = type,
                                    amount = parsedTxn.amountMinor,
                                    currency = parsedTxn.currency,
                                    dateTime = parsedTxn.dateTime ?: sms.receivedAt,
                                    merchantRaw = rawMerchant ?: normMerchant,
                                    merchantNormalised = normMerchant,
                                    categorySource = category,
                                    channel = resolvedChannel,
                                    refNumber = parsedTxn.refNumber,
                                    upiId = parsedTxn.upiId,
                                    balanceAfter = parsedTxn.balanceAfterMinor,
                                    note = sms.body.trim(),
                                    parseConfidence = parsedTxn.confidence,
                                    isReviewed = isReviewed,
                                    templateId = parsedTxn.templateId ?: "generic_fallback",
                                    accountLast4 = resolvedAccountLast4,
                                    bankName = parsedTxn.bankName,
                                    sender = sms.sender,
                                    isCounted = isCounted,
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

                try {
                    val accountRepo = accountRepository ?: DefaultAccountRepository.getInstance(context)
                    val allTxns = transactionRepository.getTransactionsSync()
                    accountRepo.autoLinkAccounts(allTxns)
                } catch (_: Throwable) {
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
    }
