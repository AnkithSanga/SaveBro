package com.example.myapplication

data class Transaction(
    val id: String,          // Unique Hash
    val title: String,       // Merchant Name (e.g. "Netflix") or Sender (e.g. "HDFCBK")
    val amount: Int,
    val type: String,        // "credit" or "debit"
    val dateKey: String,     // "2025-12-20"
    val timeFormatted: String, // "11:45 AM"
    val sender: String,      // The Bank Sender ID (e.g., "AD-HDFCBK-S")
    val rawMessage: String   // The full SMS body
)