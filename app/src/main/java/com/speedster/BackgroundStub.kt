package com.speedster

import android.app.Service
import android.content.Intent
import android.os.IBinder

class BackgroundStub : Service() {
    override fun onCreate() {
        super.onCreate()
        DynamicEntry.activeBackgroundService = this
    }

    override fun onDestroy() {
        super.onDestroy()
        if (DynamicEntry.activeBackgroundService === this) {
            DynamicEntry.activeBackgroundService = null
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return DynamicEntry.serviceLifecycleInterceptor?.invoke(this, intent, flags, startId) 
            ?: super.onStartCommand(intent, flags, startId)
    }
}