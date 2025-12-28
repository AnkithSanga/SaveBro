package com.example.myapplication

object TransactionParser {
    private val amountRegex = Regex("""(?:Rs\.?:?|₹|INR)\s*([\d,]+\.?\d*)""", RegexOption.IGNORE_CASE)

    fun parse(message: String): ParsedData? {
        val text = message.lowercase()
        if (text.contains("otp") || text.contains("login") || text.contains("requested") || text.contains("fail")) return null

        val type = when {
            text.contains("credit") || text.contains("deposited") || text.contains("received") -> "credit"
            text.contains("debit") || text.contains("spent") || text.contains("sent") || text.contains("paid") || text.contains("deducted") -> "debit"
            else -> return null
        }

        val match = amountRegex.find(message) ?: return null
        val amount = match.groupValues[1].replace(",", "").toDoubleOrNull()?.toInt() ?: 0
        if (amount <= 0) return null

        val extractedName = when {
            message.contains("To ", true) -> message.substringAfter("To ", "").substringBefore("\n").substringBefore(" On ").trim()
            message.contains("At ", true) -> message.substringAfter("At ", "").substringBefore(" On ").trim()
            message.contains("from VPA ", true) -> message.substringAfter("from VPA ", "").substringBefore(" (").trim()
            else -> null
        }

        return ParsedData(extractedName, type, amount)
    }
}

data class ParsedData(val title: String?, val type: String, val amount: Int)