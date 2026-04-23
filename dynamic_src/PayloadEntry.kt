package com.speedster.payload

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedster.DynamicEntry
import java.io.File

class PayloadEntry : DynamicEntry {
    
    @Composable
    override fun Render(context: Context, resDir: File) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
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
                        launchFullScreenTakeover(context)
                    }
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("ENGAGE FULL TAKEOVER")
            }
        }
    }

    private fun launchFullScreenTakeover(context: Context) {
        // 1. Configure for FULL SCREEN
        DynamicEntry.overlayConfig = DynamicEntry.OverlayConfig(
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE 
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN 
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            width = WindowManager.LayoutParams.MATCH_PARENT,
            height = WindowManager.LayoutParams.MATCH_PARENT,
            gravity = Gravity.CENTER,
            alpha = 1.0f
        )

        // 2. Set the Takeover UI
        DynamicEntry.overlayContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xCC000000), // Translucent Black
                                Color(0xEE1A237E)  // Deep Midnight Blue
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "CONDUIT SYSTEM ACTIVE",
                        color = Color(0xFF00E5FF),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SCREEN CONQUERED",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    
                    Spacer(modifier = Modifier.height(40.dp))
                    
                    // The Nuke Button
                    Button(
                        onClick = { DynamicEntry.overlayContent = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("RELEASE SCREEN", fontWeight = FontWeight.Bold)
                    }
                }
                
                // Status indicator at bottom
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp)
                        .background(Color(0xFF00E5FF).copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("BYPASSING SYSTEM LIMITS", color = Color(0xFF00E5FF), fontSize = 10.sp)
                }
            }
        }

        // 3. Kickstart the background host
        try {
            val intent = Intent(context, Class.forName("com.speedster.BackgroundStub"))
            context.startService(intent)
        } catch (e: Exception) { e.printStackTrace() }
    }
}