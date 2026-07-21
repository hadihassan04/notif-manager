package com.notifmanager.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class OpenHoursScheduler(private val context: Context) {
    fun schedule(windowId: Long, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            windowId.toInt() + OPEN_HOURS_REQUEST_CODE,
            Intent(context, OpenHoursReceiver::class.java).apply {
                putExtra(OpenHoursReceiver.EXTRA_WINDOW_ID, windowId)
                putExtra(OpenHoursReceiver.EXTRA_TRIGGER_AT_MILLIS, triggerAtMillis)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    companion object {
        private const val OPEN_HOURS_REQUEST_CODE = 8100
    }
}
