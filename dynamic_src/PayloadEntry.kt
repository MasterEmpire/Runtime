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
import androidx.compose.foundation.lazy.items
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
        var status by mutableStateOf("Idle")
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
        val sysLogs = DynamicEntry.systemLogs

        // 1. Service Monitor Log
        LaunchedEffect(Unit) {
            DynamicEntry.log("🚀 PayloadEntry: Render cycle started")
            while(true) {
                val currentState = DynamicEntry.activeAccessibilityService != null
                if (currentState != isServiceActive) {
                    isServiceActive = currentState
                    DynamicEntry.log("⚠️ Service Status Changed: Active = $isServiceActive")
                }
                delay(2000)
            }
        }

        // 2. Interceptor Setup Log
        if (!isSnifferBooted) {
            isSnifferBooted = true
            DynamicEntry.log("🛡️ Initializing Accessibility Interceptor...")
            DynamicEntry.accessibilityInterceptor = { event ->
                val type = AccessibilityEvent.eventTypeToString(event.eventType)
                val pkg = event.packageName?.toString() ?: "unknown"
                
                eventCount++
                if (pkg != "com.speedster" && pkg != "com.android.systemui") {
                    lastPkg = pkg
                }

                val root = DynamicEntry.activeAccessibilityService?.rootInActiveWindow
                if (root == null) {
                    status = "Last: $type | Root: NULL"
                } else {
                    status = "Last: $type | Root: OK"
                    val snapshot = mutableListOf<String>()
                    crawl(root, 0, snapshot)
                    root.recycle()
                    
                    Handler(Looper.getMainLooper()).post {
                        liveNodes.clear()
                        liveNodes.addAll(snapshot)
                        if (eventCount % 10 == 0) {
                             DynamicEntry.log("📊 Captured ${snapshot.size} nodes from $lastPkg")
                        }
                    }
                }
            }
            DynamicEntry.log("✅ Interceptor Hooked Successfully")
        }

        // 3. Overlay Lifecycle Log
        LaunchedEffect(isServiceActive) {
            if (isServiceActive) {
                DynamicEntry.log("📺 Deploying Global Ghost HUD")
                DynamicEntry.overlayContent = {
                    FloatingHUD(eventCount, lastPkg, liveNodes.take(10))
                }
            } else {
                DynamicEntry.log("🚫 Revoking Global HUD (Service Inactive)")
                DynamicEntry.overlayContent = null
            }
        }

        Scaffold(
            containerColor = Color(0xFF0A0A0A),
            topBar = {
                TopAppBar(
                    title = { Text("Cortex Sniffer + Logs", color = Color.White, fontSize = 16.sp) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                
                // Live Stats Dashboard
                Row(modifier = Modifier.fillMaxWidth().background(Color(0xFF151515)).padding(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PKG: $lastPkg", color = Color.Yellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("EVT: $eventCount | STATUS: $status", color = Color.Gray, fontSize = 10.sp)
                    }
                }

                // Node Tree (Middle)
                Box(modifier = Modifier.weight(0.6f).fillMaxWidth()) {
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                        item { Text("--- UI TREE SNAPSHOT ---", color = Color.DarkGray, fontSize = 10.sp, modifier = Modifier.padding(vertical = 4.dp)) }
                        items(liveNodes.size) { index ->
                            Text(liveNodes[index], color = Color(0xFF00FF00), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Divider(color = Color.DarkGray)

                // System Logs (Bottom)
                Column(modifier = Modifier.weight(0.4f).fillMaxWidth().background(Color.Black)) {
                    Text(" SYSTEM LOGS", color = Color.Magenta, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                        items(sysLogs.reversed()) { log ->
                            Text("> $log", color = Color.LightGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(vertical = 1.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingHUD(events: Int, pkg: String, nodes: List<String>) {
    Box(modifier = Modifier.fillMaxSize().padding(top = 50.dp, end = 10.dp), contentAlignment = Alignment.TopEnd) {
        Column(modifier = Modifier.background(Color(0xCC000000), shape = RoundedCornerShape(4.dp)).padding(6.dp).width(180.dp)) {
            Text("GHOST: $pkg", color = Color.Green, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text("Events: $events", color = Color.White, fontSize = 8.sp)
            nodes.forEach { Text(it.trim(), color = Color.Cyan, fontSize = 7.sp, maxLines = 1) }
        }
    }
}
