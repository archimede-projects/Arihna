from pathlib import Path

screen_path = Path('app/src/main/java/com/archimedeprojects/arihna/feature/quran/QuranPlaceholderScreen.kt')
screen = screen_path.read_text()

def replace_once(old: str, new: str):
    global screen
    count = screen.count(old)
    if count != 1:
        raise SystemExit(f'expected one match, got {count}: {old[:120]!r}')
    screen = screen.replace(old, new, 1)

replace_once(
    'import androidx.compose.foundation.background\n',
    'import androidx.compose.foundation.background\nimport androidx.compose.foundation.horizontalScroll\nimport androidx.compose.foundation.rememberScrollState\n',
)
replace_once(
    'import androidx.compose.ui.text.font.FontWeight\n',
    'import androidx.compose.ui.text.SpanStyle\nimport androidx.compose.ui.text.buildAnnotatedString\nimport androidx.compose.ui.text.font.FontWeight\n',
)
replace_once(
    'enum class QuranReadingMode { EASY, HAFS_UTHMANI, WARSH_NAFI }',
    'enum class QuranReadingMode { EASY, HAFS_UTHMANI, HAFS_TAJWID, WARSH_NAFI }',
)
replace_once(
    '''internal fun QuranReadingMode.riwayaOrNull(): QuranRiwaya? = when (this) {\n    QuranReadingMode.HAFS_UTHMANI -> QuranRiwaya.HAFS\n    QuranReadingMode.WARSH_NAFI -> QuranRiwaya.WARSH\n    QuranReadingMode.EASY -> null\n}''',
    '''internal fun QuranReadingMode.riwayaOrNull(): QuranRiwaya? = when (this) {\n    QuranReadingMode.HAFS_UTHMANI, QuranReadingMode.HAFS_TAJWID -> QuranRiwaya.HAFS\n    QuranReadingMode.WARSH_NAFI -> QuranRiwaya.WARSH\n    QuranReadingMode.EASY -> null\n}''',
)
replace_once(
    '''    if (fullscreenOpen) {\n        FullscreenMushafReader(\n            startPage = requestedPage,\n            style = visualStyle,\n            riwaya = activeRiwaya,\n            onDismiss = { fullscreenOpen = false },\n            onPageChanged = { pageIndex ->\n                requestedPage = pageIndex\n                MushafRepository.surahForPage(context, activeRiwaya, pageIndex)?.let { selectedSurah = it.number }\n            },\n        )\n        return\n    }''',
    '''    if (fullscreenOpen) {\n        if (mode == QuranReadingMode.HAFS_TAJWID) {\n            FullscreenTajwidReader(\n                corpus = corpus,\n                selectedSurah = selectedSurah,\n                onDismiss = { fullscreenOpen = false },\n            )\n        } else {\n            FullscreenMushafReader(\n                startPage = requestedPage,\n                style = visualStyle,\n                riwaya = activeRiwaya,\n                onDismiss = { fullscreenOpen = false },\n                onPageChanged = { pageIndex ->\n                    requestedPage = pageIndex\n                    MushafRepository.surahForPage(context, activeRiwaya, pageIndex)?.let { selectedSurah = it.number }\n                },\n            )\n        }\n        return\n    }''',
)
replace_once(
    '''            onModeChange = { selected ->\n                selected.riwayaOrNull()?.let { riwaya ->\n                    requestedPage = QuranReadingPrefs.lastPage(context, riwaya)\n                    selectedSurah = MushafRepository.surahForPage(context, riwaya, requestedPage)?.number ?: 1\n                }\n                mode = selected\n                QuranReadingPrefs.setMode(context, selected)\n                explorerOpen = false\n            },''',
    '''            onModeChange = { selected ->\n                if (selected == QuranReadingMode.HAFS_TAJWID) {\n                    selectedSurah = QuranReadingPrefs.lastTajwidSurah(context)\n                } else {\n                    selected.riwayaOrNull()?.let { riwaya ->\n                        requestedPage = QuranReadingPrefs.lastPage(context, riwaya)\n                        selectedSurah = MushafRepository.surahForPage(context, riwaya, requestedPage)?.number ?: 1\n                    }\n                }\n                mode = selected\n                QuranReadingPrefs.setMode(context, selected)\n                explorerOpen = false\n            },''',
)
replace_once(
    '''        } else if (mode == QuranReadingMode.HAFS_UTHMANI || mode == QuranReadingMode.WARSH_NAFI) {\n            MushafBookReader(''',
    '''        } else if (mode == QuranReadingMode.HAFS_TAJWID) {\n            TajwidQuranReader(\n                corpus = corpus,\n                selectedSurah = selectedSurah,\n                onOpenExplorer = { explorerOpen = true },\n                onOpenFullscreen = { fullscreenOpen = true },\n                modifier = Modifier.weight(1f),\n            )\n        } else if (mode == QuranReadingMode.HAFS_UTHMANI || mode == QuranReadingMode.WARSH_NAFI) {\n            MushafBookReader(''',
)
replace_once('        QuranAttribution(mode.riwayaOrNull())', '        QuranAttribution(mode)')

