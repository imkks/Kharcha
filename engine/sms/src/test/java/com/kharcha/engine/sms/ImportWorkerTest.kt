package com.kharcha.engine.sms

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.kharcha.core.model.RawSms
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ImportWorkerTest {
    private lateinit var context: Context
    private lateinit var inboxReader: InboxReader
    private lateinit var senderFilter: SenderFilter
    private lateinit var deduplicator: Deduplicator
    private lateinit var repository: SmsRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        inboxReader = mockk()
        senderFilter = mockk()
        deduplicator = mockk()
        repository = mockk(relaxed = true)
    }

    @Test
    fun doWorkprocesses10000messagessuccessfully() =
        runTest {
            val totalMessages = 10000
            val batchSize = 500
            val batches = (totalMessages / batchSize)

            coEvery { repository.getCheckpoint() } returns 0L
            coEvery { inboxReader.getSmsCount(any()) } returns totalMessages

            var currentId = 1L
            for (i in 0 until batches) {
                val batch =
                    (1..batchSize).map {
                        RawSms(
                            id = currentId++,
                            sender = "XX-HDFCBK",
                            body = "Test $it",
                            receivedAt = 12345L,
                            subId = 1,
                        )
                    }
                coEvery {
                    inboxReader.readSmsBatch(if (i == 0) 0L else currentId - batchSize - 1, batchSize)
                } returns batch
            }
            coEvery {
                inboxReader.readSmsBatch(currentId - 1, batchSize)
            } returns emptyList()
            coEvery { deduplicator.generateHash(any(), any(), any()) } returns "hash"
            coEvery { deduplicator.isDuplicate(any(), any(), any()) } returns false
            coEvery { senderFilter.isFinancialSender("XX-HDFCBK") } returns true

            val worker =
                TestListenableWorkerBuilder<ImportWorker>(context)
                    .setWorkerFactory(MockWorkerFactory(inboxReader, senderFilter, deduplicator, repository))
                    .build()

            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            coVerify(exactly = totalMessages) { repository.saveRawSms(any()) }
        }
}
