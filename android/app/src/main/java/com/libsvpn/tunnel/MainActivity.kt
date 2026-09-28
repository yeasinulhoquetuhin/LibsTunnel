package com.libsvpn.tunnel

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.libsvpn.tunnel.data.LibsConfigCodec
import com.libsvpn.tunnel.service.VpnStatus
import com.libsvpn.tunnel.ui.LibsTheme
import com.libsvpn.tunnel.ui.LibsTunnelApp

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<AppViewModel>()
    private var pendingExport: String? = null

    private val vpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (VpnService.prepare(this) == null) viewModel.startSelected()
        else viewModel.message("VPN permission was not granted")
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val openProfile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@registerForActivityResult
        importUri(uri)
    }

    private val createProfile = registerForActivityResult(
        ActivityResultContracts.CreateDocument(LibsConfigCodec.MIME_TYPE)
    ) { uri ->
        val content = pendingExport
        pendingExport = null
        if (uri != null && content != null) {
            runCatching {
                contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(content) }
                    ?: error("Unable to open destination")
            }.onSuccess { viewModel.message("Profile exported") }
                .onFailure { viewModel.message(it.message ?: "Export failed") }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        handleIntent(intent)

        setContent {
            val store by viewModel.store.collectAsState()
            LibsTheme(
                themeMode = store.settings.themeMode,
                accent = store.settings.accent,
                dynamicColor = store.settings.dynamicColor
            ) {
                LibsTunnelApp(
                    viewModel = viewModel,
                    onConnect = {
                        if (viewModel.runtime.value.status in setOf(VpnStatus.CONNECTED, VpnStatus.CONNECTING)) {
                            viewModel.stop()
                        } else {
                            val permission = VpnService.prepare(this)
                            if (permission == null) viewModel.startSelected() else vpnPermission.launch(permission)
                        }
                    },
                    onImportFile = { openProfile.launch(arrayOf("*/*")) },
                    onExportFile = { content, fileName ->
                        pendingExport = content
                        createProfile.launch(fileName.sanitizeFileName() + LibsConfigCodec.EXTENSION)
                    },
                    onOpenUrl = { url ->
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) intent.data?.let(::importUri)
    }

    private fun importUri(uri: Uri) {
        runCatching {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Unable to read profile")
        }.onSuccess { viewModel.importText(it) }
            .onFailure { viewModel.message(it.message ?: "Import failed") }
    }
}

private fun String.sanitizeFileName(): String = trim()
    .replace(Regex("[^A-Za-z0-9._-]+"), "-")
    .trim('-')
    .ifBlank { "libs-profile" }
