package com.myvpn.client.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.myvpn.client.R
import com.myvpn.client.ui.MainActivity

class XrayVpnService : VpnService() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "vpn_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_CONNECT = "com.myvpn.client.CONNECT"
        const val ACTION_DISCONNECT = "com.myvpn.client.DISCONNECT"
        const val EXTRA_CONFIG = "config"

        var instance: XrayVpnService? = null
            private set
    }

    var vpnInterface: ParcelFileDescriptor? = null
        private set

    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                startForeground(NOTIFICATION_ID, createNotification("Подключение к VPN..."))
                val config = intent.getStringExtra(EXTRA_CONFIG) ?: ""
                if (config.isNotEmpty()) {
                    startVpn()
                }
            }
            ACTION_DISCONNECT -> {
                stopVpn()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopVpn()
        instance = null
        super.onDestroy()
    }

    override fun onRevoke() {
        // Another VPN app took over, or user revoked permission
        stopVpn()
        // Notify VpnConnectionManager
        val app = application as? com.myvpn.client.MyVpnApp
        app?.vpnConnectionManager?.disconnect()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        super.onRevoke()
    }

    fun startVpn(): ParcelFileDescriptor? {
        if (isRunning) return vpnInterface
        try {
            val builder = Builder()
                .setSession("ATnetTool - XRAY")
                .addAddress("10.1.0.1", 30)
                .addRoute("0.0.0.0", 0)
                .addRoute("::", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("1.0.0.1")
                .setMtu(1500)

            // Exclude our app from VPN to prevent loops
            try {
                builder.addDisallowedApplication(packageName)
            } catch (_: Exception) {}

            vpnInterface = builder.establish()
            isRunning = vpnInterface != null

            if (isRunning) {
                updateNotification("VPN подключён")
            }

            return vpnInterface
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun stopVpn() {
        isRunning = false
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (_: Exception) {}
    }

    fun protectSocket(fd: Int): Boolean {
        return protect(fd)
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification(text))
    }

    private fun createNotification(text: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("ATnetTool")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_vpn_key)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "VPN Connection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Статус VPN подключения"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
