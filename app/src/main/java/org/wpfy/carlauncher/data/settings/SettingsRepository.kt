package org.wpfy.carlauncher.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.settingsPreferences by preferencesDataStore(name = "settings_preferences")

class SettingsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dataStore = appContext.settingsPreferences
    private val migrationPreferences = appContext.getSharedPreferences(
        MigrationPreferencesName,
        Context.MODE_PRIVATE,
    )

    val settings: Flow<LauncherSettings> = dataStore.data.map { preferences ->
        LauncherSettings(
            onboardingCompleted = preferences[OnboardingCompletedKey] ?: false,
            startOnBoot = preferences[StartOnBootKey] ?: false,
            startupScreen = preferences[StartupScreenKey]
                ?.let { value -> runCatching { StartupScreen.valueOf(value) }.getOrNull() }
                ?: StartupScreen.Home,
            textSizePreset = preferences[TextSizeKey]
                ?.let { value -> runCatching { TextSizePreset.valueOf(value) }.getOrNull() }
                ?: TextSizePreset.Standard,
            preferredMediaAppKey = preferences[PreferredMediaAppKey],
            navigationAppKey = preferences[NavigationAppKey],
            navigationCompatibilityMode = preferences[NavigationCompatibilityModeKey] ?: false,
            railPosition = preferences[RailPositionKey]
                ?.let { value -> runCatching { RailPosition.valueOf(value) }.getOrNull() }
                ?: RailPosition.Left,
        )
    }

    suspend fun current(): LauncherSettings = settings.first()

    suspend fun setStartOnBoot(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[StartOnBootKey] = enabled }
    }

    suspend fun setStartupScreen(screen: StartupScreen) {
        dataStore.edit { preferences -> preferences[StartupScreenKey] = screen.name }
    }

    suspend fun setTextSizePreset(preset: TextSizePreset) {
        dataStore.edit { preferences -> preferences[TextSizeKey] = preset.name }
    }

    suspend fun setPreferredMediaApp(appKey: String?) {
        dataStore.edit { preferences ->
            if (appKey == null) preferences.remove(PreferredMediaAppKey)
            else preferences[PreferredMediaAppKey] = appKey
        }
    }

    suspend fun setNavigationApp(appKey: String?) {
        dataStore.edit { preferences ->
            if (appKey == null) preferences.remove(NavigationAppKey)
            else preferences[NavigationAppKey] = appKey
        }
    }

    suspend fun setNavigationCompatibilityMode(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[NavigationCompatibilityModeKey] = enabled }
    }

    suspend fun setRailPosition(position: RailPosition) {
        dataStore.edit { preferences -> preferences[RailPositionKey] = position.name }
    }

    suspend fun completeOnboarding(position: RailPosition) {
        dataStore.edit { preferences ->
            preferences[RailPositionKey] = position.name
            preferences[OnboardingCompletedKey] = true
            preferences[OnboardingInitializedKey] = true
        }
    }

    suspend fun markOnboardingInitialized() {
        dataStore.edit { preferences ->
            if (preferences[OnboardingInitializedKey] == null) {
                preferences[OnboardingInitializedKey] = true
            }
        }
    }

    suspend fun migrateExistingInstallOnUpgrade(defaultRailPosition: RailPosition) {
        runUpgradeMigrationWithPendingMarker(
            markPending = { setUpgradeMigrationPending(true) },
            migrate = {
                dataStore.edit { preferences ->
                    if (
                        shouldMigrateLegacyOnboardingState(
                            onboardingCompleted = preferences[OnboardingCompletedKey],
                            onboardingInitialized = preferences[OnboardingInitializedKey],
                        )
                    ) {
                        if (preferences[RailPositionKey] == null) {
                            preferences[RailPositionKey] = defaultRailPosition.name
                        }
                        preferences[OnboardingCompletedKey] = true
                    }
                    preferences[OnboardingInitializedKey] = true
                }
            },
            clearPending = { setUpgradeMigrationPending(false) },
        )
    }

    suspend fun retryPendingUpgradeMigration(defaultRailPosition: RailPosition) {
        if (upgradeMigrationPending()) {
            migrateExistingInstallOnUpgrade(defaultRailPosition)
        }
    }

    private suspend fun upgradeMigrationPending(): Boolean = withContext(Dispatchers.IO) {
        migrationPreferences.getBoolean(UpgradeMigrationPendingKey, false)
    }

    private suspend fun setUpgradeMigrationPending(pending: Boolean) = withContext(Dispatchers.IO) {
        check(
            migrationPreferences.edit()
                .putBoolean(UpgradeMigrationPendingKey, pending)
                .commit(),
        ) { "Unable to persist onboarding migration state" }
    }
}

private val OnboardingCompletedKey = booleanPreferencesKey("onboarding_completed")
private val OnboardingInitializedKey = booleanPreferencesKey("onboarding_initialized")
private val StartOnBootKey = booleanPreferencesKey("start_on_boot")
private val StartupScreenKey = stringPreferencesKey("startup_screen")
private val TextSizeKey = stringPreferencesKey("text_size")
private val PreferredMediaAppKey = stringPreferencesKey("preferred_media_app")
private val NavigationAppKey = stringPreferencesKey("navigation_app")
private val NavigationCompatibilityModeKey = booleanPreferencesKey("navigation_compatibility_mode")
private val RailPositionKey = stringPreferencesKey("rail_position")
private const val MigrationPreferencesName = "settings_migrations"
private const val UpgradeMigrationPendingKey = "onboarding_upgrade_migration_pending"

internal fun shouldMigrateLegacyOnboardingState(
    onboardingCompleted: Boolean?,
    onboardingInitialized: Boolean?,
): Boolean = onboardingCompleted == null && onboardingInitialized == null

internal suspend fun runUpgradeMigrationWithPendingMarker(
    markPending: suspend () -> Unit,
    migrate: suspend () -> Unit,
    clearPending: suspend () -> Unit,
) {
    markPending()
    migrate()
    clearPending()
}
