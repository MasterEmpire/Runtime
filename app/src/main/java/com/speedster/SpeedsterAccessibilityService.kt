package com.speedster

import android.view.accessibility.AccessibilityEvent
import android.accessibilityservice.AccessibilityService

import android.view.KeyEvent

class SpeedsterAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        return DynamicEntry.keyInterceptor?.invoke(event) ?: super.onKeyEvent(event)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
    }
}