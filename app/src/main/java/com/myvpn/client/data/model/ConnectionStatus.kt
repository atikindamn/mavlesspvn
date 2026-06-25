package com.myvpn.client.data.model

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    DISCONNECTING,
    ERROR;

    val displayName: String
        get() = when (this) {
            DISCONNECTED -> "Отключено"
            CONNECTING -> "Подключение..."
            CONNECTED -> "Подключено"
            DISCONNECTING -> "Отключение..."
            ERROR -> "Ошибка"
        }
}

data class ConnectionStatus(
    val state: ConnectionState = ConnectionState.DISCONNECTED,
    val connectedProfile: VpnProfile? = null,
    val connectedSince: Long? = null,
    val bytesIn: Long = 0,
    val bytesOut: Long = 0,
    val assignedIp: String? = null,
    val errorMessage: String? = null
)
