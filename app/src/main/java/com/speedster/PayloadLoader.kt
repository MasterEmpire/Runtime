package com.speedster

import android.content.Context
import dalvik.system.DexClassLoader
import java.io.File

import java.util.zip.ZipInputStream
import java.io.FileInputStream
import java.io.FileOutputStream

object PayloadLoader {
    private const val PAYLOAD_CLASS = "com.speedster.payload.PayloadEntry"
    
    fun getBundleDir(context: Context) = File(context.filesDir, "payload_bundle")
    fun getResDir(context: Context) = File(getBundleDir(context), "res")

    fun loadPayload(context: Context, bundleZip: File): DynamicEntry? {
        return try {
            val bundleDir = getBundleDir(context)
            bundleDir.deleteRecursively()
            bundleDir.mkdirs()

            // Unzip the bundle
            ZipInputStream(FileInputStream(bundleZip)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val newFile = File(bundleDir, entry.name)
                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile?.mkdirs()
                        FileOutputStream(newFile).use { fos -> zis.copyTo(fos) }
                    }
                    entry = zis.nextEntry
                }
            }

            val internalDex = File(bundleDir, "classes.dex")
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
                val tempFile = File(context.cacheDir, "bundle.zip")
                tempFile.outputStream().use { input.copyTo(it) }
                
                val loaded = loadPayload(context, tempFile)
                android.os.Handler(android.os.Looper.getMainLooper()).post { onComplete(loaded) }
            } catch (e: Exception) {
                e.printStackTrace()
                android.os.Handler(android.os.Looper.getMainLooper()).post { onComplete(null) }
            }
        }.start()
    }

    fun clearPayload(context: Context) {
        getBundleDir(context).deleteRecursively()
        context.getSharedPreferences("speedster_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("has_payload", false).apply()
    }

    fun loadExistingPayload(context: Context): DynamicEntry? {
        val internalDex = File(getBundleDir(context), "classes.dex")
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