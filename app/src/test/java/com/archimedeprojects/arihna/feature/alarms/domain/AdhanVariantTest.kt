package com.archimedeprojects.arihna.feature.alarms.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AdhanVariantTest {
    @Test
    fun catalogueContainsSevenStableDistinctVariants() {
        val values = AdhanVariant.entries.map { it.storageValue }
        assertEquals(7, values.size)
        assertEquals(values.size, values.toSet().size)
        assertNotEquals(AdhanVariant.CLASSIC.storageValue, AdhanVariant.BEAUTIFUL.storageValue)
    }

    @Test
    fun establishedStorageIdsStayMigrationSafe() {
        assertEquals("arihna://adhan/classic", AdhanVariant.CLASSIC.storageValue)
        assertEquals("arihna://adhan/beautiful", AdhanVariant.BEAUTIFUL.storageValue)
        assertEquals("arihna://adhan/short", AdhanVariant.SHORT.storageValue)
        assertEquals("arihna://adhan/takbir-x2", AdhanVariant.TAKBIR_X2.storageValue)
    }

    @Test
    fun legacyAndUnknownValuesFallBackToClassic() {
        assertEquals(AdhanVariant.CLASSIC, AdhanVariant.fromStorage(null))
        assertEquals(AdhanVariant.CLASSIC, AdhanVariant.fromStorage("content://legacy"))
    }

    @Test
    fun storedVariantRoundTrips() {
        AdhanVariant.entries.forEach { variant ->
            assertEquals(variant, AdhanVariant.fromStorage(variant.storageValue))
        }
    }
}
