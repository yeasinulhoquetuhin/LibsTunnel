package com.libsvpn.tunnel.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libsvpn.tunnel.AppViewModel
import com.libsvpn.tunnel.BuildConfig
import com.libsvpn.tunnel.InstalledApp
import com.libsvpn.tunnel.R
import com.libsvpn.tunnel.data.LibsConfigCodec
import com.libsvpn.tunnel.model.AccentColor
import com.libsvpn.tunnel.model.AppRoutingMode
import com.libsvpn.tunnel.model.AppSettings
import com.libsvpn.tunnel.model.PayloadMode
import com.libsvpn.tunnel.model.ProfileStore
import com.libsvpn.tunnel.model.SecurityType
import com.libsvpn.tunnel.model.ThemeMode
import com.libsvpn.tunnel.model.TransportType
import com.libsvpn.tunnel.model.TunnelProfile
import com.libsvpn.tunnel.model.TunnelType
import com.libsvpn.tunnel.service.VpnRuntimeState
import com.libsvpn.tunnel.service.VpnStatus
import kotlinx.coroutines.delay
import java.util.Locale

private enum class AppSection(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home),
    PROFILES("Profiles", Icons.Rounded.Storage),
    LOGS("Logs", Icons.Rounded.Terminal),
    SETTINGS("Settings", Icons.Rounded.Settings)
}

