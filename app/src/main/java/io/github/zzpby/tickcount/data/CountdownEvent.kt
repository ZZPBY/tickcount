package io.github.zzpby.tickcount.data

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

/**
 * A single saved countdown: a name on a calendar date, optionally on a time of
 * day, carrying any number of free-form tags, in a colour of its own.
 *
 * The date is stored as a date rather than an instant because a calendar day is a
 * local-time concept, and "the 6th" has to keep meaning the 6th wherever the
 * device is. [time] is null for an all-day countdown, whose target is then 00:00
 * on [date] — which is what every countdown was before times existed.
 *
 * [id] exists because a day may hold several countdowns, so the date can no longer
 * serve as the identity. It is also what a home-screen widget stores, which means
 * editing a countdown's date no longer orphans its widget.
 *
 * [colorHue] is a hue in degrees, or null for the default. A hue rather than a
 * packed colour so that one choice can be rendered as a tint on a light surface
 * and a different one on a dark surface, instead of washing out in one of them.
 *
 * [createdAt] is when the countdown was added, which is a different thing from the
 * moment it counts to. It exists so the list can be ordered by what was added most
 * recently. Records written before it existed load as zero, which the sort treats as
 * "older than everything" and breaks the tie with the date — so an old list keeps
 * the order it was showing.
 */
data class CountdownEvent(
    val id: String,
    val date: LocalDate,
    val title: String,
    val time: LocalTime? = null,
    val tags: List<String> = emptyList(),
    val colorHue: Float? = null,
    val createdAt: Long = 0L,
    /** Pinned countdowns lead the list whatever order it is in. */
    val pinned: Boolean = false,
    /**
     * How early to be reminded about this one, in minutes, or null to follow the app-wide
     * setting.
     *
     * Minutes rather than days because the useful offsets are unevenly spaced — half an hour
     * before a deadline, two days before a birthday — and null rather than a sentinel so
     * "nobody has said anything about this one" is a state the code can test for.
     */
    val remindLeadMinutes: Int? = null,
    /**
     * The lunar date this countdown was entered as, when it was entered that way.
     *
     * Kept beside the solar [date] rather than instead of it: the countdown still counts down
     * to one solar day, and the lunar fields exist so the card can say which lunar day that
     * is. Null when the date was chosen in the ordinary solar way.
     */
    val lunarMonth: Int? = null,
    val lunarDay: Int? = null,
    val lunarLeap: Boolean = false,
) {
    val epochDay: Long get() = date.toEpochDay()

    /** True when this countdown names a time of day rather than the whole day. */
    val hasTime: Boolean get() = time != null

    /** The instant this countdown counts to, as observed in [zone]. */
    fun target(zone: ZoneId): ZonedDateTime =
        date.atTime(time ?: LocalTime.MIDNIGHT).atZone(zone)

    companion object {
        const val MAX_TITLE_LENGTH = 60
        const val MAX_TAG_LENGTH = 24
        const val MAX_TAG_COUNT = 8

        fun newId(): String = UUID.randomUUID().toString()
    }
}
