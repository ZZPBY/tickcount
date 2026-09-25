package com.tickcount.app.ui.countdown

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tickcount.app.R
import com.tickcount.app.domain.Countdown
import com.tickcount.app.domain.CountdownPhase
import com.tickcount.app.domain.formatDayProgress
import com.tickcount.app.ui.components.rememberDateFormatter
import com.tickcount.app.ui.theme.ClockTextStyle
import com.tickcount.app.ui.theme.DayCountTextStyle
import java.time.LocalDate

/**
 * The top half of the screen: which date, how many days, and a live clock.
 *
 * The two numbers answer two different questions and are labelled accordingly —
 * see [Countdown] for why they are not a single duration.
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
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))

        TitlePill(
            title = title,
            accent = accent,
            hasEvent = hasEvent,
            onClick = onTitleClick,
        )

        Spacer(Modifier.height(18.dp))

        DayCount(countdown = countdown, accent = accent)

        Spacer(Modifier.height(18.dp))

        Text(
            text = date.format(fullDateFormatter),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(22.dp))

        HorizontalDivider(
            modifier = Modifier.width(64.dp),
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        Spacer(Modifier.height(22.dp))

        LiveClock(countdown = countdown)

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
private fun DayCount(countdown: Countdown, accent: Color) {
    when (countdown.phase) {
        CountdownPhase.TODAY -> {
            Text(
                text = stringResource(R.string.label_today),
                style = DayCountTextStyle.copy(fontSize = 64.sp, letterSpacing = 0.sp),
                color = accent,
                textAlign = TextAlign.Center,
            )
        }

        else -> {
            Text(
                text = countdown.days.toString(),
                style = DayCountTextStyle,
                color = accent,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = stringResource(
                    if (countdown.phase == CountdownPhase.FUTURE) R.string.label_days_away
                    else R.string.label_days_ago
                ),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LiveClock(countdown: Countdown) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(
                if (countdown.isCountingDown) R.string.label_left_today
                else R.string.label_elapsed_today
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatDayProgress(countdown.dayProgressMillis),
            style = ClockTextStyle,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }
}
