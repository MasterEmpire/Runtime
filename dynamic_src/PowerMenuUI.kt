package com.speedster.payload

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PowerMenuOverlay(onDismiss: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        com.speedster.DynamicEntry.log("🎨 CUSTOM POWER MENU UI MOUNTED!")
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .blur(25.dp)
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row {
                PowerButton(PowerIcons.Power, "Power off", Color(0xFF3A3A3A))
                Spacer(modifier = Modifier.width(40.dp))
                PowerButton(PowerIcons.Restart, "Restart", Color(0xFF2ECC71))
            }
            Spacer(modifier = Modifier.height(40.dp))
            Row {
                PowerButton(PowerIcons.Emergency, "Emergency mode", Color(0xFFE74C3C), "off")
                Spacer(modifier = Modifier.width(40.dp))
                PowerButton(PowerIcons.Lockdown, "Lockdown mode", Color(0xFF1ABC9C))
            }
        }

        Button(
            onClick = { },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
            shape = CircleShape
        ) {
            Text("Side key settings", color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
fun PowerButton(icon: ImageVector, label: String, bgColor: Color, subLabel: String? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(120.dp)) {
        Box(
            modifier = Modifier
                .size(82.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(38.dp), tint = Color.White)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(label, color = Color.White, fontSize = 17.sp, textAlign = TextAlign.Center)
        if (subLabel != null) {
            Text(subLabel, color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
        }
    }
}