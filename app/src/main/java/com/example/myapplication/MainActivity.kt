package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Request SMS & Notification Permissions
        val permissions = mutableListOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            requestPermissions(permissions.toTypedArray(), 101)
        }

        // 2. Request File Access (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }

        // 3. START THE SERVICE (Important: Keeps the SMS Observer alive)
        val serviceIntent = Intent(this, SmsService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        setContent {
            BudgetScreen()
        }
    }
}

@Composable
fun BudgetScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State Variables
    var transactions by remember { mutableStateOf(listOf<Transaction>()) }
    var showJsonDialog by remember { mutableStateOf(false) }
    var rawJson by remember { mutableStateOf("") }
    var isSyncing by remember { mutableStateOf(false) }

    // Calc Totals
    val totalDebit = transactions.filter { it.type == "debit" }.sumOf { it.amount }
    val totalCredit = transactions.filter { it.type == "credit" }.sumOf { it.amount }

    // Group by Date
    val groupedTransactions = transactions.groupBy { it.dateKey }

    // 🔄 Auto-Refresh UI Loop (Reads JSON every 2 seconds)
    LaunchedEffect(Unit) {
        while (true) {
            transactions = TransactionStorage.readAllForUI()
            delay(2000) // Update UI every 2s
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // 🔄 SYNC BUTTON (Force Read Inbox)
                // Inside MainActivity.kt -> BudgetScreen -> Scaffold -> FloatingActionButton

                FloatingActionButton(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    onClick = {
                        scope.launch {
                            isSyncing = true
                            withContext(Dispatchers.IO) {
                                // ✅ CHANGED: Calls the new "Today Only" function
                                InboxReader.syncTodaysInbox(context)
                            }
                            // Refresh UI List
                            transactions = TransactionStorage.readAllForUI()
                            isSyncing = false
                            Toast.makeText(context, "Synced Today's Messages", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    if (isSyncing) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    else Icon(Icons.Default.Refresh, contentDescription = "Sync Today")
                }

                // 📄 JSON BUTTON (View Raw File)
                FloatingActionButton(onClick = {
                    rawJson = TransactionStorage.readRawJson()
                    showJsonDialog = true
                }) {
                    Text("JSON", modifier = Modifier.padding(16.dp))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("💰 Budget Tracker", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            // 📊 STAT CARDS
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Income", "₹$totalCredit", Color(0xFFE8F5E9), Color(0xFF2E7D32), Modifier.weight(1f))
                StatCard("Expense", "₹$totalDebit", Color(0xFFFFEBEE), Color(0xFFC62828), Modifier.weight(1f))
            }

            Spacer(Modifier.height(20.dp))
            Text("Recent Transactions", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            // 📜 LIST
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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

                if (transactions.isEmpty()) {
                    item {
                        Text(
                            "No transactions found.\nTry clicking the Sync button.",
                            modifier = Modifier.padding(20.dp),
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }

    // 🖥️ RAW JSON DIALOG
    if (showJsonDialog) {
        Dialog(onDismissRequest = { showJsonDialog = false }) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
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
    Card(colors = CardDefaults.cardColors(containerColor = bg), modifier = modifier) {
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
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Show Merchant Name, fall back to Sender if empty
                Text(
                    text = if(txn.title.isNotEmpty()) txn.title else txn.sender,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
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