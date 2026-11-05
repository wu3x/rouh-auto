package org.wpfy.carlauncher.feature.home

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AltRoute
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.rotate
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.R
import org.wpfy.carlauncher.design.theme.CarColors
import org.wpfy.carlauncher.design.theme.CarShapes
import org.wpfy.carlauncher.design.theme.CarSpacing
import org.wpfy.carlauncher.feature.map.MiniMap
import org.wpfy.carlauncher.feature.media.MediaSourceDots
import org.wpfy.carlauncher.feature.media.MediaSourceSession
import org.wpfy.carlauncher.feature.media.MediaState
import org.wpfy.carlauncher.feature.media.WavyProgressBar
import org.wpfy.carlauncher.feature.navigation.DestinationSearchOverlay
import org.wpfy.carlauncher.feature.navigation.GeoPoint
import org.wpfy.carlauncher.feature.navigation.Maneuver
import org.wpfy.carlauncher.feature.navigation.ManeuverType
import org.wpfy.carlauncher.feature.navigation.NavigationController
import org.wpfy.carlauncher.feature.navigation.NavigationError
import org.wpfy.carlauncher.feature.navigation.NavigationProgress
import org.wpfy.carlauncher.feature.navigation.NavigationState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.collect

@Composable
fun HomeRoute(
    mediaState: MediaState,
    onMediaClick: () -> Unit,
    onMediaSessionSelect: (String) -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaPrevious: () -> Unit,
    onMediaNext: () -> Unit,
) {
    val context = LocalContext.current
    val controller = remember { NavigationController.get(context) }
    val navigation by controller.state.collectAsState()
    var searchOpen by remember { mutableStateOf(false) }

    HomeScreen(
        state = HomeUiState(navigation = navigation, media = mediaState),
        onSearchClick = { searchOpen = true },
        onStopNavigation = controller::stop,
        onMediaClick = onMediaClick,
        onMediaSessionSelect = onMediaSessionSelect,
        onMediaPlayPause = onMediaPlayPause,
        onMediaPrevious = onMediaPrevious,
        onMediaNext = onMediaNext,
    )

    if (searchOpen) {
        DestinationSearchOverlay(
            controller = controller,
            onDismiss = { searchOpen = false },
            onSelected = { destination ->
                searchOpen = false
                controller.start(destination)
            },
        )
    }
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onSearchClick: () -> Unit = {},
    onStopNavigation: () -> Unit = {},
    onMediaClick: () -> Unit = {},
    onMediaSessionSelect: (String) -> Unit = {},
    onMediaPlayPause: () -> Unit = {},
    onMediaPrevious: () -> Unit = {},
    onMediaNext: () -> Unit = {},
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isPortraitFamily = maxWidth < 880.dp && maxHeight >= 560.dp
        val contextWidth = when {
            maxWidth >= 1700.dp -> 600.dp
            maxWidth >= 1150.dp -> 420.dp
            else -> 304.dp
        }

        if (isPortraitFamily) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = CarSpacing.Md, end = CarSpacing.Md, bottom = CarSpacing.Md),
                verticalArrangement = Arrangement.spacedBy(CarSpacing.Md),
            ) {
                NavigationSurface(
                    state = state.navigation,
                    onSearchClick = onSearchClick,
                    onStopNavigation = onStopNavigation,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )

                MediaCard(
                    state = state.media,
                    onClick = onMediaClick,
                    onSessionSelect = onMediaSessionSelect,
                    onPlayPause = onMediaPlayPause,
                    onPrevious = onMediaPrevious,
                    onNext = onMediaNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(196.dp),
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(CarSpacing.Md),
            ) {
                NavigationSurface(
                    state = state.navigation,
                    onSearchClick = onSearchClick,
                    onStopNavigation = onStopNavigation,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )

                ContextColumn(
                    media = state.media,
                    onMediaClick = onMediaClick,
                    onMediaSessionSelect = onMediaSessionSelect,
                    onMediaPlayPause = onMediaPlayPause,
                    onMediaPrevious = onMediaPrevious,
                    onMediaNext = onMediaNext,
                    width = contextWidth,
                )
            }
        }
    }
}

