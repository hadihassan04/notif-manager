package com.tide.app.notifications

import org.junit.Assert.assertFalse
import org.junit.Test

class PendingIntentRegistryTest {
    @Test
    fun missingIntentDoesNotPretendToOpenAThread() {
        assertFalse(PendingIntentRegistry.contains("0|com.whatsapp|1|missing"))
        assertFalse(PendingIntentRegistry.send("0|com.whatsapp|1|missing"))
    }

    @Test
    fun cancelForgetsTheCapturedIntent() {
        NotificationStatusController.cancel("0|com.whatsapp|1|gone")
        assertFalse(PendingIntentRegistry.contains("0|com.whatsapp|1|gone"))
    }
}
