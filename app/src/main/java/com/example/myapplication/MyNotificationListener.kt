package com.example.myapplication

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class MyNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return

        // Use the parser to see if it's a valid credit/debit
        val parsed = TransactionParser.parse(text)

        if (parsed != null) {
            // Save to ObjectBox or JSON
            TransactionStorage.saveTransaction(parsed.title, parsed.type, parsed.amount)

            // Update UI Store
            NotificationStore.lastNotification = "${parsed.type.uppercase()}: ₹${parsed.amount} - ${parsed.title}"
        }
    }
}