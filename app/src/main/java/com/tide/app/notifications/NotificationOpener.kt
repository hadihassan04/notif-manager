package com.tide.app.notifications

import android.content.Context
import android.content.Intent

object NotificationOpener {
    fun open(
        context: Context,
        notificationKey: String,
        packageName: String,
        appLabel: String,
    ): OpenResult {
        if (PendingIntentRegistry.send(notificationKey)) return OpenResult.SentOriginal
        return if (launchApp(context, packageName)) {
            OpenResult.OpenedApp(appLabel)
        } else {
            OpenResult.Unavailable
        }
    }

    fun launchApp(context: Context, packageName: String): Boolean {
        val launch = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(launch)
            true
        }.getOrDefault(false)
    }

    sealed class OpenResult {
        data object SentOriginal : OpenResult()
        data class OpenedApp(val appLabel: String) : OpenResult()
        data object Unavailable : OpenResult()
    }
}
