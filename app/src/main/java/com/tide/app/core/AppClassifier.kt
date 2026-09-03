package com.tide.app.core

import com.tide.app.data.DeliveryMode

/**
 * What an app is *for*, used to pick Instant vs Batch defaults and to group the
 * shared picker. Classification is by role (messaging, banking, games) rather
 * than a short list of package names, so Messenger and Telegram land with
 * WhatsApp, and Proton VPN does not land with email.
 */
enum class AppRole {
    MEDIA,
    MESSAGING,
    PHONE,
    EMAIL,
    FINANCE,
    DELIVERY,
    TIME,
    AUTH,
    GAME,
    SOCIAL,
    SHOPPING,
    OTHER,
    ;

    val defaultsToInstant: Boolean
        get() = when (this) {
            MEDIA, MESSAGING, PHONE, EMAIL, FINANCE, DELIVERY, TIME, AUTH -> true
            GAME, SOCIAL, SHOPPING, OTHER -> false
        }

    val selectionGroup: AppSelectionGroup
        get() = when (this) {
            MEDIA -> AppSelectionGroup.ALWAYS_INSTANT
            MESSAGING, PHONE, EMAIL, FINANCE, DELIVERY, TIME, AUTH -> AppSelectionGroup.TIME_SENSITIVE
            GAME, SOCIAL, SHOPPING, OTHER -> AppSelectionGroup.EVERYTHING_ELSE
        }
}

enum class AppSelectionGroup {
    ALWAYS_INSTANT,
    INSTANT,
    TIME_SENSITIVE,
    EVERYTHING_ELSE,
    ;

    val title: String
        get() = when (this) {
            ALWAYS_INSTANT -> "Always instant"
            INSTANT -> "Instant"
            TIME_SENSITIVE -> "Recommended"
            EVERYTHING_ELSE -> "Everything else"
        }

    val body: String
        get() = when (this) {
            ALWAYS_INSTANT ->
                "Music and video players, which stop playing if their notification is held."
            INSTANT -> ""
            TIME_SENSITIVE ->
                "Calls, messages, email, banks, deliveries, calendars and sign-in codes."
            EVERYTHING_ELSE ->
                "Social, games, shopping and the rest. These wait for a delivery time."
        }
}

/**
 * Live picker buckets. Recommended is only time-sensitive apps that are not
 * Instant yet. Media stays under Always instant even after the user turns it off.
 */
fun pickerSelectionGroup(role: AppRole, instant: Boolean): AppSelectionGroup {
    return when {
        role == AppRole.MEDIA -> AppSelectionGroup.ALWAYS_INSTANT
        instant -> AppSelectionGroup.INSTANT
        role.defaultsToInstant -> AppSelectionGroup.TIME_SENSITIVE
        else -> AppSelectionGroup.EVERYTHING_ELSE
    }
}

data class AppSignals(
    val packageName: String,
    val label: String,
    val isMediaPlayer: Boolean = false,
    val androidCategory: Int = AppClassifier.CATEGORY_UNDEFINED,
    val handlesSms: Boolean = false,
    val handlesEmail: Boolean = false,
    val handlesDial: Boolean = false,
    val handlesCalendar: Boolean = false,
)

object AppClassifier {
    // ApplicationInfo.category values, inlined so unit tests stay JVM-only.
    const val CATEGORY_UNDEFINED = -1
    const val CATEGORY_GAME = 0
    const val CATEGORY_AUDIO = 1
    const val CATEGORY_VIDEO = 2
    const val CATEGORY_IMAGE = 3
    const val CATEGORY_SOCIAL = 4
    const val CATEGORY_NEWS = 5
    const val CATEGORY_MAPS = 6
    const val CATEGORY_PRODUCTIVITY = 7
    const val CATEGORY_ACCESSIBILITY = 8

    fun classify(signals: AppSignals): AppRole {
        if (signals.isMediaPlayer ||
            signals.androidCategory == CATEGORY_AUDIO ||
            signals.androidCategory == CATEGORY_VIDEO
        ) {
            return AppRole.MEDIA
        }

        val vpn = matches(signals, VPN_TOKENS)
        val emailish = signals.handlesEmail || matches(signals, EMAIL_TOKENS)
        if (vpn && !emailish) return AppRole.OTHER

        if (signals.androidCategory == CATEGORY_GAME) return AppRole.GAME
        if (matches(signals, AUTH_TOKENS)) return AppRole.AUTH
        if (signals.handlesDial || matches(signals, PHONE_TOKENS)) return AppRole.PHONE
        if (emailish) return AppRole.EMAIL
        if (signals.handlesSms || matches(signals, MESSAGING_TOKENS)) return AppRole.MESSAGING
        if (matches(signals, FINANCE_TOKENS)) return AppRole.FINANCE
        if (signals.androidCategory == CATEGORY_MAPS || matches(signals, DELIVERY_TOKENS)) {
            return AppRole.DELIVERY
        }
        if (signals.handlesCalendar || matches(signals, TIME_TOKENS)) return AppRole.TIME
        if (matches(signals, SHOPPING_TOKENS)) return AppRole.SHOPPING
        if (signals.androidCategory == CATEGORY_SOCIAL || matches(signals, SOCIAL_TOKENS)) {
            return AppRole.SOCIAL
        }
        if (matches(signals, GAME_TOKENS)) return AppRole.GAME
        return AppRole.OTHER
    }

