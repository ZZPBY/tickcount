package io.github.zzpby.tickcount.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import io.github.zzpby.tickcount.MainActivity
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.data.CountdownEvent
import io.github.zzpby.tickcount.data.EventStore
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * The one reminder TickCount can give without asking for an alarm permission.
 *
 * Only the *next* countdown is scheduled, not every one: an alarm for a date six months out
 * would have to survive a reboot, a time-zone change and a clock change to be worth anything,
 * and the receiver reschedules the following one the moment this one fires. That keeps one
 * alarm in the system instead of thirty.
 *
 * The alarm is `setAndAllowWhileIdle`, which is inexact on purpose. Exact alarms need
 * `SCHEDULE_EXACT_ALARM` from Android 12, and a countdown is not a train: a reminder that
 * arrives within the hour is enough, and the permission is not worth asking for.
 */
object Reminders {

    const val CHANNEL_ID = "reminders"
    const val EXTRA_ID = "event_id"

    private const val REQUEST_CODE = 1077

    /** The time of day a reminder is delivered. */
    private val AT: LocalTime = LocalTime.of(9, 0)

    /** Creates the channel, which is idempotent and must happen before any notification. */
    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        channel.description = context.getString(R.string.reminder_channel_description)
        manager.createNotificationChannel(channel)
    }

    /** Whether the app may post a notification at all on this version of Android. */
    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** The next countdown that still has a reminder ahead of it, or null for none. */
    fun nextReminder(events: List<CountdownEvent>, now: Instant = Instant.now()): CountdownEvent? {
        val zone = ZoneId.systemDefault()
        return events
            .sortedBy { it.date }
            .firstOrNull { it.date.atTime(AT).atZone(zone).toInstant().isAfter(now) }
    }

    /**
     * Replaces whatever was scheduled with the reminder for [events].
     *
     * Called whenever the list changes and on every launch, because the next countdown can
     * change without an alarm ever firing — deleting the upcoming one, say.
     */
    fun schedule(context: Context, events: List<CountdownEvent>) {
        val host = context.applicationContext
        val next = nextReminder(events)
        val alarms = host.getSystemService(AlarmManager::class.java) ?: return
        if (next == null) {
            alarms.cancel(pendingIntent(host))
            return
        }
        ensureChannel(host)
        val moment = next.date.atTime(AT).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, moment, pendingIntent(host))
    }

    fun cancel(context: Context) {
        val host = context.applicationContext
        host.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(host))
    }

    /** Posts the reminder for [event] and schedules whatever comes next. */
    fun deliver(context: Context, event: CountdownEvent) {
        val host = context.applicationContext
        ensureChannel(host)
        val open = PendingIntent.getActivity(
            host,
            0,
            Intent(host, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = when {
            event.date.isAfter(LocalDate.now()) ->
                host.getString(R.string.reminder_notification_later, event.date.toString())
            else -> host.getString(R.string.reminder_notification_today)
        }
        val notification = androidx.core.app.NotificationCompat.Builder(host, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(event.title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        host.getSystemService(NotificationManager::class.java)
            ?.notify(event.id.hashCode(), notification)

        // The list may have changed since this one was scheduled, so the next reminder is
        // picked from what is stored now rather than from what was scheduled then.
        schedule(host, EventStore(host).load())
    }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
