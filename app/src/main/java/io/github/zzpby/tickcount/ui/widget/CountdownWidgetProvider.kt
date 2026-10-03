package io.github.zzpby.tickcount.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * The home-screen widget's entry point.
 *
 * Everything it can be asked to do — an update, a resize, a deletion, the midnight
 * tick it schedules for itself — ends in the same place, so this stays a thin
 * translation of framework callbacks over [CountdownWidgets].
 */
class CountdownWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { CountdownWidgets.update(context, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        // A resize can cross one of the shape thresholds, so the arrangement is
        // picked again rather than merely redrawn. The bundle is passed straight
        // through: it is the authoritative size, and reading it back from
        // AppWidgetManager can still return the previous one.
        CountdownWidgets.update(context, appWidgetId, newOptions)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // Otherwise the mapping from widget id to chosen date grows for ever.
        appWidgetIds.forEach { CountdownWidgets.forget(context, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (CountdownWidgets.isTick(intent)) {
            CountdownWidgets.refreshAll(context)
        }
    }
}
