package com.speedster

import android.view.accessibility.AccessibilityEvent
import android.accessibilityservice.AccessibilityService

import android.view.KeyEvent

class SpeedsterAccessibilityService : AccessibilityService() {
    companion object {
        var keyInterceptor: ((KeyEvent) -> Boolean)? = null
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        return keyInterceptor?.invoke(event) ?: super.onKeyEvent(event)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
    }
}