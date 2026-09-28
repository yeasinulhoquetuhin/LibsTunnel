package com.libsvpn.tunnel.core

import android.net.VpnService
import android.util.Log
import com.libsvpn.tunnel.bindings.vpncore.Platform
import com.libsvpn.tunnel.bindings.vpncore.Vpncore
import com.libsvpn.tunnel.service.VpnStateBus

class CoreBridge(private val service: VpnService) : Platform {
    private val manager = Vpncore.newManager(this)

    @Synchronized
    fun start(configJson: String) {
        manager.start(configJson)
    }

    @Synchronized
    fun startTun(fileDescriptor: Int, mtu: Int) {
        manager.startTun(fileDescriptor.toLong(), mtu.toLong())
    }

    @Synchronized
    fun stop() {
        manager.stop()
    }

    fun uploadTotal(): Long = manager.uploadTotal()

    fun downloadTotal(): Long = manager.downloadTotal()

    override fun log(message: String?) {
        Log.i(TAG, message.orEmpty())
        VpnStateBus.log(message.orEmpty())
    }

    override fun protect(socket: Long): Boolean = service.protect(socket.toInt())

    companion object {
        private const val TAG = "LibsTunnel"
    }
}
