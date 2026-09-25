package io.github.zzpby.tickcount.ui.countdown

import androidx.annotation.PluralsRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.domain.Countdown
import io.github.zzpby.tickcount.domain.CountdownPhase
import io.github.zzpby.tickcount.domain.TimeParts
import io.github.zzpby.tickcount.domain.TimeUnit
import io.github.zzpby.tickcount.ui.components.rememberDateFormatter
import io.github.zzpby.tickcount.ui.theme.CountNumberTextStyle
import io.github.zzpby.tickcount.ui.theme.EmptyValueTextStyle
import java.time.LocalDate

private val CELL_WIDTH = 78.dp

/**
 * The top half of the screen: the selected date and, if it has been named, the
 * distance to it broken into years, months, days, hours, minutes and seconds.
 *
 * A day with no saved countdown deliberately shows `--` instead of a number: the
 * countdown is something the user creates, so inventing one for every day they
 * browse past would be noise.
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
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))

        TitlePill(
            title = title,
            accent = accent,
            hasEvent = hasEvent,
            onClick = onTitleClick,
        )

        Spacer(Modifier.height(30.dp))

        if (hasEvent) {
            PhaseLabel(countdown.phase, accent)
            Spacer(Modifier.height(14.dp))
            Breakdown(parts = countdown.parts)
        } else {
            Text(
                text = stringResource(R.string.value_empty),
                style = EmptyValueTextStyle,
                color = MaterialTheme.colorScheme.outline,
            )
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
 * Lays the components out in rows of three, centred, so the grid stays balanced
 * whether the countdown spans six units or only two.
 */
@Composable
private fun Breakdown(parts: TimeParts) {
    val units = parts.significantUnits()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        units.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { unit ->
                    TimeCell(value = parts.value(unit), unit = unit)
                }
            }
        }
    }
}

@Composable
private fun TimeCell(value: Long, unit: TimeUnit) {
    Column(
        modifier = Modifier.width(CELL_WIDTH),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value.toString(),
            style = CountNumberTextStyle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = pluralStringResource(unit.labelRes(), value.toInt()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@PluralsRes
private fun TimeUnit.labelRes(): Int = when (this) {
    TimeUnit.YEARS -> R.plurals.unit_years
    TimeUnit.MONTHS -> R.plurals.unit_months
    TimeUnit.DAYS -> R.plurals.unit_days
    TimeUnit.HOURS -> R.plurals.unit_hours
    TimeUnit.MINUTES -> R.plurals.unit_minutes
    TimeUnit.SECONDS -> R.plurals.unit_seconds
}
