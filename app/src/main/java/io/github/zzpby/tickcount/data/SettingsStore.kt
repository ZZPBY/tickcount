package io.github.zzpby.tickcount.data

import android.content.Context
import androidx.core.content.edit
import io.github.zzpby.tickcount.domain.SortOrder

/**
 * The palettes the app offers, plus one the user mixes themselves.
 *
 * Black is the dark scheme rather than a colour of its own, so choosing it fixes the app's
 * lightness and stops it following the system setting. White is the plain one — a white page
 * and near-black ink. The other four are the same recipe with a hue: a comfortable saturated
 * accent, and a page tinted with that same hue so the whole screen changes together rather
 * than only the buttons. [CUSTOM] does the same with the hue the user mixed.
 */
enum class ThemePreset(val dark: Boolean) {
    WHITE(false),
    BLACK(true),
    TEAL(false),
    MIST(false),
    AMBER(false),
    ROSE(false),
    CUSTOM(false),
}

/**
 * Which language the app's own interface is in.
 *
 * [SYSTEM] leaves it to Android. The other two override it for this app only, by
 * rebuilding the activity's resources — no dependency on a per-app locale API that only
 * newer versions have.
 */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    CHINESE("zh"),
    ENGLISH("en"),
}

/**
 * Everything the user has chosen about how the app looks, and how the list reads.
 *
 * [customHue] is kept even while a preset is selected, so switching to
 * [ThemePreset.CUSTOM] returns the colour that was last mixed rather than resetting
 * it.
 */
data class AppSettings(
    val preset: ThemePreset = ThemePreset.TEAL,
    val customHue: Float = DEFAULT_HUE,
    val sortOrder: SortOrder = SortOrder.DATE_ASC,
    val language: AppLanguage = AppLanguage.SYSTEM,
    /** Whether a notification is posted on the day a countdown lands. */
    val remind: Boolean = false,
    /** Minutes after midnight at which the reminder is delivered. 540 is 09:00. */
    val remindMinutes: Int = 540,
    /** How many days before the countdown the reminder arrives: 0 is on the day. */
    val remindAdvanceDays: Int = 0,
)

/**
 * Where the hue strip starts before the user has moved it.
 *
 * The emerald the app's own icon and default theme are built on, so mixing a colour starts
 * from the app's own look rather than from an unrelated blue.
 */
const val DEFAULT_HUE = 174f

/**
 * Persists the appearance settings.
 *
 * Deliberately separate from [EventStore]: this is read on the way into the theme,
 * before any countdown has been loaded, so it must not depend on that document
 * being readable.
 */
class SettingsStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AppSettings {
        val preset = prefs.getString(KEY_THEME, null)
            ?.let { name -> ThemePreset.entries.firstOrNull { it.name == name } }
            ?: ThemePreset.TEAL
        val order = prefs.getString(KEY_SORT, null)
            ?.let { name -> SortOrder.entries.firstOrNull { it.name == name } }
            ?: SortOrder.DATE_ASC
        val language = prefs.getString(KEY_LANGUAGE, null)
            ?.let { name -> AppLanguage.entries.firstOrNull { it.name == name } }
            ?: AppLanguage.SYSTEM
        return AppSettings(
            preset = preset,
            customHue = prefs.getFloat(KEY_HUE, DEFAULT_HUE),
            sortOrder = order,
            language = language,
            remind = prefs.getBoolean(KEY_REMIND, false),
            remindMinutes = prefs.getInt(KEY_REMIND_TIME, 540),
            remindAdvanceDays = prefs.getInt(KEY_REMIND_ADVANCE, 0),
        )
    }

    fun save(settings: AppSettings) {
        prefs.edit {
            putString(KEY_THEME, settings.preset.name)
            putFloat(KEY_HUE, settings.customHue)
            putString(KEY_SORT, settings.sortOrder.name)
            putString(KEY_LANGUAGE, settings.language.name)
            putBoolean(KEY_REMIND, settings.remind)
            putInt(KEY_REMIND_TIME, settings.remindMinutes)
            putInt(KEY_REMIND_ADVANCE, settings.remindAdvanceDays)
        }
    }

    /** Whether the first-run explanation has been shown, which happens once per install. */
    fun hasSeenGuide(): Boolean = prefs.getBoolean(KEY_GUIDE, false)

    fun markGuideSeen() {
        prefs.edit { putBoolean(KEY_GUIDE, true) }
    }

    private companion object {
        const val PREFS_NAME = "tickcount_settings"
        const val KEY_THEME = "theme"
        const val KEY_HUE = "hue"
        const val KEY_SORT = "sort"
        const val KEY_LANGUAGE = "language"
        const val KEY_SECONDS = "seconds"
        const val KEY_REMIND_TIME = "remind_time"
        const val KEY_REMIND_ADVANCE = "remind_advance"
        const val KEY_GUIDE = "guide_seen"
        const val KEY_REMIND = "remind"
    }
}
