package com.myvpn.client.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "proxy_profiles")
data class ProxyProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val serverAddress: String,
    val serverPort: Int = 1080,
    val username: String = "",
    val password: String = "",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastConnectedAt: Long? = null
)
