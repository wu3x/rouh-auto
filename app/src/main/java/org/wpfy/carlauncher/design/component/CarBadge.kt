package org.wpfy.carlauncher.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.design.theme.CarColors

@Composable
fun CarBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .background(CarColors.AccentMuted, RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = ColorOnBadge,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

private val ColorOnBadge = androidx.compose.ui.graphics.Color(0xFF002C6F)
