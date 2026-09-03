package com.tide.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tide.app.TideApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BatchReleaseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as TideApp
                val batchId = intent?.getStringExtra(EXTRA_BATCH_ID)
                    ?: intent?.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
                        ?.takeIf { it > 0 }
                        ?.let { app.repository.batchIdForSchedule(it) }
                if (batchId != null) {
                    app.repository.releaseBatch(batchId)
                }
                app.repository.reschedule()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_SCHEDULE_ID = "schedule_id"
        const val EXTRA_BATCH_ID = "batch_id"
    }
}
