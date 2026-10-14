package io.github.zzpby.tickcount.domain

import io.github.zzpby.tickcount.data.CountdownEvent

/**
 * The largest gap a batch may step by, in days: ten years.
 *
 * A ceiling rather than no limit at all, because the field is typed into and a slipped
 * digit should be refused rather than turned into a countdown centuries out.
 */
const val MAX_BATCH_STEP_DAYS = 3650

/**
 * The most countdowns one batch may add.
 *
 * Bounded for the same reason: the whole document is rewritten on every save, and a
 * mistyped zero — 100 for 10 — would put a hundred countdowns in one list. A hundred is
 * already far more than a list of dates is usually read for.
 */
const val MAX_BATCH_COUNT = 100

/**
 * The countdowns a batch adds: [count] further copies of [template], each [stepDays] after
 * the one before.
 *
 * The date the editor was opened on is *not* part of this list — the screen creates that
 * one itself, with the values typed into it, and this is what goes alongside. So adding a
 * countdown on the 14th with a step of 2 and a count of 3 leaves the 14th, the 16th, the
 * 18th and the 20th in the list, which is what the editor says it will do before it does
 * any of it.
 *
 * Counting from the date after the first is what makes the interval mean what it says.
 * Starting at zero and stepping would describe the same days as "a count of 4", and the
 * two fields would then agree with neither the countdown list nor each other.
 *
 * Everything but the date and the id is copied from [template]: the name, the time of day,
 * the tags, the colour and the pin are one answer given once, and [createdAt] is the moment
 * of the whole batch, because that is when its entries were added. A step of zero or less,
 * or a count of zero or less, adds nothing.
 */
fun batchOf(
    template: CountdownEvent,
    stepDays: Int,
    count: Int,
    createdAt: Long,
): List<CountdownEvent> {
    if (stepDays <= 0 || count <= 0) return emptyList()
    return (1..count).map { index ->
        template.copy(
            id = CountdownEvent.newId(),
            date = template.date.plusDays(index.toLong() * stepDays),
            createdAt = createdAt,
        )
    }
}
