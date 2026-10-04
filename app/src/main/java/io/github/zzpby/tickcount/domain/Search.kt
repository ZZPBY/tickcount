package io.github.zzpby.tickcount.domain

import io.github.zzpby.tickcount.data.CountdownEvent

/** Which field the search box is looking in. */
enum class SearchScope { NAME, DATE, TIME }

/**
 * The text a countdown is found by under [scope].
 *
 * Tags count as part of the name: they are the other thing someone would remember
 * about a date, and giving them a scope of their own would add a fourth choice to a
 * box that only has room for three. The formatted date and time are passed in
 * rather than built here, so this stays free of both locale and formatting.
 */
fun searchText(
    event: CountdownEvent,
    scope: SearchScope,
    dateText: String,
    timeText: String,
): String = when (scope) {
    SearchScope.NAME -> (listOf(event.title) + event.tags).joinToString(" ")
    SearchScope.DATE -> dateText
    SearchScope.TIME -> timeText
}

/**
 * True when [haystack] answers to [query].
 *
 * A blank query matches everything, which is what makes an empty box show the whole
 * list rather than nothing at all.
 */
fun matchesQuery(haystack: String, query: String): Boolean {
    val trimmed = query.trim()
    return trimmed.isEmpty() || haystack.contains(trimmed, ignoreCase = true)
}
