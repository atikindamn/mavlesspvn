package com.myvpn.client.ui.screens.proxy

import android.app.Activity
import android.app.Application
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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myvpn.client.MyVpnApp
import com.myvpn.client.data.model.ConnectionState
import com.myvpn.client.data.model.ProxyProfile
import com.myvpn.client.ui.theme.VpnGreen
import com.myvpn.client.ui.theme.VpnOrange
import com.myvpn.client.ui.theme.VpnRed
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ==================== ViewModel ====================

data class ProxyFormState(
    val name: String = "",
    val serverAddress: String = "",
    val serverPort: String = "1080",
    val username: String = "",
    val password: String = "",
    val isEditing: Boolean = false,
    val editingId: Long = 0,
    val error: String? = null
)

class ProxyViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MyVpnApp
    private val repository = app.proxyRepository
    private val vpnManager = app.vpnConnectionManager

    val profiles: StateFlow<List<ProxyProfile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val connectionStatus = vpnManager.connectionStatus
    val currentMode = vpnManager.currentMode

    private val _selectedProfileId = MutableStateFlow<Long?>(null)
    val selectedProfileId: StateFlow<Long?> = _selectedProfileId.asStateFlow()

    private val _formState = MutableStateFlow(ProxyFormState())
    val formState: StateFlow<ProxyFormState> = _formState.asStateFlow()

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog.asStateFlow()

    fun selectProfile(id: Long) { _selectedProfileId.value = id }

    fun showAdd() {
        _formState.value = ProxyFormState()
        _showAddDialog.value = true
    }

    fun showEdit(profile: ProxyProfile) {
        _formState.value = ProxyFormState(
            name = profile.name,
            serverAddress = profile.serverAddress,
            serverPort = profile.serverPort.toString(),
            username = profile.username,
            password = profile.password,
            isEditing = true,
            editingId = profile.id
        )
        _showAddDialog.value = true
    }

    fun hideDialog() { _showAddDialog.value = false }

    fun updateName(v: String) { _formState.value = _formState.value.copy(name = v, error = null) }
    fun updateAddress(v: String) { _formState.value = _formState.value.copy(serverAddress = v.trim(), error = null) }
    fun updatePort(v: String) { _formState.value = _formState.value.copy(serverPort = v.filter { it.isDigit() }, error = null) }
    fun updateUsername(v: String) { _formState.value = _formState.value.copy(username = v, error = null) }
    fun updatePassword(v: String) { _formState.value = _formState.value.copy(password = v, error = null) }

    fun saveProfile() {
        val s = _formState.value
        if (s.name.isBlank()) { _formState.value = s.copy(error = "Введите название"); return }
        if (s.serverAddress.isBlank()) { _formState.value = s.copy(error = "Введите IP адрес"); return }
        val port = s.serverPort.toIntOrNull()
        if (port == null || port !in 1..65535) { _formState.value = s.copy(error = "Порт: 1-65535"); return }

        viewModelScope.launch {
            val profile = ProxyProfile(
                id = if (s.isEditing) s.editingId else 0,
                name = s.name.trim(),
                serverAddress = s.serverAddress.trim(),
                serverPort = port,
                username = s.username.trim(),
                password = s.password
            )
            repository.saveProfile(profile)
            _showAddDialog.value = false
        }
    }

    fun deleteProfile(profile: ProxyProfile) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
            if (_selectedProfileId.value == profile.id) _selectedProfileId.value = null
        }
    }

    fun connect() {
        viewModelScope.launch {
            val id = _selectedProfileId.value ?: return@launch
            val profile = repository.getProfileById(id) ?: return@launch
            vpnManager.connectProxy(profile)
            repository.updateLastConnected(id)
        }
    }

    fun disconnect() { vpnManager.disconnect() }

    fun toggleConnection() {
        val state = connectionStatus.value.state
        val mode = currentMode.value
        if (state == ConnectionState.CONNECTED && mode == "proxy") {
            disconnect()
        } else if (state == ConnectionState.DISCONNECTED || state == ConnectionState.ERROR) {
            connect()
        }
    }

    fun prepareVpn() = vpnManager.prepareVpn()
}

