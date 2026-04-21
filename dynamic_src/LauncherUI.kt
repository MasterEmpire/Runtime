package com.speedster.payload

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.speedster.DynamicEntry
import androidx.compose.ui.text.font.FontFamily

@Composable
fun LauncherScreen(apps: List<AppModel>, engine: LauncherEngine, onAppClick: (String) -> Unit) {
    Scaffold(
        containerColor = Color(0xFF0A0A0A)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 16.dp),
            modifier = Modifier.padding(it).fillMaxSize()
        ) {
            items(apps, key = { it.packageName }) { app ->
                AppIconItem(app, engine, onAppClick)
            }
        }
    }
}

@Composable
fun AppIconItem(app: AppModel, engine: LauncherEngine, onClick: (String) -> Unit) {
    var iconBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(app.packageName) {
        withContext(Dispatchers.IO) {
            val drawable = engine.getAppIcon(app.packageName)
            iconBitmap = drawable.toBitmap(128, 128) // Fixed size = less memory
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(app.packageName) }
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(modifier = Modifier.size(48.dp).background(Color.DarkGray, RoundedCornerShape(8.dp)))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = app.label,
            color = Color.White,
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun OnboardingStep(title: String, desc: String, buttonText: String, onAction: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(title, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
        Spacer(modifier = Modifier.height(16.dp))
        Text(desc, color = Color.Gray, fontSize = 16.sp, lineHeight = 24.sp)
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onAction,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D5AFE))
        ) {
            Text(buttonText, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SystemLogOverlay(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val logs = DynamicEntry.systemLogs
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("System Logs", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Row {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("logs", logs.joinToString("\n")))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D5AFE))
                    ) { Text("Copy") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) { Text("Close") }
                }
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(logs.reversed()) { log ->
                    Text(log, color = Color.Green, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Divider(color = Color.DarkGray, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}