package com.archimedeprojects.arihna.feature.quran

import org.junit.Test
import org.junit.Assert.assertEquals

class QuranCorpusTest {
    @Test
    fun parsesVerbatimTextAndJuzHizbBoundaries() {
        val corpus = QuranCorpus.parse(
            quranText = "1|1|نص أول\n2|142|نص ثان\n",
            juzJson = """{"1":{"surahNum":1,"ayahNum":1},"2":{"surahNum":2,"ayahNum":142}}""",
            hizbJson = """{"1":{"surahNum":1,"ayahNum":1},"3":{"surahNum":2,"ayahNum":142}}""",
        )
        assertEquals("نص أول", corpus.ayahs.first().text)
        assertEquals(2, corpus.juzAt(2, 142))
        assertEquals(3, corpus.hizbAt(2, 142))
    }
}
