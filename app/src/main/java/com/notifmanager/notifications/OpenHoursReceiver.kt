package com.notifmanager.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notifmanager.TideApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OpenHoursReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val windowId = intent?.getLongExtra(EXTRA_WINDOW_ID, -1L)?.takeIf { it > 0 } ?: return
        val triggerAtMillis = intent.getLongExtra(EXTRA_TRIGGER_AT_MILLIS, 0L).takeIf { it > 0 }
            ?: System.currentTimeMillis()
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as TideApp
                app.repository.handleOpenHoursStart(windowId, triggerAtMillis)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_WINDOW_ID = "open_window_id"
        const val EXTRA_TRIGGER_AT_MILLIS = "open_trigger_at_millis"
    }
}
