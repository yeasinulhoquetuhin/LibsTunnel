package com.libsvpn.tunnel.model

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class TunnelType(val title: String) {
    VLESS("VLESS"),
    VMESS("VMess"),
    TROJAN("Trojan")
}

@Serializable
enum class TransportType(val title: String) {
    TCP("TCP"),
    WEBSOCKET("WebSocket"),
    GRPC("gRPC"),
    HTTP_UPGRADE("HTTP Upgrade"),
    SPLIT_HTTP("SplitHTTP / XHTTP"),
    KCP("mKCP")
}

@Serializable
enum class SecurityType(val title: String) {
    NONE("None"),
    TLS("TLS"),
    REALITY("REALITY")
}

@Serializable
enum class PayloadMode(val title: String, val engineValue: String) {
    DIRECT("Direct", "direct"),
    PROXY("Proxy", "proxy")
}

@Serializable
enum class AppRoutingMode(val title: String) {
    ALL("All apps"),
    INCLUDE("Only selected"),
    EXCLUDE("Exclude selected")
}

@Serializable
data class TunnelProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "New tunnel",
    val type: TunnelType = TunnelType.VLESS,
    val server: String = "",
    val port: Int = 0,
    val userId: String = "",
    val password: String = "",
    val encryption: String = "none",
    val flow: String = "",
    val transport: TransportType = TransportType.TCP,
    val security: SecurityType = SecurityType.TLS,
    val serverName: String = "",
    val hostHeader: String = "",
    val path: String = "/",
    val grpcServiceName: String = "",
    val alpn: String = "h2,http/1.1",
    val fingerprint: String = "chrome",
    val realityPublicKey: String = "",
    val realityShortId: String = "",
    val realitySpiderX: String = "/",
    val allowInsecure: Boolean = false,
    val payloadEnabled: Boolean = false,
    val payloadMode: PayloadMode = PayloadMode.DIRECT,
    val proxyHost: String = "",
    val proxyPort: Int = 8080,
    val payload: String = "[method] [host_port] [protocol][crlf]Host: [host][crlf][crlf]",
    val responseMode: String = "headers",
    val customDnsEnabled: Boolean = false,
    val dnsPrimary: String = "1.1.1.1",
    val dnsSecondary: String = "8.8.8.8",
    val mtu: Int = 1500,
    val appRoutingMode: AppRoutingMode = AppRoutingMode.ALL,
    val applications: Set<String> = emptySet(),
    val locked: Boolean = false,
    val lockMessage: String = "Managed configuration",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun endpoint(): String = if (server.isBlank() || port !in 1..65535) "Not configured" else "$server:$port"

    fun validationErrors(): List<String> = buildList {
        if (name.isBlank()) add("Profile name is required")
        if (mtu !in 1280..9000) add("MTU must be between 1280 and 9000")
        if (server.isBlank()) add("Server is required")
        if (port !in 1..65535) add("Server port is invalid")
        when (type) {
            TunnelType.VLESS, TunnelType.VMESS -> if (userId.isBlank()) add("User ID is required")
            TunnelType.TROJAN -> if (password.isBlank()) add("Password is required")
        }
        if (security == SecurityType.REALITY && realityPublicKey.isBlank()) {
            add("REALITY public key is required")
        }
        if (payloadEnabled && payloadMode == PayloadMode.PROXY) {
            if (proxyHost.isBlank() || proxyPort !in 1..65535) add("Payload proxy is invalid")
        }
        if (appRoutingMode == AppRoutingMode.INCLUDE && applications.isEmpty()) {
            add("Select at least one app for include routing")
        }
    }
}

@Serializable
enum class ThemeMode(val title: String) {
    SYSTEM("Follow system"),
    LIGHT("Light"),
    DARK("Dark")
}

@Serializable
enum class AccentColor(val title: String, val rgb: Long) {
    GREEN("Green", 0xFF006C49),
    BLUE("Blue", 0xFF0B57D0),
    PURPLE("Purple", 0xFF6750A4),
    ORANGE("Orange", 0xFF9A4522),
    RED("Red", 0xFFB3261E),
    TEAL("Teal", 0xFF006A6A),
    PINK("Pink", 0xFF9C3D70)
}

@Serializable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentColor = AccentColor.GREEN,
    val dynamicColor: Boolean = false,
    val showConnectionStats: Boolean = true,
    val keepScreenOn: Boolean = false,
    val hapticFeedback: Boolean = true
)

@Serializable
data class ProfileStore(
    val profiles: List<TunnelProfile> = emptyList(),
    val selectedId: String? = null,
    val settings: AppSettings = AppSettings()
) {
    val selected: TunnelProfile?
        get() = profiles.firstOrNull { it.id == selectedId } ?: profiles.firstOrNull()
}
