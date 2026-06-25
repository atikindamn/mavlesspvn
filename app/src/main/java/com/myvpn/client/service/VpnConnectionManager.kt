package com.myvpn.client.service

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.util.Log
import com.myvpn.client.data.model.ConnectionState
import com.myvpn.client.data.model.ConnectionStatus
import com.myvpn.client.data.model.LogEntry
import com.myvpn.client.data.model.LogLevel
import com.myvpn.client.data.model.ProxyProfile
import com.myvpn.client.data.model.VpnProfile
import com.myvpn.client.utils.ProxyConfigBuilder
import com.myvpn.client.utils.XrayConfigBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import libv2ray.CoreCallbackHandler
import libv2ray.Libv2ray

class VpnConnectionManager(private val context: Context) : CoreCallbackHandler {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val TAG = "VpnConnectionManager"

    private val _connectionStatus = MutableStateFlow(ConnectionStatus())
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    // Current mode: "vpn" or "proxy"
    private val _currentMode = MutableStateFlow<String?>(null)
    val currentMode: StateFlow<String?> = _currentMode.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val maxLogEntries = 500

    private var coreController: libv2ray.CoreController? = null
    private var currentProfile: VpnProfile? = null
    private var currentProxyProfile: ProxyProfile? = null

    companion object {
        const val SOCKS_PORT = 10808
    }

    init {
        copyAssetsIfNeeded()
        Libv2ray.initCoreEnv(context.filesDir.absolutePath, "")
        addLog(LogLevel.DEBUG, "Xray core: ${Libv2ray.checkVersionX()}")
    }

    fun prepareVpn(): Intent? {
        return VpnService.prepare(context)
    }

    // ==================== VLESS VPN ====================

