package com.myvpn.client

import android.app.Application
import com.myvpn.client.data.db.AppDatabase
import com.myvpn.client.data.repository.ProxyProfileRepository
import com.myvpn.client.data.repository.VpnProfileRepository
import com.myvpn.client.service.VpnConnectionManager

class MyVpnApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: VpnProfileRepository
        private set

    lateinit var proxyRepository: ProxyProfileRepository
        private set

    lateinit var vpnConnectionManager: VpnConnectionManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        repository = VpnProfileRepository(database.vpnProfileDao())
        proxyRepository = ProxyProfileRepository(database.proxyProfileDao())
        vpnConnectionManager = VpnConnectionManager(this)
    }
}
