from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected 1 match, found {count}")
    return text.replace(old, new, 1)

root = Path('.')

# --- MushafRepository: make metadata and reader state riwaya-aware. ---
repo = root / 'app/src/main/java/com/archimedeprojects/arihna/feature/quran/MushafRepository.kt'
repo.write_text(r'''package com.archimedeprojects.arihna.feature.quran

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
''', encoding='utf-8')

# --- Native page renderer: select separate Hafs/Warsh asset trees and cache keys. ---
native = root / 'app/src/main/java/com/archimedeprojects/arihna/feature/quran/NativeMushafPage.kt'
native.write_text(r'''package com.archimedeprojects.arihna.feature.quran

import android.content.Context
import android.graphics.Picture
import android.graphics.drawable.PictureDrawable
import android.util.LruCache
import android.view.View
import android.widget.ImageView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.caverock.androidsvg.SVG
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object MushafPictureCache {
    private val pictures = LruCache<String, Picture>(12)

    fun load(context: Context, riwaya: QuranRiwaya, pageName: String): Picture? {
        val cacheKey = "${riwaya.name}:$pageName"
        pictures.get(cacheKey)?.let { return it }
        val folder = if (riwaya == QuranRiwaya.WARSH) "mushaf-warsh" else "mushaf"
        val picture = runCatching {
            context.assets.open("$folder/$pageName.svg").use { input ->
                SVG.getFromInputStream(input).renderToPicture()
            }
        }.getOrNull() ?: return null
        pictures.put(cacheKey, picture)
        return picture
    }
}

/** Renders one pinned Muṣḥaf page for the explicitly selected riwāya. */
@Composable
internal fun NativeMushafPage(
    page: Int,
    riwaya: QuranRiwaya,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val pageName = String.format(Locale.US, "%03d", page.coerceIn(1, 604))
    val picture by produceState<Picture?>(initialValue = null, pageName, riwaya) {
        value = withContext(Dispatchers.IO) {
            MushafPictureCache.load(context.applicationContext, riwaya, pageName)
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val ready = picture
        if (ready == null) {
            CircularProgressIndicator()
        } else {
            AndroidView(
                factory = { ctx ->
                    ImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        adjustViewBounds = true
                        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                update = { image -> image.setImageDrawable(PictureDrawable(ready)) },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
internal fun MushafPageLoadError(page: Int) {
    Text(text = "Pagina Muṣḥaf $page non disponibile", color = Color.Gray, fontSize = 12.sp)
}
''', encoding='utf-8')

# --- Build assets: keep existing Hafs source and pin/download only Warsh KFQC numeric SVGs. ---
build = root / 'app/build.gradle.kts'
t = build.read_text(encoding='utf-8')
t = replace_once(t,
    'val quranSvgCommit = "78d97544bfdc57e9f04bc97ace3f857ed972d772"\n',
    'val quranSvgCommit = "78d97544bfdc57e9f04bc97ace3f857ed972d772"\nval quranWarshSvgCommit = "b91d39e1065b57bdda3e94aca8ecf3575e50e1e6"\n',
    'warsh commit pin')
needle = '''        check(licenseTarget.isFile && licenseTarget.length() > 0L) { "Missing pinned Mushaf MIT license" }\n'''
addition = needle + r'''

        // Warsh ʿan Nāfiʿ: Quranpedia metadata is CC0; the page artwork is the
        // King Fahd Complex digital Muṣḥaf and is NOT covered by the Hafs MIT licence.
        val warshDir = root.resolve("mushaf-warsh")
        if (warshDir.exists()) warshDir.deleteRecursively()
        warshDir.mkdirs()
        val warshSurahTarget = root.resolve("mushaf-warsh-surah.json")
        val warshNoticeTarget = root.resolve("WARSH_KFQC_NOTICE.md")
        warshSurahTarget.delete()
        warshNoticeTarget.delete()
        val warshCheckout = layout.buildDirectory.dir("tmp/quranpedia-warsh").get().asFile
        if (warshCheckout.exists()) warshCheckout.deleteRecursively()

        fun git(vararg args: String) {
            val command = mutableListOf("git")
            command.addAll(args)
            val process = ProcessBuilder(command).inheritIO().start()
            check(process.waitFor() == 0) { "git command failed: ${command.joinToString(" ")}" }
        }
        git(
            "clone", "--filter=blob:none", "--no-checkout",
            "https://github.com/quranpedia/quran-svg.git", warshCheckout.absolutePath,
        )
        git("-C", warshCheckout.absolutePath, "sparse-checkout", "init", "--no-cone")
        git(
            "-C", warshCheckout.absolutePath, "sparse-checkout", "set", "--no-cone",
            "mushafs/warsh/kfqc/svg/[0-9][0-9][0-9].svg",
            "mushafs/warsh/kfqc/json/surah.json",
            "NOTICE.md",
        )
        git("-C", warshCheckout.absolutePath, "fetch", "--depth=1", "origin", quranWarshSvgCommit)
        git("-C", warshCheckout.absolutePath, "checkout", "--detach", quranWarshSvgCommit)

        val warshSvgSource = warshCheckout.resolve("mushafs/warsh/kfqc/svg")
        warshSvgSource.listFiles { file -> file.isFile && Regex("[0-9]{3}\\.svg").matches(file.name) }
            .orEmpty()
            .forEach { file -> file.copyTo(warshDir.resolve(file.name), overwrite = true) }
        warshCheckout.resolve("mushafs/warsh/kfqc/json/surah.json")
            .copyTo(warshSurahTarget, overwrite = true)
        warshCheckout.resolve("NOTICE.md").copyTo(warshNoticeTarget, overwrite = true)
        warshCheckout.deleteRecursively()

        val warshPages = warshDir.listFiles { file ->
            file.isFile && Regex("[0-9]{3}\\.svg").matches(file.name)
        }?.size ?: 0
        check(warshPages == 604) { "Expected 604 pinned Warsh SVG pages, found $warshPages" }
        val warshSurahRaw = warshSurahTarget.readText(Charsets.UTF_8)
        check(Regex("\\\"number\\\"\\s*:").findAll(warshSurahRaw).count() == 114) {
            "Expected 114 Warsh surahs"
        }
        val warshAyahCount = Regex("\\\"ayahCount\\\"\\s*:\\s*(\\d+)")
            .findAll(warshSurahRaw).sumOf { it.groupValues[1].toInt() }
        check(warshAyahCount == 6214) { "Expected 6214 Warsh ayat, found $warshAyahCount" }
        val notice = warshNoticeTarget.readText(Charsets.UTF_8)
        check("King Fahd" in notice && "digital publishing" in notice && "CC0 1.0" in notice) {
            "Warsh KFQC/Quranpedia usage notice is incomplete"
        }
'''
t = replace_once(t, needle, addition, 'warsh asset task')
build.write_text(t, encoding='utf-8')

