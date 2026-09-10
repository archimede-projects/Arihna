package com.archimedeprojects.arihna.feature.quran

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.archimedeprojects.arihna.core.i18n.appText
import com.archimedeprojects.arihna.core.ui.theme.ArihnaCream
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnBottom
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnGold
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnTop
import com.archimedeprojects.arihna.core.ui.theme.ArihnaForest
import com.archimedeprojects.arihna.core.ui.theme.ArihnaGreen
import com.archimedeprojects.arihna.core.ui.theme.ArihnaInk
import com.archimedeprojects.arihna.core.ui.theme.ArihnaMutedText
import com.archimedeprojects.arihna.core.ui.theme.ArihnaSage
import com.archimedeprojects.arihna.core.ui.theme.ArihnaWarmOutline
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.launch

enum class QuranReadingMode { EASY, HAFS_UTHMANI, HAFS_TAJWID, WARSH_NAFI }

internal fun QuranReadingMode.riwayaOrNull(): QuranRiwaya? = when (this) {
    QuranReadingMode.HAFS_UTHMANI, QuranReadingMode.HAFS_TAJWID -> QuranRiwaya.HAFS
    QuranReadingMode.WARSH_NAFI -> QuranRiwaya.WARSH
    QuranReadingMode.EASY -> null
}

enum class MushafVisualStyle { CLASSIC, CLEAN }

private const val MUSHAF_PAGE_ASPECT_RATIO = 510.23599f / 729.448f

private enum class QuranExplorerView { SURAHS, JUZ, HIZB, BOOKMARKS, RECENT }