old_chips = '''            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {\n                FilterChip(\n                    selected = mode == QuranReadingMode.HAFS_UTHMANI,\n                    onClick = { onModeChange(QuranReadingMode.HAFS_UTHMANI) },\n                    label = { Text(appText("Muṣḥaf Ḥafṣ", "مصحف حفص")) },\n                    leadingIcon = { Icon(Icons.Rounded.AutoStories, null, Modifier.size(17.dp)) },\n                    modifier = Modifier.testTag("quran-mode-hafs"),\n                )\n                FilterChip(\n                    selected = mode == QuranReadingMode.WARSH_NAFI,\n                    onClick = { onModeChange(QuranReadingMode.WARSH_NAFI) },\n                    label = { Text(appText("Muṣḥaf Warsh", "مصحف ورش")) },\n                    leadingIcon = { Icon(Icons.Rounded.AutoStories, null, Modifier.size(17.dp)) },\n                    modifier = Modifier.testTag("quran-mode-warsh"),\n                )\n                FilterChip(\n                    selected = mode == QuranReadingMode.EASY,\n                    onClick = { onModeChange(QuranReadingMode.EASY) },\n                    label = { Text(appText("Facile", "قراءة سهلة")) },\n                    modifier = Modifier.testTag("quran-mode-easy"),\n                )\n            }'''
new_chips = '''            Row(\n                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),\n                horizontalArrangement = Arrangement.spacedBy(8.dp),\n            ) {\n                FilterChip(\n                    selected = mode == QuranReadingMode.HAFS_UTHMANI,\n                    onClick = { onModeChange(QuranReadingMode.HAFS_UTHMANI) },\n                    label = { Text(appText("Muṣḥaf Ḥafṣ", "مصحف حفص")) },\n                    leadingIcon = { Icon(Icons.Rounded.AutoStories, null, Modifier.size(17.dp)) },\n                    modifier = Modifier.testTag("quran-mode-hafs"),\n                )\n                FilterChip(\n                    selected = mode == QuranReadingMode.HAFS_TAJWID,\n                    onClick = { onModeChange(QuranReadingMode.HAFS_TAJWID) },\n                    label = { Text(appText("Ḥafṣ Tajwid · Beta", "حفص تجويد · تجريبي")) },\n                    leadingIcon = { Icon(Icons.Rounded.AutoStories, null, Modifier.size(17.dp)) },\n                    modifier = Modifier.testTag("quran-mode-tajwid"),\n                )\n                FilterChip(\n                    selected = mode == QuranReadingMode.WARSH_NAFI,\n                    onClick = { onModeChange(QuranReadingMode.WARSH_NAFI) },\n                    label = { Text(appText("Muṣḥaf Warsh", "مصحف ورش")) },\n                    leadingIcon = { Icon(Icons.Rounded.AutoStories, null, Modifier.size(17.dp)) },\n                    modifier = Modifier.testTag("quran-mode-warsh"),\n                )\n                FilterChip(\n                    selected = mode == QuranReadingMode.EASY,\n                    onClick = { onModeChange(QuranReadingMode.EASY) },\n                    label = { Text(appText("Facile", "قراءة سهلة")) },\n                    modifier = Modifier.testTag("quran-mode-easy"),\n                )\n            }'''
replace_once(old_chips, new_chips)

anchor = '@Composable\nprivate fun EasyQuranReader('
if screen.count(anchor) != 1:
    raise SystemExit('EasyQuranReader anchor mismatch')
tajwid_ui = r'''@Composable
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

private fun tajwidRuleLabel(rule: TajwidRule): String = when (rule) {
    TajwidRule.QALQALAH -> appText("Qalqalah", "قلقلة")
    TajwidRule.IKHFA -> appText("Ikhfāʾ", "إخفاء")
    TajwidRule.IQLAB -> appText("Iqlāb", "إقلاب")
    TajwidRule.IDGHAM_WITH_GHUNNAH -> appText("Idghām + ghunnah", "إدغام بغنة")
    TajwidRule.IDGHAM_WITHOUT_GHUNNAH -> appText("Idghām", "إدغام بلا غنة")
    TajwidRule.GHUNNAH -> appText("Ghunnah", "غنة")
}

'''
screen = screen.replace(anchor, tajwid_ui + anchor, 1)

