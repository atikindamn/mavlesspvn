package com.myvpn.client.utils

import com.myvpn.client.data.model.ProxyProfile
import org.json.JSONArray
import org.json.JSONObject

object ProxyConfigBuilder {

    fun buildConfig(profile: ProxyProfile): String {
        val config = JSONObject()

        config.put("log", JSONObject().apply {
            put("loglevel", "warning")
        })

        // DNS over TCP
        config.put("dns", JSONObject().apply {
            put("servers", JSONArray().apply {
                put("tcp://1.1.1.1")
                put("tcp://1.0.0.1")
            })
            put("queryStrategy", "UseIP")
        })

        // TUN inbound with sniffing
        config.put("inbounds", JSONArray().apply {
            put(JSONObject().apply {
                put("tag", "tun-in")
                put("protocol", "tun")
                put("listen", "127.0.0.1")
                put("port", 10808)
                put("settings", JSONObject().apply {
                    put("network", "tcp,udp")
                })
                put("sniffing", JSONObject().apply {
                    put("enabled", true)
                    put("destOverride", JSONArray().apply {
                        put("http")
                        put("tls")
                        put("fakedns")
                    })
                })
            })
        })

        config.put("outbounds", JSONArray().apply {
            // SOCKS5 proxy outbound
            put(JSONObject().apply {
                put("tag", "proxy")
                put("protocol", "socks")
                put("settings", JSONObject().apply {
                    put("servers", JSONArray().apply {
                        put(JSONObject().apply {
                            put("address", profile.serverAddress)
                            put("port", profile.serverPort)
                            // Only add auth if both username and password are provided
                            // Add auth if username is provided (password can be empty)
                            if (profile.username.isNotEmpty()) {
                                put("users", JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("user", profile.username)
                                        put("pass", profile.password)
                                    })
                                })
                            }
                        })
                    })
                })
            })

            put(JSONObject().apply { put("tag", "direct"); put("protocol", "freedom") })
            put(JSONObject().apply { put("tag", "block"); put("protocol", "blackhole") })
            put(JSONObject().apply { put("tag", "dns-out"); put("protocol", "dns") })
        })

        // Routing: DNS -> dns-out, other UDP -> block, TCP -> proxy
        config.put("routing", JSONObject().apply {
            put("domainStrategy", "IPIfNonMatch")
            put("rules", JSONArray().apply {
                put(JSONObject().apply { put("type", "field"); put("port", 53); put("outboundTag", "dns-out") })
                put(JSONObject().apply { put("type", "field"); put("network", "udp"); put("outboundTag", "block") })
                put(JSONObject().apply { put("type", "field"); put("inboundTag", JSONArray().apply { put("tun-in") }); put("outboundTag", "proxy") })
            })
        })

        return config.toString(2)
    }
}
