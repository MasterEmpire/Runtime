package com.speedster

import android.service.quicksettings.TileService
import android.content.Intent
import kotlin.system.exitProcess

class SpeedsterTileService : TileService() {
    override fun onClick() {
        super.onClick()
        PayloadLoader.clearPayload(applicationContext)
        // Kill everything to ensure a clean slate
        exitProcess(0)
    }
}