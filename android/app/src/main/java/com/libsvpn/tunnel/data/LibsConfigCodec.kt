package com.libsvpn.tunnel.data

import android.util.Base64
import com.libsvpn.tunnel.model.SecurityType
import com.libsvpn.tunnel.model.TransportType
import com.libsvpn.tunnel.model.TunnelProfile
import com.libsvpn.tunnel.model.TunnelType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object LibsConfigCodec {
    const val EXTENSION = ".libs"
    const val MIME_TYPE = "application/octet-stream"

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    data class ImportResult(
        val profiles: List<TunnelProfile>,
        val failures: List<String>
    ) {
        val single: TunnelProfile?
            get() = profiles.singleOrNull()
    }

    fun export(profile: TunnelProfile): String {
        val payload = json.encodeToString(TunnelProfile.serializer(), profile)
        return json.encodeToString(
            LibsEnvelope.serializer(),
            LibsEnvelope(
                version = 1,
                profile = payload
            )
        )
    }

    /** Import a single profile. Throws on failure. */
    fun import(raw: String): TunnelProfile {
        val result = importMany(raw)
        result.single?.let { return it }
        if (result.profiles.isNotEmpty()) return result.profiles.first()
        error(result.failures.firstOrNull() ?: "No profile found in the input")
    }

    /** Import one or more profiles; never throws. */
    fun importMany(raw: String, depth: Int = 0): ImportResult {
        val text = raw.removePrefix("﻿").trim()
        val profiles = mutableListOf<TunnelProfile>()
        val failures = mutableListOf<String>()

        if (text.isEmpty()) {
            failures += "Input is empty"
            return ImportResult(profiles, failures)
        }

        // .libs envelope or raw JSON.
        if (text.startsWith("{")) {
            runCatching { importLibs(text) }
                .onSuccess { profiles += it }
                .onFailure { failures += (it.message ?: "Invalid .libs file") }
            return ImportResult(profiles, failures)
        }

        // Split multiline pastes into per-line links.
        val lines = text.lines().map { it.trim() }
        val linkLines = lines.filter {
            it.isNotBlank() && SHARE_SCHEMES.any { scheme -> it.startsWith(scheme, true) }
        }

        if (linkLines.size > 1 || (linkLines.size == 1 && lines.any { it.isNotBlank() && SHARE_SCHEMES.none { s -> it.startsWith(s, true) } })) {
            // Mixed content: parse each recognized line, skip noise.
            linkLines.forEachIndexed { index, line ->
                runCatching { parseShareLink(line) }
                    .onSuccess { profiles += it }
                    .onFailure { failures += "Line ${index + 1}: ${it.message ?: "unrecognized format"}" }
            }
            return ImportResult(profiles, failures)
        }

        if (linkLines.size == 1) {
            runCatching { parseShareLink(linkLines.first()) }
                .onSuccess { profiles += it }
                .onFailure { failures += (it.message ?: "Could not parse the share link") }
            return ImportResult(profiles, failures)
        }

        // Bare Base64 subscription: decode once, then recurse on the decoded list.
        if (depth == 0) {
            runCatching {
                val decoded = String(decode64Flexible(text), StandardCharsets.UTF_8)
                if (SHARE_SCHEMES.any { decoded.contains(it, true) }) {
                    importMany(decoded, depth + 1)
                } else {
                    null
                }
            }.getOrNull()?.let { return it }
        }

        failures += "Unsupported format. Paste a VLESS/VMess/Trojan link or a .libs file."
        return ImportResult(profiles, failures)
    }

    private fun importLibs(text: String): List<TunnelProfile> {
        val envelope = json.decodeFromString(LibsEnvelope.serializer(), text)
        require(envelope.format == "libs-tunnel" && envelope.version in 1..2) { "Unsupported .libs format" }
        require(!envelope.encrypted) { "Encrypted configs are not supported" }
        val payload = requireNotNull(envelope.profile) { "Profile payload is missing" }
        val imported = if (envelope.version == 1) {
            listOf(json.decodeFromString(TunnelProfile.serializer(), payload))
        } else {
            json.decodeFromString(LibsBundle.serializer(), payload).profiles
        }
        return imported.map {
            it.copy(id = java.util.UUID.randomUUID().toString(), updatedAt = System.currentTimeMillis())
        }
    }

    private fun parseShareLink(link: String): TunnelProfile = when {
        link.startsWith("vless://", true) -> parseUriProfile(link, TunnelType.VLESS)
        link.startsWith("trojan://", true) -> parseUriProfile(link, TunnelType.TROJAN)
        link.startsWith("vmess://", true) -> parseVmess(link)
        else -> error("Unsupported scheme")
    }

    private fun parseUriProfile(raw: String, type: TunnelType): TunnelProfile {
        val uri = try {
            URI(raw.trim())
        } catch (e: Exception) {
            error("Invalid ${type.title} link: ${e.message}")
        }
        val query = parseQuery(uri.rawQuery)
        fun q(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
            query.entries.firstOrNull { it.key.equals(key, true) }?.value
        }
        val security = when (q("security")?.lowercase()) {
            "reality" -> SecurityType.REALITY
            "tls" -> SecurityType.TLS
            null, "", "none" -> if (type == TunnelType.TROJAN) SecurityType.TLS else SecurityType.NONE
            else -> SecurityType.NONE
        }
        val rawName = uri.rawFragment?.takeIf { it.isNotBlank() }
        return TunnelProfile(
            name = rawName?.let(::decode) ?: type.title,
            type = type,
            server = uri.host.orEmpty().removePrefix("[").removeSuffix("]"),
            port = if (uri.port > 0) uri.port else 443,
            userId = if (type == TunnelType.VLESS) decode(uri.rawUserInfo.orEmpty()) else "",
            password = if (type == TunnelType.TROJAN) decode(uri.rawUserInfo.orEmpty()) else "",
            transport = parseTransport(q("type")),
            security = security,
            serverName = q("sni", "peer").orEmpty(),
            hostHeader = q("host", "authority").orEmpty(),
            path = q("path") ?: "/",
            grpcServiceName = q("serviceName", "service_name", "path").orEmpty(),
            flow = q("flow").orEmpty(),
            alpn = q("alpn") ?: "h2,http/1.1",
            fingerprint = q("fp") ?: "chrome",
            allowInsecure = q("allowInsecure", "insecure", "allow_insecure") in TRUTHY,
            realityPublicKey = q("pbk", "publicKey").orEmpty(),
            realityShortId = q("sid", "shortId").orEmpty(),
            realitySpiderX = q("spx", "spiderX") ?: "/",
            encryption = q("encryption") ?: "none"
        )
    }

    private fun parseVmess(raw: String): TunnelProfile {
        val payload = raw.substringAfter("://").substringBefore('#').substringBefore('?').trim()
        val decoded = try {
            String(decode64Flexible(payload), StandardCharsets.UTF_8)
        } catch (e: Exception) {
            error("Invalid VMess Base64 payload")
        }
        val obj = try {
            json.parseToJsonElement(decoded).jsonObject
        } catch (e: Exception) {
            error("VMess payload is not valid JSON")
        }
        fun value(key: String) = obj[key]?.jsonPrimitive?.content.orEmpty()
        fun truthy(key: String) = value(key).lowercase() in TRUTHY
        return TunnelProfile(
            name = value("ps").ifBlank { "VMess import" },
            type = TunnelType.VMESS,
            server = value("add"),
            port = value("port").toIntOrNull() ?: 443,
            userId = value("id"),
            transport = parseTransport(value("net")),
            security = if (value("tls").isNotBlank() && !value("tls").equals("none", true) && !value("tls").equals("false", true)) SecurityType.TLS else SecurityType.NONE,
            serverName = value("sni"),
            hostHeader = value("host"),
            path = value("path").ifBlank { "/" },
            grpcServiceName = value("path"),
            alpn = value("alpn").ifBlank { "h2,http/1.1" },
            fingerprint = value("fp").ifBlank { "chrome" },
            allowInsecure = truthy("allowInsecure")
        )
    }

    private fun parseTransport(value: String?): TransportType = when (value?.lowercase()) {
        "ws", "websocket" -> TransportType.WEBSOCKET
        "grpc" -> TransportType.GRPC
        "httpupgrade", "http-upgrade" -> TransportType.HTTP_UPGRADE
        "xhttp", "splithttp", "split-http" -> TransportType.SPLIT_HTTP
        "kcp", "mkcp" -> TransportType.KCP
        else -> TransportType.TCP
    }

    private fun parseQuery(raw: String?): Map<String, String> = raw.orEmpty()
        .split('&')
        .filter { it.isNotBlank() }
        .associate { part -> decode(part.substringBefore('=')) to decode(part.substringAfter('=', "")) }

    internal fun decode64Flexible(value: String): ByteArray {
        val cleaned = value
            .filterNot { it.isWhitespace() }
            .replace('-', '+')
            .replace('_', '/')
        val withPadding = when (cleaned.length % 4) {
            0 -> cleaned
            2 -> "$cleaned=="
            3 -> "$cleaned="
            else -> error("Invalid Base64 length")
        }
        return try {
            Base64.decode(withPadding, Base64.DEFAULT)
        } catch (e: Exception) {
            error("Invalid Base64 content")
        }
    }

    private fun decode(value: String): String = try {
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    } catch (e: Exception) {
        value
    }

    private val SHARE_SCHEMES = listOf("vless://", "trojan://", "vmess://")
    private val TRUTHY = setOf("1", "true", "yes")
}

@Serializable
private data class LibsEnvelope(
    val format: String = "libs-tunnel",
    val version: Int = 1,
    val encrypted: Boolean = false,
    val profile: String? = null
)

@Serializable
private data class LibsBundle(
    val profiles: List<TunnelProfile>
)
