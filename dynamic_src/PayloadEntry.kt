package com.speedster.payload

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.speedster.DynamicEntry
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
class PayloadEntry : DynamicEntry {
    private val liveNodes = mutableStateListOf<String>()

    @Composable
    override fun Render(context: Context, resDir: File) {
        var isServiceEnabled by remember { mutableStateOf(checkAccessibility(context)) }
        val lifecycleOwner = LocalLifecycleOwner.current

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
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
                        Handler(Looper.getMainLooper()).post {
                            liveNodes.clear()
                            liveNodes.addAll(snapshot)
                        }
                    }
                }
            }
        }

        Scaffold(
            containerColor = Color(0xFF0F0F0F),
            topBar = {
                TopAppBar(
                    title = { Text("Node Sniffer", color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                if (!isServiceEnabled) {
                    PermissionBanner { 
                        val intent = android.content.Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(liveNodes.size) { index ->
                        NodeItem(text = liveNodes[index])
                    }
                }
            }
        }
    }

    private fun crawl(node: AccessibilityNodeInfo, depth: Int, list: MutableList<String>) {
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
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        return enabledServices?.contains(expectedId) == true
    }
}

@Composable
fun PermissionBanner(onAction: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE74C3C))
            .clickable { onAction() }
            .padding(16.dp)
    ) {
        Text(
            "Accessibility Disabled. Tap to fix.",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun NodeItem(text: String) {
    Text(
        text = text,
        color = Color(0xFF00FF00),
        fontSize = 12.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
