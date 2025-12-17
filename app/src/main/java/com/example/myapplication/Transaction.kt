package com.example.myapplication

data class Transaction(
    val title: String,      // e.g., "HDFC Bank" or "Paytm"
    val type: String,       // "credit" or "debit"
    val amount: Int,
    val dateKey: String,    // "2023-10-27" (Used for grouping)
    val timeFormatted: String // "10:30 PM" (IST)
)