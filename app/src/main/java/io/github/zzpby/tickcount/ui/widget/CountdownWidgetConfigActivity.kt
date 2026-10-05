package io.github.zzpby.tickcount.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.data.CountdownEvent
import io.github.zzpby.tickcount.data.EventStore
import io.github.zzpby.tickcount.ui.components.rememberDateFormatter
import io.github.zzpby.tickcount.ui.theme.TickCountTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * The screen the launcher shows while a widget is being placed: pick which
 * countdown that instance should display.
 *
 * The result is set to [ComponentActivity.RESULT_CANCELED] before anything else,
 * so backing out leaves no widget behind — which is the behaviour the widget
 * framework expects.
 */
class CountdownWidgetConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent?.extras
            ?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setResult(
            RESULT_CANCELED,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
        )
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Soonest first, and same-day countdowns by their time, because that is the
        // order someone picking one is thinking in.
        val events = EventStore(this).load()
            .sortedWith(compareBy({ it.epochDay }, { it.time }, { it.title }))

        setContent {
            TickCountTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    WidgetConfigScreen(
                        events = events,
                        onPick = { eventId ->
                            CountdownWidgets.saveEventId(this, appWidgetId, eventId)
                            CountdownWidgets.update(this, appWidgetId)
                            setResult(
                                RESULT_OK,
                                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId),
                            )
                            finish()
                        },
                        onCancel = { finish() },
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetConfigScreen(
    events: List<CountdownEvent>,
    onPick: (String) -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text(
            text = stringResource(R.string.widget_config_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(16.dp))

        if (events.isEmpty()) {
            Text(
                text = stringResource(R.string.widget_config_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val formatter = rememberDateFormatter(R.string.date_format_full)
                val timeFormatter = rememberDateFormatter(R.string.time_format)
                events.forEach { event ->
                    WidgetConfigRow(
                        event = event,
                        dateLine = dateLine(event, formatter, timeFormatter),
                        onClick = { onPick(event.id) },
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    }
}

/**
 * The date a countdown is on, with its time after it when it names one.
 *
 * The list is what someone picks from, so a countdown set to 09:30 has to look different
 * from an all-day one here — otherwise the only way to tell them apart is to pick one and
 * read the widget.
 */
@Composable
private fun dateLine(
    event: CountdownEvent,
    formatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter,
): String {
    val date = event.date.format(formatter)
    val time = event.time ?: return date
    return stringResource(R.string.text_with_time, date, time.format(timeFormatter))
}

@Composable
private fun WidgetConfigRow(
    event: CountdownEvent,
    dateLine: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = dateLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
