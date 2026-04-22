package com.speedster

import android.view.accessibility.AccessibilityEvent
import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.WindowManager
import android.graphics.PixelFormat
import android.util.Log
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
import androidx.compose.ui.platform.ViewCompositionStrategy

class SpeedsterAccessibilityService : AccessibilityService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var overlayView: ComposeView? = null
    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        Log.d("Speedster", "Service onCreate")
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let { DynamicEntry.accessibilityInterceptor?.invoke(it) }
    }

    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        return DynamicEntry.keyInterceptor?.invoke(event) ?: super.onKeyEvent(event)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("Speedster", "Service Connected")
        DynamicEntry.activeAccessibilityService = this
        
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        scope.launch {
            snapshotFlow { DynamicEntry.overlayContent }.collect { content ->
                if (content != null) showOverlay(content) else hideOverlay()
            }
        }
    }

    private fun showOverlay(content: @Composable () -> Unit) {
        if (overlayView != null) return
        Log.d("Speedster", "Showing Overlay")
        
        val themeContext = android.view.ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault_NoActionBar)
        overlayView = ComposeView(themeContext).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            
            // Set owners BEFORE setContent
            setViewTreeLifecycleOwner(this@SpeedsterAccessibilityService)
            setViewTreeViewModelStoreOwner(this@SpeedsterAccessibilityService)
            setViewTreeSavedStateRegistryOwner(this@SpeedsterAccessibilityService)
            
            setContent { content() }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        try {
            windowManager.addView(overlayView, params)
        } catch (e: Exception) {
            Log.e("Speedster", "WindowManager addView FAILED: ${e.message}")
        }
    }

    private fun hideOverlay() {
        overlayView?.let { 
            Log.d("Speedster", "Hiding Overlay")
            try {
                windowManager.removeView(it)
            } catch (e: Exception) { }
            overlayView = null 
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        DynamicEntry.activeAccessibilityService = null
        scope.cancel()
    }
}