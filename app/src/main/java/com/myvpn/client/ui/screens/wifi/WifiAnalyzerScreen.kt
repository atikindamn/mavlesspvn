package com.myvpn.client.ui.screens.wifi

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myvpn.client.ui.theme.VpnGreen
import com.myvpn.client.ui.theme.VpnOrange
import com.myvpn.client.ui.theme.VpnRed
import com.myvpn.client.utils.AiHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

// ==================== Data ====================

data class WifiNetwork(
    val ssid: String, val bssid: String, val signalLevel: Int,
    val frequency: Int, val channel: Int, val capabilities: String, val signalPercent: Int
)

data class CurrentConnection(
    val ssid: String = "", val bssid: String = "", val ipAddress: String = "",
    val gateway: String = "", val linkSpeed: Int = 0, val frequency: Int = 0,
    val rssi: Int = 0, val channel: Int = 0, val dns1: String = "", val dns2: String = ""
)

data class LanDevice(
    val ip: String,
    val mac: String = "",
    val hostname: String = "",
    val vendor: String = "",
    val openPorts: List<Int> = emptyList(),
    val deviceType: String = "" // router, phone, pc, printer, camera, tv, iot
)

data class WifiState(
    val isLoading: Boolean = false, val hasPermission: Boolean = false,
    val currentConnection: CurrentConnection? = null,
    val networks: List<WifiNetwork> = emptyList(),
    val channelUsage2g: Map<Int, Int> = emptyMap(),
    val channelUsage5g: Map<Int, Int> = emptyMap(),
    val error: String? = null, val aiAnalysis: String? = null, val isAiLoading: Boolean = false,
    val lanDevices: List<LanDevice> = emptyList(),
    val isScanning: Boolean = false, val scanProgress: Float = 0f
)

// ==================== MAC Vendor DB (top vendors) ====================

private val macVendors = mapOf(
    "00:50:56" to "VMware", "00:0C:29" to "VMware", "00:1A:11" to "Google",
    "3C:5A:B4" to "Google", "F4:F5:D8" to "Google", "94:EB:2C" to "Google",
    "A4:77:33" to "Google", "DC:A6:32" to "Raspberry Pi", "B8:27:EB" to "Raspberry Pi",
    "00:17:88" to "Philips Hue", "EC:B5:FA" to "Philips Hue",
    "AC:BC:32" to "Apple", "F0:18:98" to "Apple", "A8:5C:2C" to "Apple",
    "14:7D:DA" to "Apple", "F4:5C:89" to "Apple", "BC:D0:74" to "Apple",
    "78:7B:8A" to "Apple", "D0:E1:40" to "Apple", "88:66:A5" to "Apple",
    "FC:E9:98" to "Apple", "AC:DE:48" to "Apple", "00:CD:FE" to "Apple",
    "30:35:AD" to "Apple", "3C:06:30" to "Apple", "70:56:81" to "Apple",
    "A4:83:E7" to "Apple", "60:F8:1D" to "Apple", "C8:69:CD" to "Apple",
    "A0:99:9B" to "Apple", "04:4B:ED" to "Apple", "28:6C:07" to "Apple",
    "30:D5:C8" to "Samsung", "78:AB:BB" to "Samsung", "AC:5F:3E" to "Samsung",
    "C0:97:27" to "Samsung", "8C:F5:A3" to "Samsung", "E4:7C:F9" to "Samsung",
    "50:01:D9" to "Samsung", "BC:72:B1" to "Samsung", "F8:04:2E" to "Samsung",
    "CC:07:AB" to "Samsung", "10:2C:6B" to "Samsung",
    "7C:2A:DB" to "Xiaomi", "64:CC:2E" to "Xiaomi", "28:6C:07" to "Xiaomi",
    "74:23:44" to "Xiaomi", "34:CE:00" to "Xiaomi", "58:44:98" to "Xiaomi",
    "50:64:2B" to "Xiaomi", "AC:C1:EE" to "Xiaomi", "78:11:DC" to "Xiaomi",
    "60:AB:67" to "Xiaomi", "04:CF:8C" to "Xiaomi", "B0:E2:35" to "Xiaomi",
    "B4:B0:24" to "Huawei", "88:3F:D3" to "Huawei", "48:8E:EF" to "Huawei",
    "C0:70:09" to "TP-Link", "50:C7:BF" to "TP-Link", "60:32:B1" to "TP-Link",
    "14:CC:20" to "TP-Link", "90:F6:52" to "TP-Link", "B0:4E:26" to "TP-Link",
    "EC:08:6B" to "TP-Link", "98:DA:C4" to "TP-Link", "AC:84:C6" to "TP-Link",
    "00:1E:58" to "D-Link", "28:10:7B" to "D-Link", "1C:7E:E5" to "D-Link",
    "00:26:5A" to "D-Link", "FC:75:16" to "D-Link",
    "F8:32:E4" to "ASUSTek", "04:D4:C4" to "ASUSTek", "2C:FD:A1" to "ASUSTek",
    "50:46:5D" to "ASUSTek", "AC:9E:17" to "ASUSTek",
    "00:1D:7E" to "Cisco", "00:26:CB" to "Cisco", "58:AC:78" to "Cisco",
    "B4:75:0E" to "Belkin", "C0:56:27" to "Belkin",
    "00:04:4B" to "Nvidia", "48:B0:2D" to "Nvidia",
    "A4:34:D9" to "Intel", "8C:EC:4B" to "Intel", "DC:1B:A1" to "Intel",
    "B4:96:91" to "Intel", "48:51:B7" to "Intel",
    "3C:7C:3F" to "ASUSTek", "C8:60:00" to "ASUSTek",
    "70:85:C2" to "Realtek", "48:5B:39" to "Realtek", "00:E0:4C" to "Realtek",
    "D8:EC:5E" to "Keenetic", "50:FF:20" to "Keenetic",
    "00:18:E7" to "MikroTik", "48:8F:5A" to "MikroTik",
    "78:8A:20" to "Ubiquiti", "FC:EC:DA" to "Ubiquiti",
    "00:1F:C6" to "PlayStation", "28:3F:69" to "PlayStation",
    "7C:BB:8A" to "Xbox", "60:45:BD" to "Xbox",
    "68:DB:F5" to "Amazon", "FC:65:DE" to "Amazon", "44:65:0D" to "Amazon",
    "00:71:47" to "Amazon", "A0:02:DC" to "Amazon",
    "CC:9E:A2" to "Amazon Echo", "74:C2:46" to "Amazon Echo",
    "18:B4:30" to "Nest", "64:16:66" to "Nest",
    "B0:BE:76" to "TP-Link Kasa", "60:01:94" to "TP-Link Kasa"
)

