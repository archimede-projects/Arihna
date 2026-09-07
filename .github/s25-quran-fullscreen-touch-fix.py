from pathlib import Path

ROOT = Path('.')
QURAN = ROOT / 'app/src/main/java/com/archimedeprojects/arihna/feature/quran/QuranPlaceholderScreen.kt'
NAV = ROOT / 'app/src/main/java/com/archimedeprojects/arihna/app/ArihnaNavHost.kt'
TEST = ROOT / 'app/src/androidTest/java/com/archimedeprojects/arihna/feature/quran/QuranFullscreenAndroidTest.kt'


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected 1 occurrence, got {count}')
    return text.replace(old, new, 1)


def replace_between(text: str, start: str, end: str, replacement: str, label: str) -> str:
    i = text.find(start)
    if i < 0:
        raise SystemExit(f'{label}: start marker missing')
    j = text.find(end, i + len(start))
    if j < 0:
        raise SystemExit(f'{label}: end marker missing')
    return text[:i] + replacement + text[j:]


q = QURAN.read_text()
q = replace_once(q, 'package com.archimedeprojects.arihna.feature.quran\n\n', '''package com.archimedeprojects.arihna.feature.quran\n\nimport android.app.Activity\nimport android.content.Context\nimport android.content.ContextWrapper\nimport androidx.activity.compose.BackHandler\n''', 'android/activity imports')
q = replace_once(q, 'import androidx.compose.foundation.layout.height\n', 'import androidx.compose.foundation.layout.height\nimport androidx.compose.foundation.layout.heightIn\n', 'heightIn import')
q = replace_once(q, 'import androidx.compose.runtime.Composable\n', 'import androidx.compose.runtime.Composable\nimport androidx.compose.runtime.DisposableEffect\n', 'DisposableEffect import')
q = replace_once(q, 'import androidx.compose.ui.platform.LocalLayoutDirection\n', 'import androidx.compose.ui.platform.LocalLayoutDirection\nimport androidx.compose.ui.platform.LocalView\n', 'LocalView import')
q = q.replace('import androidx.compose.ui.window.Dialog\n', '')
q = q.replace('import androidx.compose.ui.window.DialogProperties\n', '')
q = replace_once(q, 'import com.archimedeprojects.arihna.core.ui.theme.ArihnaWarmOutline\n', '''import com.archimedeprojects.arihna.core.ui.theme.ArihnaWarmOutline\nimport androidx.core.view.WindowCompat\nimport androidx.core.view.WindowInsetsCompat\nimport androidx.core.view.WindowInsetsControllerCompat\n''', 'window imports')

new_screen = '''@Composable
fun QuranPlaceholderScreen(
    contentPadding: PaddingValues,
    onImmersiveChanged: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context.findActivity() }
    val corpus = remember(context) { QuranCorpus.load(context.applicationContext) }
    val surahs = remember(context) { MushafRepository.surahs(context.applicationContext) }
    var mode by remember { mutableStateOf(QuranReadingPrefs.mode(context)) }
    var selectedSurah by remember { mutableIntStateOf(1) }
    var requestedPage by remember { mutableIntStateOf(QuranReadingPrefs.lastPage(context)) }
    var explorerOpen by remember { mutableStateOf(false) }
    var fullscreenOpen by remember { mutableStateOf(false) }

    DisposableEffect(fullscreenOpen, activity, view) {
        onImmersiveChanged(fullscreenOpen)
        val insetsController = activity?.let { WindowCompat.getInsetsController(it.window, view) }
        if (fullscreenOpen) {
            insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            if (fullscreenOpen) {
                insetsController?.show(WindowInsetsCompat.Type.systemBars())
                onImmersiveChanged(false)
            }
        }
    }

    if (fullscreenOpen) {
        FullscreenMushafReader(
            startPage = requestedPage,
            onDismiss = { fullscreenOpen = false },
            onPageChanged = { pageIndex ->
                requestedPage = pageIndex
                MushafRepository.surahForPage(context, pageIndex)?.let { selectedSurah = it.number }
            },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ArihnaDawnTop, ArihnaDawnBottom)))
            .padding(contentPadding)
            .testTag("quran-reader"),
    ) {
        QuranReaderHeader(
            mode = mode,
            currentPage = requestedPage,
            surah = surahs.lastOrNull { it.pageNumber <= requestedPage + 1 },
            explorerOpen = explorerOpen,
            onModeChange = { selected ->
                mode = selected
                QuranReadingPrefs.setMode(context, selected)
                explorerOpen = false
            },
            onExplorer = { explorerOpen = !explorerOpen },
        )

        if (explorerOpen) {
            QuranExplorer(
                surahs = surahs,
                currentPage = requestedPage,
                onSelectPage = { pageIndex, surahNumber ->
                    requestedPage = pageIndex.coerceIn(0, 603)
                    if (surahNumber != null) selectedSurah = surahNumber
                    explorerOpen = false
                },
                modifier = Modifier.weight(1f),
            )
        } else if (mode == QuranReadingMode.HAFS_UTHMANI) {
            MushafBookReader(
                startPage = requestedPage,
                onPageChanged = { pageIndex ->
                    requestedPage = pageIndex
                    MushafRepository.surahForPage(context, pageIndex)?.let { selectedSurah = it.number }
                },
                onOpenFullscreen = { fullscreenOpen = true },
                modifier = Modifier.weight(1f),
            )
        } else {
            EasyQuranReader(
                corpus = corpus,
                selectedSurah = selectedSurah,
                onOpenExplorer = { explorerOpen = true },
                modifier = Modifier.weight(1f),
            )
        }

        QuranAttribution()
    }
}

'''
q = replace_between(q, '@Composable\nfun QuranPlaceholderScreen', '@Composable\nprivate fun QuranReaderHeader', new_screen, 'screen function')

