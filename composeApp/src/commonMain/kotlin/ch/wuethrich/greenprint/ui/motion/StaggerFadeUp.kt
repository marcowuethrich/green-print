package ch.wuethrich.greenprint.ui.motion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ch.wuethrich.greenprint.ui.theme.LocalAnimationsEnabled
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Buttery staggered entrance. Wraps content and fades it in from below after
 * `index * stepMs` milliseconds — the curtain-rising feel from the style guide.
 *
 * Usage:
 *   items.forEachIndexed { i, item ->
 *     StaggerFadeUp(index = i) { Card(...) { ... } }
 *   }
 */
@Composable
fun StaggerFadeUp(
    index: Int,
    modifier: Modifier = Modifier,
    stepMs: Int = 80,
    initialDelayMs: Int = 60,
    durationMs: Int = 720,
    content: @Composable () -> Unit,
) {
    val animationsEnabled = LocalAnimationsEnabled.current
    var visible by remember { mutableStateOf(!animationsEnabled) }
    
    if (animationsEnabled) {
        LaunchedEffect(Unit) {
            delay((initialDelayMs + index * stepMs).toLong().milliseconds)
            visible = true
        }
    }

    val finalDuration = if (animationsEnabled) durationMs else 0

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(finalDuration)) +
                slideInVertically(
                    initialOffsetY = { it / 4 + 28 },
                    animationSpec = tween(finalDuration),
                ),
    ) {
        content()
    }
}
