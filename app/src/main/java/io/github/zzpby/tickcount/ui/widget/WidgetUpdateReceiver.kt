package io.github.zzpby.tickcount.ui.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Redraws every placed widget as soon as this app has been replaced by a newer build.
 *
 * An update swaps the app underneath a widget that the launcher is still holding. Some
 * launchers answer that by applying the next `RemoteViews` to the view hierarchy they
 * already have instead of inflating a new one, and `RemoteViews` drops any action whose
 * view is not in that hierarchy silently — so a layout whose ids moved leaves the widget
 * showing yesterday's picture for good. Drawing once right after the replace gives the
 * launcher a fresh set of actions before anyone looks at it.
 *
 * The midnight alarm is re-armed by the same draw, which matters because an update is not
 * a restart: nothing else in the app runs at that moment.
 *
 * `MY_PACKAGE_REPLACED` is sent to this app alone, so the receiver stays unexported.
 */
class WidgetUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        refreshAllWidgets(context)
    }
}
