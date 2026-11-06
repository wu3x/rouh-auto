package org.wpfy.carlauncher

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.wpfy.carlauncher.feature.home.WelcomePreferences
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
import org.wpfy.carlauncher.design.theme.ThmanyahFontFamily
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
    var welcomeVisible by remember { mutableStateOf(WelcomePreferences.enabled(context)) }
    var welcomeLetters by remember { mutableStateOf(0) }
    val welcomeText = remember(context) { "أهلاً "+WelcomePreferences.name(context) }
    LaunchedEffect(Unit) {
        if (welcomeVisible) {
            for (i in 1..welcomeText.length) {
                welcomeLetters = i
                delay(85)
            }
            delay(1300)
            welcomeVisible = false
        }
    }
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
        Box(modifier = Modifier.fillMaxSize()) {
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
        if (welcomeVisible && !settingsState.isLoading && settingsState.settings.onboardingCompleted) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = welcomeText.take(welcomeLetters),
                        color = Color(0xFFF4F4F4),
                        fontSize = 42.sp,
                        fontFamily = ThmanyahFontFamily,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "CAR LAUNCHER",
                        color = Color(0xFF777F8C),
                        fontSize = 12.sp,
                        fontFamily = ThmanyahFontFamily,
                        modifier = Modifier.padding(top = 18.dp),
                    )
                }
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
