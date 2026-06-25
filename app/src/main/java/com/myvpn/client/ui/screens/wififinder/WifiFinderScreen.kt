package com.myvpn.client.ui.screens.wififinder

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myvpn.client.ui.theme.VpnGreen
import com.myvpn.client.ui.theme.VpnOrange
import com.myvpn.client.ui.theme.VpnRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ==================== Data ====================

data class TrackedNetwork(
    val ssid: String,
    val bssid: String,
    val signalLevel: Int, // dBm
    val signalPercent: Int, // 0-100
    val frequency: Int,
    val isConnected: Boolean = false
)

data class FinderState(
    val hasPermission: Boolean = false,
    val isScanning: Boolean = false,
    val step: FinderStep = FinderStep.INTRO,
    val networks: List<TrackedNetwork> = emptyList(),
    val connectedNetwork: TrackedNetwork? = null,
    val topNetwork: TrackedNetwork? = null, // strongest signal
    val signalHistory: List<Int> = emptyList(), // last N signal readings
    val tip: String = ""
)

enum class FinderStep {
    INTRO, SCANNING, WALK_AROUND, FOUND
}

// ==================== ViewModel ====================

class WifiFinderViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(FinderState())
    val state: StateFlow<FinderState> = _state.asStateFlow()

    private var scanning = false

    fun onPermissionResult(granted: Boolean) {
        _state.value = _state.value.copy(hasPermission = granted)
    }

    fun startFinding() {
        _state.value = _state.value.copy(step = FinderStep.SCANNING, signalHistory = emptyList())
        startContinuousScan()
    }

    fun goToWalkAround() {
        _state.value = _state.value.copy(step = FinderStep.WALK_AROUND, signalHistory = emptyList())
    }

    fun reset() {
        scanning = false
        _state.value = FinderState(hasPermission = _state.value.hasPermission, step = FinderStep.INTRO)
    }

    private fun startContinuousScan() {
        if (scanning) return
        scanning = true

        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val wifiManager = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            while (scanning) {
                try {
                    @Suppress("DEPRECATION")
                    wifiManager.startScan()
                    delay(2500)

                    val results = try { wifiManager.scanResults ?: emptyList() } catch (_: SecurityException) { emptyList() }
                    val connInfo = try { wifiManager.connectionInfo } catch (_: SecurityException) { null }
                    val connBssid = connInfo?.bssid

                    val networks = results.map { sr ->
                        TrackedNetwork(
                            ssid = sr.SSID.ifEmpty { "<Hidden>" },
                            bssid = sr.BSSID ?: "",
                            signalLevel = sr.level,
                            signalPercent = WifiManager.calculateSignalLevel(sr.level, 100),
                            frequency = sr.frequency,
                            isConnected = sr.BSSID == connBssid
                        )
                    }.sortedByDescending { it.signalPercent }

                    val connected = networks.find { it.isConnected }
                    val top = networks.firstOrNull()

                    // Update signal history for walk-around mode
                    val history = if (_state.value.step == FinderStep.WALK_AROUND && top != null) {
                        (_state.value.signalHistory + top.signalPercent).takeLast(20)
                    } else _state.value.signalHistory

                    // Generate tip
                    val tip = generateTip(connected, top, networks, _state.value.step)

                    _state.value = _state.value.copy(
                        networks = networks.take(10),
                        connectedNetwork = connected,
                        topNetwork = top,
                        signalHistory = history,
                        isScanning = true,
                        tip = tip
                    )

                    // Vibrate when signal is strong in walk-around mode
                    if (_state.value.step == FinderStep.WALK_AROUND && top != null && top.signalPercent > 80) {
                        vibrateShort(ctx)
                    }

                } catch (_: Exception) {
                    delay(3000)
                }
            }
        }
    }

    fun stopScanning() {
        scanning = false
    }

    private fun generateTip(connected: TrackedNetwork?, top: TrackedNetwork?, all: List<TrackedNetwork>, step: FinderStep): String {
        return when (step) {
            FinderStep.SCANNING -> {
                if (connected != null) {
                    "Вы подключены к \"${connected.ssid}\". Сигнал: ${connected.signalPercent}%. Это ваша сеть?"
                } else {
                    "Вы не подключены к WiFi. Подключитесь к любой сети или начните поиск."
                }
            }
            FinderStep.WALK_AROUND -> {
                if (top == null) return "Сканирование..."
                when {
                    top.signalPercent >= 85 -> "Сигнал отличный! Вы рядом с роутером \"${top.ssid}\". Скорее всего это ваша сеть!"
                    top.signalPercent >= 65 -> "Сигнал хороший. Продолжайте двигаться к роутеру. Самая сильная сеть: \"${top.ssid}\""
                    top.signalPercent >= 40 -> "Сигнал средний. Попробуйте подойти ближе к роутеру."
                    else -> "Сигнал слабый. Вы далеко от ближайшего роутера."
                }
            }
            else -> ""
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrateShort(ctx: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {}
    }
}

// ==================== UI ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiFinderScreen(onNavigateBack: () -> Unit, viewModel: WifiFinderViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onPermissionResult(it)
    }

    LaunchedEffect(Unit) {
        val has = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionResult(has)
        if (!has) permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    DisposableEffect(Unit) { onDispose { viewModel.stopScanning() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Найди свой WiFi", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = { viewModel.stopScanning(); onNavigateBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (state.step) {
                FinderStep.INTRO -> IntroContent(
                    hasPermission = state.hasPermission,
                    onStart = { viewModel.startFinding() },
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                )
                FinderStep.SCANNING -> ScanningContent(
                    state = state,
                    onWalkAround = { viewModel.goToWalkAround() },
                    onReset = { viewModel.reset() }
                )
                FinderStep.WALK_AROUND -> WalkAroundContent(
                    state = state,
                    onReset = { viewModel.reset() }
                )
                FinderStep.FOUND -> {} // not used yet
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun IntroContent(hasPermission: Boolean, onStart: () -> Unit, onRequestPermission: () -> Unit) {
    Spacer(Modifier.height(32.dp))

    Icon(Icons.Default.WifiFind, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(80.dp))

    Spacer(Modifier.height(16.dp))

    Text("Найдите свою WiFi сеть", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)

    Spacer(Modifier.height(8.dp))

    Text(
        "Этот инструмент поможет определить, какая WiFi сеть принадлежит вашему роутеру. Просто подойдите к роутеру — самая сильная сеть рядом с ним будет вашей.",
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
    )

    Spacer(Modifier.height(8.dp))

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StepItem("1", "Нажмите \"Начать поиск\"")
            StepItem("2", "Подойдите к роутеру (коробочка с антеннами)")
            StepItem("3", "Сеть с самым сильным сигналом — ваша!")
        }
    }

    Spacer(Modifier.height(16.dp))

    if (!hasPermission) {
        FilledTonalButton(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
            Text("Разрешить доступ к WiFi")
        }
    }

    Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp),
        enabled = hasPermission) {
        Icon(Icons.Default.Search, null, Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Начать поиск", fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StepItem(number: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
            Text(number, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ScanningContent(state: FinderState, onWalkAround: () -> Unit, onReset: () -> Unit) {
    // Connected network highlight
    state.connectedNetwork?.let { conn ->
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = VpnGreen.copy(alpha = 0.15f))) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Wifi, null, tint = VpnGreen, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(8.dp))
                Text("Вы подключены к:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(conn.ssid, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                SignalBar(conn.signalPercent)
                Spacer(Modifier.height(4.dp))
                Text("Если это ваша сеть — отлично!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    // Tip
    if (state.tip.isNotEmpty()) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lightbulb, null, tint = VpnOrange, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(state.tip, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    // Not sure? Walk around
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))) {
        Column(Modifier.padding(16.dp)) {
            Text("Не уверены какая сеть ваша?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("Включите режим радара и подойдите к роутеру — приложение покажет какая сеть усиливается.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onWalkAround, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Radar, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Включить радар")
            }
        }
    }

    // All networks
    Text("Все сети рядом:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())

    state.networks.forEach { net ->
        NetworkSignalCard(net)
    }

    TextButton(onClick = onReset) { Text("Начать заново") }
}

@Composable
fun WalkAroundContent(state: FinderState, onReset: () -> Unit) {
    val top = state.topNetwork

    // Pulsing radar
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(
            durationMillis = if (top != null) (2000 - top.signalPercent * 15).coerceAtLeast(300) else 1500,
            easing = EaseInOut
        ), RepeatMode.Reverse), label = "pulse"
    )

    val radarColor by animateColorAsState(
        targetValue = when {
            top == null -> Color.Gray
            top.signalPercent >= 80 -> VpnGreen
            top.signalPercent >= 50 -> VpnOrange
            else -> VpnRed
        }, label = "radarColor"
    )

    Spacer(Modifier.height(8.dp))

    // Big radar circle
    Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        // Outer pulse
        Box(Modifier.size((200 * pulseScale).dp).clip(CircleShape).background(radarColor.copy(alpha = 0.1f)))
        // Inner circle
        Box(Modifier.size(140.dp).clip(CircleShape).background(radarColor.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${top?.signalPercent ?: 0}%", style = MaterialTheme.typography.headlineLarge.copy(fontSize = 48.sp),
                    fontWeight = FontWeight.Black, color = radarColor)
                Text("сигнал", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    // Network name
    top?.let {
        Text(it.ssid, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("${it.signalLevel} dBm · ${it.frequency} MHz", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    // Tip
    if (state.tip.isNotEmpty()) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = radarColor.copy(alpha = 0.1f))) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NearMe, null, tint = radarColor, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(state.tip, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
        }
    }

    // Signal history mini-chart
    if (state.signalHistory.size > 2) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.padding(12.dp)) {
                Text("История сигнала", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().height(40.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                    state.signalHistory.forEach { signal ->
                        val barColor = when {
                            signal >= 70 -> VpnGreen; signal >= 40 -> VpnOrange; else -> VpnRed
                        }
                        Box(Modifier.width(8.dp).height((signal * 0.4f).coerceAtLeast(2f).dp)
                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)).background(barColor))
                    }
                }
            }
        }
    }

    // All networks sorted by signal
    Text("Сети по силе сигнала:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())

    state.networks.forEachIndexed { index, net ->
        NetworkSignalCard(net, rank = index + 1)
    }

    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Text("Завершить поиск")
    }
}

