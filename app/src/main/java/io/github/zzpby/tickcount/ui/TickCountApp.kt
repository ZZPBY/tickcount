package io.github.zzpby.tickcount.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.data.AppLanguage
import io.github.zzpby.tickcount.domain.SearchScope
import io.github.zzpby.tickcount.domain.SortOrder
import io.github.zzpby.tickcount.domain.countdownList
import io.github.zzpby.tickcount.ui.calendar.MonthYearPickerDialog
import io.github.zzpby.tickcount.ui.components.PlusIcon
import io.github.zzpby.tickcount.ui.components.SortIcon
import java.time.LocalDate

/** Sentinel for "no day chosen", which is not a valid epoch day. */
private const val NO_DAY = Long.MIN_VALUE

/**
 * The whole app: one screen at a time, a drawer that slides in over them, and the
 * dialogs that belong to whichever screen raised them.
 *
 * Navigation is a single piece of state rather than a navigation library. There is
 * exactly one level of it — a screen, plus optionally one countdown opened from a
 * list — so a back stack would be more machinery than the thing it models.
 */
@Composable
fun TickCountApp(viewModel: MainViewModel = viewModel()) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val now by viewModel.now.collectAsStateWithLifecycle()
    val visibleMonth by viewModel.visibleMonth.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var screenName by rememberSaveable { mutableStateOf(AppScreen.HOME.name) }
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    var monthPickerOpen by rememberSaveable { mutableStateOf(false) }
    var calendarEpochDay by rememberSaveable { mutableLongStateOf(NO_DAY) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var languageOpen by rememberSaveable { mutableStateOf(false) }

    // Held here rather than in the screens so that opening a countdown and coming
    // back leaves the search as it was. The calendar keeps its own, because it is
    // searching a different list.
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searchScopeName by rememberSaveable { mutableStateOf(SearchScope.NAME.name) }
    var calendarQuery by rememberSaveable { mutableStateOf("") }
    var calendarScopeName by rememberSaveable { mutableStateOf(SearchScope.NAME.name) }

    val searchScope = remember(searchScopeName) { searchScopeName.toScope() }
    val calendarScope = remember(calendarScopeName) { calendarScopeName.toScope() }

    // The editor is described by what it was opened for, not by a copy of the
    // countdown: an id for an existing one, or a day for a new one.
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var editorIsNew by rememberSaveable { mutableStateOf(false) }
    var editorId by rememberSaveable { mutableStateOf("") }
    var editorNewOn by rememberSaveable { mutableLongStateOf(0L) }

    val screen = remember(screenName) {
        runCatching { AppScreen.valueOf(screenName) }.getOrDefault(AppScreen.HOME)
    }
    val today = now.toLocalDate()
    val byDate = remember(events) { events.groupBy { it.date } }
    val detailEvent = viewModel.event(detailId)
    val calendarDate = calendarEpochDay.takeIf { it != NO_DAY }?.let(LocalDate::ofEpochDay)

    // A new countdown's id is random, so it has to be minted once per editing
    // session rather than on every recomposition.
    val editorEvent = remember(editorOpen, editorIsNew, editorId, editorNewOn) {
        when {
            !editorOpen -> null
            editorIsNew -> viewModel.draftEvent(LocalDate.ofEpochDay(editorNewOn))
            else -> viewModel.event(editorId)
        }
    }

    fun startNewEvent(date: LocalDate) {
        editorIsNew = true
        editorNewOn = date.toEpochDay()
        editorId = ""
        editorOpen = true
    }

    fun startEditEvent(id: String) {
        editorIsNew = false
        editorId = id
        editorOpen = true
    }

    // Enabled while there is anywhere to go back to; disabled lets the system have
    // the press, which is what closes the app from the home screen. Each press goes
    // up exactly one level, matching the arrow the title bar shows.
    BackHandler(enabled = drawerOpen || editorOpen || detailId != null || screen != AppScreen.HOME) {
        when {
            drawerOpen -> drawerOpen = false
            editorOpen -> editorOpen = false
            detailId != null -> detailId = null
            else -> screenName = AppScreen.HOME.name
        }
    }

    val uriHandler = LocalUriHandler.current
    val linkUrl = stringResource(R.string.about_link_url)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                AppTitleBar(
                    title = when {
                        detailEvent != null -> detailEvent.title
                        screen == AppScreen.HOME -> stringResource(R.string.app_name)
                        else -> stringResource(screen.labelRes)
                    },
                    // Every screen but a countdown's own is a drawer destination, so
                    // the button opens the drawer; that one goes back to its list.
                    navigation = if (detailEvent != null) {
                        TitleBarNavigation.BACK
                    } else {
                        TitleBarNavigation.MENU
                    },
                    onNavigate = {
                        if (detailEvent != null) detailId = null else drawerOpen = true
                    },
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    when {
                        detailEvent != null -> TextButton(onClick = { startEditEvent(detailEvent.id) }) {
                            Text(stringResource(R.string.action_edit))
                        }

                        screen == AppScreen.HOME -> {
                            SortMenu(settings.sortOrder, viewModel::chooseSortOrder)
                            IconButton(onClick = { startNewEvent(today) }) { PlusIcon() }
                        }

                        // On the calendar the plus belongs to the chosen day, and
                        // there is nothing to add to until one is chosen.
                        screen == AppScreen.CALENDAR -> {
                            SortMenu(settings.sortOrder, viewModel::chooseSortOrder)
                            if (calendarDate != null) {
                                IconButton(onClick = { startNewEvent(calendarDate) }) { PlusIcon() }
                            }
                        }
                    }
                }
            },
        ) { insets ->
            Box(modifier = Modifier.padding(insets)) {
                when {
                    detailEvent != null -> EventDetailScreen(
                        event = detailEvent,
                        now = now,
                        onTogglePin = { viewModel.togglePin(detailEvent.id) },
                    )

                    screen == AppScreen.HOME -> HomeScreen(
                        entries = countdownList(events, today, settings.sortOrder),
                        today = today,
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        scope = searchScope,
                        onScopeChange = { searchScopeName = it.name },
                        onOpen = { detailId = it },
                    )

                    screen == AppScreen.CALENDAR -> CalendarScreen(
                        month = visibleMonth,
                        today = today,
                        selectedDate = calendarDate,
                        eventsByDate = byDate,
                        query = calendarQuery,
                        onQueryChange = { calendarQuery = it },
                        scope = calendarScope,
                        onScopeChange = { calendarScopeName = it.name },
                        sortOrder = settings.sortOrder,
                        onSelectDate = { calendarEpochDay = it.toEpochDay() },
                        onStepMonth = viewModel::stepMonth,
                        onMonthClick = { monthPickerOpen = true },
                        onToday = viewModel::goToToday,
                        onOpenEvent = { detailId = it },
                    )

                    screen == AppScreen.APPEARANCE -> AppearanceScreen(
                        settings = settings,
                        onPreset = viewModel::chooseTheme,
                        onCustomHue = viewModel::chooseCustomHue,
                    )

                    screen == AppScreen.DATA -> DataScreen(
                        onExport = viewModel::exportBackup,
                        onImport = viewModel::importBackup,
                    )

                    screen == AppScreen.CHANGELOG -> ChangelogScreen()

                    else -> ProjectIntroScreen(
                        // The link is the one thing here that leaves the app, and it
                        // is handed to the browser: TickCount has no INTERNET
                        // permission of its own.
                        onOpenLink = { runCatching { uriHandler.openUri(linkUrl) } },
                    )
                }
            }
        }

        // The drawer, over the screen rather than beside it. Fading the scrim and
        // sliding the panel are separate animations so the two can differ.
        AnimatedVisibility(visible = drawerOpen, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { drawerOpen = false },
                    ),
            )
        }

        AnimatedVisibility(
            visible = drawerOpen,
            enter = slideInHorizontally { -it },
            exit = slideOutHorizontally { -it },
        ) {
            AppDrawerContent(
                // Nothing is highlighted while a countdown's own screen is open,
                // because that screen belongs to no drawer entry.
                current = if (detailEvent == null) screen else null,
                language = settings.language,
                onSelect = { picked ->
                    screenName = picked.name
                    detailId = null
                    drawerOpen = false
                },
                onLanguage = {
                    drawerOpen = false
                    languageOpen = true
                },
                modifier = Modifier.fillMaxWidth(0.5f),
            )
        }
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

    if (languageOpen) {
        LanguageDialog(
            current = settings.language,
            onPick = {
                languageOpen = false
                // Changing this rebuilds the activity, so nothing after it in this
                // composition is guaranteed to run against the old resources.
                viewModel.chooseLanguage(it)
            },
            onDismiss = { languageOpen = false },
        )
    }

    if (editorEvent != null) {
        EventEditorDialog(
            initial = editorEvent,
            isNew = editorIsNew,
            onSave = { edited ->
                viewModel.saveEvent(edited)
                editorOpen = false
            },
            // The editor closes first: the confirmation is the next question, and
            // stacking two dialogs would bury the name being confirmed.
            onDelete = editorEvent.takeIf { !editorIsNew }?.let {
                {
                    editorOpen = false
                    pendingDeleteId = it.id
                }
            },
            onDismiss = { editorOpen = false },
        )
    }

    // Only rendered while the countdown still exists, so one that has already gone
    // — deleted from another path, say — cannot raise a dialog about itself.
    val pendingDeleteName = viewModel.event(pendingDeleteId)?.title
    if (pendingDeleteId != null && pendingDeleteName != null) {
        DeleteConfirmDialog(
            name = pendingDeleteName,
            onConfirm = {
                val removed = pendingDeleteId
                viewModel.removeEvent(removed!!)
                if (detailId == removed) detailId = null
                pendingDeleteId = null
            },
            onDismiss = { pendingDeleteId = null },
        )
    }
}

