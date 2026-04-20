package com.speedster.payload

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings

data class AppModel(
    val label: String,
    val packageName: String,
    val icon: Bitmap
)

class LauncherEngine(private val context: Context) {
    private val pm: PackageManager = context.packageManager

    fun getInstalledApps(): List<AppModel> {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        return pm.queryIntentActivities(intent, 0)
            .mapNotNull { info ->
                val packageName = info.activityInfo.packageName
                val label = info.loadLabel(pm).toString()
                val icon = drawableToBitmap(info.loadIcon(pm))
                AppModel(label, packageName, icon)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun launchApp(packageName: String) {
        val intent = pm.getLaunchIntentForPackage(packageName)
        intent?.let { 
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it) 
        }
    }

    fun openHomeSettings() {
        val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openOverlaySettings() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) return drawable.bitmap
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}