@Composable
fun NetworkSignalCard(network: TrackedNetwork, rank: Int? = null) {
    val signalColor = when {
        network.signalPercent >= 70 -> VpnGreen; network.signalPercent >= 40 -> VpnOrange; else -> VpnRed
    }

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (network.isConnected) VpnGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (rank != null) {
                Text("#$rank", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                    color = if (rank == 1) signalColor else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(30.dp))
            }
            Box(Modifier.size(36.dp).clip(CircleShape).background(signalColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Text("${network.signalPercent}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = signalColor)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(network.ssid, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (network.isConnected) {
                        Spacer(Modifier.width(6.dp))
                        Badge(containerColor = VpnGreen) { Text("Подключён", color = Color.Black) }
                    }
                }
                Text("${network.signalLevel} dBm · ${network.frequency} MHz",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SignalBars(network.signalPercent, signalColor)
        }
    }
}

@Composable
fun SignalBar(percent: Int) {
    val color = when { percent >= 70 -> VpnGreen; percent >= 40 -> VpnOrange; else -> VpnRed }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$percent%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.width(120.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.outlineVariant)) {
            Box(Modifier.fillMaxHeight().width((120 * percent / 100f).dp).clip(RoundedCornerShape(4.dp)).background(color))
        }
    }
}

@Composable
fun SignalBars(percent: Int, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        for (i in 1..4) {
            val threshold = i * 25
            val active = percent >= threshold
            Box(Modifier.width(4.dp).height((6 + i * 4).dp).clip(RoundedCornerShape(1.dp))
                .background(if (active) color else MaterialTheme.colorScheme.outlineVariant))
        }
    }
}
