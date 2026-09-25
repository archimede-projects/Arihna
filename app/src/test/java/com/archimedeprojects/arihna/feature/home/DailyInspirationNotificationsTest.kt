package com.archimedeprojects.arihna.feature.home

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyInspirationNotificationsTest {
    @Test
    fun defaultsAreOptInOffAtEightLocal() {
        val settings = DailyInspirationNotificationSettings()
        assertFalse(settings.enabled)
        assertEquals(LocalTime.of(8, 0), settings.deliveryTime)
    }

    @Test
    fun nextTriggerUsesTodayBeforeTimeAndTomorrowAfterTime() {
        val zone = ZoneId.of("Europe/Rome")
        val before = ZonedDateTime.of(2026, 9, 25, 7, 30, 0, 0, zone)
        val after = ZonedDateTime.of(2026, 9, 25, 8, 30, 0, 0, zone)

        assertEquals(
            ZonedDateTime.of(2026, 9, 25, 8, 0, 0, 0, zone),
            nextDailyInspirationTrigger(before, LocalTime.of(8, 0)),
        )
        assertEquals(
            ZonedDateTime.of(2026, 9, 26, 8, 0, 0, 0, zone),
            nextDailyInspirationTrigger(after, LocalTime.of(8, 0)),
        )
    }

    @Test
    fun sameDateIsNeverDeliveredTwice() {
        val date = LocalDate.of(2026, 9, 25)
        assertTrue(
            shouldDeliverDailyInspiration(
                DailyInspirationNotificationSettings(enabled = true),
                date,
            ),
        )
        assertFalse(
            shouldDeliverDailyInspiration(
                DailyInspirationNotificationSettings(
                    enabled = true,
                    lastDeliveredDate = date.toString(),
                ),
                date,
            ),
        )
        assertFalse(
            shouldDeliverDailyInspiration(
                DailyInspirationNotificationSettings(enabled = false),
                date,
            ),
        )
    }

    @Test
    fun notificationPayloadUsesExactDailyHomeSelection() {
        val date = LocalDate.of(2026, 9, 25)
        val home = dailyInspirationFor(date)
        val payload = dailyInspirationNotificationPayloadFor(date)

        assertEquals(home.text, payload.arabic)
        assertEquals(home.translationItalian.trim(), payload.translationItalian)
        assertEquals(home.reference, payload.reference)
    }

    @Test
    fun notificationPermissionIsRequiredOnlyOnApi33Plus() {
        assertTrue(isDailyInspirationNotificationPermissionGranted(32, false))
        assertFalse(isDailyInspirationNotificationPermissionGranted(33, false))
        assertTrue(isDailyInspirationNotificationPermissionGranted(36, true))
    }
}
