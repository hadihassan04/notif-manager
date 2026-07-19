package com.notifmanager.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationCaptureFilterTest {
    @Test
    fun ignoresCommonDeviceStatusNoise() {
        assertTrue(NotificationCaptureFilter.isKnownNoise("android", "Flashlight on", null))
        assertTrue(NotificationCaptureFilter.isKnownNoise("com.backup", "Backing up", "7%"))
        assertTrue(NotificationCaptureFilter.isKnownNoise("com.files", "Downloading", "42%"))
        assertTrue(NotificationCaptureFilter.isKnownNoise("com.files", null, "Progress 8%"))
    }

    @Test
    fun keepsOrdinaryMessagesWithSimilarWords() {
        assertFalse(NotificationCaptureFilter.isKnownNoise("com.chat", "Status", "I am updating the doc now"))
        assertFalse(NotificationCaptureFilter.isKnownNoise("com.chat", "Sale", "Save 20% today"))
        assertFalse(NotificationCaptureFilter.isKnownNoise("com.chat", "File", "Download the report when you can"))
    }

    @Test
    fun ignoresWhatsappMessagePollingNoise() {
        assertTrue(
            NotificationCaptureFilter.isKnownNoise(
                "com.whatsapp",
                "WhatsApp",
                "Checking for new messages",
            ),
        )
    }
}
