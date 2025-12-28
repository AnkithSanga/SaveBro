package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val sender = sms.originatingAddress ?: ""
                val body = sms.messageBody ?: ""

                // Strict -S Filter
                if (sender.uppercase().endsWith("-S")) {
                    val parsed = TransactionParser.parse(body)
                    if (parsed != null) {
                        // We use a Coroutine because getting location might take a second
                        CoroutineScope(Dispatchers.IO).launch {
                            val coords = LocationProvider.getCurrentLocation(context)

                            TransactionStorage.saveTransaction(
                                senderName = sender,
                                merchantName = parsed.title ?: sender,
                                type = parsed.type,
                                amount = parsed.amount,
                                rawMessage = body,
                                location = coords // New Parameter
                            )
                        }
                    }
                }
            }
        }
    }
}