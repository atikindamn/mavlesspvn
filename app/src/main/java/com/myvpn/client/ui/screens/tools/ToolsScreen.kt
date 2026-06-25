package com.myvpn.client.ui.screens.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myvpn.client.ui.theme.VpnGreen
import com.myvpn.client.ui.theme.VpnOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    onNavigateToWifi: () -> Unit,
    onNavigateToSpeedTest: () -> Unit,
    onNavigateToRouter: () -> Unit,
    onNavigateToIpInfo: () -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToProxy: () -> Unit,
    onNavigateToWifiFinder: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Инструменты", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {

            Text("Подключение", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            ToolCard(Icons.Default.Lan, "Прокси", "SOCKS5 прокси с авторизацией", Color(0xFF7C4DFF), onNavigateToProxy)
            ToolCard(Icons.Default.Subscriptions, "Подписки VPN", "Импорт серверов из ссылок и Base64", Color(0xFF9C27B0), onNavigateToSubscriptions)

            Spacer(Modifier.height(4.dp))
            Text("Диагностика", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            ToolCard(Icons.Default.Speed, "Speed Test", "Замер скорости интернета через fast.com", MaterialTheme.colorScheme.primary, onNavigateToSpeedTest)
            ToolCard(Icons.Default.Security, "IP Info", "Информация о текущем IP, DNS и утечках", VpnGreen, onNavigateToIpInfo)
            ToolCard(Icons.Default.Wifi, "WiFi Analyzer", "Сканирование сетей, каналы, AI-анализ", Color(0xFF2196F3), onNavigateToWifi)

            Spacer(Modifier.height(4.dp))
            Text("Помощники", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            ToolCard(Icons.Default.WifiFind, "Найди свой WiFi", "Определите свою сеть по силе сигнала", Color(0xFFE91E63), onNavigateToWifiFinder)
            ToolCard(Icons.Default.Router, "Настройка роутера", "AI-помощник для настройки через админ-панель", VpnOrange, onNavigateToRouter)

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun ToolCard(icon: ImageVector, title: String, description: String, color: Color, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.15f), modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(icon, null, tint = color, modifier = Modifier.size(28.dp)) }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
