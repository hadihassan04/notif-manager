package com.tide.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.tide.app.R
import com.tide.app.data.NotificationEntity

class NotificationPublisher(private val context: Context) {
    fun release(notifications: List<NotificationEntity>) {
        ensureChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (notifications.isEmpty()) return
        val specs = ReleasedNotificationPlan.specs(notifications, PendingIntentRegistry::contains)
        specs.forEachIndexed { index, spec -> post(spec, alert = index == 0) }
    }

    private fun post(spec: ReleasedNotificationSpec, alert: Boolean) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_tide)
            .setContentTitle(spec.title)
            .setContentText(spec.text)
            .setSubText(spec.appLabel)
            .setStyle(NotificationCompat.BigTextStyle().bigText(spec.text))
            .setContentIntent(contentIntent(spec))
            .setDeleteIntent(dismissIntent(spec))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setSilent(!alert)
            .apply {
                largeIcon(spec.packageName)?.let(::setLargeIcon)
            }
            .build()
        NotificationManagerCompat.from(context).notify(RELEASED_TAG, shadeId(spec.notificationKey), notification)
    }

    /**
     * Attach the captured app PendingIntent directly so SystemUI delivers the
     * tap with notification-click privileges. Tide's BroadcastReceiver is only
     * the swipe-away deleteIntent — it must not start another app's UI.
     */
    private fun contentIntent(spec: ReleasedNotificationSpec): PendingIntent {
        return ReleasedNotificationIntents.shadeContentIntent(
            PendingIntentRegistry.get(spec.notificationKey),
            trampolineIntent(spec),
        )
    }

    private fun trampolineIntent(spec: ReleasedNotificationSpec): PendingIntent {
        val intent = Intent(context, ReleasedNotificationTrampolineActivity::class.java).apply {
            data = "tide://released/open/${Uri.encode(spec.notificationKey)}".toUri()
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
            putExtra(ReleasedNotificationIntents.EXTRA_NOTIFICATION_KEY, spec.notificationKey)
            putExtra(ReleasedNotificationIntents.EXTRA_PACKAGE_NAME, spec.packageName)
            putExtra(ReleasedNotificationIntents.EXTRA_APP_LABEL, spec.appLabel)
        }
        return PendingIntent.getActivity(
            context,
            shadeId(spec.notificationKey.xorHash("trampoline")),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun dismissIntent(spec: ReleasedNotificationSpec): PendingIntent {
        val intent = Intent(context, ReleasedNotificationReceiver::class.java).apply {
            action = ReleasedNotificationReceiver.ACTION_DISMISS
            data = "tide://released/dismiss/${Uri.encode(spec.notificationKey)}".toUri()
            putExtra(ReleasedNotificationIntents.EXTRA_NOTIFICATION_KEY, spec.notificationKey)
        }
        return PendingIntent.getBroadcast(
            context,
            shadeId(spec.notificationKey.xorHash(ReleasedNotificationReceiver.ACTION_DISMISS)),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun largeIcon(packageName: String): Bitmap? {
        return runCatching {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val size = (48 * context.resources.displayMetrics.density).toInt().coerceAtLeast(48)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
            bitmap
        }.getOrNull()
    }

    private fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.digest_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.digest_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "batch_digests"
        const val RELEASED_TAG = "released"

        fun shadeId(notificationKey: String): Int = notificationKey.hashCode()

        fun cancel(context: Context, notificationKey: String) {
            NotificationManagerCompat.from(context).cancel(RELEASED_TAG, shadeId(notificationKey))
        }
    }
}

private fun String.xorHash(other: String): String = "$this\n$other"
