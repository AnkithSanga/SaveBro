package com.example.myapplication

object TransactionParser {

    // Regex Explanation:
    // (?:Rs\.?|₹|INR) -> Matches "Rs", "Rs.", "₹", or "INR"
    // \s* -> Matches optional whitespace (0 or more spaces)
    // ([\d,]+\.?\d*)  -> Capture Group 1: Matches numbers like "1,200.50" or "500"
    private val amountRegex = Regex("""(?:Rs\.?:?|₹|INR)\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)

    fun parse(message: String): ParsedData? {
        val text = message.lowercase()

        // 1. Type Detection
        val type = when {
            text.contains("credit") || text.contains("deposited") || text.contains("added") || text.contains("received") -> "credit"
            text.contains("debit") || text.contains("debited")|| text.contains("spent") || text.contains("sent") || text.contains("paid") || text.contains("deducted") -> "debit"
            else -> return null
        }

        // 2. Amount Extraction
        val match = amountRegex.find(message) ?: return null
        val amountStr = match.groupValues[1].replace(",", "")

        // Handle cases where regex matches a lone "."
        val amount = amountStr.toDoubleOrNull()?.toInt() ?: 0
        if (amount <= 0) return null

        // 3. Name Extraction
        val extractedName = when {
            message.contains("To ", true) -> message.substringAfter("To ", "to ").substringBefore("\n").substringBefore(" On ").trim()
            message.contains("At ", true) -> message.substringAfter("At ", "at ").substringBefore(" On ").trim()
            message.contains("from VPA ", true) -> message.substringAfter("from VPA ").substringBefore(" (").trim()
            else -> null
        }

        return ParsedData(extractedName, type, amount)
    }
}

data class ParsedData(val title: String?, val type: String, val amount: Int)