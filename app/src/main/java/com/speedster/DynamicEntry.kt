package com.speedster

import android.content.Context
import androidx.compose.runtime.Composable

/**
 * The Secret Handshake.
 * Your dynamic payload MUST implement this as 'com.speedster.payload.PayloadEntry'
 */
import java.io.File

import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.content.Intent
import android.app.Service
import java.io.File

interface DynamicEntry {
    companion object {
        // The Command Center
        var keyInterceptor: ((KeyEvent) -> Boolean)? = null
        var accessibilityInterceptor: ((AccessibilityEvent) -> Unit)? = null
        var broadcastInterceptor: ((Context, Intent) -> Unit)? = null
        var serviceLifecycleInterceptor: ((Service, Intent?, Int, Int) -> Int)? = null
    }
    @Composable
    fun Render(context: Context, resDir: File)
}