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
            currentStep = when {
                !engine.isHomeApp() -> SetupStep.HOME
                !engine.hasOverlayPermission() -> SetupStep.OVERLAY
                !engine.isAccessibilityEnabled() -> SetupStep.ACCESSIBILITY
                else -> SetupStep.READY
            }
        }

        var lastPowerDown by remember { mutableLongStateOf(0L) }

        LaunchedEffect(Unit) {
            updateStep()
            DynamicEntry.keyInterceptor = { event ->
                if (event.keyCode == KeyEvent.KEYCODE_POWER) {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        if (lastPowerDown == 0L) lastPowerDown = System.currentTimeMillis()
                        
                        val duration = System.currentTimeMillis() - lastPowerDown
                        if (duration > 500) { // Long press threshold
                            if (!showPowerMenu) {
                                val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                vib.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                                showPowerMenu = true
                            }
                            true // Consume
                        } else false
                    } else {
                        lastPowerDown = 0L
                        showPowerMenu // If menu is showing, consume the UP event too
                    }
                } else false
            }
        }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) updateStep()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { 
                lifecycleOwner.lifecycle.removeObserver(observer)
                DynamicEntry.keyInterceptor = null
            }
        }

        if (showPowerMenu) {
            PowerMenuOverlay(onDismiss = { showPowerMenu = false })
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentStep) {
                    SetupStep.HOME -> OnboardingStep(
                        title = "Make it Home",
                        desc = "To act as a shell, Speedster must be set as your default Home app.",
                        buttonText = "Select Speedster",
                        onAction = { engine.openHomeSettings() }
                    )
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
                            withContext(Dispatchers.IO) {
                                val loadedApps = engine.getInstalledApps()
                                withContext(Dispatchers.Main) { apps = loadedApps }
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