package com.speedster.payload

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.speedster.DynamicEntry

class PayloadEntry : DynamicEntry {
    @Composable
    override fun Render(context: Context) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "⚡ SPEEDSTER ACTIVE",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Color.Cyan
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "The Ghost is in the Machine",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }
    }
}