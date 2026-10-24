package io.github.zzpby.tickcount.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Three bars of decreasing length, the usual shorthand for "this list can be put in
 * an order".
 */
@Composable
fun SortIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = LocalContentColor.current,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val bar = h * 0.13f
        val gap = (h - bar * 3) / 2f
        val inset = bar / 2f
        val lengths = listOf(1f, 0.68f, 0.36f)

        repeat(3) { index ->
            val y = inset + index * (bar + gap)
            drawLine(
                color = color,
                start = Offset(inset, y),
                end = Offset(w * lengths[index] - inset, y),
                strokeWidth = bar,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * A plus sign, drawn on the canvas.
 *
 * Used for the one action the home screen offers, so it matches the other two
 * hand-drawn icons rather than mixing in a font glyph.
 */
@Composable
fun PlusIcon(
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    color: Color = LocalContentColor.current,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = strokeWidth.toPx()
        val inset = stroke / 2f

        drawLine(
            color = color,
            start = Offset(w / 2f, inset),
            end = Offset(w / 2f, h - inset),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(inset, h / 2f),
            end = Offset(w - inset, h / 2f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
