package io.github.zzpby.tickcount.ui.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import io.github.zzpby.tickcount.MainActivity
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.data.EventStore
import io.github.zzpby.tickcount.domain.CountdownPhase
import io.github.zzpby.tickcount.domain.CountdownUnit
import io.github.zzpby.tickcount.domain.UnitCount
import io.github.zzpby.tickcount.domain.WidgetCountdown
import io.github.zzpby.tickcount.domain.WidgetShape
import io.github.zzpby.tickcount.domain.breakdownOf
import io.github.zzpby.tickcount.domain.countdownTo
import io.github.zzpby.tickcount.domain.widgetCountdownFor
import io.github.zzpby.tickcount.domain.widgetShapeFor
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private const val PREFS = "tickcount_widgets"
private const val KEY_ACTION_TICK = "io.github.zzpby.tickcount.WIDGET_TICK"
private const val TAG = "TickCountWidget"

/** Both arrangements own a clock, and every update re-bases both. */
private val CLOCKS = intArrayOf(R.id.row_clock, R.id.col_clock)

/**
 * The home-screen widget's plumbing, kept out of [CountdownWidgetProvider] so the
 * receiver stays a thin translation of framework callbacks.
 *
 * The display is split in two on purpose. A widget cannot be redrawn every second
 * — Android allows one periodic update per half hour — so the whole-day count is
 * static text and the ticking comes from a `Chronometer`, which the system drives
 * on its own without waking this app at all.
 */
object CountdownWidgets {

    /**
     * The midnight the alarm is already armed for.
     *
     * A resize fires the options callback many times over, and re-arming the alarm
     * on every one of those steps is a system call per frame for no gain.
     */
    @Volatile
    private var scheduledMidnight = Long.MIN_VALUE

    // ------------------------------------------------------------- placement

    /** The countdown a given widget instance was configured to show. */
    fun savedDate(context: Context, appWidgetId: Int): LocalDate? =
        prefs(context).getLong(key(appWidgetId), Long.MIN_VALUE)
            .takeIf { it != Long.MIN_VALUE }
            ?.let(LocalDate::ofEpochDay)

    fun saveDate(context: Context, appWidgetId: Int, date: LocalDate) {
        prefs(context).edit().putLong(key(appWidgetId), date.toEpochDay()).apply()
    }

    fun forget(context: Context, appWidgetId: Int) {
        prefs(context).edit().remove(key(appWidgetId)).apply()
    }

    private fun key(appWidgetId: Int) = "widget_$appWidgetId"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // -------------------------------------------------------------- redrawing

