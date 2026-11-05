package org.wpfy.carlauncher.design.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.wpfy.carlauncher.design.theme.CarTheme
import org.wpfy.carlauncher.feature.home.HomeScreen
import org.wpfy.carlauncher.feature.home.HomeUiState
import org.wpfy.carlauncher.feature.media.MediaControls
import org.wpfy.carlauncher.feature.media.MediaState
import org.wpfy.carlauncher.feature.navigation.Maneuver
import org.wpfy.carlauncher.feature.navigation.ManeuverType
import org.wpfy.carlauncher.feature.navigation.NavigationProgress
import org.wpfy.carlauncher.feature.navigation.NavigationState
import org.wpfy.carlauncher.shell.LauncherShell

@Preview(name = "YT5760D 1600×720", widthDp = 1600, heightDp = 720)
@Preview(name = "BMW iX 1920×584", widthDp = 1920, heightDp = 584)
@Preview(name = "Widescreen 1280×480", widthDp = 1280, heightDp = 480)
@Preview(name = "Minimum widescreen 1120×480", widthDp = 1120, heightDp = 480)
@Preview(name = "Vertical rail 880×480", widthDp = 880, heightDp = 480)
@Preview(name = "Compact 800×480", widthDp = 800, heightDp = 480)
@Preview(name = "Compact 748×450", widthDp = 748, heightDp = 450)
@Preview(name = "Portrait 800×880", widthDp = 800, heightDp = 880)
@Preview(name = "Short portrait 790×686", widthDp = 790, heightDp = 686)
@Composable
private fun LauncherShellPreview() {
    CarTheme {
        LauncherShell()
    }
}

@Preview(name = "Home parked 1600×720", widthDp = 1600, heightDp = 720)
@Preview(name = "Home parked 1920×584", widthDp = 1920, heightDp = 584)
@Preview(name = "Home parked 1280×480", widthDp = 1280, heightDp = 480)
@Preview(name = "Home parked 1120×480", widthDp = 1120, heightDp = 480)
@Preview(name = "Home parked 880×480", widthDp = 880, heightDp = 480)
@Preview(name = "Home parked 800×480", widthDp = 800, heightDp = 480)
@Preview(name = "Home parked 748×450", widthDp = 748, heightDp = 450)
@Preview(name = "Home parked 800×880", widthDp = 800, heightDp = 880)
@Preview(name = "Home parked 790×686", widthDp = 790, heightDp = 686)
@Composable
private fun HomeParkedPreview() {
    CarTheme {
        LauncherShell { _, _ -> HomeScreen(state = HomeUiState()) }
    }
}

@Preview(name = "Home active navigation 1600×720", widthDp = 1600, heightDp = 720)
@Composable
private fun HomeActiveNavigationPreview() {
    CarTheme {
        LauncherShell { _, _ ->
            HomeScreen(
                state = HomeUiState(
                    navigation = NavigationState(
                        active = true,
                        progress = NavigationProgress(
                            nextManeuver = Maneuver(
                                type = ManeuverType.TurnRight,
                                instruction = "Turn right",
                                roadName = "101 W Pacific Coast Hwy",
                                distanceMeters = 106.68,
                            ),
                            secondaryManeuver = Maneuver(
                                type = ManeuverType.TurnRight,
                                instruction = "Turn right",
                            ),
                            remainingDistanceMeters = 52_303.7,
                            remainingDurationSeconds = 3_000L,
                            etaEpochMillis = 1_788_226_760_000L,
                        ),
                    ),
                    media = MediaState(
                        hasSession = true,
                        title = "You got to listen",
                        artist = "Michael Evans",
                        positionMs = 72_000L,
                        durationMs = 225_000L,
                        isPlaying = true,
                        controls = MediaControls(
                            canPlayPause = true,
                            canSkipPrevious = true,
                            canSkipNext = true,
                        ),
                    ),
                ),
            )
        }
    }
}
