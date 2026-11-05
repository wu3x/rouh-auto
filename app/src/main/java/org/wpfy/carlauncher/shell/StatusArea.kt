package org.wpfy.carlauncher.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.design.theme.CarColors
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.LaunchedEffect

@Composable
fun StatusArea(modifier: Modifier = Modifier) {
    var currentTime by remember { mutableStateOf(formattedTime()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = formattedTime()
            delay(30_000)
        }
    }

    Box(
        modifier = modifier.size(64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = currentTime,
                color = CarColors.TextPrimary,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private val TimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun formattedTime(): String = LocalTime.now().format(TimeFormatter)
