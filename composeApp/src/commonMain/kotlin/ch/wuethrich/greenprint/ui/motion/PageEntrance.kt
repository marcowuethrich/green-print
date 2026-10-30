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
import androidx.compose.ui.platform.LocalDensity
import ch.wuethrich.greenprint.ui.theme.DesignSystem
import ch.wuethrich.greenprint.ui.theme.LocalAnimationsEnabled

/**
 * The style guide's page entrance: the content fades in and slides up 16dp over the standard
 * duration, once, when the page opens. With animations off it is simply there.
 */
@Composable
fun PageEntrance(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val animationsEnabled = LocalAnimationsEnabled.current
    var visible by remember { mutableStateOf(!animationsEnabled) }
    LaunchedEffect(Unit) { visible = true }
    val duration = if (animationsEnabled) DesignSystem.Motion.DURATION_STANDARD_MS else 0
    val offset = with(LocalDensity.current) { DesignSystem.Space4.roundToPx() }

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(animationSpec = tween(duration)) +
            slideInVertically(initialOffsetY = { offset }, animationSpec = tween(duration)),
    ) {
        content()
    }
}
