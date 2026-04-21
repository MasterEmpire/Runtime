package com.speedster

import android.content.Context
import androidx.compose.runtime.Composable
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.content.Intent
import android.app.Service
import java.io.File
import androidx.compose.runtime.mutableStateListOf

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
    }
    
    @Composable
    fun Render(context: Context, resDir: File)
}