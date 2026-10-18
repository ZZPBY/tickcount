package io.github.zzpby.tickcount.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.zzpby.tickcount.data.EventStore
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
        val events = EventStore(context).load()
        val event = if (id == null) events.firstOrNull { it.date >= LocalDate.now() }
        else events.firstOrNull { it.id == id }
        if (event != null && !event.date.isBefore(LocalDate.now()) && Reminders.canNotify(context)) {
            Reminders.deliver(context, event)
        } else {
            // Still reschedule, or a deleted countdown would silence every later reminder.
            val settings = io.github.zzpby.tickcount.data.SettingsStore(context).load()
            Reminders.schedule(context, events, settings.remindMinutes, settings.remindAdvanceDays)
        }
    }
}