# --- Theme: explicitly allow edge-to-edge short-edge cutout layout. ---
themes = root / 'app/src/main/res/values/themes.xml'
t = themes.read_text(encoding='utf-8')
t = replace_once(t,
    '        <item name="android:windowLightNavigationBar">true</item>\n',
    '        <item name="android:windowLightNavigationBar">true</item>\n        <item name="android:windowLayoutInDisplayCutoutMode">shortEdges</item>\n',
    'cutout theme')
themes.write_text(t, encoding='utf-8')

# --- Quran screen: multi-riwaya UI/state + toolbar z-order + cutout-safe immersive top bar. ---
screen = root / 'app/src/main/java/com/archimedeprojects/arihna/feature/quran/QuranPlaceholderScreen.kt'
t = screen.read_text(encoding='utf-8')
t = replace_once(t,
    'import androidx.compose.foundation.layout.PaddingValues\n',
    'import androidx.compose.foundation.layout.PaddingValues\nimport androidx.compose.foundation.layout.WindowInsets\nimport androidx.compose.foundation.layout.displayCutout\nimport androidx.compose.foundation.layout.statusBars\nimport androidx.compose.foundation.layout.union\nimport androidx.compose.foundation.layout.windowInsetsPadding\n',
    'window insets imports')
t = replace_once(t,
    'import androidx.compose.ui.Alignment\n',
    'import androidx.compose.ui.Alignment\nimport androidx.compose.ui.draw.clipToBounds\n',
    'clip import')
t = replace_once(t,
    'import androidx.compose.ui.unit.sp\n',
    'import androidx.compose.ui.unit.sp\nimport androidx.compose.ui.zIndex\n',
    'zindex import')
t = replace_once(t,
    'enum class QuranReadingMode { EASY, HAFS_UTHMANI }\n',
    '''enum class QuranReadingMode { EASY, HAFS_UTHMANI, WARSH_NAFI }\n\ninternal fun QuranReadingMode.riwayaOrNull(): QuranRiwaya? = when (this) {\n    QuranReadingMode.HAFS_UTHMANI -> QuranRiwaya.HAFS\n    QuranReadingMode.WARSH_NAFI -> QuranRiwaya.WARSH\n    QuranReadingMode.EASY -> null\n}\n''',
    'mode enum')
old_state = '''    val corpus = remember(context) { QuranCorpus.load(context.applicationContext) }\n    val surahs = remember(context) { MushafRepository.surahs(context.applicationContext) }\n    var mode by remember { mutableStateOf(QuranReadingPrefs.mode(context)) }\n    var visualStyle by remember { mutableStateOf(QuranReadingPrefs.visualStyle(context)) }\n    var selectedSurah by remember { mutableIntStateOf(1) }\n    var requestedPage by remember { mutableIntStateOf(QuranReadingPrefs.lastPage(context)) }\n'''
new_state = '''    val corpus = remember(context) { QuranCorpus.load(context.applicationContext) }\n    var mode by remember { mutableStateOf(QuranReadingPrefs.mode(context)) }\n    val activeRiwaya = mode.riwayaOrNull() ?: QuranRiwaya.HAFS\n    val surahs = remember(context, activeRiwaya) {\n        MushafRepository.surahs(context.applicationContext, activeRiwaya)\n    }\n    var visualStyle by remember { mutableStateOf(QuranReadingPrefs.visualStyle(context)) }\n    var selectedSurah by remember { mutableIntStateOf(1) }\n    var requestedPage by remember {\n        mutableIntStateOf(QuranReadingPrefs.lastPage(context, activeRiwaya))\n    }\n'''
t = replace_once(t, old_state, new_state, 'top state')
t = replace_once(t,
    '''        FullscreenMushafReader(\n            startPage = requestedPage,\n            style = visualStyle,\n''',
    '''        FullscreenMushafReader(\n            startPage = requestedPage,\n            style = visualStyle,\n            riwaya = activeRiwaya,\n''',
    'fullscreen riwaya')
