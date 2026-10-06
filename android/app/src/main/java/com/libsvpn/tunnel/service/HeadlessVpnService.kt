package com.libsvpn.tunnel.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.libsvpn.tunnel.MainActivity
import com.libsvpn.tunnel.R
import com.libsvpn.tunnel.core.CoreBridge
import com.libsvpn.tunnel.model.AppRoutingMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class HeadlessVpnService : VpnService() {
    private val worker = Executors.newSingleThreadExecutor()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var core: CoreBridge
    private var tun: ParcelFileDescriptor? = null
    private var statsJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var activeProfileName = "Libs Tunnel"
    private var lastStartArgs: StartArgs? = null

    private data class StartArgs(
        val config: String?,
        val mtu: Int,
        val dnsPrimary: String,
        val dnsSecondary: String,
        val routingMode: String,
        val applications: List<String>,
        val keepAwake: Boolean
    )

    override fun onCreate() {
        super.onCreate()
        core = CoreBridge(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            VpnCommands.ACTION_STOP -> worker.execute { stopVpn() }
            VpnCommands.ACTION_RECONNECT -> {
                val args = lastStartArgs
                if (args == null) {
                    VpnStateBus.log("Reconnect unavailable: no saved connection")
                } else {
                    VpnStateBus.connecting(activeProfileName)
                    startForeground(NOTIFICATION_ID, notification(getString(R.string.vpn_connecting)))
                    worker.execute {
                        startVpn(
                            args.config, args.mtu, args.dnsPrimary, args.dnsSecondary,
                            args.routingMode, args.applications, args.keepAwake
                        )
                    }
                }
            }
            VpnCommands.ACTION_START -> {
                activeProfileName = intent.getStringExtra(VpnCommands.EXTRA_PROFILE_NAME).orEmpty()
                    .ifBlank { getString(R.string.app_name) }
                VpnStateBus.connecting(activeProfileName)
                startForeground(NOTIFICATION_ID, notification(getString(R.string.vpn_connecting)))
                val config = intent.getStringExtra(VpnCommands.EXTRA_CONFIG_JSON)
                val mtu = intent.getIntExtra(VpnCommands.EXTRA_MTU, DEFAULT_MTU)
                val dnsPrimary = intent.getStringExtra(VpnCommands.EXTRA_DNS_PRIMARY).orEmpty()
                val dnsSecondary = intent.getStringExtra(VpnCommands.EXTRA_DNS_SECONDARY).orEmpty()
                val routingMode = intent.getStringExtra(VpnCommands.EXTRA_ROUTING_MODE).orEmpty()
                val applications = intent.getStringArrayListExtra(VpnCommands.EXTRA_APPLICATIONS).orEmpty()
                val keepAwake = intent.getBooleanExtra(VpnCommands.EXTRA_KEEP_SCREEN_ON, false)
                lastStartArgs = StartArgs(config, mtu, dnsPrimary, dnsSecondary, routingMode, applications, keepAwake)
                worker.execute {
                    startVpn(config, mtu, dnsPrimary, dnsSecondary, routingMode, applications, keepAwake)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startVpn(
        config: String?,
        mtu: Int,
        dnsPrimary: String,
        dnsSecondary: String,
        routingMode: String,
        applications: List<String>,
        keepAwake: Boolean
    ) {
        if (config.isNullOrBlank()) {
            Log.e(TAG, "Missing engine configuration")
            stopVpn()
            return
        }
        if (prepare(this) != null) {
            Log.e(TAG, "VPN permission has not been granted")
            stopVpn()
            return
        }

        try {
            stopVpnResources()

            // The engine resolves the remote endpoint before the VPN route is installed.
            core.start(config)

            val mode = runCatching { AppRoutingMode.valueOf(routingMode) }.getOrDefault(AppRoutingMode.ALL)
            val routedApplications = applications.filterNot { it == packageName }
            if (mode == AppRoutingMode.INCLUDE && routedApplications.isEmpty()) {
                error("Select at least one app for include routing")
            }

            val builder = Builder()
                .setSession(activeProfileName)
                .setMtu(mtu)
                .addAddress("26.26.26.1", 30)
                .addRoute("0.0.0.0", 0)
                .addAddress("fd26:26:26::1", 126)
                .addRoute("::", 0)
                .apply {
                    if (dnsPrimary.isNotBlank()) addDnsServer(dnsPrimary)
                    if (dnsSecondary.isNotBlank() && dnsSecondary != dnsPrimary) addDnsServer(dnsSecondary)
                    // Keep the tunnel engine's own outbound sockets outside its VPN route.
                    if (mode != AppRoutingMode.INCLUDE) addDisallowedApplication(packageName)
                    routedApplications.forEach { routedPackage ->
                        runCatching {
                            if (mode == AppRoutingMode.INCLUDE) addAllowedApplication(routedPackage)
                            if (mode == AppRoutingMode.EXCLUDE) addDisallowedApplication(routedPackage)
                        }.onFailure { VpnStateBus.log("Skipped app rule: $routedPackage") }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        setMetered(false)
                    }
                }

            val descriptor = builder.establish() ?: error("Android refused to establish the VPN interface")

            tun = descriptor
            val fd = descriptor.detachFd()
            tun = null // Ownership moved to the Go tun2socks engine.
            core.startTun(fd, mtu)
            startForeground(NOTIFICATION_ID, notification(getString(R.string.vpn_connected)))
            if (keepAwake) acquireWakeLock()
            VpnStateBus.connected()
            statsJob?.cancel()
            statsJob = scope.launch {
                while (isActive) {
                    VpnStateBus.stats(core.uploadTotal(), core.downloadTotal())
                    delay(1_000)
                }
            }
        } catch (error: Throwable) {
            Log.e(TAG, "VPN startup failed", error)
            VpnStateBus.error(error)
            stopVpn()
        }
    }

    private fun stopVpn() {
        VpnStateBus.stopping()
        stopVpnResources()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        VpnStateBus.disconnected()
    }

    private fun stopVpnResources() {
        statsJob?.cancel()
        statsJob = null
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
        runCatching { core.stop() }
        runCatching { tun?.close() }
        tun = null
    }

    override fun onRevoke() {
        worker.execute { stopVpn() }
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpnResources()
        scope.cancel()
        worker.shutdownNow()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)

    private fun notification(text: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_vpn_notification)
        .setContentTitle(getString(R.string.app_name))
        .setContentText(text)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .addAction(
            0,
            "Reconnect",
            PendingIntent.getService(
                this,
                2,
                Intent(this, HeadlessVpnService::class.java).setAction(VpnCommands.ACTION_RECONNECT),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .addAction(
            0,
            "Stop",
            PendingIntent.getService(
                this,
                1,
                Intent(this, HeadlessVpnService::class.java).setAction(VpnCommands.ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()

    private fun acquireWakeLock() {
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:LibsTunnel")
            .apply { acquire(12 * 60 * 60 * 1000L) }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.vpn_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
    }

    companion object {
        private const val TAG = "LibsTunnel"
        private const val CHANNEL_ID = "vpn_connection"
        private const val NOTIFICATION_ID = 1001
        private const val DEFAULT_MTU = 1500
    }
}
