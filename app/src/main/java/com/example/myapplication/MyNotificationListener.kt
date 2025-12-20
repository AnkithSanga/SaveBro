package com.example.myapplication

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MyNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras

        // 1. Get Sender (Title)
        val senderTitle = extras.getString(Notification.EXTRA_TITLE) ?: return

        // 🎯 STRICT FILTER: Only official service messages (ending in -S or -A etc)
        // Adjust this if you want to allow others, but generally banks use suffixes.
        if (!senderTitle.contains("-")) return

        // 2. Get Full Body
        val body = (extras.get(Notification.EXTRA_BIG_TEXT) ?: extras.get(Notification.EXTRA_TEXT))?.toString() ?: ""
        if (body.isEmpty()) return

        // 3. Parse
        val parsed = TransactionParser.parse(body)


        if (parsed != null) {
            // ✅ LOGIC: If parser found a name (e.g. "Netflix"), use it.
            // If parser returned null (generic msg), use the Sender Title (e.g. "AD-HDFCBK").
            val finalDisplayName = parsed.title ?: senderTitle

            TransactionStorage.saveTransaction(
                senderName = senderTitle,
                merchantName = finalDisplayName,
                type = parsed.type,
                amount = parsed.amount,
                rawMessage = body
            )

            NotificationStore.lastNotification = "Saved: $finalDisplayName (₹${parsed.amount})"
        }
    }
}