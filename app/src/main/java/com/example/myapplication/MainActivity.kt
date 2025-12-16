package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ask user to enable notification access
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))

        setContent {
            NotificationScreen()
        }
    }
}

@Composable
fun NotificationScreen() {
    var latestNotification by remember { mutableStateOf("") }

    // Poll store every second
    LaunchedEffect(Unit) {
        while (true) {
            latestNotification = NotificationStore.lastNotification
            delay(1000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Latest Notification", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (latestNotification.isEmpty())
                "No notification yet"
            else
                latestNotification
        )
    }
}
