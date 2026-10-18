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
import androidx.core.app.NotificationCompat
import io.github.zzpby.tickcount.MainActivity
import io.github.zzpby.tickcount.R
import io.github.zzpby.tickcount.data.CountdownEvent
import io.github.zzpby.tickcount.data.EventStore
import java.time.Instant
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

    /** Where a countdown may be reminded before its day arrives: the choices on screen. */
    val ADVANCE_CHOICES = listOf(0, 1, 3)

    /** When the reminder falls, for the countdown it belongs to. */
    data class Planned(val event: CountdownEvent, val at: Instant)

    /** Creates the channel, which is idempotent and must happen before any notification. */
    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notify_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        channel.description = context.getString(R.string.notify_channel_description)
        manager.createNotificationChannel(channel)
    }

    /** Whether the app may post a notification at all on this version of Android. */
    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * The next reminder that still lies ahead, or null when there is none.
     *
     * Computed rather than stored, so the diagnostics on the notification screen describe
     * the same arithmetic the alarm uses — a "next reminder" line of its own would be a
     * second implementation to keep in step.
     */
    fun planned(
        events: List<CountdownEvent>,
        minutes: Int,
        advanceDays: Int,
        now: Instant = Instant.now(),
    ): Planned? {
        val zone = ZoneId.systemDefault()
        val at = LocalTime.of(minutes / 60, minutes % 60)
        return events
            .sortedBy { it.date }
            .map { event ->
                Planned(
                    event = event,
                    at = event.date.minusDays(advanceDays.toLong()).atTime(at)
                        .atZone(zone).toInstant(),
                )
            }
            .firstOrNull { it.at.isAfter(now) }
    }

    /**
     * Replaces whatever was scheduled with the reminder for [events].
     *
     * Called whenever the list changes, whenever the reminder is turned on, and on every
     * launch: the next countdown can change without an alarm ever firing — deleting the
     * upcoming one, say.
     */
    fun schedule(context: Context, events: List<CountdownEvent>, minutes: Int, advanceDays: Int) {
        val host = context.applicationContext
        val alarms = host.getSystemService(AlarmManager::class.java) ?: return
        val next = planned(events, minutes, advanceDays)
        if (next == null) {
            alarms.cancel(pendingIntent(host))
            return
        }
        ensureChannel(host)
        alarms.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            next.at.toEpochMilli(),
            pendingIntent(host),
        )
    }

    fun cancel(context: Context) {
        val host = context.applicationContext
        host.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(host))
    }

    /** Posts the reminder for [event] and schedules whatever comes next. */
    fun deliver(context: Context, event: CountdownEvent) {
        val host = context.applicationContext
        ensureChannel(host)
        val text = when {
            event.date.atStartOfDay(host.zone()).toInstant().isAfter(Instant.now()) ->
                host.getString(R.string.notify_later, event.date.toString())
            else -> host.getString(R.string.notify_today)
        }
        post(host, event.id.hashCode(), event.title, text)

        // The list may have changed since this one was scheduled, so the next reminder is
        // picked from what is stored now rather than from what was scheduled then.
        val settings = io.github.zzpby.tickcount.data.SettingsStore(host).load()
        schedule(host, EventStore(host).load(), settings.remindMinutes, settings.remindAdvanceDays)
    }

    /** A notification to confirm that the permission, channel and popup all work. */
    fun deliverTest(context: Context) {
        val host = context.applicationContext
        ensureChannel(host)
        post(host, TEST_ID, host.getString(R.string.app_name), host.getString(R.string.notify_test_body))
    }

    private fun post(context: Context, id: Int, title: String, text: String) {
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)?.notify(id, notification)
    }

    private const val TEST_ID = 77

    private fun Context.zone(): ZoneId = ZoneId.systemDefault()

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