new_book = '''@Composable
private fun MushafBookReader(
    startPage: Int,
    onPageChanged: (Int) -> Unit,
    onOpenFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = startPage.coerceIn(0, 603),
        pageCount = { 604 },
    )
    var bookmarkVersion by remember { mutableIntStateOf(0) }
    val currentPage = pagerState.currentPage
    val meta = remember(currentPage) { MushafRepository.metaForPage(context, currentPage) }
    val bookmarked = remember(currentPage, bookmarkVersion) {
        QuranReadingPrefs.isBookmarked(context, currentPage)
    }

    LaunchedEffect(startPage) {
        if (pagerState.currentPage != startPage.coerceIn(0, 603)) {
            pagerState.scrollToPage(startPage.coerceIn(0, 603))
        }
    }
    LaunchedEffect(currentPage) {
        QuranReadingPrefs.recordVisitedPage(context, currentPage)
        onPageChanged(currentPage)
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                appText("Juz ${meta.juz} · Hizb ${meta.hizb}", "الجزء ${toArabicIndic(meta.juz)} · الحزب ${toArabicIndic(meta.hizb)}"),
                color = ArihnaForest,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onOpenFullscreen,
                modifier = Modifier.heightIn(min = 52.dp).testTag("quran-reading-fullscreen"),
            ) {
                Icon(Icons.Rounded.Fullscreen, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(5.dp))
                Text(appText("Lettura", "قراءة"), fontWeight = FontWeight.Bold)
            }
            IconButton(
                onClick = {
                    QuranReadingPrefs.setBookmarked(context, currentPage, !bookmarked)
                    bookmarkVersion++
                },
                modifier = Modifier.size(52.dp).testTag("quran-bookmark-toggle"),
            ) {
                Icon(
                    if (bookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = appText("Segnalibro", "إشارة مرجعية"),
                    tint = if (bookmarked) ArihnaDawnGold else ArihnaGreen,
                    modifier = Modifier.size(27.dp),
                )
            }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f).testTag("quran-mushaf-rtl-pager"),
                beyondViewportPageCount = 1,
                pageSpacing = 3.dp,
            ) { page ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 1.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    NativeMushafPage(
                        page = page + 1,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(scaleX = 1.22f, scaleY = 1.22f)
                            .testTag("quran-mushaf-page-${page + 1}"),
                    )
                }
            }
        }

        Text(
            appText("Sfoglia da destra a sinistra · Lettura per schermo intero e zoom", "اقرأ من اليمين إلى اليسار · وضع القراءة للتكبير"),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = ArihnaMutedText,
            fontSize = 9.sp,
        )
    }
}

'''
q = replace_between(q, '@Composable\nprivate fun MushafBookReader', '@Composable\nprivate fun FullscreenMushafReader', new_book, 'book reader')

