package io.github.zzpby.tickcount.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

/**
 * Persists the countdown list as a small JSON document inside
 * [android.content.SharedPreferences].
 *
 * A hand-rolled JSON store is deliberate: the data set is a handful of records, so
 * pulling in a database or DataStore would cost more than it saves. Writes are
 * applied asynchronously and the whole document is rewritten each time, which is
 * trivially cheap at this size.
 *
 * The document has always been an array; what changed when a day was allowed to
 * hold several countdowns is that the array is no longer collapsed into a
 * date-keyed map on the way in. Records written by an older build therefore load
 * as they are — they carry no id, and one is derived for them.
 */
class EventStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Reads the saved countdowns in stored order. Never throws; corrupt records are
     * skipped and unreadable data starts the app empty.
     */
    fun load(): List<CountdownEvent> {
        val raw = prefs.getString(KEY_EVENTS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    val epochDay = obj.optLong(KEY_EPOCH_DAY, Long.MIN_VALUE)
                    if (epochDay == Long.MIN_VALUE) continue

                    add(
                        CountdownEvent(
                            id = obj.optString(KEY_ID).takeIf { it.isNotEmpty() }
                                ?: migratedId(epochDay, index),
                            date = LocalDate.ofEpochDay(epochDay),
                            title = obj.optString(KEY_TITLE).take(CountdownEvent.MAX_TITLE_LENGTH),
                            time = readTime(obj.optInt(KEY_MINUTE, NO_TIME)),
                            tags = readTags(obj.optJSONArray(KEY_TAGS)),
                            colorHue = readHue(obj),
                            // Absent on records written before the list could be
                            // ordered by it; zero sorts them below everything added
                            // since, and the sort falls back to the date from there.
                            createdAt = obj.optLong(KEY_CREATED, 0L),
                            pinned = obj.optBoolean(KEY_PINNED, false),
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read saved countdowns; starting empty.", e)
            emptyList()
        }
    }

    /** Replaces the stored document with [events]. */
    fun save(events: List<CountdownEvent>) {
        val array = JSONArray()
        // Sorted so the file is stable and diffable rather than insertion-ordered.
        // The id breaks ties, which keeps the order total even in the improbable
        // case of two countdowns sharing a date, a time and a title.
        events.sortedWith(compareBy({ it.epochDay }, { it.time }, { it.id })).forEach { event ->
            array.put(
                JSONObject()
                    .put(KEY_ID, event.id)
                    .put(KEY_EPOCH_DAY, event.epochDay)
                    .put(KEY_TITLE, event.title)
                    .put(KEY_MINUTE, event.time?.let { it.hour * 60 + it.minute } ?: NO_TIME)
                    .put(KEY_TAGS, JSONArray(event.tags))
                    .put(KEY_CREATED, event.createdAt)
                    .put(KEY_PINNED, event.pinned)
                    .apply { event.colorHue?.let { put(KEY_HUE, it.toDouble()) } }
            )
        }
        prefs.edit { putString(KEY_EVENTS, array.toString()) }
    }

    /**
     * The colour of a record, from whichever generation of the format it is in.
     *
     * Countdowns used to carry an index into a fixed six-colour palette that the
     * app assigned for them. Those indices are translated to the hue of the accent
     * they used to mean, so an existing list keeps the colours it was showing
     * rather than all turning into the default at once.
     */
    private fun readHue(obj: JSONObject): Float? = when {
        obj.has(KEY_HUE) -> obj.optDouble(KEY_HUE).toFloat().takeIf { it.isFinite() }
        obj.has(KEY_LEGACY_COLOR) -> {
            val index = obj.optInt(KEY_LEGACY_COLOR, 0)
            LEGACY_PALETTE_HUES[((index % LEGACY_PALETTE_HUES.size) + LEGACY_PALETTE_HUES.size) %
                LEGACY_PALETTE_HUES.size]
        }

        else -> null
    }

    /**
     * An id for a record written before countdowns had one.
     *
     * Derived from the date and the array position rather than randomly, so that
     * merely loading the old document twice yields the same ids and a widget bound
     * to one of them keeps pointing at the same countdown.
     */
    private fun migratedId(epochDay: Long, index: Int) = "day-$epochDay-$index"

    private fun readTime(minuteOfDay: Int): LocalTime? =
        if (minuteOfDay == NO_TIME || minuteOfDay !in 0..MINUTES_PER_DAY - 1) {
            null
        } else {
            LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
        }

    private fun readTags(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val tag = array.optString(i).trim().take(CountdownEvent.MAX_TAG_LENGTH)
                if (tag.isNotEmpty() && tag !in this) add(tag)
            }
        }.take(CountdownEvent.MAX_TAG_COUNT)
    }

    private companion object {
        const val TAG = "EventStore"
        const val PREFS_NAME = "tickcount_events"
        const val KEY_EVENTS = "events"
        const val KEY_ID = "i"
        const val KEY_EPOCH_DAY = "d"
        const val KEY_TITLE = "t"
        const val KEY_MINUTE = "m"
        const val KEY_TAGS = "g"
        const val KEY_HUE = "h"
        const val KEY_CREATED = "a"
        const val KEY_PINNED = "p"
        const val KEY_LEGACY_COLOR = "c"

        /** Sentinel for "no time of day", which is not a valid minute. */
        const val NO_TIME = -1
        const val MINUTES_PER_DAY = 24 * 60

        /** The hues of the six accents the app used to assign by rotation. */
        val LEGACY_PALETTE_HUES = listOf(234f, 352f, 144f, 32f, 199f, 272f)
    }
}
