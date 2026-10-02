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
import io.github.zzpby.tickcount.domain.countdownList
import io.github.zzpby.tickcount.domain.countdownTo
import io.github.zzpby.tickcount.ui.calendar.MonthCalendar
import io.github.zzpby.tickcount.ui.calendar.MonthYearPickerDialog
import io.github.zzpby.tickcount.ui.countdown.CountdownHeader
import io.github.zzpby.tickcount.ui.countdown.CountdownListBar
import io.github.zzpby.tickcount.ui.theme.eventAccent
import java.time.LocalDate

private const val NO_DATE = Long.MIN_VALUE

/**
 * The one screen: the app's name over a countdown for the selected date, over the
 * month grid.
 *
 * Selecting a day in the calendar is the only navigation there is — the header
 * always describes whatever day is selected, whether or not it has been named.
 * The list bar is the same navigation by name instead of by position, for when
 * the day is easier to remember than the date.
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
    var listExpanded by rememberSaveable { mutableStateOf(false) }
    var aboutOpen by rememberSaveable { mutableStateOf(false) }
    var changelogOpen by rememberSaveable { mutableStateOf(false) }

    // The date a delete *button* has asked about, or NO_DATE. Both delete buttons
    // funnel through this one value, which is what lets the confirmation be
    // written once instead of at each call site.
    var pendingDeleteEpochDay by rememberSaveable { mutableStateOf(NO_DATE) }

    val today = now.toLocalDate()
    val countdown = remember(now, selectedDate) { countdownTo(selectedDate, now) }
    val selectedEvent = events[selectedDate]
    val accent = remember(selectedEvent) {
        selectedEvent?.let { eventAccent(it.colorIndex) }
    }
    val listEntries = remember(events, today) { countdownList(events, today) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(8.dp))

        AppTitleBar(
            onChangelog = { changelogOpen = true },
            onAbout = { aboutOpen = true },
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        CountdownHeader(
            title = selectedEvent?.title,
            date = selectedDate,
            countdown = countdown,
            accent = accent ?: MaterialTheme.colorScheme.primary,
            onTitleClick = { editorEpochDay = selectedDate.toEpochDay() },
            onPrimaryAction = { editorEpochDay = selectedDate.toEpochDay() },
            onDelete = selectedEvent?.let {
                { pendingDeleteEpochDay = selectedDate.toEpochDay() }
            },
            modifier = Modifier.fillMaxWidth(),
            listBar = {
                CountdownListBar(
                    entries = listEntries,
                    selectedName = selectedEvent?.title,
                    expanded = listExpanded,
                    onToggle = { listExpanded = !listExpanded },
                    onDismiss = { listExpanded = false },
                    // Picking a row is a jump, so the list gets out of the way and
                    // leaves the calendar showing the day that was picked.
                    onSelect = { date ->
                        viewModel.select(date)
                        listExpanded = false
                    },
                )
            },
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

    if (changelogOpen) {
        ChangelogDialog(onDismiss = { changelogOpen = false })
    }

    if (aboutOpen) {
        AboutDialog(onDismiss = { aboutOpen = false })
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
            // The editor closes first: the confirmation is the next question, and
            // stacking two dialogs would bury the name being confirmed.
            onDelete = {
                editorEpochDay = NO_DATE
                pendingDeleteEpochDay = target.toEpochDay()
            },
            onDismiss = { editorEpochDay = NO_DATE },
        )
    }

    // Only rendered while the countdown still exists, so one that has already
    // gone — deleted from another path, say — cannot raise a dialog about itself.
    val pendingDeleteDate = pendingDeleteEpochDay
        .takeIf { it != NO_DATE }
        ?.let(LocalDate::ofEpochDay)
    val pendingDeleteName = pendingDeleteDate?.let { events[it]?.title }

    if (pendingDeleteDate != null && pendingDeleteName != null) {
        DeleteConfirmDialog(
            name = pendingDeleteName,
            onConfirm = {
                viewModel.removeEvent(pendingDeleteDate)
                pendingDeleteEpochDay = NO_DATE
            },
            onDismiss = { pendingDeleteEpochDay = NO_DATE },
        )
    }
}
