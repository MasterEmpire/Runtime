package com.speedster.payload

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.speedster.DynamicEntry
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class PayloadEntry : DynamicEntry {
    enum class SetupStep { HOME, OVERLAY, ACCESSIBILITY, READY }

    @Composable
    override fun Render(context: Context) {
        val engine = remember { LauncherEngine(context) }
        var apps by remember { mutableStateOf<List<AppModel>>(emptyList()) }
        var currentStep by remember { mutableStateOf(SetupStep.HOME) }
        val lifecycleOwner = LocalLifecycleOwner.current

        fun updateStep() {
            currentStep = when {
                !engine.isHomeApp() -> SetupStep.HOME
                !engine.hasOverlayPermission() -> SetupStep.OVERLAY
                !engine.isAccessibilityEnabled() -> SetupStep.ACCESSIBILITY
                else -> SetupStep.READY
            }
        }

        // Run initial check when entering the composition
        LaunchedEffect(Unit) {
            updateStep()
        }

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) updateStep()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

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
            }
        }
    }
}