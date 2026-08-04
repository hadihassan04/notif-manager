package com.tide.app.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.StatusBarNotification

data class CapturedAppProfile(
    val isSystemApp: Boolean,
    val hasLauncherActivity: Boolean,
    val isMediaPlayer: Boolean = false,
) {
    val batchesByDefault: Boolean = !isSystemApp && hasLauncherActivity && !isMediaPlayer
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
            isMediaPlayer = isMediaPlayerPackage(packageName),
        )
    }

    /**
     * Media/transport notifications carry the playback controls. Batching them cancels the
     * notification, which strips the controls and can tear down the app's foreground playback
     * service, so they must always be delivered instantly.
     */
    fun isMediaPlayback(sbn: StatusBarNotification, appProfile: CapturedAppProfile): Boolean {
        val notification = sbn.notification
        if (notification.category == Notification.CATEGORY_TRANSPORT) return true
        if (notification.extras.containsKey(MEDIA_SESSION_EXTRA)) return true
        val template = notification.extras.getString(Notification.EXTRA_TEMPLATE)
        if (template != null && template.contains("MediaStyle")) return true
        return appProfile.isMediaPlayer
    }

    internal fun isMediaPlayerPackage(packageName: String): Boolean {
        val lower = packageName.lowercase()
        return MEDIA_PLAYER_PACKAGES.contains(lower) ||
            MEDIA_PLAYER_HINTS.any { lower.contains(it) }
    }

    fun shouldStore(
        sbn: StatusBarNotification,
        appProfile: CapturedAppProfile,
        title: String?,
        text: String?,
        channelImportance: Int? = null,
    ): Boolean {
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return false
        if (title.isNullOrBlank() && text.isNullOrBlank()) return false
        if (isKnownNoise(sbn.packageName, title, text)) return false
        if (channelImportance != null && channelImportance <= NotificationManager.IMPORTANCE_MIN) return false
        if (isStatusNotification(sbn)) return false
        if (!appProfile.isSystemApp) return true
        return !isTransientSystemNotification(sbn, title, text)
    }

    /**
     * Something the app is *doing*, not something that happened: a sync running, a
     * download in flight, a player holding its controls open. These are never inbox
     * items, and holding one back would cancel it — which for a foreground service
     * means tearing down the work it represents.
     */
    private fun isStatusNotification(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification
        val flags = notification.flags
        if (flags and Notification.FLAG_ONGOING_EVENT != 0) return true
        if (flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return true
        if (!sbn.isClearable) return true
        val extras = notification.extras
        if (extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)) return true
        return extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) > 0
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
        return PROGRESS_NOISE_REGEXES.any { it.containsMatchIn(content) } ||
            PERCENT_ONLY_REGEX.matches(content.trim()) ||
            DOWNLOADING_PERCENT_REGEX.containsMatchIn(content) ||
            BACKUP_PERCENT_REGEX.containsMatchIn(content)
    }

    private fun ApplicationInfo.isSystemApp(): Boolean {
        return flags and ApplicationInfo.FLAG_SYSTEM != 0 ||
            flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
    }

    private const val MEDIA_SESSION_EXTRA = "android.mediaSession"

    private val MEDIA_PLAYER_PACKAGES = setOf(
        // Video
        "com.google.android.youtube",
        "com.google.android.apps.youtube.music",
        "com.google.android.youtube.tv",
        "com.netflix.mediaclient",
        "com.amazon.avod.thirdpartyclient",
        "com.disney.disneyplus",
        "com.hulu.plus",
        "com.hbo.hbonow",
        "com.wbd.stream",
        "tv.twitch.android.app",
        "com.plexapp.android",
        "com.jellyfin.mobile",
        "org.jellyfin.mobile",
        "com.mxtech.videoplayer.ad",
        "com.mxtech.videoplayer.pro",
        "is.xyz.mpv",
        "org.videolan.vlc",
        "com.instantbits.cast.webvideo",
        "com.brouken.player",
        "com.google.android.apps.photos",
        // Music / audio
        "com.spotify.music",
        "com.spotify.lite",
        "com.apple.android.music",
        "com.amazon.mp3",
        "deezer.android.app",
        "com.aspiro.tidal",
        "com.soundcloud.android",
        "com.bandcamp.android",
        "com.pandora.android",
        "com.clearchannel.iheartradio.controller",
        "tunein.player",
        "com.audible.application",
        "au.com.shiftyjelly.pocketcasts",
        "fm.castbox.audiobook.radio.podcast",
        "com.google.android.apps.podcasts",
        "de.danoeh.antennapod",
        "org.videolan.vlc.debug",
        "com.kapp.youtube.final",
        "com.maxmpz.audioplayer",
        "com.jrtstudio.AnotherMusicPlayer",
        "code.name.monkey.retromusic",
        "com.simplecity.amp_library",
        "org.oxycblt.auxio",
        "com.shabinder.spotiflyer",
        "com.awedea.nyx",
        "io.github.muntashirakon.music",
        "com.doubleTwist.androidPlayer",
        "com.foobar2000.foobar2000",
        "com.n7mobile.nplayer",
    )

    private val MEDIA_PLAYER_HINTS = listOf(
        "musicplayer",
        "videoplayer",
        "mediaplayer",
        "audioplayer",
        "podcast",
        "youtube",
        "spotify",
        "vlc",
        "mpv",
        "netflix",
        "deezer",
        "soundcloud",
        "audiobook",
        ".music",
        "music.player",
        "kodi",
        "poweramp",
        "musicolet",
        "blackplayer",
        "phonograph",
        "vanced",
        "newpipe",
        "libretube",
        "grayjay",
        "smarttube",
    )

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
        // Message apps that keep a socket open say so in a notification.
        Regex("""\b(checking|waiting|listening)\s+for\s+(new\s+)?messages?\b"""),
        Regex("""\bwaiting\s+for\s+(new\s+)?(mail|email|notifications?)\b"""),
        // Media/file indexing sweeps.
        Regex("""\bscanning\s+(for\s+)?(media|music|videos?|photos?|library)\b"""),
        Regex("""\bmedia\s+scann?(er|ing)\b"""),
        Regex("""\bindexing\s+(media|files?|library)\b"""),
        Regex("""\bsync(ing)?\s+in\s+progress\b"""),
    )

    private val PERCENT_ONLY_REGEX = Regex("""^(progress\s*)?\d{1,3}\s?%$""")
    private val DOWNLOADING_PERCENT_REGEX = Regex("""\b(download|downloading|upload|uploading|installing|updating)\b.*\b\d{1,3}\s?%""")
    private val BACKUP_PERCENT_REGEX = Regex("""\b(backing up|backup|syncing)\b.*\b\d{1,3}\s?%""")
}
