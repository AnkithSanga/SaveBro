package com.example.myapplication

import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsService : Service() {
    private lateinit var observer: android.database.ContentObserver

    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())
        observer = object : android.database.ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                if (uri.toString().contains("sms")) {
                    CoroutineScope(Dispatchers.IO).launch {
                        delayProcessing() // Small delay for DB write
                        InboxReader.syncTodaysInbox(this@SmsService)
                    }
                }
            }
        }
        contentResolver.registerContentObserver(Uri.parse("content://sms"), true, observer)
    }

    private suspend fun delayProcessing() { kotlinx.coroutines.delay(1000) }

    private fun createNotification(): Notification {
        val channelId = "budget_sync"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "SMS Sync", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId).setContentTitle("Save Bro is running").setSmallIcon(android.R.drawable.ic_menu_save).build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() { contentResolver.unregisterContentObserver(observer); super.onDestroy() }
}