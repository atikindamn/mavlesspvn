package com.myvpn.client.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileId: Long? = null,
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val formState by viewModel.formState.collectAsState()
    val saved by viewModel.saved.collectAsState()
    var showAdvanced by remember { mutableStateOf(false) }

    LaunchedEffect(profileId) { profileId?.let { viewModel.loadProfile(it) } }
    LaunchedEffect(saved) { if (saved) onNavigateBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (formState.isEditing) "Редактировать" else "Новый сервер",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
            // Error
            formState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        error,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Import from vless:// link
            if (!formState.isEditing) {
                Text(
                    "Импорт из ссылки",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = formState.vlessLink,
                    onValueChange = viewModel::updateVlessLink,
                    label = { Text("vless:// ссылка") },
                    placeholder = { Text("vless://uuid@host:port?params#name") },
                    leadingIcon = { Icon(Icons.Outlined.Link, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                FilledTonalButton(
                    onClick = viewModel::importFromLink,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Импортировать")
                }

                Divider(modifier = Modifier.padding(vertical = 4.dp))
            }

            // Profile name
            Text(
                "Основные",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = formState.name,
                onValueChange = viewModel::updateName,
                label = { Text("Название") },
                placeholder = { Text("My VLESS Server") },
                leadingIcon = { Icon(Icons.Outlined.Label, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Server
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = formState.serverAddress,
                    onValueChange = viewModel::updateServerAddress,
                    label = { Text("IP / Хост") },
                    placeholder = { Text("192.168.0.0") },
                    leadingIcon = { Icon(Icons.Outlined.Dns, null) },
                    modifier = Modifier.weight(2f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                OutlinedTextField(
                    value = formState.serverPort,
                    onValueChange = viewModel::updateServerPort,
                    label = { Text("Порт") },
                    leadingIcon = { Icon(Icons.Outlined.Tag, null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // UUID
            OutlinedTextField(
                value = formState.uuid,
                onValueChange = viewModel::updateUuid,
                label = { Text("UUID") },
                placeholder = { Text("0bb00-f000-4c00-aaaa-38533464") },
                leadingIcon = { Icon(Icons.Outlined.Key, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // REALITY settings
            Text(
                "REALITY",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )

            OutlinedTextField(
                value = formState.publicKey,
                onValueChange = viewModel::updatePublicKey,
                label = { Text("Public Key") },
                leadingIcon = { Icon(Icons.Outlined.VpnKey, null) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = formState.shortId,
                    onValueChange = viewModel::updateShortId,
                    label = { Text("Short ID") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = formState.sni,
                    onValueChange = viewModel::updateSni,
                    label = { Text("SNI") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Advanced toggle
            TextButton(onClick = { showAdvanced = !showAdvanced }) {
                Icon(
                    if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    null, Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(if (showAdvanced) "Скрыть дополнительные" else "Дополнительные настройки")
            }

            if (showAdvanced) {
                // Fingerprint selector
                Text("Fingerprint", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("chrome", "firefox", "safari").forEach { fp ->
                        FilterChip(
                            selected = formState.fingerprint == fp,
                            onClick = { viewModel.updateFingerprint(fp) },
                            label = { Text(fp) },
                            modifier = Modifier.weight(1f),
                            leadingIcon = if (formState.fingerprint == fp) {
                                { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Save button
            Button(
                onClick = viewModel::saveProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !formState.isSaving
            ) {
                if (formState.isSaving) {
                    // Simple loading text instead of CircularProgressIndicator
                    // to avoid Compose animation version incompatibility crash
                    Text(
                        "Сохранение...",
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Icon(Icons.Default.Save, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (formState.isEditing) "Сохранить" else "Добавить сервер",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
