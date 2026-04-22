package com.speedster.payload

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.runtime.*
import com.speedster.DynamicEntry
import java.io.File

class PayloadEntry : DynamicEntry {
    override @Composable fun Render(context: Context, resDir: File) {
        LaunchedEffect(Unit) {
            DynamicEntry.log("🔬 Microscope Brain Initialized")
            
            // Clear any old overlays
            DynamicEntry.overlayContent = null
            
            DynamicEntry.accessibilityInterceptor = { event ->
                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
                    event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                    
                    val root = DynamicEntry.activeAccessibilityService?.rootInActiveWindow
                    root?.let {
                        DynamicEntry.log("--- Window Update: ${event.packageName} ---")
                        inspectNode(it, 0)
                    }
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
