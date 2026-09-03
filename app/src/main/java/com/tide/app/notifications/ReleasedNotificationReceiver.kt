package com.tide.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.tide.app.TideApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReleasedNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val key = intent?.getStringExtra(EXTRA_NOTIFICATION_KEY) ?: return
        when (intent.action) {
            ACTION_OPEN -> {
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
                val appLabel = intent.getStringExtra(EXTRA_APP_LABEL).orEmpty()
                when (val result = NotificationOpener.open(context, key, packageName, appLabel)) {
                    NotificationOpener.OpenResult.SentOriginal -> Unit
                    is NotificationOpener.OpenResult.OpenedApp -> {
                        Toast.makeText(
                            context,
                            ReleasedNotificationPlan.fallbackMessage(true, result.appLabel),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    NotificationOpener.OpenResult.Unavailable -> {
                        Toast.makeText(
                            context,
                            ReleasedNotificationPlan.fallbackMessage(false, appLabel),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            }
            ACTION_DISMISS -> {
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
        }
    }

    companion object {
        const val ACTION_OPEN = "com.tide.app.action.OPEN_RELEASED_NOTIFICATION"
        const val ACTION_DISMISS = "com.tide.app.action.DISMISS_RELEASED_NOTIFICATION"
        const val EXTRA_NOTIFICATION_KEY = "notification_key"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val EXTRA_APP_LABEL = "app_label"
    }
}
