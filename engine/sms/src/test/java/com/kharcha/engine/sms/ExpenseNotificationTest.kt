package com.kharcha.engine.sms

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import com.kharcha.core.model.Transaction
import com.kharcha.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(packageName = "com.kharcha.engine.sms", sdk = [28])
class ExpenseNotificationTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun publishesExpenseWithReferenceActionsAndCurrency() {
        val transaction = expense()
        publisher().show(transaction, "HungerBox", "Food and Dining", "HDFC Bank")

        val notification =
            shadowOf(context.getSystemService(NotificationManager::class.java))
                .getNotification(transaction.id.toInt())

        assertNotNull(notification)
        assertEquals("Spent ₹25.00 at HungerBox", notification.extras.getString("android.title"))
        assertEquals("Food and Dining · HDFC Bank", notification.extras.getString("android.text"))
        assertEquals(3, notification.actions.size)
        assertEquals("Edit", notification.actions[0].title)
        assertEquals("Not an expense", notification.actions[1].title)
        assertEquals("Show SMS", notification.actions[2].title)
    }

    @Test
    fun hidesAmountWhenRequested() {
        val transaction = expense()
        publisher().show(transaction, "HungerBox", "Food and Dining", "HDFC Bank", hideAmount = true)

        val notification =
            shadowOf(context.getSystemService(NotificationManager::class.java))
                .getNotification(transaction.id.toInt())

        assertEquals("New expense at HungerBox", notification.extras.getString("android.title"))
    }

    @Test
    fun doesNotPublishNonExpense() {
        val transaction = expense().copy(type = TransactionType.Income)
        publisher().show(transaction, "Employer", "Salary", "HDFC Bank")

        val notification =
            shadowOf(context.getSystemService(NotificationManager::class.java))
                .getNotification(transaction.id.toInt())

        assertNull(notification)
    }

    private fun publisher() =
        ExpenseNotification(
            context = context,
            openTransactionIntent = { Intent(context, Any::class.java) },
            changeCategoryIntent = { Intent(context, Any::class.java) },
            notExpenseIntent = { Intent(context, Any::class.java) },
            showSmsIntent = { Intent(context, Any::class.java) },
        )

    private fun expense() =
        Transaction(
            id = 123L,
            accountId = 1L,
            type = TransactionType.Expense,
            amount = 2_500L,
            dateTime = 1L,
        )
}