old_mode_change = '''            onModeChange = { selected ->\n                mode = selected\n                QuranReadingPrefs.setMode(context, selected)\n                explorerOpen = false\n            },\n'''
new_mode_change = '''            onModeChange = { selected ->\n                selected.riwayaOrNull()?.let { riwaya ->\n                    requestedPage = QuranReadingPrefs.lastPage(context, riwaya)\n                    selectedSurah = MushafRepository.surahForPage(context, riwaya, requestedPage)?.number ?: 1\n                }\n                mode = selected\n                QuranReadingPrefs.setMode(context, selected)\n                explorerOpen = false\n            },\n'''
t = replace_once(t, old_mode_change, new_mode_change, 'mode change restore')
t = replace_once(t,
    '''            QuranExplorer(\n                surahs = surahs,\n                currentPage = requestedPage,\n''',
    '''            QuranExplorer(\n                surahs = surahs,\n                currentPage = requestedPage,\n                riwaya = activeRiwaya,\n''',
    'explorer riwaya')
t = replace_once(t,
    '''        } else if (mode == QuranReadingMode.HAFS_UTHMANI) {\n            MushafBookReader(\n                startPage = requestedPage,\n''',
    '''        } else if (mode == QuranReadingMode.HAFS_UTHMANI || mode == QuranReadingMode.WARSH_NAFI) {\n            MushafBookReader(\n                startPage = requestedPage,\n                riwaya = activeRiwaya,\n''',
    'mushaf modes')
t = replace_once(t,
    '                    MushafRepository.surahForPage(context, pageIndex)?.let { selectedSurah = it.number }\n',
    '                    MushafRepository.surahForPage(context, activeRiwaya, pageIndex)?.let { selectedSurah = it.number }\n',
    'normal page change')
# The fullscreen page change has the same legacy call; replace remaining exact occurrence.
t = replace_once(t,
    '                MushafRepository.surahForPage(context, pageIndex)?.let { selectedSurah = it.number }\n',
    '                MushafRepository.surahForPage(context, activeRiwaya, pageIndex)?.let { selectedSurah = it.number }\n',
    'fullscreen page change')
t = replace_once(t, '        QuranAttribution()\n', '        QuranAttribution(mode.riwayaOrNull())\n', 'attribution call')

hafs_chip = '''                FilterChip(\n                    selected = mode == QuranReadingMode.HAFS_UTHMANI,\n                    onClick = { onModeChange(QuranReadingMode.HAFS_UTHMANI) },\n                    label = { Text(appText("Muṣḥaf Ḥafṣ", "مصحف حفص")) },\n                    leadingIcon = { Icon(Icons.Rounded.AutoStories, null, Modifier.size(17.dp)) },\n                    modifier = Modifier.testTag("quran-mode-hafs"),\n                )\n'''
warsh_chip = hafs_chip + '''                FilterChip(\n                    selected = mode == QuranReadingMode.WARSH_NAFI,\n                    onClick = { onModeChange(QuranReadingMode.WARSH_NAFI) },\n                    label = { Text(appText("Muṣḥaf Warsh", "مصحف ورش")) },\n                    leadingIcon = { Icon(Icons.Rounded.AutoStories, null, Modifier.size(17.dp)) },\n                    modifier = Modifier.testTag("quran-mode-warsh"),\n                )\n'''
t = replace_once(t, hafs_chip, warsh_chip, 'warsh chip')

t = replace_once(t,
    '''    private fun MushafBookReader(\n        startPage: Int,\n        style: MushafVisualStyle,\n''',
    '''    private fun MushafBookReader(\n        startPage: Int,\n        riwaya: QuranRiwaya,\n        style: MushafVisualStyle,\n''',
    'book reader signature')
t = replace_once(t,
    '''        val meta = remember(currentPage) { MushafRepository.metaForPage(context, currentPage) }\n        val surah = remember(currentPage) { MushafRepository.surahForPage(context, currentPage) }\n        val bookmarked = remember(currentPage, bookmarkVersion) {\n            QuranReadingPrefs.isBookmarked(context, currentPage)\n        }\n''',
    '''        val meta = remember(currentPage, riwaya) { MushafRepository.metaForPage(context, riwaya, currentPage) }\n        val surah = remember(currentPage, riwaya) { MushafRepository.surahForPage(context, riwaya, currentPage) }\n        val bookmarked = remember(currentPage, bookmarkVersion, riwaya) {\n            QuranReadingPrefs.isBookmarked(context, riwaya, currentPage)\n        }\n''',
    'book reader data')
t = replace_once(t,
    '            QuranReadingPrefs.recordVisitedPage(context, currentPage)\n            onPageChanged(currentPage)\n',
    '            QuranReadingPrefs.recordVisitedPage(context, riwaya, currentPage)\n            onPageChanged(currentPage)\n',
    'book reader visited')
t = replace_once(t,
    '''                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),\n''',
    '''                modifier = Modifier\n                    .fillMaxWidth()\n                    .heightIn(min = 56.dp)\n                    .background(ArihnaDawnTop.copy(alpha = 0.98f))\n                    .zIndex(2f)\n                    .testTag("quran-mushaf-toolbar"),\n''',
    'toolbar z order')
