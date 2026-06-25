package com.myvpn.client.ui.screens.logs

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.myvpn.client.MyVpnApp
import com.myvpn.client.data.model.LogEntry
import kotlinx.coroutines.flow.StateFlow

class LogsViewModel(application: Application) : AndroidViewModel(application) {

    private val vpnManager = (application as MyVpnApp).vpnConnectionManager

    val logs: StateFlow<List<LogEntry>> = vpnManager.logs

    fun clearLogs() {
        vpnManager.clearLogs()
    }
}
