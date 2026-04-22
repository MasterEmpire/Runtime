package com.speedster

import android.service.quicksettings.TileService
import android.content.Intent
import android.app.PendingIntent
import android.os.Build

class SpeedsterTileService : TileService() {
    override fun onClick() {
        super.onClick()
        
        // 1. Nuke the payload from disk
        PayloadLoader.clearPayload(applicationContext)
        
        // 2. Wipe memory/static state instead of nuking the whole process
        DynamicEntry.overlayContent = null
        DynamicEntry.keyInterceptor = null
        DynamicEntry.accessibilityInterceptor = null
        DynamicEntry.broadcastInterceptor = null
        DynamicEntry.serviceLifecycleInterceptor = null
        DynamicEntry.systemLogs.clear()
        
        // 3. Summon the Lobby and collapse the notification shade
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}