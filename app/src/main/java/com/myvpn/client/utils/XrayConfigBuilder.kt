package com.myvpn.client.utils

import com.myvpn.client.data.model.VpnProfile
import org.json.JSONArray
import org.json.JSONObject

object XrayConfigBuilder {

    fun buildConfig(profile: VpnProfile, socksPort: Int = 10808): String {
        val config = JSONObject()

        config.put("log", JSONObject().apply {
            put("loglevel", "warning")
        })

        config.put("dns", JSONObject().apply {
            put("servers", JSONArray().apply {
                put("8.8.8.8")
                put("8.8.4.4")
            })
        })

        // Single TUN inbound only
        config.put("inbounds", JSONArray().apply {
            put(JSONObject().apply {
                put("tag", "tun-in")
                put("protocol", "tun")
                put("listen", "127.0.0.1")
                put("port", socksPort)
                put("settings", JSONObject().apply {
                    put("network", "tcp,udp")
                })
            })
        })

        config.put("outbounds", JSONArray().apply {
            put(JSONObject().apply {
                put("tag", "proxy")
                put("protocol", "vless")
                put("settings", JSONObject().apply {
                    put("vnext", JSONArray().apply {
                        put(JSONObject().apply {
                            put("address", profile.serverAddress)
                            put("port", profile.serverPort.toString().toIntOrNull() ?: 8443)
                            put("users", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("id", profile.uuid)
                                    put("encryption", "none")
                                    put("flow", profile.flow)
                                })
                            })
                        })
                    })
                })
                put("streamSettings", JSONObject().apply {
                    put("network", profile.network)
                    put("security", profile.security)
                    if (profile.security == "reality") {
                        put("realitySettings", JSONObject().apply {
                            put("serverName", profile.sni)
                            put("publicKey", profile.publicKey)
                            put("shortId", profile.shortId)
                            put("fingerprint", profile.fingerprint)
                            put("spiderX", "")
                        })
                    } else if (profile.security == "tls") {
                        put("tlsSettings", JSONObject().apply {
                            put("serverName", profile.sni)
                            put("fingerprint", profile.fingerprint)
                        })
                    }
                })
            })

            put(JSONObject().apply {
                put("tag", "direct")
                put("protocol", "freedom")
            })

            put(JSONObject().apply {
                put("tag", "block")
                put("protocol", "blackhole")
            })
        })

        config.put("routing", JSONObject().apply {
            put("domainStrategy", "AsIs")
            put("rules", JSONArray().apply {
                put(JSONObject().apply {
                    put("type", "field")
                    put("inboundTag", JSONArray().apply { put("tun-in") })
                    put("outboundTag", "proxy")
                })
            })
        })
        return config.toString(2)
    }

    fun generateShareLink(profile: VpnProfile): String {
        val params = mutableListOf<String>()
        params.add("type=${profile.network}")
        params.add("security=${profile.security}")
        params.add("flow=${profile.flow}")

        if (profile.security == "reality") {
            params.add("sni=${profile.sni}")
            params.add("pbk=${profile.publicKey}")
            params.add("sid=${profile.shortId}")
            params.add("fp=${profile.fingerprint}")
        }

        val query = params.joinToString("&")
        val name = java.net.URLEncoder.encode(profile.name, "UTF-8")
        return "vless://${profile.uuid}@${profile.serverAddress}:${profile.serverPort}?$query#$name"
    }

    fun parseVlessUri(uri: String): VpnProfile? {
        try {
            if (!uri.startsWith("vless://")) return null

            val withoutScheme = uri.removePrefix("vless://")
            val fragmentSplit = withoutScheme.split("#", limit = 2)
            val name = if (fragmentSplit.size > 1)
                java.net.URLDecoder.decode(fragmentSplit[1], "UTF-8") else "Imported"

            val mainPart = fragmentSplit[0]
            val querySplit = mainPart.split("?", limit = 2)
            val params = if (querySplit.size > 1) parseQueryParams(querySplit[1]) else emptyMap()

            val userHost = querySplit[0]
            val atSplit = userHost.split("@", limit = 2)
            if (atSplit.size != 2) return null

            val uuid = atSplit[0]
            val hostPort = atSplit[1]
            val colonIdx = hostPort.lastIndexOf(":")
            if (colonIdx < 0) return null

            val address = hostPort.substring(0, colonIdx)
            val port = hostPort.substring(colonIdx + 1).toIntOrNull() ?: 443

            return VpnProfile(
                name = name,
                serverAddress = address,
                serverPort = port,
                uuid = uuid,
                flow = params["flow"] ?: "xtls-rprx-vision",
                security = params["security"] ?: "reality",
                sni = params["sni"] ?: "",
                publicKey = params["pbk"] ?: "",
                shortId = params["sid"] ?: "",
                fingerprint = params["fp"] ?: "chrome",
                network = params["type"] ?: "tcp"
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        return query.split("&").mapNotNull {
            val kv = it.split("=", limit = 2)
            if (kv.size == 2) kv[0] to kv[1] else null
        }.toMap()
    }
}
