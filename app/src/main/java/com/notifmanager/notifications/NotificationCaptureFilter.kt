package com.notifmanager.notifications

import android.app.Notification
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.StatusBarNotification

data class CapturedAppProfile(
    val isSystemApp: Boolean,
    val hasLauncherActivity: Boolean,
) {
    val batchesByDefault: Boolean = !isSystemApp && hasLauncherActivity
}

object NotificationCaptureFilter {
    fun appProfile(packageManager: PackageManager, packageName: String): CapturedAppProfile {
        val appInfo = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, 0)
            }
        }.getOrNull()
        return CapturedAppProfile(
            isSystemApp = appInfo?.isSystemApp() ?: true,
            hasLauncherActivity = packageManager.getLaunchIntentForPackage(packageName) != null,
        )
    }

    fun shouldStore(
        sbn: StatusBarNotification,
        appProfile: CapturedAppProfile,
        title: String?,
        text: String?,
    ): Boolean {
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return false
        if (title.isNullOrBlank() && text.isNullOrBlank()) return false
        if (isKnownNoise(sbn.packageName, title, text)) return false
        if (!appProfile.isSystemApp) return true
        return !isTransientSystemNotification(sbn, title, text)
    }

    private fun isTransientSystemNotification(
        sbn: StatusBarNotification,
        title: String?,
        text: String?,
    ): Boolean {
        val notification = sbn.notification
        val flags = notification.flags
        val ongoing = flags and Notification.FLAG_ONGOING_EVENT != 0
        val foregroundService = flags and Notification.FLAG_FOREGROUND_SERVICE != 0
        val noClear = flags and Notification.FLAG_NO_CLEAR != 0
        val transientCategory = notification.category in TRANSIENT_SYSTEM_CATEGORIES
        val content = listOfNotNull(title, text, notification.tickerText?.toString())
            .joinToString(" ")
            .lowercase()
        return !sbn.isClearable ||
            ongoing ||
            foregroundService ||
            noClear ||
            transientCategory ||
            TRANSIENT_SYSTEM_TEXT.any { content.contains(it) }
    }

    internal fun isKnownNoise(packageName: String, title: String?, text: String?): Boolean {
        val content = listOfNotNull(title, text).joinToString(" ").lowercase()
        if (content.isBlank()) return false
        if (
            packageName.contains("whatsapp", ignoreCase = true) &&
            (content.contains("checking for new messages") || content.contains("checking for messages"))
        ) {
            return true
        }
        return PROGRESS_NOISE_REGEXES.any { it.containsMatchIn(content) } ||
            PERCENT_ONLY_REGEX.matches(content.trim()) ||
            DOWNLOADING_PERCENT_REGEX.containsMatchIn(content) ||
            BACKUP_PERCENT_REGEX.containsMatchIn(content)
    }

    private fun ApplicationInfo.isSystemApp(): Boolean {
        return flags and ApplicationInfo.FLAG_SYSTEM != 0 ||
            flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
    }

    private val TRANSIENT_SYSTEM_CATEGORIES = setOf(
        Notification.CATEGORY_PROGRESS,
        Notification.CATEGORY_SERVICE,
        Notification.CATEGORY_STATUS,
        Notification.CATEGORY_SYSTEM,
        Notification.CATEGORY_TRANSPORT,
    )

    private val TRANSIENT_SYSTEM_TEXT = listOf(
        "battery",
        "charging",
        "power",
        "torch",
        "flashlight",
        "usb",
        "hotspot",
        "vpn",
        "do not disturb",
        "screen recording",
        "screenshot",
    )

    private val PROGRESS_NOISE_REGEXES = listOf(
        Regex("""\bflashlight\s+on\b"""),
        Regex("""\btorch\s+on\b"""),
        Regex("""\bbacking\s+up\b"""),
        Regex("""\bbackup\s+in\s+progress\b"""),
        Regex("""\bdownload\s+in\s+progress\b"""),
        Regex("""\bupload\s+in\s+progress\b"""),
        Regex("""\binstalling\s+update\b"""),
        Regex("""\bcopying\s+files?\b"""),
        Regex("""\bmoving\s+files?\b"""),
        Regex("""\bscanning\s+files?\b"""),
    )

    private val PERCENT_ONLY_REGEX = Regex("""^(progress\s*)?\d{1,3}\s?%$""")
    private val DOWNLOADING_PERCENT_REGEX = Regex("""\b(download|downloading|upload|uploading|installing|updating)\b.*\b\d{1,3}\s?%""")
    private val BACKUP_PERCENT_REGEX = Regex("""\b(backing up|backup|syncing)\b.*\b\d{1,3}\s?%""")
}