    /** Every placed instance, for when the app's own data changed underneath. */
    fun refreshAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, CountdownWidgetProvider::class.java)
        )
        ids.forEach { update(context, it) }
    }

    fun update(context: Context, appWidgetId: Int, options: Bundle? = null) {
        try {
            draw(context, appWidgetId, options)
        } catch (error: RuntimeException) {
            // A widget that throws once must not stay frozen for ever. Swallowing it
            // here means the next callback — the half-hourly update, the next resize,
            // the app being opened — gets a fresh attempt rather than a dead receiver.
            Log.w(TAG, "could not draw widget $appWidgetId", error)
        }
    }

    private fun draw(context: Context, appWidgetId: Int, options: Bundle?) {
        val manager = AppWidgetManager.getInstance(context)
        val (widthDp, heightDp) = widgetSize(manager, appWidgetId, options)
        val shape = widgetShapeFor(widthDp, heightDp)
        Log.d(TAG, "widget $appWidgetId measured ${widthDp}x${heightDp}dp -> $shape")

        // One layout id for every shape, so a resize is an ordinary property change
        // rather than a re-inflate. See the layout's own comment for why that matters.
        val views = RemoteViews(context.packageName, R.layout.widget_countdown)

        val events = EventStore(context).load()
        // A widget whose countdown was deleted falls back to the soonest one still
        // standing, rather than going blank.
        val event = savedDate(context, appWidgetId)?.let { events[it] }
            ?: events.values.minByOrNull { it.epochDay }

        arrange(views, shape)
        if (event == null) {
            renderEmpty(context, views)
        } else {
            render(context, views, event.title, event.date)
        }

        views.setOnClickPendingIntent(R.id.widget_root, openApp(context, appWidgetId))
        manager.updateAppWidget(appWidgetId, views)
        scheduleMidnightTick(context)
    }

    /**
     * The widget's size in dp, taken from whichever source actually reported one.
     *
     * The bundle handed to `onAppWidgetOptionsChanged` is the freshest, but nothing
     * guarantees it carries these keys. Trusting it alone — as this did — means a
     * launcher that sends a partial bundle reads as 0x0, which [widgetShapeFor] takes
     * to mean "not measured yet" and answers with the stacked arrangement every time.
     * The widget then stops changing with its size, for good, which is exactly the
     * symptom a partial bundle would produce on a device we cannot debug.
     *
     * So each dimension walks the candidates and keeps the first one above zero. The
     * maximum is the last resort, because some launchers fill that in and leave the
     * minimum at zero.
     */
    private fun widgetSize(
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

    private fun render(
        context: Context,
        views: RemoteViews,
        title: String,
        date: LocalDate,
    ) {
        val now = ZonedDateTime.now()
        val countdown = widgetCountdownFor(date, now)

        // Both arrangements are filled in on every update, not just the visible one.
        // All of these ids exist in the single layout, so writing the hidden half
        // costs nothing and leaves a resize with nothing to do but flip visibility.
        views.setTextViewText(R.id.row_name, title)
        views.setTextViewText(R.id.col_name, title)
        // The stacked arrangement has room to spell the countdown out in calendar
        // units; the one-line arrangement keeps the single day total.
        views.setTextViewText(R.id.row_days, dayLabel(context, countdown.days, countdown.phase))
        views.setTextViewText(R.id.col_days, calendarLabel(context, date, now, countdown))
        views.setTextViewText(R.id.col_date, date.format(dateFormatter(context)))

        // The Chronometer is anchored to the nearer midnight, never to the target
        // itself: counting to a date months away would render as thousands of
        // hours. `null` format leaves Android's own HH:MM:SS.
        val elapsed = SystemClock.elapsedRealtime()
        val offset = countdown.secondsToBoundary * 1_000L
        val base = if (countdown.countingDown) elapsed + offset else elapsed - offset
        CLOCKS.forEach { id ->
            views.setChronometer(id, base, null, true)
            views.setChronometerCountDown(id, countdown.countingDown)
        }
    }

    private fun renderEmpty(context: Context, views: RemoteViews) {
        val message = context.getString(R.string.label_no_countdown)
        views.setTextViewText(R.id.row_name, message)
        views.setTextViewText(R.id.col_name, message)
        views.setTextViewText(R.id.row_days, "")
        views.setTextViewText(R.id.col_days, "")
        views.setTextViewText(R.id.col_date, "")

        // A stopped clock cannot be hidden outright in every launcher, so it is left
        // showing a dash rather than a stalled number.
        val now = SystemClock.elapsedRealtime()
        CLOCKS.forEach { views.setChronometer(it, now, "—", false) }

        // Even the narrowest shape can say why it is empty. In the one-line shape the
        // whole column is hidden anyway, so this costs nothing there.
        views.setViewVisibility(R.id.col_name, View.VISIBLE)
    }

    private fun dayLabel(context: Context, days: Long, phase: CountdownPhase): String = when {
        phase == CountdownPhase.TODAY -> context.getString(R.string.widget_today)
        // Fewer than 24 hours to a midnight means the target is tomorrow.
        phase == CountdownPhase.FUTURE && days == 0L -> context.getString(R.string.widget_tomorrow)
        // A plural resource rather than a plain string: English needs "1 day left",
        // and Chinese carries a single `other` form.
        phase == CountdownPhase.FUTURE -> context.resources.getQuantityString(
            R.plurals.widget_days_left, days.toInt(), days,
        )
        else -> context.resources.getQuantityString(
            R.plurals.widget_days_ago, days.toInt(), days,
        )
    }

    /**
     * The tall shape's label: the same countdown spelled out in calendar units.
     *
     * "Today" and "tomorrow" stay words in both shapes — they are shorter and
     * clearer than the arithmetic that produces them. Everything else becomes the
     * years/months/days split, so a date a year out reads "1 year 1 month 4 days"
     * where the one-row shape has room only for "400 days".
     */
    private fun calendarLabel(
        context: Context,
        date: LocalDate,
        now: ZonedDateTime,
        countdown: WidgetCountdown,
    ): String = when {
        countdown.phase == CountdownPhase.TODAY -> context.getString(R.string.widget_today)
        countdown.phase == CountdownPhase.FUTURE && countdown.days == 0L ->
            context.getString(R.string.widget_tomorrow)
        else -> {
            val spelled = spelledOut(context, breakdownOf(countdownTo(date, now).parts))
            // Only reachable if every unit came out zero, which the phases above
            // should have caught; the total is a safe thing to fall back to.
            if (spelled.isEmpty()) {
                dayLabel(context, countdown.days, countdown.phase)
            } else {
                context.getString(
                    if (countdown.phase == CountdownPhase.FUTURE) {
                        R.string.widget_breakdown_left
                    } else {
                        R.string.widget_breakdown_ago
                    },
                    spelled,
                )
            }
        }
    }

    /** "1 year 1 month 4 days" — each unit carries its own separator. */
    private fun spelledOut(context: Context, units: List<UnitCount>): String {
        val resources = context.resources
        return units.joinToString("") { (unit, count) ->
            val plural = when (unit) {
                CountdownUnit.YEAR -> R.plurals.widget_years
                CountdownUnit.MONTH -> R.plurals.widget_months
                CountdownUnit.DAY -> R.plurals.widget_breakdown_days
            }
            resources.getQuantityString(plural, count.toInt(), count)
        }.trim()
    }

    // ----------------------------------------------------------------- shapes

    /**
     * Shows the rows the shape has room for and hides the rest.
     *
     * Every branch is a visibility change on a view that always exists. That is the
     * point of the single layout: no launcher is ever asked to re-inflate the widget,
     * so none of them can get it wrong and leave the content frozen.
     */
    private fun arrange(views: RemoteViews, shape: WidgetShape) {
        val oneLine = shape == WidgetShape.ROW
        views.setViewVisibility(R.id.widget_row, shown(oneLine))
        views.setViewVisibility(R.id.widget_column, shown(!oneLine))

        // Too narrow for the name beside the clock: keep the countdown and drop only
        // the outer two rows.
        views.setViewVisibility(R.id.col_name, shown(shape == WidgetShape.STACK))
        views.setViewVisibility(
            R.id.col_days,
            shown(shape == WidgetShape.STACK || shape == WidgetShape.MINI),
        )
        views.setViewVisibility(R.id.col_date, shown(shape == WidgetShape.STACK))
    }

    private fun shown(on: Boolean) = if (on) View.VISIBLE else View.GONE

    // ------------------------------------------------------------------ clock

    /**
     * Re-derives the day count and re-bases the Chronometer just after midnight.
     *
     * Without this a clock anchored to yesterday's midnight would run negative
     * until the half-hourly update caught up. `setAndAllowWhileIdle` is inexact
     * and, unlike the exact variants, needs no alarm permission — which is what
     * lets the app keep its promise of declaring none. Alarms do not survive a
     * reboot, but the widget framework re-broadcasts a full update then anyway.
     */
    private fun scheduleMidnightTick(context: Context) {
        val nextMidnight = ZonedDateTime.now()
            .toLocalDate()
            .plusDays(1)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        // Already armed for this midnight. A resize gesture calls this many times a
        // second, and re-arming the alarm on each step buys nothing.
        if (scheduledMidnight == nextMidnight) return

        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        alarms.setAndAllowWhileIdle(AlarmManager.RTC, nextMidnight, tickIntent(context))
        scheduledMidnight = nextMidnight
    }

    private fun tickIntent(context: Context) = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, CountdownWidgetProvider::class.java).setAction(KEY_ACTION_TICK),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** True when a broadcast is the midnight tick rather than a framework update. */
    fun isTick(intent: Intent?) = intent?.action == KEY_ACTION_TICK

    private fun openApp(context: Context, appWidgetId: Int) = PendingIntent.getActivity(
        context,
        appWidgetId,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun dateFormatter(context: Context): DateTimeFormatter {
        val locale = context.resources.configuration.locales[0]
        return DateTimeFormatter.ofPattern(context.getString(R.string.date_format_full), locale)
    }
}
