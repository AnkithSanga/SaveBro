package com.example.myapplication

data class Transaction(
    val id: String,
    val title: String,
    val amount: Int,
    val type: String,
    val dateKey: String,
    val timeFormatted: String,
    val sender: String,
    val rawMessage: String,
    val location: String
)