old_attr = '''@Composable\nprivate fun QuranAttribution(riwaya: QuranRiwaya?) {\n    HorizontalDivider(color = ArihnaWarmOutline.copy(alpha = 0.65f))\n    val credit = when (riwaya) {\n        QuranRiwaya.WARSH -> appText(\n            "Warsh: pagine King Fahd Complex (uso digitale/app consentito; no stampa commerciale) · metadati Quranpedia CC0 · offline",\n            "ورش: صفحات مجمع الملك فهد للاستخدام الرقمي والتطبيقي · بيانات Quranpedia CC0 · دون اتصال",\n        )\n        QuranRiwaya.HAFS -> appText(\n            "Testo: Tanzil/Tarteel CC BY 3.0 · pagine Ḥafṣ: batoulapps/quran-svg MIT · tutto offline",\n            "النص: تنزيل/ترتيل CC BY 3.0 · صفحات حفص: batoulapps/quran-svg MIT · دون اتصال",\n        )\n        null -> appText(\n            "Testo: Tanzil/Tarteel CC BY 3.0 · tutto offline",\n            "النص: تنزيل/ترتيل CC BY 3.0 · يعمل دون اتصال",\n        )\n    }\n    Text(\n        text = credit,\n        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp).testTag("quran-attribution"),\n        color = ArihnaMutedText,\n        fontSize = 8.sp,\n        textAlign = TextAlign.Center,\n    )\n}'''
new_attr = '''@Composable\nprivate fun QuranAttribution(mode: QuranReadingMode) {\n    HorizontalDivider(color = ArihnaWarmOutline.copy(alpha = 0.65f))\n    val credit = when (mode) {\n        QuranReadingMode.WARSH_NAFI -> appText(\n            "Warsh: pagine King Fahd Complex (uso digitale/app consentito; no stampa commerciale) · metadati Quranpedia CC0 · offline",\n            "ورش: صفحات مجمع الملك فهد للاستخدام الرقمي والتطبيقي · بيانات Quranpedia CC0 · دون اتصال",\n        )\n        QuranReadingMode.HAFS_UTHMANI -> appText(\n            "Testo: Tanzil/Tarteel CC BY 3.0 · pagine Ḥafṣ: batoulapps/quran-svg MIT · tutto offline",\n            "النص: تنزيل/ترتيل CC BY 3.0 · صفحات حفص: batoulapps/quran-svg MIT · دون اتصال",\n        )\n        QuranReadingMode.HAFS_TAJWID -> appText(\n            "Testo Uthmani: Tanzil/Tarteel CC BY 3.0 · colorazione locale beta, logica adattata da fcat97/tajweedApi MIT · QCF V4 non incluso",\n            "النص العثماني: تنزيل/ترتيل CC BY 3.0 · تلوين محلي تجريبي بمنطق مقتبس من fcat97/tajweedApi MIT · QCF V4 غير مضمن",\n        )\n        QuranReadingMode.EASY -> appText(\n            "Testo: Tanzil/Tarteel CC BY 3.0 · tutto offline",\n            "النص: تنزيل/ترتيل CC BY 3.0 · يعمل دون اتصال",\n        )\n    }\n    Text(\n        text = credit,\n        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp).testTag("quran-attribution"),\n        color = ArihnaMutedText,\n        fontSize = 8.sp,\n        textAlign = TextAlign.Center,\n    )\n}'''
replace_once(old_attr, new_attr)
screen_path.write_text(screen)

repo_path = Path('app/src/main/java/com/archimedeprojects/arihna/feature/quran/MushafRepository.kt')
repo = repo_path.read_text()
old_constants = '    private const val KEY_RECENT_PREFIX = "recent_mushaf_pages_v2_"\n'
new_constants = '''    private const val KEY_RECENT_PREFIX = "recent_mushaf_pages_v2_"\n    private const val KEY_TAJWID_BOOKMARKS = "tajwid_bookmarked_surahs_v1"\n    private const val KEY_TAJWID_LAST_SURAH = "tajwid_last_surah_v1"\n    private const val KEY_TAJWID_RECENT = "tajwid_recent_surahs_v1"\n'''
if repo.count(old_constants) != 1:
    raise SystemExit('prefs constants anchor mismatch')
