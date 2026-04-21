package com.speedster

import android.view.accessibility.AccessibilityEvent
import android.accessibilityservice.AccessibilityService

import android.view.KeyEvent

class SpeedsterAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        event?.let { DynamicEntry.accessibilityInterceptor?.invoke(it) }
    }

    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val actionStr = if (event.action == KeyEvent.ACTION_DOWN) "DOWN" else "UP  "
        DynamicEntry.log("Key: ${event.keyCode} | Action: $actionStr | Flags: ${event.flags}")
        return DynamicEntry.keyInterceptor?.invoke(event) ?: super.onKeyEvent(event)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Bridge is hot
    }
}