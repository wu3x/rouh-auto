package org.wpfy.carlauncher.feature.home

import org.wpfy.carlauncher.feature.media.MediaState
import org.wpfy.carlauncher.feature.navigation.NavigationState

data class HomeUiState(
    val navigation: NavigationState = NavigationState(),
    val media: MediaState = MediaState(),
)
