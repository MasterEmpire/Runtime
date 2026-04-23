package com.speedster.payload

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedster.DynamicEntry
import java.io.File
import kotlin.math.sqrt

class PayloadEntry : DynamicEntry {

    @Composable
    override fun Render(context: Context, resDir: File) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Image Spoof Lab", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(onClick = { launchImageSpoof(context, resDir) }) {
                Text("DEPLOY FAKE MENU")
            }
        }
    }

    private fun launchImageSpoof(context: Context, resDir: File) {
        val imageFile = File(resDir, "fake_menu.png")
        if (!imageFile.exists()) {
            DynamicEntry.log("❌ Error: fake_menu.png not found in res folder!")
            return
        }

        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
        val imageBitmap = bitmap.asImageBitmap()

        DynamicEntry.overlayConfig = DynamicEntry.OverlayConfig(
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL 
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN 
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            width = WindowManager.LayoutParams.MATCH_PARENT,
            height = WindowManager.LayoutParams.MATCH_PARENT
        )

        DynamicEntry.overlayContent = {
            DisposableEffect(Unit) {
                val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
                val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]
                        val gForce = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
                        
                        if (gForce > 2.7f) {
                            DynamicEntry.overlayContent = null
                            DynamicEntry.log("📳 Shake detected! Overlay killed.")
                        }
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }

                sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
                onDispose {
                    sensorManager.unregisterListener(listener)
                }
            }

            Image(
                bitmap = imageBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }

        try {
            val intent = Intent(context, Class.forName("com.speedster.BackgroundStub"))
            context.startService(intent)
        } catch (e: Exception) { e.printStackTrace() }
    }
}