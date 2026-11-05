package org.wpfy.carlauncher.design.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.design.theme.CarColors
import org.wpfy.carlauncher.design.theme.CarShapes

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun CarGridItem(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    icon: @Composable BoxScope.() -> Unit,
) {
    val interactionModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        )
    } else {
        Modifier.clickable(onClick = onClick)
    }

    Column(
        modifier = modifier
            .height(156.dp)
            .clip(CarShapes.small)
            .then(interactionModifier)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(84.dp),
            contentAlignment = Alignment.Center,
            content = icon,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            color = CarColors.TextPrimary,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (secondaryLabel != null) {
            Text(
                text = secondaryLabel,
                modifier = Modifier.fillMaxWidth(),
                color = CarColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
