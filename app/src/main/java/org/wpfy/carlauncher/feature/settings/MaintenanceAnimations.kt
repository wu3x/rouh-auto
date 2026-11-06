package org.wpfy.carlauncher.feature.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Native Compose equivalent of a dynamic label button. */
@Composable
internal fun AnimatedScanButton(busy: Boolean, finished: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled && !busy, modifier = Modifier.animateContentSize(animationSpec = tween(240))) {
        AnimatedContent(targetState = when {
            busy -> "اصبر شوي، نفحص لك..."
            finished -> "فحص مرة ثانية"
            else -> "فحص الآن"
        }, label = "scan-button-label") { label -> Text(label) }
    }
}

/** Hold continuously for 1.5 seconds to confirm a destructive action; release cancels. */
@Composable
internal fun HoldToDeleteButton(onConfirmed: () -> Unit) {
    var holding by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(holding) {
        if (holding) {
            val start = withFrameNanos { it }
            while (holding && progress < 1f) {
                val now = withFrameNanos { it }
                progress = ((now - start) / 1_500_000_000f).coerceIn(0f, 1f)
            }
            if (holding && progress >= 1f && !completed) {
                completed = true
                onConfirmed()
            }
        } else {
            animate( progress, 0f, animationSpec = tween(180)) { value, _ -> progress = value }
        }
    }
    Box(
        modifier = Modifier
            .height(48.dp)
            .widthIn(min = 190.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF4B2020))
            .pointerInput(completed) {
                detectTapGestures(onPress = {
                    if (completed) return@detectTapGestures
                    holding = true
                    tryAwaitRelease()
                    holding = false
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.fillMaxSize().fillMaxWidth(progress).align(Alignment.CenterStart).background(Color(0xFFB93838)))
        Text(if (completed) "تم التأكيد" else if (holding) "استمر بالضغط..." else "اضغط مطولًا للحذف", color = Color.White)
    }
}
