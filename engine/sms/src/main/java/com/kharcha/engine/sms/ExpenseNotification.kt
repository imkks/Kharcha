package com.kharcha.engine.sms

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
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType

/** Renders a newly saved expense with the suggested categories as notification actions. */
class ExpenseNotification(
    private val context: Context,
    private val openTransactionIntent: (Long) -> Intent,
    private val changeCategoryIntent: (Long) -> Intent,
    private val notExpenseIntent: (Long) -> Intent,
    private val showSmsIntent: (Long) -> Intent,
) {
    fun show(
        transaction: Transaction,
        merchant: String,
        categoryName: String,
        accountDescription: String,
        hideAmount: Boolean = false,
    ) {
        if (transaction.type != TransactionType.Expense) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        createChannel()
        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(
                    if (hideAmount) {
                        context.getString(R.string.expense_notification_hidden_title, merchant)
                    } else {
                        context.getString(
                            R.string.expense_notification_title,
                            formatAmount(transaction.amount),
                            merchant,
                        )
                    },
                )
                .setContentText(context.getString(R.string.expense_notification_body, categoryName, accountDescription))
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(
                            context.getString(
                                R.string.expense_notification_body,
                                categoryName,
                                accountDescription,
                            ),
                        ),
                ).setContentIntent(pendingIntent(openTransactionIntent(transaction.id), transaction.id, 0))
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .apply {
                    addAction(
                        0,
                        context.getString(R.string.expense_notification_edit),
                        pendingIntent(changeCategoryIntent(transaction.id), transaction.id, 1),
                    )
                    addAction(
                        0,
                        context.getString(R.string.expense_notification_not_expense),
                        pendingIntent(notExpenseIntent(transaction.id), transaction.id, 2),
                    )
                    addAction(
                        0,
                        context.getString(R.string.expense_notification_show_sms),
                        pendingIntent(showSmsIntent(transaction.id), transaction.id, 3),
                    )
                }.build()

        NotificationManagerCompat.from(context).notify(notificationId(transaction.id), notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.expense_notification_channel),
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
        }
    }

    private fun pendingIntent(
        intent: Intent,
        transactionId: Long,
        requestCodeSuffix: Int,
    ): PendingIntent =
        PendingIntent.getActivity(
            context,
            notificationId(transactionId) * ACTION_REQUEST_CODE_FACTOR + requestCodeSuffix,
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun formatAmount(amountMinor: Long): String {
        val whole = amountMinor / MINOR_UNITS_PER_RUPEE
        val paise = (amountMinor % MINOR_UNITS_PER_RUPEE).toString().padStart(2, '0')
        return context.getString(R.string.expense_notification_amount, whole, paise)
    }

    private fun notificationId(transactionId: Long): Int = (transactionId % Int.MAX_VALUE).toInt()

    companion object {
        const val CHANNEL_ID = "transaction_expenses"
        const val EXTRA_TRANSACTION_ID = "com.kharcha.notification.TRANSACTION_ID"
        const val EXTRA_ACTION = "com.kharcha.notification.ACTION"
        const val ACTION_OPEN = "open"
        const val ACTION_EDIT_CATEGORY = "edit_category"
        const val ACTION_NOT_EXPENSE = "not_expense"
        const val ACTION_SHOW_SMS = "show_sms"
        private const val MINOR_UNITS_PER_RUPEE = 100L
        private const val ACTION_REQUEST_CODE_FACTOR = 10

        fun forApplication(context: Context): ExpenseNotification =
            ExpenseNotification(
                context = context,
                openTransactionIntent = { id -> createLaunchIntent(context, id, ACTION_OPEN) },
                changeCategoryIntent = { id -> createLaunchIntent(context, id, ACTION_EDIT_CATEGORY) },
                notExpenseIntent = { id -> createLaunchIntent(context, id, ACTION_NOT_EXPENSE) },
                showSmsIntent = { id -> createLaunchIntent(context, id, ACTION_SHOW_SMS) },
            )

        private fun createLaunchIntent(
            context: Context,
            transactionId: Long,
            action: String,
        ): Intent =
            (context.packageManager.getLaunchIntentForPackage(context.packageName) ?: Intent()).apply {
                putExtra(EXTRA_TRANSACTION_ID, transactionId)
                putExtra(EXTRA_ACTION, action)
            }
    }
}
