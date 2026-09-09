from pathlib import Path

path = Path('app/src/main/java/com/archimedeprojects/arihna/feature/quran/QuranPlaceholderScreen.kt')
text = path.read_text()

def replace_once(old: str, new: str):
    global text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'expected one match, got {count}: {old[:140]!r}')
    text = text.replace(old, new, 1)

replace_once(
    '''    var selectedSurah by remember { mutableIntStateOf(1) }\n    var requestedPage by remember {\n        mutableIntStateOf(QuranReadingPrefs.lastPage(context, activeRiwaya))\n    }''',
    '''    var selectedSurah by remember {\n        mutableIntStateOf(\n            if (mode == QuranReadingMode.HAFS_TAJWID) QuranReadingPrefs.lastTajwidSurah(context) else 1,\n        )\n    }\n    var requestedPage by remember {\n        mutableIntStateOf(\n            if (mode == QuranReadingMode.HAFS_TAJWID) {\n                surahs.firstOrNull { it.number == selectedSurah }?.pageNumber?.minus(1)?.coerceIn(0, 603) ?: 0\n            } else {\n                QuranReadingPrefs.lastPage(context, activeRiwaya)\n            },\n        )\n    }''',
)

replace_once(
    '''                if (selected == QuranReadingMode.HAFS_TAJWID) {\n                    selectedSurah = QuranReadingPrefs.lastTajwidSurah(context)\n                } else {''',
    '''                if (selected == QuranReadingMode.HAFS_TAJWID) {\n                    selectedSurah = QuranReadingPrefs.lastTajwidSurah(context)\n                    requestedPage = MushafRepository.surahs(context, QuranRiwaya.HAFS)\n                        .firstOrNull { it.number == selectedSurah }\n                        ?.pageNumber\n                        ?.minus(1)\n                        ?.coerceIn(0, 603)\n                        ?: 0\n                } else {''',
)

replace_once(
    '''            QuranExplorer(\n                surahs = surahs,\n                currentPage = requestedPage,\n                riwaya = activeRiwaya,\n                onSelectPage = { pageIndex, surahNumber ->\n                    requestedPage = pageIndex.coerceIn(0, 603)\n                    if (surahNumber != null) selectedSurah = surahNumber\n                    explorerOpen = false\n                },\n                modifier = Modifier.weight(1f),\n            )''',
    '''            QuranExplorer(\n                surahs = surahs,\n                currentPage = requestedPage,\n                riwaya = activeRiwaya,\n                tajwidMode = mode == QuranReadingMode.HAFS_TAJWID,\n                onSelectPage = { pageIndex, surahNumber ->\n                    requestedPage = pageIndex.coerceIn(0, 603)\n                    selectedSurah = if (mode == QuranReadingMode.HAFS_TAJWID) {\n                        surahNumber\n                            ?: MushafRepository.surahForPage(context, QuranRiwaya.HAFS, requestedPage)?.number\n                            ?: selectedSurah\n                    } else {\n                        surahNumber ?: selectedSurah\n                    }\n                    explorerOpen = false\n                },\n                modifier = Modifier.weight(1f),\n            )''',
)

replace_once(
    '''private fun QuranExplorer(\n    surahs: List<MushafSurah>,\n    currentPage: Int,\n    riwaya: QuranRiwaya,\n    onSelectPage: (Int, Int?) -> Unit,''',
    '''private fun QuranExplorer(\n    surahs: List<MushafSurah>,\n    currentPage: Int,\n    riwaya: QuranRiwaya,\n    tajwidMode: Boolean,\n    onSelectPage: (Int, Int?) -> Unit,''',
)

replace_once(
    '''    val bookmarked = remember(bookmarkVersion, currentPage, riwaya) { QuranReadingPrefs.bookmarkedPages(context, riwaya) }\n    val recent = remember(currentPage, riwaya) { QuranReadingPrefs.recentPages(context, riwaya) }''',
    '''    val bookmarked = remember(bookmarkVersion, currentPage, riwaya, tajwidMode) {\n        if (tajwidMode) QuranReadingPrefs.tajwidBookmarkedSurahs(context).toList()\n        else QuranReadingPrefs.bookmarkedPages(context, riwaya)\n    }\n    val recent = remember(currentPage, riwaya, tajwidMode) {\n        if (tajwidMode) QuranReadingPrefs.recentTajwidSurahs(context)\n        else QuranReadingPrefs.recentPages(context, riwaya)\n    }''',
)

