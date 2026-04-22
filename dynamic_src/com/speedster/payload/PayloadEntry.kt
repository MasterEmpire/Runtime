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

    override @Composable fun Render(context: Context, resDir: File) {
        // Initialize the Ghost Window
        LaunchedEffect(Unit) {
            DynamicEntry.log("📡 Diagnostic Mode: Wireframe Active")
            
            DynamicEntry.overlayConfig = DynamicEntry.OverlayConfig(
                flags = android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                        android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                alpha = 1.0f
            )

            DynamicEntry.accessibilityInterceptor = { event ->
                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    currentPackage = event.packageName?.toString() ?: "unknown"
                }
            }
            
            // Keep the overlay content set
            DynamicEntry.overlayContent = { DiagnosticUI(currentPackage) }
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
    fun DiagnosticUI(pkg: String) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(3.dp, Color.Red.copy(alpha = 0.4f)) // Visual Boundary
                .background(Color.Green.copy(alpha = 0.05f)) // Visual Area
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .align(androidx.compose.ui.Alignment.BottomStart)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp)
            ) {
                Text("SCOPE: SYSTEM_OVERLAY", color = Color.White, fontSize = 10.sp)
                Text("PACKAGE: $pkg", color = Color.Yellow, fontSize = 12.sp)
                Text("TOUCH: PASS-THROUGH", color = Color.Cyan, fontSize = 10.sp)
            }
        }
    }
}
