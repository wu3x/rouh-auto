package org.wpfy.carlauncher.feature.media

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.wpfy.carlauncher.design.component.CarHeader
import org.wpfy.carlauncher.design.theme.CarColors
import org.wpfy.carlauncher.design.theme.CarShapes
import org.wpfy.carlauncher.design.theme.CarSpacing

@Composable
fun MediaRoute(
    state: MediaState,
    controller: MediaSessionController,
    onSelectSession: (String) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    MediaScreen(
        state = state,
        onSelectSession = onSelectSession,
        onClose = onClose,
        onRequestAccess = {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        },
        onOpenSource = { controller.openSource(context) },
        onPlayPause = controller::playPause,
        onPrevious = controller::previous,
        onNext = controller::next,
        onRepeat = controller::toggleRepeat,
        onShuffle = controller::toggleShuffle,
        onFavorite = controller::toggleFavorite,
    )
}

@Composable
fun MediaScreen(
    state: MediaState,
    onSelectSession: (String) -> Unit,
    onClose: () -> Unit,
    onRequestAccess: () -> Unit,
    onOpenSource: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    onShuffle: () -> Unit,
    onFavorite: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        CarHeader(
            title = "Now playing",
            leading = {
                MediaHeaderAction(
                    icon = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    onClick = onClose,
                )
            },
            trailing = {
                Surface(
                    onClick = onClose,
                    shape = RoundedCornerShape(28.dp),
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CarColors.AccentStrong),
                ) {
                    Text(
                        text = "Close",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        color = CarColors.AccentMuted,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            },
        )

        when {
            state.accessStatus == MediaAccessStatus.PermissionRequired -> MediaAccessRequired(
                onRequestAccess = onRequestAccess,
            )

            !state.hasSession -> MediaIdle()

            else -> MediaNowPlaying(
                state = state,
                onSelectSession = onSelectSession,
                onOpenSource = onOpenSource,
                onPlayPause = onPlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onRepeat = onRepeat,
                onShuffle = onShuffle,
                onFavorite = onFavorite,
            )
        }
    }
}

@Composable
private fun MediaNowPlaying(
    state: MediaState,
    onSelectSession: (String) -> Unit,
    onOpenSource: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    onShuffle: () -> Unit,
    onFavorite: () -> Unit,
) {
    val sessions = state.availableSessions.ifEmpty {
        listOfNotNull(state.selectedSession)
    }
    if (sessions.isEmpty()) return

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
                    onSelectSession(id)
                }
            }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clip(CarShapes.medium),
    ) {
        val artworkSize = if (maxHeight >= 500.dp) 300.dp else 178.dp

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
        ) { page ->
            MediaPlayerPage(
                session = sessions[page],
                artworkSize = artworkSize,
                onOpenSource = onOpenSource,
                onPlayPause = onPlayPause,
                onPrevious = onPrevious,
                onNext = onNext,
                onRepeat = onRepeat,
                onShuffle = onShuffle,
                onFavorite = onFavorite,
            )
        }

        MediaSourceDots(
            count = sessions.size,
            selectedIndex = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 20.dp, end = 24.dp),
        )
    }
}

@Composable
private fun MediaPlayerPage(
    session: MediaSourceSession,
    artworkSize: androidx.compose.ui.unit.Dp,
    onOpenSource: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    onShuffle: () -> Unit,
    onFavorite: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        session.artwork?.let { artwork ->
            Image(
                bitmap = artwork.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.28f,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color.Black.copy(alpha = 0.80f),
                        0.58f to Color.Black.copy(alpha = 0.62f),
                        1f to Color.Black.copy(alpha = 0.78f),
                    ),
                ),
        )

        session.source?.let { source ->
            Surface(
                onClick = onOpenSource,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 12.dp, start = 18.dp),
                color = Color.Black.copy(alpha = 0.24f),
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    source.icon?.let { icon ->
                        Image(
                            bitmap = icon.asImageBitmap(),
                            contentDescription = source.label,
                            modifier = Modifier.size(30.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    Text(
                        text = source.label,
                        color = Color.White.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 26.dp),
            horizontalArrangement = Arrangement.spacedBy(30.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArtwork(
                session = session,
                modifier = Modifier.size(artworkSize),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 680.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = session.title.ifBlank { "Unknown track" },
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = session.artist.ifBlank { session.source?.label.orEmpty() },
                    color = Color.White.copy(alpha = 0.76f),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(22.dp))
                MediaProgress(session = session)
                Spacer(modifier = Modifier.height(14.dp))
                MediaActions(
                    session = session,
                    onPlayPause = onPlayPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onRepeat = onRepeat,
                    onShuffle = onShuffle,
                    onFavorite = onFavorite,
                )
            }
        }
    }
}

@Composable
private fun AlbumArtwork(
    session: MediaSourceSession,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier,
        color = CarColors.SurfaceContainer,
        shape = CarShapes.small,
    ) {
        val artwork = session.artwork
        if (artwork != null) {
            Image(
                bitmap = artwork.asImageBitmap(),
                contentDescription = "Album art",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = CarColors.TextDisabled,
                )
            }
        }
    }
}

