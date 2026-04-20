package com.speedster.payload

import android.content.Context
import androidx.compose.runtime.*
import com.speedster.DynamicEntry

class PayloadEntry : DynamicEntry {
    @Composable
    override fun Render(context: Context) {
        val engine = remember { LauncherEngine(context) }
        var apps by remember { mutableStateOf<List<AppModel>>(emptyList()) }
        var permissionGranted by remember { mutableStateOf(false) }

        // In a real app, you'd check if permissions are actually granted here.
        // For now, we toggle state to show the transition.
        if (!permissionGranted) {
            PermissionScreen(onGrantClick = {
                engine.openAppSettings()
                permissionGranted = true // Move forward after opening settings
            })
        } else {
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