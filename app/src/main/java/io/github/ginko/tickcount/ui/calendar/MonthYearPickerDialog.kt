package io.github.ginko.tickcount.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.ginko.tickcount.R
import io.github.ginko.tickcount.ui.components.ChevronIcon
import io.github.ginko.tickcount.ui.components.currentLocale
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle

private const val YEAR_STEP_NEAR = 1
private const val YEAR_STEP_FAR = 10

/**
 * A compact year-and-month chooser, opened by tapping the month title in the
 * calendar header.
 *
 * Stepping the month grid one month at a time is fine for "next month" but
 * useless for "the year after next", so this dialog pairs single-year arrows
 * with ten-year arrows and shows all twelve months at once.
 */
@Composable
fun MonthYearPickerDialog(
    initial: YearMonth,
    onSelect: (YearMonth) -> Unit,
    onDismiss: () -> Unit,
) {
    var year by remember(initial) { mutableStateOf(initial.year) }
    val locale = currentLocale()
    val monthLabels = remember(locale) {
        Month.entries.map { it.getDisplayName(TextStyle.SHORT, locale) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_pick_month_title)) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                YearStepper(year = year, onYearChange = { year = it })

                Spacer(Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    monthLabels.chunked(3).forEachIndexed { rowIndex, row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            row.forEachIndexed { columnIndex, label ->
                                val monthValue = rowIndex * 3 + columnIndex + 1
                                MonthChip(
                                    label = label,
                                    selected = year == initial.year && monthValue == initial.monthValue,
                                    onClick = { onSelect(YearMonth.of(year, monthValue)) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(YearMonth.now()) }) {
                Text(stringResource(R.string.action_this_month))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun YearStepper(year: Int, onYearChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperButton(
            pointsLeft = true,
            double = true,
            contentDescription = stringResource(R.string.action_year_back_ten),
            onClick = { onYearChange(year - YEAR_STEP_FAR) },
        )
        StepperButton(
            pointsLeft = true,
            double = false,
            contentDescription = stringResource(R.string.action_year_back),
            onClick = { onYearChange(year - YEAR_STEP_NEAR) },
        )

        Text(
            text = year.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(84.dp),
        )

        StepperButton(
            pointsLeft = false,
            double = false,
            contentDescription = stringResource(R.string.action_year_forward),
            onClick = { onYearChange(year + YEAR_STEP_NEAR) },
        )
        StepperButton(
            pointsLeft = false,
            double = true,
            contentDescription = stringResource(R.string.action_year_forward_ten),
            onClick = { onYearChange(year + YEAR_STEP_FAR) },
        )
    }
}

@Composable
private fun StepperButton(
    pointsLeft: Boolean,
    double: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(38.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        ChevronIcon(pointsLeft = pointsLeft, double = double, size = 20.dp)
    }
}

@Composable
private fun MonthChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    val background = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .height(40.dp)
            .clip(shape)
            .background(background, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
}
