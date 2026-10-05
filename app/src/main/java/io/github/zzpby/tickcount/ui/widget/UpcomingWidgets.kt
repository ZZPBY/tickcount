package io.github.zzpby.tickcount.ui.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.data.CountdownEvent
import io.github.zzpby.tickcount.data.EventStore
import io.github.zzpby.tickcount.domain.UPCOMING_WIDGET_ROWS
import io.github.zzpby.tickcount.domain.daysFromToday
import io.github.zzpby.tickcount.domain.upcomingCountdowns
import io.github.zzpby.tickcount.domain.upcomingRowsFor
import java.time.LocalDate

private const val TAG = "TickCountWidget"

/**
 * The list widget: what is coming next, one row each.
 *
 * Where the other widget follows a single countdown to the second, this one answers the
 * other question a home screen asks — what have I got coming — and answers it in whole
 * days, which is all a row has room for and all that changes overnight. Nothing here ticks
 * by itself, so the half-hourly update the framework already provides is enough.
 *
 * Rows are addressed by id rather than built by an adapter, because [RemoteViews] has no
 * recycling: the layout holds the maximum number of rows and each update hides the ones
 * this widget has no room for.
 */
object UpcomingWidgets {

    private val rowIds = intArrayOf(
        R.id.upcoming_row_1,
        R.id.upcoming_row_2,
        R.id.upcoming_row_3,
        R.id.upcoming_row_4,
    )

    private val titleIds = intArrayOf(
        R.id.upcoming_title_1,
        R.id.upcoming_title_2,
        R.id.upcoming_title_3,
        R.id.upcoming_title_4,
    )

    private val daysIds = intArrayOf(
        R.id.upcoming_days_1,
        R.id.upcoming_days_2,
        R.id.upcoming_days_3,
        R.id.upcoming_days_4,
    )

    init {
        // The layout and these arrays have to agree, or a row is drawn into nothing.
        check(rowIds.size == UPCOMING_WIDGET_ROWS && titleIds.size == rowIds.size &&
            daysIds.size == rowIds.size) { "the list widget's rows are out of step" }
    }

    /** Every placed instance, for when the app's own data changed underneath. */
    fun refreshAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, UpcomingWidgetProvider::class.java)
        )
        ids.forEach { update(context, it) }
    }

    fun update(context: Context, appWidgetId: Int, options: Bundle? = null) {
        try {
            draw(context, appWidgetId, options)
        } catch (error: RuntimeException) {
            // Same reasoning as the other widget: one bad draw must not leave the receiver
            // dead for every later callback.
            Log.w(TAG, "could not draw list widget $appWidgetId", error)
        }
    }

    private fun draw(context: Context, appWidgetId: Int, options: Bundle?) {
        val manager = AppWidgetManager.getInstance(context)
        val (_, heightDp) = widgetSize(manager, appWidgetId, options)
        val room = upcomingRowsFor(heightDp)

        val today = LocalDate.now()
        val next = upcomingCountdowns(EventStore(context).load(), today, UPCOMING_WIDGET_ROWS)

        val views = RemoteViews(context.packageName, R.layout.widget_upcoming)
        rowIds.indices.forEach { index ->
            val event = next.getOrNull(index)?.takeIf { index < room }
            if (event == null) {
                views.setViewVisibility(rowIds[index], View.GONE)
                return@forEach
            }
            views.setViewVisibility(rowIds[index], View.VISIBLE)
            views.setTextViewText(titleIds[index], event.title)
            views.setTextViewText(daysIds[index], daysLabel(context, event, today))
            // A request code per row as well as per widget: reusing one would leave every
            // row holding whichever countdown was written into it last.
            views.setOnClickPendingIntent(
                rowIds[index],
                openApp(context, appWidgetId * rowIds.size + index, event.id),
            )
        }

        views.setViewVisibility(
            R.id.upcoming_empty,
            if (next.isEmpty()) View.VISIBLE else View.GONE,
        )
        manager.updateAppWidget(appWidgetId, views)
    }

    /**
     * "12 days left", "Tomorrow", or the word for today — and the time of day after it when
     * the countdown names one.
     *
     * A row has no clock of its own, so the time of day is the only way it can say that a
     * countdown is set to half past nine rather than to the whole day.
     */
    private fun daysLabel(context: Context, event: CountdownEvent, today: LocalDate): String {
        val days = daysFromToday(event.date, today)
        val day = when (days) {
            0L -> context.getString(R.string.widget_today)
            1L -> context.getString(R.string.widget_tomorrow)
            else -> context.resources.getQuantityString(
                R.plurals.widget_days_left,
                days.toInt(),
                days.toInt(),
            )
        }

        val time = event.time ?: return day
        return context.getString(
            R.string.text_with_time,
            day,
            time.format(widgetFormatter(context, R.string.time_format)),
        )
    }
}
