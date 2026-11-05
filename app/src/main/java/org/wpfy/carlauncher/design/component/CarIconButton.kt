package org.wpfy.carlauncher.design.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.design.theme.CarColors
import org.wpfy.carlauncher.design.theme.CarShapes

@Composable
fun CarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    outlined: Boolean = false,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(56.dp)
            .clip(CarShapes.extraLarge)
            .then(
                if (outlined) {
                    Modifier.border(2.dp, CarColors.TextDisabled, CarShapes.extraLarge)
                } else {
                    Modifier
                },
        ),
        enabled = enabled,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = Color.Transparent,
            contentColor = CarColors.TextPrimary,
            disabledContentColor = CarColors.TextDisabled,
        ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(34.dp),
        )
    }
}
