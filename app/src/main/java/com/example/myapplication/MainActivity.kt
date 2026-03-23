package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            startService(Intent(this, SmsService::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
        setContent { BudgetScreen() }
    }

    private fun checkPermissions() {
        val ps = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val allGranted = ps.all { ContextCompat.checkSelfPermission(this, it) == android.content.pm.PackageManager.PERMISSION_GRANTED }
        
        if (!allGranted) {
            permissionLauncher.launch(ps)
        } else {
            startService(Intent(this, SmsService::class.java))
        }

        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")))
        }
    }
}

@Composable
fun BudgetScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var transactions by remember { mutableStateOf(listOf<Transaction>()) }

    LaunchedEffect(Unit) { while(true) { transactions = TransactionStorage.readAllForUI(context); delay(2000) } }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { scope.launch { withContext(Dispatchers.IO) { InboxReader.syncTodaysInbox(context) } } }) {
                Icon(Icons.Default.Refresh, "Sync")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("💰 Budget Tracker", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(transactions) { TransactionRow(it) }
            }
        }
    }
}

@Composable
fun TransactionRow(txn: Transaction) {
    val context = LocalContext.current
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(txn.title, fontWeight = FontWeight.Bold)
                Row {
                    Text(txn.timeFormatted, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    if (txn.location != "Unknown") {
                        Text("  |  ", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                        Text("View Map 📍", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${txn.location}(${txn.title})")).setPackage("com.google.android.apps.maps"))
                            }
                        )
                    }
                }
            }
            Text("₹${txn.amount}", color = if(txn.type == "credit") Color(0xFF2E7D32) else Color(0xFFC62828), fontWeight = FontWeight.Bold)
        }
    }
}