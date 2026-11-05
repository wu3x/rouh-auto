package org.wpfy.carlauncher.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.design.theme.CarColors
import org.wpfy.carlauncher.design.theme.CarDimensions
import org.wpfy.carlauncher.design.theme.CarShapes

@Composable
fun CarRailButton(
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = CarDimensions.RailActionSize,
    iconSize: Dp = 32.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CarShapes.extraLarge)
            .then(
                if (selected) {
                    Modifier.background(CarColors.SurfaceContainer, CarShapes.extraLarge)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = if (selected) CarColors.TextPrimary else CarColors.TextSecondary,
        )
    }
}
