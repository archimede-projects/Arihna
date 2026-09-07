package com.archimedeprojects.arihna.feature.alarms.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AdhanVariantTest {
    @Test
    fun eachVariantHasStableDistinctStorageValue() {
        val values = AdhanVariant.entries.map { it.storageValue }
        assertEquals(values.size, values.toSet().size)
        assertNotEquals(AdhanVariant.CLASSIC.storageValue, AdhanVariant.BEAUTIFUL.storageValue)
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
