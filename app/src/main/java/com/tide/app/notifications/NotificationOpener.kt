package com.tide.app.notifications

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle

object NotificationOpener {
    fun open(
        context: Context,
        notificationKey: String,
        packageName: String,
        appLabel: String,
    ): OpenResult {
        if (PendingIntentRegistry.send(notificationKey, sendOptions(context))) {
            return OpenResult.SentOriginal
        }
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

    /**
     * Background-activity-start options are only meaningful from an Activity
     * (Inbox row taps, or the shade trampoline). A cold BroadcastReceiver must
     * not claim this exemption.
     */
    fun sendOptions(context: Context): Bundle? {
        if (!usesActivitySendOptions(context is Activity, Build.VERSION.SDK_INT)) return null
        val options = ActivityOptions.makeBasic()
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
        } else {
            @Suppress("DEPRECATION")
            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
        }
        options.setPendingIntentBackgroundActivityStartMode(mode)
        return options.toBundle()
    }

    fun usesActivitySendOptions(isActivityContext: Boolean, sdkInt: Int): Boolean {
        return isActivityContext && sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    }

    sealed class OpenResult {
        data object SentOriginal : OpenResult()
        data class OpenedApp(val appLabel: String) : OpenResult()
        data object Unavailable : OpenResult()
    }
}
