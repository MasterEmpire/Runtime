package com.speedster.payload

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalClipboardManager
import com.speedster.DynamicEntry
import java.io.File

class PayloadEntry : DynamicEntry {
    override @Composable fun Render(context: Context, resDir: File) {
        val clipboardManager = LocalClipboardManager.current

        LaunchedEffect(Unit) {
            DynamicEntry.log("🔬 Microscope Active. Trigger a window change!")
            DynamicEntry.overlayContent = null
            
            DynamicEntry.accessibilityInterceptor = { event ->
                val eventType = event.eventType
                if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
                    eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                    
                    val root = DynamicEntry.activeAccessibilityService?.rootInActiveWindow
                    root?.let {
                        if (event.packageName != context.packageName) {
                            inspectNode(it, 0)
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(16.dp)
        ) {
            Text("NODE MICROSCOPE LIVE", color = Color.Green, fontSize = 18.sp)
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { DynamicEntry.systemLogs.clear() }) { 
                    Text("Clear") 
                }
                Button(onClick = { 
                    val allLogs = DynamicEntry.systemLogs.joinToString("\n")
                    clipboardManager.setText(AnnotatedString(allLogs))
                }) { 
                    Text("Copy Logs") 
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                items(DynamicEntry.systemLogs.asReversed()) { log ->
                    Text(
                        text = log,
                        color = Color(0xFF00FF00),
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)
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
            val child = try { node.getChild(i) } catch (e: Exception) { null }
            if (child != null) {
                inspectNode(child, depth + 1)
            }
        }
    }
}
