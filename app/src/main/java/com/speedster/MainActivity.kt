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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import java.io.File
import java.io.FileOutputStream
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("speedster_prefs", MODE_PRIVATE) }
            
            // Attempt to recover payload on start
            var payloadInstance by remember {
                mutableStateOf(if (prefs.getBoolean("has_payload", false)) PayloadLoader.loadExistingPayload(context) else null)
            }

            if (payloadInstance != null) {
                payloadInstance!!.Render(context, PayloadLoader.getResDir(context))
            } else {
                LobbyScreen(onPayloadLoaded = { 
                    payloadInstance = it
                })
            }
        }
    }
}

@Composable
fun LobbyScreen(onPayloadLoaded: (DynamicEntry) -> Unit) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var remotePayloads by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    
    val supabaseUrl = "https://xvldfsmxskhemkslsbym.supabase.co"
    val anonKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inh2bGRmc214c2toZW1rc2xzYnltIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjI2ODgxNzksImV4cCI6MjA3ODI2NDE3OX0.5arqrx8Tt7v-hpXpo_ncoK4IX8th9IibxAuv93SSoOU"

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
            Button(onClick = { launcher.launch("*/*") }, modifier = Modifier.fillMaxWidth(0.8f)) {
                Text("Local Upload (Manual)")
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Button(
                onClick = {
                    isLoading = true
                    thread {
                        try {
                            val url = URL("$supabaseUrl/rest/v1/payload_versions?select=*")
                            val conn = url.openConnection() as HttpURLConnection
                            conn.setRequestProperty("apikey", anonKey)
                            conn.setRequestProperty("Authorization", "Bearer $anonKey")
                            
                            val response = conn.inputStream.bufferedReader().readText()
                            val json = JSONArray(response)
                            val list = mutableListOf<Pair<String, String>>()
                            for (i in 0 until json.length()) {
                                val item = json.getJSONObject(i)
                                list.add(item.getString("name") to item.getString("file_path"))
                            }
                            remotePayloads = list
                        } catch (e: Exception) { e.printStackTrace() }
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(0.8f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Fetch Cloud Brains")
            }

            remotePayloads.forEach { payload ->
                TextButton(onClick = {
                    isLoading = true
                    val downloadUrl = "$supabaseUrl/storage/v1/object/public/payloads/${payload.second}"
                    PayloadLoader.downloadAndLoad(context, downloadUrl) { loaded ->
                        if (loaded != null) {
                            context.getSharedPreferences("speedster_prefs", android.content.Context.MODE_PRIVATE)
                                .edit().putBoolean("has_payload", true).apply()
                            onPayloadLoaded(loaded)
                        }
                        isLoading = false
                    }
                }) {
                    Text("🚀 Inject ${payload.first}")
                }
            }


        }
    }
}