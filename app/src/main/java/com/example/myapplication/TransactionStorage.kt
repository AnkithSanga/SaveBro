package com.example.myapplication

import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object TransactionStorage {

    private const val DIR_NAME = "transaction-storage"
    private const val FILE_NAME = "budget_data_nested.json"

    private fun getFile(): File {
        val baseDir = Environment.getExternalStorageDirectory()
        val dir = File(baseDir, DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, FILE_NAME)
    }

    // 🕒 Helpers for formatting
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    }
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    }

    // ✅ SAVE: Writes to the specific Day -> Type -> Array
    fun saveTransaction(title: String, type: String, amount: Int) {
        val file = getFile()
        val now = Date()
        val dateKey = dateFormat.format(now)
        val timeStr = timeFormat.format(now)

        // 1. Load Root JSON
        val rootJson = if (file.exists()) JSONObject(file.readText()) else JSONObject()

        // 2. Get or Create Day Object
        val dayObj = rootJson.optJSONObject(dateKey) ?: JSONObject()

        // 3. Get or Create List (credits or debits)
        val listKey = if (type == "credit") "credits" else "debits"
        val txnArray = dayObj.optJSONArray(listKey) ?: JSONArray()

        // 4. Create Transaction Object
        val txnObj = JSONObject().apply {
            put("name", title)
            put("amount", amount)
            put("time", timeStr)
        }

        // 5. Append and Save
        txnArray.put(txnObj)
        dayObj.put(listKey, txnArray)
        rootJson.put(dateKey, dayObj)

        try {
            file.writeText(rootJson.toString(4)) // Indent for readability
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ✅ READ RAW JSON (For your "View JSON" button)
    fun readRawJson(): String {
        val file = getFile()
        return if (file.exists()) file.readText() else "{}"
    }

    // ✅ READ PARSED (For the UI List)
    // Flattens the nested JSON back into a list so the UI can iterate easily
    fun readAllForUI(): List<Transaction> {
        val file = getFile()
        if (!file.exists()) return emptyList()

        val list = mutableListOf<Transaction>()
        val rootJson = JSONObject(file.readText())

        // Iterate Dates
        for (dateKey in rootJson.keys()) {
            val dayObj = rootJson.getJSONObject(dateKey)

            // Parse Credits
            parseList(dateKey, "credit", dayObj.optJSONArray("credits"), list)
            // Parse Debits
            parseList(dateKey, "debit", dayObj.optJSONArray("debits"), list)
        }

        // Sort by Date (newest first)
        return list.sortedByDescending { it.dateKey }
    }

    private fun parseList(date: String, type: String, jsonArray: JSONArray?, list: MutableList<Transaction>) {
        if (jsonArray == null) return
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            list.add(
                Transaction(
                    title = obj.getString("name"),
                    type = type,
                    amount = obj.getInt("amount"),
                    dateKey = date,
                    timeFormatted = obj.getString("time")
                )
            )
        }
    }
}