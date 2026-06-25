package com.myvpn.client.ui.screens.settings

import android.app.Application
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myvpn.client.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ==================== Theme Manager ====================

object ThemeManager {
    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private const val PREF_KEY = "theme_mode"
    private var prefs: android.content.SharedPreferences? = null

    fun init(context: android.content.Context) {
        prefs = context.getSharedPreferences("atnettool_settings", android.content.Context.MODE_PRIVATE)
        val saved = prefs?.getString(PREF_KEY, "DARK") ?: "DARK"
        _themeMode.value = try { ThemeMode.valueOf(saved) } catch (_: Exception) { ThemeMode.DARK }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs?.edit()?.putString(PREF_KEY, mode.name)?.apply()
    }
}

// ==================== ViewModel ====================

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    val themeMode: StateFlow<ThemeMode> = ThemeManager.themeMode

    fun setTheme(mode: ThemeMode) {
        ThemeManager.setThemeMode(mode)
    }
}

// ==================== UI ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val currentTheme by viewModel.themeMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Оформление", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                // Theme picker
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Тема", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

                        ThemeOption(
                            icon = Icons.Default.LightMode,
                            title = "Светлая",
                            subtitle = "Светлые цвета с Material You акцентами",
                            selected = currentTheme == ThemeMode.LIGHT,
                            onClick = { viewModel.setTheme(ThemeMode.LIGHT) }
                        )

                        ThemeOption(
                            icon = Icons.Default.DarkMode,
                            title = "Тёмная",
                            subtitle = "Тёмные цвета с Material You акцентами",
                            selected = currentTheme == ThemeMode.DARK,
                            onClick = { viewModel.setTheme(ThemeMode.DARK) }
                        )

                        ThemeOption(
                            icon = Icons.Default.Brightness2,
                            title = "AMOLED",
                            subtitle = "Чисто чёрный фон для OLED экранов",
                            selected = currentTheme == ThemeMode.AMOLED,
                            onClick = { viewModel.setTheme(ThemeMode.AMOLED) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("О приложении", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("ATnetTool", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("Версия 1.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("VPN, прокси и сетевые инструменты",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(60.dp))
            }

            // atikin credit
            Text(
                "atikin",
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
            )
        }
    }
}

@Composable
fun ThemeOption(icon: ImageVector, title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Card(
        modifier = Modifier.fillMaxWidth()
            .border(width = if (selected) 2.dp else 1.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) {
                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}
