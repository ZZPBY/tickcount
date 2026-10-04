package io.github.zzpby.tickcount.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

/**
 * The stored form of the countdown list, as JSON.
 *
 * Split out from [EventStore] so that the backup file can use exactly the same records
 * the store does — a backup that spoke a second dialect would be a second thing to keep
 * in step — and so that the reading half, which is where the migrations live, can be
 * exercised by a unit test without a device.
 *
 * Unlike the store, [decode] throws on unreadable input rather than returning nothing:
 * a caller restoring a backup has to be able to tell "this file is not a backup" from
 * "this backup is empty".
 */
object EventCodec {

    private const val KEY_ID = "i"
    private const val KEY_EPOCH_DAY = "d"
    private const val KEY_TITLE = "t"
    private const val KEY_MINUTE = "m"
    private const val KEY_TAGS = "g"
    private const val KEY_HUE = "h"
    private const val KEY_CREATED = "a"
    private const val KEY_PINNED = "p"
    private const val KEY_LEGACY_COLOR = "c"

    /** Sentinel for "no time of day", which is not a valid minute. */
    private const val NO_TIME = -1
    private const val MINUTES_PER_DAY = 24 * 60

    /** The hues of the six accents the app used to assign by rotation. */
    private val LEGACY_PALETTE_HUES = listOf(234f, 352f, 144f, 32f, 199f, 272f)

    /**
     * Writes [events] as a JSON array, in a fixed order so the document is stable and
     * diffable rather than hash-ordered. The id breaks ties, which keeps the order total
     * even in the improbable case of two countdowns sharing a date, a time and a title.
     */
    fun encode(events: List<CountdownEvent>): String = toJson(events).toString()

    /** Reads a JSON array written by [encode], or by any earlier version of it. */
    fun decode(text: String): List<CountdownEvent> = fromJson(JSONArray(text))

    /**
     * The records as a JSON array, for callers that embed them in a larger document —
     * the backup file does, and it should carry exactly the records the store does.
     */
    fun toJson(events: List<CountdownEvent>): JSONArray {
        val array = JSONArray()
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
        return array
    }

    /** Reads records from a JSON array written by [toJson], or by any earlier version. */
    fun fromJson(array: JSONArray): List<CountdownEvent> = buildList {
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
                    // Absent on records written before the list could be ordered by it;
                    // zero sorts them below everything added since, and the sort falls
                    // back to the date from there.
                    createdAt = obj.optLong(KEY_CREATED, 0L),
                    pinned = obj.optBoolean(KEY_PINNED, false),
                )
            )
        }
    }

    /**
     * The colour of a record, from whichever generation of the format it is in.
     *
     * Countdowns used to carry an index into a fixed six-colour palette that the app
     * assigned for them. Those indices are translated to the hue of the accent they used
     * to mean, so an existing list keeps the colours it was showing rather than all
     * turning into the default at once.
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
     * Derived from the date and the array position rather than randomly, so that merely
     * loading the old document twice yields the same ids and a widget bound to one of
     * them keeps pointing at the same countdown.
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
}
