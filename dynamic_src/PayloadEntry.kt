package com.speedster.payload

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedster.DynamicEntry

class PayloadEntry : DynamicEntry {
    @Composable
    override fun Render(context: Context) {
        val engine = remember { CalculatorEngine() }
        var displayState by remember { mutableStateOf("0") }

        val update = { 
            displayState = engine.display 
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(bottom = 24.dp)
        ) {
            // Display Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Text(
                    text = displayState,
                    fontSize = 80.sp,
                    color = Color.White,
                    textAlign = TextAlign.End,
                    lineHeight = 90.sp
                )
            }

            // Keypad
            val buttons = listOf(
                listOf("C", "±", "%", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "-"),
                listOf("1", "2", "3", "+"),
                listOf("0", ".", "=")
            )

            buttons.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { label ->
                        CalcButton(
                            label = label,
                            modifier = Modifier
                                .weight(if (label == "0") 2f else 1f)
                                .aspectRatio(if (label == "0") 2f else 1f),
                            onClick = {
                                when {
                                    label in "0".."9" || label == "." -> engine.onNumber(label)
                                    label == "C" -> engine.clear()
                                    label == "=" -> engine.onCalculate()
                                    else -> engine.onOperator(label)
                                }
                                update()
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    @Composable
    fun CalcButton(label: String, modifier: Modifier, onClick: () -> Unit) {
        val bgColor = when {
            label in "÷×-+= " -> Color(0xFFFF9F0A) // Orange
            label in "C±%" -> Color(0xFFA5A5A5) // Light Gray
            else -> Color(0xFF333333) // Dark Gray
        }
        val textColor = if (label in "C±%") Color.Black else Color.White

        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(bgColor)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = textColor,
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}