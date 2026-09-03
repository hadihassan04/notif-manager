package com.tide.app.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleasedNotificationIntentsTest {
    @Test
    fun capturedIntentIsTheShadeContentIntentWhenPresent() {
        val captured = "whatsapp-content-intent"
        val trampoline = "tide-trampoline-activity"
        assertEquals(
            captured,
            ReleasedNotificationIntents.shadeContentIntent(captured, trampoline),
        )
    }

    @Test
    fun missingCapturedIntentUsesActivityTrampolineNotABroadcast() {
        assertEquals(
            "tide-trampoline-activity",
            ReleasedNotificationIntents.shadeContentIntent(null, "tide-trampoline-activity"),
        )
    }

    @Test
    fun sendOptionsComeFromAnActivityOnApi34NotAReceiver() {
        assertTrue(NotificationOpener.usesActivitySendOptions(isActivityContext = true, sdkInt = 34))
        assertTrue(NotificationOpener.usesActivitySendOptions(isActivityContext = true, sdkInt = 36))
        assertFalse(NotificationOpener.usesActivitySendOptions(isActivityContext = false, sdkInt = 34))
        assertFalse(NotificationOpener.usesActivitySendOptions(isActivityContext = true, sdkInt = 33))
    }
}

class CapturedContentIntentsTest {
    @Test
    fun prefersContentIntentOverPublicVersion() {
        assertEquals("content", CapturedContentIntents.prefer("content", "public"))
    }

    @Test
    fun usesPublicVersionWhenContentIntentIsNull() {
        assertEquals("public", CapturedContentIntents.prefer(null, "public"))
    }

    @Test
    fun staysNullWhenNeitherIntentExists() {
        assertNull(CapturedContentIntents.prefer(null, null))
    }
}
