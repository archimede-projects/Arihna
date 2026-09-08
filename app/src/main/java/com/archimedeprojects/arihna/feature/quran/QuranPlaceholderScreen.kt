package com.archimedeprojects.arihna.feature.quran

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

enum class QuranReadingMode { EASY, HAFS_UTHMANI }

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
    val surahs = remember(context) { MushafRepository.surahs(context.applicationContext) }
    var mode by remember { mutableStateOf(QuranReadingPrefs.mode(context)) }
    var visualStyle by remember { mutableStateOf(QuranReadingPrefs.visualStyle(context)) }
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
            style = visualStyle,
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
                style = visualStyle,
                onStyleChange = { selected ->
                    visualStyle = selected
                    QuranReadingPrefs.setVisualStyle(context, selected)
                },
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == QuranReadingMode.HAFS_UTHMANI,
                    onClick = { onModeChange(QuranReadingMode.HAFS_UTHMANI) },
                    label = { Text(appText("Muṣḥaf Ḥafṣ", "مصحف حفص")) },
                    leadingIcon = { Icon(Icons.Rounded.AutoStories, null, Modifier.size(17.dp)) },
                    modifier = Modifier.testTag("quran-mode-hafs"),
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
        val meta = remember(currentPage) { MushafRepository.metaForPage(context, currentPage) }
        val surah = remember(currentPage) { MushafRepository.surahForPage(context, currentPage) }
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
            modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
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
                    Text(
                        appText(
                            "pag. ${currentPage + 1} · Juz ${meta.juz} · Hizb ${meta.hizb}",
                            "صفحة ${toArabicIndic(currentPage + 1)} · الجزء ${toArabicIndic(meta.juz)} · الحزب ${toArabicIndic(meta.hizb)}",
                        ),
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
                            style = style,
                            modifier = Modifier.fillMaxWidth(),
                        )
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
    private fun FullscreenMushafReader(
        startPage: Int,
        style: MushafVisualStyle,
        onDismiss: () -> Unit,
        onPageChanged: (Int) -> Unit,
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
        val meta = remember(currentPage) { MushafRepository.metaForPage(context, currentPage) }
        val surah = remember(currentPage) { MushafRepository.surahForPage(context, currentPage) }
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
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("quran-fullscreen-chrome"),
                        color = ArihnaCream.copy(alpha = 0.96f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, ArihnaWarmOutline.copy(alpha = 0.8f)),
                        shadowElevation = 3.dp,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
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
                                appText(
                                    "Juz ${meta.juz} · Hizb ${meta.hizb}",
                                    "الجزء ${toArabicIndic(meta.juz)} · الحزب ${toArabicIndic(meta.hizb)}",
                                ),
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
        style: MushafVisualStyle,
        modifier: Modifier = Modifier,
    ) {
        val classic = style == MushafVisualStyle.CLASSIC
        Surface(
            modifier = modifier
                .aspectRatio(MUSHAF_PAGE_ASPECT_RATIO)
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
                NativeMushafPage(page = page, modifier = Modifier.fillMaxSize())
            }
        }
    }

@Composable
private fun QuranExplorer(
    surahs: List<MushafSurah>,
    currentPage: Int,
    onSelectPage: (Int, Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var view by remember { mutableStateOf(QuranExplorerView.SURAHS) }
    var bookmarkVersion by remember { mutableIntStateOf(0) }
    val bookmarked = remember(bookmarkVersion, currentPage) { QuranReadingPrefs.bookmarkedPages(context) }
    val recent = remember(currentPage) { QuranReadingPrefs.recentPages(context) }
    val juzPages = remember(context) { MushafRepository.juzStartPages(context) }
    val hizbPages = remember(context) { MushafRepository.hizbStartPages(context) }
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
            ExplorerChip(QuranExplorerView.JUZ, view, appText("Juz", "الأجزاء")) { view = it }
            ExplorerChip(QuranExplorerView.HIZB, view, appText("Hizb", "الأحزاب")) { view = it }
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
                        bookmarked = (surah.pageNumber - 1) in bookmarked,
                        onBookmark = {
                            val page = (surah.pageNumber - 1).coerceIn(0, 603)
                            QuranReadingPrefs.setBookmarked(context, page, page !in bookmarked)
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
                QuranExplorerView.BOOKMARKS -> items(bookmarked.sorted(), key = { it }) { page ->
                    SavedPageRow(page = page, onClick = { onSelectPage(page, null) })
                }
                QuranExplorerView.RECENT -> items(recent, key = { it }) { page ->
                    SavedPageRow(page = page, onClick = { onSelectPage(page, null) })
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
private fun SavedPageRow(page: Int, onClick: () -> Unit) {
    val context = LocalContext.current
    val surah = remember(page) { MushafRepository.surahForPage(context, page) }
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
private fun QuranAttribution() {
    HorizontalDivider(color = ArihnaWarmOutline.copy(alpha = 0.65f))
    Text(
        text = appText(
            "Testo: Tanzil/Tarteel CC BY 3.0 · pagine Muṣḥaf: quran-svg MIT · tutto offline",
            "النص: تنزيل/ترتيل CC BY 3.0 · صفحات المصحف: quran-svg MIT · يعمل دون اتصال",
        ),
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
