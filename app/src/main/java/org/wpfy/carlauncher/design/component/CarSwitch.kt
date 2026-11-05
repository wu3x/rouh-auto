package org.wpfy.carlauncher.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CarSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val trackColor = if (checked) OnTrack else OffTrack
    val handleColor = if (checked) OnHandle else OffHandle
    val handleSize = if (checked) 24.dp else 16.dp
    val handlePadding = if (checked) 4.dp else 8.dp
    val handleAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = modifier
            .width(52.dp)
            .size(width = 52.dp, height = 32.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(RoundedCornerShape(100.dp))
            .background(trackColor)
            .then(
                if (!checked) {
                    Modifier.border(2.dp, OffHandle, RoundedCornerShape(100.dp))
                } else {
                    Modifier
                },
            )
            .clickable(enabled = enabled) { onCheckedChange(!checked) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(handlePadding),
        ) {
            Box(
                modifier = Modifier
                    .size(handleSize)
                    .align(handleAlignment)
                    .background(handleColor, RoundedCornerShape(100.dp)),
            )
        }
    }
}

private val OnTrack = Color(0xFFC5D4FF)
private val OnHandle = Color(0xFF002C6F)
private val OffTrack = Color(0xFF3B3E3D)
private val OffHandle = Color(0xFF8F9190)
