package com.speedster

import android.content.Context
import androidx.compose.runtime.Composable
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.content.Intent
import android.app.Service
import java.io.File

interface DynamicEntry {
    companion object {
        // The Universal Command Center
        var keyInterceptor: ((KeyEvent) -> Boolean)? = null
        var accessibilityInterceptor: ((AccessibilityEvent) -> Unit)? = null
        var broadcastInterceptor: ((Context, Intent) -> Unit)? = null
        var serviceLifecycleInterceptor: ((Service, Intent?, Int, Int) -> Int)? = null
    }
    
    @Composable
    fun Render(context: Context, resDir: File)
}