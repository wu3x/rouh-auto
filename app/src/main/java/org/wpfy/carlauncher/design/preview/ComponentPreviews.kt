package org.wpfy.carlauncher.design.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.design.component.CarBadge
import org.wpfy.carlauncher.design.component.CarGridItem
import org.wpfy.carlauncher.design.component.CarHeader
import org.wpfy.carlauncher.design.component.CarIconButton
import org.wpfy.carlauncher.design.component.CarListRow
import org.wpfy.carlauncher.design.component.CarRailButton
import org.wpfy.carlauncher.design.component.CarSearchEmptyState
import org.wpfy.carlauncher.design.component.CarSearchField
import org.wpfy.carlauncher.design.component.CarSearchResultsList
import org.wpfy.carlauncher.design.component.CarSwitch
import org.wpfy.carlauncher.design.theme.CarColors
import org.wpfy.carlauncher.design.theme.CarTheme

@Preview(name = "Components 1600×720", widthDp = 1600, heightDp = 720)
@Preview(name = "Components 800×480", widthDp = 800, heightDp = 480)
@Composable
private fun ComponentCatalogPreview() {
    CarTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CarColors.Background)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CarHeader(
                title = "Settings",
                trailing = {
                    CarIconButton(
                        icon = Icons.Rounded.Search,
                        contentDescription = "Search",
                        onClick = {},
                        outlined = true,
                    )
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                CarRailButton(Icons.Rounded.Home, "Home", true, {})
                CarRailButton(Icons.Rounded.Notifications, "Notifications", false, {}, size = 56.dp, iconSize = 24.dp)
                CarBadge(text = "9+")
            }

            CarListRow(
                title = "Start on boot",
                subtitle = "Open the launcher after the head unit starts",
                trailing = { CarSwitch(checked = true, onCheckedChange = {}) },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                PreviewGridItem("Apps", Icons.Rounded.Apps, Modifier.weight(1f))
                PreviewGridItem("Settings", Icons.Rounded.Settings, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PreviewGridItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
) {
    CarGridItem(
        label = label,
        onClick = {},
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(Color.White, androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = CarColors.Background,
            )
        }
    }
}

@Preview(name = "Search pattern 800×480", widthDp = 800, heightDp = 480)
@Composable
private fun SearchPatternPreview() {
    CarTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CarColors.Background)
                .padding(16.dp),
        ) {
            CarSearchField(
                query = "maps",
                onQueryChange = {},
                placeholder = "Search apps",
            )
            CarSearchResultsList(modifier = Modifier.weight(1f)) {
                item {
                    CarListRow(
                        title = "Maps",
                        subtitle = "com.google.android.apps.maps",
                    )
                }
                item {
                    CarListRow(
                        title = "Organic Maps",
                        subtitle = "app.organicmaps",
                    )
                }
            }
        }
    }
}

@Preview(name = "Search empty 800×480", widthDp = 800, heightDp = 480)
@Composable
private fun SearchEmptyPreview() {
    CarTheme {
        CarSearchEmptyState(
            title = "No matching apps",
            message = "Try a different app name or package.",
            modifier = Modifier.background(CarColors.Background),
        )
    }
}
