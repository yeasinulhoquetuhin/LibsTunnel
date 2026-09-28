package com.libsvpn.tunnel.service

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.core.content.ContextCompat
import com.libsvpn.tunnel.model.AppRoutingMode
import com.libsvpn.tunnel.model.TunnelProfile

object VpnCommands {
    const val ACTION_START = "com.libsvpn.tunnel.action.START"
    const val ACTION_STOP = "com.libsvpn.tunnel.action.STOP"
    const val EXTRA_CONFIG_JSON = "config_json"
    const val EXTRA_MTU = "mtu"
    const val EXTRA_PROFILE_NAME = "profile_name"
    const val EXTRA_DNS_PRIMARY = "dns_primary"
    const val EXTRA_DNS_SECONDARY = "dns_secondary"
    const val EXTRA_ROUTING_MODE = "routing_mode"
    const val EXTRA_APPLICATIONS = "applications"
    const val EXTRA_KEEP_SCREEN_ON = "keep_screen_on"

    fun permissionIntent(context: Context): Intent? = VpnService.prepare(context)

    fun start(context: Context, configJson: String, profile: TunnelProfile, keepScreenOn: Boolean = false) {
        val intent = Intent(context, HeadlessVpnService::class.java)
            .setAction(ACTION_START)
            .putExtra(EXTRA_CONFIG_JSON, configJson)
            .putExtra(EXTRA_MTU, profile.mtu)
            .putExtra(EXTRA_PROFILE_NAME, profile.name)
            .putExtra(EXTRA_DNS_PRIMARY, if (profile.customDnsEnabled) profile.dnsPrimary else "1.1.1.1")
            .putExtra(EXTRA_DNS_SECONDARY, if (profile.customDnsEnabled) profile.dnsSecondary else "8.8.8.8")
            .putExtra(EXTRA_ROUTING_MODE, profile.appRoutingMode.name)
            .putStringArrayListExtra(EXTRA_APPLICATIONS, ArrayList(profile.applications))
            .putExtra(EXTRA_KEEP_SCREEN_ON, keepScreenOn)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stop(context: Context) {
        context.startService(
            Intent(context, HeadlessVpnService::class.java).setAction(ACTION_STOP)
        )
    }
}
