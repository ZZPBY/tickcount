package io.github.zzpby.tickcount.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.zzpby.tickcount.data.AppLanguage
import io.github.zzpby.tickcount.data.AppSettings
import io.github.zzpby.tickcount.data.Backup
import io.github.zzpby.tickcount.data.BackupCodec
import io.github.zzpby.tickcount.data.BackupCrypto
import io.github.zzpby.tickcount.data.CountdownEvent
import io.github.zzpby.tickcount.data.EventStore
import io.github.zzpby.tickcount.data.ImportMode
import io.github.zzpby.tickcount.data.ReadOutcome
import io.github.zzpby.tickcount.data.SettingsStore
import io.github.zzpby.tickcount.data.ThemePreset
import io.github.zzpby.tickcount.data.previewOf
import io.github.zzpby.tickcount.domain.SortOrder
import io.github.zzpby.tickcount.ui.widget.refreshAllWidgets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import javax.crypto.AEADBadTagException

/**
 * Owns the whole app state: the saved countdowns, the month the calendar is
 * showing, and the clock.
 *
 * The clock lives here rather than in a composition so that rotating the device
 * never restarts or stutters it, and so there is exactly one timer in the process.
 *
 * Countdowns are held as a list rather than a date-keyed map because a day may hold
 * more than one, which is also why every lookup is by id.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val store = EventStore(application)

    /**
     * Read before anything else, because the theme is applied above this view model
     * and has to be right on the first frame rather than one frame later.
     */
    private val settingsStore = SettingsStore(application)

    private val _events = MutableStateFlow(store.load())
    val events: StateFlow<List<CountdownEvent>> = _events.asStateFlow()

    private val _settings = MutableStateFlow(settingsStore.load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    /**
     * The clock every screen reads.
     *
     * Ticking while somebody is watching and stopping when nobody is, rather than for the
     * life of the process: an app left in the background has no one looking at a countdown,
     * and a coroutine waking every second to say so is work for nothing. The five seconds of
     * grace are for the gaps a configuration change makes.
     */
    val now: StateFlow<ZonedDateTime> = flow {
        while (true) {
            emit(ZonedDateTime.now())
            // Land on the next whole second. The displayed value is always recomputed from
            // the wall clock, so a late tick can never make the clock drift — it just
            // redraws the correct time.
            delay(1_000L - System.currentTimeMillis() % 1_000L)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = ZonedDateTime.now(),
    )

    private val _visibleMonth = MutableStateFlow(YearMonth.now())
    val visibleMonth: StateFlow<YearMonth> = _visibleMonth.asStateFlow()

    init {
        // Placed widgets are redrawn on every launch. It keeps them in step after the
        // app has been away, and it doubles as the way out of a launcher that stopped
        // telling the widget its size: opening the app pushes the current arrangement
        // back out, without the user having to delete the widget and add it again.
        //
        // Off the main thread because it reads the store and talks to the widget
        // service once per placed widget, neither of which belongs in a constructor.
        viewModelScope.launch(Dispatchers.IO) {
            refreshAllWidgets(application)
        }
    }

    // ------------------------------------------------------------- countdowns

    fun event(id: String?): CountdownEvent? =
        id?.let { wanted -> _events.value.firstOrNull { it.id == wanted } }

    /** The countdowns on [date], earliest time first, which is the order a day reads in. */
    fun eventsOn(date: LocalDate): List<CountdownEvent> =
        _events.value
            .filter { it.date == date }
            .sortedWith(compareBy({ it.time }, { it.title }))

    fun eventsByDate(): Map<LocalDate, List<CountdownEvent>> = _events.value.groupBy { it.date }

    /** Adds [event], or replaces the stored one carrying the same id. */
    fun saveEvent(event: CountdownEvent) {
        val index = _events.value.indexOfFirst { it.id == event.id }
        val updated = if (index >= 0) {
            _events.value.toMutableList().also { it[index] = event }
        } else {
            _events.value + event
        }
        persist(updated)
    }

    fun removeEvent(id: String) {
        if (_events.value.none { it.id == id }) return
        persist(_events.value.filterNot { it.id == id })
    }

    /**
     * Pins or unpins a countdown, which is what keeps it at the top of any list it
     * appears in.
     */
    fun togglePin(id: String) {
        val event = _events.value.firstOrNull { it.id == id } ?: return
        saveEvent(event.copy(pinned = !event.pinned))
    }

    /**
     * A blank countdown on [date], ready for the editor.
     *
     * The id is minted here rather than on save so that the editor, the delete
     * confirmation and the widget all refer to the same thing from the start. The
     * colour is left unset: the default is the theme's own, and nothing is assigned
     * on the user's behalf. The added-at stamp is taken now, because that is the
     * moment it is being added.
     */
    fun draftEvent(date: LocalDate): CountdownEvent = CountdownEvent(
        id = CountdownEvent.newId(),
        date = date,
        title = "",
        createdAt = System.currentTimeMillis(),
    )

    private fun persist(events: List<CountdownEvent>) {
        _events.value = events
        store.save(events)
        // Any placed widget is showing one of these countdowns and has no way of
        // knowing one was renamed, re-timed, moved, or removed.
        refreshAllWidgets(getApplication())
    }

    // -------------------------------------------------------------- backup

    /**
     * The whole app as one file, encrypted only when [password] is given.
     *
     * Encryption is the caller's choice rather than the default: a forgotten password
     * cannot be recovered from anywhere, because there is no server and no account to
     * recover it from.
     */
    fun exportBackup(password: String?): String {
        val document = BackupCodec.encode(Backup(_events.value, _settings.value))
        return if (password.isNullOrEmpty()) {
            document
        } else {
            BackupCrypto.encrypt(document, password.toCharArray())
        }
    }

    /**
     * Opens a backup file without putting anything back.
     *
     * Kept apart from applying it so that the file, and any password on it, are settled
     * before the screen asks whether to replace or merge. Asking first and reporting a
     * wrong password afterwards throws away an answer the user has already given.
     */
    fun readBackup(text: String, password: String?): ReadOutcome {
        val document = try {
            if (BackupCrypto.isEncrypted(text)) {
                val given = password ?: return ReadOutcome.WrongPassword
                BackupCrypto.decrypt(text, given.toCharArray())
            } else {
                text
            }
        } catch (e: AEADBadTagException) {
            // GCM authenticates as it decrypts, so a wrong password fails the tag rather
            // than yielding plausible nonsense.
            return ReadOutcome.WrongPassword
        } catch (e: Exception) {
            return ReadOutcome.Unreadable
        }

        val restored = try {
            BackupCodec.decode(document)
        } catch (e: Exception) {
            return ReadOutcome.NotABackup
        }

        return ReadOutcome.Read(restored, previewOf(restored, _events.value))
    }

    /** Puts a backup that [readBackup] accepted back, replacing or merging by [mode]. */
    fun applyBackup(backup: Backup, mode: ImportMode): Int {
        val events = when (mode) {
            ImportMode.REPLACE -> backup.events
            // Same id means the same countdown, so the file wins; anything else is
            // added alongside what is already here.
            ImportMode.MERGE -> {
                val byId = _events.value.associateBy { it.id }.toMutableMap()
                backup.events.forEach { byId[it.id] = it }
                byId.values.toList()
            }
        }

        persist(events)
        _settings.value = backup.settings
        settingsStore.save(backup.settings)

        return events.size
    }

    // ------------------------------------------------------------------ theme

    fun chooseTheme(preset: ThemePreset) {
        updateSettings(_settings.value.copy(preset = preset))
    }

    /**
     * Stores a mixed hue. The preset is left alone, because the strip is only on
     * screen while [ThemePreset.CUSTOM] is already the choice.
     */
    fun chooseCustomHue(hue: Float) {
        updateSettings(_settings.value.copy(customHue = hue))
    }

    /**
     * The order the home list is drawn in. Kept beside the theme because it is the
     * same kind of thing: a preference that outlives the process.
     */
    fun chooseSortOrder(order: SortOrder) {
        updateSettings(_settings.value.copy(sortOrder = order))
    }

    /**
     * The language the interface is in. The activity watches this and rebuilds itself,
     * because the strings come from resources and those are fixed once it has started.
     */
    fun chooseLanguage(language: AppLanguage) {
        updateSettings(_settings.value.copy(language = language))
    }

    private fun updateSettings(next: AppSettings) {
        _settings.value = next
        settingsStore.save(next)
    }

    // ------------------------------------------------------------------ month

    fun showMonth(month: YearMonth) {
        _visibleMonth.value = month
    }

    fun stepMonth(delta: Long) {
        _visibleMonth.value = _visibleMonth.value.plusMonths(delta)
    }

    fun goToToday() {
        _visibleMonth.value = YearMonth.now()
    }
}
