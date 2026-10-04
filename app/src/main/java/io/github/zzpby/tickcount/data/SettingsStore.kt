package io.github.zzpby.tickcount.data

import android.content.Context
import androidx.core.content.edit
import io.github.zzpby.tickcount.domain.SortOrder

/**
 * The palettes the app offers, plus one the user mixes themselves.
 *
 * White and black are the light and dark schemes rather than colours of their own,
 * so choosing either fixes the app's lightness and stops it following the system
 * setting. Blue and green are the light scheme with a coloured accent, and
 * [CUSTOM] takes its hue from [AppSettings.customHue].
 */
enum class ThemePreset(val dark: Boolean) {
    WHITE(false),
    BLACK(true),
    BLUE(false),
    GREEN(false),
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
    val preset: ThemePreset = ThemePreset.WHITE,
    val customHue: Float = DEFAULT_HUE,
    val sortOrder: SortOrder = SortOrder.DATE_ASC,
    val language: AppLanguage = AppLanguage.SYSTEM,
)

/** Where the hue strip starts before the user has moved it: Tailwind's blue. */
const val DEFAULT_HUE = 221f

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
            ?: ThemePreset.WHITE
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
        )
    }

    fun save(settings: AppSettings) {
        prefs.edit {
            putString(KEY_THEME, settings.preset.name)
            putFloat(KEY_HUE, settings.customHue)
            putString(KEY_SORT, settings.sortOrder.name)
            putString(KEY_LANGUAGE, settings.language.name)
        }
    }

    private companion object {
        const val PREFS_NAME = "tickcount_settings"
        const val KEY_THEME = "theme"
        const val KEY_HUE = "hue"
        const val KEY_SORT = "sort"
        const val KEY_LANGUAGE = "language"
    }
}