// ==================== ViewModel ====================

class WifiViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(WifiState())
    val state: StateFlow<WifiState> = _state.asStateFlow()

    fun onPermissionResult(granted: Boolean) {
        _state.value = _state.value.copy(hasPermission = granted)
        if (granted) scan()
    }

    fun scan() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, aiAnalysis = null, lanDevices = emptyList())
            delay(300)
            val ctx = getApplication<Application>()
            try {
                val wifiManager = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val current = getCurrentConnection(wifiManager)

                @Suppress("DEPRECATION")
                wifiManager.startScan()
                delay(2000)
                val results = try { wifiManager.scanResults ?: emptyList() } catch (_: SecurityException) { emptyList() }

                val networks = results.map { sr ->
                    WifiNetwork(sr.SSID.ifEmpty { "<Hidden>" }, sr.BSSID ?: "", sr.level, sr.frequency,
                        frequencyToChannel(sr.frequency), getSecurityType(sr.capabilities),
                        WifiManager.calculateSignalLevel(sr.level, 100))
                }.sortedByDescending { it.signalPercent }

                val ch2g = mutableMapOf<Int, Int>()
                val ch5g = mutableMapOf<Int, Int>()
                for (n in networks) {
                    if (n.frequency < 3000) ch2g[n.channel] = (ch2g[n.channel] ?: 0) + 1
                    else ch5g[n.channel] = (ch5g[n.channel] ?: 0) + 1
                }

                _state.value = WifiState(hasPermission = true, currentConnection = current,
                    networks = networks, channelUsage2g = ch2g, channelUsage5g = ch5g)
                analyzeWithAi()
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = "Ошибка: ${e.message}")
            }
        }
    }

    fun scanLanDevices() {
        val gateway = _state.value.currentConnection?.gateway ?: return
        val subnet = gateway.substringBeforeLast(".") // e.g. "192.168.0"

        viewModelScope.launch {
            _state.value = _state.value.copy(isScanning = true, scanProgress = 0f, lanDevices = emptyList())
            try {
                val devices = withContext(Dispatchers.IO) {
                    // Phase 1: Ping sweep to populate ARP table
                    val jobs = (1..254).map { i ->
                        async {
                            try {
                                val ip = "$subnet.$i"
                                val addr = InetAddress.getByName(ip)
                                addr.isReachable(300)
                            } catch (_: Exception) {}
                        }
                    }
                    // Update progress
                    var done = 0
                    for (job in jobs) {
                        job.await()
                        done++
                        if (done % 25 == 0) {
                            _state.value = _state.value.copy(scanProgress = done / 254f * 0.5f)
                        }
                    }

                    _state.value = _state.value.copy(scanProgress = 0.5f)

                    // Phase 2: Read ARP table
                    val arpDevices = readArpTable(subnet)

                    _state.value = _state.value.copy(scanProgress = 0.6f)

                    // Phase 3: Port scan + hostname for each device
                    val enrichedJobs = arpDevices.mapIndexed { index, device ->
                        async {
                            val enriched = enrichDevice(device, gateway)
                            _state.value = _state.value.copy(
                                scanProgress = 0.6f + (index.toFloat() / arpDevices.size) * 0.4f
                            )
                            enriched
                        }
                    }
                    enrichedJobs.awaitAll()
                }

                _state.value = _state.value.copy(
                    lanDevices = devices.sortedBy { it.ip.split(".").last().toIntOrNull() ?: 0 },
                    isScanning = false, scanProgress = 1f
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isScanning = false, error = "Ошибка сканирования: ${e.message}")
            }
        }
    }

    private fun readArpTable(subnet: String): List<LanDevice> {
        val devices = mutableListOf<LanDevice>()
        try {
            // Try ip neigh (works on newer Android)
            val process = Runtime.getRuntime().exec("ip neigh")
            val reader = process.inputStream.bufferedReader()
            var line = reader.readLine()
            while (line != null) {
                // Format: 192.168.0.1 dev wlan0 lladdr aa:bb:cc:dd:ee:ff REACHABLE
                val parts = line.split("\\s+".toRegex())
                if (parts.size >= 5) {
                    val ip = parts[0]
                    val llIndex = parts.indexOf("lladdr")
                    if (ip.startsWith(subnet) && llIndex >= 0 && llIndex + 1 < parts.size) {
                        val mac = parts[llIndex + 1].uppercase()
                        val state = parts.last()
                        if (mac != "00:00:00:00:00:00" && state != "FAILED") {
                            val vendor = lookupVendor(mac)
                            devices.add(LanDevice(ip = ip, mac = mac, vendor = vendor))
                        }
                    }
                }
                line = reader.readLine()
            }
            reader.close()
            process.waitFor()
        } catch (_: Exception) {}

        // Fallback: try /proc/net/arp anyway (works on older Android)
        if (devices.isEmpty()) {
            try {
                BufferedReader(FileReader("/proc/net/arp")).use { reader ->
                    reader.readLine()
                    var line = reader.readLine()
                    while (line != null) {
                        val parts = line.split("\\s+".toRegex())
                        if (parts.size >= 4) {
                            val ip = parts[0]
                            val mac = parts[3].uppercase()
                            if (ip.startsWith(subnet) && mac != "00:00:00:00:00:00") {
                                devices.add(LanDevice(ip = ip, mac = mac, vendor = lookupVendor(mac)))
                            }
                        }
                        line = reader.readLine()
                    }
                }
            } catch (_: Exception) {}
        }

        // Last fallback: just use IPs that responded to ping (no MAC)
        if (devices.isEmpty()) {
            for (i in 1..254) {
                val ip = "$subnet.$i"
                try {
                    if (InetAddress.getByName(ip).isReachable(100)) {
                        devices.add(LanDevice(ip = ip))
                    }
                } catch (_: Exception) {}
            }
        }

        return devices
    }

    private suspend fun enrichDevice(device: LanDevice, gateway: String): LanDevice = withContext(Dispatchers.IO) {
        // Hostname
        val hostname = try {
            val addr = InetAddress.getByName(device.ip)
            val h = addr.canonicalHostName
            if (h != device.ip) h else ""
        } catch (_: Exception) { "" }

        // Quick port scan
        val commonPorts = listOf(21, 22, 23, 53, 80, 443, 445, 554, 3389, 5353, 5555, 8080, 8443, 9100, 62078)
        val openPorts = commonPorts.filter { port ->
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(device.ip, port), 200)
                    true
                }
            } catch (_: Exception) { false }
        }

        // Determine device type
        val type = guessDeviceType(device, openPorts, gateway)

        device.copy(hostname = hostname, openPorts = openPorts, deviceType = type)
    }

    private fun guessDeviceType(device: LanDevice, ports: List<Int>, gateway: String): String {
        val vendor = device.vendor.lowercase()
        return when {
            device.ip == gateway -> "router"
            62078 in ports -> "iphone" // Apple mobile sync
            5555 in ports -> "android" // ADB
            9100 in ports -> "printer"
            554 in ports -> "camera" // RTSP
            3389 in ports -> "pc" // RDP
            445 in ports && 139 !in ports -> "pc" // SMB
            vendor.contains("apple") -> "apple"
            vendor.contains("samsung") || vendor.contains("xiaomi") || vendor.contains("huawei") -> "phone"
            vendor.contains("amazon") || vendor.contains("echo") -> "iot"
            vendor.contains("nest") || vendor.contains("philips") || vendor.contains("kasa") -> "iot"
            vendor.contains("playstation") -> "console"
            vendor.contains("xbox") -> "console"
            vendor.contains("tp-link") || vendor.contains("d-link") || vendor.contains("asus") || vendor.contains("keenetic") || vendor.contains("mikrotik") -> "router"
            vendor.contains("intel") || vendor.contains("realtek") -> "pc"
            vendor.contains("nvidia") -> "pc"
            80 in ports || 443 in ports -> "server"
            else -> "unknown"
        }
    }

    private fun lookupVendor(mac: String): String {
        val prefix = mac.take(8).uppercase()
        return macVendors[prefix] ?: ""
    }

    fun analyzeWithAi() {
        val s = _state.value
        if (s.currentConnection == null && s.networks.isEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isAiLoading = true)
            try {
                val report = buildWifiReport(s)
                val response = AiHelper.askTextOnly(report)
                _state.value = _state.value.copy(aiAnalysis = response, isAiLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(aiAnalysis = "Не удалось получить анализ: ${e.message}", isAiLoading = false)
            }
        }
    }

    private fun buildWifiReport(s: WifiState): String {
        val sb = StringBuilder()
        sb.appendLine("Проанализируй результаты WiFi сканирования и дай рекомендации простым языком.")
        sb.appendLine("Объясни значение ключевых параметров (dBm, каналы, частоты) для обычного пользователя.")
        sb.appendLine("Укажи что хорошо, что плохо, и что можно улучшить.")
        sb.appendLine("Отвечай кратко, по делу, на языке пользователя. НЕ используй Markdown форматирование. Используй только простой текст с нумерацией.")
        sb.appendLine()
        s.currentConnection?.let { c ->
            sb.appendLine("=== Текущее подключение ===")
            sb.appendLine("SSID: ${c.ssid}, Сигнал: ${c.rssi} dBm, Скорость: ${c.linkSpeed} Mbps")
            sb.appendLine("Частота: ${c.frequency} MHz (канал ${c.channel}), DNS: ${c.dns1}, ${c.dns2}")
            sb.appendLine()
        }
        sb.appendLine("=== Найдено сетей: ${s.networks.size} ===")
        s.networks.take(10).forEach { n -> sb.appendLine("${n.ssid}: ${n.signalLevel} dBm, CH ${n.channel}, ${n.capabilities}") }
        sb.appendLine()
        sb.appendLine("Формат: 1) Оценка подключения 2) Объяснение параметров 3) Рекомендации 4) На что обратить внимание")
        return sb.toString()
    }

    @Suppress("DEPRECATION")
    private fun getCurrentConnection(wifiManager: WifiManager): CurrentConnection? {
        try {
            val info = wifiManager.connectionInfo ?: return null
            if (info.networkId == -1) return null
            val dhcp = wifiManager.dhcpInfo
            val ssid = info.ssid?.replace("\"", "") ?: ""
            if (ssid == "<unknown ssid>") return null
            return CurrentConnection(ssid, info.bssid ?: "", intToIp(dhcp.ipAddress), intToIp(dhcp.gateway),
                info.linkSpeed, info.frequency, info.rssi, frequencyToChannel(info.frequency),
                intToIp(dhcp.dns1), intToIp(dhcp.dns2))
        } catch (_: SecurityException) { return null }
    }

    private fun intToIp(ip: Int) = "${ip and 0xFF}.${ip shr 8 and 0xFF}.${ip shr 16 and 0xFF}.${ip shr 24 and 0xFF}"
    private fun frequencyToChannel(freq: Int): Int = when {
        freq in 2412..2484 -> (freq - 2412) / 5 + 1; freq in 5170..5825 -> (freq - 5170) / 5 + 34
        freq == 2484 -> 14; else -> 0
    }
    private fun getSecurityType(cap: String): String = when {
        cap.contains("WPA3") -> "WPA3"; cap.contains("WPA2") -> "WPA2"
        cap.contains("WPA") -> "WPA"; cap.contains("WEP") -> "WEP"; else -> "Open"
    }
}