repo = repo.replace(old_constants, new_constants, 1)
mode_anchor = '    fun mode(context: Context): QuranReadingMode = runCatching {\n'
methods = '''    fun tajwidBookmarkedSurahs(context: Context): Set<Int> =\n        prefs(context).getStringSet(KEY_TAJWID_BOOKMARKS, emptySet()).orEmpty()\n            .mapNotNull { it.toIntOrNull() }\n            .filter { it in 1..114 }\n            .toSet()\n\n    fun isTajwidSurahBookmarked(context: Context, surah: Int): Boolean =\n        surah.coerceIn(1, 114) in tajwidBookmarkedSurahs(context)\n\n    fun setTajwidSurahBookmarked(context: Context, surah: Int, bookmarked: Boolean) {\n        val safe = surah.coerceIn(1, 114)\n        val set = tajwidBookmarkedSurahs(context).map(Int::toString).toMutableSet()\n        if (bookmarked) set += safe.toString() else set -= safe.toString()\n        prefs(context).edit().putStringSet(KEY_TAJWID_BOOKMARKS, set).apply()\n    }\n\n    fun lastTajwidSurah(context: Context): Int =\n        prefs(context).getInt(KEY_TAJWID_LAST_SURAH, 1).coerceIn(1, 114)\n\n    fun recentTajwidSurahs(context: Context): List<Int> =\n        prefs(context).getString(KEY_TAJWID_RECENT, "").orEmpty()\n            .split(',')\n            .mapNotNull { it.toIntOrNull() }\n            .filter { it in 1..114 }\n            .distinct()\n\n    fun recordVisitedTajwidSurah(context: Context, surah: Int) {\n        val safe = surah.coerceIn(1, 114)\n        val recent = recentTajwidSurahs(context).toMutableList().apply {\n            remove(safe)\n            add(0, safe)\n        }.take(8)\n        prefs(context).edit()\n            .putInt(KEY_TAJWID_LAST_SURAH, safe)\n            .putString(KEY_TAJWID_RECENT, recent.joinToString(","))\n            .apply()\n    }\n\n'''
if repo.count(mode_anchor) != 1:
    raise SystemExit('prefs mode anchor mismatch')
repo = repo.replace(mode_anchor, methods + mode_anchor, 1)
repo_path.write_text(repo)

android_test_path = Path('app/src/androidTest/java/com/archimedeprojects/arihna/feature/quran/QuranFullscreenAndroidTest.kt')
android_test = android_test_path.read_text()
if not android_test.rstrip().endswith('}'):
    raise SystemExit('android test class closing brace missing')
android_test = android_test.rstrip()[:-1] + r'''

    @Test
    fun tajwidBetaIsClearlyLabeledAndUsesSeparateSurahBookmarks() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        QuranReadingPrefs.setMode(context, QuranReadingMode.HAFS_TAJWID)
        QuranReadingPrefs.recordVisitedTajwidSurah(context, 1)
        QuranReadingPrefs.setTajwidSurahBookmarked(context, 1, false)
        QuranReadingPrefs.setBookmarked(context, QuranRiwaya.HAFS, 0, false)

        composeRule.setContent { QuranPlaceholderScreen(PaddingValues(0.dp)) }
        waitForExists("quran-tajwid-beta-reader")
        composeRule.onNodeWithTag("quran-tajwid-beta-disclaimer").assertIsDisplayed()
        composeRule.onNodeWithTag("quran-tajwid-legend").assertIsDisplayed()
        composeRule.onNodeWithTag("quran-tajwid-bookmark").performClick()
        composeRule.runOnIdle {
            assertTrue(QuranReadingPrefs.isTajwidSurahBookmarked(context, 1))
            assertFalse(QuranReadingPrefs.isBookmarked(context, QuranRiwaya.HAFS, 0))
        }
    }

    @Test
    fun tajwidFullscreenIsImmersiveAndKeepsZoomControls() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        QuranReadingPrefs.setMode(context, QuranReadingMode.HAFS_TAJWID)
        QuranReadingPrefs.recordVisitedTajwidSurah(context, 1)
        var immersive = false
        composeRule.setContent {
            QuranPlaceholderScreen(
                contentPadding = PaddingValues(0.dp),
                onImmersiveChanged = { immersive = it },
            )
        }
        waitForExists("quran-tajwid-fullscreen")
        composeRule.onNodeWithTag("quran-tajwid-fullscreen").performClick()
        waitForExists("quran-tajwid-fullscreen-reader")
        composeRule.onNodeWithTag("quran-tajwid-fullscreen-disclaimer").assertIsDisplayed()
        composeRule.onNodeWithTag("quran-tajwid-fullscreen-zoom-in").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("quran-tajwid-fullscreen-zoom-out").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertTrue(immersive) }
        composeRule.onNodeWithTag("quran-tajwid-fullscreen-close").performClick()
        waitForMissing("quran-tajwid-fullscreen-reader")
        composeRule.runOnIdle { assertFalse(immersive) }
    }
}
'''
android_test_path.write_text(android_test)
