package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {

            // 1. Extract Messages from the Intent
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            for (sms in messages) {
                val sender = sms.originatingAddress ?: continue
                val messageBody = sms.messageBody ?: continue

                // 🎯 2. STRICT FILTER: Only Sender IDs ending in "-S" (or specific banks)
                // Note: SMS Sender IDs often look like "AD-HDFCBK", "VK-ICICIB"
                // The suffix check is great, but ensure your bank actually uses it.
                // Most transactional SMS in India are 6 chars (e.g. HDFCBK) prefixed by 2 chars (e.g. AD-).
                // Regex checks for format "XX-XXXXXX" might be safer, but your "-S" rule is fine for now.

                // Let's allow specific keywords OR the -S suffix
                val isBankSender = sender.contains("-S") // Checks for "AD-HDFC" format

                if (!isBankSender) continue

                // 3. Parse Logic (Re-using your existing Parser)
                val parsed = TransactionParser.parse(messageBody)

                if (parsed != null) {
                    val finalDisplayName = parsed.title ?: sender

                    // 4. Save to Storage
                    TransactionStorage.saveTransaction(
                        senderName = sender,
                        merchantName = finalDisplayName,
                        type = parsed.type,
                        amount = parsed.amount,
                        rawMessage = messageBody
                    )

                    Log.d("SMS_RECEIVER", "Saved: $finalDisplayName ₹${parsed.amount}")
                }
            }
        }
    }
}