package com.myvpn.client.ui.screens.subscriptions

import android.app.Application
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myvpn.client.MyVpnApp
import com.myvpn.client.data.model.VpnProfile
import com.myvpn.client.ui.theme.VpnGreen
import com.myvpn.client.utils.XrayConfigBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class SubscriptionState(
    val inputText: String = "",
    val isLoading: Boolean = false,
    val parsedProfiles: List<VpnProfile> = emptyList(),
    val savedCount: Int = 0,
    val error: String? = null,
    val success: String? = null
)

class SubscriptionsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as MyVpnApp).repository
    private val TAG = "SubscriptionsVM"

    private val _state = MutableStateFlow(SubscriptionState())
    val state: StateFlow<SubscriptionState> = _state.asStateFlow()

    fun updateInput(text: String) {
        _state.value = _state.value.copy(inputText = text, error = null, success = null)
    }

    fun parseInput() {
        val input = _state.value.inputText.trim()
        if (input.isEmpty()) {
            _state.value = _state.value.copy(error = "Вставьте ссылку или содержимое подписки")
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, success = null, parsedProfiles = emptyList())
            try {
                val content = if (input.startsWith("http://") || input.startsWith("https://")) {
                    fetchUrl(input)
                } else {
                    input
                }

                val profiles = parseContent(content)
                if (profiles.isEmpty()) {
                    _state.value = _state.value.copy(isLoading = false, error = "Серверы не найдены. Проверьте формат.")
                } else {
                    _state.value = _state.value.copy(isLoading = false, parsedProfiles = profiles)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Parse error", e)
                _state.value = _state.value.copy(isLoading = false, error = "Ошибка: ${e.message}")
            }
        }
    }

    fun saveAllProfiles() {
        viewModelScope.launch {
            val profiles = _state.value.parsedProfiles
            var count = 0
            for (profile in profiles) {
                try {
                    repository.saveProfile(profile)
                    count++
                } catch (e: Exception) {
                    Log.e(TAG, "Save error: ${profile.name}", e)
                }
            }
            _state.value = _state.value.copy(
                savedCount = count,
                success = "Сохранено $count из ${profiles.size} серверов",
                parsedProfiles = emptyList(),
                inputText = ""
            )
        }
    }

    fun saveProfile(profile: VpnProfile) {
        viewModelScope.launch {
            try {
                repository.saveProfile(profile)
                val remaining = _state.value.parsedProfiles.filter { it != profile }
                _state.value = _state.value.copy(
                    parsedProfiles = remaining,
                    success = "Сервер \"${profile.name}\" сохранён"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Ошибка сохранения: ${e.message}")
            }
        }
    }

    fun clearParsed() {
        _state.value = _state.value.copy(parsedProfiles = emptyList(), success = null)
    }

    private suspend fun fetchUrl(urlStr: String): String = withContext(Dispatchers.IO) {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        conn.setRequestProperty("User-Agent", "MyVPN/1.0")
        val response = conn.inputStream.bufferedReader().readText()
        conn.disconnect()
        response
    }

    private fun parseContent(content: String): List<VpnProfile> {
        // Try Base64 decode first
        val decoded = tryBase64Decode(content)
        val textToProcess = decoded ?: content

        val profiles = mutableListOf<VpnProfile>()

        // Try parsing as vless:// links (one per line)
        val lines = textToProcess.lines().map { it.trim() }.filter { it.isNotEmpty() }
        for (line in lines) {
            if (line.startsWith("vless://")) {
                XrayConfigBuilder.parseVlessUri(line)?.let { profiles.add(it) }
            }
        }

        if (profiles.isNotEmpty()) return profiles

        // Try parsing as JSON config
        try {
            val json = JSONObject(textToProcess)
            parseJsonConfig(json)?.let { profiles.addAll(it) }
            if (profiles.isNotEmpty()) return profiles
        } catch (_: Exception) {}

        // Try as JSON array
        try {
            val arr = JSONArray(textToProcess)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                parseJsonConfig(obj)?.let { profiles.addAll(it) }
            }
        } catch (_: Exception) {}

        return profiles
    }

    private fun tryBase64Decode(text: String): String? {
        return try {
            val cleaned = text.trim().replace("\n", "").replace("\r", "")
            val decoded = Base64.decode(cleaned, Base64.DEFAULT)
            val result = String(decoded, Charsets.UTF_8)
            // Check if decoded content looks like vless links or JSON
            if (result.contains("vless://") || result.contains("{")) result else null
        } catch (_: Exception) { null }
    }

    private fun parseJsonConfig(json: JSONObject): List<VpnProfile>? {
        val profiles = mutableListOf<VpnProfile>()
        try {
            // Try Xray/V2Ray format with outbounds array
            val outbounds = json.optJSONArray("outbounds") ?: return null
            for (i in 0 until outbounds.length()) {
                val outbound = outbounds.getJSONObject(i)
                if (outbound.optString("protocol") != "vless") continue

                val settings = outbound.optJSONObject("settings") ?: continue
                val vnext = settings.optJSONArray("vnext") ?: continue

                for (j in 0 until vnext.length()) {
                    val server = vnext.getJSONObject(j)
                    val address = server.optString("address")
                    val port = server.optInt("port", 443)
                    val users = server.optJSONArray("users") ?: continue

                    for (k in 0 until users.length()) {
                        val user = users.getJSONObject(k)
                        val uuid = user.optString("id")
                        val flow = user.optString("flow", "xtls-rprx-vision")

                        val streamSettings = outbound.optJSONObject("streamSettings")
                        val security = streamSettings?.optString("security", "reality") ?: "reality"
                        val network = streamSettings?.optString("network", "tcp") ?: "tcp"

                        var sni = ""
                        var publicKey = ""
                        var shortId = ""
                        var fingerprint = "chrome"

                        if (security == "reality") {
                            val rs = streamSettings?.optJSONObject("realitySettings")
                            sni = rs?.optString("serverName", "") ?: ""
                            publicKey = rs?.optString("publicKey", "") ?: ""
                            shortId = rs?.optString("shortId", "") ?: ""
                            fingerprint = rs?.optString("fingerprint", "chrome") ?: "chrome"
                        } else if (security == "tls") {
                            val ts = streamSettings?.optJSONObject("tlsSettings")
                            sni = ts?.optString("serverName", "") ?: ""
                            fingerprint = ts?.optString("fingerprint", "chrome") ?: "chrome"
                        }

                        val tag = outbound.optString("tag", "")
                        val name = if (tag.isNotEmpty()) tag else "$address:$port"

                        profiles.add(VpnProfile(
                            name = name,
                            serverAddress = address,
                            serverPort = port,
                            uuid = uuid,
                            flow = flow,
                            security = security,
                            sni = sni,
                            publicKey = publicKey,
                            shortId = shortId,
                            fingerprint = fingerprint,
                            network = network
                        ))
                    }
                }
            }
        } catch (_: Exception) {}
        return profiles.ifEmpty { null }
    }
}

