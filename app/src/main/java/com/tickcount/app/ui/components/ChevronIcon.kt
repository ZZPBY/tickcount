package com.tickcount.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A chevron drawn directly on the canvas.
 *
 * Hand-drawing it keeps the app on `material3` alone: the Compose icon
 * artifact carries several thousand vectors to ship two chevrons and a plus.
 */
@Composable
fun ChevronIcon(
    pointsLeft: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = LocalContentColor.current,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            if (pointsLeft) {
                moveTo(w * 0.62f, h * 0.19f)
                lineTo(w * 0.33f, h * 0.50f)
                lineTo(w * 0.62f, h * 0.81f)
            } else {
                moveTo(w * 0.38f, h * 0.19f)
                lineTo(w * 0.67f, h * 0.50f)
                lineTo(w * 0.38f, h * 0.81f)
            }
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}
