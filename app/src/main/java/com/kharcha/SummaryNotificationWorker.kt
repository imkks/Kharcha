package com.kharcha

import android.Manifest
import android.app.AlarmManager
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
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kharcha.core.common.TransactionRepository
import com.kharcha.core.common.UserPreferences
import com.kharcha.core.common.NotificationChannels
import com.kharcha.core.model.TransactionType
import com.kharcha.core.common.MoneyFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import java.util.concurrent.TimeUnit

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
        try {
            if (enabled) publishSummary(weekly)
        } finally {
            schedule(applicationContext, weekly, preferences, ExistingWorkPolicy.APPEND_OR_REPLACE)
        }
        return Result.success()
    }

    private fun publishSummary(weekly: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val now = System.currentTimeMillis()
        val days = if (weekly) 7 else 1
        val start = now - TimeUnit.DAYS.toMillis(days.toLong())
        val total = repository.getTransactionsSync()
            .asSequence()
            .filter { it.dateTime in start..now && it.type == TransactionType.Expense && it.isCounted }
            .sumOf { it.amount }
        ensureSummaryChannel(applicationContext)
        val openIntent = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
            ?.putExtra(EXTRA_OPEN_WEEKLY_SUMMARY, weekly)
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
        private const val KEY_WEEKLY = "weekly"
        private const val DAILY_NOTIFICATION_ID = 21001
        private const val WEEKLY_NOTIFICATION_ID = 21002
        private const val DAILY_WORK = "daily_spend_summary"
        private const val WEEKLY_WORK = "weekly_spend_summary"
        const val EXTRA_OPEN_WEEKLY_SUMMARY = "open_weekly_summary"

        fun scheduleAll(context: Context, preferences: UserPreferences) {
            ensureSummaryChannel(context)
            schedule(context, weekly = false, preferences)
            schedule(context, weekly = true, preferences)
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

        fun schedule(
            context: Context,
            weekly: Boolean,
            preferences: UserPreferences,
            existingWorkPolicy: ExistingWorkPolicy = ExistingWorkPolicy.REPLACE,
        ) {
            val minuteOfDay = if (weekly) preferences.weeklySummaryMinuteOfDay else preferences.dailySummaryMinuteOfDay
            val delay = millisUntilNextRun(minuteOfDay, weekly)
            val workName = if (weekly) WEEKLY_WORK else DAILY_WORK
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val alarmIntent = Intent(context, SummaryNotificationReceiver::class.java)
                .setAction(SummaryAlarmContract.ACTION_PUBLISH)
                .putExtra(SummaryAlarmContract.EXTRA_WEEKLY, weekly)
            val alarmPendingIntent = PendingIntent.getBroadcast(
                context,
                if (weekly) WEEKLY_NOTIFICATION_ID else DAILY_NOTIFICATION_ID,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.cancel(alarmPendingIntent)
            val exactAlarmAllowed =
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    exactAlarmAllowed(Build.VERSION.SDK_INT, alarmManager.canScheduleExactAlarms())
            if (exactAlarmAllowed) {
                try {
                    if (existingWorkPolicy == ExistingWorkPolicy.REPLACE) {
                        WorkManager.getInstance(context).cancelUniqueWork(workName)
                    }
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + delay,
                        alarmPendingIntent,
                    )
                    return
                } catch (_: SecurityException) {
                    // A permission change between checking and scheduling uses the WorkManager fallback.
                }
            }
            val request = OneTimeWorkRequestBuilder<SummaryNotificationWorker>()
                .setInputData(Data.Builder().putBoolean(KEY_WEEKLY, weekly).build())
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                workName,
                existingWorkPolicy,
                request,
            )
        }

        fun scheduleFromSystem(context: Context) {
            scheduleAll(context, com.kharcha.core.common.DefaultUserPreferences(context))
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

internal fun exactAlarmAllowed(apiLevel: Int, canScheduleExactAlarms: Boolean): Boolean =
    apiLevel < Build.VERSION_CODES.S || canScheduleExactAlarms
