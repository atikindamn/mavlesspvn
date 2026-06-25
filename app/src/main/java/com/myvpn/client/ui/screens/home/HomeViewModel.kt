package com.myvpn.client.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myvpn.client.MyVpnApp
import com.myvpn.client.data.model.ConnectionState
import com.myvpn.client.data.model.ConnectionStatus
import com.myvpn.client.data.model.VpnProfile
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MyVpnApp
    private val repository = app.repository
    private val vpnManager = app.vpnConnectionManager

    val profiles: StateFlow<List<VpnProfile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val connectionStatus: StateFlow<ConnectionStatus> = vpnManager.connectionStatus
    val currentMode: StateFlow<String?> = vpnManager.currentMode

    private val _selectedProfileId = MutableStateFlow<Long?>(null)
    val selectedProfileId: StateFlow<Long?> = _selectedProfileId.asStateFlow()

    fun selectProfile(profileId: Long) { _selectedProfileId.value = profileId }

    fun connect() {
        viewModelScope.launch {
            val profileId = _selectedProfileId.value ?: return@launch
            val profile = repository.getProfileById(profileId) ?: return@launch
            vpnManager.connect(profile)
            repository.updateLastConnected(profileId)
        }
    }

    fun disconnect() { vpnManager.disconnect() }

    fun toggleConnection() {
        when (connectionStatus.value.state) {
            ConnectionState.DISCONNECTED, ConnectionState.ERROR -> connect()
            ConnectionState.CONNECTED -> disconnect()
            else -> {}
        }
    }

    fun deleteProfile(profile: VpnProfile) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
            if (_selectedProfileId.value == profile.id) _selectedProfileId.value = null
        }
    }

    fun prepareVpn() = vpnManager.prepareVpn()
}
