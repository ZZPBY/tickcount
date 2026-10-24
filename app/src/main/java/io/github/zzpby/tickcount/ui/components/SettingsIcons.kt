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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The marks that lead the rows of the settings list.
 *
 * Line drawings on a single stroke width, all filling the canvas their caller sizes, so a
 * column of rows reads as one set. Hand-drawn for the same reason as every other icon in the
 * app: the Material artifact is several thousand vectors for a handful of shapes.
 */

/** A bell: the reminder rows. */
@Composable
fun BellIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = LocalContentColor.current,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.082f

        val body = Path().apply {
            moveTo(w * 0.24f, h * 0.68f)
            cubicTo(w * 0.24f, h * 0.34f, w * 0.34f, h * 0.18f, w * 0.5f, h * 0.18f)
            cubicTo(w * 0.66f, h * 0.18f, w * 0.76f, h * 0.34f, w * 0.76f, h * 0.68f)
            close()
        }
        drawPath(body, color, style = Stroke(width = stroke, join = StrokeJoin.Round))

        drawLine(
            color = color,
            start = Offset(w * 0.14f, h * 0.68f),
            end = Offset(w * 0.86f, h * 0.68f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawCircle(color = color, radius = stroke * 0.85f, center = Offset(w * 0.5f, h * 0.85f))
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.09f),
            end = Offset(w * 0.5f, h * 0.17f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/** A round palette with three wells: the appearance rows. */
@Composable
fun PaletteIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = LocalContentColor.current,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.082f
        val center = Offset(w / 2f, h / 2f)
        val radius = w * 0.37f

        drawCircle(color = color, radius = radius, center = center, style = Stroke(width = stroke))

        // The three wells sit on a shallow arc rather than in a row, which is what makes the
        // shape read as a palette instead of a face.
        val dot = radius * 0.20f
        listOf(
            Offset(w * 0.36f, h * 0.40f),
            Offset(w * 0.64f, h * 0.40f),
            Offset(w * 0.50f, h * 0.63f),
        ).forEach { at -> drawCircle(color = color, radius = dot, center = at) }
    }
}

/** A lidded box: the backup and data rows. */
@Composable
fun ArchiveIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = LocalContentColor.current,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.082f

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.14f, h * 0.18f),
            size = Size(w * 0.72f, h * 0.19f),
            cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
            style = Stroke(width = stroke),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.19f, h * 0.37f),
            size = Size(w * 0.62f, h * 0.47f),
            cornerRadius = CornerRadius(w * 0.07f, w * 0.07f),
            style = Stroke(width = stroke),
        )
        // The handle notch that says "this opens".
        drawLine(
            color = color,
            start = Offset(w * 0.41f, h * 0.53f),
            end = Offset(w * 0.59f, h * 0.53f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

/** A globe: the language row. */
@Composable
fun GlobeIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = LocalContentColor.current,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.082f
        val center = Offset(w / 2f, h / 2f)
        val radius = w * 0.37f

        drawCircle(color = color, radius = radius, center = center, style = Stroke(width = stroke))
        // A meridian and the equator: enough to read as a sphere without drawing a map.
        drawOval(
            color = color,
            topLeft = Offset(w * 0.35f, h * 0.13f),
            size = Size(w * 0.30f, h * 0.74f),
            style = Stroke(width = stroke * 0.8f),
        )
        drawLine(
            color = color,
            start = Offset(w * 0.13f, h * 0.5f),
            end = Offset(w * 0.87f, h * 0.5f),
            strokeWidth = stroke * 0.8f,
            cap = StrokeCap.Round,
        )
    }
}

/** A document with ruled lines: the changelog row. */
@Composable
fun DocIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = LocalContentColor.current,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.082f

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.18f, h * 0.12f),
            size = Size(w * 0.64f, h * 0.76f),
            cornerRadius = CornerRadius(w * 0.09f, w * 0.09f),
            style = Stroke(width = stroke),
        )
        listOf(0.38f, 0.53f, 0.68f).forEachIndexed { index, fraction ->
            drawLine(
                color = color,
                start = Offset(w * 0.32f, h * fraction),
                end = Offset(w * (if (index == 2) 0.58f else 0.68f), h * fraction),
                strokeWidth = stroke * 0.9f,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** A circled i: the about row. */
@Composable
fun InfoIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color = LocalContentColor.current,
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.082f

        drawCircle(
            color = color,
            radius = w * 0.38f,
            center = Offset(w / 2f, h / 2f),
            style = Stroke(width = stroke),
        )
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.44f),
            end = Offset(w * 0.5f, h * 0.68f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawCircle(color = color, radius = stroke * 0.62f, center = Offset(w * 0.5f, h * 0.32f))
    }
}
