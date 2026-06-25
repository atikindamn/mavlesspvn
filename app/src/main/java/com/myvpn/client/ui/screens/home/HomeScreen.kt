package com.myvpn.client.ui.screens.home

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myvpn.client.data.model.ConnectionState
import com.myvpn.client.data.model.VpnProfile
import com.myvpn.client.ui.theme.VpnGreen
import com.myvpn.client.ui.theme.VpnOrange
import com.myvpn.client.ui.theme.VpnRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAddProfile: () -> Unit,
    onNavigateToEditProfile: (Long) -> Unit,
    onNavigateToLogs: (() -> Unit)? = null,
    viewModel: HomeViewModel = viewModel()
) {
    val profiles by viewModel.profiles.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val currentMode by viewModel.currentMode.collectAsState()
    val selectedProfileId by viewModel.selectedProfileId.collectAsState()

    val isProxyActive = connectionStatus.state == ConnectionState.CONNECTED && currentMode == "proxy"
    val isVpnActive = currentMode == "vpn"

    // For this screen, show status only if mode is VPN or disconnected
    val effectiveState = when {
        isProxyActive -> ConnectionState.DISCONNECTED
        currentMode == "vpn" -> connectionStatus.state
        else -> connectionStatus.state
    }

    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.connect()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            val now = System.currentTimeMillis()
                            if (now - lastTapTime > 2000) tapCount = 1 else tapCount++
                            lastTapTime = now
                            if (tapCount >= 5) { tapCount = 0; onNavigateToLogs?.invoke() }
                        }
                    ) {
                        Text("ATvpn", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                            Text("XRAY", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddProfile, containerColor = MaterialTheme.colorScheme.primary) {
                Icon(Icons.Default.Add, "Добавить")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Warning if proxy is active
            if (isProxyActive) {
                Card(colors = CardDefaults.cardColors(containerColor = VpnOrange.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = VpnOrange, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Прокси активен. Отключите прокси для использования VPN.",
                            style = MaterialTheme.typography.bodySmall, color = VpnOrange)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            ConnectionStatusCard(
                state = effectiveState,
                connectedSince = if (isVpnActive) connectionStatus.connectedSince else null,
                profileName = if (isVpnActive) connectionStatus.connectedProfile?.name else null,
                selectedProfileId = selectedProfileId,
                enabled = !isProxyActive,
                onToggle = {
                    if (isProxyActive) return@ConnectionStatusCard
                    if (connectionStatus.state == ConnectionState.DISCONNECTED || connectionStatus.state == ConnectionState.ERROR) {
                        if (selectedProfileId == null) return@ConnectionStatusCard
                        val vpnIntent = viewModel.prepareVpn()
                        if (vpnIntent != null) vpnPermissionLauncher.launch(vpnIntent) else viewModel.connect()
                    } else if (isVpnActive) {
                        viewModel.toggleConnection()
                    }
                }
            )

            Spacer(Modifier.height(20.dp))
            Text("Серверы", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            if (profiles.isEmpty()) {
                EmptyProfilesCard(onAdd = onNavigateToAddProfile)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                    items(profiles, key = { it.id }) { profile ->
                        ProfileCard(
                            profile = profile,
                            isSelected = selectedProfileId == profile.id,
                            isConnected = isVpnActive && connectionStatus.state == ConnectionState.CONNECTED && connectionStatus.connectedProfile?.id == profile.id,
                            onSelect = { viewModel.selectProfile(profile.id) },
                            onEdit = { onNavigateToEditProfile(profile.id) },
                            onDelete = { viewModel.deleteProfile(profile) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusCard(
    state: ConnectionState,
    connectedSince: Long?,
    profileName: String?,
    selectedProfileId: Long?,
    enabled: Boolean = true,
    onToggle: () -> Unit
) {
    val statusColor by animateColorAsState(
        targetValue = if (!enabled) Color.Gray else when (state) {
            ConnectionState.CONNECTED -> VpnGreen
            ConnectionState.CONNECTING, ConnectionState.DISCONNECTING -> VpnOrange
            ConnectionState.ERROR -> VpnRed
            ConnectionState.DISCONNECTED -> Color.Gray
        }, label = "statusColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse), label = "pulseScale"
    )
    val buttonScale = if (state == ConnectionState.CONNECTING && enabled) pulseScale else 1f

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(100.dp).scale(buttonScale).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(statusColor, statusColor.copy(alpha = 0.6f))))
                    .clickable(enabled = enabled && (selectedProfileId != null || state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING)) { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(if (state == ConnectionState.CONNECTED) Icons.Default.Lock else Icons.Default.LockOpen,
                    "VPN Toggle", tint = Color.White, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(state.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = statusColor)
            if (profileName != null && state != ConnectionState.DISCONNECTED) {
                Text(profileName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state == ConnectionState.CONNECTED && connectedSince != null) {
                Spacer(Modifier.height(8.dp))
                StatusChip("С", SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(connectedSince)))
            }
            if (selectedProfileId == null && state == ConnectionState.DISCONNECTED && enabled) {
                Spacer(Modifier.height(8.dp))
                Text("Выберите сервер для подключения", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun StatusChip(label: String, value: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
            Text("$label: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ProfileCard(profile: VpnProfile, isSelected: Boolean, isConnected: Boolean, onSelect: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val borderColor = when { isConnected -> VpnGreen; isSelected -> MaterialTheme.colorScheme.primary; else -> Color.Transparent }
    Card(modifier = Modifier.fillMaxWidth().clickable { onSelect() }, shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(if (isSelected || isConnected) 2.dp else 0.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(if (isConnected) VpnGreen else Color.Gray))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(profile.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${profile.serverAddress}:${profile.serverPort} · VLESS+${profile.security.uppercase()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) { Icon(Icons.Outlined.Edit, "Edit", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(36.dp)) { Icon(Icons.Outlined.Delete, "Delete", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error) }
        }
    }
    if (showDeleteDialog) {
        AlertDialog(onDismissRequest = { showDeleteDialog = false }, title = { Text("Удалить профиль?") }, text = { Text("\"${profile.name}\" будет удалён.") },
            confirmButton = { TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("Удалить", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") } })
    }
}

@Composable
fun EmptyProfilesCard(onAdd: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.VpnLock, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text("Нет сохранённых серверов", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = onAdd) { Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Добавить сервер") }
        }
    }
}
