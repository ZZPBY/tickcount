package io.github.zzpby.tickcount.ui.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Bundle

/**
 * The list widget's entry point.
 *
 * Thinner than its sibling: this widget has no configuration to be given, no alarm of its
 * own and nothing to tick, so update and resize are the whole of what it is told.
 */
class UpcomingWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { UpcomingWidgets.update(context, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        // A resize changes how many rows fit, so the arrangement is worked out again
        // rather than merely redrawn. The bundle is passed straight through because it is
        // the authoritative size.
        UpcomingWidgets.update(context, appWidgetId, newOptions)
    }
}
