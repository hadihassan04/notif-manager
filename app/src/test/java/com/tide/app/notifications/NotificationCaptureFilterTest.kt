package com.tide.app.notifications

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
    fun ignoresMessagePollingNoiseFromAnyApp() {
        assertTrue(NotificationCaptureFilter.isKnownNoise("com.whatsapp", "WhatsApp", "Checking for new messages"))
        assertTrue(NotificationCaptureFilter.isKnownNoise("com.signal", "Signal", "Waiting for messages"))
        assertTrue(NotificationCaptureFilter.isKnownNoise("com.mail", "Mail", "Waiting for new email"))
    }

    @Test
    fun ignoresLibraryScanNoise() {
        assertTrue(NotificationCaptureFilter.isKnownNoise("com.player", "Scanning for media", null))
        assertTrue(NotificationCaptureFilter.isKnownNoise("com.player", "Music", "Scanning music library"))
        assertTrue(NotificationCaptureFilter.isKnownNoise("com.gallery", "Media scanner", "Working"))
        assertTrue(NotificationCaptureFilter.isKnownNoise("com.drive", "Sync in progress", null))
    }

    @Test
    fun keepsMessagesThatMerelyMentionScanningOrWaiting() {
        assertFalse(NotificationCaptureFilter.isKnownNoise("com.chat", "Ana", "Waiting for you outside"))
        assertFalse(NotificationCaptureFilter.isKnownNoise("com.chat", "Sam", "Can you scan the receipt?"))
        assertFalse(NotificationCaptureFilter.isKnownNoise("com.chat", "Lee", "I am scanning for a new flat"))
    }
}