@Composable
fun QuranPlaceholderScreen(
    contentPadding: PaddingValues,
    onImmersiveChanged: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = remember(context) { context.findActivity() }
    val corpus = remember(context) { QuranCorpus.load(context.applicationContext) }
    var mode by remember { mutableStateOf(QuranReadingPrefs.mode(context)) }
    val activeRiwaya = mode.riwayaOrNull() ?: QuranRiwaya.HAFS
    val surahs = remember(context, activeRiwaya) {
        MushafRepository.surahs(context.applicationContext, activeRiwaya)
    }
    var visualStyle by remember { mutableStateOf(QuranReadingPrefs.visualStyle(context)) }
    var selectedSurah by remember {
        mutableIntStateOf(
            if (mode == QuranReadingMode.HAFS_TAJWID) QuranReadingPrefs.lastTajwidSurah(context) else 1,
        )
    }
    var requestedPage by remember {
        mutableIntStateOf(
            if (mode == QuranReadingMode.HAFS_TAJWID) {
                surahs.firstOrNull { it.number == selectedSurah }?.pageNumber?.minus(1)?.coerceIn(0, 603) ?: 0
            } else {
                QuranReadingPrefs.lastPage(context, activeRiwaya)
            },
        )
    }
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
        if (mode == QuranReadingMode.HAFS_TAJWID) {
            FullscreenTajwidReader(
                corpus = corpus,
                selectedSurah = selectedSurah,
                onDismiss = { fullscreenOpen = false },
            )
        } else {
            FullscreenMushafReader(
                startPage = requestedPage,
                style = visualStyle,
                riwaya = activeRiwaya,
                onDismiss = { fullscreenOpen = false },
                onPageChanged = { pageIndex ->
                    requestedPage = pageIndex
                    MushafRepository.surahForPage(context, activeRiwaya, pageIndex)?.let { selectedSurah = it.number }
                },
            )
        }
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
                if (selected == QuranReadingMode.HAFS_TAJWID) {
                    selectedSurah = QuranReadingPrefs.lastTajwidSurah(context)
                    requestedPage = MushafRepository.surahs(context, QuranRiwaya.HAFS)
                        .firstOrNull { it.number == selectedSurah }
                        ?.pageNumber
                        ?.minus(1)
                        ?.coerceIn(0, 603)
                        ?: 0
                } else {
                    selected.riwayaOrNull()?.let { riwaya ->
                        requestedPage = QuranReadingPrefs.lastPage(context, riwaya)
                        selectedSurah = MushafRepository.surahForPage(context, riwaya, requestedPage)?.number ?: 1
                    }
                }
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
                riwaya = activeRiwaya,
                tajwidMode = mode == QuranReadingMode.HAFS_TAJWID,
                onSelectPage = { pageIndex, surahNumber ->
                    requestedPage = pageIndex.coerceIn(0, 603)
                    selectedSurah = if (mode == QuranReadingMode.HAFS_TAJWID) {
                        surahNumber
                            ?: MushafRepository.surahForPage(context, QuranRiwaya.HAFS, requestedPage)?.number
                            ?: selectedSurah
                    } else {
                        surahNumber ?: selectedSurah
                    }
                    explorerOpen = false
                },
                modifier = Modifier.weight(1f),
            )
        } else if (mode == QuranReadingMode.HAFS_TAJWID) {
            TajwidQuranReader(
                corpus = corpus,
                selectedSurah = selectedSurah,
                onOpenExplorer = { explorerOpen = true },
                onOpenFullscreen = { fullscreenOpen = true },
                modifier = Modifier.weight(1f),
            )
        } else if (mode == QuranReadingMode.HAFS_UTHMANI || mode == QuranReadingMode.WARSH_NAFI) {
            MushafBookReader(
                startPage = requestedPage,
                riwaya = activeRiwaya,
                style = visualStyle,
                onStyleChange = { selected ->
                    visualStyle = selected
                    QuranReadingPrefs.setVisualStyle(context, selected)
                },
                onPageChanged = { pageIndex ->
                    requestedPage = pageIndex
                    MushafRepository.surahForPage(context, activeRiwaya, pageIndex)?.let { selectedSurah = it.number }
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

        QuranAttribution(mode)
    }
}

@Composable
private fun QuranReaderHeader(
    mode: QuranReadingMode,
    currentPage: Int,
    surah: MushafSurah?,
    explorerOpen: Boolean,
    onModeChange: (QuranReadingMode) -> Unit,
    onExplorer: () -> Unit,
) {
    Surface(
        color = ArihnaCream.copy(alpha = 0.98f),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, ArihnaWarmOutline.copy(alpha = 0.7f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        appText("Il Corano", "القرآن الكريم"),
                        color = ArihnaForest,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    val pageLabel = appText(
                        "pag. ${currentPage + 1}",
                        "صفحة ${toArabicIndic(currentPage + 1)}",
                    )
                    val readingLocation = if (explorerOpen) {
                        appText("Scegli dove leggere", "اختر موضع القراءة")
                    } else {
                        listOfNotNull(surah?.nameArabic?.takeIf { it.isNotBlank() }, pageLabel)
                            .joinToString("  •  ")
                    }
                    Text(
                        readingLocation,
                        color = ArihnaMutedText,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                OutlinedButton(
                    onClick = onExplorer,
                    border = BorderStroke(1.dp, ArihnaWarmOutline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ArihnaForest),
                    modifier = Modifier.testTag("quran-surah-selector"),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Icon(if (explorerOpen) Icons.Rounded.MenuBook else Icons.Rounded.Search, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(appText(if (explorerOpen) "Lettura" else "Indice", if (explorerOpen) "المصحف" else "الفهرس"), fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = mode == QuranReadingMode.HAFS_UTHMANI,
                    onClick = { onModeChange(QuranReadingMode.HAFS_UTHMANI) },
                    label = { Text(appText("Muṣḥaf Ḥafṣ", "مصحف حفص")) },
                    leadingIcon = { Icon(Icons.Rounded.AutoStories, null, Modifier.size(17.dp)) },
                    modifier = Modifier.testTag("quran-mode-hafs"),
                )
                FilterChip(
                    selected = mode == QuranReadingMode.HAFS_TAJWID,
                    onClick = { onModeChange(QuranReadingMode.HAFS_TAJWID) },
                    label = { Text(appText("Ḥafṣ Tajwid · Beta", "حفص تجويد · تجريبي")) },
                    leadingIcon = { Icon(Icons.Rounded.AutoStories, null, Modifier.size(17.dp)) },
                    modifier = Modifier.testTag("quran-mode-tajwid"),
                )
                FilterChip(
                    selected = mode == QuranReadingMode.WARSH_NAFI,
                    onClick = { onModeChange(QuranReadingMode.WARSH_NAFI) },
                    label = { Text(appText("Muṣḥaf Warsh", "مصحف ورش")) },
                    leadingIcon = { Icon(Icons.Rounded.AutoStories, null, Modifier.size(17.dp)) },
                    modifier = Modifier.testTag("quran-mode-warsh"),
                )
                FilterChip(
                    selected = mode == QuranReadingMode.EASY,
                    onClick = { onModeChange(QuranReadingMode.EASY) },
                    label = { Text(appText("Facile", "قراءة سهلة")) },
                    modifier = Modifier.testTag("quran-mode-easy"),
                )
            }
        }
    }
}

    @Composable
    private fun MushafBookReader(
        startPage: Int,
        riwaya: QuranRiwaya,
        style: MushafVisualStyle,
        onStyleChange: (MushafVisualStyle) -> Unit,
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
        val meta = remember(currentPage, riwaya) { MushafRepository.metaForPage(context, riwaya, currentPage) }
        val surah = remember(currentPage, riwaya) { MushafRepository.surahForPage(context, riwaya, currentPage) }
        val bookmarked = remember(currentPage, bookmarkVersion, riwaya) {
            QuranReadingPrefs.isBookmarked(context, riwaya, currentPage)
        }

        LaunchedEffect(startPage) {
            if (pagerState.currentPage != startPage.coerceIn(0, 603)) {
                pagerState.scrollToPage(startPage.coerceIn(0, 603))
            }
        }
        LaunchedEffect(currentPage) {
            QuranReadingPrefs.recordVisitedPage(context, riwaya, currentPage)
            onPageChanged(currentPage)
        }

        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .testTag("quran-mushaf-riwaya-${riwaya.name.lowercase()}"),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .background(ArihnaDawnTop.copy(alpha = 0.98f))
                    .zIndex(2f)
                    .testTag("quran-mushaf-toolbar"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        surah?.nameArabic ?: appText("Corano", "القرآن"),
                        color = ArihnaForest,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val pageContext = if (meta != null) {
                        appText(
                            "pag. ${currentPage + 1} · Juz ${meta.juz} · Hizb ${meta.hizb}",
                            "صفحة ${toArabicIndic(currentPage + 1)} · الجزء ${toArabicIndic(meta.juz)} · الحزب ${toArabicIndic(meta.hizb)}",
                        )
                    } else {
                        appText("pag. ${currentPage + 1} · Warsh", "صفحة ${toArabicIndic(currentPage + 1)} · ورش")
                    }
                    Text(
                        pageContext,
                        color = ArihnaMutedText,
                        fontSize = 9.sp,
                        maxLines = 1,
                    )
                }
                MushafStyleMenu(style = style, onStyleChange = onStyleChange)
                TextButton(
                    onClick = onOpenFullscreen,
                    modifier = Modifier.heightIn(min = 52.dp).testTag("quran-reading-fullscreen"),
                    contentPadding = PaddingValues(horizontal = 7.dp, vertical = 5.dp),
                ) {
                    Icon(Icons.Rounded.Fullscreen, null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(appText("Lettura", "قراءة"), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                IconButton(
                    onClick = {
                        QuranReadingPrefs.setBookmarked(context, riwaya, currentPage, !bookmarked)
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 6.dp)
                    .clipToBounds()
                    .zIndex(0f)
                    .testTag("quran-mushaf-preview-container"),
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize().testTag("quran-mushaf-rtl-pager"),
                        beyondViewportPageCount = 1,
                        pageSpacing = 2.dp,
                    ) { page ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 2.dp, vertical = 1.dp)
                                .testTag("quran-mushaf-page-${page + 1}"),
                            contentAlignment = Alignment.Center,
                        ) {
                            PremiumMushafPageFrame(
                                page = page + 1,
                                riwaya = riwaya,
                                style = style,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            Text(
                appText(
                    "Sfoglia da destra a sinistra · Lettura apre la modalità immersiva",
                    "اقرأ من اليمين إلى اليسار · وضع القراءة يفتح العرض الغامر",
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = ArihnaMutedText,
                fontSize = 9.sp,
            )
        }
    }

    @Composable
    internal fun FullscreenMushafReader(
        startPage: Int,
        style: MushafVisualStyle,
        riwaya: QuranRiwaya,
        onDismiss: () -> Unit,
        onPageChanged: (Int) -> Unit,
        topBarInsets: WindowInsets = WindowInsets.displayCutout.union(WindowInsets.statusBars),
    ) {
        val context = LocalContext.current
        val pagerState = rememberPagerState(
            initialPage = startPage.coerceIn(0, 603),
            pageCount = { 604 },
        )
        var zoomed by remember { mutableStateOf(false) }
        var chromeVisible by remember { mutableStateOf(true) }
        var bookmarkVersion by remember { mutableIntStateOf(0) }
        val currentPage = pagerState.currentPage
        val meta = remember(currentPage, riwaya) { MushafRepository.metaForPage(context, riwaya, currentPage) }
        val surah = remember(currentPage, riwaya) { MushafRepository.surahForPage(context, riwaya, currentPage) }
        val bookmarked = remember(currentPage, bookmarkVersion, riwaya) {
            QuranReadingPrefs.isBookmarked(context, riwaya, currentPage)
        }

        BackHandler(onBack = onDismiss)

        LaunchedEffect(currentPage) {
            zoomed = false
            QuranReadingPrefs.recordVisitedPage(context, riwaya, currentPage)
            onPageChanged(currentPage)
        }

        Surface(
            modifier = Modifier.fillMaxSize().testTag("quran-fullscreen-reader"),
            color = if (style == MushafVisualStyle.CLASSIC) Color(0xFFF4EBD8) else Color(0xFFFFFCF3),
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
                            riwaya = riwaya,
                            style = style,
                            onZoomedChange = { active ->
                                if (pagerState.currentPage == page) zoomed = active
                            },
                            onPageTap = {
                                if (pagerState.currentPage == page) chromeVisible = !chromeVisible
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                if (chromeVisible) {
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
                }
            }
        }
    }

    @Composable
    private fun ZoomableMushafPage(
        page: Int,
        riwaya: QuranRiwaya,
        style: MushafVisualStyle,
        onZoomedChange: (Boolean) -> Unit,
        onPageTap: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        var scale by remember(page) { mutableFloatStateOf(1f) }
        var offset by remember(page) { mutableStateOf(Offset.Zero) }

        Box(
            modifier = modifier
                .pointerInput(page) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var maxPointers = 1
                        var travel = 0f
                        do {
                            val event = awaitPointerEvent()
                            val pressedPointers = event.changes.count { it.pressed }
                            maxPointers = maxOf(maxPointers, pressedPointers)
                            val pan = event.calculatePan()
                            travel += pan.getDistance()
                            if (pressedPointers >= 2 || scale > 1.01f) {
                                val nextScale = (scale * event.calculateZoom()).coerceIn(1f, 5f)
                                scale = nextScale
                                offset = if (nextScale <= 1.01f) Offset.Zero else offset + pan
                                onZoomedChange(nextScale > 1.01f)
                                event.changes.forEach { it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                        if (maxPointers == 1 && travel < 12f) onPageTap()
                    }
                }
                .semantics {
                    onClick {
                        onPageTap()
                        true
                    }
                }
                .testTag("quran-zoomable-page-$page"),
            contentAlignment = Alignment.Center,
        ) {
            PremiumMushafPageFrame(
                page = page,
                riwaya = riwaya,
                style = style,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (style == MushafVisualStyle.CLASSIC) 5.dp else 1.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
            )
        }
    }

    @Composable
    private fun MushafStyleMenu(
        style: MushafVisualStyle,
        onStyleChange: (MushafVisualStyle) -> Unit,
    ) {
        var expanded by remember { mutableStateOf(false) }
        Box {
            TextButton(
                onClick = { expanded = true },
                modifier = Modifier.heightIn(min = 52.dp).testTag("quran-style-menu"),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 5.dp),
            ) {
                Text(
                    if (style == MushafVisualStyle.CLASSIC) appText("Classico", "كلاسيكي") else appText("Pulito", "بسيط"),
                    color = ArihnaForest,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = {
                        Text(
                            appText("Classico", "كلاسيكي"),
                            fontWeight = if (style == MushafVisualStyle.CLASSIC) FontWeight.ExtraBold else FontWeight.Medium,
                        )
                    },
                    onClick = {
                        onStyleChange(MushafVisualStyle.CLASSIC)
                        expanded = false
                    },
                    modifier = Modifier.testTag("quran-style-classic"),
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            appText("Pulito", "بسيط"),
                            fontWeight = if (style == MushafVisualStyle.CLEAN) FontWeight.ExtraBold else FontWeight.Medium,
                        )
                    },
                    onClick = {
                        onStyleChange(MushafVisualStyle.CLEAN)
                        expanded = false
                    },
                    modifier = Modifier.testTag("quran-style-clean"),
                )
            }
        }
    }

    @Composable
    private fun PremiumMushafPageFrame(
        page: Int,
        riwaya: QuranRiwaya,
        style: MushafVisualStyle,
        modifier: Modifier = Modifier,
    ) {
        val classic = style == MushafVisualStyle.CLASSIC
        Surface(
            modifier = modifier
                .aspectRatio(if (riwaya == QuranRiwaya.WARSH) 345f / 550f else MUSHAF_PAGE_ASPECT_RATIO)
                .testTag("quran-premium-page-frame"),
            color = if (classic) Color(0xFFFFFAEC) else Color(0xFFFFFEF9),
            shape = RoundedCornerShape(if (classic) 16.dp else 6.dp),
            border = if (classic) {
                BorderStroke(1.dp, ArihnaDawnGold.copy(alpha = 0.45f))
            } else {
                BorderStroke(1.dp, ArihnaWarmOutline.copy(alpha = 0.35f))
            },
            shadowElevation = if (classic) 5.dp else 0.dp,
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(if (classic) 3.dp else 0.dp),
                contentAlignment = Alignment.Center,
            ) {
                NativeMushafPage(page = page, riwaya = riwaya, modifier = Modifier.fillMaxSize())
            }
        }
    }

@Composable
private fun QuranExplorer(
    surahs: List<MushafSurah>,
    currentPage: Int,
    riwaya: QuranRiwaya,
    tajwidMode: Boolean,
    onSelectPage: (Int, Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var view by remember { mutableStateOf(QuranExplorerView.SURAHS) }
    var bookmarkVersion by remember { mutableIntStateOf(0) }
    val bookmarked = remember(bookmarkVersion, currentPage, riwaya, tajwidMode) {
        if (tajwidMode) QuranReadingPrefs.tajwidBookmarkedSurahs(context).toList()
        else QuranReadingPrefs.bookmarkedPages(context, riwaya)
    }
    val recent = remember(currentPage, riwaya, tajwidMode) {
        if (tajwidMode) QuranReadingPrefs.recentTajwidSurahs(context)
        else QuranReadingPrefs.recentPages(context, riwaya)
    }
    val supportsBoundaries = riwaya == QuranRiwaya.HAFS
    val juzPages = remember(context, riwaya) { MushafRepository.juzStartPages(context, riwaya) }
    val hizbPages = remember(context, riwaya) { MushafRepository.hizbStartPages(context, riwaya) }
    val filteredSurahs = remember(query, surahs) {
        val needle = query.trim().lowercase()
        if (needle.isBlank()) surahs else surahs.filter {
            it.number.toString() == needle ||
                it.nameArabic.contains(query.trim(), ignoreCase = true) ||
                it.nameEnglish.lowercase().contains(needle) ||
                it.nameTranslation.lowercase().contains(needle)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            appText("Indice del Corano", "فهرس القرآن"),
            color = ArihnaForest,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 17.sp,
            modifier = Modifier.testTag("quran-global-index-title"),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ExplorerChip(QuranExplorerView.SURAHS, view, appText("Sure", "السور")) { view = it }
            if (supportsBoundaries) {
                ExplorerChip(QuranExplorerView.JUZ, view, appText("Juz", "الأجزاء")) { view = it }
                ExplorerChip(QuranExplorerView.HIZB, view, appText("Hizb", "الأحزاب")) { view = it }
            }
        }
        if (!supportsBoundaries) {
            Text(
                appText(
                    "Warsh: Juz/Hizb non mostrati finché non è integrato un indice autorevole specifico.",
                    "ورش: لا نعرض حدود الجزء والحزب حتى يتوفر فهرس موثوق خاص بهذه الرواية.",
                ),
                color = ArihnaMutedText,
                fontSize = 10.sp,
                modifier = Modifier.testTag("quran-warsh-boundaries-unavailable"),
            )
        }
        if (view == QuranExplorerView.SURAHS) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().testTag("quran-surah-search"),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                placeholder = { Text(appText("Cerca una sura…", "ابحث في السور…")) },
                shape = RoundedCornerShape(18.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ExplorerShortcut(
                title = appText("Segnalibri", "المحفوظات"),
                value = bookmarked.size.toString(),
                icon = if (view == QuranExplorerView.BOOKMARKS) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                selected = view == QuranExplorerView.BOOKMARKS,
                modifier = Modifier.weight(1f).testTag("quran-shortcut-bookmarks"),
            ) { view = QuranExplorerView.BOOKMARKS }
            ExplorerShortcut(
                title = appText("Recenti", "الأخيرة"),
                value = recent.size.toString(),
                icon = Icons.Rounded.History,
                selected = view == QuranExplorerView.RECENT,
                modifier = Modifier.weight(1f).testTag("quran-shortcut-recent"),
            ) { view = QuranExplorerView.RECENT }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).testTag("quran-explorer-list"),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when (view) {
                QuranExplorerView.SURAHS -> items(filteredSurahs, key = { it.number }) { surah ->
                    SurahRow(
                        surah = surah,
                        bookmarked = if (tajwidMode) surah.number in bookmarked else (surah.pageNumber - 1) in bookmarked,
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
                        },
                        onClick = { onSelectPage(surah.pageNumber - 1, surah.number) },
                    )
                }
                QuranExplorerView.JUZ -> items(juzPages.indices.toList(), key = { it }) { index ->
                    NavigationBoundaryRow(
                        number = index + 1,
                        label = appText("Juz", "الجزء"),
                        page = juzPages[index],
                        onClick = { onSelectPage(juzPages[index], null) },
                    )
                }
                QuranExplorerView.HIZB -> items(hizbPages.indices.toList(), key = { it }) { index ->
                    NavigationBoundaryRow(
                        number = index + 1,
                        label = appText("Hizb", "الحزب"),
                        page = hizbPages[index],
                        onClick = { onSelectPage(hizbPages[index], null) },
                    )
                }
                QuranExplorerView.BOOKMARKS -> {
                    if (tajwidMode) {
                        items(bookmarked.sorted(), key = { "tajwid-bookmark-$it" }) { surahNumber ->
                            surahs.firstOrNull { it.number == surahNumber }?.let { surah ->
                                SurahRow(
                                    surah = surah,
                                    bookmarked = true,
                                    onBookmark = {
                                        QuranReadingPrefs.setTajwidSurahBookmarked(context, surah.number, false)
                                        bookmarkVersion++
                                    },
                                    onClick = { onSelectPage(surah.pageNumber - 1, surah.number) },
                                )
                            }
                        }
                    } else {
                        items(bookmarked.sorted(), key = { it }) { page ->
                            SavedPageRow(page = page, riwaya = riwaya, onClick = { onSelectPage(page, null) })
                        }
                    }
                }
                QuranExplorerView.RECENT -> {
                    if (tajwidMode) {
                        items(recent, key = { "tajwid-recent-$it" }) { surahNumber ->
                            surahs.firstOrNull { it.number == surahNumber }?.let { surah ->
                                SurahRow(
                                    surah = surah,
                                    bookmarked = surah.number in bookmarked,
                                    onBookmark = {
                                        QuranReadingPrefs.setTajwidSurahBookmarked(
                                            context,
                                            surah.number,
                                            surah.number !in bookmarked,
                                        )
                                        bookmarkVersion++
                                    },
                                    onClick = { onSelectPage(surah.pageNumber - 1, surah.number) },
                                )
                            }
                        }
                    } else {
                        items(recent, key = { it }) { page ->
                            SavedPageRow(page = page, riwaya = riwaya, onClick = { onSelectPage(page, null) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
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

@Composable
private fun ExplorerShortcut(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 64.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) ArihnaSage else ArihnaCream),
        border = BorderStroke(1.dp, if (selected) ArihnaGreen else ArihnaWarmOutline),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = if (selected) ArihnaGreen else ArihnaDawnGold, modifier = Modifier.size(25.dp))
            Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = ArihnaInk, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(value, color = ArihnaMutedText, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SurahRow(
    surah: MushafSurah,
    bookmarked: Boolean,
    onBookmark: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ArihnaCream),
        border = BorderStroke(1.dp, ArihnaWarmOutline.copy(alpha = 0.72f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                shape = CircleShape,
                color = ArihnaSage,
                border = BorderStroke(1.dp, ArihnaDawnGold.copy(alpha = 0.8f)),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(toArabicIndic(surah.number), color = ArihnaForest, fontWeight = FontWeight.ExtraBold)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(surah.nameArabic, color = ArihnaForest, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                Text(
                    "${surah.nameEnglish} · ${appText("pag.", "صفحة")} ${surah.pageNumber}",
                    color = ArihnaMutedText,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onBookmark, modifier = Modifier.size(52.dp).testTag("quran-surah-bookmark-${surah.number}")) {
                Icon(
                    if (bookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = appText("Segnalibro", "إشارة مرجعية"),
                    tint = if (bookmarked) ArihnaDawnGold else ArihnaGreen,
                )
            }
            Icon(Icons.Rounded.KeyboardArrowLeft, null, tint = ArihnaMutedText)
        }
    }
}

@Composable
private fun NavigationBoundaryRow(number: Int, label: String, page: Int, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        colors = CardDefaults.cardColors(containerColor = ArihnaCream),
        border = BorderStroke(1.dp, ArihnaWarmOutline),
        shape = RoundedCornerShape(15.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = ArihnaGreen) {
                Text(
                    number.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("$label $number", color = ArihnaInk, fontWeight = FontWeight.Bold)
                Text(appText("Inizio a pagina ${page + 1}", "يبدأ من الصفحة ${toArabicIndic(page + 1)}"), color = ArihnaMutedText, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SavedPageRow(page: Int, riwaya: QuranRiwaya, onClick: () -> Unit) {
    val context = LocalContext.current
    val surah = remember(page, riwaya) { MushafRepository.surahForPage(context, riwaya, page) }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        colors = CardDefaults.cardColors(containerColor = ArihnaCream),
        border = BorderStroke(1.dp, ArihnaWarmOutline),
        shape = RoundedCornerShape(15.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Bookmark, null, tint = ArihnaDawnGold)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(surah?.nameArabic ?: appText("Corano", "القرآن"), color = ArihnaForest, fontWeight = FontWeight.Bold)
                Text(appText("Pagina ${page + 1}", "صفحة ${toArabicIndic(page + 1)}"), color = ArihnaMutedText, fontSize = 10.sp)
            }
            Icon(Icons.Rounded.KeyboardArrowLeft, null, tint = ArihnaMutedText)
        }
    }
}

@Composable
private fun TajwidQuranReader(
    corpus: QuranCorpus,
    selectedSurah: Int,
    onOpenExplorer: () -> Unit,
    onOpenFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val ayahs = remember(corpus, selectedSurah) { corpus.ayahsForSurah(selectedSurah) }
    var bookmarkVersion by remember { mutableIntStateOf(0) }
    var fontScale by remember { mutableFloatStateOf(1f) }
    val bookmarked = remember(selectedSurah, bookmarkVersion) {
        QuranReadingPrefs.isTajwidSurahBookmarked(context, selectedSurah)
    }

    LaunchedEffect(selectedSurah) {
        QuranReadingPrefs.recordVisitedTajwidSurah(context, selectedSurah)
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).testTag("quran-tajwid-beta-reader"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            color = ArihnaCream,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, ArihnaDawnGold.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth().testTag("quran-tajwid-beta-disclaimer"),
        ) {
            Text(
                appText(
                    "Colorazione tajwid · Beta / regole principali. Generata localmente: può non coprire tutte le regole del Mushaf Tajwid tradizionale.",
                    "تلوين التجويد · تجريبي / القواعد الرئيسية. يُولَّد محليًا وقد لا يشمل جميع قواعد مصحف التجويد التقليدي.",
                ),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                color = ArihnaMutedText,
                fontSize = 10.sp,
                lineHeight = 15.sp,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    appText("Sura $selectedSurah · Tajwid Beta", "سورة ${toArabicIndic(selectedSurah)} · تجويد تجريبي"),
                    color = ArihnaForest,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                )
                TextButton(onClick = onOpenExplorer, modifier = Modifier.testTag("quran-tajwid-index")) {
                    Text(appText("Cambia sura / indice", "تغيير السورة / الفهرس"), fontSize = 10.sp)
                }
            }
            TextButton(
                onClick = { fontScale = (fontScale - 0.1f).coerceAtLeast(0.8f) },
                modifier = Modifier.testTag("quran-tajwid-zoom-out"),
            ) { Text("A−", fontWeight = FontWeight.Bold) }
            TextButton(
                onClick = { fontScale = (fontScale + 0.1f).coerceAtMost(1.8f) },
                modifier = Modifier.testTag("quran-tajwid-zoom-in"),
            ) { Text("A+", fontWeight = FontWeight.Bold) }
            TextButton(
                onClick = onOpenFullscreen,
                modifier = Modifier.testTag("quran-tajwid-fullscreen"),
            ) {
                Icon(Icons.Rounded.Fullscreen, null, Modifier.size(19.dp))
                Spacer(Modifier.width(3.dp))
                Text(appText("Lettura", "قراءة"), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(
                onClick = {
                    QuranReadingPrefs.setTajwidSurahBookmarked(context, selectedSurah, !bookmarked)
                    bookmarkVersion++
                },
                modifier = Modifier.size(52.dp).testTag("quran-tajwid-bookmark"),
            ) {
                Icon(
                    if (bookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = appText("Segnalibro Tajwid", "إشارة تجويد مرجعية"),
                    tint = if (bookmarked) ArihnaDawnGold else ArihnaGreen,
                )
            }
        }

        TajwidLegend()

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f).testTag("quran-tajwid-ayah-list"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ayahs, key = { "tajwid-${it.surah}:${it.ayah}" }) { ayah ->
                    TajwidAyahCard(ayah = ayah, corpus = corpus, fontScale = fontScale)
                }
            }
        }
    }
}

@Composable
private fun FullscreenTajwidReader(
    corpus: QuranCorpus,
    selectedSurah: Int,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val ayahs = remember(corpus, selectedSurah) { corpus.ayahsForSurah(selectedSurah) }
    var fontScale by remember { mutableFloatStateOf(1.15f) }
    var bookmarkVersion by remember { mutableIntStateOf(0) }
    val bookmarked = remember(selectedSurah, bookmarkVersion) {
        QuranReadingPrefs.isTajwidSurahBookmarked(context, selectedSurah)
    }

    BackHandler(onBack = onDismiss)
    LaunchedEffect(selectedSurah) { QuranReadingPrefs.recordVisitedTajwidSurah(context, selectedSurah) }

    Surface(
        modifier = Modifier.fillMaxSize().testTag("quran-tajwid-fullscreen-reader"),
        color = Color(0xFFFFFCF3),
    ) {
        Column(Modifier.fillMaxSize()) {
            Surface(
                color = ArihnaCream.copy(alpha = 0.97f),
                border = BorderStroke(1.dp, ArihnaWarmOutline),
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.displayCutout.union(WindowInsets.statusBars)),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(52.dp).testTag("quran-tajwid-fullscreen-close")) {
                        Icon(Icons.Rounded.Close, appText("Chiudi", "إغلاق"), tint = ArihnaForest)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            appText("Ḥafṣ Tajwid · Beta", "حفص تجويد · تجريبي"),
                            color = ArihnaForest,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            appText("Regole principali · Sura $selectedSurah", "القواعد الرئيسية · سورة ${toArabicIndic(selectedSurah)}"),
                            color = ArihnaMutedText,
                            fontSize = 9.sp,
                        )
                    }
                    TextButton(
                        onClick = { fontScale = (fontScale - 0.1f).coerceAtLeast(0.8f) },
                        modifier = Modifier.testTag("quran-tajwid-fullscreen-zoom-out"),
                    ) { Text("A−") }
                    TextButton(
                        onClick = { fontScale = (fontScale + 0.1f).coerceAtMost(2.2f) },
                        modifier = Modifier.testTag("quran-tajwid-fullscreen-zoom-in"),
                    ) { Text("A+") }
                    IconButton(
                        onClick = {
                            QuranReadingPrefs.setTajwidSurahBookmarked(context, selectedSurah, !bookmarked)
                            bookmarkVersion++
                        },
                        modifier = Modifier.size(52.dp).testTag("quran-tajwid-fullscreen-bookmark"),
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
                appText(
                    "Beta: colorazione algoritmica delle regole principali, non Mushaf Tajwid completo.",
                    "تجريبي: تلوين خوارزمي للقواعد الرئيسية، وليس مصحف تجويد كاملًا.",
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp).testTag("quran-tajwid-fullscreen-disclaimer"),
                textAlign = TextAlign.Center,
                color = ArihnaMutedText,
                fontSize = 9.sp,
            )

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .pointerInput(selectedSurah) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                do {
                                    val event = awaitPointerEvent()
                                    if (event.changes.count { it.pressed } >= 2) {
                                        fontScale = (fontScale * event.calculateZoom()).coerceIn(0.8f, 2.2f)
                                        event.changes.forEach { it.consume() }
                                    }
                                } while (event.changes.any { it.pressed })
                            }
                        }
                        .padding(horizontal = 12.dp)
                        .testTag("quran-tajwid-fullscreen-content"),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    items(ayahs, key = { "tajwid-full-${it.surah}:${it.ayah}" }) { ayah ->
                        TajwidAyahCard(ayah = ayah, corpus = corpus, fontScale = fontScale)
                    }
                }
            }
        }
    }
}

@Composable
private fun TajwidLegend() {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("quran-tajwid-legend"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TajwidRule.entries.forEach { rule ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ArihnaCream,
                border = BorderStroke(1.dp, tajwidColor(rule).copy(alpha = 0.65f)),
            ) {
                Text(
                    tajwidRuleLabel(rule),
                    color = tajwidColor(rule),
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun TajwidAyahCard(ayah: QuranAyah, corpus: QuranCorpus, fontScale: Float) {
    val spans = remember(ayah.text) { UthmaniTajwidEngine.find(ayah.text) }
    val decorated = remember(ayah.text, spans) {
        buildAnnotatedString {
            append(ayah.text)
            spans.forEach { span ->
                addStyle(SpanStyle(color = tajwidColor(span.rule)), span.start, span.endExclusive)
            }
        }
    }
    val juz = corpus.juzAt(ayah.surah, ayah.ayah)
    val hizb = corpus.hizbAt(ayah.surah, ayah.ayah)
    Card(
        modifier = Modifier.fillMaxWidth().testTag("quran-tajwid-ayah-${ayah.surah}-${ayah.ayah}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ArihnaCream),
        border = BorderStroke(1.dp, ArihnaWarmOutline),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            val markerParts = mutableListOf<String>()
            if (juz != null) markerParts += appText("Juz $juz", "الجزء ${toArabicIndic(juz)}")
            if (hizb != null) markerParts += appText("Hizb $hizb", "الحزب ${toArabicIndic(hizb)}")
            if (markerParts.isNotEmpty()) {
                Text(
                    markerParts.joinToString("  •  "),
                    color = ArihnaDawnGold,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = decorated,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
                fontSize = (29f * fontScale).sp,
                lineHeight = (47f * fontScale).sp,
                color = ArihnaInk,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "﴿${toArabicIndic(ayah.ayah)}﴾",
                color = ArihnaDawnGold,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

private fun tajwidColor(rule: TajwidRule): Color = when (rule) {
    TajwidRule.QALQALAH -> Color(0xFFC62828)
    TajwidRule.IKHFA -> Color(0xFF7B1FA2)
    TajwidRule.IQLAB -> Color(0xFF1565C0)
    TajwidRule.IDGHAM_WITH_GHUNNAH -> Color(0xFF00897B)
    TajwidRule.IDGHAM_WITHOUT_GHUNNAH -> Color(0xFF6D4C41)
    TajwidRule.GHUNNAH -> Color(0xFFE65100)
}

@Composable
private fun tajwidRuleLabel(rule: TajwidRule): String = when (rule) {
    TajwidRule.QALQALAH -> appText("Qalqalah", "قلقلة")
    TajwidRule.IKHFA -> appText("Ikhfāʾ", "إخفاء")
    TajwidRule.IQLAB -> appText("Iqlāb", "إقلاب")
    TajwidRule.IDGHAM_WITH_GHUNNAH -> appText("Idghām + ghunnah", "إدغام بغنة")
    TajwidRule.IDGHAM_WITHOUT_GHUNNAH -> appText("Idghām", "إدغام بلا غنة")
    TajwidRule.GHUNNAH -> appText("Ghunnah", "غنة")
}

@Composable
private fun EasyQuranReader(
    corpus: QuranCorpus,
    selectedSurah: Int,
    onOpenExplorer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ayahs = remember(corpus, selectedSurah) { corpus.ayahsForSurah(selectedSurah) }
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                appText("Sura $selectedSurah", "سورة ${toArabicIndic(selectedSurah)}"),
                color = ArihnaForest,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpenExplorer) { Text(appText("Cambia", "تغيير")) }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).testTag("quran-ayah-list"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(ayahs, key = { "${it.surah}:${it.ayah}" }) { ayah ->
                QuranAyahCard(ayah = ayah, corpus = corpus)
            }
        }
    }
}

@Composable
private fun QuranAyahCard(ayah: QuranAyah, corpus: QuranCorpus) {
    val juz = corpus.juzAt(ayah.surah, ayah.ayah)
    val hizb = corpus.hizbAt(ayah.surah, ayah.ayah)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ArihnaCream),
        border = BorderStroke(1.dp, ArihnaWarmOutline),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 17.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val markerParts = mutableListOf<String>()
            if (juz != null) markerParts += appText("Juz $juz", "الجزء ${toArabicIndic(juz)}")
            if (hizb != null) markerParts += appText("Hizb $hizb", "الحزب ${toArabicIndic(hizb)}")
            val marker = markerParts.joinToString("  •  ")
            if (marker.isNotBlank()) {
                Text(
                    marker,
                    color = ArihnaDawnGold,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.testTag("quran-boundary-${ayah.surah}-${ayah.ayah}"),
                )
            }
            Text(
                text = ayah.text,
                modifier = Modifier.fillMaxWidth().testTag("quran-ayah-${ayah.surah}-${ayah.ayah}"),
                textAlign = TextAlign.End,
                fontSize = 29.sp,
                lineHeight = 47.sp,
                color = ArihnaInk,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "﴿${toArabicIndic(ayah.ayah)}﴾",
                color = ArihnaDawnGold,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

@Composable
private fun QuranAttribution(mode: QuranReadingMode) {
    HorizontalDivider(color = ArihnaWarmOutline.copy(alpha = 0.65f))
    val credit = when (mode) {
        QuranReadingMode.WARSH_NAFI -> appText(
            "Warsh: pagine King Fahd Complex (uso digitale/app consentito; no stampa commerciale) · metadati Quranpedia CC0 · offline",
            "ورش: صفحات مجمع الملك فهد للاستخدام الرقمي والتطبيقي · بيانات Quranpedia CC0 · دون اتصال",
        )
        QuranReadingMode.HAFS_UTHMANI -> appText(
            "Testo: Tanzil/Tarteel CC BY 3.0 · pagine Ḥafṣ: batoulapps/quran-svg MIT · tutto offline",
            "النص: تنزيل/ترتيل CC BY 3.0 · صفحات حفص: batoulapps/quran-svg MIT · دون اتصال",
        )
        QuranReadingMode.HAFS_TAJWID -> appText(
            "Testo Uthmani: Tanzil/Tarteel CC BY 3.0 · colorazione locale beta, logica adattata da fcat97/tajweedApi MIT · QCF V4 non incluso",
            "النص العثماني: تنزيل/ترتيل CC BY 3.0 · تلوين محلي تجريبي بمنطق مقتبس من fcat97/tajweedApi MIT · QCF V4 غير مضمن",
        )
        QuranReadingMode.EASY -> appText(
            "Testo: Tanzil/Tarteel CC BY 3.0 · tutto offline",
            "النص: تنزيل/ترتيل CC BY 3.0 · يعمل دون اتصال",
        )
    }
    Text(
        text = credit,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp).testTag("quran-attribution"),
        color = ArihnaMutedText,
        fontSize = 8.sp,
        textAlign = TextAlign.Center,
    )
}


private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal fun toArabicIndic(value: Int): String = value.toString().map { c ->
    if (c in '0'..'9') ('٠'.code + (c - '0')).toChar() else c
}.joinToString("")
