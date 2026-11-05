package org.wpfy.carlauncher.feature.navigation

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import org.wpfy.carlauncher.R
import org.wpfy.carlauncher.design.theme.CarColors

private enum class SearchStatus { Idle, Loading, Empty, Error, Results }

/** Destination search sheet: type (or speak) a place, pick a result, navigation starts. */
@Composable
fun DestinationSearchOverlay(
    controller: NavigationController,
    onDismiss: () -> Unit,
    onSelected: (NavigationDestination) -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<NavigationDestination>>(emptyList()) }
    var status by remember { mutableStateOf(SearchStatus.Idle) }

    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    }
    val speechAvailable = remember {
        runCatching { context.packageManager.queryIntentActivities(speechIntent, 0).isNotEmpty() }
            .getOrDefault(false)
    }
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spoken.isNullOrBlank()) query = spoken
    }

    LaunchedEffect(query) {
        val text = query.trim()
        if (text.length < 2) {
            results = emptyList()
            status = SearchStatus.Idle
            return@LaunchedEffect
        }
        status = SearchStatus.Loading
        delay(450) // debounce typing; also keeps the free geocoding services happy
        controller.search(text).fold(
            onSuccess = { found ->
                results = found
                status = if (found.isEmpty()) SearchStatus.Empty else SearchStatus.Results
            },
            onFailure = {
                results = emptyList()
                status = SearchStatus.Error
            },
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val focusManager = LocalFocusManager.current
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.TopCenter,
            ) {
                Surface(
                    modifier = Modifier
                        .padding(top = 24.dp, start = 16.dp, end = 16.dp)
                        .widthIn(max = 680.dp)
                        .fillMaxWidth()
                        .clickable(enabled = false) {},
                    color = CarColors.Surface,
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, CarColors.Outline),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                singleLine = true,
                                placeholder = { Text(stringResource(R.string.nav_search_placeholder)) },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                                shape = RoundedCornerShape(16.dp),
                            )
                            if (speechAvailable) {
                                IconButton(onClick = { runCatching { speechLauncher.launch(speechIntent) } }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Mic,
                                        contentDescription = stringResource(R.string.nav_voice_search),
                                        tint = CarColors.TextPrimary,
                                    )
                                }
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.settings_back),
                                    tint = CarColors.TextPrimary,
                                )
                            }
                        }

                        when (status) {
                            SearchStatus.Idle -> Hint(stringResource(R.string.nav_search_hint))
                            SearchStatus.Loading -> Hint(stringResource(R.string.nav_search_loading))
                            SearchStatus.Empty -> Hint(stringResource(R.string.nav_search_empty))
                            SearchStatus.Error -> Hint(stringResource(R.string.nav_search_error))
                            SearchStatus.Results -> LazyColumn(
                                modifier = Modifier.heightIn(max = 360.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                items(results) { destination ->
                                    ResultRow(destination) {
                                        focusManager.clearFocus()
                                        onSelected(destination)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
        color = CarColors.TextSecondary,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun ResultRow(destination: NavigationDestination, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Place,
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = CarColors.AccentMuted,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = destination.label,
                color = CarColors.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            destination.address?.let { address ->
                Text(
                    text = address,
                    color = CarColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
