from pathlib import Path

Q = Path('app/src/main/java/com/archimedeprojects/arihna/feature/quran/QuranPlaceholderScreen.kt')
R = Path('app/src/main/java/com/archimedeprojects/arihna/feature/quran/MushafRepository.kt')
T = Path('app/src/androidTest/java/com/archimedeprojects/arihna/feature/quran/QuranFullscreenAndroidTest.kt')


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise AssertionError(f'{label}: expected exactly one match, got {count}')
    return text.replace(old, new, 1)


# ---------------- MushafRepository: authoritative page starts + separate Tajwid page state ----------------
r = R.read_text(encoding='utf-8')
r = replace_once(
    r,
    '    @Volatile private var hafsPageMetaCache: List<MushafPageMeta>? = null\n',
    '    @Volatile private var hafsPageMetaCache: List<MushafPageMeta>? = null\n'
    '    @Volatile private var hafsPageStartsCache: List<Pair<Int, Int>>? = null\n',
    'page starts cache',
)

needle = '''    fun surahForPage(context: Context, riwaya: QuranRiwaya, pageIndex: Int): MushafSurah? {
        val pageNumber = pageIndex.coerceIn(0, 603) + 1
        val source = surahs(context, riwaya)
        return source.lastOrNull { it.pageNumber <= pageNumber } ?: source.firstOrNull()
    }

'''
insert = needle + '''    /** Exact Madani/Hafs page starts from the pinned QuranData.Page asset. No synthetic fallback. */
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

'''
r = replace_once(r, needle, insert, 'authoritative page API')
r = replace_once(
    r,
    '        val pageStarts = parsePairs(metadata, "Page").take(604)\n',
    '        val pageStarts = hafsPageStarts(context)\n',
    'page meta reuse authoritative starts',
)

r = replace_once(
    r,
    '    private const val KEY_TAJWID_RECENT = "tajwid_recent_surahs_v1"\n',
    '    private const val KEY_TAJWID_RECENT = "tajwid_recent_surahs_v1"\n'
    '    private const val KEY_TAJWID_PAGE_BOOKMARKS = "tajwid_bookmarked_pages_v2"\n'
    '    private const val KEY_TAJWID_LAST_PAGE = "tajwid_last_page_v2"\n'
    '    private const val KEY_TAJWID_PAGE_RECENT = "tajwid_recent_pages_v2"\n',
    'tajwid page keys',
)

mode_marker = '    fun mode(context: Context): QuranReadingMode = runCatching {\n'
page_prefs = '''    fun tajwidBookmarkedPages(context: Context): Set<Int> =
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

'''
if r.count(mode_marker) != 1:
    raise AssertionError('mode marker missing')
r = r.replace(mode_marker, page_prefs + mode_marker, 1)
R.write_text(r, encoding='utf-8')


# ---------------- Quran UI ----------------
q = Q.read_text(encoding='utf-8')
q = replace_once(
    q,
    'import androidx.compose.foundation.rememberScrollState\n',
    'import androidx.compose.foundation.rememberScrollState\nimport androidx.compose.foundation.verticalScroll\n',
    'verticalScroll import',
)

state_start = q.index('    var selectedSurah by remember {')
state_end = q.index('    var explorerOpen by remember', state_start)
q = q[:state_start] + '''    var requestedPage by remember {
        mutableIntStateOf(
            if (mode == QuranReadingMode.HAFS_TAJWID) {
                QuranReadingPrefs.lastTajwidPage(context)
            } else {
                QuranReadingPrefs.lastPage(context, activeRiwaya)
            },
        )
    }
    var selectedSurah by remember {
        mutableIntStateOf(
            MushafRepository.surahForPage(context, activeRiwaya, requestedPage)?.number ?: 1,
        )
    }
''' + q[state_end:]

q = replace_once(
    q,
    '''            FullscreenTajwidReader(
                corpus = corpus,
                selectedSurah = selectedSurah,
                onDismiss = { fullscreenOpen = false },
            )''',
    '''            FullscreenTajwidReader(
                corpus = corpus,
                startPage = requestedPage,
                onDismiss = { fullscreenOpen = false },
                onPageChanged = { pageIndex ->
                    requestedPage = pageIndex
                    MushafRepository.surahForPage(context, QuranRiwaya.HAFS, pageIndex)?.let { selectedSurah = it.number }
                },
            )''',
    'fullscreen Tajwid call',
)

