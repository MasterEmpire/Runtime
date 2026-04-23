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

    private var isPanicTriggered = false

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val action = event.action

        // Panic Button Logic: Vol Up held for 4000ms
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (action == KeyEvent.ACTION_DOWN) {
                val holdDuration = event.eventTime - event.downTime
                if (holdDuration >= 4000 && !isPanicTriggered) {
                    isPanicTriggered = true
                    DynamicEntry.overlayContent = null
                    DynamicEntry.log("🛑 PANIC: Vol Up held for 4s. Overlay Cleared.")
                    return true
                }
            } else if (action == KeyEvent.ACTION_UP) {
                isPanicTriggered = false
            }
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
            combine(
                snapshotFlow { DynamicEntry.overlayContent },
                snapshotFlow { DynamicEntry.overlayConfig },
                ::Pair
            ).collect { (content, config) ->
                if (content != null) updateOverlay(content, config) else hideOverlay()
            }
        }
    }

    private fun <T1, T2, R> combine(f1: kotlinx.coroutines.flow.Flow<T1>, f2: kotlinx.coroutines.flow.Flow<T2>, transform: (T1, T2) -> R): kotlinx.coroutines.flow.Flow<R> = 
        kotlinx.coroutines.flow.combine(f1, f2, transform)

    private fun updateOverlay(content: @Composable () -> Unit, config: DynamicEntry.OverlayConfig) {
        val params = WindowManager.LayoutParams(
            config.width,
            config.height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            config.flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = config.gravity
            alpha = config.alpha
            x = config.x
            y = config.y
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