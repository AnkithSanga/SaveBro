package com.example.myapplication

import android.content.Context
import android.net.Uri
import java.util.Calendar

object InboxReader {

    // ⚡ Triggered by Service Observer (New messages)
    suspend fun readLatestSms(context: Context) {
        val uri = Uri.parse("content://sms/inbox")
        val cursor = context.contentResolver.query(uri, arrayOf("address", "body", "date"), null, null, "date DESC LIMIT 5")

        cursor?.use {
            val addrIdx = it.getColumnIndexOrThrow("address")
            val bodyIdx = it.getColumnIndexOrThrow("body")
            val dateIdx = it.getColumnIndexOrThrow("date")
            val twoMinsAgo = System.currentTimeMillis() - 120000

            while (it.moveToNext()) {
                val date = it.getLong(dateIdx)
                if (date >= twoMinsAgo) {
                    processMessage(context, it.getString(addrIdx), it.getString(bodyIdx))
                }
            }
        }
    }

    // 📅 Triggered by UI Button
    suspend fun syncTodaysInbox(context: Context) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }
        val cursor = context.contentResolver.query(
            Uri.parse("content://sms/inbox"), arrayOf("address", "body"),
            "date >= ?", arrayOf(calendar.timeInMillis.toString()), "date DESC"
        )
        cursor?.use {
            while (it.moveToNext()) {
                processMessage(context, it.getString(0), it.getString(1) )
            }
        }
    }

    private suspend fun processMessage(context: Context, address: String, body: String) {
        if (address.uppercase().endsWith("-S") && !address.contains("-P")) {
            val parsed = TransactionParser.parse(body)
            if (parsed != null) {
                // ✅ Fetch location and WAIT for it
                val loc = LocationProvider.getCurrentLocation(context.applicationContext)

                TransactionStorage.saveTransaction(
                    senderName = address,
                    merchantName = parsed.title ?: address,
                    type = parsed.type,
                    amount = parsed.amount,
                    rawMessage = body,
                    location = loc
                )
            }
        }
    }
}