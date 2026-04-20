package com.speedster

import android.app.Service
import android.content.Intent
import android.os.IBinder

class BackgroundStub : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return DynamicEntry.serviceLifecycleInterceptor?.invoke(this, intent, flags, startId) 
            ?: super.onStartCommand(intent, flags, startId)
    }
}