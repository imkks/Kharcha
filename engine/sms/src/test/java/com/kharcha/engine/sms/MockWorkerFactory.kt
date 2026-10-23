package com.kharcha.engine.sms

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters

class MockWorkerFactory(
    private val inboxReader: InboxReader,
    private val senderFilter: SenderFilter,
    private val deduplicator: Deduplicator,
    private val repository: SmsRepository,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? {
        return when (workerClassName) {
            ImportWorker::class.java.name ->
                ImportWorker(appContext, workerParameters, inboxReader, senderFilter, deduplicator, repository)
            else -> null
        }
    }
}
