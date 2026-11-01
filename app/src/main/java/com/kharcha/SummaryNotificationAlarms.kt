package com.kharcha

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kharcha.core.common.DefaultTransactionRepository
import com.kharcha.core.common.DefaultUserPreferences
import com.kharcha.core.common.MoneyFormatter
import com.kharcha.core.common.NotificationChannels
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

internal object SummaryAlarmContract {
    const val ACTION_PUBLISH = "com.kharcha.action.PUBLISH_SPEND_SUMMARY"
    const val EXTRA_WEEKLY = "summary_is_weekly"
}

class SummaryNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SummaryAlarmContract.ACTION_PUBLISH) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val weekly = intent.getBooleanExtra(SummaryAlarmContract.EXTRA_WEEKLY, false)
        CoroutineScope(Dispatchers.IO).launch {
            val preferences = DefaultUserPreferences(appContext)
            try {
                val enabled = if (weekly) preferences.isWeeklySummaryEnabled else preferences.isDailySummaryEnabled
                if (enabled) {
                    val transactions = DefaultTransactionRepository.getInstance(appContext).getTransactionsSync()
                    publishSummary(appContext, weekly, transactions)
                }
            } finally {
                try {
                    SummaryNotificationWorker.schedule(appContext, weekly, preferences)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

class SummaryScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            -> SummaryNotificationWorker.scheduleFromSystem(context.applicationContext)
        }
    }
}

internal fun publishSummary(context: Context, weekly: Boolean, transactions: List<Transaction>) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return
    val now = System.currentTimeMillis()
    val days = if (weekly) 7 else 1
    val start = now - TimeUnit.DAYS.toMillis(days.toLong())
    val total = transactions.asSequence()
        .filter { it.dateTime in start..now && it.type == TransactionType.Expense && it.isCounted }
        .sumOf { it.amount }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            android.app.NotificationChannel(
                NotificationChannels.SPEND_SUMMARIES,
                context.getString(R.string.summary_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }
    val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        ?.putExtra(SummaryNotificationWorker.EXTRA_OPEN_WEEKLY_SUMMARY, weekly)
    val contentIntent = openIntent?.let {
        PendingIntent.getActivity(context, if (weekly) 102 else 101, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    val title = context.getString(if (weekly) R.string.weekly_summary_notification_title else R.string.daily_summary_notification_title)
    val message = context.getString(R.string.summary_notification_body, MoneyFormatter.formatMinorUnits(total))
    NotificationManagerCompat.from(context).notify(
        if (weekly) 21002 else 21001,
        NotificationCompat.Builder(context, NotificationChannels.SPEND_SUMMARIES)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build(),
    )
}
