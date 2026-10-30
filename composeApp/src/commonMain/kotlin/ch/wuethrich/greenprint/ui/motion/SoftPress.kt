package ch.wuethrich.greenprint.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private const val DEFAULT_PRESSED_SCALE = 0.98f

/**
 * Soft press feedback — scales the composable down to [pressedScale] while the
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
fun Modifier.softPress(interactionSource: InteractionSource, pressedScale: Float = DEFAULT_PRESSED_SCALE): Modifier =
    this then SoftPressElement(interactionSource, pressedScale)

private data class SoftPressElement(val interactionSource: InteractionSource, val pressedScale: Float) :
    ModifierNodeElement<SoftPressNode>() {
    override fun create(): SoftPressNode = SoftPressNode(interactionSource, pressedScale)

    override fun update(node: SoftPressNode) = node.update(interactionSource, pressedScale)

    override fun InspectorInfo.inspectableProperties() {
        name = "softPress"
        properties["interactionSource"] = interactionSource
        properties["pressedScale"] = pressedScale
    }
}

private class SoftPressNode(private var interactionSource: InteractionSource, private var pressedScale: Float) :
    Modifier.Node(),
    LayoutModifierNode {
    private val scale = Animatable(1f)
    private var pressJob: Job? = null

    override fun onAttach() = observePresses()

    fun update(interactionSource: InteractionSource, pressedScale: Float) {
        this.pressedScale = pressedScale
        if (interactionSource != this.interactionSource) {
            this.interactionSource = interactionSource
            pressJob?.cancel()
            if (isAttached) observePresses()
        }
    }

    private fun observePresses() {
        pressJob = coroutineScope.launch {
            val presses = mutableListOf<PressInteraction.Press>()
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> presses += interaction
                    is PressInteraction.Release -> presses -= interaction.press
                    is PressInteraction.Cancel -> presses -= interaction.press
                }
                val target = if (presses.isEmpty()) 1f else pressedScale
                launch {
                    scale.animateTo(
                        targetValue = target,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    )
                }
            }
        }
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            // Read inside the layer block, so each animation frame only redraws the layer.
            placeable.placeWithLayer(0, 0) {
                scaleX = scale.value
                scaleY = scale.value
            }
        }
    }
}
