package com.example.myapplication

import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object TransactionStorage {
    private const val DIR_NAME = "BudgetTracker"
    private const val FILE_NAME = "transactions_final.json"
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Kolkata") }
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Kolkata") }

    private fun getFile(): File {
        val dir = File(Environment.getExternalStorageDirectory(), DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, FILE_NAME)
    }

    fun saveTransaction(senderName: String, merchantName: String, type: String, amount: Int, rawMessage: String, location: String = "Unknown") {
        val file = getFile()
        val dateKey = dateFormatter.format(Date())
        val txnId = (rawMessage + amount + dateKey).hashCode().toString()
        val rootJson = if (file.exists() && file.readText().isNotEmpty()) JSONObject(file.readText()) else JSONObject()

        if (rootJson.toString().contains(txnId)) return

        val dayObj = rootJson.optJSONObject(dateKey) ?: JSONObject()
        val listKey = if (type == "credit") "credits" else "debits"
        val txnArray = dayObj.optJSONArray(listKey) ?: JSONArray()

        txnArray.put(JSONObject().apply {
            put("id", txnId); put("sender", senderName); put("name", merchantName)
            put("amount", amount); put("type", type); put("time", timeFormatter.format(Date()))
            put("raw_body", rawMessage); put("location", location)
        })
        dayObj.put(listKey, txnArray)
        rootJson.put(dateKey, dayObj)
        file.writeText(rootJson.toString(4))
    }

    fun readAllForUI(): List<Transaction> {
        val file = getFile()
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
                            obj.getString("id"),
                            if (obj.getString("type") == "credit") obj.getString("sender") else obj.optString("name", obj.getString("sender")),
                            obj.getInt("amount"), obj.getString("type"), date,
                            obj.getString("time"), obj.getString("sender"),
                            obj.getString("raw_body"), obj.optString("location", "Unknown")
                        ))
                    }
                }
            }
        } catch (e: Exception) {}
        return list.sortedByDescending { it.dateKey + it.timeFormatted }
    }

    fun readRawJson(): String = if (getFile().exists()) getFile().readText() else "{}"
}