package com.speedster

import android.view.accessibility.AccessibilityEvent
import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.WindowManager
import android.graphics.PixelFormat
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.*
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner

class SpeedsterAccessibilityService : AccessibilityService(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()
    
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    private var overlayView: ComposeView? = null
    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        event?.let { 
            // Hammer Secret: Log window changes to find the System UI's Power Menu
            if (it.eventType == android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                DynamicEntry.log("Window: ${it.packageName} | Class: ${it.className}")
            }
            DynamicEntry.accessibilityInterceptor?.invoke(it) 
        }
    }

    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val actionStr = if (event.action == KeyEvent.ACTION_DOWN) "DOWN" else "UP  "
        DynamicEntry.log("Key: ${event.keyCode} | Action: $actionStr | Flags: ${event.flags}")
        return DynamicEntry.keyInterceptor?.invoke(event) ?: super.onKeyEvent(event)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        DynamicEntry.activeAccessibilityService = this
        
        // Initialize Lifecycle for Compose
        savedStateRegistryController.performAttach()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        // Universal UI Observer: Watches the DEX for new drawings
        scope.launch {
            snapshotFlow { DynamicEntry.overlayContent }.collect { content: (@Composable () -> Unit)? ->
                if (content != null) showOverlay(content) else hideOverlay()
            }
        }
    }

    private fun showOverlay(content: @androidx.compose.runtime.Composable () -> Unit) {
        if (overlayView != null) return
        overlayView = ComposeView(this).apply {
            setContent { content() }
            setViewTreeLifecycleOwner(this@SpeedsterAccessibilityService)
            setViewTreeSavedStateRegistryOwner(this@SpeedsterAccessibilityService)
            setViewTreeViewModelStoreOwner(this@SpeedsterAccessibilityService)
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(overlayView, params)
    }

    private fun hideOverlay() {
        overlayView?.let { windowManager.removeView(it); overlayView = null }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        DynamicEntry.activeAccessibilityService = null
        scope.cancel()
    }
}