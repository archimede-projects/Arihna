package com.archimedeprojects.arihna.feature.quran

import android.content.Context

data class QuranAyah(
    val surah: Int,
    val ayah: Int,
    val text: String,
)

data class QuranBoundary(
    val number: Int,
    val surah: Int,
    val ayah: Int,
)

data class QuranCorpus(
    val ayahs: List<QuranAyah>,
    val juzBoundaries: List<QuranBoundary>,
    val hizbBoundaries: List<QuranBoundary>,
) {
    val surahNumbers: List<Int> = ayahs.map { it.surah }.distinct()

    fun ayahsForSurah(surah: Int): List<QuranAyah> = ayahs.filter { it.surah == surah }

    fun juzAt(surah: Int, ayah: Int): Int? =
        juzBoundaries.firstOrNull { it.surah == surah && it.ayah == ayah }?.number

    fun hizbAt(surah: Int, ayah: Int): Int? =
        hizbBoundaries.firstOrNull { it.surah == surah && it.ayah == ayah }?.number

    companion object {
        private val boundaryRegex = Regex(
            "\\\"(\\d+)\\\"\\s*:\\s*\\{\\s*\\\"surahNum\\\"\\s*:\\s*(\\d+)\\s*,\\s*\\\"ayahNum\\\"\\s*:\\s*(\\d+)",
        )

        fun parse(quranText: String, juzJson: String, hizbJson: String): QuranCorpus {
            val ayahs = quranText.lineSequence().mapNotNull { line ->
                if (line.isBlank() || line.startsWith('#')) return@mapNotNull null
                val parts = line.split('|', limit = 3)
                if (parts.size != 3) return@mapNotNull null
                val surah = parts[0].toIntOrNull() ?: return@mapNotNull null
                val ayah = parts[1].toIntOrNull() ?: return@mapNotNull null
                QuranAyah(surah = surah, ayah = ayah, text = parts[2])
            }.toList()
            return QuranCorpus(
                ayahs = ayahs,
                juzBoundaries = parseBoundaries(juzJson),
                hizbBoundaries = parseBoundaries(hizbJson),
            )
        }

        private fun parseBoundaries(json: String): List<QuranBoundary> =
            boundaryRegex.findAll(json).map { match ->
                QuranBoundary(
                    number = match.groupValues[1].toInt(),
                    surah = match.groupValues[2].toInt(),
                    ayah = match.groupValues[3].toInt(),
                )
            }.toList()

        fun load(context: Context): QuranCorpus {
            fun asset(name: String): String = context.assets.open("quran/$name")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            return parse(
                quranText = asset("quran-uthmani.txt"),
                juzJson = asset("juz-info.json"),
                hizbJson = asset("hizb-info.json"),
            )
        }
    }
}