old_meta_text = '''                    Text(\n                        appText(\n                            "pag. ${currentPage + 1} · Juz ${meta.juz} · Hizb ${meta.hizb}",\n                            "صفحة ${toArabicIndic(currentPage + 1)} · الجزء ${toArabicIndic(meta.juz)} · الحزب ${toArabicIndic(meta.hizb)}",\n                        ),\n                        color = ArihnaMutedText,\n                        fontSize = 9.sp,\n                        maxLines = 1,\n                    )\n'''
new_meta_text = '''                    val pageContext = if (meta != null) {\n                        appText(\n                            "pag. ${currentPage + 1} · Juz ${meta.juz} · Hizb ${meta.hizb}",\n                            "صفحة ${toArabicIndic(currentPage + 1)} · الجزء ${toArabicIndic(meta.juz)} · الحزب ${toArabicIndic(meta.hizb)}",\n                        )\n                    } else {\n                        appText("pag. ${currentPage + 1} · Warsh", "صفحة ${toArabicIndic(currentPage + 1)} · ورش")\n                    }\n                    Text(\n                        pageContext,\n                        color = ArihnaMutedText,\n                        fontSize = 9.sp,\n                        maxLines = 1,\n                    )\n'''
t = replace_once(t, old_meta_text, new_meta_text, 'nullable meta text')
t = replace_once(t,
    '                        QuranReadingPrefs.setBookmarked(context, currentPage, !bookmarked)\n',
    '                        QuranReadingPrefs.setBookmarked(context, riwaya, currentPage, !bookmarked)\n',
    'book reader bookmark')
old_pager = '''            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {\n                HorizontalPager(\n                    state = pagerState,\n                    modifier = Modifier.fillMaxWidth().weight(1f).testTag("quran-mushaf-rtl-pager"),\n                    beyondViewportPageCount = 1,\n                    pageSpacing = 2.dp,\n                ) { page ->\n                    Box(\n                        modifier = Modifier\n                            .fillMaxSize()\n                            .padding(horizontal = 2.dp, vertical = 1.dp)\n                            .testTag("quran-mushaf-page-${page + 1}"),\n                        contentAlignment = Alignment.Center,\n                    ) {\n                        PremiumMushafPageFrame(\n                            page = page + 1,\n                            style = style,\n                            modifier = Modifier.fillMaxWidth(),\n                        )\n                    }\n                }\n            }\n'''
new_pager = '''            Box(\n                modifier = Modifier\n                    .fillMaxWidth()\n                    .weight(1f)\n                    .padding(top = 6.dp)\n                    .clipToBounds()\n                    .zIndex(0f)\n                    .testTag("quran-mushaf-preview-container"),\n            ) {\n                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {\n                    HorizontalPager(\n                        state = pagerState,\n                        modifier = Modifier.fillMaxSize().testTag("quran-mushaf-rtl-pager"),\n                        beyondViewportPageCount = 1,\n                        pageSpacing = 2.dp,\n                    ) { page ->\n                        Box(\n                            modifier = Modifier\n                                .fillMaxSize()\n                                .padding(horizontal = 2.dp, vertical = 1.dp)\n                                .testTag("quran-mushaf-page-${page + 1}"),\n                            contentAlignment = Alignment.Center,\n                        ) {\n                            PremiumMushafPageFrame(\n                                page = page + 1,\n                                riwaya = riwaya,\n                                style = style,\n                                modifier = Modifier.fillMaxWidth(),\n                            )\n                        }\n                    }\n                }\n            }\n'''
t = replace_once(t, old_pager, new_pager, 'preview clipping')

t = replace_once(t,
    '''    private fun FullscreenMushafReader(\n        startPage: Int,\n        style: MushafVisualStyle,\n        onDismiss: () -> Unit,\n        onPageChanged: (Int) -> Unit,\n    ) {\n''',
    '''    internal fun FullscreenMushafReader(\n        startPage: Int,\n        style: MushafVisualStyle,\n        riwaya: QuranRiwaya,\n        onDismiss: () -> Unit,\n        onPageChanged: (Int) -> Unit,\n        topBarInsets: WindowInsets = WindowInsets.displayCutout.union(WindowInsets.statusBars),\n    ) {\n''',
    'fullscreen signature')
t = replace_once(t,
    '''        val meta = remember(currentPage) { MushafRepository.metaForPage(context, currentPage) }\n        val surah = remember(currentPage) { MushafRepository.surahForPage(context, currentPage) }\n        val bookmarked = remember(currentPage, bookmarkVersion) {\n            QuranReadingPrefs.isBookmarked(context, currentPage)\n        }\n''',
    '''        val meta = remember(currentPage, riwaya) { MushafRepository.metaForPage(context, riwaya, currentPage) }\n        val surah = remember(currentPage, riwaya) { MushafRepository.surahForPage(context, riwaya, currentPage) }\n        val bookmarked = remember(currentPage, bookmarkVersion, riwaya) {\n            QuranReadingPrefs.isBookmarked(context, riwaya, currentPage)\n        }\n''',
    'fullscreen data')
t = replace_once(t,
    '            QuranReadingPrefs.recordVisitedPage(context, currentPage)\n            onPageChanged(currentPage)\n',
    '            QuranReadingPrefs.recordVisitedPage(context, riwaya, currentPage)\n            onPageChanged(currentPage)\n',
    'fullscreen visited')
