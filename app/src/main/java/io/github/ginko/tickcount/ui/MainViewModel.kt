package io.github.ginko.tickcount.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.ginko.tickcount.data.CountdownEvent
import io.github.ginko.tickcount.data.EventStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime

/**
 * Owns the whole screen state: the saved countdowns, the currently selected date,
 * the month being displayed, and the clock.
 *
 * The clock lives here rather than in the composition so that a rotation or a
 * recomposition never restarts or stutters it, and so there is exactly one timer
 * in the process.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val store = EventStore(application)

    private val _events = MutableStateFlow(store.load())
    val events: StateFlow<Map<LocalDate, CountdownEvent>> = _events.asStateFlow()

    private val _now = MutableStateFlow(ZonedDateTime.now())
    val now: StateFlow<ZonedDateTime> = _now.asStateFlow()

    private val _selectedDate = MutableStateFlow(initialSelection(_events.value))
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _visibleMonth = MutableStateFlow(YearMonth.from(_selectedDate.value))
    val visibleMonth: StateFlow<YearMonth> = _visibleMonth.asStateFlow()

    init {
        viewModelScope.launch {
            while (isActive) {
                _now.value = ZonedDateTime.now()
                // Land on the next whole second. The displayed value is always
                // recomputed from the wall clock, so a late tick can never make
                // the clock drift — it just redraws the correct time.
                delay(1_000L - System.currentTimeMillis() % 1_000L)
            }
        }
    }

    /** Selects [date] and scrolls the calendar to the month containing it. */
    fun select(date: LocalDate) {
        _selectedDate.value = date
        _visibleMonth.value = YearMonth.from(date)
    }

    fun showMonth(month: YearMonth) {
        _visibleMonth.value = month
    }

    fun stepMonth(delta: Long) {
        _visibleMonth.value = _visibleMonth.value.plusMonths(delta)
    }

    fun goToToday() {
        select(LocalDate.now())
    }

    /**
     * Creates or renames the countdown on [date]. An all-blank title removes the
     * countdown, which makes the dialog's "clear the text" gesture do the obvious
     * thing.
     */
    fun saveEvent(date: LocalDate, title: String) {
        val trimmed = title.trim().take(CountdownEvent.MAX_TITLE_LENGTH)
        if (trimmed.isEmpty()) {
            removeEvent(date)
            return
        }
        val existing = _events.value[date]
        val updated = CountdownEvent(
            date = date,
            title = trimmed,
            colorIndex = existing?.colorIndex ?: nextColorIndex(),
        )
        persist(_events.value + (date to updated))
    }

    fun removeEvent(date: LocalDate) {
        if (date !in _events.value) return
        persist(_events.value - date)
    }

    private fun persist(events: Map<LocalDate, CountdownEvent>) {
        _events.value = events
        store.save(events)
    }

    /** Spreads new countdowns across the accent palette instead of reusing one colour. */
    private fun nextColorIndex(): Int = _events.value.size % CountdownEvent.COLOR_COUNT

    private companion object {
        /**
         * Opens on whatever the user most likely cares about: the next countdown
         * that has not happened yet, otherwise the most recent one that has.
         */
        fun initialSelection(events: Map<LocalDate, CountdownEvent>): LocalDate {
            val today = LocalDate.now()
            val upcoming = events.keys.filter { !it.isBefore(today) }.minOrNull()
            val recent = events.keys.filter { it.isBefore(today) }.maxOrNull()
            return upcoming ?: recent ?: today
        }
    }
}
