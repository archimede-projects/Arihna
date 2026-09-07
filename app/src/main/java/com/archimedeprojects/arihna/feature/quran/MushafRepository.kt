package com.archimedeprojects.arihna.feature.quran

import android.content.Context
import org.json.JSONArray

internal data class MushafSurah(
    val number: Int,
    val nameArabic: String,
    val nameEnglish: String,
    val nameTranslation: String,
    val pageNumber: Int,
    val ayahCount: Int,
    val juzNumber: Int,
)

internal data class MushafPageMeta(
    val page: Int,
    val juz: Int,
    val hizb: Int,
    val quarter: Int,
)

internal object MushafRepository {
    @Volatile private var surahCache: List<MushafSurah>? = null
    @Volatile private var pageMetaCache: List<MushafPageMeta>? = null

    fun surahs(context: Context): List<MushafSurah> {
        surahCache?.let { return it }
        return synchronized(this) {
            surahCache ?: runCatching {
                val raw = context.assets.open("mushaf-surah.json")
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
                val array = JSONArray(raw)
                List(array.length()) { index ->
                    val item = array.getJSONObject(index)
                    MushafSurah(
                        number = item.getInt("number"),
                        nameArabic = item.optString("nameArabic"),
                        nameEnglish = item.optString("nameEnglish"),
                        nameTranslation = item.optString("nameTranslation"),
                        pageNumber = item.optInt("pageNumber", 1).coerceIn(1, 604),
                        ayahCount = item.optInt("ayahCount", 0),
                        juzNumber = item.optInt("juzNumber", 1).coerceIn(1, 30),
                    )
                }.sortedBy { it.number }
            }.getOrElse { emptyList() }.also { surahCache = it }
        }
    }

    fun surahForPage(context: Context, pageIndex: Int): MushafSurah? {
        val pageNumber = pageIndex.coerceIn(0, 603) + 1
        return surahs(context).lastOrNull { it.pageNumber <= pageNumber }
            ?: surahs(context).firstOrNull()
    }

    fun pageMeta(context: Context): List<MushafPageMeta> {
        pageMetaCache?.let { return it }
        return synchronized(this) {
            pageMetaCache ?: buildPageMeta(context).also { pageMetaCache = it }
        }
    }

    fun metaForPage(context: Context, pageIndex: Int): MushafPageMeta =
        pageMeta(context).getOrElse(pageIndex.coerceIn(0, 603)) {
            MushafPageMeta(page = pageIndex.coerceIn(0, 603) + 1, juz = 1, hizb = 1, quarter = 1)
        }

    fun juzStartPages(context: Context): List<Int> = startsFor(context) { it.juz }
    fun hizbStartPages(context: Context): List<Int> = startsFor(context) { it.hizb }

    private fun startsFor(context: Context, selector: (MushafPageMeta) -> Int): List<Int> {
        val pages = pageMeta(context)
        val result = mutableListOf<Int>()
        var previous = Int.MIN_VALUE
        pages.forEachIndexed { index, meta ->
            val value = selector(meta)
            if (value != previous) {
                result += index
                previous = value
            }
        }
        return result
    }

    private fun buildPageMeta(context: Context): List<MushafPageMeta> = runCatching {
        val corpus = QuranCorpus.load(context.applicationContext)
        val positions = corpus.ayahs.mapIndexed { index, ayah -> (ayah.surah to ayah.ayah) to index }.toMap()
        val metadata = context.assets.open("quran/quran-data.js")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val pageStarts = parsePairs(metadata, "Page").take(604)
        val juzStarts = parsePairs(metadata, "Juz").take(30)
        val hizbQuarterStarts = parsePairs(metadata, "HizbQaurter").take(240)
        require(pageStarts.size == 604) { "Quran page metadata incomplete" }
        require(juzStarts.size == 30) { "Quran juz metadata incomplete" }
        require(hizbQuarterStarts.size >= 240) { "Quran hizb metadata incomplete" }

        val juzPositions = juzStarts.mapNotNull(positions::get)
        val quarterPositions = hizbQuarterStarts.mapNotNull(positions::get)
        require(juzPositions.size == 30)
        require(quarterPositions.size >= 240)

        pageStarts.mapIndexed { index, start ->
            val position = positions[start] ?: error("Missing Quran page start $start")
            val juzIndex = juzPositions.indexOfLast { it <= position }.coerceAtLeast(0)
            val quarterIndex = quarterPositions.indexOfLast { it <= position }.coerceAtLeast(0)
            MushafPageMeta(
                page = index + 1,
                juz = (juzIndex + 1).coerceIn(1, 30),
                hizb = (quarterIndex / 4 + 1).coerceIn(1, 60),
                quarter = (quarterIndex % 4 + 1).coerceIn(1, 4),
            )
        }
    }.getOrElse {
        // The pinned metadata is expected to succeed; this fallback keeps the reader usable
        // instead of crashing if a single metadata asset cannot be decoded.
        List(604) { page ->
            val ratio = page / 604.0
            MushafPageMeta(
                page = page + 1,
                juz = (ratio * 30).toInt().coerceIn(0, 29) + 1,
                hizb = (ratio * 60).toInt().coerceIn(0, 59) + 1,
                quarter = 1,
            )
        }
    }

