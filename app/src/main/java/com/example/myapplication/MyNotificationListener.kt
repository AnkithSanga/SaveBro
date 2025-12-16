package com.example.myapplication

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class MyNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras

        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        NotificationStore.lastNotification =
            if (title.isNotEmpty()) "$title : $text" else text
    }
}
