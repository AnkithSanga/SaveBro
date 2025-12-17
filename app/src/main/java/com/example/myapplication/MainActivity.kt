package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Permissions
        if (!Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }
        if (!Settings.Secure.getString(contentResolver, "enabled_notification_listeners").contains(packageName)) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        setContent {
            BudgetScreen()
        }
    }
}

@Composable
fun BudgetScreen() {
    var transactions by remember { mutableStateOf(listOf<Transaction>()) }
    var showJsonDialog by remember { mutableStateOf(false) }
    var rawJson by remember { mutableStateOf("") }

    // Logic to calculate totals
    val totalDebit = transactions.filter { it.type == "debit" }.sumOf { it.amount }
    val totalCredit = transactions.filter { it.type == "credit" }.sumOf { it.amount }

    // Grouping for the UI List
    val groupedTransactions = transactions.groupBy { it.dateKey }

    // Auto-refresh data loop
    LaunchedEffect(Unit) {
        while (true) {
            transactions = TransactionStorage.readAllForUI()
            kotlinx.coroutines.delay(2000)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                rawJson = TransactionStorage.readRawJson()
                showJsonDialog = true
            }) {
                Text("JSON", modifier = Modifier.padding(16.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("💰 Wallet Watch", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            // 📊 Dashboard Cards
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Income", "₹$totalCredit", Color(0xFFE8F5E9), Color(0xFF2E7D32), Modifier.weight(1f))
                StatCard("Expense", "₹$totalDebit", Color(0xFFFFEBEE), Color(0xFFC62828), Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))
            Text("Recent Transactions", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            // 📜 Transaction List Grouped by Date
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                groupedTransactions.forEach { (date, txns) ->
                    item {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }
                    items(txns) { txn ->
                        TransactionRow(txn)
                    }
                }
            }
        }
    }

    // 🖥️ Raw JSON Dialog
    if (showJsonDialog) {
        Dialog(onDismissRequest = { showJsonDialog = false }) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth().height(400.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Raw JSON Data", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        Text(rawJson, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                    Button(onClick = { showJsonDialog = false }, modifier = Modifier.align(Alignment.End)) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, bg: Color, textCol: Color, modifier: Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = bg),
        modifier = modifier
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, color = textCol)
            Text(value, style = MaterialTheme.typography.headlineSmall, color = textCol, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TransactionRow(txn: Transaction) {
    val isCredit = txn.type == "credit"
    val color = if (isCredit) Color(0xFF2E7D32) else Color(0xFFC62828)
    val icon = if (isCredit) "+" else "-"

    Card(
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(txn.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(txn.timeFormatted, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Text(
                text = "$icon ₹${txn.amount}",
                color = color,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}