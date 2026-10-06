package com.libsvpn.tunnel.engine

import com.libsvpn.tunnel.model.PayloadMode
import com.libsvpn.tunnel.model.SecurityType
import com.libsvpn.tunnel.model.TransportType
import com.libsvpn.tunnel.model.TunnelProfile
import com.libsvpn.tunnel.model.TunnelType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object EngineConfigBuilder {

    fun build(profile: TunnelProfile): String {
        val errors = profile.validationErrors()
        require(errors.isEmpty()) { errors.joinToString("\n") }
        return buildJsonObject {
            put("backend", "xray")
            put("xray", buildXray(profile))
            put("outboundTag", "proxy")
            put("injection", buildInjection(profile))
            put("tun", buildJsonObject {
                put("socksAddress", "127.0.0.1:10808")
                put("mtu", profile.mtu)
            })
        }.toString()
    }

    private fun buildXray(profile: TunnelProfile): JsonObject = buildJsonObject {
        put("log", buildJsonObject { put("loglevel", "warning") })
        if (profile.customDnsEnabled) {
            put("dns", buildJsonObject {
                put("servers", buildJsonArray {
                    if (profile.dnsPrimary.isNotBlank()) add(JsonPrimitive(profile.dnsPrimary))
                    if (profile.dnsSecondary.isNotBlank()) add(JsonPrimitive(profile.dnsSecondary))
                })
                put("queryStrategy", "UseIP")
            })
        }
        put("inbounds", buildJsonArray {
            add(buildJsonObject {
                put("tag", "socks-in")
                put("listen", "127.0.0.1")
                put("port", 10808)
                put("protocol", "socks")
                put("settings", buildJsonObject {
                    put("auth", "noauth")
                    put("udp", true)
                })
                put("sniffing", buildJsonObject {
                    put("enabled", true)
                    put("destOverride", JsonArray(listOf(JsonPrimitive("http"), JsonPrimitive("tls"), JsonPrimitive("quic"))))
                    put("routeOnly", true)
                })
            })
        })
        put("outbounds", buildJsonArray {
            add(buildProxyOutbound(profile))
            add(buildJsonObject {
                put("tag", "direct")
                put("protocol", "freedom")
            })
            add(buildJsonObject {
                put("tag", "blocked")
                put("protocol", "blackhole")
            })
        })
        put("routing", buildJsonObject {
            put("domainStrategy", "IPIfNonMatch")
            put("rules", buildJsonArray {
                add(buildJsonObject {
                    put("type", "field")
                    put("ip", JsonArray(PRIVATE_CIDRS.map(::JsonPrimitive)))
                    put("outboundTag", "direct")
                })
            })
        })
    }

    private fun buildProxyOutbound(profile: TunnelProfile): JsonObject = buildJsonObject {
        put("tag", "proxy")
        put("protocol", profile.type.name.lowercase())
        put("settings", when (profile.type) {
            TunnelType.VLESS, TunnelType.VMESS -> buildJsonObject {
                put("vnext", buildJsonArray {
                    add(buildJsonObject {
                        put("address", profile.server)
                        put("port", profile.port)
                        put("users", buildJsonArray {
                            add(buildJsonObject {
                                put("id", profile.userId.trim())
                                if (profile.type == TunnelType.VLESS) {
                                    put("encryption", profile.encryption.ifBlank { "none" })
                                    if (profile.flow.isNotBlank()) put("flow", profile.flow)
                                } else {
                                    put("alterId", 0)
                                    put("security", "auto")
                                }
                            })
                        })
                    })
                })
            }
            TunnelType.TROJAN -> buildJsonObject {
                put("servers", buildJsonArray {
                    add(buildJsonObject {
                        put("address", profile.server)
                        put("port", profile.port)
                        put("password", profile.password)
                    })
                })
            }
        })
        put("streamSettings", buildStreamSettings(profile))
        put("mux", buildJsonObject { put("enabled", false) })
    }

    private fun buildStreamSettings(profile: TunnelProfile): JsonObject = buildJsonObject {
        val network = when (profile.transport) {
            TransportType.TCP -> "tcp"
            TransportType.WEBSOCKET -> "ws"
            TransportType.GRPC -> "grpc"
            TransportType.HTTP_UPGRADE -> "httpupgrade"
            TransportType.SPLIT_HTTP -> "xhttp"
            TransportType.KCP -> "kcp"
        }
        put("network", network)
        when (profile.transport) {
            TransportType.WEBSOCKET -> put("wsSettings", pathAndHeaders(profile))
            TransportType.GRPC -> put("grpcSettings", buildJsonObject {
                put("serviceName", profile.grpcServiceName)
                put("multiMode", false)
            })
            TransportType.HTTP_UPGRADE -> put("httpupgradeSettings", pathAndHost(profile))
            TransportType.SPLIT_HTTP -> put("xhttpSettings", pathAndHost(profile))
            TransportType.KCP -> put("kcpSettings", buildJsonObject {
                put("header", buildJsonObject { put("type", "none") })
            })
            TransportType.TCP -> Unit
        }
        when (profile.security) {
            SecurityType.NONE -> put("security", "none")
            SecurityType.TLS -> {
                put("security", "tls")
                put("tlsSettings", buildJsonObject {
                    put("serverName", profile.serverName.ifBlank { profile.server })
                    put("allowInsecure", profile.allowInsecure)
                    if (profile.fingerprint.isNotBlank()) put("fingerprint", profile.fingerprint)
                    val alpn = profile.alpn.split(',').map(String::trim).filter(String::isNotBlank)
                    if (alpn.isNotEmpty()) put("alpn", JsonArray(alpn.map(::JsonPrimitive)))
                })
            }
            SecurityType.REALITY -> {
                put("security", "reality")
                put("realitySettings", buildJsonObject {
                    put("serverName", profile.serverName.ifBlank { profile.server })
                    put("fingerprint", profile.fingerprint.ifBlank { "chrome" })
                    put("publicKey", profile.realityPublicKey)
                    put("shortId", profile.realityShortId)
                    put("spiderX", profile.realitySpiderX.ifBlank { "/" })
                })
            }
        }
    }

    private fun pathAndHeaders(profile: TunnelProfile) = buildJsonObject {
        put("path", profile.path.ifBlank { "/" })
        if (profile.hostHeader.isNotBlank()) {
            put("headers", buildJsonObject { put("Host", profile.hostHeader) })
        }
    }

    private fun pathAndHost(profile: TunnelProfile) = buildJsonObject {
        put("path", profile.path.ifBlank { "/" })
        if (profile.hostHeader.isNotBlank()) put("host", profile.hostHeader)
    }

    private fun buildInjection(profile: TunnelProfile): JsonObject = buildJsonObject {
        put("enabled", profile.payloadEnabled)
        put("mode", profile.payloadMode.engineValue)
        if (profile.payloadMode == PayloadMode.PROXY) {
            put("proxyHost", profile.proxyHost)
            put("proxyPort", profile.proxyPort)
        }
        put("payload", profile.payload)
        put("responseMode", profile.responseMode)
        put("acceptedStatus", JsonArray(listOf(JsonPrimitive(101), JsonPrimitive(200))))
        put("method", "CONNECT")
        put("protocol", "HTTP/1.1")
        put("userAgent", "libs-tunnel/1.0")
    }

    // geoip.dat is not bundled in the APK, so use literal RFC1918/ULA/private
    // CIDRs instead of the "geoip:private" symbol that crashed at startup.
    private val PRIVATE_CIDRS = listOf(
        "0.0.0.0/8",
        "10.0.0.0/8",
        "100.64.0.0/10",
        "127.0.0.0/8",
        "169.254.0.0/16",
        "172.16.0.0/12",
        "192.0.0.0/24",
        "192.0.2.0/24",
        "192.168.0.0/16",
        "198.18.0.0/15",
        "198.51.100.0/24",
        "203.0.113.0/24",
        "224.0.0.0/4",
        "240.0.0.0/4",
        "::1/128",
        "fc00::/7",
        "fe80::/10"
    )

}
