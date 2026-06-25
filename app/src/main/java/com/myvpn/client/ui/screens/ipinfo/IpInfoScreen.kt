package com.myvpn.client.ui.screens.ipinfo

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myvpn.client.MyVpnApp
import com.myvpn.client.data.model.ConnectionState
import com.myvpn.client.ui.theme.VpnGreen
import com.myvpn.client.ui.theme.VpnOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class IpInfoData(
    val ip: String = "",
    val country: String = "",
    val countryCode: String = "",
    val region: String = "",
    val city: String = "",
    val isp: String = "",
    val org: String = "",
    val timezone: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0
)

data class IpInfoState(
    val isLoading: Boolean = false,
    val isProtected: Boolean = false,
    val mode: String? = null,
    val serverName: String? = null,
    val visibleIp: IpInfoData? = null,
    val error: String? = null
)

class IpInfoViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MyVpnApp
    private val vpnManager = app.vpnConnectionManager

    private val _state = MutableStateFlow(IpInfoState())
    val state: StateFlow<IpInfoState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = IpInfoState(isLoading = true)

            val connStatus = vpnManager.connectionStatus.value
            val isConnected = connStatus.state == ConnectionState.CONNECTED
            val mode = vpnManager.currentMode.value

            try {
                if (isConnected && mode != null) {
                    val serverAddress = when (mode) {
                        "vpn" -> connStatus.connectedProfile?.serverAddress
                        "proxy" -> vpnManager.getConnectedProxyProfile()?.serverAddress
                        else -> null
                    }
                    val serverName = when (mode) {
                        "vpn" -> connStatus.connectedProfile?.name
                        "proxy" -> vpnManager.getConnectedProxyProfile()?.name
                        else -> null
                    }
                    val serverInfo = if (serverAddress != null) fetchIpInfo(serverAddress) else null
                    _state.value = IpInfoState(
                        isProtected = true, mode = mode, serverName = serverName,
                        visibleIp = serverInfo,
                        error = if (serverInfo == null) "Не удалось получить данные о сервере" else null
                    )
                } else {
                    val realInfo = fetchMyIp()
                    _state.value = IpInfoState(
                        visibleIp = realInfo,
                        error = if (realInfo == null) "Не удалось определить IP" else null
                    )
                }
            } catch (e: Exception) {
                _state.value = IpInfoState(error = "Ошибка: ${e.message}")
            }
        }
    }

    private suspend fun fetchMyIp(): IpInfoData? = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://ip-api.com/json/?fields=status,country,countryCode,regionName,city,isp,org,timezone,lat,lon,query")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000; conn.readTimeout = 10000
            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect(); parseResponse(response)
        } catch (_: Exception) { null }
    }

    private suspend fun fetchIpInfo(ip: String): IpInfoData? = withContext(Dispatchers.IO) {
        try {
            val url = URL("http://ip-api.com/json/$ip?fields=status,country,countryCode,regionName,city,isp,org,timezone,lat,lon,query")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10000; conn.readTimeout = 10000
            val response = conn.inputStream.bufferedReader().readText()
            conn.disconnect(); parseResponse(response)
        } catch (_: Exception) { null }
    }

    private fun parseResponse(response: String): IpInfoData? {
        val json = JSONObject(response)
        if (json.optString("status") != "success") return null
        return IpInfoData(
            ip = json.optString("query"), country = json.optString("country"),
            countryCode = json.optString("countryCode"), region = json.optString("regionName"),
            city = json.optString("city"), isp = json.optString("isp"),
            org = json.optString("org"), timezone = json.optString("timezone"),
            lat = json.optDouble("lat", 0.0), lon = json.optDouble("lon", 0.0)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IpInfoScreen(
    onNavigateBack: () -> Unit,
    viewModel: IpInfoViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("IP Info", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
                actions = { IconButton(onClick = { viewModel.refresh() }) { Icon(Icons.Default.Refresh, "Обновить") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isLoading) {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Text("Проверка...", style = MaterialTheme.typography.titleMedium)
                }
            }

            state.error?.let { error ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(12.dp))
                        Text(error, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            // Status
            if (!state.isLoading) {
                val modeLabel = when (state.mode) { "vpn" -> "VPN"; "proxy" -> "Прокси"; else -> null }
                val connected = state.isProtected && modeLabel != null

                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (connected) VpnGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (connected) Icons.Default.Lock else Icons.Default.Public, null,
                            tint = if (connected) VpnGreen else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(if (connected) "$modeLabel подключён" else "Текущий IP",
                                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                                color = if (connected) VpnGreen else MaterialTheme.colorScheme.onSurface)
                            if (connected && state.serverName != null) {
                                Text("Сервер: ${state.serverName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // IP data
            state.visibleIp?.let { info ->
                InfoSection(title = "IP Адрес", icon = Icons.Filled.Public,
                    statusColor = if (state.isProtected) VpnGreen else MaterialTheme.colorScheme.primary,
                    statusText = if (state.isProtected) "Через ${if (state.mode == "vpn") "VPN" else "прокси"}" else "Прямой") {
                    InfoRow("IP", info.ip)
                    InfoRow("Страна", "${info.country} (${info.countryCode})")
                    InfoRow("Регион", info.region)
                    InfoRow("Город", info.city)
                    InfoRow("ISP", info.isp)
                    if (info.org.isNotEmpty() && info.org != info.isp) InfoRow("Организация", info.org)
                    InfoRow("Часовой пояс", info.timezone)
                    InfoRow("Координаты", "%.4f, %.4f".format(info.lat, info.lon))
                    if (state.mode == "proxy") {
                        Spacer(Modifier.height(8.dp))
                        Text("Резидентский прокси: реальный выходной IP может отличаться. Проверьте в браузере.",
                            style = MaterialTheme.typography.bodySmall, color = VpnOrange)
                    }
                }
            }

            // DNS
            if (!state.isLoading) {
                InfoSection(title = "DNS", icon = Icons.Filled.Dns,
                    statusColor = if (state.isProtected) VpnGreen else MaterialTheme.colorScheme.primary,
                    statusText = if (state.isProtected) "Через туннель" else "Прямой") {
                    Text(
                        if (state.isProtected) "DNS запросы идут через туннель (1.1.1.1, 1.0.0.1)."
                        else "DNS запросы идут через провайдера.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (!state.isLoading) {
                InfoSection(title = "WebRTC", icon = Icons.Filled.Videocam, statusColor = VpnGreen, statusText = "Нет утечки") {
                    Text("Нативные Android-приложения не используют WebRTC.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun InfoSection(title: String, icon: ImageVector, statusColor: Color, statusText: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(8.dp), color = statusColor.copy(alpha = 0.15f)) {
                    Text(statusText, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