    private fun parsePairs(raw: String, property: String): List<Pair<Int, Int>> {
        val propertyRegex = Regex(
            "QuranData\\.${Regex.escape(property)}\\s*=\\s*\\[(.*?)]\\s*;",
            setOf(RegexOption.DOT_MATCHES_ALL),
        )
        val body = propertyRegex.find(raw)?.groupValues?.get(1).orEmpty()
        return Regex("\\[(\\d+)\\s*,\\s*(\\d+)]")
            .findAll(body)
            .map { match -> match.groupValues[1].toInt() to match.groupValues[2].toInt() }
            .filterNot { it.first == 115 }
            .toList()
    }
}

internal object QuranReadingPrefs {
    private const val PREFS = "arihna_quran_reader"
    private const val KEY_BOOKMARKS = "mushaf_bookmarks_v1"
    private const val KEY_LAST_PAGE = "last_mushaf_page_v1"
    private const val KEY_RECENT = "recent_mushaf_pages_v1"
    private const val KEY_MODE = "quran_reading_mode_v1"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun bookmarkedPages(context: Context): Set<Int> = prefs(context)
        .getStringSet(KEY_BOOKMARKS, emptySet())
        .orEmpty()
        .mapNotNull { it.toIntOrNull() }
        .filter { it in 0..603 }
        .toSet()

    fun isBookmarked(context: Context, pageIndex: Int): Boolean =
        pageIndex.coerceIn(0, 603) in bookmarkedPages(context)

    fun setBookmarked(context: Context, pageIndex: Int, bookmarked: Boolean) {
        val safe = pageIndex.coerceIn(0, 603)
        val set = bookmarkedPages(context).map(Int::toString).toMutableSet()
        if (bookmarked) set += safe.toString() else set -= safe.toString()
        prefs(context).edit().putStringSet(KEY_BOOKMARKS, set).apply()
    }

    fun lastPage(context: Context): Int = prefs(context).getInt(KEY_LAST_PAGE, 0).coerceIn(0, 603)

    fun recordVisitedPage(context: Context, pageIndex: Int) {
        val safe = pageIndex.coerceIn(0, 603)
        val recent = recentPages(context).toMutableList().apply {
            remove(safe)
            add(0, safe)
        }.take(8)
        prefs(context).edit()
            .putInt(KEY_LAST_PAGE, safe)
            .putString(KEY_RECENT, recent.joinToString(","))
            .apply()
    }

    fun recentPages(context: Context): List<Int> = prefs(context)
        .getString(KEY_RECENT, "")
        .orEmpty()
        .split(',')
        .mapNotNull { it.toIntOrNull() }
        .filter { it in 0..603 }
        .distinct()

    fun mode(context: Context): QuranReadingMode = runCatching {
        QuranReadingMode.valueOf(prefs(context).getString(KEY_MODE, QuranReadingMode.HAFS_UTHMANI.name).orEmpty())
    }.getOrDefault(QuranReadingMode.HAFS_UTHMANI)

    fun setMode(context: Context, mode: QuranReadingMode) {
        prefs(context).edit().putString(KEY_MODE, mode.name).apply()
    }
}
