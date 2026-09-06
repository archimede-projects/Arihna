package com.archimedeprojects.arihna.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StartupCapabilityGateTest {
    @Test
    fun missingCapabilitiesFollowStableStartupOrder() {
        val snapshot = StartupCapabilitySnapshot(
            locationReady = false,
            notificationsReady = false,
            exactAlarmReady = false,
            fullScreenReady = false,
            overlayReady = false,
        )

        assertEquals(StartupCapability.LOCATION, nextStartupCapability(snapshot, emptySet()))
        assertEquals(
            StartupCapability.NOTIFICATIONS,
            nextStartupCapability(snapshot, setOf(StartupCapability.LOCATION)),
        )
        assertEquals(
            StartupCapability.EXACT_ALARM,
            nextStartupCapability(
                snapshot,
                setOf(StartupCapability.LOCATION, StartupCapability.NOTIFICATIONS),
            ),
        )
        assertEquals(
            StartupCapability.FULL_SCREEN,
            nextStartupCapability(
                snapshot,
                setOf(
                    StartupCapability.LOCATION,
                    StartupCapability.NOTIFICATIONS,
                    StartupCapability.EXACT_ALARM,
                ),
            ),
        )
        assertEquals(
            StartupCapability.OVERLAY,
            nextStartupCapability(
                snapshot,
                setOf(
                    StartupCapability.LOCATION,
                    StartupCapability.NOTIFICATIONS,
                    StartupCapability.EXACT_ALARM,
                    StartupCapability.FULL_SCREEN,
                ),
            ),
        )
    }

    @Test
    fun readyAndDismissedCapabilitiesAreSkipped() {
        val snapshot = StartupCapabilitySnapshot(
            locationReady = true,
            notificationsReady = true,
            exactAlarmReady = true,
            fullScreenReady = false,
            overlayReady = false,
        )

        assertEquals(StartupCapability.FULL_SCREEN, nextStartupCapability(snapshot, emptySet()))
        assertEquals(
            StartupCapability.OVERLAY,
            nextStartupCapability(snapshot, setOf(StartupCapability.FULL_SCREEN)),
        )
        assertNull(
            nextStartupCapability(
                snapshot.copy(fullScreenReady = true, overlayReady = true),
                emptySet(),
            ),
        )
    }
}
