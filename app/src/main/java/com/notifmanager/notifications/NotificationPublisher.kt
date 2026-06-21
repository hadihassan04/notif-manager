package com.notifmanager.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.notifmanager.MainActivity
import com.notifmanager.R
import com.notifmanager.data.NotificationEntity

class NotificationPublisher(private val context: Context) {
    fun showDigest(batchId: String, notifications: List<NotificationEntity>) {
        ensureChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (notifications.isEmpty()) return

        val topApps = notifications
            .groupingBy { it.appLabel }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(3)
            .joinToString { it.key }

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_BATCH_ID, batchId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            batchId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, DIGEST_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Your inbox is ready")
            .setContentText("${notifications.size} notifications held from $topApps")
            .setStyle(NotificationCompat.BigTextStyle().bigText("${notifications.size} notifications are ready. Top apps: $topApps."))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(batchId.hashCode(), notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            DIGEST_CHANNEL_ID,
            context.getString(R.string.digest_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.digest_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val DIGEST_CHANNEL_ID = "batch_digests"
    }
}
