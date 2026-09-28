package com.libsvpn.tunnel

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.libsvpn.tunnel.data.LibsConfigCodec
import com.libsvpn.tunnel.data.ProfileRepository
import com.libsvpn.tunnel.engine.EngineConfigBuilder
import com.libsvpn.tunnel.model.AppSettings
import com.libsvpn.tunnel.model.ProfileStore
import com.libsvpn.tunnel.model.TunnelProfile
import com.libsvpn.tunnel.service.VpnCommands
import com.libsvpn.tunnel.service.VpnStateBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InstalledApp(val label: String, val packageName: String)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ProfileRepository(application)
    private val mutableMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)

    val store = repository.store.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ProfileStore()
    )
    val runtime = VpnStateBus.state
    val messages = mutableMessages.asSharedFlow()

    fun save(profile: TunnelProfile) {
        viewModelScope.launch {
            repository.upsert(profile)
            message("${profile.name} saved")
        }
    }

    fun duplicate(profile: TunnelProfile) {
        save(
            profile.copy(
                id = java.util.UUID.randomUUID().toString(),
                name = "${profile.name} copy",
                locked = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun delete(profile: TunnelProfile) {
        if (profile.locked) {
            message("Locked profiles cannot be deleted")
            return
        }
        viewModelScope.launch {
            repository.delete(profile.id)
            message("Profile deleted")
        }
    }

    fun select(profile: TunnelProfile) {
        viewModelScope.launch { repository.select(profile.id) }
    }

    fun updateSettings(settings: AppSettings) {
        viewModelScope.launch { repository.updateSettings(settings) }
    }

    fun importText(raw: String, passphrase: String = "") {
        viewModelScope.launch(Dispatchers.Default) {
            val result = LibsConfigCodec.importMany(raw, passphrase)
            result.profiles.forEach { repository.upsert(it) }
            when {
                result.profiles.size > 1 ->
                    message("Imported ${result.profiles.size} profiles" + if (result.failures.isNotEmpty()) " (${result.failures.size} skipped)" else "")
                result.profiles.size == 1 ->
                    message("Imported ${result.profiles.first().name}")
                else ->
                    message(result.failures.firstOrNull() ?: "Import failed")
            }
        }
    }

    fun exportSelected(passphrase: String = ""): String? {
        val profile = store.value.selected ?: run {
            message("Select a profile first")
            return null
        }
        return runCatching { LibsConfigCodec.export(profile, passphrase) }
            .onFailure { message(it.message ?: "Export failed") }
            .getOrNull()
    }

    fun startSelected() {
        val profile = store.value.selected ?: run {
            message("Create or import a profile first")
            return
        }
        runCatching {
            val config = EngineConfigBuilder.build(profile)
            VpnCommands.start(
                getApplication(),
                config,
                profile,
                store.value.settings.keepScreenOn
            )
        }.onFailure { message(it.message ?: "Unable to start tunnel") }
    }

    fun stop() = VpnCommands.stop(getApplication())

    fun clearLogs() = VpnStateBus.clearLogs()

    suspend fun installedApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val packageManager = getApplication<Application>().packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            .map {
                InstalledApp(
                    label = it.loadLabel(packageManager).toString(),
                    packageName = it.activityInfo.packageName
                )
            }
            .distinctBy { it.packageName }
            .filterNot { it.packageName == getApplication<Application>().packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun message(text: String) {
        mutableMessages.tryEmit(text)
    }
}
