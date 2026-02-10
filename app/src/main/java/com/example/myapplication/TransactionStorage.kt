package com.example.myapplication

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object TransactionStorage {
    private const val DIR_NAME = "BudgetTracker"
    private const val FILE_NAME = "transactions_final.json"

    // Timezone fixed to India
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Kolkata") }
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Kolkata") }

    /**
     * ✅ UPDATED: Uses App-Specific Storage
     * Does NOT require MANAGE_EXTERNAL_STORAGE permission.
     * Path: /Android/data/com.example.myapplication/files/BudgetTracker/
     */
    private fun getFile(context: Context): File {
        // getExternalFilesDir(null) gives the private app folder on storage
        val dir = File(context.getExternalFilesDir(null), DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, FILE_NAME)
    }

    // ✅ Added 'context' parameter
    fun saveTransaction(context: Context, senderName: String, merchantName: String, type: String, amount: Int, rawMessage: String, location: String = "Unknown") {
        val file = getFile(context)
        val dateKey = dateFormatter.format(Date())

        // Generate a unique ID based on content to prevent duplicates
        val txnId = (rawMessage + amount + dateKey).hashCode().toString()

        val rootJson = if (file.exists() && file.readText().isNotEmpty()) JSONObject(file.readText()) else JSONObject()

        // 🛑 Deduplication Check
        if (rootJson.toString().contains(txnId)) return

        val dayObj = rootJson.optJSONObject(dateKey) ?: JSONObject()
        val listKey = if (type == "credit") "credits" else "debits"
        val txnArray = dayObj.optJSONArray(listKey) ?: JSONArray()

        txnArray.put(JSONObject().apply {
            put("id", txnId)
            put("sender", senderName)
            put("name", merchantName)
            put("amount", amount)
            put("type", type)
            put("time", timeFormatter.format(Date()))
            put("raw_body", rawMessage)
            put("location", location)
        })

        dayObj.put(listKey, txnArray)
        rootJson.put(dateKey, dayObj)

        // Write back to file
        file.writeText(rootJson.toString(4))
    }

    // ✅ Added 'context' parameter
    fun readAllForUI(context: Context): List<Transaction> {
        val file = getFile(context)
        if (!file.exists()) return emptyList()

        val list = mutableListOf<Transaction>()
        try {
            val rootJson = JSONObject(file.readText())
            rootJson.keys().forEach { date ->
                val day = rootJson.getJSONObject(date)
                listOf("credits", "debits").forEach { key ->
                    val arr = day.optJSONArray(key) ?: JSONArray()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)

                        list.add(Transaction(
                            id = obj.getString("id"),
                            // Logic: Show Sender Name for Credits (Salary/UPI), Merchant Name for Debits
                            title = if (obj.getString("type") == "credit") obj.getString("sender") else obj.optString("name", obj.getString("sender")),
                            amount = obj.getInt("amount"),
                            type = obj.getString("type"),
                            dateKey = date,
                            timeFormatted = obj.getString("time"),
                            sender = obj.getString("sender"),
                            rawMessage = obj.getString("raw_body"),
                            location = obj.optString("location", "Unknown")
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Sort by Date + Time (Newest First)
        return list.sortedByDescending { it.dateKey + it.timeFormatted }
    }

    // ✅ Added 'context' parameter
    fun readRawJson(context: Context): String {
        val file = getFile(context)
        return if (file.exists()) file.readText() else "{}"
    }
}