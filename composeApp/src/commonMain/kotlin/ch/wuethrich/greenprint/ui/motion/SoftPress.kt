package ch.wuethrich.greenprint.ui.motion

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Soft press feedback — scales the composable down to [pressedScale] when the
 * given [interactionSource] reports a press, then springs back. Calm, not
 * bouncy: damping is high so the rebound settles in one motion.
 *
 * Usage:
 *   val interaction = remember { MutableInteractionSource() }
 *   Button(
 *       interactionSource = interaction,
 *       modifier = Modifier.softPress(interaction),
 *       ...
 *   )
 */
fun Modifier.softPress(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.98f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "softPressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Boolean-driven variant for cases that aren't backed by an InteractionSource
 * (drag handles, custom tap detectors).
 */
@Composable
fun Modifier.softPress(
    pressed: Boolean,
    pressedScale: Float = 0.98f,
): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "softPressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
