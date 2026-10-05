package io.github.zzpby.tickcount.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * An eye, drawn on the canvas like the rest of the app's icons.
 *
 * [revealed] draws the plain eye; otherwise a stroke crosses it out. The lens and the pupil
 * are the same in both, so the button does not shift or resize when it is pressed — only
 * the stroke through it appears and disappears.
 */
@Composable
fun EyeIcon(
    revealed: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    color: Color = LocalContentColor.current,
    strokeWidth: Dp = 1.8.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(
            width = strokeWidth.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )

        // The lens: two curves meeting at the corners, which leaves the middle of the eye
        // clear for the pupil rather than drawing it as a filled almond.
        val lens = Path().apply {
            moveTo(w * 0.08f, h * 0.50f)
            cubicTo(w * 0.30f, h * 0.20f, w * 0.70f, h * 0.20f, w * 0.92f, h * 0.50f)
            cubicTo(w * 0.70f, h * 0.80f, w * 0.30f, h * 0.80f, w * 0.08f, h * 0.50f)
            close()
        }
        drawPath(path = lens, color = color, style = stroke)
        drawCircle(
            color = color,
            radius = w * 0.12f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = stroke,
        )

        if (!revealed) {
            drawLine(
                color = color,
                start = Offset(w * 0.16f, h * 0.16f),
                end = Offset(w * 0.84f, h * 0.84f),
                strokeWidth = strokeWidth.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}
