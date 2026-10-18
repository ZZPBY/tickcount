package io.github.zzpby.tickcount.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.zzpby.tickcount.data.EventStore
import io.github.zzpby.tickcount.data.SettingsStore

/**
 * Puts the reminder back after a restart.
 *
 * Alarms do not survive a reboot, and a countdown app whose reminders quietly stop working
 * after the phone is switched off once is worse than one that never offered them.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val settings = SettingsStore(context).load()
        if (!settings.remind) return
        Reminders.schedule(
            context,
            EventStore(context).load(),
            settings.remindMinutes,
            settings.remindLeadMinutes,
        )
    }
}
