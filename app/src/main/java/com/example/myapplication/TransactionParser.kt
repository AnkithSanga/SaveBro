package com.example.myapplication

object TransactionParser {

    private val amountRegex = Regex("""(?:Rs\.?|₹|INR)\s?([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)

    fun parse(message: String): ParsedData? {
        val text = message.lowercase()

        // 🛑 Filter: Ignore non-financial messages
        if (text.contains("requested") || text.contains("successfully created") ||
            text.contains("declined") || text.contains("login alert")) return null

        // 1. Detect Type
        val type = when {
            text.contains("credit") || text.contains("credited") -> "credit"
            text.contains("debit") || text.contains("sent rs") || text.contains("spent rs") || text.contains("deducted") || text.contains("paid") -> "debit"
            else -> return null
        }

        // 2. Extract Amount
        val match = amountRegex.find(message) ?: return null
        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull()?.toInt() ?: 0

        // 3. Extract Merchant/Name (Generic Logic)
        // Looks for "To X", "At X", "From X", "towards X"
        val extractedName = when {
            message.contains("To ", true) -> message.substringAfter("To ", "to ").substringBefore("\n").substringBefore(" On ").trim()
            message.contains("At ", true) -> message.substringAfter("At ", "at ").substringBefore(" On ").trim()
            message.contains("from VPA ", true) -> message.substringAfter("from VPA ").substringBefore(" (").trim()
            message.contains("towards ", true) -> message.substringAfter("towards ").substringBefore(" from ").trim()
            message.contains("credited to", true) -> null // Usually "credited to YOUR account", so no merchant name
            else -> null
        }

        return ParsedData(extractedName, type, amount)
    }
}

data class ParsedData(val title: String?, val type: String, val amount: Int)