// ==================== UI ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiAnalyzerScreen(onNavigateBack: () -> Unit, viewModel: WifiViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.onPermissionResult(it) }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            viewModel.scan() else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("WiFi Analyzer", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
                actions = { IconButton(onClick = { viewModel.scan() }) { Icon(Icons.Default.Refresh, "Обновить") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {

            if (!state.hasPermission) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Нужен доступ к местоположению для сканирования WiFi", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        FilledTonalButton(onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) { Text("Разрешить") }
                    }
                }
            }

            if (state.isLoading) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Сканирование...", style = MaterialTheme.typography.titleMedium)
                }
            }

            // Current connection
            state.currentConnection?.let { conn ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = VpnGreen.copy(alpha = 0.1f))) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Wifi, null, tint = VpnGreen, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Текущее подключение", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        InfoRow("SSID", conn.ssid); InfoRow("IP", conn.ipAddress)
                        InfoRow("Шлюз", conn.gateway); InfoRow("Скорость", "${conn.linkSpeed} Mbps")
                        InfoRow("Сигнал", "${conn.rssi} dBm"); InfoRow("Частота", "${conn.frequency} MHz (канал ${conn.channel})")
                        InfoRow("DNS", "${conn.dns1}, ${conn.dns2}")
                    }
                }
            }

            // Channels 2.4 GHz
            if (state.channelUsage2g.isNotEmpty()) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Каналы 2.4 GHz", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            for (ch in 1..13) {
                                val count = state.channelUsage2g[ch] ?: 0; val cur = state.currentConnection?.channel == ch
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(Modifier.width(16.dp).height((8 + count * 16).coerceAtMost(64).dp)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(when { cur -> VpnGreen; count == 0 -> MaterialTheme.colorScheme.outlineVariant; count <= 2 -> VpnOrange; else -> VpnRed }))
                                    Text("$ch", style = MaterialTheme.typography.labelSmall, fontWeight = if (cur) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            LegendDot(VpnGreen, "Ваш"); LegendDot(MaterialTheme.colorScheme.outlineVariant, "Свободен")
                            LegendDot(VpnOrange, "1-2"); LegendDot(VpnRed, "3+")
                        }
                    }
                }
            }

            // Channels 5 GHz
            if (state.channelUsage5g.isNotEmpty()) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Каналы 5 GHz", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            for (ch in state.channelUsage5g.keys.sorted()) {
                                val count = state.channelUsage5g[ch] ?: 0; val cur = state.currentConnection?.channel == ch
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(Modifier.width(16.dp).height((8 + count * 16).coerceAtMost(64).dp)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(when { cur -> VpnGreen; count == 0 -> MaterialTheme.colorScheme.outlineVariant; count <= 2 -> VpnOrange; else -> VpnRed }))
                                    Text("$ch", style = MaterialTheme.typography.labelSmall, fontWeight = if (cur) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }
            }

            // AI Analysis
            Card(Modifier.fillMaxWidth().wrapContentHeight(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().wrapContentHeight().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = VpnOrange, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("AI Анализ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        if (!state.isAiLoading && state.aiAnalysis != null) {
                            IconButton(onClick = { viewModel.analyzeWithAi() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Refresh, "Обновить", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    when {
                        state.isAiLoading -> Text("Анализирую сеть...", style = MaterialTheme.typography.bodyMedium, color = VpnOrange)
                        state.aiAnalysis != null -> SelectionContainer {
                            Text(state.aiAnalysis!!, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
                        }
                        else -> Text("Выполните сканирование для анализа", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ==================== LAN Devices ====================
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Devices, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Устройства в сети", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        if (state.lanDevices.isNotEmpty()) {
                            Text("${state.lanDevices.size}", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    if (state.isScanning) {
                        Text("Сканирование сети... ${(state.scanProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall, color = VpnOrange)
                    } else if (state.lanDevices.isEmpty()) {
                        FilledTonalButton(onClick = { viewModel.scanLanDevices() }, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.Search, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                            Text("Сканировать устройства")
                        }
                    }
                }
            }

            // Device list
            state.lanDevices.forEach { device ->
                DeviceCard(device, isGateway = device.ip == state.currentConnection?.gateway,
                    isMe = device.ip == state.currentConnection?.ipAddress)
            }

            // Networks list
            if (state.networks.isNotEmpty()) {
                Text("Найдено сетей: ${state.networks.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                state.networks.forEach { network ->
                    NetworkCard(network, isCurrentNetwork = network.bssid == state.currentConnection?.bssid)
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun DeviceCard(device: LanDevice, isGateway: Boolean, isMe: Boolean) {
    val icon = when (device.deviceType) {
        "router" -> Icons.Default.Router; "phone", "iphone", "android" -> Icons.Default.Smartphone
        "pc" -> Icons.Default.Computer; "printer" -> Icons.Default.Print
        "camera" -> Icons.Default.Videocam; "apple" -> Icons.Default.PhoneIphone
        "iot" -> Icons.Default.Sensors; "console" -> Icons.Default.SportsEsports
        "server" -> Icons.Default.Dns; "tv" -> Icons.Default.Tv
        else -> Icons.Default.DeviceUnknown
    }
    val color = when {
        isGateway -> VpnOrange; isMe -> VpnGreen; else -> MaterialTheme.colorScheme.primary
    }
    var expanded by remember { mutableStateOf(false) }

    Card(Modifier.fillMaxWidth().clickable { expanded = !expanded }, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(device.ip, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (isGateway) { Spacer(Modifier.width(6.dp)); Badge { Text("Роутер") } }
                        if (isMe) { Spacer(Modifier.width(6.dp)); Badge(containerColor = VpnGreen) { Text("Вы", color = Color.Black) } }
                    }
                    val subtitle = listOfNotNull(
                        device.vendor.ifEmpty { null },
                        device.hostname.ifEmpty { null },
                        if (device.openPorts.isNotEmpty()) "${device.openPorts.size} портов" else null
                    ).joinToString(" · ")
                    if (subtitle.isNotEmpty()) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(8.dp))
                InfoRow("MAC", device.mac)
                if (device.vendor.isNotEmpty()) InfoRow("Производитель", device.vendor)
                if (device.hostname.isNotEmpty()) InfoRow("Hostname", device.hostname)
                if (device.deviceType.isNotEmpty()) InfoRow("Тип", device.deviceType)
                if (device.openPorts.isNotEmpty()) {
                    InfoRow("Открытые порты", device.openPorts.joinToString(", "))
                    val services = device.openPorts.map { port ->
                        when (port) {
                            21 -> "FTP"; 22 -> "SSH"; 23 -> "Telnet"; 53 -> "DNS"
                            80 -> "HTTP"; 443 -> "HTTPS"; 445 -> "SMB"; 554 -> "RTSP"
                            3389 -> "RDP"; 5353 -> "mDNS"; 5555 -> "ADB"; 8080 -> "HTTP-Alt"
                            8443 -> "HTTPS-Alt"; 9100 -> "Print"; 62078 -> "Apple Sync"
                            else -> "$port"
                        }
                    }
                    InfoRow("Сервисы", services.joinToString(", "))
                }
            }
        }
    }
}

@Composable
fun NetworkCard(network: WifiNetwork, isCurrentNetwork: Boolean) {
    val signalColor = when { network.signalPercent >= 70 -> VpnGreen; network.signalPercent >= 40 -> VpnOrange; else -> VpnRed }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (isCurrentNetwork) VpnGreen.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(signalColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Text("${network.signalPercent}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = signalColor)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(network.ssid, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("CH ${network.channel} · ${network.frequency} MHz · ${network.signalLevel} dBm · ${network.capabilities}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isCurrentNetwork) Icon(Icons.Default.CheckCircle, "Connected", tint = VpnGreen, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color)); Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