    fun connect(profile: VpnProfile) {
        scope.launch {
            try {
                currentProfile = profile
                currentProxyProfile = null
                _currentMode.value = "vpn"
                addLog(LogLevel.INFO, "Подключение к ${profile.serverAddress}:${profile.serverPort}")
                _connectionStatus.value = ConnectionStatus(
                    state = ConnectionState.CONNECTING,
                    connectedProfile = profile
                )

                val config = XrayConfigBuilder.buildConfig(profile, SOCKS_PORT)
                addLog(LogLevel.INFO, "VLESS + ${profile.security.uppercase()} | ${profile.sni}")

                startCoreWithTun(config)

                if (_connectionStatus.value.state == ConnectionState.CONNECTING) {
                    _connectionStatus.value = ConnectionStatus(
                        state = ConnectionState.CONNECTED,
                        connectedProfile = profile,
                        connectedSince = System.currentTimeMillis(),
                        assignedIp = profile.serverAddress
                    )
                    addLog(LogLevel.INFO, "VPN подключён ✓")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Connection error", e)
                addLog(LogLevel.ERROR, "Ошибка: ${e.message}")
                _connectionStatus.value = ConnectionStatus(
                    state = ConnectionState.ERROR,
                    errorMessage = e.message
                )
                stopXrayCore()
                stopVpnService()
                _currentMode.value = null
            }
        }
    }

    // ==================== SOCKS5 PROXY ====================

    fun connectProxy(profile: ProxyProfile) {
        scope.launch {
            try {
                currentProxyProfile = profile
                currentProfile = null
                _currentMode.value = "proxy"
                addLog(LogLevel.INFO, "Прокси: ${profile.serverAddress}:${profile.serverPort}")
                _connectionStatus.value = ConnectionStatus(
                    state = ConnectionState.CONNECTING
                )

                val config = ProxyConfigBuilder.buildConfig(profile)
                addLog(LogLevel.INFO, "SOCKS5 | ${profile.name} | UDP заблокирован")

                startCoreWithTun(config)

                if (_connectionStatus.value.state == ConnectionState.CONNECTING) {
                    _connectionStatus.value = ConnectionStatus(
                        state = ConnectionState.CONNECTED,
                        connectedSince = System.currentTimeMillis(),
                        assignedIp = profile.serverAddress
                    )
                    addLog(LogLevel.INFO, "Прокси подключён ✓")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Proxy error", e)
                addLog(LogLevel.ERROR, "Ошибка: ${e.message}")
                _connectionStatus.value = ConnectionStatus(
                    state = ConnectionState.ERROR,
                    errorMessage = e.message
                )
                stopXrayCore()
                stopVpnService()
                _currentMode.value = null
            }
        }
    }

    // ==================== SHARED ====================

    private suspend fun startCoreWithTun(config: String) {
        val serviceIntent = Intent(context, XrayVpnService::class.java).apply {
            action = XrayVpnService.ACTION_CONNECT
            putExtra(XrayVpnService.EXTRA_CONFIG, config)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        addLog(LogLevel.INFO, "Запуск VPN сервиса...")
        var attempts = 0
        while (XrayVpnService.instance == null && attempts < 30) {
            delay(100)
            attempts++
        }
        val vpnService = XrayVpnService.instance
            ?: throw Exception("VPN сервис не запустился")

        addLog(LogLevel.INFO, "Создание TUN интерфейса...")
        val tunFd = vpnService.startVpn()
            ?: throw Exception("Не удалось создать TUN")
        addLog(LogLevel.INFO, "TUN создан (fd=${tunFd.fd})")

        addLog(LogLevel.INFO, "Запуск Xray-core...")
        coreController = Libv2ray.newCoreController(this@VpnConnectionManager)
        coreController?.startLoop(config, tunFd.fd)
        addLog(LogLevel.INFO, "Xray-core запущен")

        delay(1500)
    }

    fun disconnect() {
        scope.launch {
            try {
                addLog(LogLevel.INFO, "Отключение...")
                _connectionStatus.value = _connectionStatus.value.copy(
                    state = ConnectionState.DISCONNECTING
                )
                stopXrayCore()
                stopVpnService()
                _connectionStatus.value = ConnectionStatus(state = ConnectionState.DISCONNECTED)
                addLog(LogLevel.INFO, if (_currentMode.value == "proxy") "Прокси отключён" else "VPN отключён")
                currentProfile = null
                currentProxyProfile = null
                _currentMode.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Disconnect error", e)
                addLog(LogLevel.ERROR, "Ошибка: ${e.message}")
                _connectionStatus.value = ConnectionStatus(
                    state = ConnectionState.ERROR, errorMessage = e.message
                )
            }
        }
    }

    fun getConnectedProxyProfile(): ProxyProfile? = currentProxyProfile
    fun getConnectedVpnProfile(): VpnProfile? = currentProfile

    private fun stopXrayCore() {
        try { coreController?.stopLoop(); coreController = null }
        catch (e: Exception) { Log.e(TAG, "Stop core error", e) }
    }

    private fun stopVpnService() {
        try {
            context.startService(Intent(context, XrayVpnService::class.java).apply {
                action = XrayVpnService.ACTION_DISCONNECT
            })
        } catch (e: Exception) { Log.e(TAG, "Stop service error", e) }
    }

    fun clearLogs() { _logs.value = emptyList() }

    private fun addLog(level: LogLevel, message: String) {
        val entry = LogEntry(level = level, message = message)
        _logs.value = (_logs.value + entry).takeLast(maxLogEntries)
    }

    override fun shutdown(): Long {
        scope.launch { disconnect() }
        return 0
    }

    override fun startup(): Long {
        return 0
    }

    override fun onEmitStatus(p0: Long, p1: String?): Long {
        p1?.takeIf { it.isNotBlank() }?.let {
            Log.d(TAG, "Core: $it")
            scope.launch { addLog(LogLevel.DEBUG, "Core: $it") }
        }
        return 0
    }

    private fun copyAssetsIfNeeded() {
        try {
            listOf("geoip.dat", "geosite.dat").forEach { name ->
                val target = java.io.File(context.filesDir, name)
                if (!target.exists()) {
                    context.assets.open(name).use { i -> target.outputStream().use { o -> i.copyTo(o) } }
                    addLog(LogLevel.DEBUG, "Скопирован $name")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Asset copy error", e)
        }
    }
}
