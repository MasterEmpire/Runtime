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
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    "Overlay Test Lab", 
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            
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
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Launch Overlay")
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                "Note: Uses BackgroundStub (No Accessibility Required)",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }

    private fun launchTestOverlay(context: Context) {
        DynamicEntry.overlayConfig = DynamicEntry.OverlayConfig(
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            width = WindowManager.LayoutParams.WRAP_CONTENT,
            height = WindowManager.LayoutParams.WRAP_CONTENT,
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL,
            y = 150
        )

        DynamicEntry.overlayContent = {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF212121)),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Speedster Active", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = { DynamicEntry.overlayContent = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Dismiss", fontSize = 12.sp)
                    }
                }
            }
        }

        try {
            val intent = Intent(context, Class.forName("com.speedster.BackgroundStub"))
            context.startService(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}