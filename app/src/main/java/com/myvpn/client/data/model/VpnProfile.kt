package com.myvpn.client.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vpn_profiles")
data class VpnProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val serverAddress: String,
    val serverPort: Int = 8443,
    val uuid: String,
    val flow: String = "xtls-rprx-vision",
    val security: String = "reality",
    val sni: String = "www.cloudflare.com",
    val publicKey: String = "",
    val shortId: String = "",
    val fingerprint: String = "chrome",
    val network: String = "tcp",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastConnectedAt: Long? = null
)
