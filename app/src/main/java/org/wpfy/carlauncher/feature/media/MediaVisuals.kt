package org.wpfy.carlauncher.feature.media

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
internal fun WavyProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    playedColor: Color = Color.White,
    remainingColor: Color = Color.White.copy(alpha = 0.24f),
) {
    Canvas(modifier = modifier.height(12.dp)) {
        val centerY = size.height / 2f
        val strokeWidth = 2.5.dp.toPx()
        val playedWidth = size.width * progress.coerceIn(0f, 1f)

        drawLine(
            color = remainingColor,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )

        if (playedWidth <= 0f) return@Canvas

        val amplitude = 2.25.dp.toPx()
        val wavelength = 14.dp.toPx()
        val path = Path().apply { moveTo(0f, centerY) }
        var x = 2f
        while (x < playedWidth) {
            val y = centerY + sin((x / wavelength) * 2.0 * PI).toFloat() * amplitude
            path.lineTo(x, y)
            x += 2f
        }
        path.lineTo(playedWidth, centerY)
        drawPath(
            path = path,
            color = playedColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}

@Composable
internal fun MediaSourceDots(
    count: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.38f),
) {
    if (count <= 1) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(if (index == selectedIndex) 8.dp else 6.dp)
                    .background(
                        color = if (index == selectedIndex) activeColor else inactiveColor,
                        shape = CircleShape,
                    ),
            )
        }
    }
}