// ==================== UI ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxyScreen(viewModel: ProxyViewModel = viewModel()) {
    val profiles by viewModel.profiles.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val currentMode by viewModel.currentMode.collectAsState()
    val selectedProfileId by viewModel.selectedProfileId.collectAsState()
    val showDialog by viewModel.showAddDialog.collectAsState()
    val formState by viewModel.formState.collectAsState()

    val isProxyMode = currentMode == "proxy"
    val isProxyConnected = connectionStatus.state == ConnectionState.CONNECTED && isProxyMode
    val isVpnActive = connectionStatus.state == ConnectionState.CONNECTED && currentMode == "vpn"

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) viewModel.connect()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Прокси", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)) {
                            Text("SOCKS5", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAdd() }, containerColor = MaterialTheme.colorScheme.tertiary) {
                Icon(Icons.Default.Add, "Добавить")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Warning if VPN is active
            if (isVpnActive) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = VpnOrange.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = VpnOrange, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("VPN активен. Отключите VPN для использования прокси.",
                            style = MaterialTheme.typography.bodySmall, color = VpnOrange)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Connect button
            ProxyConnectionCard(
                state = if (isProxyMode) connectionStatus.state else ConnectionState.DISCONNECTED,
                connectedSince = if (isProxyConnected) connectionStatus.connectedSince else null,
                profileName = if (isProxyConnected) viewModel.let {
                    val mgr = (viewModel.getApplication<MyVpnApp>()).vpnConnectionManager
                    mgr.getConnectedProxyProfile()?.name
                } else null,
                selectedProfileId = selectedProfileId,
                enabled = !isVpnActive,
                onToggle = {
                    if (isVpnActive) return@ProxyConnectionCard
                    if (connectionStatus.state == ConnectionState.DISCONNECTED || connectionStatus.state == ConnectionState.ERROR) {
                        if (selectedProfileId == null) return@ProxyConnectionCard
                        val intent = viewModel.prepareVpn()
                        if (intent != null) vpnPermissionLauncher.launch(intent)
                        else viewModel.connect()
                    } else if (isProxyMode) {
                        viewModel.toggleConnection()
                    }
                }
            )

            Spacer(Modifier.height(20.dp))

            Text("Прокси-серверы", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            if (profiles.isEmpty()) {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Lan, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Text("Нет прокси-серверов", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        FilledTonalButton(onClick = { viewModel.showAdd() }) {
                            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Добавить прокси")
                        }
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                    items(profiles, key = { it.id }) { profile ->
                        ProxyCard(
                            profile = profile,
                            isSelected = selectedProfileId == profile.id,
                            isConnected = isProxyConnected && viewModel.let {
                                val mgr = (viewModel.getApplication<MyVpnApp>()).vpnConnectionManager
                                mgr.getConnectedProxyProfile()?.id == profile.id
                            },
                            onSelect = { viewModel.selectProfile(profile.id) },
                            onEdit = { viewModel.showEdit(profile) },
                            onDelete = { viewModel.deleteProfile(profile) }
                        )
                    }
                }
            }
        }
    }

    // Add/Edit dialog
    if (showDialog) {
        ProxyFormDialog(
            formState = formState,
            onDismiss = { viewModel.hideDialog() },
            onSave = { viewModel.saveProfile() },
            onUpdateName = viewModel::updateName,
            onUpdateAddress = viewModel::updateAddress,
            onUpdatePort = viewModel::updatePort,
            onUpdateUsername = viewModel::updateUsername,
            onUpdatePassword = viewModel::updatePassword
        )
    }
}

@Composable
fun ProxyConnectionCard(
    state: ConnectionState,
    connectedSince: Long?,
    profileName: String?,
    selectedProfileId: Long?,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    val statusColor by animateColorAsState(
        targetValue = when (state) {
            ConnectionState.CONNECTED -> VpnGreen
            ConnectionState.CONNECTING, ConnectionState.DISCONNECTING -> VpnOrange
            ConnectionState.ERROR -> VpnRed
            ConnectionState.DISCONNECTED -> Color.Gray
        }, label = "proxyColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse), label = "pulse"
    )
    val scale = if (state == ConnectionState.CONNECTING) pulseScale else 1f

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(100.dp).scale(scale).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(
                        if (enabled) statusColor else Color.Gray,
                        if (enabled) statusColor.copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.6f)
                    )))
                    .clickable(enabled = enabled && (selectedProfileId != null || state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING)) { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (state == ConnectionState.CONNECTED) Icons.Default.Lock else Icons.Default.LockOpen,
                    "Toggle", tint = Color.White, modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                when (state) {
                    ConnectionState.CONNECTED -> "Прокси активен"
                    ConnectionState.CONNECTING -> "Подключение..."
                    ConnectionState.DISCONNECTING -> "Отключение..."
                    ConnectionState.ERROR -> "Ошибка"
                    ConnectionState.DISCONNECTED -> "Отключён"
                },
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                color = if (enabled) statusColor else Color.Gray
            )

            if (profileName != null && state != ConnectionState.DISCONNECTED) {
                Text(profileName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (state == ConnectionState.CONNECTED && connectedSince != null) {
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)) {
                    Text("С ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(connectedSince))}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }

            if (selectedProfileId == null && state == ConnectionState.DISCONNECTED) {
                Spacer(Modifier.height(8.dp))
                Text("Выберите прокси-сервер", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ProxyCard(
    profile: ProxyProfile,
    isSelected: Boolean,
    isConnected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val borderColor = when {
        isConnected -> VpnGreen
        isSelected -> MaterialTheme.colorScheme.tertiary
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(if (isSelected || isConnected) 2.dp else 0.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(if (isConnected) VpnGreen else Color.Gray))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(profile.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${profile.serverAddress}:${profile.serverPort} · SOCKS5${if (profile.username.isNotEmpty()) " · Auth" else ""}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Edit, "Редактировать", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.Delete, "Удалить", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить прокси?") },
            text = { Text("\"${profile.name}\" будет удалён.") },
            confirmButton = { TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("Удалить", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") } }
        )
    }
}

@Composable
fun ProxyFormDialog(
    formState: ProxyFormState,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onUpdateName: (String) -> Unit,
    onUpdateAddress: (String) -> Unit,
    onUpdatePort: (String) -> Unit,
    onUpdateUsername: (String) -> Unit,
    onUpdatePassword: (String) -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (formState.isEditing) "Редактировать прокси" else "Новый прокси") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                formState.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }

                OutlinedTextField(value = formState.name, onValueChange = onUpdateName,
                    label = { Text("Название") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = formState.serverAddress, onValueChange = onUpdateAddress,
                        label = { Text("IP") }, singleLine = true, modifier = Modifier.weight(2f), shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
                    OutlinedTextField(value = formState.serverPort, onValueChange = onUpdatePort,
                        label = { Text("Порт") }, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }

                OutlinedTextField(value = formState.username, onValueChange = onUpdateUsername,
                    label = { Text("Логин") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))

                OutlinedTextField(value = formState.password, onValueChange = onUpdatePassword,
                    label = { Text("Пароль") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Toggle")
                        }
                    })
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