private fun String.toScope(): SearchScope =
    runCatching { SearchScope.valueOf(this) }.getOrDefault(SearchScope.NAME)

/** The name each ordering is offered under. */
private val SortOrder.labelRes: Int
    get() = when (this) {
        SortOrder.DATE_ASC -> R.string.sort_date_asc
        SortOrder.DATE_DESC -> R.string.sort_date_desc
        SortOrder.ADDED -> R.string.sort_added
    }

/**
 * The button that chooses how a list is ordered.
 *
 * Sits beside the plus rather than inside the list, because the order is a property
 * of the screen rather than of any one countdown in it. Both lists that can be
 * ordered share it.
 */
@Composable
private fun SortMenu(current: SortOrder, onPick: (SortOrder) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val description = stringResource(R.string.action_sort)

    Box {
        IconButton(
            onClick = { open = true },
            modifier = Modifier.semantics { contentDescription = description },
        ) {
            SortIcon()
        }

        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SortOrder.entries.forEach { option ->
                val selected = option == current
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(option.labelRes),
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onPick(option)
                        open = false
                    },
                    trailingIcon = {
                        // A tick rather than a highlight, so the current choice is
                        // legible without relying on a colour.
                        if (selected) {
                            Text(
                                text = "✓",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    },
                )
            }
        }
    }
}

/**
 * Which language the interface is in.
 *
 * Each option is named in its own language rather than in the current one, which is the
 * only way the choice makes sense to someone who cannot read the language they are
 * currently stuck in.
 */
@Composable
private fun LanguageDialog(
    current: AppLanguage,
    onPick: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.drawer_language)) },
        text = {
            Column {
                AppLanguage.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(option) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = option == current,
                            onClick = { onPick(option) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(option.labelRes),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
