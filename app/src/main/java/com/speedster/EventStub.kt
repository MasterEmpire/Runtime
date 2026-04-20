package com.speedster

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class EventStub : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        DynamicEntry.broadcastInterceptor?.invoke(context, intent)
    }
}