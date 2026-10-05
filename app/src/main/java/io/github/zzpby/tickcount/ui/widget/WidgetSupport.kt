package io.github.zzpby.tickcount.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.github.zzpby.tickcount.EXTRA_OPEN_EVENT
import io.github.zzpby.tickcount.MainActivity
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.data.CountdownEvent
import java.time.format.DateTimeFormatter

/**
 * The parts both widgets need.
 *
 * They are drawn from the same store and open the same app, so the measurement, the tap
 * target and the refresh belong to neither of them in particular.
 */

/**
 * Redraws every widget the app has placed, of both kinds.
 *
 * Anything that changes the store calls this rather than the two separately: a caller that
 * has to remember which widgets exist is a caller that will eventually forget one.
 */
internal fun refreshAllWidgets(context: Context) {
    CountdownWidgets.refreshAll(context)
    UpcomingWidgets.refreshAll(context)
}

/**
 * A date or time pattern from resources, in the language the device is set to.
 *
 * Built per call rather than cached: the patterns are translated — a date reads differently
 * in Chinese and English — so the formatter belongs to the configuration the widget is being
 * drawn under.
 */
internal fun widgetFormatter(context: Context, patternRes: Int): DateTimeFormatter {
    val locale = context.resources.configuration.locales[0]
    return DateTimeFormatter.ofPattern(context.getString(patternRes), locale)
}

/**
 * A countdown's date line: the full date, and the time it names when it names one.
 *
 * The time is what "precise to the minute" looks like on a widget, and leaving it out made
 * a countdown set to 09:30 indistinguishable from an all-day one.
 */
internal fun dateLine(context: Context, event: CountdownEvent): String {
    val date = event.date.format(widgetFormatter(context, R.string.date_format_full))
    val time = event.time ?: return date
    return context.getString(
        R.string.text_with_time,
        date,
        time.format(widgetFormatter(context, R.string.time_format)),
    )
}

/**
 * A widget's size in dp, taken from whichever source actually reported one.
 *
 * The bundle handed to `onAppWidgetOptionsChanged` is the freshest, but nothing
 * guarantees it carries these keys. Trusting it alone means a launcher that sends a
 * partial bundle reads as 0x0, which the layout rules take to mean "not measured yet" and
 * answer with the default arrangement for good — the exact symptom a partial bundle would
 * produce on a device we cannot debug.
 *
 * So each dimension walks the candidates and keeps the first one above zero. The maximum
 * is the last resort, because some launchers fill that in and leave the minimum at zero.
 */
internal fun widgetSize(
    manager: AppWidgetManager,
    appWidgetId: Int,
    options: Bundle?,
): Pair<Int, Int> {
    val stored = manager.getAppWidgetOptions(appWidgetId)

    fun dimension(min: String, max: String): Int = sequenceOf(
        options?.getInt(min),
        stored.getInt(min),
        options?.getInt(max),
        stored.getInt(max),
    ).filterNotNull().firstOrNull { it > 0 } ?: 0

    return dimension(
        AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
        AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
    ) to dimension(
        AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
        AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
    )
}

/**
 * A tap target that brings the app up on [eventId], or merely up when it is null.
 *
 * The countdown is named in the intent rather than looked up after the launch: the widget
 * already knows which one it is about, so landing on the list and leaving the user to find
 * it again would be asking them to repeat themselves.
 *
 * [requestCode] has to differ per tap target — per widget, and per row within a widget —
 * or the pending intents would be one intent with the last countdown written into it.
 */
internal fun openApp(context: Context, requestCode: Int, eventId: String?): PendingIntent =
    PendingIntent.getActivity(
        context,
        requestCode,
        Intent(context, MainActivity::class.java).putExtra(EXTRA_OPEN_EVENT, eventId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
