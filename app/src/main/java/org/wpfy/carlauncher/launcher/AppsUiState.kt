package org.wpfy.carlauncher.launcher

data class AppsUiState(
    val apps: List<LauncherApp> = emptyList(),
    val pinnedAppKeys: Set<String> = emptySet(),
    val query: String = "",
    val isLoading: Boolean = true,
) {
    fun isPinned(app: LauncherApp): Boolean = app.stableKey in pinnedAppKeys

    val visibleApps: List<LauncherApp>
        get() {
            val normalizedQuery = query.trim()
            if (normalizedQuery.isEmpty()) {
                return apps.filter(::isPinned) + apps.filterNot(::isPinned)
            }

            return apps.filter { app ->
                app.label.contains(normalizedQuery, ignoreCase = true) ||
                    app.packageName.contains(normalizedQuery, ignoreCase = true)
            }
        }
}
