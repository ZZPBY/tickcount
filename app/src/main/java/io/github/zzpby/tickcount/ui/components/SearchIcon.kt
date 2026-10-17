package io.github.zzpby.tickcount.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A magnifier: a ring and a stem.
 *
 * Hand-drawn like the other icons here, for the same reason — two shapes do not justify the
 * icon artifact. The ring is drawn slightly left of centre and the stem runs to the corner,
 * because that is where the two meet without the stem poking out of the ring.
 */
@Composable
fun SearchIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = LocalContentColor.current,
    strokeWidth: Dp = 1.9.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = strokeWidth.toPx()
        // The stroke is centred on the path, so everything is inset by half of it to keep
        // the ring inside its canvas.
        val inset = stroke / 2f
        val radius = (w - inset * 2f) * 0.32f
        val centre = Offset(inset + radius, inset + radius)

        drawCircle(
            color = color,
            radius = radius,
            center = centre,
            style = Stroke(width = stroke),
        )
        drawLine(
            color = color,
            start = Offset(centre.x + radius * 0.72f, centre.y + radius * 0.72f),
            end = Offset(w - inset, h - inset),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
