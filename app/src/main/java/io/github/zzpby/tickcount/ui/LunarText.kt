package io.github.zzpby.tickcount.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.data.CountdownEvent

/**
 * The lunar date of a countdown, written the way the current language writes it.
 *
 * A function rather than a field on the model: the numbers are stored, and the words around
 * them belong to the screen — the same lunar day reads differently in Chinese and English.
 */
@Composable
fun lunarLabel(month: Int, day: Int, leap: Boolean): String =
    stringResource(
        if (leap) R.string.lunar_date_leap else R.string.lunar_date,
        month,
        day,
    )

/** The same, for a countdown that may not have one, or null when it was chosen as a solar day. */
@Composable
fun lunarLabelOf(event: CountdownEvent): String? {
    val month = event.lunarMonth ?: return null
    val day = event.lunarDay ?: return null
    return lunarLabel(month, day, event.lunarLeap)
}

/** [text] with the lunar date appended, when the countdown has one. */
@Composable
fun withLunar(event: CountdownEvent, text: String): String {
    val lunar = lunarLabelOf(event) ?: return text
    return "$text · $lunar"
}
