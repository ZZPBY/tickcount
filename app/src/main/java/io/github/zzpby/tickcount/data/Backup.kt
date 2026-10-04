package io.github.zzpby.tickcount.data

import io.github.zzpby.tickcount.domain.SortOrder
import org.json.JSONObject

/** What an import should do with what is already on the device. */
enum class ImportMode { REPLACE, MERGE }

/** How an import went, for the screen to report. */
sealed interface ImportOutcome {
    data class Applied(val mode: ImportMode, val count: Int) : ImportOutcome

    /** The file is encrypted and the password did not open it. */
    data object WrongPassword : ImportOutcome

    /** The file is not a TickCount backup at all. */
    data object NotABackup : ImportOutcome

    /** A backup, but one that cannot be read — truncated, or written by something else. */
    data object Unreadable : ImportOutcome
}

/** Everything a backup carries: the countdowns, and how the app was set up. */
data class Backup(val events: List<CountdownEvent>, val settings: AppSettings)

/**
 * The backup file's shape.
 *
 * The countdowns are written by [EventCodec], the same one the store uses, so a backup
 * cannot drift from what the app actually keeps. The settings travel with them because a
 * backup is normally made in order to put everything back the way it was on another
 * device, and a list restored into a differently-themed app is only half restored.
 */
object BackupCodec {

    private const val APP = "tickcount"
    private const val FORMAT = 1

    private const val KEY_APP = "app"
    private const val KEY_FORMAT = "format"
    private const val KEY_EXPORTED_AT = "exportedAt"
    private const val KEY_EVENTS = "events"
    private const val KEY_SETTINGS = "settings"

    /** [exportedAt] is a parameter rather than read here, so that encoding is repeatable. */
    fun encode(backup: Backup, exportedAt: Long = System.currentTimeMillis()): String =
        JSONObject()
            .put(KEY_APP, APP)
            .put(KEY_FORMAT, FORMAT)
            .put(KEY_EXPORTED_AT, exportedAt)
            .put(KEY_EVENTS, EventCodec.toJson(backup.events))
            .put(KEY_SETTINGS, settingsToJson(backup.settings))
            // Indented: a backup written in plain text is meant to be legible, and one
            // that is not is encrypted before it is written anyway.
            .toString(2)

    /** Throws when [text] is not a backup this version understands. */
    fun decode(text: String): Backup {
        val root = JSONObject(text)
        require(root.optString(KEY_APP) == APP) { "not a TickCount backup" }
        return Backup(
            events = root.optJSONArray(KEY_EVENTS)?.let(EventCodec::fromJson) ?: emptyList(),
            // An older backup may predate a setting; the defaults are the right answer
            // for anything it does not carry.
            settings = root.optJSONObject(KEY_SETTINGS)?.let(::settingsFromJson) ?: AppSettings(),
        )
    }

    private fun settingsToJson(settings: AppSettings): JSONObject = JSONObject()
        .put("theme", settings.preset.name)
        .put("hue", settings.customHue.toDouble())
        .put("sort", settings.sortOrder.name)
        .put("language", settings.language.name)

    private fun settingsFromJson(json: JSONObject): AppSettings = AppSettings(
        preset = json.optString("theme").toEnum(ThemePreset.entries, AppSettings().preset),
        customHue = json.optDouble("hue", DEFAULT_HUE.toDouble()).toFloat(),
        sortOrder = json.optString("sort").toEnum(SortOrder.entries, AppSettings().sortOrder),
        language = json.optString("language").toEnum(AppLanguage.entries, AppSettings().language),
    )

    /** The named constant, or [fallback] when the name is missing or from a newer build. */
    private fun <T : Enum<T>> String.toEnum(values: List<T>, fallback: T): T =
        values.firstOrNull { it.name == this } ?: fallback
}
