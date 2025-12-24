package com.example.myapplication

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat

class SmsService : Service() {

    private lateinit var smsObserver: SmsContentObserver

    override fun onCreate() {
        super.onCreate()

        // 1. Start as Foreground (Required to keep Observer alive)
        startForeground(101, createNotification())

        // 2. Register the Observer
        smsObserver = SmsContentObserver(this)
        contentResolver.registerContentObserver(
            Uri.parse("content://sms"),
            true,
            smsObserver
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        // Prevent memory leaks
        contentResolver.unregisterContentObserver(smsObserver)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "BudgetServiceChannel"
        val channel = NotificationChannel(channelId, "Budget Tracker Service", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Budget Tracker Active")
            .setContentText("Watching SMS Database for transactions...")
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .build()
    }
}