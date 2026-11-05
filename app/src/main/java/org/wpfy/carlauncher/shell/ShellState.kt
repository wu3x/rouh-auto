package org.wpfy.carlauncher.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable

@Stable
class ShellState(initialDestination: ShellDestination) {
    var destination by mutableStateOf(initialDestination)
        private set

    fun navigateTo(destination: ShellDestination) {
        this.destination = destination
    }
}

@Composable
fun rememberShellState(
    initialDestination: ShellDestination = ShellDestination.Home,
): ShellState = rememberSaveable(
    saver = Saver(
        save = { state -> state.destination.name },
        restore = { savedDestination ->
            val destination = runCatching {
                ShellDestination.valueOf(savedDestination)
            }.getOrDefault(initialDestination)
            ShellState(destination)
        },
    ),
) {
    ShellState(initialDestination)
}
