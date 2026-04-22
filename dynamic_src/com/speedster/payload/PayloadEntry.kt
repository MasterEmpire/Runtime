package com.speedster.payload

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedster.DynamicEntry
import java.io.File

class PayloadEntry : DynamicEntry {
    private var currentPackage by mutableStateOf("waiting...")
    private var lastTouchRect by mutableStateOf(android.graphics.Rect())
    private var touchTimestamp by mutableLongStateOf(0L)

    override @Composable fun Render(context: Context, resDir: File) {
        LaunchedEffect(Unit) {
            DynamicEntry.log("📡 Diagnostic Mode: Touch Sniffing Active")
            
            DynamicEntry.overlayConfig = DynamicEntry.OverlayConfig(
                flags = android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                        android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                alpha = 1.0f
            )

            DynamicEntry.accessibilityInterceptor = { event ->
                // 1. Track Package Changes
                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    currentPackage = event.packageName?.toString() ?: "unknown"
                }

                // 2. Sniff Touches/Clicks
                if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED || 
                    event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
                    val node = event.source
                    if (node != null) {
                        val rect = android.graphics.Rect()
                        node.getBoundsInScreen(rect)
                        lastTouchRect = rect
                        touchTimestamp = System.currentTimeMillis()
                        DynamicEntry.log("🎯 Sniffed Touch: ${rect.centerX()}, ${rect.centerY()}")
                    }
                }
            }
            
            DynamicEntry.overlayContent = { DiagnosticUI(currentPackage, lastTouchRect, touchTimestamp) }
        }

        // The UI inside the main Speedster App Screen
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF121212)),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text("DIAGNOSTIC OVERLAY: LIVE", color = Color.Green, fontSize = 20.sp)
        }
    }

    @Composable
    fun DiagnosticUI(pkg: String, rect: android.graphics.Rect, timestamp: Long) {
        val showPing = (System.currentTimeMillis() - timestamp) < 1000
        
        Box(modifier = Modifier.fillMaxSize()) {
            // The Boundary Wireframe
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(3.dp, Color.Red.copy(alpha = 0.4f))
                    .background(Color.Green.copy(alpha = 0.05f))
            )

            // The Touch Ping (Visual Confirmation)
            if (showPing && !rect.isEmpty) {
                val density = androidx.compose.ui.platform.LocalDensity.current
                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) { rect.centerX().toDp() - 20.dp },
                            y = with(density) { rect.centerY().toDp() - 20.dp }
                        )
                        .size(40.dp)
                        .border(2.dp, Color.Cyan, androidx.compose.foundation.shape.CircleShape)
                        .background(Color.Cyan.copy(alpha = 0.3f), androidx.compose.foundation.shape.CircleShape)
                )
            }

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .align(androidx.compose.ui.Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp)
            ) {
                Text("SCOPE: DIAGNOSTIC", color = Color.White, fontSize = 10.sp)
                Text("PACKAGE: $pkg", color = Color.Yellow, fontSize = 12.sp)
                Text("LAST_X: ${if(rect.isEmpty) "-" else rect.centerX()}", color = Color.Cyan, fontSize = 10.sp)
                Text("LAST_Y: ${if(rect.isEmpty) "-" else rect.centerY()}", color = Color.Cyan, fontSize = 10.sp)
            }
        }
    }
}