t = replace_once(t,
    '''                        ZoomableMushafPage(\n                            page = page + 1,\n                            style = style,\n''',
    '''                        ZoomableMushafPage(\n                            page = page + 1,\n                            riwaya = riwaya,\n                            style = style,\n''',
    'fullscreen zoom riwaya')
start = t.index('                if (chromeVisible) {')
end_marker = '\n            }\n        }\n    }\n\n    @Composable\n    private fun ZoomableMushafPage('
end = t.index(end_marker, start)
chrome = r'''                if (chromeVisible) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .windowInsetsPadding(topBarInsets)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("quran-fullscreen-safe-top"),
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = ArihnaCream.copy(alpha = 0.96f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, ArihnaWarmOutline.copy(alpha = 0.8f)),
                            shadowElevation = 3.dp,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp)
                                    .testTag("quran-fullscreen-chrome"),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.size(52.dp).testTag("quran-fullscreen-close"),
                                ) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = appText("Chiudi lettura", "إغلاق القراءة"),
                                        tint = ArihnaForest,
                                        modifier = Modifier.size(27.dp),
                                    )
                                }
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 6.dp)
                                        .testTag("quran-fullscreen-page-context"),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        surah?.nameArabic ?: appText("Corano", "القرآن"),
                                        color = ArihnaForest,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        appText("Pagina ${currentPage + 1}", "صفحة ${toArabicIndic(currentPage + 1)}"),
                                        color = ArihnaMutedText,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp,
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        QuranReadingPrefs.setBookmarked(context, riwaya, currentPage, !bookmarked)
                                        bookmarkVersion++
                                    },
                                    modifier = Modifier.size(52.dp).testTag("quran-fullscreen-bookmark"),
                                ) {
                                    Icon(
                                        if (bookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                        contentDescription = appText("Segnalibro", "إشارة مرجعية"),
                                        tint = if (bookmarked) ArihnaDawnGold else ArihnaGreen,
                                        modifier = Modifier.size(27.dp),
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .testTag("quran-fullscreen-bottom-context"),
                        color = ArihnaCream.copy(alpha = 0.95f),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, ArihnaWarmOutline.copy(alpha = 0.75f)),
                        shadowElevation = 2.dp,
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                if (meta != null) {
                                    appText(
                                        "Juz ${meta.juz} · Hizb ${meta.hizb}",
                                        "الجزء ${toArabicIndic(meta.juz)} · الحزب ${toArabicIndic(meta.hizb)}",
                                    )
                                } else {
                                    appText("Warsh · Pagina ${currentPage + 1}", "ورش · صفحة ${toArabicIndic(currentPage + 1)}")
                                },
                                color = ArihnaForest,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                            )
                            Text(
                                appText(
                                    "Tocca la pagina per nascondere i controlli · pizzica per zoomare",
                                    "المس الصفحة لإخفاء الأدوات · قرّب بإصبعين للتكبير",
                                ),
                                color = ArihnaMutedText,
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }'''
t = t[:start] + chrome + t[end:]

t = replace_once(t,
    '''    private fun ZoomableMushafPage(\n        page: Int,\n        style: MushafVisualStyle,\n''',
    '''    private fun ZoomableMushafPage(\n        page: Int,\n        riwaya: QuranRiwaya,\n        style: MushafVisualStyle,\n''',
    'zoom signature')
t = replace_once(t,
    '''            PremiumMushafPageFrame(\n                page = page,\n                style = style,\n''',
    '''            PremiumMushafPageFrame(\n                page = page,\n                riwaya = riwaya,\n                style = style,\n''',
    'zoom frame riwaya')
t = replace_once(t,
    '''    private fun PremiumMushafPageFrame(\n        page: Int,\n        style: MushafVisualStyle,\n''',
    '''    private fun PremiumMushafPageFrame(\n        page: Int,\n        riwaya: QuranRiwaya,\n        style: MushafVisualStyle,\n''',
    'frame signature')
t = replace_once(t,
    '                .aspectRatio(MUSHAF_PAGE_ASPECT_RATIO)\n',
    '                .aspectRatio(if (riwaya == QuranRiwaya.WARSH) 345f / 550f else MUSHAF_PAGE_ASPECT_RATIO)\n',
    'warsh aspect ratio')
t = replace_once(t,
    '                NativeMushafPage(page = page, modifier = Modifier.fillMaxSize())\n',
    '                NativeMushafPage(page = page, riwaya = riwaya, modifier = Modifier.fillMaxSize())\n',
    'native riwaya')

t = replace_once(t,
    '''private fun QuranExplorer(\n    surahs: List<MushafSurah>,\n    currentPage: Int,\n    onSelectPage: (Int, Int?) -> Unit,\n''',
    '''private fun QuranExplorer(\n    surahs: List<MushafSurah>,\n    currentPage: Int,\n    riwaya: QuranRiwaya,\n    onSelectPage: (Int, Int?) -> Unit,\n''',
    'explorer signature')
