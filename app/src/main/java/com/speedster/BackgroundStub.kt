package com.speedster

import android.app.Service
import android.content.Intent
import android.os.IBinder

import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.*
import android.graphics.PixelFormat
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import kotlinx.coroutines.*

class BackgroundStub : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
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
        DynamicEntry.activeBackgroundService = this
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        
        // Watch for overlay requests
        scope.launch {
            snapshotFlow { DynamicEntry.overlayContent }.collect { content ->
                if (content != null) updateOverlay(content) else hideOverlay()
            }
        }
    }

    private fun updateOverlay(content: @androidx.compose.runtime.Composable () -> Unit) {
        val config = DynamicEntry.overlayConfig
        val params = WindowManager.LayoutParams(
            config.width,
            config.height,
            config.type,
            config.flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = config.gravity
            alpha = config.alpha
            x = config.x
            y = config.y
        }

        if (overlayView == null) {
            overlayView = ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setViewTreeLifecycleOwner(this@BackgroundStub)
                setViewTreeViewModelStoreOwner(this@BackgroundStub)
                setViewTreeSavedStateRegistryOwner(this@BackgroundStub)
                setContent { content() }
            }
            windowManager.addView(overlayView, params)
        } else {
            windowManager.updateViewLayout(overlayView, params)
        }
    }

    private fun hideOverlay() {
        overlayView?.let { 
            try { windowManager.removeView(it) } catch (e: Exception) { }
            overlayView = null 
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        hideOverlay()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        if (DynamicEntry.activeBackgroundService === this) {
            DynamicEntry.activeBackgroundService = null
        }
        scope.cancel()
    }

    override fun onBind(intent: Intent?): android.os.IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        return DynamicEntry.serviceLifecycleInterceptor?.invoke(this, intent, flags, startId) 
            ?: START_STICKY
    }
}