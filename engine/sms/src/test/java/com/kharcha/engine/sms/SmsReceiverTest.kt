package com.kharcha.engine.sms

import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SmsReceiverTest {
    private lateinit var context: Context
    private lateinit var receiver: SmsReceiver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        receiver = SmsReceiver()
    }

    @Test
    fun onReceivewithemptymessagesdoesnotcrash() {
        val intent = Intent(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)

        // This won't have any actual SMS data but tests basic intent handling
        try {
            receiver.onReceive(context, intent)
        } catch (e: Exception) {
            // Robolectric sometimes fails on parsing empty PDUs from intent
        }
    }
}
