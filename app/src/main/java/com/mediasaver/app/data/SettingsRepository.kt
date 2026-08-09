package com.mediasaver.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class DefaultQuality(val label: String) {
    BEST("أفضل جودة متاحة"),
    HD1080("1080p"),
    HD720("720p"),
    AUDIO_ONLY("صوت فقط")
}

data class AppSettings(
    val defaultQuality: DefaultQuality = DefaultQuality.BEST,
    val saveToMoviesFolder: Boolean = true // true: Movies/MediaSaver — false: Downloads-style Movies root
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val QUALITY = stringPreferencesKey("default_quality")
        val SAVE_LOCATION = stringPreferencesKey("save_location")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            defaultQuality = prefs[Keys.QUALITY]?.let {
                runCatching { DefaultQuality.valueOf(it) }.getOrNull()
            } ?: DefaultQuality.BEST,
            saveToMoviesFolder = prefs[Keys.SAVE_LOCATION] != "root"
        )
    }

    suspend fun setDefaultQuality(quality: DefaultQuality) {
        context.dataStore.edit { it[Keys.QUALITY] = quality.name }
    }

    suspend fun setSaveToMoviesFolder(toSubfolder: Boolean) {
        context.dataStore.edit { it[Keys.SAVE_LOCATION] = if (toSubfolder) "subfolder" else "root" }
    }
}
