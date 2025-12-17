package com.example.myapplication

object TransactionParser {

    // Regex to handle "Rs.140.00", "Rs 140", or "₹140"
    private val amountRegex = Regex("""(?:Rs\.?|₹)\s?([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)

    fun parse(message: String): ParsedData? {
        val text = message.lowercase()

        // 🛑 FILTER: Ignore mandate creations, requests, or login alerts
        if (text.contains("requested") || text.contains("successfully created") || text.contains("login alert")) {
            return null
        }

        // 1. Detect Type
        val type = when {
            text.contains("credit alert") || text.contains("credited to") -> "credit"
            text.contains("sent rs") || text.contains("spent rs") || text.contains("will be deducted") -> "debit"
            else -> return null
        }

        // 2. Extract Amount
        val match = amountRegex.find(message) ?: return null
        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull()?.toInt() ?: 0

        // 3. Extract Merchant/Recipient Name
        // Logic: For "To [Name]", extract [Name]. For "from VPA [Name]", extract [Name].
        val name = when {
            text.contains("to ") -> message.substringAfter("To ").substringBefore("\n").substringBefore("On ").trim()
            text.contains("at ") -> message.substringAfter("At ").substringBefore(" On ").trim()
            text.contains("from vpa ") -> message.substringAfter("from VPA ").substringBefore(" (").trim()
            else -> "HDFC Transaction"
        }

        return ParsedData(name, type, amount)
    }
}

data class ParsedData(val title: String, val type: String, val amount: Int)