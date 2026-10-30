package com.kharcha

import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.kharcha.core.common.DefaultUserPreferences

/** Dismisses only notifications from the device's configured default SMS app when opted in. */
class SmsNotificationHider : NotificationListenerService() {
    override fun onNotificationPosted(notification: StatusBarNotification) {
        val smsPackage = Telephony.Sms.getDefaultSmsPackage(this) ?: return
        if (notification.packageName == smsPackage && DefaultUserPreferences(this).hideDefaultSmsNotifications) {
            cancelNotification(notification.key)
        }
    }
}