@Composable
private fun MediaProgress(session: MediaSourceSession) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WavyProgressBar(
            progress = session.progress,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDuration(session.positionMs),
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = formatDuration(session.durationMs),
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun MediaActions(
    session: MediaSourceSession,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    onShuffle: () -> Unit,
    onFavorite: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (session.controls.canShuffle) {
            MediaAction(
                icon = Icons.Rounded.Shuffle,
                contentDescription = "Shuffle",
                selected = session.isShuffleEnabled == true,
                onClick = onShuffle,
            )
        }
        if (session.controls.canFavorite) {
            MediaAction(
                icon = if (session.isFavorite == true) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = "Favorite",
                selected = session.isFavorite == true,
                onClick = onFavorite,
            )
        }
        MediaAction(
            icon = Icons.Rounded.SkipPrevious,
            contentDescription = "Previous",
            enabled = session.controls.canSkipPrevious,
            onClick = onPrevious,
        )
        MediaAction(
            icon = if (session.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (session.isPlaying) "Pause" else "Play",
            enabled = session.controls.canPlayPause,
            primary = true,
            onClick = onPlayPause,
        )
        MediaAction(
            icon = Icons.Rounded.SkipNext,
            contentDescription = "Next",
            enabled = session.controls.canSkipNext,
            onClick = onNext,
        )
        if (session.controls.canRepeat) {
            MediaAction(
                icon = if (session.isRepeatEnabled == true) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                contentDescription = "Repeat",
                selected = session.isRepeatEnabled == true,
                onClick = onRepeat,
            )
        }
    }
}

@Composable
private fun MediaAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    selected: Boolean = false,
    primary: Boolean = false,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        color = if (primary) CarColors.AccentMuted else Color.Transparent,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(if (primary) 36.dp else 30.dp),
                tint = when {
                    !enabled -> CarColors.TextDisabled.copy(alpha = 0.45f)
                    primary -> Color(0xFF002C6F)
                    selected -> CarColors.AccentMuted
                    else -> CarColors.TextPrimary
                },
            )
        }
    }
}

@Composable
private fun MediaAccessRequired(onRequestAccess: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 560.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CarSpacing.Md),
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = CarColors.AccentMuted,
            )
            Text(
                text = "Allow media access",
                color = CarColors.TextPrimary,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Enable Car Launcher under Notification access so it can read and control active media sessions.",
                color = CarColors.TextSecondary,
                style = MaterialTheme.typography.bodyLarge,
            )
            Surface(
                onClick = onRequestAccess,
                shape = RoundedCornerShape(36.dp),
                color = CarColors.AccentMuted,
            ) {
                Text(
                    text = "Open notification access",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                    color = Color(0xFF002C6F),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun MediaIdle() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = CarColors.TextDisabled,
            )
            Text(
                text = "Nothing playing",
                color = CarColors.TextPrimary,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Start playback in Spotify, YouTube Music, or another media app.",
                color = CarColors.TextSecondary,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun MediaHeaderAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(48.dp),
        color = Color.Transparent,
        shape = CircleShape,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(28.dp),
                tint = CarColors.TextPrimary,
            )
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "--:--"
    val totalSeconds = durationMs / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
