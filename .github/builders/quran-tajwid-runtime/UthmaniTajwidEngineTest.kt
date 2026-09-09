package com.archimedeprojects.arihna.feature.quran

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UthmaniTajwidEngineTest {
    @Test
    fun detectsMainRulesOnUthmaniFixtures() {
        assertRule("يَجْعَلْ", TajwidRule.QALQALAH)
        assertRule("مِنۡ شَرٍّ", TajwidRule.IKHFA)
        assertRule("مِنۡ بَعْدِ", TajwidRule.IQLAB)
        assertRule("مِنۡ وَالٍ", TajwidRule.IDGHAM_WITH_GHUNNAH)
        assertRule("مِنۡ رَّبِّهِمْ", TajwidRule.IDGHAM_WITHOUT_GHUNNAH)
        assertRule("إِنَّ", TajwidRule.GHUNNAH)
    }

    @Test
    fun negativeFixtureDoesNotInventRule() {
        val spans = UthmaniTajwidEngine.find("مِنۡ أَهْلِ")
        assertTrue(spans.isEmpty())
    }

    @Test
    fun outputIsDeterministicValidAndNeverMutatesText() {
        val text = "مِنۡ شَرِّ مَا خَلَقَ"
        val first = UthmaniTajwidEngine.find(text)
        val second = UthmaniTajwidEngine.find(text)
        assertEquals(first, second)
        assertEquals("مِنۡ شَرِّ مَا خَلَقَ", text)
        first.forEach { span ->
            assertTrue(span.start >= 0)
            assertTrue(span.endExclusive <= text.length)
            assertTrue(span.endExclusive > span.start)
        }
        first.zipWithNext().forEach { (a, b) -> assertTrue(a.endExclusive <= b.start) }
    }

    @Test
    fun idghamRequiresAWordBoundary() {
        val spans = UthmaniTajwidEngine.find("دُنۡيَا")
        assertTrue(spans.none { it.rule == TajwidRule.IDGHAM_WITH_GHUNNAH })
    }

    private fun assertRule(text: String, rule: TajwidRule) {
        assertTrue("Expected $rule in $text", UthmaniTajwidEngine.find(text).any { it.rule == rule })
    }
}
