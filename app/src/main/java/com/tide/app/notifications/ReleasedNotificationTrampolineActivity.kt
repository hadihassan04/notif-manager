package com.tide.app.notifications

import android.app.Activity
import android.os.Bundle
import android.widget.Toast

/**
 * Shade tap fallback when the captured content PendingIntent is gone.
 *
 * Started via [android.app.PendingIntent.getActivity] so the click runs under
 * the notification-click exemption. From here Tide may send the original
 * PendingIntent (with background-activity-start options on API 34+) or launch
 * the app. A BroadcastReceiver must not be the thing that starts another app's UI.
 */
class ReleasedNotificationTrampolineActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
        val key = intent.getStringExtra(ReleasedNotificationIntents.EXTRA_NOTIFICATION_KEY)
        val packageName = intent.getStringExtra(ReleasedNotificationIntents.EXTRA_PACKAGE_NAME).orEmpty()
        val appLabel = intent.getStringExtra(ReleasedNotificationIntents.EXTRA_APP_LABEL).orEmpty()
        if (key != null) {
            when (val result = NotificationOpener.open(this, key, packageName, appLabel)) {
                NotificationOpener.OpenResult.SentOriginal -> Unit
                is NotificationOpener.OpenResult.OpenedApp -> {
                    Toast.makeText(
                        this,
                        ReleasedNotificationPlan.fallbackMessage(true, result.appLabel),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                NotificationOpener.OpenResult.Unavailable -> {
                    Toast.makeText(
                        this,
                        ReleasedNotificationPlan.fallbackMessage(false, appLabel),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
        finish()
        overridePendingTransition(0, 0)
    }
}