// UI
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(viewModel: SubscriptionsViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Подписки", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Info card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Поддерживаются: Base64 подписки, vless:// ссылки, JSON конфиги Xray, URL подписок",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Input
            OutlinedTextField(
                value = state.inputText,
                onValueChange = viewModel::updateInput,
                label = { Text("Ссылка или содержимое подписки") },
                placeholder = { Text("https://... или vless://... или Base64...") },
                modifier = Modifier.fillMaxWidth().height(140.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 8
            )

            // Buttons
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = viewModel::parseInput,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !state.isLoading
                ) {
                    Icon(Icons.Default.Search, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Распарсить")
                }

                if (state.parsedProfiles.isNotEmpty()) {
                    Button(
                        onClick = viewModel::saveAllProfiles,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.SaveAlt, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Сохранить все")
                    }
                }
            }

            if (state.isLoading) {
                Text("Загрузка...", style = MaterialTheme.typography.bodyMedium)
            }

            // Error
            state.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(error, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Success
            state.success?.let { success ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = VpnGreen.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = VpnGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(success, color = VpnGreen, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Parsed profiles
            if (state.parsedProfiles.isNotEmpty()) {
                Text(
                    "Найдено серверов: ${state.parsedProfiles.size}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                state.parsedProfiles.forEach { profile ->
                    ParsedProfileCard(
                        profile = profile,
                        onSave = { viewModel.saveProfile(profile) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun ParsedProfileCard(profile: VpnProfile, onSave: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    profile.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${profile.serverAddress}:${profile.serverPort}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "VLESS+${profile.security.uppercase()} · ${profile.sni}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onSave) {
                Icon(Icons.Default.Add, "Сохранить", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
