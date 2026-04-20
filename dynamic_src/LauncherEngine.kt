package com.speedster.payload

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings

data class AppModel(
    val label: String,
    val packageName: String
)

class LauncherEngine(private val context: Context) {
    private val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager

    fun getInstalledApps(): List<AppModel> {
        val allApps = mutableListOf<AppModel>()
        val profiles = userManager.userProfiles

        for (profile in profiles) {
            val activities = launcherApps.getActivityList(null, profile)
            for (info in activities) {
                val label = info.label.toString()
                val packageName = info.componentName.packageName
                
                allApps.add(AppModel(label, packageName))
            }
        }

        return allApps
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun getAppIcon(packageName: String): android.graphics.drawable.Drawable {
        return try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            context.packageManager.defaultActivityIcon
        }
    }

    fun launchApp(packageName: String) {
        val pm = context.packageManager
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