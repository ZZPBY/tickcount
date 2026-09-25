package io.github.ginko.tickcount.data

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * Persists the countdown list as a small JSON document inside
 * [android.content.SharedPreferences].
 *
 * A hand-rolled JSON store is deliberate: the data set is a handful of
 * `(date, name)` pairs, so pulling in a database or DataStore would cost more
 * than it saves. Writes are applied asynchronously and the whole document is
 * rewritten each time, which is trivially cheap at this size.
 */
class EventStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Reads the saved countdowns, keyed by date. Never throws; corrupt data is dropped. */
    fun load(): Map<LocalDate, CountdownEvent> {
        val raw = prefs.getString(KEY_EVENTS, null) ?: return emptyMap()
        return try {
            val array = JSONArray(raw)
            buildMap {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val epochDay = obj.optLong(KEY_EPOCH_DAY, Long.MIN_VALUE)
                    if (epochDay == Long.MIN_VALUE) continue
                    val title = obj.optString(KEY_TITLE).take(CountdownEvent.MAX_TITLE_LENGTH)
                    val colorIndex =
                        obj.optInt(KEY_COLOR_INDEX, 0).coerceIn(0, CountdownEvent.COLOR_COUNT - 1)
                    val date = LocalDate.ofEpochDay(epochDay)
                    put(date, CountdownEvent(date = date, title = title, colorIndex = colorIndex))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read saved countdowns; starting empty.", e)
            emptyMap()
        }
    }

    /** Replaces the stored document with [events]. */
    fun save(events: Map<LocalDate, CountdownEvent>) {
        val array = JSONArray()
        // Sorted so the file is stable and diffable rather than hash-ordered.
        events.values
            .sortedBy { it.epochDay }
            .forEach { event ->
                array.put(
                    JSONObject()
                        .put(KEY_EPOCH_DAY, event.epochDay)
                        .put(KEY_TITLE, event.title)
                        .put(KEY_COLOR_INDEX, event.colorIndex)
                )
            }
        prefs.edit().putString(KEY_EVENTS, array.toString()).apply()
    }

    private companion object {
        const val TAG = "EventStore"
        const val PREFS_NAME = "tickcount_events"
        const val KEY_EVENTS = "events"
        const val KEY_EPOCH_DAY = "d"
        const val KEY_TITLE = "t"
        const val KEY_COLOR_INDEX = "c"
    }
}
