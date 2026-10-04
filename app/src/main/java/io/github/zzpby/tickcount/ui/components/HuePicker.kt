package io.github.zzpby.tickcount.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.R

private val TRACK_HEIGHT = 16.dp
private val THUMB_SIZE = 26.dp

/**
 * A hue strip with a draggable knob.
 *
 * Hand-rolled rather than a `Slider` with a gradient behind it, because the track
 * here *is* the value scale — the knob's position and the colour under it have to
 * agree exactly, which a slider themed from the outside cannot promise.
 *
 * The ends are inset by half a knob so the knob's centre, not its edge, marks the
 * hue it selects; without that the first and last few degrees are unreachable.
 */
@Composable
fun HueSlider(
    hue: Float,
    onHueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.field_hue)
    val rainbow = remember {
        // 37 stops: one every ten degrees, which is finer than the eye resolves on a
        // strip this wide, and cheap enough to build once.
        List(37) { step -> Color.hsl((step * 10f) % 360f, 0.85f, 0.50f) }
    }
    val normalized = ((hue % 360f) + 360f) % 360f

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(THUMB_SIZE)
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                detectTapGestures { offset -> onHueChange(hueAt(offset.x, size.width, this)) }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> onHueChange(hueAt(offset.x, size.width, this)) },
                ) { change, _ ->
                    onHueChange(hueAt(change.position.x, size.width, this))
                }
            },
    ) {
        val span = maxWidth - THUMB_SIZE

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = THUMB_SIZE / 2)
                .fillMaxWidth()
                .height(TRACK_HEIGHT)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(rainbow)),
        )

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = span * (normalized / 360f))
                .size(THUMB_SIZE)
                .clip(CircleShape)
                .background(Color.hsl(normalized, 0.85f, 0.50f))
                .semantics { contentDescription = description },
        )
    }
}

/**
 * Maps an x position on the strip to a hue.
 *
 * [pointerWidth] is the pointer input node's width, which includes the full strip;
 * the knob's own width is subtracted so that both ends of the scale are reachable.
 */
private fun hueAt(x: Float, pointerWidth: Int, density: Density): Float {
    val knob = with(density) { THUMB_SIZE.toPx() }
    val span = (pointerWidth - knob).coerceAtLeast(1f)
    val fraction = ((x - knob / 2f) / span).coerceIn(0f, 1f)
    return (fraction * 360f).coerceIn(0f, 359.9f)
}

/** A round swatch of a hue, for showing what the knob is currently on. */
@Composable
fun HueSwatch(hue: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.hsl(hue, 0.85f, 0.50f)),
    )
}
