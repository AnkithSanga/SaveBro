package com.example.myapplication

import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object TransactionStorage {

    private const val DIR_NAME = "BudgetTracker"
    private const val FILE_NAME = "transactions_full.json"

    // 🕒 Formatters
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Kolkata") }
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Kolkata") }

    private fun getFile(): File {
        val dir = File(Environment.getExternalStorageDirectory(), DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, FILE_NAME)
    }

    // ✅ SAVE FUNCTION
    fun saveTransaction(senderName: String, merchantName: String, type: String, amount: Int, rawMessage: String) {
        val file = getFile()
        val now = Date()
        val dateKey = dateFormatter.format(now)
        val timeStr = timeFormatter.format(now)

        // 🆔 Unique Hash
        val txnId = (rawMessage + amount + dateKey).hashCode().toString()

        val rootJson = if (file.exists() && file.readText().isNotEmpty()) JSONObject(file.readText()) else JSONObject()

        if (rootJson.toString().contains(txnId)) return // Deduplicate

        val dayObj = rootJson.optJSONObject(dateKey) ?: JSONObject()
        val listKey = if (type == "credit") "credits" else "debits"
        val txnArray = dayObj.optJSONArray(listKey) ?: JSONArray()

        val txnObj = JSONObject().apply {
            put("id", txnId)
            put("sender", senderName)
            put("name", merchantName)
            put("amount", amount)
            put("type", type)
            put("time", timeStr)
            put("raw_body", rawMessage)
        }

        txnArray.put(txnObj)
        dayObj.put(listKey, txnArray)
        rootJson.put(dateKey, dayObj)

        try { file.writeText(rootJson.toString(4)) } catch (e: Exception) { e.printStackTrace() }
    }

    // ✅ FIXED: Renamed to match your MainActivity call
    fun readAllForUI(): List<Transaction> {
        val file = getFile()
        if (!file.exists()) return emptyList()

        val list = mutableListOf<Transaction>()
        try {
            val rootJson = JSONObject(file.readText())
            val dates = rootJson.keys()

            while (dates.hasNext()) {
                val date = dates.next()
                val dayObj = rootJson.getJSONObject(date)

                listOf("credits", "debits").forEach { listType ->
                    val arr = dayObj.optJSONArray(listType) ?: JSONArray()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(Transaction(
                            id = obj.optString("id"),
                            title = obj.optString("name"), // Merchant Name
                            amount = obj.optInt("amount"),
                            type = obj.optString("type"),
                            dateKey = date,
                            timeFormatted = obj.optString("time"),
                            sender = obj.optString("sender"),
                            rawMessage = obj.optString("raw_body")
                        ))
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }

        // Sort: Newest Date First, then Time
        return list.sortedByDescending { it.dateKey + it.timeFormatted }
    }

    // ✅ FIXED: Renamed to match your MainActivity call
    fun readRawJson(): String {
        val file = getFile()
        return if (file.exists()) file.readText() else "{}"
    }
}