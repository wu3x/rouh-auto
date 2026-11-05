package org.wpfy.carlauncher.design.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.design.theme.CarColors
import org.wpfy.carlauncher.design.theme.CarSpacing

@Composable
fun CarSearchEmptyState(
    title: String,
    message: String? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.SearchOff,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(CarSpacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CarColors.TextDisabled,
        )
        Text(
            text = title,
            modifier = Modifier.padding(top = CarSpacing.Md),
            color = CarColors.TextPrimary,
            style = MaterialTheme.typography.titleLarge,
        )
        if (!message.isNullOrBlank()) {
            Text(
                text = message,
                modifier = Modifier.padding(top = CarSpacing.Sm),
                color = CarColors.TextSecondary,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
fun CarSearchResultsList(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    content: LazyListScope.() -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(CarSpacing.Xs),
        content = content,
    )
}