new_fullscreen = '''@Composable
private fun FullscreenMushafReader(
    startPage: Int,
    onDismiss: () -> Unit,
    onPageChanged: (Int) -> Unit,
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = startPage.coerceIn(0, 603),
        pageCount = { 604 },
    )
    var zoomed by remember { mutableStateOf(false) }
    var bookmarkVersion by remember { mutableIntStateOf(0) }
    val currentPage = pagerState.currentPage
    val bookmarked = remember(currentPage, bookmarkVersion) {
        QuranReadingPrefs.isBookmarked(context, currentPage)
    }

    BackHandler(onBack = onDismiss)

    LaunchedEffect(currentPage) {
        zoomed = false
        QuranReadingPrefs.recordVisitedPage(context, currentPage)
        onPageChanged(currentPage)
    }

    Surface(
        modifier = Modifier.fillMaxSize().testTag("quran-fullscreen-reader"),
        color = Color(0xFFFFFCF3),
    ) {
        Box(Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize().testTag("quran-fullscreen-rtl-pager"),
                    userScrollEnabled = !zoomed,
                    beyondViewportPageCount = 1,
                    pageSpacing = 2.dp,
                ) { page ->
                    ZoomableMushafPage(
                        page = page + 1,
                        onZoomedChange = { active ->
                            if (pagerState.currentPage == page) zoomed = active
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Surface(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
                color = ArihnaCream.copy(alpha = 0.94f),
                shape = RoundedCornerShape(999.dp),
                shadowElevation = 4.dp,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(horizontal = 4.dp),
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
                    Text(
                        appText("pag. ${currentPage + 1}", "صفحة ${toArabicIndic(currentPage + 1)}"),
                        color = ArihnaForest,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                    IconButton(
                        onClick = {
                            QuranReadingPrefs.setBookmarked(context, currentPage, !bookmarked)
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
    }
}

'''
q = replace_between(q, '@Composable\nprivate fun FullscreenMushafReader', '@Composable\nprivate fun ZoomableMushafPage', new_fullscreen, 'fullscreen reader')

q = replace_once(q, '''        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
''', '''        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
''', 'explorer chip spacing')

old_chip = '''@Composable
private fun ExplorerChip(
    value: QuranExplorerView,
    selected: QuranExplorerView,
    label: String,
    onClick: (QuranExplorerView) -> Unit,
) {
    FilterChip(
        selected = value == selected,
        onClick = { onClick(value) },
        label = { Text(label) },
    )
}
'''
new_chip = '''@Composable
private fun ExplorerChip(
    value: QuranExplorerView,
    selected: QuranExplorerView,
    label: String,
    onClick: (QuranExplorerView) -> Unit,
) {
    FilterChip(
        selected = value == selected,
        onClick = { onClick(value) },
        label = { Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
        modifier = Modifier
            .heightIn(min = 52.dp)
            .testTag("quran-explorer-${value.name.lowercase()}"),
    )
}
'''
q = replace_once(q, old_chip, new_chip, 'explorer chip')

q = replace_once(q, '        modifier = modifier,\n        shape = RoundedCornerShape(16.dp),\n', '        modifier = modifier.heightIn(min = 64.dp),\n        shape = RoundedCornerShape(16.dp),\n', 'shortcut min height')
q = replace_once(q, '            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),\n', '            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),\n', 'shortcut padding')
q = replace_once(q, '            Icon(icon, null, tint = if (selected) ArihnaGreen else ArihnaDawnGold, modifier = Modifier.size(21.dp))\n', '            Icon(icon, null, tint = if (selected) ArihnaGreen else ArihnaDawnGold, modifier = Modifier.size(25.dp))\n', 'shortcut icon')
q = replace_once(q, '                Text(title, color = ArihnaInk, fontWeight = FontWeight.Bold, fontSize = 11.sp)\n                Text(value, color = ArihnaMutedText, fontSize = 9.sp)\n', '                Text(title, color = ArihnaInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)\n                Text(value, color = ArihnaMutedText, fontSize = 10.sp)\n', 'shortcut text')

q = replace_once(q, '            IconButton(onClick = onBookmark, modifier = Modifier.size(38.dp)) {\n', '            IconButton(onClick = onBookmark, modifier = Modifier.size(52.dp).testTag("quran-surah-bookmark-${surah.number}")) {\n', 'surah bookmark target')
q = replace_once(q, '''        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ArihnaCream),
        border = BorderStroke(1.dp, ArihnaWarmOutline),
        shape = RoundedCornerShape(15.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
''', '''        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        colors = CardDefaults.cardColors(containerColor = ArihnaCream),
        border = BorderStroke(1.dp, ArihnaWarmOutline),
        shape = RoundedCornerShape(15.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
''', 'navigation row target')

# Also enlarge saved bookmark/recent rows (second matching Card block only by direct function-local replacement).
saved_old = '''private fun SavedPageRow(page: Int, onClick: () -> Unit) {
    val context = LocalContext.current
    val surah = remember(page) { MushafRepository.surahForPage(context, page) }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
'''
saved_new = '''private fun SavedPageRow(page: Int, onClick: () -> Unit) {
    val context = LocalContext.current
    val surah = remember(page) { MushafRepository.surahForPage(context, page) }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
'''
q = replace_once(q, saved_old, saved_new, 'saved row target')

