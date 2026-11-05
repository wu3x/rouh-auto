package org.wpfy.carlauncher.data.persistence

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Maximum number of pinned apps shown in the side rail / dock. */
const val MaxPinnedApps = 5

private val Context.launcherPreferences by preferencesDataStore(name = "launcher_preferences")

class PinnedAppsRepository(context: Context) {
    private val dataStore = context.applicationContext.launcherPreferences

    val pinnedAppKeys: Flow<Set<String>> = dataStore.data.map { preferences ->
        preferences[PinnedAppsKey].orEmpty()
    }

    suspend fun setPinned(appKey: String, pinned: Boolean) {
        dataStore.edit { preferences ->
            val pinnedApps = preferences[PinnedAppsKey].orEmpty().toMutableSet()
            if (pinned) {
                pinnedApps.add(appKey)
            } else {
                pinnedApps.remove(appKey)
            }
            preferences[PinnedAppsKey] = pinnedApps
        }
    }
}

private val PinnedAppsKey = stringSetPreferencesKey("pinned_apps")
