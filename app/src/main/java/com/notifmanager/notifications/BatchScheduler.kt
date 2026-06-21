package com.notifmanager.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class BatchScheduler(private val context: Context) {
    fun schedule(scheduleId: Long, batchId: String, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleId.toInt() + DIGEST_REQUEST_CODE,
            Intent(context, BatchReleaseReceiver::class.java).apply {
                putExtra(BatchReleaseReceiver.EXTRA_SCHEDULE_ID, scheduleId)
                putExtra(BatchReleaseReceiver.EXTRA_BATCH_ID, batchId)
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
        private const val DIGEST_REQUEST_CODE = 4100
    }
}
