package com.kharcha

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kharcha.core.common.RecurringTransactionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@HiltWorker
class RecurringTransactionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val recurringTransactionRepository: RecurringTransactionRepository,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val WORK_NAME_PERIODIC = "recurring_transactions_materialize_periodic"
        private const val WORK_NAME_ONE_SHOT = "recurring_transactions_materialize_one_shot"

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(
                1, TimeUnit.DAYS
            ).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME_PERIODIC,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }

        fun enqueueOneShotCatchUp(context: Context) {
            val request = OneTimeWorkRequestBuilder<RecurringTransactionWorker>().build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORK_NAME_ONE_SHOT,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
        }
    }

    override suspend fun doWork(): Result {
        recurringTransactionRepository.materializeDue(LocalDate.now())
        return Result.success()
    }
}
