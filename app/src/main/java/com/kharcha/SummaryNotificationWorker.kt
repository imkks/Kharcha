package com.kharcha

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.common.NotificationChannels
import com.kharcha.core.common.TransactionRepository
import com.kharcha.core.common.UserPreferences
import com.kharcha.core.model.TransactionType
import com.kharcha.core.model.forTotals
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import java.util.concurrent.TimeUnit

internal fun exactAlarmAllowed(apiLevel: Int, canScheduleExactAlarms: Boolean): Boolean =
    apiLevel < 31 || canScheduleExactAlarms

@HiltWorker
class SummaryNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: TransactionRepository,
    private val preferences: UserPreferences,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val weekly = inputData.getBoolean(KEY_WEEKLY, false)
        val enabled = if (weekly) preferences.isWeeklySummaryEnabled else preferences.isDailySummaryEnabled

        val minuteOfDay = if (weekly) preferences.weeklySummaryMinuteOfDay else preferences.dailySummaryMinuteOfDay
        val nextDelay = millisUntilNextRun(minuteOfDay, weekly)
        runCatching {
            WorkManager.getInstance(applicationContext).updateWork(
                PeriodicWorkRequestBuilder<SummaryNotificationWorker>(if (weekly) 7L else 1L, TimeUnit.DAYS)
                    .setId(id)
                    .setNextScheduleTimeOverride(System.currentTimeMillis() + nextDelay)
                    .build(),
            )
        }

        if (!enabled) return Result.success()

        publishSummary(weekly)
        return Result.success()
    }

    private fun publishSummary(weekly: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = if (weekly) now - TimeUnit.DAYS.toMillis(7) else calendar.timeInMillis
        val total = repository.getTransactionsSync()
            .forTotals()
            .asSequence()
            .filter { it.dateTime in start..now && it.type == TransactionType.Expense }
            .sumOf { it.amount }

        ensureSummaryChannel(applicationContext)
        val openIntent = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
            ?.putExtra(EXTRA_OPEN_WEEKLY_SUMMARY, weekly)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = openIntent?.let {
            PendingIntent.getActivity(applicationContext, if (weekly) 102 else 101, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val title = applicationContext.getString(if (weekly) R.string.weekly_summary_notification_title else R.string.daily_summary_notification_title)
        val message = applicationContext.getString(R.string.summary_notification_body, MoneyFormatter.formatMinorUnits(total))
        NotificationManagerCompat.from(applicationContext).notify(
            if (weekly) WEEKLY_NOTIFICATION_ID else DAILY_NOTIFICATION_ID,
            NotificationCompat.Builder(applicationContext, NotificationChannels.SPEND_SUMMARIES)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build(),
        )
    }

    companion object {
        const val KEY_WEEKLY = "weekly"
        private const val DAILY_NOTIFICATION_ID = 21001
        private const val WEEKLY_NOTIFICATION_ID = 21002
        private const val DAILY_WORK = "daily_spend_summary"
        private const val WEEKLY_WORK = "weekly_spend_summary"
        const val EXTRA_OPEN_WEEKLY_SUMMARY = "open_weekly_summary"

        fun scheduleAll(context: Context, preferences: UserPreferences, updateExisting: Boolean = false) {
            ensureSummaryChannel(context)
            sync(context, weekly = false, preferences.isDailySummaryEnabled, preferences.dailySummaryMinuteOfDay, updateExisting)
            sync(context, weekly = true, preferences.isWeeklySummaryEnabled, preferences.weeklySummaryMinuteOfDay, updateExisting)
        }

        fun sync(
            context: Context,
            weekly: Boolean,
            enabled: Boolean,
            minuteOfDay: Int = 21 * 60,
            updateExisting: Boolean = false,
        ) {
            val workManager = WorkManager.getInstance(context)
            val workName = if (weekly) WEEKLY_WORK else DAILY_WORK
            if (!enabled) {
                workManager.cancelUniqueWork(workName)
                return
            }

            val delayMillis = millisUntilNextRun(minuteOfDay, weekly)
            val intervalDays = if (weekly) 7L else 1L
            val request = PeriodicWorkRequestBuilder<SummaryNotificationWorker>(intervalDays, TimeUnit.DAYS)
                .setInputData(androidx.work.Data.Builder().putBoolean(KEY_WEEKLY, weekly).build())
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build()

            val policy = if (updateExisting) {
                ExistingPeriodicWorkPolicy.UPDATE
            } else {
                ExistingPeriodicWorkPolicy.KEEP
            }

            workManager.enqueueUniquePeriodicWork(
                workName,
                policy,
                request,
            )
        }

        fun triggerNow(context: Context, weekly: Boolean = false) {
            ensureSummaryChannel(context)
            val request = OneTimeWorkRequestBuilder<SummaryNotificationWorker>()
                .setInputData(androidx.work.Data.Builder().putBoolean(KEY_WEEKLY, weekly).build())
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }

        fun scheduleFromSystem(context: Context) {
            scheduleAll(context, com.kharcha.core.common.DefaultUserPreferences(context))
        }

        private fun ensureSummaryChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(NotificationManager::class.java)
                manager.createNotificationChannel(
                    NotificationChannel(
                        NotificationChannels.SPEND_SUMMARIES,
                        context.getString(R.string.summary_notification_channel),
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ),
                )
            }
        }

        internal fun millisUntilNextRun(
            minuteOfDay: Int,
            weekly: Boolean = false,
            now: Calendar = Calendar.getInstance(),
        ): Long {
            val next = now.clone() as Calendar
            next.set(Calendar.HOUR_OF_DAY, minuteOfDay.coerceIn(0, 1439) / 60)
            next.set(Calendar.MINUTE, minuteOfDay.coerceIn(0, 1439) % 60)
            next.set(Calendar.SECOND, 0)
            next.set(Calendar.MILLISECOND, 0)
            if (weekly) {
                val daysUntilSunday = (Calendar.SUNDAY - next.get(Calendar.DAY_OF_WEEK) + 7) % 7
                next.add(Calendar.DAY_OF_YEAR, daysUntilSunday)
                if (next.timeInMillis <= now.timeInMillis) next.add(Calendar.DAY_OF_YEAR, 7)
            } else if (next.timeInMillis <= now.timeInMillis) {
                next.add(Calendar.DAY_OF_YEAR, 1)
            }
            return (next.timeInMillis - now.timeInMillis).coerceAtLeast(1L)
        }
    }
}
