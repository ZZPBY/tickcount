package io.github.zzpby.tickcount.ui.countdown

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.domain.Countdown
import io.github.zzpby.tickcount.domain.CountdownPhase
import io.github.zzpby.tickcount.domain.TimeParts
import io.github.zzpby.tickcount.domain.formatSlots
import io.github.zzpby.tickcount.ui.components.rememberDateFormatter
import io.github.zzpby.tickcount.ui.theme.CountdownLineTextStyle
import java.time.LocalDate

/**
 * The line is measured once at this size and then scaled to whatever width is
 * actually available — see [CountdownLine].
 */
private val LINE_REFERENCE_SIZE = 20.sp

/** Stops an extreme screen or accessibility scale producing absurd text. */
private val MIN_LINE_SIZE = 9.sp
private val MAX_LINE_SIZE = 28.sp

/**
 * The top half of the screen: the selected date and, if it has been named, a
 * single fixed-width line showing the distance to it as
 * `yyyy年MM月dd日 HH时mm分ss秒`.
 *
 * A slot whose value is zero is replaced by dashes the width of its pattern
 * letter, so a countdown under a year reads `----年...`. A day with no saved
 * countdown shows the same line with every slot blanked, which keeps the layout
 * from jumping while making it obvious that nothing is set.
 */
@Composable
fun CountdownHeader(
    title: String?,
    date: LocalDate,
    countdown: Countdown,
    accent: Color,
    onTitleClick: () -> Unit,
    onPrimaryAction: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val fullDateFormatter = rememberDateFormatter(R.string.date_format_full)
    val hasEvent = title != null

    Column(
        modifier = modifier.padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))

        TitlePill(
            title = title,
            accent = accent,
            hasEvent = hasEvent,
            onClick = onTitleClick,
        )

        Spacer(Modifier.height(28.dp))

        if (hasEvent) {
            PhaseLabel(countdown.phase, accent)
            Spacer(Modifier.height(12.dp))
            CountdownLine(countdown.parts)
        } else {
            CountdownLine(TimeParts.ZERO)
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = date.format(fullDateFormatter),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(26.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hasEvent) {
                TextButton(onClick = onPrimaryAction) {
                    Text(stringResource(R.string.action_edit))
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(
                            text = stringResource(R.string.action_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            } else {
                Button(onClick = onPrimaryAction) {
                    Text(stringResource(R.string.action_add))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TitlePill(
    title: String?,
    accent: Color,
    hasEvent: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = if (hasEvent) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.clip(CircleShape).clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(if (hasEvent) accent else MaterialTheme.colorScheme.outline, CircleShape)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = title ?: stringResource(R.string.label_no_countdown),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = if (hasEvent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PhaseLabel(phase: CountdownPhase, accent: Color) {
    Text(
        text = stringResource(
            when (phase) {
                CountdownPhase.FUTURE -> R.string.label_countdown_left
                CountdownPhase.TODAY -> R.string.label_today_elapsed
                CountdownPhase.PAST -> R.string.label_countdown_ago
            }
        ),
        style = MaterialTheme.typography.titleSmall,
        color = accent,
    )
}

/**
 * The countdown itself, on exactly one line.
 *
 * The font size is derived by *measuring* the rendered line and scaling it to
 * the width actually available, rather than by guessing an em-width. Text width
 * is linear in font size, so one measurement is exact — and because it uses the
 * real resolved font, the line still fits after a font fallback, a different
 * monospace face, or the user turning up the accessibility font scale.
 */
@Composable
private fun CountdownLine(parts: TimeParts) {
    val line = stringResource(
        R.string.countdown_line_format,
        *formatSlots(parts).toTypedArray(),
    )
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val availablePx = with(density) { maxWidth.toPx() }

        val fontSize = remember(line, availablePx) {
            val measuredWidth = measurer.measure(
                text = AnnotatedString(line),
                style = CountdownLineTextStyle,
            ).size.width

            if (measuredWidth <= 0) {
                LINE_REFERENCE_SIZE
            } else {
                // Clamp on the raw sp number: TextUnit is not Comparable, so
                // coerceIn is not available on it.
                (LINE_REFERENCE_SIZE.value * (availablePx / measuredWidth))
                    .coerceIn(MIN_LINE_SIZE.value, MAX_LINE_SIZE.value)
                    .sp
            }
        }

        Text(
            text = line,
            style = CountdownLineTextStyle.copy(fontSize = fontSize),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