t = replace_once(t,
    '''    val bookmarked = remember(bookmarkVersion, currentPage) { QuranReadingPrefs.bookmarkedPages(context) }\n    val recent = remember(currentPage) { QuranReadingPrefs.recentPages(context) }\n    val juzPages = remember(context) { MushafRepository.juzStartPages(context) }\n    val hizbPages = remember(context) { MushafRepository.hizbStartPages(context) }\n''',
    '''    val bookmarked = remember(bookmarkVersion, currentPage, riwaya) { QuranReadingPrefs.bookmarkedPages(context, riwaya) }\n    val recent = remember(currentPage, riwaya) { QuranReadingPrefs.recentPages(context, riwaya) }\n    val supportsBoundaries = riwaya == QuranRiwaya.HAFS\n    val juzPages = remember(context, riwaya) { MushafRepository.juzStartPages(context, riwaya) }\n    val hizbPages = remember(context, riwaya) { MushafRepository.hizbStartPages(context, riwaya) }\n''',
    'explorer state')
old_chips = '''        Row(\n            modifier = Modifier.fillMaxWidth(),\n            horizontalArrangement = Arrangement.spacedBy(8.dp),\n        ) {\n            ExplorerChip(QuranExplorerView.SURAHS, view, appText("Sure", "السور")) { view = it }\n            ExplorerChip(QuranExplorerView.JUZ, view, appText("Juz", "الأجزاء")) { view = it }\n            ExplorerChip(QuranExplorerView.HIZB, view, appText("Hizb", "الأحزاب")) { view = it }\n        }\n'''
new_chips = '''        Row(\n            modifier = Modifier.fillMaxWidth(),\n            horizontalArrangement = Arrangement.spacedBy(8.dp),\n        ) {\n            ExplorerChip(QuranExplorerView.SURAHS, view, appText("Sure", "السور")) { view = it }\n            if (supportsBoundaries) {\n                ExplorerChip(QuranExplorerView.JUZ, view, appText("Juz", "الأجزاء")) { view = it }\n                ExplorerChip(QuranExplorerView.HIZB, view, appText("Hizb", "الأحزاب")) { view = it }\n            }\n        }\n        if (!supportsBoundaries) {\n            Text(\n                appText(\n                    "Warsh: Juz/Hizb non mostrati finché non è integrato un indice autorevole specifico.",\n                    "ورش: لا نعرض حدود الجزء والحزب حتى يتوفر فهرس موثوق خاص بهذه الرواية.",\n                ),\n                color = ArihnaMutedText,\n                fontSize = 10.sp,\n                modifier = Modifier.testTag("quran-warsh-boundaries-unavailable"),\n            )\n        }\n'''
t = replace_once(t, old_chips, new_chips, 'warsh boundaries chips')
t = replace_once(t,
    '                            QuranReadingPrefs.setBookmarked(context, page, page !in bookmarked)\n',
    '                            QuranReadingPrefs.setBookmarked(context, riwaya, page, page !in bookmarked)\n',
    'explorer bookmark')
t = replace_once(t,
    '                    SavedPageRow(page = page, onClick = { onSelectPage(page, null) })\n',
    '                    SavedPageRow(page = page, riwaya = riwaya, onClick = { onSelectPage(page, null) })\n',
    'bookmark saved row')
t = replace_once(t,
    '                    SavedPageRow(page = page, onClick = { onSelectPage(page, null) })\n',
    '                    SavedPageRow(page = page, riwaya = riwaya, onClick = { onSelectPage(page, null) })\n',
    'recent saved row')
t = replace_once(t,
    'private fun SavedPageRow(page: Int, onClick: () -> Unit) {\n    val context = LocalContext.current\n    val surah = remember(page) { MushafRepository.surahForPage(context, page) }\n',
    'private fun SavedPageRow(page: Int, riwaya: QuranRiwaya, onClick: () -> Unit) {\n    val context = LocalContext.current\n    val surah = remember(page, riwaya) { MushafRepository.surahForPage(context, riwaya, page) }\n',
    'saved row signature')
old_attr = '''@Composable\nprivate fun QuranAttribution() {\n    HorizontalDivider(color = ArihnaWarmOutline.copy(alpha = 0.65f))\n    Text(\n        text = appText(\n            "Testo: Tanzil/Tarteel CC BY 3.0 · pagine Muṣḥaf: quran-svg MIT · tutto offline",\n            "النص: تنزيل/ترتيل CC BY 3.0 · صفحات المصحف: quran-svg MIT · يعمل دون اتصال",\n        ),\n        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp).testTag("quran-attribution"),\n        color = ArihnaMutedText,\n        fontSize = 8.sp,\n        textAlign = TextAlign.Center,\n    )\n}\n'''
new_attr = '''@Composable\nprivate fun QuranAttribution(riwaya: QuranRiwaya?) {\n    HorizontalDivider(color = ArihnaWarmOutline.copy(alpha = 0.65f))\n    val credit = when (riwaya) {\n        QuranRiwaya.WARSH -> appText(\n            "Warsh: pagine King Fahd Complex (uso digitale/app consentito; no stampa commerciale) · metadati Quranpedia CC0 · offline",\n            "ورش: صفحات مجمع الملك فهد للاستخدام الرقمي والتطبيقي · بيانات Quranpedia CC0 · دون اتصال",\n        )\n        QuranRiwaya.HAFS -> appText(\n            "Testo: Tanzil/Tarteel CC BY 3.0 · pagine Ḥafṣ: batoulapps/quran-svg MIT · tutto offline",\n            "النص: تنزيل/ترتيل CC BY 3.0 · صفحات حفص: batoulapps/quran-svg MIT · دون اتصال",\n        )\n        null -> appText(\n            "Testo: Tanzil/Tarteel CC BY 3.0 · tutto offline",\n            "النص: تنزيل/ترتيل CC BY 3.0 · يعمل دون اتصال",\n        )\n    }\n    Text(\n        text = credit,\n        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp).testTag("quran-attribution"),\n        color = ArihnaMutedText,\n        fontSize = 8.sp,\n        textAlign = TextAlign.Center,\n    )\n}\n'''
t = replace_once(t, old_attr, new_attr, 'attribution function')
screen.write_text(t, encoding='utf-8')

