package com.github.wooongyee.komvi.core

/**
 * Logger interface for Komvi framework.
 *
 * Provides platform-independent logging capability.
 * Implementations can use platform-specific logging mechanisms
 * (e.g., android.util.Log on Android, println on JVM).
 */
interface KomviLogger {
    /**
     * Logs a debug message.
     *
     * @param tag Used to identify the source of the log message
     * @param message The message to log
     */
    fun debug(tag: String, message: String)
}

/**
 * Console-based logger implementation.
 *
 * Uses standard output (println) for logging.
 * Suitable for JVM environments and testing.
 */
class ConsoleLogger : KomviLogger {
    override fun debug(tag: String, message: String) {
        println("[$tag] $message")
    }
}
