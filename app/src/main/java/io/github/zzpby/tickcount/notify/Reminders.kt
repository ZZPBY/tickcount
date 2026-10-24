package io.github.zzpby.tickcount.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
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

    /**
     * The permission's name written out rather than taken from `Manifest.permission`, whose
     * constant only exists from Android 13: the string is what the platform matches on, and
     * asking for it on an older release is simply never done.
     */
    const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"

    private const val REQUEST_CODE = 1077

    /** How far ahead a reminder may be set: a month, which is as far as anyone plans ahead. */
    const val MAX_LEAD_MINUTES = 30 * 24 * 60

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

    /**
     * Whether a notification the app posts would actually be seen.
     *
     * Three separate switches can each silence it, and reading only one of them is how this
     * went wrong: below Android 13 there is no runtime permission at all, so asking for
     * `POST_NOTIFICATIONS` answered "yes" while the system settings had the app's
     * notifications turned off — the reminder was then scheduled, fired, and dropped with
     * nothing in the app saying so.
     */
    fun canNotify(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        // The app's own switch, which is the only one below Android 13 and the first one set
        // by "turn notifications off" anywhere in the system settings.
        if (!manager.areNotificationsEnabled()) return false
        if (needsPermission(context)) return false
        // A channel can be silenced on its own since Android 8, leaving the app's switch on.
        val channel = manager.getNotificationChannel(CHANNEL_ID)
        return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
    }

    /**
     * Whether what is missing is the Android 13 runtime permission, which the app may ask for
     * itself — as opposed to a switch in the system settings, which only the user can flip.
     *
     * Below Android 13 this is always false: the permission does not exist, and asking for it
     * would do nothing at all, so the caller has to offer the settings screen instead.
     */
    fun needsPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED

    /** Opens the system screen where this app's notifications are turned on and off. */
    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // A few ROMs ship without this screen; a missing setting is not worth a crash.
        runCatching { context.startActivity(intent) }
    }

    /**
     * When the reminder for [event] falls, in [zone].
     *
     * Anchored to the countdown itself — its own time when it names one, the reminder time
     * of day when it is all day — and then moved back by its own offset if it has one, or by
     * the app-wide [leadMinutes] if it does not.
     */
    fun momentOf(
        event: CountdownEvent,
        minutes: Int,
        leadMinutes: Int,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Instant {
        val fallback = LocalTime.of(minutes / 60, minutes % 60)
        val base = event.time?.let { event.date.atTime(it) } ?: event.date.atTime(fallback)
        return base.minusMinutes((event.remindLeadMinutes ?: leadMinutes).toLong())
            .atZone(zone).toInstant()
    }

    fun planned(
        events: List<CountdownEvent>,
        minutes: Int,
        leadMinutes: Int,
        now: Instant = Instant.now(),
        // A parameter rather than a straight read of the default zone: the tests assert an
        // exact instant, and a machine set to a different time zone would otherwise be
        // testing a different question. The app always passes its own zone.
        zone: ZoneId = ZoneId.systemDefault(),
    ): Planned? {
        return events
            .sortedBy { it.date }
            .map { event -> Planned(event, momentOf(event, minutes, leadMinutes, zone)) }
            .firstOrNull { it.at.isAfter(now) }
    }

    /**
     * Replaces whatever was scheduled with the reminder for [events].
     *
     * Called whenever the list changes, whenever the reminder is turned on, and on every
     * launch: the next countdown can change without an alarm ever firing — deleting the
     * upcoming one, say.
     */
    fun schedule(context: Context, events: List<CountdownEvent>, minutes: Int, leadMinutes: Int) {
        val host = context.applicationContext
        val alarms = host.getSystemService(AlarmManager::class.java) ?: return
        val next = planned(events, minutes, leadMinutes)
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
        schedule(host, EventStore(host).load(), settings.remindMinutes, settings.remindLeadMinutes)
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
