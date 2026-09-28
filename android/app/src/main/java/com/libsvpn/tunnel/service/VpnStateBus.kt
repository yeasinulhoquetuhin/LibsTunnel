package com.libsvpn.tunnel.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class VpnStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    STOPPING,
    ERROR
}

data class VpnRuntimeState(
    val status: VpnStatus = VpnStatus.DISCONNECTED,
    val profileName: String = "",
    val connectedAt: Long = 0L,
    val uploadBytes: Long = 0L,
    val downloadBytes: Long = 0L,
    val lastError: String? = null,
    val logs: List<String> = emptyList()
)

object VpnStateBus {
    private val mutableState = MutableStateFlow(VpnRuntimeState())
    val state = mutableState.asStateFlow()
    private val time = SimpleDateFormat("HH:mm:ss", Locale.US)

    fun connecting(profileName: String) {
        mutableState.update {
            it.copy(
                status = VpnStatus.CONNECTING,
                profileName = profileName,
                uploadBytes = 0,
                downloadBytes = 0,
                lastError = null
            )
        }
        log("Starting $profileName")
    }

    fun connected() {
        mutableState.update { it.copy(status = VpnStatus.CONNECTED, connectedAt = System.currentTimeMillis()) }
        log("Tunnel connected")
    }

    fun stopping() {
        mutableState.update { it.copy(status = VpnStatus.STOPPING) }
        log("Stopping tunnel")
    }

    fun disconnected() {
        mutableState.update {
            it.copy(
                status = VpnStatus.DISCONNECTED,
                profileName = "",
                connectedAt = 0,
                uploadBytes = 0,
                downloadBytes = 0
            )
        }
        log("Tunnel stopped")
    }

    fun error(error: Throwable) {
        val message = error.message ?: error.javaClass.simpleName
        mutableState.update { it.copy(status = VpnStatus.ERROR, lastError = message) }
        log("Error: $message")
    }

    fun stats(upload: Long, download: Long) {
        mutableState.update { it.copy(uploadBytes = upload, downloadBytes = download) }
    }

    fun log(message: String) {
        if (message.isBlank()) return
        val line = "${time.format(Date())}  $message"
        mutableState.update { it.copy(logs = (it.logs + line).takeLast(250)) }
    }

    fun clearLogs() {
        mutableState.update { it.copy(logs = emptyList()) }
    }
}