# --- Instrumentation: physical click regression, multi-riwaya persistence, inset simulation. ---
test = root / 'app/src/androidTest/java/com/archimedeprojects/arihna/feature/quran/QuranFullscreenAndroidTest.kt'
test.write_text(r'''package com.archimedeprojects.arihna.feature.quran

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QuranFullscreenAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun waitForExists(tag: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForMissing(tag: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun readingButtonOpensAndClosesRealFullscreenReader() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        QuranReadingPrefs.setMode(context, QuranReadingMode.HAFS_UTHMANI)
        var immersive = false
        composeRule.setContent {
            QuranPlaceholderScreen(
                contentPadding = PaddingValues(0.dp),
                onImmersiveChanged = { immersive = it },
            )
        }

        waitForExists("quran-reading-fullscreen")
        composeRule.onNodeWithTag("quran-reading-fullscreen")
            .performSemanticsAction(SemanticsActions.OnClick)
        waitForExists("quran-fullscreen-reader")
        waitForExists("quran-fullscreen-close")
        composeRule.runOnIdle { assertTrue(immersive) }

        composeRule.onNodeWithTag("quran-fullscreen-close")
            .performSemanticsAction(SemanticsActions.OnClick)
        waitForMissing("quran-fullscreen-reader")
        waitForExists("quran-reading-fullscreen")
        composeRule.runOnIdle { assertFalse(immersive) }
    }

    @Test
    fun readingButtonStaysTouchableWhilePremiumPreviewIsVisible() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        QuranReadingPrefs.setMode(context, QuranReadingMode.HAFS_UTHMANI)
        QuranReadingPrefs.recordVisitedPage(context, QuranRiwaya.HAFS, 11)
        composeRule.setContent { QuranPlaceholderScreen(PaddingValues(0.dp)) }

        waitForExists("quran-mushaf-toolbar")
        waitForExists("quran-premium-page-frame")
        composeRule.onNodeWithTag("quran-premium-page-frame").assertIsDisplayed()
        composeRule.onNodeWithTag("quran-reading-fullscreen")
            .assertIsDisplayed()
            .performTouchInput { click() }
        waitForExists("quran-fullscreen-reader")
    }

    @Test
    fun fullscreenTopBarHonorsInjectedSafeInset() {
        composeRule.setContent {
            FullscreenMushafReader(
                startPage = 0,
                style = MushafVisualStyle.CLASSIC,
                riwaya = QuranRiwaya.HAFS,
                onDismiss = {},
                onPageChanged = {},
                topBarInsets = WindowInsets(top = 72.dp),
            )
        }
        waitForExists("quran-fullscreen-chrome")
        val density = composeRule.density
        val top = composeRule.onNodeWithTag("quran-fullscreen-chrome").fetchSemanticsNode().boundsInRoot.top
        val expected = with(density) { 72.dp.toPx() }
        assertTrue("fullscreen chrome top=$top expected >= $expected", top >= expected)
    }

    @Test
    fun warshSwitchKeepsBookmarksSeparatedByRiwaya() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        QuranReadingPrefs.setMode(context, QuranReadingMode.HAFS_UTHMANI)
        QuranReadingPrefs.recordVisitedPage(context, QuranRiwaya.HAFS, 0)
        QuranReadingPrefs.recordVisitedPage(context, QuranRiwaya.WARSH, 0)
        QuranReadingPrefs.setBookmarked(context, QuranRiwaya.HAFS, 0, false)
        QuranReadingPrefs.setBookmarked(context, QuranRiwaya.WARSH, 0, false)

        composeRule.setContent { QuranPlaceholderScreen(PaddingValues(0.dp)) }
        waitForExists("quran-mushaf-riwaya-hafs")
        composeRule.onNodeWithTag("quran-bookmark-toggle").performClick()
        composeRule.runOnIdle {
            assertTrue(QuranReadingPrefs.isBookmarked(context, QuranRiwaya.HAFS, 0))
            assertFalse(QuranReadingPrefs.isBookmarked(context, QuranRiwaya.WARSH, 0))
        }

        composeRule.onNodeWithTag("quran-mode-warsh").performClick()
        waitForExists("quran-mushaf-riwaya-warsh")
        waitForExists("quran-mushaf-page-1")
        composeRule.runOnIdle {
            assertTrue(QuranReadingPrefs.isBookmarked(context, QuranRiwaya.HAFS, 0))
            assertFalse(QuranReadingPrefs.isBookmarked(context, QuranRiwaya.WARSH, 0))
        }

        composeRule.onNodeWithTag("quran-bookmark-toggle").performClick()
        composeRule.runOnIdle {
            assertTrue(QuranReadingPrefs.isBookmarked(context, QuranRiwaya.HAFS, 0))
            assertTrue(QuranReadingPrefs.isBookmarked(context, QuranRiwaya.WARSH, 0))
        }
    }

    @Test
    fun eachRiwayaRestoresOwnLastPageAndRecentHistory() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        QuranReadingPrefs.recordVisitedPage(context, QuranRiwaya.HAFS, 11)
        QuranReadingPrefs.recordVisitedPage(context, QuranRiwaya.WARSH, 22)
        assertEquals(11, QuranReadingPrefs.lastPage(context, QuranRiwaya.HAFS))
        assertEquals(22, QuranReadingPrefs.lastPage(context, QuranRiwaya.WARSH))
        assertEquals(11, QuranReadingPrefs.recentPages(context, QuranRiwaya.HAFS).first())
        assertEquals(22, QuranReadingPrefs.recentPages(context, QuranRiwaya.WARSH).first())

        QuranReadingPrefs.setMode(context, QuranReadingMode.WARSH_NAFI)
        composeRule.setContent { QuranPlaceholderScreen(PaddingValues(0.dp)) }
        waitForExists("quran-mushaf-riwaya-warsh")
        waitForExists("quran-mushaf-page-23")
        composeRule.onNodeWithTag("quran-mode-hafs").performClick()
        waitForExists("quran-mushaf-riwaya-hafs")
        waitForExists("quran-mushaf-page-12")
    }

    @Test
    fun indexLargeTargetsAreClickable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        QuranReadingPrefs.setMode(context, QuranReadingMode.HAFS_UTHMANI)
        composeRule.setContent { QuranPlaceholderScreen(PaddingValues(0.dp)) }

        composeRule.onNodeWithTag("quran-surah-selector").performClick()
        composeRule.onNodeWithTag("quran-explorer-juz").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("quran-explorer-hizb").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("quran-shortcut-bookmarks").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("quran-shortcut-recent").assertIsDisplayed().performClick()
    }

    @Test
    fun warshIndexDoesNotPretendHafsJuzHizbBoundaries() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        QuranReadingPrefs.setMode(context, QuranReadingMode.WARSH_NAFI)
        composeRule.setContent { QuranPlaceholderScreen(PaddingValues(0.dp)) }
        composeRule.onNodeWithTag("quran-surah-selector").performClick()
        waitForExists("quran-warsh-boundaries-unavailable")
        composeRule.onNodeWithTag("quran-warsh-boundaries-unavailable").assertIsDisplayed()
    }

    @Test
    fun premiumStylePersistsAndFullscreenChromeToggles() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        QuranReadingPrefs.setMode(context, QuranReadingMode.HAFS_UTHMANI)
        QuranReadingPrefs.setVisualStyle(context, MushafVisualStyle.CLASSIC)
        val pageNumber = QuranReadingPrefs.lastPage(context, QuranRiwaya.HAFS) + 1

        composeRule.setContent { QuranPlaceholderScreen(PaddingValues(0.dp)) }
        waitForExists("quran-style-menu")
        composeRule.onNodeWithTag("quran-style-menu")
            .performSemanticsAction(SemanticsActions.OnClick)
        waitForExists("quran-style-clean")
        composeRule.onNodeWithTag("quran-style-clean")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.runOnIdle {
            assertEquals(MushafVisualStyle.CLEAN, QuranReadingPrefs.visualStyle(context))
        }

        waitForExists("quran-reading-fullscreen")
        composeRule.onNodeWithTag("quran-reading-fullscreen")
            .performSemanticsAction(SemanticsActions.OnClick)
        waitForExists("quran-fullscreen-reader")
        waitForExists("quran-fullscreen-chrome")
        waitForExists("quran-fullscreen-page-context")
        waitForExists("quran-fullscreen-bottom-context")

        waitForExists("quran-zoomable-page-$pageNumber")
        composeRule.onNodeWithTag("quran-zoomable-page-$pageNumber")
            .performSemanticsAction(SemanticsActions.OnClick)
        waitForMissing("quran-fullscreen-chrome")
        composeRule.onNodeWithTag("quran-zoomable-page-$pageNumber")
            .performSemanticsAction(SemanticsActions.OnClick)
        waitForExists("quran-fullscreen-chrome")
    }
}
''', encoding='utf-8')

