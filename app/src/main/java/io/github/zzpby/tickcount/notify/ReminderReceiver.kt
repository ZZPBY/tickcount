package io.github.zzpby.tickcount.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.zzpby.tickcount.data.EventStore
import io.github.zzpby.tickcount.domain.resolvedOn
import java.time.LocalDate

/**
 * Fires at the reminder time and shows the notification.
 *
 * The countdown is looked up in the store rather than carried in the intent: between the
 * alarm being set and it firing, the countdown may have been renamed, moved or deleted, and
 * a reminder for yesterday's date would be worse than none.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(Reminders.EXTRA_ID)
        val today = LocalDate.now()
        // Resolved first: a yearly countdown's stored date is the day it was entered on, and
        // comparing that to today would drop the reminder the year after it was written.
        val events = EventStore(context).load().resolvedOn(today)
        val event = if (id == null) events.firstOrNull { it.date >= today }
        else events.firstOrNull { it.id == id }
        if (event != null && !event.date.isBefore(today) && Reminders.canNotify(context)) {
            Reminders.deliver(context, event)
        } else {
            // Still reschedule, or a deleted countdown would silence every later reminder.
            val settings = io.github.zzpby.tickcount.data.SettingsStore(context).load()
            Reminders.schedule(context, events, settings.remindMinutes, settings.remindLeadMinutes)
        }
    }
}
