package io.github.zzpby.tickcount.domain

import io.github.zzpby.tickcount.data.CountdownEvent
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Writes countdowns out as an iCalendar file, so they can be read into a system calendar.
 *
 * The point is the hand-over, not syncing: the app still has no network and no calendar
 * permission, and the file goes wherever the user saves it. Everything here is plain text —
 * the format is old and strict enough that a library would only hide which line was wrong.
 */
private const val CRLF = "\r\n"

/** `PRODID`, `VERSION` and the rest of the envelope are fixed; only the events vary. */
private fun header(): String = buildString {
    append("BEGIN:VCALENDAR").append(CRLF)
    append("VERSION:2.0").append(CRLF)
    append("PRODID:-//TickCount//Countdown//CN").append(CRLF)
    append("CALSCALE:GREGORIAN").append(CRLF)
    append("METHOD:PUBLISH").append(CRLF)
}

/**
 * The whole file for [events], stamped with [now].
 *
 * All-day countdowns become all-day events — `DTEND` is exclusive in iCalendar, so it is the
 * day after — and a countdown with a time becomes a one-hour event starting at that minute.
 * An event with a time still gets its date from [CountdownEvent.date], in [zone].
 */
fun icsFor(
    events: List<CountdownEvent>,
    zone: ZoneId,
    now: ZonedDateTime,
): String {
    val stamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
        .withZone(ZoneId.of("UTC"))
        .format(now)

    val raw = buildString {
        append(header())
        events.forEach { event ->
            append("BEGIN:VEVENT").append(CRLF)
            append("UID:").append(event.id).append("@tickcount").append(CRLF)
            append("DTSTAMP:").append(stamp).append(CRLF)

            val start = event.time?.let { event.date.atTime(it) }
            if (start == null) {
                append("DTSTART;VALUE=DATE:").append(dateText(event.date)).append(CRLF)
                append("DTEND;VALUE=DATE:").append(dateText(event.date.plusDays(1))).append(CRLF)
            } else {
                val startZoned = start.atZone(zone)
                append("DTSTART:").append(utcText(startZoned)).append(CRLF)
                append("DTEND:").append(utcText(startZoned.plusHours(1))).append(CRLF)
            }

            append("SUMMARY:").append(escape(event.title)).append(CRLF)
            if (event.tags.isNotEmpty()) {
                append("CATEGORIES:").append(escape(event.tags.joinToString(","))).append(CRLF)
            }
            append("END:VEVENT").append(CRLF)
        }
        append("END:VCALENDAR").append(CRLF)
    }

    // Folded line by line at the end, so no builder above has to remember the limit.
    return raw.trimEnd('\r', '\n')
        .split(CRLF)
        .joinToString(CRLF) { fold(it) } + CRLF
}

private fun dateText(date: LocalDate): String =
    DateTimeFormatter.BASIC_ISO_DATE.format(date)

private fun utcText(moment: ZonedDateTime): String =
    DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
        .withZone(ZoneId.of("UTC"))
        .format(moment)

/**
 * Escapes one of iCalendar's text fields.
 *
 * Backslash first: escaping it after the others would double the backslashes they added.
 */
internal fun escape(value: String): String = buildString {
    value.forEach { char ->
        when (char) {
            '\\' -> append("\\\\")
            ';' -> append("\\;")
            ',' -> append("\\,")
            '\n' -> append("\\n")
            '\r' -> Unit
            else -> append(char)
        }
    }
}

/**
 * Folds one content line at 75 octets, as the format requires.
 *
 * The fold has to be counted in *bytes* rather than characters: a Chinese title is three bytes
 * a character, and folding by character count produces lines that are over the limit for every
 * reader that checks. A continuation line starts with a single space, which counts with it.
 */
internal fun fold(line: String): String {
    val bytes = line.toByteArray(Charsets.UTF_8)
    if (bytes.size <= MAX_OCTETS) return line

    val out = StringBuilder()
    var used = 0
    var first = true
    var index = 0
    while (index < line.length) {
        val codePoint = line.codePointAt(index)
        val charCount = Character.charCount(codePoint)
        val width = String(Character.toChars(codePoint)).toByteArray(Charsets.UTF_8).size
        // The first line may hold 75 octets; every continuation starts with a space, so it
        // may hold 74 before that space is added.
        val limit = if (first) MAX_OCTETS else MAX_OCTETS - 1
        if (used + width > limit) {
            out.append(CRLF).append(' ')
            used = 0
            first = false
        }
        out.append(String(Character.toChars(codePoint)))
        used += width
        index += charCount
    }
    return out.toString()
}

/** 75 octets is the limit the format gives a content line, not counting the line break. */
private const val MAX_OCTETS = 75
