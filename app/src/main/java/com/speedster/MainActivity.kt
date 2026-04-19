package com.speedster

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var hasPayload by remember { 
                mutableStateOf(getSharedPreferences("speedster_prefs", MODE_PRIVATE).getBoolean("has_payload", false)) 
            }
            var payloadInstance by remember { mutableStateOf<DynamicEntry?>(null) }
            val context = LocalContext.current

            if (hasPayload && payloadInstance != null) {
                payloadInstance!!.Render(context)
            } else {
                LobbyScreen(onPayloadLoaded = { 
                    payloadInstance = it
                    hasPayload = true
                })
            }
        }
    }
}

@Composable
fun LobbyScreen(onPayloadLoaded: (DynamicEntry) -> Unit) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            isLoading = true
            val tempFile = File(context.cacheDir, "temp.dex")
            context.contentResolver.openInputStream(it)?.use { input ->
                FileOutputStream(tempFile).use { output -> input.copyTo(output) }
            }
            val loaded = PayloadLoader.loadPayload(context, tempFile)
            if (loaded != null) {
                context.getSharedPreferences("speedster_prefs", android.content.Context.MODE_PRIVATE)
                    .edit().putBoolean("has_payload", true).apply()
                onPayloadLoaded(loaded)
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Speedster Shell", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(20.dp))
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(onClick = { launcher.launch("*/*") }) {
                Text("Upload DEX Brain")
            }
        }
    }
}