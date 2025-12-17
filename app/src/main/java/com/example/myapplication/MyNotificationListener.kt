package com.example.myapplication

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MyNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // 🏦 1. Get the Sender Name (e.g., "AD-HDFCBK-S")
        val sender = sbn.notification.extras.getString(Notification.EXTRA_TITLE) ?: ""

        // 🎯 2. STRICT FILTER: Only process if it ends with "-S"
        if (!sender.uppercase().endsWith("-S")) {
            return
        }

        // 3. Get message body
        val text = sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return

        // 🔍 4. Parse the content (Credit/Debit/Merchant)
        val parsed = TransactionParser.parse(text)

        if (parsed != null) {
            // ✅ Only saves if it's a real transaction, not a mandate creation
            TransactionStorage.saveTransaction(parsed.title, parsed.type, parsed.amount)

            // Log for debugging
            NotificationStore.lastNotification = "Saved: ${parsed.title} (₹${parsed.amount})"
        }
    }
}