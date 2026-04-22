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

    private var volumeUpPressed = false
    private var volumeDownPressed = false

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Panic Button Logic: Vol Up + Vol Down = Nuke Overlay
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) volumeUpPressed = (event.action == KeyEvent.ACTION_DOWN)
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) volumeDownPressed = (event.action == KeyEvent.ACTION_DOWN)
        
        if (volumeUpPressed && volumeDownPressed) {
            DynamicEntry.overlayContent = null
            DynamicEntry.log("⚠️ PANIC TRIGGERED: Overlay Cleared")
            return true
        }

        return DynamicEntry.keyInterceptor?.invoke(event) ?: super.onKeyEvent(event)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("Speedster", "Service Connected")
        DynamicEntry.activeAccessibilityService = this
        
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        scope.launch {
            // Watch for both content changes AND touch property changes
            combine(
                snapshotFlow { DynamicEntry.overlayContent },
                snapshotFlow { DynamicEntry.isOverlayTouchable },
                ::Pair
            ).collect { (content, touchable) ->
                if (content != null) updateOverlay(content, touchable) else hideOverlay()
            }
        }
    }

    // Helper for the combine function
    private fun <T1, T2, R> combine(f1: kotlinx.coroutines.flow.Flow<T1>, f2: kotlinx.coroutines.flow.Flow<T2>, transform: (T1, T2) -> R): kotlinx.coroutines.flow.Flow<R> = 
        kotlinx.coroutines.flow.combine(f1, f2, transform)

    private fun updateOverlay(content: @Composable () -> Unit, touchable: Boolean) {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        // If NOT touchable, we add the flag that makes touches pass THROUGH
        if (!touchable) {
            params.flags = params.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }

        if (overlayView == null) {
            val themeContext = android.view.ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault_NoActionBar)
            overlayView = ComposeView(themeContext).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setViewTreeLifecycleOwner(this@SpeedsterAccessibilityService)
                setViewTreeViewModelStoreOwner(this@SpeedsterAccessibilityService)
                setViewTreeSavedStateRegistryOwner(this@SpeedsterAccessibilityService)
                setContent { content() }
            }
            windowManager.addView(overlayView, params)
        } else {
            windowManager.updateViewLayout(overlayView, params)
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