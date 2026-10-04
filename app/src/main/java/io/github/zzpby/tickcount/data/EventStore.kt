package io.github.zzpby.tickcount.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit

/**
 * Persists the countdown list as a small JSON document inside
 * [android.content.SharedPreferences].
 *
 * A hand-rolled JSON store is deliberate: the data set is a handful of records, so
 * pulling in a database or DataStore would cost more than it saves. Writes are applied
 * asynchronously and the whole document is rewritten each time, which is trivially cheap
 * at this size.
 *
 * The document has always been an array; what changed when a day was allowed to hold
 * several countdowns is that the array is no longer collapsed into a date-keyed map on
 * the way in. Records written by an older build therefore load as they are — they carry
 * no id, and one is derived for them. [EventCodec] owns both directions, because the
 * backup file carries the same records and must not drift from them.
 */
class EventStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Reads the saved countdowns in stored order. Never throws: corrupt records are
     * skipped and unreadable data starts the app empty, because there is nothing useful
     * to do about it at this point.
     */
    fun load(): List<CountdownEvent> {
        val raw = prefs.getString(KEY_EVENTS, null) ?: return emptyList()
        return try {
            EventCodec.decode(raw)
        } catch (e: Exception) {
            Log.w(TAG, "Could not read saved countdowns; starting empty.", e)
            emptyList()
        }
    }

    /** Replaces the stored document with [events]. */
    fun save(events: List<CountdownEvent>) {
        prefs.edit { putString(KEY_EVENTS, EventCodec.encode(events)) }
    }

    private companion object {
        const val TAG = "EventStore"
        const val PREFS_NAME = "tickcount_events"
        const val KEY_EVENTS = "events"
    }
}