old_mode_switch = '''                if (selected == QuranReadingMode.HAFS_TAJWID) {
                    selectedSurah = QuranReadingPrefs.lastTajwidSurah(context)
                    requestedPage = MushafRepository.surahs(context, QuranRiwaya.HAFS)
                        .firstOrNull { it.number == selectedSurah }
                        ?.pageNumber
                        ?.minus(1)
                        ?.coerceIn(0, 603)
                        ?: 0
                } else {'''
new_mode_switch = '''                if (selected == QuranReadingMode.HAFS_TAJWID) {
                    requestedPage = QuranReadingPrefs.lastTajwidPage(context)
                    selectedSurah = MushafRepository.surahForPage(context, QuranRiwaya.HAFS, requestedPage)?.number ?: 1
                } else {'''
q = replace_once(q, old_mode_switch, new_mode_switch, 'mode switch Tajwid page state')

q = replace_once(
    q,
    '''            TajwidQuranReader(
                corpus = corpus,
                selectedSurah = selectedSurah,
                onOpenExplorer = { explorerOpen = true },
                onOpenFullscreen = { fullscreenOpen = true },
                modifier = Modifier.weight(1f),
            )''',
    '''            TajwidQuranReader(
                corpus = corpus,
                startPage = requestedPage,
                onOpenFullscreen = { fullscreenOpen = true },
                onPageChanged = { pageIndex ->
                    requestedPage = pageIndex
                    MushafRepository.surahForPage(context, QuranRiwaya.HAFS, pageIndex)?.let { selectedSurah = it.number }
                },
                modifier = Modifier.weight(1f),
            )''',
    'Tajwid reader call',
)

# Replace horizontally clipped reading mode chips with four compact fixed-width tabs.
row_start_marker = '''            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {'''
row_start = q.index(row_start_marker)
row_end_marker = '\n            }\n        }\n    }\n}\n\n    @Composable\n    private fun MushafBookReader'
row_end = q.index(row_end_marker, row_start)
q = q[:row_start] + '''            QuranModeTabs(mode = mode, onModeChange = onModeChange)
''' + q[row_end + len('\n            }'):]

insert_marker = '    @Composable\n    private fun MushafBookReader'
mode_tabs = '''@Composable
private fun QuranModeTabs(
    mode: QuranReadingMode,
    onModeChange: (QuranReadingMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag("quran-mode-tabs"),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        QuranModeTab(QuranReadingMode.HAFS_UTHMANI, mode, appText("Ḥafṣ", "حفص"), "quran-mode-hafs", onModeChange, Modifier.weight(1f))
        QuranModeTab(QuranReadingMode.HAFS_TAJWID, mode, appText("Tajwid β", "تجويد β"), "quran-mode-tajwid", onModeChange, Modifier.weight(1f))
        QuranModeTab(QuranReadingMode.WARSH_NAFI, mode, appText("Warsh", "ورش"), "quran-mode-warsh", onModeChange, Modifier.weight(1f))
        QuranModeTab(QuranReadingMode.EASY, mode, appText("Facile", "سهل"), "quran-mode-easy", onModeChange, Modifier.weight(1f))
    }
}

@Composable
private fun QuranModeTab(
    value: QuranReadingMode,
    selected: QuranReadingMode,
    label: String,
    tag: String,
    onModeChange: (QuranReadingMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = value == selected
    Surface(
        onClick = { onModeChange(value) },
        modifier = modifier.heightIn(min = 44.dp).testTag(tag),
        shape = RoundedCornerShape(12.dp),
        color = if (active) ArihnaSage else Color.Transparent,
        border = BorderStroke(1.dp, if (active) ArihnaGreen else ArihnaWarmOutline),
    ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 9.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (active) ArihnaForest else ArihnaMutedText,
                fontSize = 10.sp,
                fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

'''
if q.count(insert_marker) != 1:
    raise AssertionError('MushafBookReader insert marker missing')
q = q.replace(insert_marker, mode_tabs + insert_marker, 1)

