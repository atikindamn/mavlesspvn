package com.myvpn.client.ui.screens.router

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.wifi.WifiManager
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myvpn.client.ui.theme.VpnGreen
import com.myvpn.client.ui.theme.VpnOrange
import com.myvpn.client.utils.AiHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ==================== Data ====================

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class RouterGuideState(
    val gatewayIp: String = "192.168.0.1",
    val currentUrl: String = "",
    val isChatOpen: Boolean = false,
    val isAiLoading: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = ""
)

// ==================== ViewModel ====================

class RouterGuideViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(RouterGuideState())
    val state: StateFlow<RouterGuideState> = _state.asStateFlow()

    var webViewRef: WebView? = null
    private var lastScreenshot: Bitmap? = null

    init {
        detectGateway()
    }

    @Suppress("DEPRECATION")
    private fun detectGateway() {
        try {
            val ctx = getApplication<Application>()
            val wifiManager = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val dhcp = wifiManager.dhcpInfo
            if (dhcp.gateway != 0) {
                val ip = "${dhcp.gateway and 0xFF}.${dhcp.gateway shr 8 and 0xFF}.${dhcp.gateway shr 16 and 0xFF}.${dhcp.gateway shr 24 and 0xFF}"
                _state.value = _state.value.copy(gatewayIp = ip)
            }
        } catch (_: Exception) {}
    }

    fun updateUrl(url: String) {
        _state.value = _state.value.copy(currentUrl = url)
    }

    fun toggleChat() {
        _state.value = _state.value.copy(isChatOpen = !_state.value.isChatOpen)
    }

    fun updateInput(text: String) {
        _state.value = _state.value.copy(inputText = text)
    }

    fun captureAndAsk() {
        val message = _state.value.inputText.trim()
        if (message.isEmpty()) return

        _state.value = _state.value.copy(
            inputText = "",
            messages = _state.value.messages + ChatMessage(message, isUser = true),
            isAiLoading = true
        )

        // Capture screenshot from WebView
        webViewRef?.let { wv ->
            try {
                val bitmap = Bitmap.createBitmap(wv.width, wv.height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                wv.draw(canvas)
                lastScreenshot = bitmap
            } catch (_: Exception) {
                lastScreenshot = null
            }
        }

        viewModelScope.launch {
            try {
                val history = _state.value.messages.dropLast(1).map { msg ->
                    (if (msg.isUser) "user" else "model") to msg.text
                }

                val response = if (lastScreenshot != null) {
                    AiHelper.askWithScreenshot(lastScreenshot!!, message, history)
                } else {
                    AiHelper.askTextOnly(message)
                }

                _state.value = _state.value.copy(
                    messages = _state.value.messages + ChatMessage(response, isUser = false),
                    isAiLoading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    messages = _state.value.messages + ChatMessage("Ошибка: ${e.message}", isUser = false),
                    isAiLoading = false
                )
            }
        }
    }

    fun askQuickQuestion(question: String) {
        _state.value = _state.value.copy(inputText = question, isChatOpen = true)
        captureAndAsk()
    }
}

// ==================== UI ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouterGuideScreen(
    onNavigateBack: () -> Unit,
    viewModel: RouterGuideViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("${state.gatewayIp}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад") } },
                actions = {
                    IconButton(onClick = { viewModel.webViewRef?.reload() }) { Icon(Icons.Default.Refresh, "Обновить") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            // WebView
            AndroidView(
                modifier = Modifier.fillMaxSize().background(Color.White),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            @Suppress("DEPRECATION")
                            allowUniversalAccessFromFileURLs = true
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                url?.let { viewModel.updateUrl(it) }
                            }
                            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
                                // Many routers use self-signed certs
                                handler?.proceed()
                            }
                        }
                        webChromeClient = WebChromeClient()
                        viewModel.webViewRef = this
                        loadUrl("http://${state.gatewayIp}")
                    }
                }
            )

            // Quick action buttons (when chat is closed)
            if (!state.isChatOpen) {
                Column(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Quick questions
                    QuickActionChip("Как сменить пароль WiFi?") { viewModel.askQuickQuestion(it) }
                    QuickActionChip("Как сменить имя сети?") { viewModel.askQuickQuestion(it) }
                    QuickActionChip("Где я сейчас?") { viewModel.askQuickQuestion("Что это за страница? Опиши что ты видишь и подскажи что тут можно настроить.") }

                    // Main AI button
                    FloatingActionButton(
                        onClick = { viewModel.toggleChat() },
                        containerColor = VpnOrange,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.SmartToy, "AI Помощник", tint = Color.White)
                    }
                }
            }

            // Chat overlay
            AnimatedVisibility(
                visible = state.isChatOpen,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ChatPanel(
                    messages = state.messages,
                    inputText = state.inputText,
                    isLoading = state.isAiLoading,
                    onInputChange = { viewModel.updateInput(it) },
                    onSend = { viewModel.captureAndAsk() },
                    onClose = { viewModel.toggleChat() }
                )
            }
        }
    }
}

@Composable
fun QuickActionChip(text: String, onClick: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.clickable { onClick(text) }
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun ChatPanel(
    messages: List<ChatMessage>,
    inputText: String,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onClose: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Card(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.55f),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SmartToy, null, tint = VpnOrange, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("AI Помощник", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, "Закрыть", modifier = Modifier.size(20.dp))
                }
            }

            Divider()

            // Messages
            if (messages.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Задайте вопрос о настройке роутера", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("AI проанализирует текущую страницу", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { message ->
                        MessageBubble(message)
                    }
                    if (isLoading) {
                        item {
                            Row(Modifier.padding(start = 8.dp)) {
                                Text("AI думает...", style = MaterialTheme.typography.bodySmall, color = VpnOrange)
                            }
                        }
                    }
                }
            }

            // Input
            Divider()
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Спросите AI...", style = MaterialTheme.typography.bodySmall) },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onSend,
                    enabled = inputText.isNotBlank() && !isLoading,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(
                        if (inputText.isNotBlank()) VpnOrange else Color.Gray.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Отправить", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val bgColor = if (message.isUser)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    val alignment = if (message.isUser) Alignment.End else Alignment.Start

    Column(Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp, topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            color = bgColor,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                message.text,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = if (message.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