@Composable
private fun NavigationSurface(
    state: NavigationState,
    onSearchClick: () -> Unit,
    onStopNavigation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The dashboard is a glanceable live map, not a turn-by-turn navigation screen.
    // Keep the map unobstructed and preserve the existing GPS location provider.
    Box(
        modifier = modifier
            .clip(CarShapes.medium)
            .background(Color(0xFF10151B)),
    ) {
        MiniMap(modifier = Modifier.fillMaxSize())

        // Small, unobtrusive GPS marker instead of a large navigation overlay.
        Surface(
            modifier = Modifier.align(Alignment.Center).size(46.dp),
            shape = CircleShape,
            color = Color(0xFF1269E8),
            shadowElevation = 6.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(27.dp),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun TurnCard(
    progress: NavigationProgress,
    modifier: Modifier = Modifier,
) {
    val maneuver = progress.nextManeuver ?: return
    val secondary = progress.secondaryManeuver
    Column(
        modifier = modifier.width(386.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFF006A54),
                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp),
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Rounded.Navigation,
                contentDescription = null,
                modifier = Modifier
                    .size(46.dp)
                    .rotate(maneuverRotation(maneuver.type)),
                tint = Color.White,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatDistance(maneuver.distanceMeters),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = maneuver.instruction,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                maneuver.roadName?.let { road ->
                    Text(
                        text = road,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFF015141),
                    shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
                )
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.nav_then),
                color = CarColors.TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
            )
            Icon(
                imageVector = Icons.Rounded.Navigation,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                tint = CarColors.TextPrimary,
            )
            Text(
                text = secondary?.instruction ?: stringResource(R.string.maneuver_continue),
                color = CarColors.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EtaCard(
    progress: NavigationProgress,
    onStopNavigation: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.width(377.dp),
        color = CarColors.Surface,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CarColors.Outline),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = formatDuration(progress.remainingDurationSeconds),
                color = Color(0xFF5BBF6B),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = listOfNotNull(
                    formatDistance(progress.remainingDistanceMeters).takeIf(String::isNotBlank),
                    formatEta(progress.etaEpochMillis),
                ).joinToString(" · "),
                color = CarColors.TextSecondary,
                style = MaterialTheme.typography.bodyLarge,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(CarColors.Outline),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TonalActionButton(Icons.Rounded.Close, stringResource(R.string.nav_stop), onStopNavigation, wide = true)
                TonalActionButton(Icons.Rounded.AltRoute, stringResource(R.string.nav_routes), {})
                TonalActionButton(Icons.Rounded.Search, stringResource(R.string.nav_search), onSearchClick)
                TonalActionButton(Icons.Rounded.Place, stringResource(R.string.nav_places), {})
            }
        }
    }
}

private fun maneuverRotation(type: ManeuverType): Float = when (type) {
    ManeuverType.TurnLeft, ManeuverType.ExitLeft -> -90f
    ManeuverType.TurnRight, ManeuverType.ExitRight -> 90f
    ManeuverType.SlightLeft -> -45f
    ManeuverType.SlightRight -> 45f
    ManeuverType.SharpLeft -> -135f
    ManeuverType.SharpRight -> 135f
    ManeuverType.UTurn -> 180f
    else -> 0f
}

private fun formatDistance(meters: Double?): String {
    if (meters == null || meters < 0.0) return ""
    return if (meters < 1_000.0) {
        val rounded = if (meters < 100.0) (meters / 10.0).toInt() * 10 else (meters / 50.0).toInt() * 50
        "$rounded م"
    } else {
        String.format(Locale.US, "%.1f كم", meters / 1_000.0)
    }
}

private fun formatDuration(seconds: Long?): String {
    if (seconds == null || seconds < 0L) return ""
    val minutes = (seconds + 30L) / 60L
    return if (minutes < 60L) {
        "$minutes د"
    } else {
        val hours = minutes / 60L
        val remainder = minutes % 60L
        if (remainder == 0L) "$hours س" else "$hours س $remainder د"
    }
}

private val EtaFormatter = DateTimeFormatter.ofPattern("h:mm a")

private fun formatEta(epochMillis: Long?): String? {
    if (epochMillis == null || epochMillis <= 0L) return null
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(EtaFormatter)
        .lowercase(Locale.getDefault())
}

