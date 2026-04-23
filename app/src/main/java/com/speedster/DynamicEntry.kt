package com.speedster

import android.content.Context
import androidx.compose.runtime.Composable
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.content.Intent
import android.app.Service
import java.io.File
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

interface DynamicEntry {
    companion object {
        val systemLogs = mutableStateListOf<String>()
        fun log(msg: String) {
            if (systemLogs.size > 100) systemLogs.removeAt(0)
            systemLogs.add(msg)
        }

        // The Universal Command Center
        var keyInterceptor: ((KeyEvent) -> Boolean)? = null
        var accessibilityInterceptor: ((AccessibilityEvent) -> Unit)? = null
        var broadcastInterceptor: ((Context, Intent) -> Unit)? = null
        var serviceLifecycleInterceptor: ((Service, Intent?, Int, Int) -> Int)? = null

        // System Bridge Instances (Outbound God-Mode)
        var activeAccessibilityService: android.accessibilityservice.AccessibilityService? = null
        var activeBackgroundService: Service? = null

        // The Universal UI Slot: Drop any Composable here to draw over the system
        var overlayContent by androidx.compose.runtime.mutableStateOf<(@androidx.compose.runtime.Composable () -> Unit)?>(null)
        
        // Remote control for the overlay's physical properties
        var overlayConfig by androidx.compose.runtime.mutableStateOf(OverlayConfig())
    }

    data class OverlayConfig(
        val type: Int = android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        val flags: Int = android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        val alpha: Float = 1.0f,
        val gravity: Int = android.view.Gravity.CENTER,
        val width: Int = android.view.WindowManager.LayoutParams.MATCH_PARENT,
        val height: Int = android.view.WindowManager.LayoutParams.MATCH_PARENT,
        val x: Int = 0,
        val y: Int = 0
    )
    
    @Composable
    fun Render(context: Context, resDir: File)
}