replace_once(
    '''                    SurahRow(\n                        surah = surah,\n                        bookmarked = (surah.pageNumber - 1) in bookmarked,\n                        onBookmark = {\n                            val page = (surah.pageNumber - 1).coerceIn(0, 603)\n                            QuranReadingPrefs.setBookmarked(context, riwaya, page, page !in bookmarked)\n                            bookmarkVersion++\n                        },\n                        onClick = { onSelectPage(surah.pageNumber - 1, surah.number) },\n                    )''',
    '''                    SurahRow(\n                        surah = surah,\n                        bookmarked = if (tajwidMode) surah.number in bookmarked else (surah.pageNumber - 1) in bookmarked,\n                        onBookmark = {\n                            if (tajwidMode) {\n                                QuranReadingPrefs.setTajwidSurahBookmarked(\n                                    context,\n                                    surah.number,\n                                    surah.number !in bookmarked,\n                                )\n                            } else {\n                                val page = (surah.pageNumber - 1).coerceIn(0, 603)\n                                QuranReadingPrefs.setBookmarked(context, riwaya, page, page !in bookmarked)\n                            }\n                            bookmarkVersion++\n                        },\n                        onClick = { onSelectPage(surah.pageNumber - 1, surah.number) },\n                    )''',
)

replace_once(
    '''                QuranExplorerView.BOOKMARKS -> items(bookmarked.sorted(), key = { it }) { page ->\n                    SavedPageRow(page = page, riwaya = riwaya, onClick = { onSelectPage(page, null) })\n                }\n                QuranExplorerView.RECENT -> items(recent, key = { it }) { page ->\n                    SavedPageRow(page = page, riwaya = riwaya, onClick = { onSelectPage(page, null) })\n                }''',
    '''                QuranExplorerView.BOOKMARKS -> {\n                    if (tajwidMode) {\n                        items(bookmarked.sorted(), key = { "tajwid-bookmark-$it" }) { surahNumber ->\n                            surahs.firstOrNull { it.number == surahNumber }?.let { surah ->\n                                SurahRow(\n                                    surah = surah,\n                                    bookmarked = true,\n                                    onBookmark = {\n                                        QuranReadingPrefs.setTajwidSurahBookmarked(context, surah.number, false)\n                                        bookmarkVersion++\n                                    },\n                                    onClick = { onSelectPage(surah.pageNumber - 1, surah.number) },\n                                )\n                            }\n                        }\n                    } else {\n                        items(bookmarked.sorted(), key = { it }) { page ->\n                            SavedPageRow(page = page, riwaya = riwaya, onClick = { onSelectPage(page, null) })\n                        }\n                    }\n                }\n                QuranExplorerView.RECENT -> {\n                    if (tajwidMode) {\n                        items(recent, key = { "tajwid-recent-$it" }) { surahNumber ->\n                            surahs.firstOrNull { it.number == surahNumber }?.let { surah ->\n                                SurahRow(\n                                    surah = surah,\n                                    bookmarked = surah.number in bookmarked,\n                                    onBookmark = {\n                                        QuranReadingPrefs.setTajwidSurahBookmarked(\n                                            context,\n                                            surah.number,\n                                            surah.number !in bookmarked,\n                                        )\n                                        bookmarkVersion++\n                                    },\n                                    onClick = { onSelectPage(surah.pageNumber - 1, surah.number) },\n                                )\n                            }\n                        }\n                    } else {\n                        items(recent, key = { it }) { page ->\n                            SavedPageRow(page = page, riwaya = riwaya, onClick = { onSelectPage(page, null) })\n                        }\n                    }\n                }''',
)

replace_once(
    '''private fun tajwidRuleLabel(rule: TajwidRule): String = when (rule) {''',
    '''@Composable\nprivate fun tajwidRuleLabel(rule: TajwidRule): String = when (rule) {''',
)

path.write_text(text)
