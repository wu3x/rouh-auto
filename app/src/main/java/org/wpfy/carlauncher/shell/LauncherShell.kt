package org.wpfy.carlauncher.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.design.theme.CarColors
import org.wpfy.carlauncher.design.theme.CarDimensions
import org.wpfy.carlauncher.design.theme.CarShapes
import org.wpfy.carlauncher.design.theme.CarSpacing
import org.wpfy.carlauncher.design.component.CarHeader
import org.wpfy.carlauncher.data.settings.RailPosition

@Composable
fun LauncherShell(
    notificationCount: Int = 0,
    onAssistantClick: () -> Unit = {},
    railPosition: RailPosition = RailPosition.Left,
    state: ShellState = rememberShellState(),
    content: @Composable BoxScope.(ShellDestination, (ShellDestination) -> Unit) -> Unit = { destination, _ ->
        PlaceholderDestination(destination)
    },
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(CarColors.Background),
    ) {
        if (railPosition == RailPosition.Bottom) {
            Column(modifier = Modifier.fillMaxSize()) {
                LauncherSurface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(
                            top = CarSpacing.Md,
                            start = CarSpacing.Md,
                            end = CarSpacing.Md,
                        ),
                ) {
                    key(state.destination) {
                        content(state.destination, state::navigateTo)
                    }
                }

                Rail(
                    selected = state.destination,
                    onDestinationSelected = state::navigateTo,
                    notificationCount = notificationCount,
                    onAssistantClick = onAssistantClick,
                    orientation = RailOrientation.Horizontal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                if (railPosition == RailPosition.Left) {
                    VerticalRail(
                        state = state,
                        notificationCount = notificationCount,
                        onAssistantClick = onAssistantClick,
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(
                            top = CarSpacing.Md,
                            start = CarSpacing.Md,
                            end = CarSpacing.Md,
                            bottom = CarSpacing.Md,
                        ),
                ) {
                    key(state.destination) {
                        content(state.destination, state::navigateTo)
                    }
                }

                if (railPosition == RailPosition.Right) {
                    VerticalRail(
                        state = state,
                        notificationCount = notificationCount,
                        onAssistantClick = onAssistantClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun VerticalRail(
    state: ShellState,
    notificationCount: Int,
    onAssistantClick: () -> Unit,
) {
    Rail(
        selected = state.destination,
        onDestinationSelected = state::navigateTo,
        notificationCount = notificationCount,
        onAssistantClick = onAssistantClick,
        orientation = RailOrientation.Vertical,
        modifier = Modifier
            .width(CarDimensions.RailWidth)
            .fillMaxHeight(),
    )
}

@Composable
private fun LauncherSurface(
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        color = CarColors.Surface,
        shape = CarShapes.medium,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = content,
        )
    }
}

@Composable
fun PlaceholderDestination(destination: ShellDestination) {
    Column(modifier = Modifier.fillMaxSize()) {
        CarHeader(title = destination.label)
    }
}
