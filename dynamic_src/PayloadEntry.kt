package com.speedster.payload

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.speedster.DynamicEntry
import java.io.File

class PayloadEntry : DynamicEntry {
    
    @Composable
    override fun Render(context: Context, resDir: File) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Test Lab: Display Over Apps", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = {
                    if (!Settings.canDrawOverlays(context)) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } else {
                        launchTestOverlay(context)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Launch Overlay (Standard Permission)")
            }
        }
    }

    private fun launchTestOverlay(context: Context) {
        // 1. Configure for Standard Overlay (Not Accessibility)
        DynamicEntry.overlayConfig = DynamicEntry.OverlayConfig(
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            width = WindowManager.LayoutParams.WRAP_CONTENT,
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL,
            y = 100
        )

        // 2. Set the content
        DynamicEntry.overlayContent = {
            Box(
                modifier = Modifier
                    .background(Color.DarkGray, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Speedster Overlay", color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { DynamicEntry.overlayContent = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }

        // 3. Start the host service
        val intent = Intent(context, Class.forName("com.speedster.BackgroundStub"))
        context.startService(intent)
    }
}