@Composable
private fun NavigationStatusCard(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.width(377.dp),
        color = CarColors.Surface,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CarColors.Outline),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = CarColors.AccentMuted,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = CarColors.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = body,
                        color = CarColors.TextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Surface(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
                color = CarColors.SurfaceElevated,
                shape = RoundedCornerShape(36.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = CarColors.TextPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = actionLabel,
                        color = CarColors.TextPrimary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun ContextColumn(
    media: MediaState,
    onMediaClick: () -> Unit,
    onMediaSessionSelect: (String) -> Unit,
    onMediaPlayPause: () -> Unit,
    onMediaPrevious: () -> Unit,
    onMediaNext: () -> Unit,
    width: Dp,
) {
    Column(
        modifier = Modifier
            .width(width)
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Top,
    ) {
        MediaCard(
            state = media,
            onClick = onMediaClick,
            onSessionSelect = onMediaSessionSelect,
            onPlayPause = onMediaPlayPause,
            onPrevious = onMediaPrevious,
            onNext = onMediaNext,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        )
    }
}

@Composable
private fun MediaCard(
    state: MediaState,
    onClick: () -> Unit,
    onSessionSelect: (String) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sessions = state.availableSessions.ifEmpty {
        listOfNotNull(state.selectedSession)
    }
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = CarColors.SurfaceContainer,
        shape = CarShapes.medium,
    ) {
        if (state.hasSession && sessions.isNotEmpty()) {
            val initialPage = sessions.indexOfFirst { it.id == state.selectedSessionId }
                .coerceAtLeast(0)
            val pagerState = rememberPagerState(initialPage = initialPage) { sessions.size }
            val sessionIds = sessions.map(MediaSourceSession::id)
            val currentSelectedSessionId = rememberUpdatedState(state.selectedSessionId)

            LaunchedEffect(state.selectedSessionId, sessionIds) {
                val selectedIndex = sessionIds.indexOf(state.selectedSessionId)
                if (selectedIndex >= 0 && pagerState.currentPage != selectedIndex) {
                    pagerState.scrollToPage(selectedIndex)
                }
            }

            LaunchedEffect(pagerState, sessionIds) {
                androidx.compose.runtime.snapshotFlow { pagerState.settledPage }
                    .collect { page ->
                        val id = sessionIds.getOrNull(page) ?: return@collect
                        if (id != currentSelectedSessionId.value) {
                            onSessionSelect(id)
                        }
                    }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1,
                ) { page ->
                    PlayingMediaContent(
                        session = sessions[page],
                        onPlayPause = onPlayPause,
                        onPrevious = onPrevious,
                        onNext = onNext,
                    )
                }

                MediaSourceDots(
                    count = sessions.size,
                    selectedIndex = pagerState.currentPage,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                )
            }
        } else {
            IdleMediaContent()
        }
    }
}

@Composable
private fun IdleMediaContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier.size(88.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.12f),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = Color.White,
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Nothing playing",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = "Open Media to choose something to play",
            color = Color.White.copy(alpha = 0.76f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun PlayingMediaContent(
    session: MediaSourceSession,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val artwork = session.artwork
        if (artwork != null) {
            Image(
                bitmap = artwork.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.10f),
                        0.46f to Color.Black.copy(alpha = 0.32f),
                        1f to Color.Black.copy(alpha = 0.92f),
                    ),
                ),
        )

        if (artwork == null) {
            DefaultCover(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-44).dp),
            )
        }

        session.source?.icon?.let { icon ->
            Image(
                bitmap = icon.asImageBitmap(),
                contentDescription = session.source.label,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(34.dp),
                contentScale = ContentScale.Fit,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = session.title.ifBlank { "Unknown track" },
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = session.artist.ifBlank { session.source?.label.orEmpty() },
                color = Color.White.copy(alpha = 0.80f),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(10.dp))
            WavyProgressBar(
                progress = session.progress,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MediaControl(
                    icon = Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous",
                    onClick = onPrevious,
                    enabled = session.controls.canSkipPrevious,
                )
                Surface(
                    onClick = onPlayPause,
                    enabled = session.controls.canPlayPause,
                    modifier = Modifier.size(58.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.92f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (session.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (session.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(32.dp),
                            tint = Color(0xFF171717),
                        )
                    }
                }
                MediaControl(
                    icon = Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    onClick = onNext,
                    enabled = session.controls.canSkipNext,
                )
            }
        }
    }
}

@Composable
private fun MediaControl(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(40.dp),
            tint = Color.White.copy(alpha = if (enabled) 1f else 0.35f),
        )
    }
}

@Composable
private fun TonalActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    wide: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(if (wide) 88.dp else 56.dp)
            .height(56.dp),
        color = if (wide) CarColors.SurfaceElevated else Color.Transparent,
        shape = RoundedCornerShape(56.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(32.dp),
                tint = CarColors.TextPrimary,
            )
        }
    }
}

/** Shown in the middle of the player when the playing source has no cover art of its own. */
@Composable
private fun DefaultCover(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(28.dp)
    val gold = Brush.linearGradient(
        listOf(Color(0xFFF3E2B3), Color(0xFF9C7E43), Color(0xFFF3E2B3)),
    )
    Box(
        modifier = modifier
            .fillMaxWidth(0.60f)
            .aspectRatio(1f)
            .shadow(
                elevation = 28.dp,
                shape = shape,
                ambientColor = Color(0xFFE2C275),
                spotColor = Color(0xFFE2C275),
            )
            .clip(shape)
            .border(width = 1.5.dp, brush = gold, shape = shape),
    ) {
        Image(
            painter = painterResource(R.drawable.default_cover),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.06f),
                        1f to Color.Black.copy(alpha = 0.28f),
                    ),
                ),
        )
    }
}
