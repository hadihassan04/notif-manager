package com.tide.app.core

import com.tide.app.data.DeliveryMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppClassifierTest {
    @Test
    fun messengersDefaultInstantByRoleNotPackageAllowlist() {
        assertRole(AppRole.MESSAGING, "com.whatsapp", "WhatsApp")
        assertRole(AppRole.MESSAGING, "com.whatsapp.w4b", "WhatsApp Business")
        assertRole(AppRole.MESSAGING, "org.telegram.messenger", "Telegram")
        assertRole(AppRole.MESSAGING, "com.facebook.orca", "Messenger")
        assertRole(AppRole.MESSAGING, "org.thoughtcrime.securesms", "Signal")
        assertRole(AppRole.MESSAGING, "com.google.android.apps.messaging", "Messages")
        listOf(
            "com.whatsapp" to "WhatsApp",
            "org.telegram.messenger" to "Telegram",
            "com.facebook.orca" to "Messenger",
        ).forEach { (pkg, label) ->
            val role = classify(pkg, label, androidCategory = AppClassifier.CATEGORY_SOCIAL)
            assertEquals(label, AppRole.MESSAGING, role)
            assertTrue(label, role.defaultsToInstant)
            assertEquals(AppSelectionGroup.TIME_SENSITIVE, role.selectionGroup)
        }
    }

    @Test
    fun smsHandlerIsMessagingEvenWithoutAMessengerName() {
        val role = AppClassifier.classify(
            AppSignals(
                packageName = "com.carrier.chat",
                label = "MyZong",
                handlesSms = true,
                androidCategory = AppClassifier.CATEGORY_SOCIAL,
            ),
        )
        assertEquals(AppRole.MESSAGING, role)
    }

    @Test
    fun socialFeedsAndGamesDefaultBatch() {
        assertRole(AppRole.SOCIAL, "com.instagram.android", "Instagram")
        assertRole(AppRole.SOCIAL, "com.facebook.katana", "Facebook")
        assertRole(AppRole.SOCIAL, "com.zhiliaoapp.musically", "TikTok")
        assertRole(
            AppRole.GAME,
            "com.mojang.minecraftpe",
            "Minecraft",
            androidCategory = AppClassifier.CATEGORY_GAME,
        )
        assertFalse(classify("com.instagram.android", "Instagram").defaultsToInstant)
        assertFalse(classify("com.mojang.minecraftpe", "Minecraft", androidCategory = AppClassifier.CATEGORY_GAME).defaultsToInstant)
    }

    @Test
    fun mediaPlayersDefaultInstantAndStayInAlwaysInstantWhenTurnedOff() {
        val netflix = classify("com.netflix.mediaclient", "Netflix", isMediaPlayer = true)
        assertEquals(AppRole.MEDIA, netflix)
        assertTrue(netflix.defaultsToInstant)
        assertEquals(AppSelectionGroup.ALWAYS_INSTANT, netflix.selectionGroup)
        assertEquals(AppSelectionGroup.ALWAYS_INSTANT, pickerSelectionGroup(netflix, instant = true))
        assertEquals(AppSelectionGroup.ALWAYS_INSTANT, pickerSelectionGroup(netflix, instant = false))
        assertEquals(
            AppRole.MEDIA,
            classify("com.spotify.music", "Spotify", androidCategory = AppClassifier.CATEGORY_AUDIO),
        )
    }

    @Test
    fun pickerHidesEnabledAppsFromRecommended() {
        assertEquals(
            AppSelectionGroup.INSTANT,
            pickerSelectionGroup(AppRole.MESSAGING, instant = true),
        )
        assertEquals(
            AppSelectionGroup.TIME_SENSITIVE,
            pickerSelectionGroup(AppRole.MESSAGING, instant = false),
        )
        assertEquals(
            AppSelectionGroup.INSTANT,
            pickerSelectionGroup(AppRole.SOCIAL, instant = true),
        )
        assertEquals(
            AppSelectionGroup.EVERYTHING_ELSE,
            pickerSelectionGroup(AppRole.SOCIAL, instant = false),
        )
    }

    @Test
    fun timeTiedRolesDefaultInstant() {
        assertRole(AppRole.EMAIL, "com.google.android.gm", "Gmail")
        assertRole(AppRole.EMAIL, "ch.protonmail.android", "Proton Mail")
        assertRole(AppRole.PHONE, "com.google.android.dialer", "Phone")
        assertRole(AppRole.FINANCE, "com.meezan.bank", "Meezan Bank")
        assertRole(AppRole.DELIVERY, "com.ubercab", "Uber")
        assertRole(AppRole.TIME, "com.google.android.calendar", "Calendar")
        assertRole(AppRole.AUTH, "com.google.android.apps.authenticator2", "Google Authenticator")
        assertTrue(classify("com.meezan.bank", "Meezan Bank").defaultsToInstant)
        assertTrue(classify("com.google.android.apps.authenticator2", "Authenticator").defaultsToInstant)
    }

    @Test
    fun protonVpnIsNotEmail() {
        assertEquals(AppRole.OTHER, classify("ch.protonvpn.android", "Proton VPN"))
        assertFalse(classify("ch.protonvpn.android", "Proton VPN").defaultsToInstant)
        assertEquals(AppRole.EMAIL, classify("ch.protonmail.android", "Proton Mail"))
    }

    @Test
    fun shoppingDefaultsBatch() {
        assertRole(AppRole.SHOPPING, "com.amazon.mShop.android.shopping", "Amazon Shopping")
        assertFalse(classify("com.amazon.mShop.android.shopping", "Amazon Shopping").defaultsToInstant)
        assertEquals(AppSelectionGroup.EVERYTHING_ELSE, classify("com.amazon.mShop.android.shopping", "Amazon Shopping").selectionGroup)
    }

    @Test
    fun shortTokensDoNotClaimUnrelatedWords() {
        assertEquals(AppRole.OTHER, classify("com.rallyhealth.app", "Rally Health"))
        assertEquals(AppRole.FINANCE, classify("com.ally.bank", "Ally Bank"))
        assertEquals(AppRole.OTHER, classify("com.example.cube", "Cube"))
    }

    @Test
    fun defaultDeliveryModeKeepsSystemSourcesInstant() {
        assertEquals(
            DeliveryMode.INSTANT,
            AppClassifier.defaultDeliveryMode(AppRole.MESSAGING, isSystemApp = false, hasLauncherActivity = true),
        )
        assertEquals(
            DeliveryMode.BATCH,
            AppClassifier.defaultDeliveryMode(AppRole.SOCIAL, isSystemApp = false, hasLauncherActivity = true),
        )
        assertEquals(
            DeliveryMode.INSTANT,
            AppClassifier.defaultDeliveryMode(AppRole.OTHER, isSystemApp = true, hasLauncherActivity = false),
        )
        assertEquals(
            DeliveryMode.INSTANT,
            AppClassifier.defaultDeliveryMode(AppRole.SOCIAL, isSystemApp = true, hasLauncherActivity = true),
        )
        assertEquals(
            DeliveryMode.INSTANT,
            AppClassifier.defaultDeliveryMode(AppRole.OTHER, isSystemApp = true, hasLauncherActivity = true),
        )
    }

    private fun assertRole(
        expected: AppRole,
        packageName: String,
        label: String,
        androidCategory: Int = AppClassifier.CATEGORY_UNDEFINED,
        isMediaPlayer: Boolean = false,
    ) {
        assertEquals(label, expected, classify(packageName, label, androidCategory, isMediaPlayer))
    }

    private fun classify(
        packageName: String,
        label: String,
        androidCategory: Int = AppClassifier.CATEGORY_UNDEFINED,
        isMediaPlayer: Boolean = false,
    ): AppRole {
        return AppClassifier.classify(
            AppSignals(
                packageName = packageName,
                label = label,
                androidCategory = androidCategory,
                isMediaPlayer = isMediaPlayer,
            ),
        )
    }
}