# Explorer state is page-based in Tajwid too.
q = replace_once(
    q,
    '''    val bookmarked = remember(bookmarkVersion, currentPage, riwaya, tajwidMode) {
        if (tajwidMode) QuranReadingPrefs.tajwidBookmarkedSurahs(context).toList()
        else QuranReadingPrefs.bookmarkedPages(context, riwaya)
    }
    val recent = remember(currentPage, riwaya, tajwidMode) {
        if (tajwidMode) QuranReadingPrefs.recentTajwidSurahs(context)
        else QuranReadingPrefs.recentPages(context, riwaya)
    }''',
    '''    val bookmarked = remember(bookmarkVersion, currentPage, riwaya, tajwidMode) {
        if (tajwidMode) QuranReadingPrefs.tajwidBookmarkedPages(context)
        else QuranReadingPrefs.bookmarkedPages(context, riwaya)
    }
    val recent = remember(currentPage, riwaya, tajwidMode) {
        if (tajwidMode) QuranReadingPrefs.recentTajwidPages(context)
        else QuranReadingPrefs.recentPages(context, riwaya)
    }''',
    'explorer page state',
)

q = replace_once(
    q,
    '''                        bookmarked = if (tajwidMode) surah.number in bookmarked else (surah.pageNumber - 1) in bookmarked,
                        onBookmark = {
                            if (tajwidMode) {
                                QuranReadingPrefs.setTajwidSurahBookmarked(
                                    context,
                                    surah.number,
                                    surah.number !in bookmarked,
                                )
                            } else {
                                val page = (surah.pageNumber - 1).coerceIn(0, 603)
                                QuranReadingPrefs.setBookmarked(context, riwaya, page, page !in bookmarked)
                            }
                            bookmarkVersion++
                        },''',
    '''                        bookmarked = (surah.pageNumber - 1) in bookmarked,
                        onBookmark = {
                            val page = (surah.pageNumber - 1).coerceIn(0, 603)
                            if (tajwidMode) {
                                QuranReadingPrefs.setTajwidPageBookmarked(context, page, page !in bookmarked)
                            } else {
                                QuranReadingPrefs.setBookmarked(context, riwaya, page, page !in bookmarked)
                            }
                            bookmarkVersion++
                        },''',
    'surah page bookmark',
)

book_start = q.index('                QuranExplorerView.BOOKMARKS -> {')
recent_start = q.index('                QuranExplorerView.RECENT -> {', book_start)
book_block = '''                QuranExplorerView.BOOKMARKS -> {
                    items(bookmarked.sorted(), key = { "bookmark-$tajwidMode-$it" }) { page ->
                        SavedPageRow(page = page, riwaya = riwaya, onClick = { onSelectPage(page, null) })
                    }
                }
'''
q = q[:book_start] + book_block + q[recent_start:]
recent_start = q.index('                QuranExplorerView.RECENT -> {', book_start)
recent_end_marker = '            }\n        }\n    }\n}\n\n@Composable\nprivate fun ExplorerChip'
recent_end = q.index(recent_end_marker, recent_start)
recent_block = '''                QuranExplorerView.RECENT -> {
                    items(recent, key = { "recent-$tajwidMode-$it" }) { page ->
                        SavedPageRow(page = page, riwaya = riwaya, onClick = { onSelectPage(page, null) })
                    }
                }
'''
q = q[:recent_start] + recent_block + q[recent_end:]

