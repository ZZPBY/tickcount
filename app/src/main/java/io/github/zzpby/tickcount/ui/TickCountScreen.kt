package io.github.zzpby.tickcount.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.zzpby.tickcount.domain.countdownTo
import io.github.zzpby.tickcount.ui.calendar.MonthCalendar
import io.github.zzpby.tickcount.ui.calendar.MonthYearPickerDialog
import io.github.zzpby.tickcount.ui.countdown.CountdownHeader
import io.github.zzpby.tickcount.ui.theme.eventAccent
import java.time.LocalDate

private const val NO_DATE = Long.MIN_VALUE

/**
 * The one screen: a countdown for the selected date on top, the month grid below.
 *
 * Selecting a day in the calendar is the only navigation there is — the header
 * always describes whatever day is selected, whether or not it has been named.
 */
@Composable
fun TickCountScreen(viewModel: MainViewModel = viewModel()) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val visibleMonth by viewModel.visibleMonth.collectAsStateWithLifecycle()

    // Which date the editor dialog is open for, or NO_DATE.
    // `mutableStateOf` rather than `mutableLongStateOf`: only the boxed
    // MutableState has a saveable overload.
    var editorEpochDay by rememberSaveable { mutableStateOf(NO_DATE) }
    var monthPickerOpen by rememberSaveable { mutableStateOf(false) }

    val today = now.toLocalDate()
    val countdown = remember(now, selectedDate) { countdownTo(selectedDate, now) }
    val selectedEvent = events[selectedDate]
    val accent = remember(selectedEvent) {
        selectedEvent?.let { eventAccent(it.colorIndex) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        CountdownHeader(
            title = selectedEvent?.title,
            date = selectedDate,
            countdown = countdown,
            accent = accent ?: MaterialTheme.colorScheme.primary,
            onTitleClick = { editorEpochDay = selectedDate.toEpochDay() },
            onPrimaryAction = { editorEpochDay = selectedDate.toEpochDay() },
            onDelete = selectedEvent?.let {
                { viewModel.removeEvent(selectedDate) }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        MonthCalendar(
            month = visibleMonth,
            selectedDate = selectedDate,
            today = today,
            events = events,
            onSelect = viewModel::select,
            onStepMonth = viewModel::stepMonth,
            onMonthClick = { monthPickerOpen = true },
            onToday = viewModel::goToToday,
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        Spacer(Modifier.height(20.dp))
    }

    if (monthPickerOpen) {
        MonthYearPickerDialog(
            initial = visibleMonth,
            onSelect = { picked ->
                viewModel.showMonth(picked)
                monthPickerOpen = false
            },
            onDismiss = { monthPickerOpen = false },
        )
    }

    if (editorEpochDay != NO_DATE) {
        val target = LocalDate.ofEpochDay(editorEpochDay)
        EventEditorDialog(
            date = target,
            initialTitle = events[target]?.title.orEmpty(),
            onSave = { title ->
                viewModel.saveEvent(target, title)
                editorEpochDay = NO_DATE
            },
            onDelete = {
                viewModel.removeEvent(target)
                editorEpochDay = NO_DATE
            },
            onDismiss = { editorEpochDay = NO_DATE },
        )
    }
}
