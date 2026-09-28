package com.libsvpn.tunnel.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.libsvpn.tunnel.model.AppSettings
import com.libsvpn.tunnel.model.ProfileStore
import com.libsvpn.tunnel.model.TunnelProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.profileDataStore by preferencesDataStore(name = "libs_tunnel_profiles")

class ProfileRepository(private val context: Context) {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    val store: Flow<ProfileStore> = context.profileDataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw error
        }
        .map { preferences ->
            preferences[STORE_KEY]?.let { raw ->
                runCatching { json.decodeFromString<ProfileStore>(raw) }.getOrNull()
            } ?: ProfileStore()
        }

    suspend fun upsert(profile: TunnelProfile, select: Boolean = true) = update { current ->
        val saved = profile.copy(updatedAt = System.currentTimeMillis())
        val profiles = current.profiles.toMutableList()
        val index = profiles.indexOfFirst { it.id == saved.id }
        if (index >= 0) profiles[index] = saved else profiles.add(0, saved)
        current.copy(profiles = profiles, selectedId = if (select) saved.id else current.selectedId)
    }

    suspend fun delete(id: String) = update { current ->
        val profiles = current.profiles.filterNot { it.id == id }
        current.copy(
            profiles = profiles,
            selectedId = if (current.selectedId == id) profiles.firstOrNull()?.id else current.selectedId
        )
    }

    suspend fun select(id: String) = update { it.copy(selectedId = id) }

    suspend fun updateSettings(settings: AppSettings) = update { it.copy(settings = settings) }

    private suspend fun update(transform: (ProfileStore) -> ProfileStore) {
        context.profileDataStore.edit { preferences ->
            val current = preferences[STORE_KEY]?.let { raw ->
                runCatching { json.decodeFromString<ProfileStore>(raw) }.getOrNull()
            } ?: ProfileStore()
            preferences[STORE_KEY] = json.encodeToString(ProfileStore.serializer(), transform(current))
        }
    }

    private companion object {
        val STORE_KEY = stringPreferencesKey("profile_store_v1")
    }
}
