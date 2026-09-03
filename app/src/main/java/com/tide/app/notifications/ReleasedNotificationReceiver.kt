package com.tide.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tide.app.TideApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Shade swipe-away / deleteIntent only. Opening another app from a tap must go
 * through the captured contentIntent or [ReleasedNotificationTrampolineActivity].
 */
class ReleasedNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_DISMISS) return
        val key = intent.getStringExtra(ReleasedNotificationIntents.EXTRA_NOTIFICATION_KEY) ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? TideApp
                app?.repository?.dismissNotifications(listOf(key))
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_DISMISS = "com.tide.app.action.DISMISS_RELEASED_NOTIFICATION"
    }
}