private val LocalActionHaptic = staticCompositionLocalOf<(HapticFeedbackType) -> Unit> { {} }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibsTunnelApp(
    viewModel: AppViewModel,
    onConnect: () -> Unit,
    onImportFile: ((String, String) -> Unit) -> Unit,
    onExportFile: (String, String) -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val store by viewModel.store.collectAsState()
    val runtime by viewModel.runtime.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    var section by remember { mutableStateOf(AppSection.HOME) }
    var editing by remember { mutableStateOf<TunnelProfile?>(null) }
    var showImport by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }
    var exportProfile by remember { mutableStateOf<TunnelProfile?>(null) }
    var showAbout by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<TunnelProfile?>(null) }
    val hapticsEnabled = store.settings.hapticFeedback
    val performHaptic: (HapticFeedbackType) -> Unit = { type ->
        if (hapticsEnabled) haptic.performHapticFeedback(type)
    }
    val installedApps by produceState<List<InstalledApp>>(emptyList()) {
        value = viewModel.installedApps()
    }

    BackHandler(enabled = editing != null || section != AppSection.HOME) {
        if (editing != null) editing = null else section = AppSection.HOME
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }
    LaunchedEffect(runtime.status, hapticsEnabled) {
        if (runtime.status == VpnStatus.CONNECTED || runtime.status == VpnStatus.ERROR) {
            performHaptic(HapticFeedbackType.LongPress)
        }
    }

    val tick = {
        performHaptic(HapticFeedbackType.TextHandleMove)
    }

    CompositionLocalProvider(LocalActionHaptic provides performHaptic) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 720.dp
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                if (editing == null) {
                    AppTopBar(onAbout = { showAbout = true })
                } else {
                    CenterAlignedTopAppBar(
                        title = { Text("Profile", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { editing = null }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (!wide && editing == null) {
                    NavigationBar(modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
                        AppSection.entries.forEach { item ->
                            NavigationBarItem(
                                selected = section == item,
                                onClick = { tick(); section = item },
                                icon = { Icon(item.icon, contentDescription = item.title) },
                                label = { Text(item.title) }
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (editing == null && section == AppSection.PROFILES) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ExtendedFloatingActionButton(
                            onClick = { tick(); showImport = true },
                            modifier = Modifier.width(220.dp),
                            icon = { Icon(Icons.Rounded.FileUpload, null) },
                            text = { Text("Import profile") }
                        )
                        ExtendedFloatingActionButton(
                            onClick = { tick(); editing = TunnelProfile() },
                            modifier = Modifier.width(220.dp),
                            icon = { Icon(Icons.Rounded.Add, null) },
                            text = { Text("New profile") }
                        )
                    }
                }
            }
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                if (wide && editing == null) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        Spacer(Modifier.height(12.dp))
                        AppSection.entries.forEach { item ->
                            NavigationRailItem(
                                selected = section == item,
                                onClick = { tick(); section = item },
                                icon = { Icon(item.icon, contentDescription = item.title) },
                                label = { Text(item.title) }
                            )
                        }
                    }
                }

                Box(Modifier.weight(1f).fillMaxHeight()) {
                    val draft = editing
                    if (draft != null) {
                        ProfileEditor(
                            initial = draft,
                            installedApps = installedApps,
                            onCancel = { editing = null },
                            onSave = {
                                performHaptic(HapticFeedbackType.Confirm)
                                viewModel.save(it)
                                editing = null
                                section = AppSection.PROFILES
                            }
                        )
                    } else {
                        when (section) {
                            AppSection.HOME -> HomeScreen(
                                store = store,
                                runtime = runtime,
                                onConnect = {
                                    performHaptic(
                                        if (runtime.status == VpnStatus.CONNECTED) HapticFeedbackType.ToggleOff
                                        else HapticFeedbackType.ToggleOn
                                    )
                                    onConnect()
                                },
                                onSelect = { tick(); viewModel.select(it) },
                                onEdit = { tick(); editing = it },
                                onProfiles = { section = AppSection.PROFILES }
                            )
                            AppSection.PROFILES -> ProfilesScreen(
                                store = store,
                                activeProfileName = runtime.profileName.takeIf {
                                    runtime.status in setOf(VpnStatus.CONNECTED, VpnStatus.CONNECTING, VpnStatus.STOPPING)
                                },
                                onSelect = { tick(); viewModel.select(it) },
                                onEdit = {
                                    tick()
                                    if (it.locked) viewModel.message(it.lockMessage) else editing = it
                                },
                                onDuplicate = { tick(); viewModel.duplicate(it) },
                                onDelete = {
                                    performHaptic(HapticFeedbackType.Reject)
                                    if (runtime.status in setOf(VpnStatus.CONNECTED, VpnStatus.CONNECTING, VpnStatus.STOPPING) && it.name == runtime.profileName) {
                                        viewModel.message("Stop this profile before deleting it")
                                    } else pendingDelete = it
                                },
                                onToggleLock = { performHaptic(HapticFeedbackType.Confirm); viewModel.toggleLock(it) },
                                onImport = { showImport = true },
                                onExport = { exportProfile = it; showExport = true }
                            )
                            AppSection.LOGS -> LogsScreen(runtime, viewModel::clearLogs, performHaptic)
                            AppSection.SETTINGS -> SettingsScreen(
                                settings = store.settings,
                                onUpdate = viewModel::updateSettings,
                                onOpenUrl = onOpenUrl
                            )
                        }
                    }
                }
            }
        }
    }

    if (showImport) {
        ImportDialog(
            hapticsEnabled = hapticsEnabled,
            onDismiss = { showImport = false },
            onFile = { onSelected ->
                performHaptic(HapticFeedbackType.TextHandleMove)
                onImportFile(onSelected)
            },
            onImport = { raw ->
                performHaptic(HapticFeedbackType.Confirm)
                viewModel.importText(raw)
                showImport = false
            }
        )
    }
    if (showExport) {
        ExportDialog(
            profile = exportProfile,
            hapticsEnabled = hapticsEnabled,
            onDismiss = { showExport = false; exportProfile = null },
            onSaveFile = { content, fileName ->
                performHaptic(HapticFeedbackType.Confirm)
                onExportFile(content, fileName)
                showExport = false
                exportProfile = null
            }
        )
    }
    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false }, onOpenUrl = onOpenUrl)
    }
    pendingDelete?.let { profile ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete config?") },
            text = { Text("${profile.name} will be permanently removed from this device.") },
            confirmButton = {
                Button(
                    onClick = {
                        performHaptic(HapticFeedbackType.Reject)
                        viewModel.delete(profile)
                        pendingDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(onAbout: () -> Unit) {
    val haptic = LocalActionHaptic.current
    TopAppBar(
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.libs_logo),
                    contentDescription = null,
                    modifier = Modifier.size(30.dp).clip(RoundedCornerShape(9.dp))
                )
                Spacer(Modifier.width(8.dp))
                Text("Libs Tunnel", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        actions = {
            IconButton(onClick = { haptic(HapticFeedbackType.TextHandleMove); onAbout() }) { Icon(Icons.Rounded.Info, "About") }
        }
    )
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit, onOpenUrl: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Image(painterResource(R.drawable.libs_logo), null, Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))) },
        title = { Text("About Libs Tunnel", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · GPL-3.0-only", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text("A local-first Android VPN client built around Xray-core. Create or import VLESS, VMess and Trojan profiles, choose routing and DNS options, then connect with the Android VPN service.")
                Text("Transports: TCP, WebSocket, gRPC, HTTP Upgrade, XHTTP and mKCP. Security: TLS and REALITY.", style = MaterialTheme.typography.bodySmall)
                Text("Engine: Xray v26.3.27 · tun2socks v2.7.0", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text("Profiles and credentials stay in app-private storage. No account, ads, analytics or telemetry.", style = MaterialTheme.typography.bodySmall)
                Text("Built by Yeasinul Hoque Tuhin", style = MaterialTheme.typography.labelMedium)
                OutlinedButton(onClick = { onOpenUrl("https://github.com/yeasinulhoquetuhin/LibsTunnel") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Code, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Source code on GitHub")
                }
                OutlinedButton(onClick = { onOpenUrl("https://t.me/TuhinBroh") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Campaign, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Channel  @TuhinBroh")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun HomeScreen(
    store: ProfileStore,
    runtime: VpnRuntimeState,
    onConnect: () -> Unit,
    onSelect: (TunnelProfile) -> Unit,
    onEdit: (TunnelProfile) -> Unit,
    onProfiles: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(0.6f))
        ConnectOrb(runtime, onConnect)
        Spacer(Modifier.height(18.dp))
        val busy = runtime.status in setOf(VpnStatus.CONNECTING, VpnStatus.STOPPING)
        val active = runtime.status == VpnStatus.CONNECTED
        val shownName = if (busy || active) runtime.profileName else store.selected?.name.orEmpty()
        Text(
            shownName.ifBlank { "No profile selected" },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            when {
                active -> store.profiles.firstOrNull { it.name == runtime.profileName }?.endpoint()
                    ?: "Tunnel active"
                busy -> "Please wait..."
                else -> store.selected?.endpoint() ?: "Create or import a profile"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(14.dp))
        if (store.settings.showConnectionStats && (active || busy)) {
            StatsStrip(runtime)
            Spacer(Modifier.height(14.dp))
        }
        runtime.lastError?.let { ErrorBanner(it) }
        Spacer(Modifier.weight(1f))
        ConfigSelector(
            selected = store.selected,
            profiles = store.profiles,
            enabled = !busy && !active,
            onSelect = onSelect,
            onProfiles = onProfiles
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ConnectOrb(runtime: VpnRuntimeState, onConnect: () -> Unit) {
    val active = runtime.status == VpnStatus.CONNECTED
    val busy = runtime.status in setOf(VpnStatus.CONNECTING, VpnStatus.STOPPING)
    val accent = when {
        active -> MaterialTheme.colorScheme.primary
        busy -> Color(0xFFF4A261)
        else -> MaterialTheme.colorScheme.primary
    }
    val ringColor = when {
        active -> MaterialTheme.colorScheme.primary
        busy -> Color(0xFFF4A261)
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val transition = rememberInfiniteTransition(label = "orb")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "spin"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(210.dp)) {
        Canvas(Modifier.size(210.dp)) {
            val stroke = 5.dp.toPx()
            val inset = stroke * 2
            when {
                busy -> {
                    rotate(rotation) {
                        drawArc(
                            color = ringColor,
                            startAngle = 0f,
                            sweepAngle = 110f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - inset * 2, size.height - inset * 2),
                            style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                    }
                    drawCircle(
                        color = ringColor.copy(alpha = 0.18f),
                        radius = (size.minDimension - inset * 2) / 2,
                        style = Stroke(stroke)
                    )
                }
                else -> drawCircle(
                    color = ringColor,
                    radius = (size.minDimension - inset * 2) / 2,
                    style = Stroke(stroke)
                )
            }
        }
        Surface(
            modifier = Modifier
                .size(152.dp)
                .clip(CircleShape)
                .clickable(enabled = !busy, onClick = onConnect),
            shape = CircleShape,
            color = if (active) accent else MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = if (active) 8.dp else 0.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Rounded.PowerSettingsNew,
                    contentDescription = if (active) "Disconnect" else "Connect",
                    modifier = Modifier.size(46.dp),
                    tint = if (active) MaterialTheme.colorScheme.onPrimary else accent
                )
                Text(
                    when {
                        active -> "STOP"
                        busy -> "WAIT"
                        else -> "START"
                    },
                    color = if (active) MaterialTheme.colorScheme.onPrimary else accent,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
private fun StatsStrip(runtime: VpnRuntimeState) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(runtime.status, runtime.connectedAt) {
        while (runtime.status == VpnStatus.CONNECTED) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem("UPLOAD", formatBytes(runtime.uploadBytes))
        StatItem("DOWNLOAD", formatBytes(runtime.downloadBytes))
        StatItem(
            "DURATION",
            if (runtime.connectedAt > 0) formatDuration(now - runtime.connectedAt) else "00:00"
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 15.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun ErrorBanner(error: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(error, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ConfigSelector(
    selected: TunnelProfile?,
    profiles: List<TunnelProfile>,
    enabled: Boolean,
    onSelect: (TunnelProfile) -> Unit,
    onProfiles: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    OutlinedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled && profiles.isNotEmpty()) { expanded = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Storage, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Config", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text(
                    selected?.name ?: "Select a profile",
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box {
                IconButton(onClick = { expanded = true }, enabled = enabled && profiles.isNotEmpty()) {
                    Icon(Icons.Rounded.MoreVert, "Choose config")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    profiles.forEach { profile ->
                        DropdownMenuItem(
                            text = { Text(profile.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = {
                                if (profile.id == selected?.id) Icon(Icons.Rounded.Check, null)
                            },
                            onClick = {
                                onSelect(profile)
                                expanded = false
                            }
                        )
                    }
                }
            }
            TextButton(onClick = onProfiles) { Text("Manage") }
        }
    }
}

@Composable
private fun ProfilesScreen(
    store: ProfileStore,
    activeProfileName: String?,
    onSelect: (TunnelProfile) -> Unit,
    onEdit: (TunnelProfile) -> Unit,
    onDuplicate: (TunnelProfile) -> Unit,
    onDelete: (TunnelProfile) -> Unit,
    onToggleLock: (TunnelProfile) -> Unit,
    onImport: () -> Unit,
    onExport: (TunnelProfile) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Profiles", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("${store.profiles.size} saved", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (store.profiles.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Storage, null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.height(8.dp))
                    Text("No profiles yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Create one or import a share link / .libs file", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onImport) { Text("Import") }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
                items(store.profiles, key = { it.id }) { profile ->
                    ProfileListItem(
                        profile = profile,
                        selected = profile.id == store.selected?.id,
                        active = profile.name == activeProfileName,
                        onSelect = { onSelect(profile) },
                        onEdit = { onEdit(profile) },
                        onDuplicate = { onDuplicate(profile) },
                        onDelete = { onDelete(profile) },
                        onToggleLock = { onToggleLock(profile) },
                        onExport = { onExport(profile) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileListItem(
    profile: TunnelProfile,
    selected: Boolean,
    active: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onToggleLock: () -> Unit,
    onExport: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val haptic = LocalActionHaptic.current
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    if (profile.locked) Icons.Rounded.Lock else Icons.Rounded.Public,
                    null,
                    modifier = Modifier.padding(9.dp).size(18.dp),
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(profile.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (selected) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(profile.endpoint(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOf(profile.type.title, profile.transport.title, profile.security.title).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                }
            Box {
                IconButton(onClick = { haptic(HapticFeedbackType.TextHandleMove); menu = true }) { Icon(Icons.Rounded.MoreVert, "Actions") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Edit") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) }, onClick = { haptic(HapticFeedbackType.TextHandleMove); menu = false; onEdit() })
                    DropdownMenuItem(text = { Text("Duplicate") }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) }, onClick = { haptic(HapticFeedbackType.Confirm); menu = false; onDuplicate() })
                    DropdownMenuItem(
                        text = { Text("Export config") },
                        leadingIcon = { Icon(Icons.Rounded.FileDownload, null) },
                        onClick = { haptic(HapticFeedbackType.Confirm); menu = false; onExport() }
                    )
                    DropdownMenuItem(
                        text = { Text(if (profile.locked) "Unlock config" else "Lock config") },
                        leadingIcon = { Icon(if (profile.locked) Icons.Rounded.LockOpen else Icons.Rounded.Lock, null) },
                        onClick = { haptic(HapticFeedbackType.ToggleOn); menu = false; onToggleLock() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                        onClick = { haptic(HapticFeedbackType.Reject); menu = false; onDelete() },
                        enabled = !profile.locked && !active
                    )
                }
            }
        }
    }
}

@Composable
private fun LogsScreen(
    runtime: VpnRuntimeState,
    onClear: () -> Unit,
    onHaptic: (HapticFeedbackType) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    var confirmClear by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val visibleLogs = remember(runtime.logs, query, filter) {
        runtime.logs.filter { line ->
            val matchesQuery = query.isBlank() || line.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                "Errors" -> line.contains("error", true) || line.contains("failed", true)
                "Warnings" -> line.contains("warn", true)
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }
    LaunchedEffect(visibleLogs.size) {
        if (visibleLogs.isNotEmpty()) listState.animateScrollToItem(visibleLogs.lastIndex)
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Logs", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("${visibleLogs.size} engine events", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = {
                onHaptic(HapticFeedbackType.TextHandleMove)
                clipboard.setText(AnnotatedString(visibleLogs.joinToString("\n")))
                Toast.makeText(context, "Logs copied", Toast.LENGTH_SHORT).show()
            }, enabled = visibleLogs.isNotEmpty()) {
                Icon(Icons.Rounded.ContentCopy, "Copy logs")
            }
            IconButton(onClick = {
                onHaptic(HapticFeedbackType.ToggleOn)
                if (visibleLogs.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Libs Tunnel logs")
                        putExtra(Intent.EXTRA_TEXT, visibleLogs.joinToString("\n"))
                    }
                    runCatching { context.startActivity(Intent.createChooser(intent, "Share logs")) }
                }
            }, enabled = visibleLogs.isNotEmpty()) {
                Icon(Icons.Rounded.Share, "Share logs")
            }
            IconButton(onClick = { onHaptic(HapticFeedbackType.Reject); confirmClear = true }, enabled = runtime.logs.isNotEmpty()) {
                Icon(Icons.Rounded.DeleteSweep, "Clear logs")
            }
        }
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            placeholder = { Text("Search logs") },
            shape = RoundedCornerShape(14.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            listOf("All", "Errors", "Warnings").forEach { option ->
                FilterChip(selected = filter == option, onClick = { filter = option }, label = { Text(option) })
            }
        }
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
            color = Color(0xFF0B0F0D)
        ) {
            LazyColumn(state = listState, contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                if (visibleLogs.isEmpty()) {
                    item {
                        Text(
                            if (runtime.logs.isEmpty()) "Waiting for engine events..." else "No matching log entries",
                            color = Color(0xFF7F9C8D),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                items(visibleLogs) { line ->
                    Text(
                        line,
                        color = when {
                            line.contains("error", true) || line.contains("failed", true) -> Color(0xFFFF8A80)
                            line.contains("warn", true) || line.contains("skipped", true) -> Color(0xFFFFD180)
                            line.contains("connected", true) || line.contains("started", true) -> Color(0xFFA5D6A7)
                            else -> Color(0xFFA9E9C7)
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().clickable {
                            onHaptic(HapticFeedbackType.TextHandleMove)
                            clipboard.setText(AnnotatedString(line))
                            Toast.makeText(context, "Log line copied", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            icon = { Icon(Icons.Rounded.DeleteSweep, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Clear all logs?") },
            text = { Text("This removes the current log history from the screen.") },
            confirmButton = {
                Button(onClick = { onHaptic(HapticFeedbackType.Reject); onClear(); confirmClear = false }) { Text("Clear logs") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsScreen(
    settings: AppSettings,
    onUpdate: (AppSettings) -> Unit,
    onOpenUrl: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        }
        item {
            SettingsGroup("APPEARANCE") {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Theme", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { onUpdate(settings.copy(themeMode = mode)) },
                                label = { Text(mode.title) }
                            )
                        }
                    }
                    Text("Accent color", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AccentColor.entries.forEach { accent ->
                            val selected = settings.accent == accent
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(accent.rgb))
                                    .border(
                                        width = if (selected) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                                    .clickable { onUpdate(settings.copy(accent = accent, dynamicColor = false)) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    SwitchSetting("Dynamic color", "Use Android 12+ wallpaper colors (overrides accent)", settings.dynamicColor) {
                        onUpdate(settings.copy(dynamicColor = it))
                    }
                    SwitchSetting("Haptic feedback", "Use different vibration patterns for app actions", settings.hapticFeedback) {
                        onUpdate(settings.copy(hapticFeedback = it))
                    }
                }
            }
        }
        item {
            SettingsGroup("CONNECTION") {
                SwitchSetting("Live transfer stats", "Show upload, download and duration on Home", settings.showConnectionStats) {
                    onUpdate(settings.copy(showConnectionStats = it))
                }
                HorizontalDivider()
                SwitchSetting("Keep tunnel awake", "Hold a partial wake lock while connected", settings.keepScreenOn) {
                    onUpdate(settings.copy(keepScreenOn = it))
                }
            }
        }
        item {
            OutlinedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Security, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Local-first privacy", fontWeight = FontWeight.Bold)
                        Text("No ads, analytics, telemetry, or account SDKs.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            SettingsGroup("ABOUT") {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.libs_logo),
                            contentDescription = null,
                            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Libs Tunnel", fontWeight = FontWeight.Bold)
                            Text("v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Text("Built by Yeasinul Hoque Tuhin", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "A free, local-first Android VPN client. Create, import, organize and protect your tunnel profiles, then connect through the bundled Xray engine.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        "Protocols: VLESS, VMess, Trojan\nTransports: TCP, WebSocket, gRPC, HTTP Upgrade, XHTTP and mKCP\nSecurity: TLS and REALITY",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("Xray v26.3.27 · tun2socks v2.7.0", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text("License: GPL-3.0-only", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    Text("Profiles remain in app-private storage. No account, ads, analytics or telemetry.", style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                    OutlinedButton(onClick = { onOpenUrl("https://tuhinbro.com") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Rounded.Public, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("tuhinbro.com")
                    }
                    OutlinedButton(onClick = { onOpenUrl("https://t.me/TuhinBroh") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Rounded.Campaign, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Channel  @TuhinBroh")
                    }
                    OutlinedButton(onClick = { onOpenUrl("https://t.me/TDZ_CHAT") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Rounded.Forum, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Group  @TDZ_CHAT")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(content = content)
    }
}

@Composable
private fun SwitchSetting(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    val haptic = LocalActionHaptic.current
    Row(
        Modifier.fillMaxWidth().clickable {
            haptic(if (checked) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
            onChecked(!checked)
        }.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Switch(
            checked = checked,
            onCheckedChange = {
                haptic(if (it) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                onChecked(it)
            }
        )
    }
}

@Composable
private fun ProfileEditor(
    initial: TunnelProfile,
    installedApps: List<InstalledApp>,
    onCancel: () -> Unit,
    onSave: (TunnelProfile) -> Unit
) {
    var draft by remember(initial.id) { mutableStateOf(initial) }
    var showApps by remember { mutableStateOf(false) }
    val errors = draft.validationErrors()

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                EditorSection("PROFILE", Icons.Rounded.Tune) {
                    OutlinedTextField(
                        value = draft.name,
                        onValueChange = { draft = draft.copy(name = it) },
                        label = { Text("Profile name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    ChoiceField("Protocol", draft.type, TunnelType.entries.toList(), { it.title }) {
                        draft = draft.copy(type = it)
                    }
                }
            }
            item {
                EditorSection("SERVER", Icons.Rounded.Public) {
                    OutlinedTextField(
                        value = draft.server,
                        onValueChange = { draft = draft.copy(server = it.trim()) },
                        label = { Text("Server host or IP") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = draft.port.takeIf { it > 0 }?.toString().orEmpty(),
                        onValueChange = { value -> draft = draft.copy(port = value.toIntOrNull() ?: 0) },
                        label = { Text("Port") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    when (draft.type) {
                        TunnelType.VLESS, TunnelType.VMESS -> SecretField("User ID / UUID", draft.userId) { draft = draft.copy(userId = it) }
                        TunnelType.TROJAN -> SecretField("Password", draft.password) { draft = draft.copy(password = it) }
                    }
                }
            }
            item {
                EditorSection("TRANSPORT", Icons.Rounded.Speed) {
                    ChoiceField("Transport", draft.transport, TransportType.entries.toList(), { it.title }) { draft = draft.copy(transport = it) }
                    ChoiceField("Security", draft.security, SecurityType.entries.toList(), { it.title }) { draft = draft.copy(security = it) }
                    if (draft.transport in setOf(TransportType.WEBSOCKET, TransportType.HTTP_UPGRADE, TransportType.SPLIT_HTTP)) {
                        OutlinedTextField(value = draft.path, onValueChange = { draft = draft.copy(path = it) }, label = { Text("Path") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = draft.hostHeader, onValueChange = { draft = draft.copy(hostHeader = it) }, label = { Text("Host header") }, modifier = Modifier.fillMaxWidth())
                    }
                    if (draft.transport == TransportType.GRPC) {
                        OutlinedTextField(value = draft.grpcServiceName, onValueChange = { draft = draft.copy(grpcServiceName = it) }, label = { Text("gRPC service name") }, modifier = Modifier.fillMaxWidth())
                    }
                    if (draft.security != SecurityType.NONE) {
                        OutlinedTextField(value = draft.serverName, onValueChange = { draft = draft.copy(serverName = it) }, label = { Text("SNI / server name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = draft.fingerprint, onValueChange = { draft = draft.copy(fingerprint = it) }, label = { Text("TLS fingerprint") }, modifier = Modifier.fillMaxWidth())
                    }
                    if (draft.security == SecurityType.TLS) {
                        OutlinedTextField(value = draft.alpn, onValueChange = { draft = draft.copy(alpn = it) }, label = { Text("ALPN (comma separated)") }, modifier = Modifier.fillMaxWidth())
                    }
                    if (draft.security == SecurityType.REALITY) {
                        SecretField("REALITY public key", draft.realityPublicKey) { draft = draft.copy(realityPublicKey = it) }
                        OutlinedTextField(value = draft.realityShortId, onValueChange = { draft = draft.copy(realityShortId = it) }, label = { Text("Short ID") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = draft.realitySpiderX, onValueChange = { draft = draft.copy(realitySpiderX = it) }, label = { Text("Spider X") }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            item {
                EditorSection("PAYLOAD", Icons.Rounded.Code) {
                    SwitchSetting("Enable payload injection", "Add an outer TCP/TLS/proxy handshake", draft.payloadEnabled) { draft = draft.copy(payloadEnabled = it) }
                    if (draft.payloadEnabled) {
                        ChoiceField("Connection mode", draft.payloadMode, PayloadMode.entries.toList(), { it.title }) { draft = draft.copy(payloadMode = it) }
                        if (draft.payloadMode == PayloadMode.PROXY) {
                            OutlinedTextField(value = draft.proxyHost, onValueChange = { draft = draft.copy(proxyHost = it) }, label = { Text("Proxy server") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = draft.proxyPort.toString(), onValueChange = { it.toIntOrNull()?.let { port -> draft = draft.copy(proxyPort = port) } }, label = { Text("Proxy port") }, modifier = Modifier.fillMaxWidth())
                        }
                        OutlinedTextField(
                            value = draft.payload,
                            onValueChange = { draft = draft.copy(payload = it) },
                            label = { Text("Payload") },
                            supportingText = { Text("[host] [port] [host_port] [method] [protocol] [ua] [crlf] [split=N]") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 5
                        )
                    }
                }
            }
            item {
                EditorSection("DNS", Icons.Rounded.Dns) {
                    SwitchSetting("Custom DNS", "Override system resolvers", draft.customDnsEnabled) { draft = draft.copy(customDnsEnabled = it) }
                    if (draft.customDnsEnabled) {
                        OutlinedTextField(value = draft.dnsPrimary, onValueChange = { draft = draft.copy(dnsPrimary = it) }, label = { Text("Primary DNS") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = draft.dnsSecondary, onValueChange = { draft = draft.copy(dnsSecondary = it) }, label = { Text("Secondary DNS") }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            item {
                EditorSection("APP ROUTING", Icons.Rounded.Apps) {
                    ChoiceField("Routing mode", draft.appRoutingMode, AppRoutingMode.entries.toList(), { it.title }) { draft = draft.copy(appRoutingMode = it) }
                    if (draft.appRoutingMode != AppRoutingMode.ALL) {
                        OutlinedButton(onClick = { showApps = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Rounded.Apps, null)
                            Spacer(Modifier.width(8.dp))
                            Text("${draft.applications.size} apps selected")
                        }
                    }
                    OutlinedTextField(value = draft.mtu.toString(), onValueChange = { it.toIntOrNull()?.let { mtu -> draft = draft.copy(mtu = mtu) } }, label = { Text("TUN MTU") }, modifier = Modifier.fillMaxWidth())
                }
            }
            if (errors.isNotEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Complete before saving", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            errors.forEach { Text("- $it", style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(70.dp)) }
        }
        Surface(shadowElevation = 10.dp) {
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Close, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Cancel")
                }
                Button(onClick = { onSave(draft) }, enabled = errors.isEmpty(), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Save, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Save profile")
                }
            }
        }
    }

    if (showApps) {
        AppSelectionDialog(
            apps = installedApps,
            selected = draft.applications,
            onDismiss = { showApps = false },
            onSave = {
                draft = draft.copy(applications = it)
                showApps = false
            }
        )
    }
}

@Composable
private fun EditorSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            }
            content()
        }
    }
}

@Composable
private fun SecretField(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
private fun <T> ChoiceField(
    label: String,
    value: T,
    options: List<T>,
    title: (T) -> String,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val haptic = LocalActionHaptic.current
    Box(Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { haptic(HapticFeedbackType.TextHandleMove); expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                Text(title(value), fontWeight = FontWeight.SemiBold)
            }
            Icon(Icons.Rounded.MoreVert, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(title(option)) },
                    leadingIcon = { if (option == value) Icon(Icons.Rounded.Check, null) },
                    onClick = {
                        haptic(HapticFeedbackType.Confirm)
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AppSelectionDialog(
    apps: List<InstalledApp>,
    selected: Set<String>,
    onDismiss: () -> Unit,
    onSave: (Set<String>) -> Unit
) {
    var selection by remember(selected) { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose applications") },
        text = {
            LazyColumn(Modifier.height(420.dp)) {
                items(apps, key = { it.packageName }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = app.packageName in selection,
                                onClick = {
                                    selection = if (app.packageName in selection) selection - app.packageName
                                    else selection + app.packageName
                                }
                            )
                            .padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = app.packageName in selection,
                            onClick = {
                                selection = if (app.packageName in selection) selection - app.packageName
                                else selection + app.packageName
                            },
                            label = { Text(if (app.packageName in selection) "ON" else "OFF") }
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(app.label, fontWeight = FontWeight.SemiBold)
                            Text(app.packageName, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(selection) }) { Text("Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ImportDialog(
    hapticsEnabled: Boolean,
    onDismiss: () -> Unit,
    onFile: (((String, String) -> Unit) -> Unit),
    onImport: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var raw by remember { mutableStateOf("") }
    var fileContent by remember { mutableStateOf<String?>(null) }
    var fileName by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.FileUpload, null) },
        title = { Text("Import profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        onFile { content, name ->
                            fileContent = content
                            fileName = name
                            raw = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.FileUpload, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Open .libs file")
                }
                if (fileName != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.FileUpload, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(fileName.orEmpty(), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            IconButton(onClick = {
                                if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.Reject)
                                fileName = null
                                fileContent = null
                            }) {
                                Icon(Icons.Rounded.Close, "Remove selected file")
                            }
                        }
                    }
                }
                Text("or paste a .libs payload, VLESS, VMess, or Trojan link", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = raw, onValueChange = { raw = it }, label = { Text("Configuration or share link") }, minLines = 5, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(fileContent ?: raw) },
                enabled = fileContent != null || raw.isNotBlank()
            ) { Text("Import") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ExportDialog(
    profile: TunnelProfile?,
    hapticsEnabled: Boolean,
    onDismiss: () -> Unit,
    onSaveFile: (String, String) -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val content = remember(profile) { profile?.let(LibsConfigCodec::export) }
    val safeProfileName = remember(profile?.name) {
        profile?.name.orEmpty().replace(Regex("[^A-Za-z0-9._-]+"), "-").trim('-').ifBlank { "libs-profile" }
    }
    val fileName = safeProfileName + LibsConfigCodec.EXTENSION

    fun feedback(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.FileDownload, null) },
        title = { Text("Export ${profile?.name.orEmpty()}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Export this profile as a .libs config file.", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            content?.let { onSaveFile(it, profile?.name ?: "libs-profile") }
                        },
                        enabled = content != null,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.FileDownload, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("File")
                    }
                    OutlinedButton(
                        onClick = {
                            if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            content?.let {
                                clipboard.setText(AnnotatedString(it))
                                feedback("Config copied to clipboard")
                            }
                        },
                        enabled = content != null,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.ContentCopy, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy")
                    }
                }
                Text(
                    "File name: $fileName",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

private fun formatBytes(value: Long): String {
    if (value < 1024) return "$value B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var amount = value.toDouble()
    var unit = -1
    while (amount >= 1024 && unit < units.lastIndex) {
        amount /= 1024
        unit++
    }
    return String.format(Locale.US, if (amount >= 100) "%.0f %s" else "%.1f %s", amount, units[unit])
}

private fun formatDuration(millis: Long): String {
    val total = (millis.coerceAtLeast(0) / 1000)
    val hours = total / 3600
    val minutes = total % 3600 / 60
    val seconds = total % 60
    return if (hours > 0) "%02d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}