# Add a riwaya tag on the MushafBookReader root after all other replacements.
t = screen.read_text(encoding='utf-8')
t = replace_once(t,
    '            modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),\n            verticalArrangement = Arrangement.spacedBy(2.dp),\n',
    '            modifier = modifier\n                .fillMaxWidth()\n                .padding(horizontal = 4.dp, vertical = 2.dp)\n                .testTag("quran-mushaf-riwaya-${riwaya.name.lowercase()}"),\n            verticalArrangement = Arrangement.spacedBy(2.dp),\n',
    'riwaya root tag')
screen.write_text(t, encoding='utf-8')

# Guard scope: only Quran implementation/build theme/test/spec-derived files may change.
allowed = {
    'app/build.gradle.kts',
    'app/src/main/java/com/archimedeprojects/arihna/feature/quran/MushafRepository.kt',
    'app/src/main/java/com/archimedeprojects/arihna/feature/quran/NativeMushafPage.kt',
    'app/src/main/java/com/archimedeprojects/arihna/feature/quran/QuranPlaceholderScreen.kt',
    'app/src/androidTest/java/com/archimedeprojects/arihna/feature/quran/QuranFullscreenAndroidTest.kt',
    'app/src/main/res/values/themes.xml',
}
print('PATCHED', *sorted(allowed), sep='\n- ')
