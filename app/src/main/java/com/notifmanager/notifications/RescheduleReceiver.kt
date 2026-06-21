package com.notifmanager.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notifmanager.NotifManagerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                (context.applicationContext as NotifManagerApp).repository.reschedule()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
