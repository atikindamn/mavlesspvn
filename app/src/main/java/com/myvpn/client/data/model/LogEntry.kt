package com.myvpn.client.data.model

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val message: String
)

enum class LogLevel {
    DEBUG, INFO, WARNING, ERROR;

    val emoji: String
        get() = when (this) {
            DEBUG -> "🔍"
            INFO -> "ℹ️"
            WARNING -> "⚠️"
            ERROR -> "❌"
        }
}
