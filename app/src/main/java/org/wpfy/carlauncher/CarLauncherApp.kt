package org.wpfy.carlauncher

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.wpfy.carlauncher.data.settings.StartupScreen
import org.wpfy.carlauncher.design.theme.CarColors
import org.wpfy.carlauncher.design.theme.CarTheme
import org.wpfy.carlauncher.feature.home.HomeRoute
import org.wpfy.carlauncher.feature.media.AndroidMediaRepository
import org.wpfy.carlauncher.feature.media.MediaRoute
import org.wpfy.carlauncher.feature.onboarding.OnboardingRoute
import org.wpfy.carlauncher.feature.notifications.AndroidNotificationRepository
import org.wpfy.carlauncher.feature.notifications.NotificationRoute
import org.wpfy.carlauncher.feature.settings.SettingsRoute
import org.wpfy.carlauncher.feature.settings.rememberSettingsStateHolder
import org.wpfy.carlauncher.launcher.AppsRoute
import org.wpfy.carlauncher.shell.LauncherShell
import org.wpfy.carlauncher.shell.PlaceholderDestination
import org.wpfy.carlauncher.shell.ShellDestination
import org.wpfy.carlauncher.shell.rememberShellState

@Composable
fun CarLauncherApp() {
    val context = LocalContext.current
    val settingsStateHolder = rememberSettingsStateHolder()
    val settingsState = settingsStateHolder.uiState
    val mediaRepository = remember(context.applicationContext) {
        AndroidMediaRepository(context.applicationContext)
    }
    val mediaState by mediaRepository.state.collectAsState()
    val notificationRepository = remember(context.applicationContext) {
        AndroidNotificationRepository(context.applicationContext)
    }
    val notificationState by notificationRepository.state.collectAsState()

    DisposableEffect(mediaRepository, notificationRepository) {
        mediaRepository.start()
        notificationRepository.start()
        onDispose {
            notificationRepository.stop()
            mediaRepository.stop()
        }
    }

    CarTheme(textScale = settingsState.settings.textSizePreset.scale) {
        if (settingsState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CarColors.Background),
            )
        } else if (!settingsState.settings.onboardingCompleted) {
            OnboardingRoute(settingsStateHolder)
        } else {
            val shellState = rememberShellState(
                initialDestination = when (settingsState.settings.startupScreen) {
                    StartupScreen.Home -> ShellDestination.Home
                    StartupScreen.Apps -> ShellDestination.Apps
                },
            )
            LauncherShell(
                notificationCount = notificationState.notifications.size,
                onAssistantClick = { launchAssistant(context) },
                railPosition = settingsState.settings.railPosition,
                state = shellState,
            ) { destination, navigateTo ->
                when (destination) {
                    ShellDestination.Home -> HomeRoute(
                        mediaState = mediaState,
                        onMediaClick = { navigateTo(ShellDestination.Media) },
                        onMediaSessionSelect = mediaRepository::selectSession,
                        onMediaPlayPause = mediaRepository.sessionController::playPause,
                        onMediaPrevious = mediaRepository.sessionController::previous,
                        onMediaNext = mediaRepository.sessionController::next,
                    )
                    ShellDestination.Apps -> AppsRoute(
                        onOpenSettings = { navigateTo(ShellDestination.Settings) },
                    )
                    ShellDestination.Media -> MediaRoute(
                        state = mediaState,
                        controller = mediaRepository.sessionController,
                        onSelectSession = mediaRepository::selectSession,
                        onClose = { navigateTo(ShellDestination.Home) },
                    )
                    ShellDestination.Notifications -> NotificationRoute(
                        state = notificationState,
                        repository = notificationRepository,
                    )
                    ShellDestination.Settings -> SettingsRoute(settingsStateHolder)
                    else -> PlaceholderDestination(destination)
                }
            }
        }
    }
}

private fun launchAssistant(context: Context) {
    val assistantIntent = Intent(Intent.ACTION_VOICE_COMMAND).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(assistantIntent) }
}
