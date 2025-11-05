package com.github.wooongyee.komvi.android

import com.github.wooongyee.komvi.core.KomviLogger

/**
 * Android platform logger implementation.
 *
 * Uses android.util.Log for logging on Android platform.
 */
class AndroidLogger : KomviLogger {
    override fun debug(tag: String, message: String) {
        android.util.Log.d(tag, message)
    }
}
