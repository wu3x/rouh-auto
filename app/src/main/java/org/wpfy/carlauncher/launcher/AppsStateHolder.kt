package org.wpfy.carlauncher.launcher

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import org.wpfy.carlauncher.data.persistence.MaxPinnedApps
import org.wpfy.carlauncher.data.persistence.PinnedAppsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Stable
class AppsStateHolder(
    private val catalog: AppCatalog,
    private val launcher: AppLauncher,
    private val pinnedAppsRepository: PinnedAppsRepository,
    private val scope: CoroutineScope,
) {
    var uiState by mutableStateOf(AppsUiState())
        private set

    /** Incremented every time the user tries to pin more than [MaxPinnedApps] apps. */
    var pinLimitEvents by mutableIntStateOf(0)
        private set

    init {
        scope.launch {
            pinnedAppsRepository.pinnedAppKeys.collectLatest { pinnedAppKeys ->
                uiState = uiState.copy(pinnedAppKeys = pinnedAppKeys)
            }
        }

        scope.launch {
            refresh()
            catalog.packageChanges().collectLatest { refresh() }
        }
    }

    fun updateQuery(query: String) {
        uiState = uiState.copy(query = query)
    }

    fun launch(app: LauncherApp) {
        launcher.launch(app)
    }

    fun openAppInfo(app: LauncherApp) {
        launcher.openAppInfo(app)
    }

    fun togglePinned(app: LauncherApp) {
        val shouldPin = !uiState.isPinned(app)
        // Count only installed apps so stale keys of uninstalled apps never block pinning.
        if (shouldPin && uiState.apps.count(uiState::isPinned) >= MaxPinnedApps) {
            pinLimitEvents += 1
            return
        }
        scope.launch {
            pinnedAppsRepository.setPinned(app.stableKey, shouldPin)
        }
    }

    suspend fun loadIcon(app: LauncherApp): ImageBitmap? = catalog.loadIcon(app)

    private suspend fun refresh() {
        uiState = uiState.copy(isLoading = true)
        val apps = catalog.loadApps()
        uiState = uiState.copy(
            apps = apps,
            isLoading = false,
        )
    }
}

@Composable
fun rememberAppsStateHolder(): AppsStateHolder {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    return remember(context, scope) {
        AppsStateHolder(
            catalog = AppCatalog(context),
            launcher = AppLauncher(context),
            pinnedAppsRepository = PinnedAppsRepository(context),
            scope = scope,
        )
    }
}
