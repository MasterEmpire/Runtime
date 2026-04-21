package com.speedster.payload

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.speedster.DynamicEntry
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File

import android.view.KeyEvent
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import android.view.accessibility.AccessibilityEvent

class PayloadEntry : DynamicEntry {
    private val liveNodes = mutableStateListOf<String>()

    @Composable
    override fun Render(context: Context, resDir: File) {
        var isServiceEnabled by remember { mutableStateOf(checkAccessibility(context)) }
        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

        DisposableEffect(lifecycleOwner) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    isServiceEnabled = checkAccessibility(context)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        LaunchedEffect(Unit) {
            DynamicEntry.accessibilityInterceptor = { event ->
                val root = try { event.source } catch (e: Exception) { null }
                if (root != null) {
                    val snapshot = mutableListOf<String>()
                    crawl(root, 0, snapshot)
                    root.recycle()
                    
                    if (snapshot.isNotEmpty()) {
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            liveNodes.clear()
                            liveNodes.addAll(snapshot)
                        }
                    }
                }
            }
        }

        androidx.compose.material3.Scaffold(
            containerColor = androidx.compose.ui.graphics.Color(0xFF0F0F0F),
            topBar = {
                androidx.compose.material3.SmallTopAppBar(
                    title = { androidx.compose.material3.Text("Node Sniffer", color = androidx.compose.ui.graphics.Color.White) },
                    colors = androidx.compose.material3.TopAppBarDefaults.smallTopAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Black)
                )
            }
        ) { padding ->
            androidx.compose.foundation.layout.Column(
                modifier = androidx.compose.foundation.layout.Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                if (!isServiceEnabled) {
                    PermissionBanner { 
                        val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                }

                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = androidx.compose.foundation.layout.Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                ) {
                    items(liveNodes.size) { index ->
                        NodeItem(text = liveNodes[index])
                    }
                }
            }
        }
    }

    private fun crawl(node: android.view.accessibility.AccessibilityNodeInfo, depth: Int, list: MutableList<String>) {
        if (depth > 10 || list.size > 50) return
        
        val id = node.viewIdResourceName ?: ""
        val txt = node.text?.toString() ?: node.contentDescription?.toString() ?: ""
        val cls = node.className?.toString()?.split(".")?.last() ?: "View"
        
        if (txt.isNotBlank() || id.isNotBlank()) {
            val indent = "  ".repeat(depth)
            list.add("$indent[$cls] ${if(id.isNotEmpty()) "#$id" else ""} -> \"$txt\"")
        }

        for (i in 0 until node.childCount) {
            val child = try { node.getChild(i) } catch (e: Exception) { null }
            if (child != null) {
                crawl(child, depth + 1, list)
                child.recycle()
            }
        }
    }

    private fun checkAccessibility(context: Context): Boolean {
        val expectedId = "${context.packageName}/com.speedster.SpeedsterAccessibilityService"
        val enabledServices = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServices?.contains(expectedId) == true
    }
}

@Composable
fun PermissionBanner(onAction: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.foundation.layout.Modifier
            .fillMaxWidth()
            .androidx.compose.foundation.background(androidx.compose.ui.graphics.Color(0xFFE74C3C))
            .androidx.compose.foundation.clickable { onAction() }
            .padding(16.dp)
    ) {
        androidx.compose.material3.Text(
            "Accessibility Disabled. Tap to fix.",
            color = androidx.compose.ui.graphics.Color.White,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = androidx.compose.ui.layout.Modifier.align(androidx.compose.ui.graphics.Alignment.Center)
        )
    }
}

@Composable
fun NodeItem(text: String) {
    androidx.compose.material3.Text(
        text = text,
        color = androidx.compose.ui.graphics.Color(0xFF00FF00),
        fontSize = 12.sp,
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        modifier = androidx.compose.ui.layout.Modifier.padding(vertical = 2.dp)
    )
}