# Tag the two large shortcuts where they are instantiated.
q = replace_once(q, '                modifier = Modifier.weight(1f),\n            ) { view = QuranExplorerView.BOOKMARKS }\n', '                modifier = Modifier.weight(1f).testTag("quran-shortcut-bookmarks"),\n            ) { view = QuranExplorerView.BOOKMARKS }\n', 'bookmark shortcut tag')
q = replace_once(q, '                modifier = Modifier.weight(1f),\n            ) { view = QuranExplorerView.RECENT }\n', '                modifier = Modifier.weight(1f).testTag("quran-shortcut-recent"),\n            ) { view = QuranExplorerView.RECENT }\n', 'recent shortcut tag')

helper = '''
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

'''
q = replace_once(q, 'internal fun toArabicIndic(value: Int): String = value.toString().map { c ->\n', helper + 'internal fun toArabicIndic(value: Int): String = value.toString().map { c ->\n', 'findActivity helper')
QURAN.write_text(q)

nav = NAV.read_text()
nav = replace_once(nav, 'import androidx.compose.runtime.getValue\nimport androidx.compose.runtime.remember\n', 'import androidx.compose.runtime.getValue\nimport androidx.compose.runtime.mutableStateOf\nimport androidx.compose.runtime.remember\nimport androidx.compose.runtime.setValue\n', 'nav state imports')
nav = replace_once(nav, '    val currentDestination = backStackEntry?.destination\n\n    Scaffold(\n', '    val currentDestination = backStackEntry?.destination\n    var quranImmersive by remember { mutableStateOf(false) }\n\n    Scaffold(\n', 'nav immersive state')
nav = replace_once(nav, '''        bottomBar = {
            NavigationBar(containerColor = AlbaNavBar, contentColor = AlbaNavIcon) {
''', '''        bottomBar = {
            if (!quranImmersive) NavigationBar(containerColor = AlbaNavBar, contentColor = AlbaNavIcon) {
''', 'hide bottom bar')
nav = replace_once(nav, '            composable(Destination.Quran.route) { QuranPlaceholderScreen(innerPadding) }\n', '''            composable(Destination.Quran.route) {
                QuranPlaceholderScreen(
                    contentPadding = innerPadding,
                    onImmersiveChanged = { quranImmersive = it },
                )
            }
''', 'quran callback')
NAV.write_text(nav)

TEST.parent.mkdir(parents=True, exist_ok=True)
TEST.write_text('''package com.archimedeprojects.arihna.feature.quran

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QuranFullscreenAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readingButtonOpensAndClosesRealFullscreenReader() {
        var immersive = false
        composeRule.setContent {
            QuranPlaceholderScreen(
                contentPadding = PaddingValues(0.dp),
                onImmersiveChanged = { immersive = it },
            )
        }

        composeRule.onNodeWithTag("quran-mode-hafs").performClick()
        composeRule.onNodeWithTag("quran-reading-fullscreen").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("quran-fullscreen-reader").assertIsDisplayed()
        composeRule.onNodeWithTag("quran-fullscreen-close").assertIsDisplayed()
        composeRule.runOnIdle { assertTrue(immersive) }

        composeRule.onNodeWithTag("quran-fullscreen-close").performClick()
        composeRule.onNodeWithTag("quran-fullscreen-reader").assertDoesNotExist()
        composeRule.onNodeWithTag("quran-reading-fullscreen").assertIsDisplayed()
        composeRule.runOnIdle { assertFalse(immersive) }
    }

    @Test
    fun indexLargeTargetsAreClickable() {
        composeRule.setContent { QuranPlaceholderScreen(PaddingValues(0.dp)) }

        composeRule.onNodeWithTag("quran-surah-selector").performClick()
        composeRule.onNodeWithTag("quran-explorer-juz").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("quran-explorer-hizb").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("quran-shortcut-bookmarks").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("quran-shortcut-recent").assertIsDisplayed().performClick()
    }
}
''')

# Structural acceptance assertions.
q2 = QURAN.read_text()
nav2 = NAV.read_text()
assert 'Dialog(' not in q2
assert 'quran-fullscreen-reader' in q2
assert 'BackHandler(onBack = onDismiss)' in q2
assert 'Modifier.size(52.dp).testTag("quran-bookmark-toggle")' in q2
assert '.heightIn(min = 52.dp)\n            .testTag("quran-explorer-' in q2
assert 'modifier = modifier.heightIn(min = 64.dp)' in q2
assert 'onImmersiveChanged = { quranImmersive = it }' in nav2
assert 'if (!quranImmersive) NavigationBar' in nav2
print('S25 Quran fullscreen/touch patch applied successfully')