# Replace the per-ayah-card Tajwid readers with a 604-page authoritative Mushaf-style pager.
tajwid_start = q.index('@Composable\nprivate fun TajwidQuranReader(')
tajwid_end = q.index('private fun tajwidColor(', tajwid_start)
new_tajwid = r'''@Composable
private fun TajwidQuranReader(
    corpus: QuranCorpus,
    startPage: Int,
    onOpenFullscreen: () -> Unit,
    onPageChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val pageStarts = remember(context) { MushafRepository.hafsPageStarts(context.applicationContext) }
    if (pageStarts.size != 604) {
        Surface(modifier = modifier.fillMaxWidth().padding(12.dp), color = ArihnaCream, shape = RoundedCornerShape(18.dp)) {
            Text(
                appText("Pagine Tajwid non disponibili: metadati Hafs incompleti.", "صفحات التجويد غير متاحة: بيانات صفحات حفص غير مكتملة."),
                modifier = Modifier.padding(18.dp),
                color = ArihnaMutedText,
            )
        }
        return
    }

    val pagerState = rememberPagerState(initialPage = startPage.coerceIn(0, 603), pageCount = { 604 })
    var bookmarkVersion by remember { mutableIntStateOf(0) }
    var fontScale by remember { mutableFloatStateOf(1f) }
    var legendExpanded by remember { mutableStateOf(false) }
    val currentPage = pagerState.currentPage
    val bookmarked = remember(currentPage, bookmarkVersion) {
        QuranReadingPrefs.isTajwidPageBookmarked(context, currentPage)
    }

    LaunchedEffect(startPage) {
        val target = startPage.coerceIn(0, 603)
        if (pagerState.currentPage != target) pagerState.scrollToPage(target)
    }
    LaunchedEffect(currentPage) {
        QuranReadingPrefs.recordVisitedTajwidPage(context, currentPage)
        onPageChanged(currentPage)
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp).testTag("quran-tajwid-beta-reader"),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    appText("Pagina ${currentPage + 1} · Tajwid Beta", "صفحة ${toArabicIndic(currentPage + 1)} · تجويد تجريبي"),
                    color = ArihnaForest,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    modifier = Modifier.testTag("quran-tajwid-page-context"),
                )
                Text(
                    appText("Colorazione tajwid · Beta / regole principali", "تلوين التجويد · تجريبي / القواعد الرئيسية"),
                    color = ArihnaMutedText,
                    fontSize = 8.sp,
                    maxLines = 1,
                    modifier = Modifier.testTag("quran-tajwid-beta-disclaimer"),
                )
            }
            TextButton(onClick = { fontScale = (fontScale - 0.08f).coerceAtLeast(0.78f) }, modifier = Modifier.testTag("quran-tajwid-zoom-out")) {
                Text("A−", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            TextButton(onClick = { fontScale = (fontScale + 0.08f).coerceAtMost(1.45f) }, modifier = Modifier.testTag("quran-tajwid-zoom-in")) {
                Text("A+", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            TextButton(onClick = onOpenFullscreen, modifier = Modifier.testTag("quran-tajwid-fullscreen")) {
                Icon(Icons.Rounded.Fullscreen, null, Modifier.size(18.dp))
                Spacer(Modifier.width(2.dp))
                Text(appText("Lettura", "قراءة"), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(
                onClick = {
                    QuranReadingPrefs.setTajwidPageBookmarked(context, currentPage, !bookmarked)
                    bookmarkVersion++
                },
                modifier = Modifier.size(46.dp).testTag("quran-tajwid-bookmark"),
            ) {
                Icon(
                    if (bookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = appText("Segnalibro Tajwid", "إشارة تجويد مرجعية"),
                    tint = if (bookmarked) ArihnaDawnGold else ArihnaGreen,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                appText("Generata localmente; non pretende di essere un Muṣḥaf Tajwid completo.", "مولد محليًا؛ لا يدّعي أنه مصحف تجويد كامل."),
                modifier = Modifier.weight(1f),
                color = ArihnaMutedText,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(
                onClick = { legendExpanded = !legendExpanded },
                modifier = Modifier.heightIn(min = 36.dp).testTag("quran-tajwid-legend-toggle"),
                contentPadding = PaddingValues(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text(appText(if (legendExpanded) "Nascondi legenda" else "Legenda", if (legendExpanded) "إخفاء الدليل" else "دليل الألوان"), fontSize = 9.sp)
            }
        }
        if (legendExpanded) TajwidLegend()

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f).testTag("quran-tajwid-rtl-pager"),
                beyondViewportPageCount = 1,
                pageSpacing = 6.dp,
            ) { page ->
                TajwidMushafPage(
                    pageIndex = page,
                    corpus = corpus,
                    pageStarts = pageStarts,
                    fontScale = fontScale,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun FullscreenTajwidReader(
    corpus: QuranCorpus,
    startPage: Int,
    onDismiss: () -> Unit,
    onPageChanged: (Int) -> Unit,
) {
    val context = LocalContext.current
    val pageStarts = remember(context) { MushafRepository.hafsPageStarts(context.applicationContext) }
    if (pageStarts.size != 604) {
        Surface(modifier = Modifier.fillMaxSize().testTag("quran-tajwid-fullscreen-reader"), color = Color(0xFFFFFCF3)) {
            Text(appText("Metadati pagina Hafs non disponibili.", "بيانات صفحات حفص غير متاحة."), modifier = Modifier.padding(24.dp))
        }
        return
    }

    val pagerState = rememberPagerState(initialPage = startPage.coerceIn(0, 603), pageCount = { 604 })
    var fontScale by remember { mutableFloatStateOf(1.08f) }
    var bookmarkVersion by remember { mutableIntStateOf(0) }
    val currentPage = pagerState.currentPage
    val surah = remember(currentPage) { MushafRepository.surahForPage(context, QuranRiwaya.HAFS, currentPage) }
    val bookmarked = remember(currentPage, bookmarkVersion) { QuranReadingPrefs.isTajwidPageBookmarked(context, currentPage) }

    BackHandler(onBack = onDismiss)
    LaunchedEffect(currentPage) {
        QuranReadingPrefs.recordVisitedTajwidPage(context, currentPage)
        onPageChanged(currentPage)
    }

    Surface(modifier = Modifier.fillMaxSize().testTag("quran-tajwid-fullscreen-reader"), color = Color(0xFFF4EBD8)) {
        Column(Modifier.fillMaxSize()) {
            Surface(
                color = ArihnaCream.copy(alpha = 0.97f),
                border = BorderStroke(1.dp, ArihnaWarmOutline),
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.displayCutout.union(WindowInsets.statusBars)),
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp).testTag("quran-tajwid-fullscreen-close")) {
                        Icon(Icons.Rounded.Close, appText("Chiudi", "إغلاق"), tint = ArihnaForest)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(surah?.nameArabic ?: appText("Corano", "القرآن"), color = ArihnaForest, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, maxLines = 1)
                        Text(
                            appText("Pagina ${currentPage + 1} · Tajwid Beta", "صفحة ${toArabicIndic(currentPage + 1)} · تجويد تجريبي"),
                            color = ArihnaMutedText,
                            fontSize = 8.sp,
                            modifier = Modifier.testTag("quran-tajwid-fullscreen-page-context"),
                        )
                    }
                    TextButton(onClick = { fontScale = (fontScale - 0.08f).coerceAtLeast(0.78f) }, modifier = Modifier.testTag("quran-tajwid-fullscreen-zoom-out")) { Text("A−") }
                    TextButton(onClick = { fontScale = (fontScale + 0.08f).coerceAtMost(1.65f) }, modifier = Modifier.testTag("quran-tajwid-fullscreen-zoom-in")) { Text("A+") }
                    IconButton(
                        onClick = {
                            QuranReadingPrefs.setTajwidPageBookmarked(context, currentPage, !bookmarked)
                            bookmarkVersion++
                        },
                        modifier = Modifier.size(48.dp).testTag("quran-tajwid-fullscreen-bookmark"),
                    ) {
                        Icon(
                            if (bookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            appText("Segnalibro", "إشارة مرجعية"),
                            tint = if (bookmarked) ArihnaDawnGold else ArihnaGreen,
                        )
                    }
                }
            }

            Text(
                appText("Tajwid Beta · regole principali · colorazione algoritmica locale", "تجويد تجريبي · القواعد الرئيسية · تلوين خوارزمي محلي"),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp).testTag("quran-tajwid-fullscreen-disclaimer"),
                textAlign = TextAlign.Center,
                color = ArihnaMutedText,
                fontSize = 8.sp,
            )

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .pointerInput(currentPage) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    if (event.changes.count { it.pressed } >= 2) {
                                        fontScale = (fontScale * event.calculateZoom()).coerceIn(0.78f, 1.65f)
                                        event.changes.forEach { it.consume() }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("quran-tajwid-fullscreen-content"),
                    beyondViewportPageCount = 1,
                    pageSpacing = 6.dp,
                ) { page ->
                    TajwidMushafPage(
                        pageIndex = page,
                        corpus = corpus,
                        pageStarts = pageStarts,
                        fontScale = fontScale,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun TajwidLegend() {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("quran-tajwid-legend"),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        TajwidRule.entries.forEach { rule ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ArihnaCream,
                border = BorderStroke(1.dp, tajwidColor(rule).copy(alpha = 0.65f)),
            ) {
                Text(
                    tajwidRuleLabel(rule),
                    color = tajwidColor(rule),
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun TajwidMushafPage(
    pageIndex: Int,
    corpus: QuranCorpus,
    pageStarts: List<Pair<Int, Int>>,
    fontScale: Float,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val ayahs = remember(corpus, pageStarts, pageIndex) { tajwidAyahsForPage(corpus, pageStarts, pageIndex) }
    val segments = remember(ayahs) { tajwidSurahSegments(ayahs) }
    val surahs = remember(context) { MushafRepository.surahs(context.applicationContext, QuranRiwaya.HAFS).associateBy { it.number } }
    val meta = remember(pageIndex) { MushafRepository.metaForPage(context.applicationContext, QuranRiwaya.HAFS, pageIndex) }

    Surface(
        modifier = modifier.padding(horizontal = 2.dp, vertical = 1.dp).testTag("quran-tajwid-page-${pageIndex + 1}"),
        color = Color(0xFFFFFBEE),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ArihnaDawnGold.copy(alpha = 0.48f)),
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 11.dp)
                .testTag("quran-tajwid-page-surface"),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    meta?.let { "الجزء ${toArabicIndic(it.juz)} · الحزب ${toArabicIndic(it.hizb)}" }.orEmpty(),
                    color = ArihnaMutedText,
                    fontSize = 8.sp,
                )
                Text("صفحة ${toArabicIndic(pageIndex + 1)}", color = ArihnaMutedText, fontSize = 8.sp)
            }

            segments.forEach { segment ->
                val first = segment.first()
                if (first.ayah == 1) {
                    val name = surahs[first.surah]?.nameArabic ?: "${toArabicIndic(first.surah)}"
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = ArihnaSage.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ArihnaDawnGold.copy(alpha = 0.42f)),
                    ) {
                        Text(
                            "سُورَةُ $name",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            textAlign = TextAlign.Center,
                            color = ArihnaForest,
                            fontSize = (16f * fontScale).sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                    if (first.surah != 1 && first.surah != 9) {
                        Text(
                            "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            textAlign = TextAlign.Center,
                            color = ArihnaInk,
                            fontSize = (18f * fontScale).sp,
                            lineHeight = (30f * fontScale).sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                Text(
                    text = buildTajwidAnnotatedPageText(segment),
                    modifier = Modifier.fillMaxWidth().testTag("quran-tajwid-page-text-${pageIndex + 1}"),
                    textAlign = TextAlign.Justify,
                    fontSize = (23f * fontScale).sp,
                    lineHeight = (38f * fontScale).sp,
                    color = ArihnaInk,
                    fontWeight = FontWeight.Normal,
                )
            }

            Spacer(Modifier.height(4.dp))
            Text(
                toArabicIndic(pageIndex + 1),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = ArihnaDawnGold,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

internal fun buildTajwidAnnotatedPageText(ayahs: List<QuranAyah>) = buildAnnotatedString {
    ayahs.forEachIndexed { index, ayah ->
        if (index > 0) append(" ")
        val ayahOffset = length
        append(ayah.text)
        UthmaniTajwidEngine.find(ayah.text).forEach { span ->
            addStyle(
                SpanStyle(color = tajwidColor(span.rule)),
                ayahOffset + span.start,
                ayahOffset + span.endExclusive,
            )
        }
        append("\u00A0")
        val markerStart = length
        append("۝${toArabicIndic(ayah.ayah)}")
        addStyle(SpanStyle(color = ArihnaDawnGold), markerStart, length)
    }
}

private fun tajwidAyahsForPage(
    corpus: QuranCorpus,
    pageStarts: List<Pair<Int, Int>>,
    pageIndex: Int,
): List<QuranAyah> {
    val start = pageStarts.getOrNull(pageIndex) ?: return emptyList()
    val end = pageStarts.getOrNull(pageIndex + 1)
    return corpus.ayahs.filter { ayah ->
        atOrAfter(ayah, start) && (end == null || before(ayah, end))
    }
}

private fun tajwidSurahSegments(ayahs: List<QuranAyah>): List<List<QuranAyah>> {
    if (ayahs.isEmpty()) return emptyList()
    val result = mutableListOf<MutableList<QuranAyah>>()
    ayahs.forEach { ayah ->
        val last = result.lastOrNull()
        if (last == null || last.last().surah != ayah.surah) result += mutableListOf(ayah)
        else last += ayah
    }
    return result.map { it.toList() }
}

private fun atOrAfter(ayah: QuranAyah, start: Pair<Int, Int>): Boolean =
    ayah.surah > start.first || (ayah.surah == start.first && ayah.ayah >= start.second)

private fun before(ayah: QuranAyah, end: Pair<Int, Int>): Boolean =
    ayah.surah < end.first || (ayah.surah == end.first && ayah.ayah < end.second)

'''
q = q[:tajwid_start] + new_tajwid + q[tajwid_end:]
Q.write_text(q, encoding='utf-8')


