package com.example.myapplication

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log

class SmsContentObserver(private val context: Context) : ContentObserver(Handler(Looper.getMainLooper())) {

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)

        // Only react to SMS database changes
        if (uri.toString().contains("sms")) {

            // 🕒 Wait 500ms to ensure the message is fully written to the DB
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    InboxReader.readLatestSms(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, 500)
        }
    }
}