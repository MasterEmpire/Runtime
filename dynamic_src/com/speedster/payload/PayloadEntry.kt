package com.speedster.payload

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.*
import com.speedster.DynamicEntry
import java.io.File

class PayloadEntry : DynamicEntry {
    override @Composable fun Render(context: Context, resDir: File) {
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

        LaunchedEffect(Unit) {
            DynamicEntry.log("🔬 Microscope Active. Trigger a window change!")
            DynamicEntry.overlayContent = null
            
            DynamicEntry.accessibilityInterceptor = { event ->
                if (event.eventType == android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
                    event.eventType == android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                    
                    val root = DynamicEntry.activeAccessibilityService?.rootInActiveWindow
                    root?.let {
                        // We don't want to log our own app's nodes and create an infinite loop
                        if (event.packageName != context.packageName) {
                            inspectNode(it, 0)
                        }
                    }
                }
            }
        }

        androidx.compose.foundation.layout.Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color(0xFF000000))
                .padding(16.dp)
        ) {
            androidx.compose.material3.Text("NODE MICROSCOPE LIVE", color = androidx.compose.ui.graphics.Color.Green, fontSize = 18.sp)
            
            androidx.compose.foundation.layout.Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
            ) {
                androidx.compose.material3.Button(onClick = { DynamicEntry.systemLogs.clear() }) { 
                    androidx.compose.material3.Text("Clear") 
                }
                androidx.compose.material3.Button(onClick = { 
                    val allLogs = DynamicEntry.systemLogs.joinToString("\n")
                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(allLogs))
                }) { 
                    androidx.compose.material3.Text("Copy Logs") 
                }
            }

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = androidx.compose.ui.Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                items(DynamicEntry.systemLogs.asReversed()) { log ->
                    androidx.compose.material3.Text(
                        text = log,
                        color = androidx.compose.ui.graphics.Color(0xFF00FF00),
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                    androidx.compose.material3.HorizontalDivider(color = androidx.compose.ui.graphics.Color.DarkGray)
                }
            }
        }
    }

    private fun inspectNode(node: AccessibilityNodeInfo?, depth: Int) {
        if (node == null) return
        val indent = " ".repeat(depth * 2)
        
        val id = node.viewIdResourceName ?: "no-id"
        val text = node.text?.toString() ?: node.contentDescription?.toString() ?: ""
        val className = node.className?.toString()?.split(".")?.last() ?: "View"
        
        if (text.isNotBlank() || id != "no-id") {
            DynamicEntry.log("$indent[$className] ID: $id | TXT: $text")
        }

        for (i in 0 until node.childCount) {
            inspectNode(node.getChild(i), depth + 1)
        }
    }
}
