package com.kharcha.engine.sms

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kharcha.core.model.IgnoreReason
import com.kharcha.core.model.IgnoredMessage
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
        override suspend fun doWork(): Result =
            withContext(Dispatchers.IO) {
                try {
                    val batchSize = 500
                    var lastProcessedId = smsRepository.getCheckpoint()
                    var processedCount = 0

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
                            }
                            lastProcessedId = maxOf(lastProcessedId, sms.id)
                        }

                        smsRepository.saveCheckpoint(lastProcessedId)
                        processedCount += batch.size
                    }
                    Result.success()
                } catch (e: Exception) {
                    Result.retry()
                }
            }
    }
