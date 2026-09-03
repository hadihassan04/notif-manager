package com.tide.app.notifications

import com.tide.app.data.DeliveryMode
import com.tide.app.data.NotificationEntity
import com.tide.app.data.RuleSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleasedNotificationPlanTest {
    @Test
    fun releaseIsOneCardPerNotificationWithNoTideDigest() {
        val captured = listOf(
            item("wa-sng", "com.whatsapp", "WhatsApp", "SnG: Ahmar Jamal", "Also gpt coming today finally"),
            item("wa-yzv", "com.whatsapp", "WhatsApp", "YZV Çakışan Dersler", "Merhaba arkadaşlar"),
            item("reddit-1", "com.reddit.frontpage", "Reddit", "r/android", "A post"),
            item("reddit-2", "com.reddit.frontpage", "Reddit", "r/kotlin", "Another post"),
            item("signal-1", "org.thoughtcrime.securesms", "Signal", "Sam", "On my way"),
            item("mail-1", "com.google.android.gm", "Gmail", "Invoice", "Your receipt"),
        )
        val intents = setOf("wa-sng", "wa-yzv", "reddit-1", "reddit-2", "signal-1", "mail-1")

        val specs = ReleasedNotificationPlan.specs(captured) { it in intents }

        assertEquals(6, specs.size)
        assertEquals(captured.map { it.notificationKey }, specs.map { it.notificationKey })
        assertTrue(specs.none { it.title.contains("Waiting notifications delivered") })
        assertTrue(specs.none { it.text.orEmpty().contains("notifications were released") })
        assertTrue(specs.none { it.text.orEmpty().contains("Top apps") })
        assertEquals("SnG: Ahmar Jamal", specs[0].title)
        assertEquals("YZV Çakışan Dersler", specs[1].title)
        assertTrue(specs.all { it.tap == ReleasedTap.CAPTURED_INTENT })
    }

    @Test
    fun missingPendingIntentUsesActivityTrampolineAndSaysSo() {
        val item = item("wa-sng", "com.whatsapp", "WhatsApp", "SnG: Ahmar Jamal", "Also gpt coming today finally")

        val spec = ReleasedNotificationPlan.specs(listOf(item)) { false }.single()

        assertEquals(ReleasedTap.TRAMPOLINE, spec.tap)
        assertEquals("SnG: Ahmar Jamal", spec.title)
        assertTrue(spec.text.orEmpty().contains("Also gpt coming today finally"))
        assertTrue(spec.text.orEmpty().contains("Original tap expired. Opens WhatsApp."))
        assertEquals(
            "Original tap expired. Opening WhatsApp.",
            ReleasedNotificationPlan.fallbackMessage(true, "WhatsApp"),
        )
        assertEquals(
            "Original notification action expired.",
            ReleasedNotificationPlan.fallbackMessage(false, "WhatsApp"),
        )
    }

    @Test
    fun capturedIntentKeepsOriginalBodyWithoutFallbackChrome() {
        val item = item("wa-sng", "com.whatsapp", "WhatsApp", "SnG: Ahmar Jamal", "Also gpt coming today finally")

        val spec = ReleasedNotificationPlan.specs(listOf(item)) { true }.single()

        assertEquals(ReleasedTap.CAPTURED_INTENT, spec.tap)
        assertEquals("Also gpt coming today finally", spec.text)
        assertFalse(spec.text.orEmpty().contains("Original tap expired"))
    }

    private fun item(
        key: String,
        packageName: String,
        appLabel: String,
        title: String,
        text: String,
    ): NotificationEntity {
        return NotificationEntity(
            notificationKey = key,
            packageName = packageName,
            appLabel = appLabel,
            title = title,
            text = text,
            channelId = "messages",
            category = "msg",
            postedAtMillis = 1_000,
            batchId = "2026-09-03-batch-1-1020",
            deliveryMode = DeliveryMode.BATCH,
            ruleSource = RuleSource.DEFAULT,
        )
    }
}
