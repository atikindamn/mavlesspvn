package com.myvpn.client.ui.screens.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.myvpn.client.MyVpnApp
import com.myvpn.client.data.model.VpnProfile
import com.myvpn.client.utils.XrayConfigBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileFormState(
    val name: String = "",
    val serverAddress: String = "",
    val serverPort: String = "8443",
    val uuid: String = "",
    val flow: String = "xtls-rprx-vision",
    val security: String = "reality",
    val sni: String = "www.cloudflare.com",
    val publicKey: String = "",
    val shortId: String = "",
    val fingerprint: String = "chrome",
    val network: String = "tcp",
    val vlessLink: String = "",
    val isEditing: Boolean = false,
    val editingId: Long = 0,
    val isSaving: Boolean = false,
    val error: String? = null
)

class ProfileViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val repository = (application as MyVpnApp).repository

    private val _formState = MutableStateFlow(ProfileFormState())
    val formState: StateFlow<ProfileFormState> = _formState.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    fun loadProfile(profileId: Long) {
        if (profileId <= 0) return
        viewModelScope.launch {
            repository.getProfileById(profileId)?.let { profile ->
                _formState.value = ProfileFormState(
                    name = profile.name,
                    serverAddress = profile.serverAddress,
                    serverPort = profile.serverPort.toString(),
                    uuid = profile.uuid,
                    flow = profile.flow,
                    security = profile.security,
                    sni = profile.sni,
                    publicKey = profile.publicKey,
                    shortId = profile.shortId,
                    fingerprint = profile.fingerprint,
                    network = profile.network,
                    isEditing = true,
                    editingId = profile.id
                )
            }
        }
    }

    fun updateName(v: String) { _formState.value = _formState.value.copy(name = v, error = null) }
    fun updateServerAddress(v: String) { _formState.value = _formState.value.copy(serverAddress = v.trim(), error = null) }
    fun updateServerPort(v: String) { _formState.value = _formState.value.copy(serverPort = v.filter { it.isDigit() }, error = null) }
    fun updateUuid(v: String) { _formState.value = _formState.value.copy(uuid = v.trim(), error = null) }
    fun updateSni(v: String) { _formState.value = _formState.value.copy(sni = v.trim(), error = null) }
    fun updatePublicKey(v: String) { _formState.value = _formState.value.copy(publicKey = v.trim(), error = null) }
    fun updateShortId(v: String) { _formState.value = _formState.value.copy(shortId = v.trim(), error = null) }
    fun updateFingerprint(v: String) { _formState.value = _formState.value.copy(fingerprint = v) }
    fun updateVlessLink(v: String) { _formState.value = _formState.value.copy(vlessLink = v, error = null) }

    fun importFromLink() {
        val link = _formState.value.vlessLink.trim()
        if (link.isEmpty()) {
            _formState.value = _formState.value.copy(error = "Вставьте vless:// ссылку")
            return
        }
        val profile = XrayConfigBuilder.parseVlessUri(link)
        if (profile == null) {
            _formState.value = _formState.value.copy(error = "Неверный формат ссылки")
            return
        }
        _formState.value = _formState.value.copy(
            name = profile.name, serverAddress = profile.serverAddress,
            serverPort = profile.serverPort.toString(), uuid = profile.uuid,
            flow = profile.flow, security = profile.security, sni = profile.sni,
            publicKey = profile.publicKey, shortId = profile.shortId,
            fingerprint = profile.fingerprint, network = profile.network, error = null
        )
    }

    fun saveProfile() {
        val state = _formState.value
        if (state.name.isBlank()) { _formState.value = state.copy(error = "Введите название"); return }
        if (state.serverAddress.isBlank()) { _formState.value = state.copy(error = "Введите адрес сервера"); return }
        val port = state.serverPort.toIntOrNull()
        if (port == null || port !in 1..65535) { _formState.value = state.copy(error = "Порт: 1-65535"); return }
        if (state.uuid.isBlank()) { _formState.value = state.copy(error = "Введите UUID"); return }
        if (state.security == "reality" && state.publicKey.isBlank()) { _formState.value = state.copy(error = "Нужен Public Key"); return }

        viewModelScope.launch {
            _formState.value = state.copy(isSaving = true)
            try {
                val profile = VpnProfile(
                    id = if (state.isEditing) state.editingId else 0,
                    name = state.name.trim(), serverAddress = state.serverAddress.trim(),
                    serverPort = port, uuid = state.uuid.trim(), flow = state.flow,
                    security = state.security, sni = state.sni.trim(),
                    publicKey = state.publicKey.trim(), shortId = state.shortId.trim(),
                    fingerprint = state.fingerprint, network = state.network
                )
                repository.saveProfile(profile)
                _saved.value = true
            } catch (e: Exception) {
                _formState.value = state.copy(error = "Ошибка: ${e.message}", isSaving = false)
            }
        }
    }
}
