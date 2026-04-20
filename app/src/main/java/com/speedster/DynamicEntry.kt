package com.speedster

import android.content.Context
import androidx.compose.runtime.Composable

/**
 * The Secret Handshake.
 * Your dynamic payload MUST implement this as 'com.speedster.payload.PayloadEntry'
 */
import java.io.File

interface DynamicEntry {
    @Composable
    fun Render(context: Context, resDir: File)
}