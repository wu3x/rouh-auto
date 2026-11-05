package org.wpfy.carlauncher.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import org.wpfy.carlauncher.core.platform.defaultRailPositionForDisplay
import org.wpfy.carlauncher.data.persistence.PinnedAppsRepository
import org.wpfy.carlauncher.data.settings.LauncherSettings
import org.wpfy.carlauncher.data.settings.RailPosition
import org.wpfy.carlauncher.data.settings.SettingsRepository
import org.wpfy.carlauncher.data.settings.StartupScreen
import org.wpfy.carlauncher.data.settings.TextSizePreset
import org.wpfy.carlauncher.launcher.AppCatalog
import org.wpfy.carlauncher.launcher.LauncherApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val settings: LauncherSettings = LauncherSettings(),
    val pinnedAppCount: Int = 0,
    val apps: List<LauncherApp> = emptyList(),
)

@Stable
class SettingsStateHolder(
    private val settingsRepository: SettingsRepository,
    private val pinnedAppsRepository: PinnedAppsRepository,
    private val appCatalog: AppCatalog,
    private val defaultRailPosition: RailPosition,
    private val scope: CoroutineScope,
) {
    var uiState by mutableStateOf(SettingsUiState())
        private set

    init {
        scope.launch {
            runCatching {
                settingsRepository.retryPendingUpgradeMigration(defaultRailPosition)
                settingsRepository.markOnboardingInitialized()
            }
        }
        scope.launch {
            settingsRepository.settings.collectLatest { settings ->
                uiState = uiState.copy(settings = settings, isLoading = false)
            }
        }
        scope.launch {
            pinnedAppsRepository.pinnedAppKeys.collectLatest { keys ->
                uiState = uiState.copy(pinnedAppCount = keys.size)
            }
        }
        scope.launch {
            refreshApps()
            appCatalog.packageChanges().collectLatest { refreshApps() }
        }
    }

    fun setStartOnBoot(enabled: Boolean) = update { settingsRepository.setStartOnBoot(enabled) }

    fun setStartupScreen(screen: StartupScreen) = update { settingsRepository.setStartupScreen(screen) }

    fun setTextSizePreset(preset: TextSizePreset) = update { settingsRepository.setTextSizePreset(preset) }

    fun setPreferredMediaApp(app: LauncherApp?) = update {
        settingsRepository.setPreferredMediaApp(app?.stableKey)
    }

    fun setNavigationApp(app: LauncherApp?) = update {
        settingsRepository.setNavigationApp(app?.stableKey)
    }

    fun setNavigationCompatibilityMode(enabled: Boolean) = update {
        settingsRepository.setNavigationCompatibilityMode(enabled)
    }

    fun setRailPosition(position: RailPosition) = update {
        settingsRepository.setRailPosition(position)
    }

    fun completeOnboarding(position: RailPosition) = update {
        settingsRepository.completeOnboarding(position)
    }

    private fun update(block: suspend () -> Unit) {
        scope.launch { block() }
    }

    private suspend fun refreshApps() {
        val apps = appCatalog.loadApps()
        uiState = uiState.copy(apps = apps)
    }
}

@Composable
fun rememberSettingsStateHolder(): SettingsStateHolder {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val configuration = context.resources.configuration
    val defaultRailPosition = defaultRailPositionForDisplay(
        screenWidthDp = configuration.screenWidthDp,
        screenHeightDp = configuration.screenHeightDp,
    )
    return remember(context, scope, defaultRailPosition) {
        SettingsStateHolder(
            settingsRepository = SettingsRepository(context),
            pinnedAppsRepository = PinnedAppsRepository(context),
            appCatalog = AppCatalog(context),
            defaultRailPosition = defaultRailPosition,
            scope = scope,
        )
    }
}
