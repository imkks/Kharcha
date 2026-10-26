package com.kharcha.engine.sms

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.kharcha.core.common.DefaultAccountRepository
import com.kharcha.core.common.DefaultTransactionRepository
import com.kharcha.core.common.TransactionClassifierHelper
import com.kharcha.core.model.Direction
import com.kharcha.core.model.IgnoreReason
import com.kharcha.core.model.IgnoredMessage
import com.kharcha.core.model.ParseResult
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import com.kharcha.engine.parser.TemplateLoader
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class ImportWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val inboxReader: InboxReader,
        private val senderFilter: SenderFilter,
        private val deduplicator: Deduplicator,
        private val smsRepository: SmsRepository,
    ) : CoroutineWorker(appContext, workerParams) {
        @Suppress("TooGenericExceptionCaught")
        override suspend fun doWork(): Result =
            withContext(Dispatchers.IO) {
                try {
                    val batchSize = 500
                    var lastProcessedId = smsRepository.getCheckpoint()
                    var processedCount = 0

                    val totalSms = inboxReader.getSmsCount()
                    setProgress(workDataOf("progress" to 0.0f, "processedCount" to 0))

                    val parser = TemplateLoader.createParser(applicationContext)

                    while (true) {
                        val batch = inboxReader.readSmsBatch(lastProcessedId, batchSize)
                        if (batch.isEmpty()) break

                        for (sms in batch) {
                            if (!senderFilter.isFinancialSender(sms.sender)) {
                                smsRepository.saveIgnoredMessage(
                                    IgnoredMessage(
                                        smsHash = deduplicator.generateHash(sms.sender, sms.body, sms.receivedAt),
                                        reason = IgnoreReason.NON_FINANCIAL,
                                        receivedAt = sms.receivedAt,
                                    ),
                                )
                            } else if (!deduplicator.isDuplicate(sms.sender, sms.body, sms.receivedAt)) {
                                smsRepository.saveRawSms(sms)
                                try {
                                    val parseResult = parser.parse(sms)
                                    if (parseResult is ParseResult.Success) {
                                        val parsed = parseResult.txn
                                        val isCredit = parsed.direction == Direction.CREDIT
                                        val rawMerchant = parsed.merchantRaw ?: parsed.upiId
                                        val normMerchant =
                                            TransactionClassifierHelper.cleanMerchantDisplay(
                                                rawMerchant,
                                                parsed.bankName,
                                                parsed.channel?.name,
                                            )
                                        val type =
                                            TransactionClassifierHelper.classifyType(
                                                parsed.direction,
                                                sms.body,
                                                normMerchant,
                                            )
                                        val isTransfer = type == TransactionType.Transfer
                                        val category =
                                            TransactionClassifierHelper.guessCategory(
                                                normMerchant,
                                                isCredit,
                                                isTransfer,
                                            )
                                        val accountId =
                                            try {
                                                val accountRepo =
                                                    DefaultAccountRepository.getInstance(applicationContext)
                                                val account =
                                                    accountRepo.resolveOrCreateAccount(
                                                        bankName = parsed.bankName,
                                                        last4 = parsed.accountLast4,
                                                        channel = parsed.channel,
                                                        balanceMinor = parsed.balanceAfterMinor,
                                                        timestamp = parsed.dateTime ?: sms.receivedAt,
                                                        smsBody = sms.body,
                                                    )
                                                account.id
                                            } catch (_: Throwable) {
                                                1L
                                            }
                                        val txn =
                                            Transaction(
                                                id = sms.id,
                                                smsHash =
                                                    deduplicator.generateHash(
                                                        sms.sender,
                                                        sms.body,
                                                        sms.receivedAt,
                                                    ),
                                                accountId = accountId,
                                                type = type,
                                                amount = parsed.amountMinor,
                                                currency = parsed.currency,
                                                dateTime = parsed.dateTime ?: sms.receivedAt,
                                                merchantRaw = rawMerchant ?: normMerchant,
                                                merchantNormalised = normMerchant,
                                                categorySource = category,
                                                channel = parsed.channel,
                                                refNumber = parsed.refNumber,
                                                upiId = parsed.upiId,
                                                balanceAfter = parsed.balanceAfterMinor,
                                                note = sms.body.trim(),
                                                parseConfidence = parsed.confidence,
                                                isReviewed = true,
                                            )
                                        DefaultTransactionRepository.getInstance(applicationContext)
                                            .insertTransaction(txn)
                                    }
                                } catch (_: Throwable) {
                                }
                            }
                            lastProcessedId = maxOf(lastProcessedId, sms.id)
                        }

                        smsRepository.saveCheckpoint(lastProcessedId)
                        processedCount += batch.size

                        val currentProgress =
                            if (totalSms > 0) {
                                (processedCount.toFloat() / totalSms.toFloat()).coerceIn(0f, 1f)
                            } else {
                                1f
                            }
                        setProgress(
                            workDataOf(
                                "progress" to currentProgress,
                                "processedCount" to processedCount,
                            ),
                        )
                    }
                    setProgress(workDataOf("progress" to 1.0f, "processedCount" to processedCount))
                    Result.success()
                } catch (e: Exception) {
                    Result.retry()
                }
            }
    }
