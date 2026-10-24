package io.github.zzpby.tickcount.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The three marks of the bottom bar, drawn on the canvas.
 *
 * Hand-drawn for the same reason as the rest of the set: the Material icon artifact carries
 * several thousand vectors to ship three shapes. Each is sized by its caller, and fills the
 * canvas it is given, so nothing here has to know how tall the bar is.
 */

/** Four rounded squares, the usual shorthand for "the overview". */
@Composable
fun GridIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = LocalContentColor.current,
) {
    Canvas(modifier = modifier.size(size)) {
        val side = minOf(this.size.width, this.size.height)
        val cell = side * 0.36f
        val gap = side - cell * 2f
        val radius = CornerRadius(cell * 0.34f, cell * 0.34f)

        for (row in 0..1) {
            for (column in 0..1) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(column * (cell + gap), row * (cell + gap)),
                    size = Size(cell, cell),
                    cornerRadius = radius,
                )
            }
        }
    }
}

/**
 * A calendar page: a rounded body, a rule under the header, and two rings above it.
 *
 * The rings are what stops it reading as a plain card, so they are drawn outside the body
 * rather than clipped to it.
 */
@Composable
fun CalendarIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = LocalContentColor.current,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.085f
        val bodyTop = h * 0.17f
        val radius = CornerRadius(w * 0.17f, w * 0.17f)

        drawRoundRect(
            color = color,
            topLeft = Offset(stroke / 2f, bodyTop + stroke / 2f),
            size = Size(w - stroke, h - bodyTop - stroke),
            cornerRadius = radius,
            style = Stroke(width = stroke),
        )

        val ruleY = bodyTop + (h - bodyTop) * 0.34f
        drawLine(
            color = color,
            start = Offset(stroke, ruleY),
            end = Offset(w - stroke, ruleY),
            strokeWidth = stroke * 0.8f,
            cap = StrokeCap.Round,
        )

        val ringTop = stroke / 2f
        val ringBottom = bodyTop + stroke
        listOf(w * 0.33f, w * 0.67f).forEach { x ->
            drawLine(
                color = color,
                start = Offset(x, ringTop),
                end = Offset(x, ringBottom),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * Three sliders, the mark the bottom bar uses for "settings".
 *
 * A gear would need a dozen segments to stay legible at 20dp; three rules with a knob at a
 * different place on each read as adjustable controls at any size.
 */
@Composable
fun SlidersIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = LocalContentColor.current,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.082f
        val knob = stroke * 1.45f
        val rows = listOf(0.22f, 0.5f, 0.78f)
        val knobs = listOf(0.68f, 0.32f, 0.58f)

        rows.forEachIndexed { index, fraction ->
            val y = h * fraction
            drawLine(
                color = color,
                start = Offset(stroke / 2f, y),
                end = Offset(w - stroke / 2f, y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawCircle(color = color, radius = knob, center = Offset(w * knobs[index], y))
        }
    }
}
