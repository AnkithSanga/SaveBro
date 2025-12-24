package com.example.myapplication

import android.content.Context
import android.net.Uri
import android.util.Log
import java.util.Calendar

object InboxReader {

    // ⚡ READ NEWEST (Triggered by Real-time Observer)
    // Fix: Reads last 10 messages to ensure we don't miss the new one due to sorting lag
    fun readLatestSms(context: Context) {
        val uri = Uri.parse("content://sms/inbox")
        val cursor = context.contentResolver.query(
            uri,
            arrayOf("address", "body", "date"),
            null,
            null,
            "date DESC LIMIT 10" // Check last 10 to be safe
        )

        cursor?.use {
            val addressIdx = it.getColumnIndex("address")
            val bodyIdx = it.getColumnIndex("body")
            val dateIdx = it.getColumnIndex("date")

            // We only want messages from the last 30 seconds (Real-time check)
            val thirtySecondsAgo = System.currentTimeMillis() - 30000

            while (it.moveToNext()) {
                val address = it.getString(addressIdx) ?: ""
                val body = it.getString(bodyIdx) ?: ""
                val date = it.getLong(dateIdx)

                // Skip old messages (prevent re-reading old stuff on every db change)
                if (date < thirtySecondsAgo) continue

                // 🛑 CHECK: Does it match our filter?
                if (isValidSender(address)) {
                    processMessage(address, body)
                } else {
                    Log.d("SMS_DEBUG", "Ignored Sender: $address")
                }
            }
        }
    }

    // 📅 SYNC TODAY (Triggered by Button)
    fun syncTodaysInbox(context: Context) {
        val uri = Uri.parse("content://sms/inbox")

        // 1. Get Midnight Today safely
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        Log.d("SMS_DEBUG", "Syncing messages since: $startOfDay")

        val selection = "date >= ?"
        val selectionArgs = arrayOf(startOfDay.toString())

        val cursor = context.contentResolver.query(
            uri,
            arrayOf("address", "body", "date"),
            selection,
            selectionArgs,
            "date DESC"
        )

        cursor?.use {
            Log.d("SMS_DEBUG", "Found ${it.count} messages today")
            val addressIdx = it.getColumnIndex("address")
            val bodyIdx = it.getColumnIndex("body")

            while (it.moveToNext()) {
                val address = it.getString(addressIdx) ?: ""
                val body = it.getString(bodyIdx) ?: ""

                if (isValidSender(address)) {
                    processMessage(address, body)
                }
            }
        }
    }

    // 🔍 SHARED FILTER LOGIC
    private fun isValidSender(address: String): Boolean {
        // 1. Must contain a hyphen (Standard for Banks)
        if (!address.contains("-")) return false

        // 2. YOUR RULE: Must end with -S (Case insensitive)
        // Example: AD-HDFCBK-S -> Valid
        // Example: VM-ICICIB   -> Invalid (if you strictly want -S)
        return address.trim().uppercase().endsWith("-S")
    }

    // 💾 SAVE LOGIC
    private fun processMessage(address: String, body: String) {
        val parsed = TransactionParser.parse(body)

        if (parsed != null && parsed.amount > 0) {
            TransactionStorage.saveTransaction(
                senderName = address,
                merchantName = parsed.title ?: address,
                type = parsed.type,
                amount = parsed.amount,
                rawMessage = body
            )
            Log.d("SMS_DEBUG", "✅ SAVED: ${parsed.title} ₹${parsed.amount}")
        } else {
            Log.d("SMS_DEBUG", "❌ PARSE FAILED: $body")
        }
    }
}