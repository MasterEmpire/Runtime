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

    fun clearPayload(context: Context) {
        File(context.codeCacheDir, "payload.dex").delete()
        val prefs = context.getSharedPreferences("speedster_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("has_payload", false).apply()
    }
}