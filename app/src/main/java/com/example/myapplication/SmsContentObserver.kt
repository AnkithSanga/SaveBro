package com.example.myapplication

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SmsContentObserver(
    private val context: Context,
    handler: Handler = Handler(Looper.getMainLooper())
) : ContentObserver(handler) {

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)

        val uriString = uri?.toString() ?: ""

        if (uriString.contains("sms") || uriString.isEmpty()) {
            Log.d("SMS_OBSERVER", "Change detected. Launching reader...")

            // ✅ FIX: Use a CoroutineScope to call the suspend function
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Give the system a moment to finish writing the SMS to the DB
                    delay(1000)

                    // Now calling the suspend function is allowed
                    InboxReader.readLatestSms(context)
                } catch (e: Exception) {
                    Log.e("SMS_OBSERVER", "Error: ${e.message}")
                }
            }
        }
    }
}