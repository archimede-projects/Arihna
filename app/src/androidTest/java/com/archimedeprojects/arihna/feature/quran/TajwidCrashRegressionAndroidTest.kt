package com.archimedeprojects.arihna.feature.quran

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TajwidCrashRegressionAndroidTest {
    @Test
    fun pinnedHafsCorpusProducesOnlyValidNonOverlappingTajwidSpans() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val corpus = QuranCorpus.load(context)
        assertEquals(6236, corpus.ayahs.size)

        corpus.ayahs.forEach { ayah ->
            val spans = UthmaniTajwidEngine.find(ayah.text)
            spans.forEach { span ->
                assertTrue("${ayah.surah}:${ayah.ayah} start=${span.start}", span.start >= 0)
                assertTrue(
                    "${ayah.surah}:${ayah.ayah} end=${span.endExclusive} length=${ayah.text.length}",
                    span.endExclusive <= ayah.text.length,
                )
                assertTrue("${ayah.surah}:${ayah.ayah} empty span", span.endExclusive > span.start)
            }
            spans.zipWithNext().forEach { (first, second) ->
                assertTrue(
                    "${ayah.surah}:${ayah.ayah} overlap $first / $second",
                    first.endExclusive <= second.start,
                )
            }
        }
    }
}
