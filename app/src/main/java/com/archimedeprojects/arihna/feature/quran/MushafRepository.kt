package com.archimedeprojects.arihna.feature.quran

import android.content.Context
import org.json.JSONArray

enum class QuranRiwaya { HAFS, WARSH }

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
    private val surahCache = mutableMapOf<QuranRiwaya, List<MushafSurah>>()
    @Volatile private var hafsPageMetaCache: List<MushafPageMeta>? = null
    @Volatile private var hafsPageStartsCache: List<Pair<Int, Int>>? = null

    fun surahs(context: Context, riwaya: QuranRiwaya): List<MushafSurah> {
        synchronized(this) {
            surahCache[riwaya]?.let { return it }
            val asset = when (riwaya) {
                QuranRiwaya.HAFS -> "mushaf-surah.json"
                QuranRiwaya.WARSH -> "mushaf-warsh-surah.json"
            }
            val loaded = runCatching {
                val raw = context.assets.open(asset)
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
            }.getOrElse { emptyList() }
            surahCache[riwaya] = loaded
            return loaded
        }
    }

    fun surahForPage(context: Context, riwaya: QuranRiwaya, pageIndex: Int): MushafSurah? {
        val pageNumber = pageIndex.coerceIn(0, 603) + 1
        val source = surahs(context, riwaya)
        return source.lastOrNull { it.pageNumber <= pageNumber } ?: source.firstOrNull()
    }

    /** Exact Madani/Hafs page starts from the pinned QuranData.Page asset. No synthetic fallback. */
    fun hafsPageStarts(context: Context): List<Pair<Int, Int>> {
        hafsPageStartsCache?.let { return it }
        return synchronized(this) {
            hafsPageStartsCache ?: runCatching {
                val metadata = context.assets.open("quran/quran-data.js")
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
                parsePairs(metadata, "Page").take(604).also { starts ->
                    require(starts.size == 604) { "Quran page metadata incomplete" }
                    require(starts.first() == (1 to 1)) { "Unexpected first Quran page boundary" }
                }
            }.getOrElse { emptyList() }.also { hafsPageStartsCache = it }
        }
    }

    fun hafsPageIndexForAyah(context: Context, surah: Int, ayah: Int): Int? {
        val starts = hafsPageStarts(context)
        if (starts.size != 604) return null
        val index = starts.indexOfLast { start ->
            start.first < surah || (start.first == surah && start.second <= ayah)
        }
        return index.takeIf { it >= 0 }
    }

    /** Hafs/Tanzil boundary metadata is authoritative only for the existing Hafs flow. */
    fun metaForPage(context: Context, riwaya: QuranRiwaya, pageIndex: Int): MushafPageMeta? =
        if (riwaya == QuranRiwaya.HAFS) {
            hafsPageMeta(context).getOrElse(pageIndex.coerceIn(0, 603)) {
                MushafPageMeta(page = pageIndex.coerceIn(0, 603) + 1, juz = 1, hizb = 1, quarter = 1)
            }
        } else {
            null
        }

    fun juzStartPages(context: Context, riwaya: QuranRiwaya): List<Int> =
        if (riwaya == QuranRiwaya.HAFS) startsFor(context) { it.juz } else emptyList()

    fun hizbStartPages(context: Context, riwaya: QuranRiwaya): List<Int> =
        if (riwaya == QuranRiwaya.HAFS) startsFor(context) { it.hizb } else emptyList()

    private fun hafsPageMeta(context: Context): List<MushafPageMeta> {
        hafsPageMetaCache?.let { return it }
        return synchronized(this) {
            hafsPageMetaCache ?: buildHafsPageMeta(context).also { hafsPageMetaCache = it }
        }
    }

    private fun startsFor(context: Context, selector: (MushafPageMeta) -> Int): List<Int> {
        val pages = hafsPageMeta(context)
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

    private fun buildHafsPageMeta(context: Context): List<MushafPageMeta> = runCatching {
        val corpus = QuranCorpus.load(context.applicationContext)
        val positions = corpus.ayahs.mapIndexed { index, ayah -> (ayah.surah to ayah.ayah) to index }.toMap()
        val metadata = context.assets.open("quran/quran-data.js")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val pageStarts = hafsPageStarts(context)
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
    private const val LEGACY_BOOKMARKS = "mushaf_bookmarks_v1"
    private const val LEGACY_LAST_PAGE = "last_mushaf_page_v1"
    private const val LEGACY_RECENT = "recent_mushaf_pages_v1"
    private const val KEY_MODE = "quran_reading_mode_v1"
    private const val KEY_VISUAL_STYLE = "quran_visual_style_v1"
    private const val KEY_BOOKMARKS_PREFIX = "mushaf_bookmarks_v2_"
    private const val KEY_LAST_PAGE_PREFIX = "last_mushaf_page_v2_"
    private const val KEY_RECENT_PREFIX = "recent_mushaf_pages_v2_"
    private const val KEY_TAJWID_BOOKMARKS = "tajwid_bookmarked_surahs_v1"
    private const val KEY_TAJWID_LAST_SURAH = "tajwid_last_surah_v1"
    private const val KEY_TAJWID_RECENT = "tajwid_recent_surahs_v1"
    private const val KEY_TAJWID_PAGE_BOOKMARKS = "tajwid_bookmarked_pages_v2"
    private const val KEY_TAJWID_LAST_PAGE = "tajwid_last_page_v2"
    private const val KEY_TAJWID_PAGE_RECENT = "tajwid_recent_pages_v2"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun suffix(riwaya: QuranRiwaya) = riwaya.name.lowercase()
    private fun bookmarksKey(riwaya: QuranRiwaya) = KEY_BOOKMARKS_PREFIX + suffix(riwaya)
    private fun lastPageKey(riwaya: QuranRiwaya) = KEY_LAST_PAGE_PREFIX + suffix(riwaya)
    private fun recentKey(riwaya: QuranRiwaya) = KEY_RECENT_PREFIX + suffix(riwaya)

    fun bookmarkedPages(context: Context, riwaya: QuranRiwaya): Set<Int> {
        val p = prefs(context)
        val key = bookmarksKey(riwaya)
        val raw = when {
            p.contains(key) -> p.getStringSet(key, emptySet()).orEmpty()
            riwaya == QuranRiwaya.HAFS -> p.getStringSet(LEGACY_BOOKMARKS, emptySet()).orEmpty()
            else -> emptySet()
        }
        return raw.mapNotNull { it.toIntOrNull() }.filter { it in 0..603 }.toSet()
    }

    fun isBookmarked(context: Context, riwaya: QuranRiwaya, pageIndex: Int): Boolean =
        pageIndex.coerceIn(0, 603) in bookmarkedPages(context, riwaya)

    fun setBookmarked(context: Context, riwaya: QuranRiwaya, pageIndex: Int, bookmarked: Boolean) {
        val safe = pageIndex.coerceIn(0, 603)
        val set = bookmarkedPages(context, riwaya).map(Int::toString).toMutableSet()
        if (bookmarked) set += safe.toString() else set -= safe.toString()
        prefs(context).edit().putStringSet(bookmarksKey(riwaya), set).apply()
    }

    fun lastPage(context: Context, riwaya: QuranRiwaya): Int {
        val p = prefs(context)
        val key = lastPageKey(riwaya)
        return when {
            p.contains(key) -> p.getInt(key, 0)
            riwaya == QuranRiwaya.HAFS -> p.getInt(LEGACY_LAST_PAGE, 0)
            else -> 0
        }.coerceIn(0, 603)
    }

    fun recordVisitedPage(context: Context, riwaya: QuranRiwaya, pageIndex: Int) {
        val safe = pageIndex.coerceIn(0, 603)
        val recent = recentPages(context, riwaya).toMutableList().apply {
            remove(safe)
            add(0, safe)
        }.take(8)
        prefs(context).edit()
            .putInt(lastPageKey(riwaya), safe)
            .putString(recentKey(riwaya), recent.joinToString(","))
            .apply()
    }

    fun recentPages(context: Context, riwaya: QuranRiwaya): List<Int> {
        val p = prefs(context)
        val key = recentKey(riwaya)
        val raw = when {
            p.contains(key) -> p.getString(key, "")
            riwaya == QuranRiwaya.HAFS -> p.getString(LEGACY_RECENT, "")
            else -> ""
        }.orEmpty()
        return raw.split(',').mapNotNull { it.toIntOrNull() }.filter { it in 0..603 }.distinct()
    }

    fun tajwidBookmarkedSurahs(context: Context): Set<Int> =
        prefs(context).getStringSet(KEY_TAJWID_BOOKMARKS, emptySet()).orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .filter { it in 1..114 }
            .toSet()

    fun isTajwidSurahBookmarked(context: Context, surah: Int): Boolean =
        surah.coerceIn(1, 114) in tajwidBookmarkedSurahs(context)

    fun setTajwidSurahBookmarked(context: Context, surah: Int, bookmarked: Boolean) {
        val safe = surah.coerceIn(1, 114)
        val set = tajwidBookmarkedSurahs(context).map(Int::toString).toMutableSet()
        if (bookmarked) set += safe.toString() else set -= safe.toString()
        prefs(context).edit().putStringSet(KEY_TAJWID_BOOKMARKS, set).apply()
    }

    fun lastTajwidSurah(context: Context): Int =
        prefs(context).getInt(KEY_TAJWID_LAST_SURAH, 1).coerceIn(1, 114)

    fun recentTajwidSurahs(context: Context): List<Int> =
        prefs(context).getString(KEY_TAJWID_RECENT, "").orEmpty()
            .split(',')
            .mapNotNull { it.toIntOrNull() }
            .filter { it in 1..114 }
            .distinct()

    fun recordVisitedTajwidSurah(context: Context, surah: Int) {
        val safe = surah.coerceIn(1, 114)
        val recent = recentTajwidSurahs(context).toMutableList().apply {
            remove(safe)
            add(0, safe)
        }.take(8)
        prefs(context).edit()
            .putInt(KEY_TAJWID_LAST_SURAH, safe)
            .putString(KEY_TAJWID_RECENT, recent.joinToString(","))
            .apply()
    }

    fun tajwidBookmarkedPages(context: Context): Set<Int> =
        prefs(context).getStringSet(KEY_TAJWID_PAGE_BOOKMARKS, emptySet()).orEmpty()
            .mapNotNull { it.toIntOrNull() }
            .filter { it in 0..603 }
            .toSet()

    fun isTajwidPageBookmarked(context: Context, pageIndex: Int): Boolean =
        pageIndex.coerceIn(0, 603) in tajwidBookmarkedPages(context)

    fun setTajwidPageBookmarked(context: Context, pageIndex: Int, bookmarked: Boolean) {
        val safe = pageIndex.coerceIn(0, 603)
        val set = tajwidBookmarkedPages(context).map(Int::toString).toMutableSet()
        if (bookmarked) set += safe.toString() else set -= safe.toString()
        prefs(context).edit().putStringSet(KEY_TAJWID_PAGE_BOOKMARKS, set).apply()
    }

    fun lastTajwidPage(context: Context): Int =
        prefs(context).getInt(KEY_TAJWID_LAST_PAGE, 0).coerceIn(0, 603)

    fun recentTajwidPages(context: Context): List<Int> =
        prefs(context).getString(KEY_TAJWID_PAGE_RECENT, "").orEmpty()
            .split(',')
            .mapNotNull { it.toIntOrNull() }
            .filter { it in 0..603 }
            .distinct()

    fun recordVisitedTajwidPage(context: Context, pageIndex: Int) {
        val safe = pageIndex.coerceIn(0, 603)
        val recent = recentTajwidPages(context).toMutableList().apply {
            remove(safe)
            add(0, safe)
        }.take(8)
        prefs(context).edit()
            .putInt(KEY_TAJWID_LAST_PAGE, safe)
            .putString(KEY_TAJWID_PAGE_RECENT, recent.joinToString(","))
            .apply()
    }

    fun mode(context: Context): QuranReadingMode = runCatching {
        QuranReadingMode.valueOf(prefs(context).getString(KEY_MODE, QuranReadingMode.HAFS_UTHMANI.name).orEmpty())
    }.getOrDefault(QuranReadingMode.HAFS_UTHMANI)

    fun setMode(context: Context, mode: QuranReadingMode) {
        prefs(context).edit().putString(KEY_MODE, mode.name).apply()
    }

    fun visualStyle(context: Context): MushafVisualStyle = runCatching {
        MushafVisualStyle.valueOf(
            prefs(context).getString(KEY_VISUAL_STYLE, MushafVisualStyle.CLASSIC.name).orEmpty(),
        )
    }.getOrDefault(MushafVisualStyle.CLASSIC)

    fun setVisualStyle(context: Context, style: MushafVisualStyle) {
        prefs(context).edit().putString(KEY_VISUAL_STYLE, style.name).apply()
    }
}
