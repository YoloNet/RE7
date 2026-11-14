package com.statushd.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.settingsDataStore: DataStore<Preferences> by
preferencesDataStore(name = "statushd_settings")

/** Tetapan aplikasi (DataStore). Tiada akaun — kunci peranti tanpa nama. */
data class Settings(
    val language: String = "ms", // ms | en
    val qualityDefault: String = "720p",
    val privacySeen: Boolean = false,
    val deviceKey: String = "",
    val whatsappNumber: String = "",
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.settingsDataStore

    private val KEY_LANGUAGE = stringPreferencesKey("language")
    private val KEY_QUALITY = stringPreferencesKey("quality_default")
    private val KEY_PRIVACY_SEEN = booleanPreferencesKey("privacy_seen")
    private val KEY_DEVICE_KEY = stringPreferencesKey("device_key")
    private val KEY_WHATSAPP = stringPreferencesKey("whatsapp_number")

    val settings: Flow<Settings> = store.data.map { prefs ->
        Settings(
            language = prefs[KEY_LANGUAGE] ?: "ms",
            qualityDefault = prefs[KEY_QUALITY] ?: "720p",
            privacySeen = prefs[KEY_PRIVACY_SEEN] ?: false,
            deviceKey = prefs[KEY_DEVICE_KEY] ?: "",
            whatsappNumber = prefs[KEY_WHATSAPP] ?: "",
        )
    }

    suspend fun setLanguage(language: String) {
        store.edit { it[KEY_LANGUAGE] = language }
    }

    suspend fun setQualityDefault(quality: String) {
        store.edit { it[KEY_QUALITY] = quality }
    }

    suspend fun setPrivacySeen(seen: Boolean = true) {
        store.edit { it[KEY_PRIVACY_SEEN] = seen }
    }

    suspend fun setWhatsappNumber(number: String) {
        store.edit { it[KEY_WHATSAPP] = number }
    }

    /** Kunci peranti tanpa nama — dijana sekali, kekal selamanya. */
    suspend fun ensureDeviceKey(): String {
        var current = ""
        store.edit { prefs ->
            current = prefs[KEY_DEVICE_KEY] ?: ""
            if (current.isBlank()) {
                current = UUID.randomUUID().toString()
                prefs[KEY_DEVICE_KEY] = current
            }
        }
        return current
    }
}