# ---------------- Instrumentation regression ----------------
t = T.read_text(encoding='utf-8')
old_test_start = t.index('    @Test\n    fun tajwidBetaIsClearlyLabeledAndUsesSeparateSurahBookmarks()')
old_fullscreen_start = t.index('    @Test\n    fun tajwidFullscreenIsImmersiveAndKeepsZoomControls()', old_test_start)
new_test = '''    @Test
    fun tajwidBetaUsesAuthoritativePagePagerInlineLayoutAndSeparatePageBookmarks() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        QuranReadingPrefs.setMode(context, QuranReadingMode.HAFS_TAJWID)
        QuranReadingPrefs.recordVisitedTajwidPage(context, 1)
        QuranReadingPrefs.setTajwidPageBookmarked(context, 1, false)
        QuranReadingPrefs.setBookmarked(context, QuranRiwaya.HAFS, 1, false)

        val starts = MushafRepository.hafsPageStarts(context)
        assertEquals(604, starts.size)
        assertEquals(1 to 1, starts[0])
        assertEquals(2 to 1, starts[1])
        assertEquals(1, MushafRepository.hafsPageIndexForAyah(context, 2, 1))

        composeRule.setContent { QuranPlaceholderScreen(PaddingValues(0.dp)) }
        waitForExists("quran-tajwid-beta-reader")
        waitForExists("quran-tajwid-rtl-pager")
        waitForExists("quran-tajwid-page-2")
        composeRule.onNodeWithTag("quran-tajwid-page-context").assertIsDisplayed()
        composeRule.onNodeWithTag("quran-tajwid-beta-disclaimer").assertIsDisplayed()
        composeRule.onNodeWithTag("quran-tajwid-page-surface").assertIsDisplayed()
        composeRule.onNodeWithTag("quran-mode-tabs").assertIsDisplayed()
        listOf("quran-mode-hafs", "quran-mode-tajwid", "quran-mode-warsh", "quran-mode-easy").forEach { tag ->
            composeRule.onNodeWithTag(tag).assertIsDisplayed()
        }
        assertEquals(0, composeRule.onAllNodesWithTag("quran-tajwid-ayah-2-1").fetchSemanticsNodes().size)

        composeRule.onNodeWithTag("quran-tajwid-legend-toggle").performClick()
        waitForExists("quran-tajwid-legend")
        composeRule.onNodeWithTag("quran-tajwid-bookmark").performClick()
        composeRule.runOnIdle {
            assertTrue(QuranReadingPrefs.isTajwidPageBookmarked(context, 1))
            assertFalse(QuranReadingPrefs.isBookmarked(context, QuranRiwaya.HAFS, 1))
        }
    }

'''
t = t[:old_test_start] + new_test + t[old_fullscreen_start:]
t = t.replace('        QuranReadingPrefs.recordVisitedTajwidSurah(context, 1)\n', '        QuranReadingPrefs.recordVisitedTajwidPage(context, 1)\n', 1)

class_end = t.rfind('\n}')
inline_test = '''

    @Test
    fun tajwidAyahMarkerIsInlineWithTheAnnotatedAyahText() {
        val ayah = QuranAyah(2, 1, "الم")
        val decorated = buildTajwidAnnotatedPageText(listOf(ayah))
        assertTrue(decorated.text.contains(ayah.text + "\u00A0۝١"))
        assertTrue(decorated.text.startsWith(ayah.text))
    }
'''
t = t[:class_end] + inline_test + t[class_end:]
T.write_text(t, encoding='utf-8')

# Candidate scope and anti-regression source assertions.
assert 'horizontalScroll(rememberScrollState())' in Q.read_text(encoding='utf-8')  # legend only
source = Q.read_text(encoding='utf-8')
assert 'quran-mode-tabs' in source
assert 'quran-tajwid-rtl-pager' in source
assert 'quran-tajwid-page-surface' in source
assert 'append("\\u00A0")' in source
assert 'append("۝${toArabicIndic(ayah.ayah)}")' in source
assert 'private fun TajwidAyahCard' not in source
assert 'quran-tajwid-ayah-${ayah.surah}-${ayah.ayah}' not in source