    fun defaultDeliveryMode(
        role: AppRole,
        isSystemApp: Boolean,
        hasLauncherActivity: Boolean,
    ): DeliveryMode {
        if (role.defaultsToInstant) return DeliveryMode.INSTANT
        return if (!isSystemApp && hasLauncherActivity) DeliveryMode.BATCH else DeliveryMode.INSTANT
    }

    internal fun matches(signals: AppSignals, tokens: List<String>): Boolean {
        return matches(signals.packageName, signals.label, tokens)
    }

    internal fun matches(packageName: String, label: String, tokens: List<String>): Boolean {
        val haystack = (packageName + " " + label).lowercase()
        val words = haystack.split(NON_WORD_REGEX).filter { it.isNotBlank() }
        return tokens.any { token ->
            if (token.contains('.')) {
                haystack.contains(token)
            } else {
                words.any { word -> wordMatchesToken(word, token) }
            }
        }
    }

    /**
     * Short tokens (sms, vpn, otp) must be a whole word so "cu" never claims a
     * random package. Longer tokens match a word start, and length-4+ tokens
     * may appear inside a word so "mail" recognises Gmail and ProtonMail.
     */
    private fun wordMatchesToken(word: String, token: String): Boolean {
        if (word == token) return true
        if (token.length <= 3) return false
        if (word.startsWith(token)) return true
        return token.length >= 4 && word.contains(token)
    }

    private val NON_WORD_REGEX = Regex("""[^a-z0-9]+""")

    private val VPN_TOKENS = listOf(
        "vpn", "wireguard", "openvpn", "nordvpn", "protonvpn", "mullvad",
        "tailscale", "surfshark", "expressvpn",
    )

    private val AUTH_TOKENS = listOf(
        "authenticator", "2fa", "otp", "totp", "authy", "aegis",
    )

    private val PHONE_TOKENS = listOf(
        "phone", "dialer", "calling", "telephony", "telecom",
    )

    private val EMAIL_TOKENS = listOf(
        "mail", "email", "gmail", "outlook", "thunderbird", "protonmail",
        "fastmail", "mailbox", "k9mail",
    )

    /**
     * Messenger product names sit here because Android labels most of them
     * CATEGORY_SOCIAL, the same bucket as Instagram. The tokens describe the
     * role (a messenger), not a three-package allowlist.
     */
    private val MESSAGING_TOKENS = listOf(
        "whatsapp", "telegram", "messenger", "signal", "viber", "wechat",
        "weixin", "kakaotalk", "threema", "discord", "slack", "skype",
        "messages", "messaging", "sms", "mms", "hangouts", "mattermost",
        "element", "session", "imo", "line", "teams",
    )

    private val FINANCE_TOKENS = listOf(
        "bank", "banking", "banco", "banque", "paypal", "venmo", "cashapp",
        "revolut", "monzo", "starling", "wallet", "finance", "brokerage",
        "invest", "trading", "coinbase", "binance", "zelle", "wise",
        "creditunion",
    )

    private val DELIVERY_TOKENS = listOf(
        "uber", "lyft", "doordash", "grubhub", "deliveroo", "instacart",
        "postmates", "delivery", "deliver", "parcel", "shipping", "fedex",
        "usps", "dhl", "waze", "maps", "grab", "careem", "gojek",
        "ubereats", "justeat",
    )

    private val TIME_TOKENS = listOf(
        "calendar", "alarm", "clock", "reminder", "reminders",
    )

    private val SHOPPING_TOKENS = listOf(
        "shop", "shopping", "store", "marketplace", "amazon", "ebay",
        "etsy", "aliexpress", "walmart", "shein", "temu",
    )

    private val SOCIAL_TOKENS = listOf(
        "instagram", "facebook", "tiktok", "snapchat", "reddit", "twitter",
        "pinterest", "linkedin", "threads", "tumblr", "bereal", "weibo",
    )

    private val GAME_TOKENS = listOf(
        "game", "games", "gaming",
    )
}
