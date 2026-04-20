package com.speedster.payload

import android.content.Context
import androidx.compose.runtime.*
import com.speedster.DynamicEntry

class PayloadEntry : DynamicEntry {
    enum class SetupStep { HOME, OVERLAY, READY }

    @Composable
    override fun Render(context: Context) {
        val engine = remember { LauncherEngine(context) }
        var apps by remember { mutableStateOf<List<AppModel>>(emptyList()) }
        var currentStep by remember { mutableStateOf(SetupStep.HOME) }

        when (currentStep) {
            SetupStep.HOME -> OnboardingStep(
                title = "Make it Home",
                desc = "To act as a shell, Speedster must be set as your default Home app.",
                buttonText = "Select Speedster",
                onAction = {
                    engine.openHomeSettings()
                    currentStep = SetupStep.OVERLAY
                }
            )
            SetupStep.OVERLAY -> OnboardingStep(
                title = "Draw Over Apps",
                desc = "This allows the shell to manage system gestures and overlays.",
                buttonText = "Enable Overlay",
                onAction = {
                    engine.openOverlaySettings()
                    currentStep = SetupStep.READY
                }
            )
            SetupStep.READY -> {
                LaunchedEffect(Unit) {
                    apps = engine.getInstalledApps()
                }
                LauncherScreen(
                    apps = apps,
                    onAppClick = { pkg -> engine.launchApp(pkg) }
                )
            }
        }
    }
}