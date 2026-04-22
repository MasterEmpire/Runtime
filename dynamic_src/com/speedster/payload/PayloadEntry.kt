package com.speedster.payload

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedster.DynamicEntry
import java.io.File

class PayloadEntry : DynamicEntry {

    private val scanResults = mutableStateListOf<String>()

    override @Composable fun Render(context: Context, resDir: File) {
        // Hook into the accessibility stream when the UI is active
        LaunchedEffect(Unit) {
            DynamicEntry.log("🧠 Brain Initialized: Scanning UI Tree...")
            
            DynamicEntry.accessibilityInterceptor = { event ->
                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
                    event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                    
                    val root = DynamicEntry.activeAccessibilityService?.rootInActiveWindow
                    root?.let {
                        val batch = mutableListOf<String>()
                        crawl(it, 0, batch)
                        it.recycle()
                        
                        // Update UI with the latest tree snapshot
                        if (batch.isNotEmpty()) {
                            scanResults.clear()
                            scanResults.addAll(batch.take(50)) // Don't explode the RAM
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212))
                .padding(16.dp)
        ) {
            Text(
                "LIVE UI STREAM",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Green,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(scanResults) { nodeData ->
                    Text(
                        text = nodeData,
                        color = Color.Cyan,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                    Text("--- End of Current Tree ---", color = Color.Gray, fontSize = 9.sp)
                }
            }
        }
    }

    private fun crawl(node: AccessibilityNodeInfo?, depth: Int, results: MutableList<String>) {
        if (node == null) return

        val indent = "  ".repeat(depth)
        val text = node.text?.toString() ?: node.contentDescription?.toString() ?: ""
        val id = node.viewIdResourceName?.split("/")?.lastOrNull() ?: ""
        val className = node.className?.split(".")?.lastOrNull() ?: "View"

        if (text.isNotBlank() || id.isNotBlank()) {
            results.add("$indent[$className] id:$id -> \"$text\"")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            crawl(child, depth + 1, results)
            child?.recycle() 
        }
    }
}