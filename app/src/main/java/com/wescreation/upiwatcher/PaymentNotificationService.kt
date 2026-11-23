package com.wescreation.upiwatcher

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** Optional: reads "Received ₹… " notifications of the UPI apps you list in the settings. */
class PaymentNotificationService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val n = sbn ?: return
        if (!Prefs.notifEnabled(this)) return
        if (n.packageName !in Filters.packageSet(Prefs.packages(this))) return

        val extras = n.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val body = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val full = "$title. $body".trim()
        if (!Filters.looksLikeIncomingMoney(full)) return

        Forwarder.enqueue(this, "app", full)
    }
}
