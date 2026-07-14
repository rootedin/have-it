package com.haveit.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.haveit.app.HaveItApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Alarms don't survive a reboot, so re-register every active reminder on boot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? HaveItApplication ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val scheduler = ReminderScheduler(context)
                app.container.habitRepository.observeActiveHabits().first().forEach { habit ->
                    scheduler.reschedule(habit)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
