from pathlib import Path


def once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, got {count}")
    return text.replace(old, new, 1)


# Quran: explicit RTL pager, clear global index, immersive zoomable reader.
path = Path("app/src/main/java/com/archimedeprojects/arihna/feature/quran/QuranPlaceholderScreen.kt")
q = path.read_text(encoding="utf-8")
q = once(q, "import androidx.compose.foundation.background\n", """import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
""", "quran gesture imports")
q = once(q, "import androidx.compose.material.icons.rounded.BookmarkBorder\n", """import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
""", "quran icons")
q = once(q, "import androidx.compose.runtime.Composable\n", """import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
""", "quran composition local")
q = once(q, "import androidx.compose.runtime.mutableIntStateOf\n", """import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
""", "quran float state")
q = once(q, "import androidx.compose.ui.Alignment\n", """import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
""", "quran offset")
q = once(q, "import androidx.compose.ui.graphics.Color\n", """import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
""", "quran graphics")
q = once(q, "import androidx.compose.ui.platform.LocalContext\n", """import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
""", "quran pointer layout imports")
q = once(q, "import androidx.compose.ui.unit.dp\n", """import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
""", "quran layout direction")
q = once(q, "import androidx.compose.ui.unit.sp\n", """import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
""", "quran dialog imports")
q = once(
    q,
    'Text(appText(if (explorerOpen) "Lettura" else "Sura", if (explorerOpen) "المصحف" else "السور"), fontWeight = FontWeight.Bold)',
    'Text(appText(if (explorerOpen) "Lettura" else "Indice", if (explorerOpen) "المصحف" else "الفهرس"), fontWeight = FontWeight.Bold)',
    "global Quran index label",
)

start = q.index("@Composable\nprivate fun MushafBookReader(")
end = q.index("\n@Composable\nprivate fun QuranExplorer(", start)
replacement = r'''@Composable
private fun MushafBookReader(
    startPage: Int,
    onPageChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = startPage.coerceIn(0, 603),
        pageCount = { 604 },
    )
    var bookmarkVersion by remember { mutableIntStateOf(0) }
    var fullscreenOpen by remember { mutableStateOf(false) }
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

    if (fullscreenOpen) {
        FullscreenMushafReader(
            startPage = currentPage,
            onDismiss = { fullscreenOpen = false },
            onPageChanged = onPageChanged,
        )
    }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(42.dp),
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
                onClick = { fullscreenOpen = true },
                modifier = Modifier.testTag("quran-reading-fullscreen"),
            ) {
                Icon(Icons.Rounded.Fullscreen, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(appText("Lettura", "قراءة"), fontWeight = FontWeight.Bold)
            }
            IconButton(
                onClick = {
                    QuranReadingPrefs.setBookmarked(context, currentPage, !bookmarked)
                    bookmarkVersion++
                },
                modifier = Modifier.testTag("quran-bookmark-toggle"),
            ) {
                Icon(
                    if (bookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = appText("Segnalibro", "إشارة مرجعية"),
                    tint = if (bookmarked) ArihnaDawnGold else ArihnaGreen,
                )
            }
        }

        // Quran is a right-to-left book even when Arihna's surrounding UI is Italian/LTR.
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
                    // No oversized decorative book-card: the pinned page owns the viewport.
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

@Composable
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

    LaunchedEffect(currentPage) {
        zoomed = false
        QuranReadingPrefs.recordVisitedPage(context, currentPage)
        onPageChanged(currentPage)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
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
                    color = ArihnaCream.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(999.dp),
                    shadowElevation = 4.dp,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(horizontal = 4.dp),
                    ) {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("quran-fullscreen-close")) {
                            Icon(Icons.Rounded.Close, contentDescription = appText("Chiudi lettura", "إغلاق القراءة"), tint = ArihnaForest)
                        }
                        Text(
                            appText("pag. ${currentPage + 1}", "صفحة ${toArabicIndic(currentPage + 1)}"),
                            color = ArihnaForest,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                        )
                        IconButton(
                            onClick = {
                                QuranReadingPrefs.setBookmarked(context, currentPage, !bookmarked)
                                bookmarkVersion++
                            },
                            modifier = Modifier.testTag("quran-fullscreen-bookmark"),
                        ) {
                            Icon(
                                if (bookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                contentDescription = appText("Segnalibro", "إشارة مرجعية"),
                                tint = if (bookmarked) ArihnaDawnGold else ArihnaGreen,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomableMushafPage(
    page: Int,
    onZoomedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var scale by remember(page) { mutableFloatStateOf(1f) }
    var offset by remember(page) { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .pointerInput(page) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pressedPointers = event.changes.count { it.pressed }
                        // At 1x a one-finger gesture belongs to the RTL pager. Two fingers enter zoom;
                        // once zoomed, one finger pans until the user returns to 1x.
                        if (pressedPointers >= 2 || scale > 1.01f) {
                            val nextScale = (scale * event.calculateZoom()).coerceIn(1f, 5f)
                            val pan = event.calculatePan()
                            scale = nextScale
                            offset = if (nextScale <= 1.01f) Offset.Zero else offset + pan
                            onZoomedChange(nextScale > 1.01f)
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .testTag("quran-zoomable-page-$page"),
        contentAlignment = Alignment.Center,
    ) {
        NativeMushafPage(
            page = page,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
    }
}'''
q = q[:start] + replacement + q[end:]

old_search = '''        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().testTag("quran-surah-search"),
            singleLine = true,
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            placeholder = { Text(appText("Cerca una sura…", "ابحث في السور…")) },
            shape = RoundedCornerShape(18.dp),
        )

        Row('''
new_search = '''        Text(
            appText("Indice del Corano", "فهرس القرآن"),
            color = ArihnaForest,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 17.sp,
            modifier = Modifier.testTag("quran-global-index-title"),
        )

        Row('''
q = once(q, old_search, new_search, "replace global search with index heading")
marker = '''        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ExplorerShortcut('''
inserted = '''        if (view == QuranExplorerView.SURAHS) {
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
            ExplorerShortcut('''
q = once(q, marker, inserted, "Sura-only search placement")
for token in (
    "LocalLayoutDirection provides LayoutDirection.Rtl",
    "quran-mushaf-rtl-pager",
    "quran-reading-fullscreen",
    "quran-fullscreen-reader",
    "quran-zoomable-page-",
    "userScrollEnabled = !zoomed",
    "Indice del Corano",
    '"Indice"',
):
    if token not in q:
        raise SystemExit(f"missing Quran contract token: {token}")
path.write_text(q, encoding="utf-8")


# Adhan: compact chooser + exact bundled audio preview with lifecycle cleanup.
path = Path("app/src/main/java/com/archimedeprojects/arihna/feature/prayers/PrayerTimesPlaceholderScreen.kt")
p = path.read_text(encoding="utf-8")
p = once(p, "import android.app.Activity\n", """import android.app.Activity
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
""", "adhan Android imports")
p = once(p, "import androidx.compose.material3.Icon\n", """import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
""", "adhan outlined button")
p = once(p, "import androidx.compose.runtime.Composable\n", """import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
""", "adhan dispose effect")
p = once(p, "import com.archimedeprojects.arihna.core.i18n.appText\n", """import com.archimedeprojects.arihna.R
import com.archimedeprojects.arihna.core.i18n.appText
""", "adhan R import")

start = p.index("@Composable\nprivate fun PrayerSoundDialog(")
end = p.index("\n@Composable\nprivate fun SoundListRow(", start)
replacement = r'''private class AdhanPreviewPlayer(private val context: Context) {
    private var player: MediaPlayer? = null

    fun play(variant: AdhanVariant) {
        stop()
        val rawResource = when (variant) {
            AdhanVariant.CLASSIC -> R.raw.adhan_cc0
            AdhanVariant.BEAUTIFUL -> R.raw.adhan_beautiful_cc0
            AdhanVariant.SHORT -> R.raw.adhan_short_cc0
            AdhanVariant.EXTENDED -> R.raw.adhan_extended_cc_by_sa
            AdhanVariant.COMPACT -> R.raw.adhan_compact_pd
            AdhanVariant.ALTERNATIVE -> R.raw.adhan_alternative_cc_by_sa
        }
        val next = MediaPlayer()
        try {
            next.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            context.resources.openRawResourceFd(rawResource).use { descriptor ->
                next.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
            }
            next.isLooping = false
            next.prepare()
            next.setOnCompletionListener { completed ->
                if (player === completed) player = null
                completed.release()
            }
            player = next
            next.start()
        } catch (_: Throwable) {
            next.release()
            player = null
        }
    }

    fun stop() {
        player?.let { active ->
            runCatching { if (active.isPlaying) active.stop() }
            active.release()
        }
        player = null
    }
}

@Composable
private fun PrayerSoundDialog(
    rule: AlarmRule,
    onDismiss: () -> Unit,
    onSave: (AlarmSoundProfile, String?, String?) -> Unit,
) {
    val context = LocalContext.current
    var profile by remember(rule.alarmId, rule.revision) { mutableStateOf(rule.soundProfile) }
    var adhanVariant by remember(rule.alarmId, rule.revision) {
        mutableStateOf(AdhanVariant.fromStorage(rule.ringtoneUri))
    }
    var ringtoneUri by remember(rule.alarmId, rule.revision) { mutableStateOf(rule.ringtoneUri) }
    var ringtoneTitle by remember(rule.alarmId, rule.revision) { mutableStateOf(rule.ringtoneTitle) }
    var adhanListOpen by remember(rule.alarmId, rule.revision) { mutableStateOf(false) }
    var previewing by remember(rule.alarmId, rule.revision) { mutableStateOf<AdhanVariant?>(null) }
    val previewPlayer = remember(context, rule.alarmId) { AdhanPreviewPlayer(context.applicationContext) }

    DisposableEffect(previewPlayer) {
        onDispose { previewPlayer.stop() }
    }

    val ringtoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            AlarmRingtonePicker.pickedUri(result.data)?.let { uri ->
                previewPlayer.stop()
                previewing = null
                ringtoneUri = uri.toString()
                ringtoneTitle = AlarmRingtonePicker.title(context, uri) ?: "Suoneria telefono"
                profile = AlarmSoundProfile.SYSTEM_DEFAULT
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            previewPlayer.stop()
            previewing = null
            onDismiss()
        },
        title = {
            Text(
                if (adhanListOpen) appText("Scegli Adhan", "اختر الأذان") else appText("Suono promemoria", "صوت التذكير"),
                color = OrariForest,
            )
        },
        containerColor = OrariCream,
        text = {
            if (adhanListOpen) {
                Column(
                    modifier = Modifier.testTag("prayer-sound-adhan-list"),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        appText("Tocca un Adhan per ascoltarlo prima di scegliere.", "اضغط على الأذان للاستماع إليه قبل الاختيار."),
                        color = OrariForest.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    AdhanVariant.entries.forEach { variant ->
                        SoundListRow(
                            title = if (previewing == variant) "${variant.displayName} · ${appText("in ascolto", "يعمل الآن")}" else variant.displayName,
                            icon = Icons.Rounded.Mosque,
                            selected = profile == AlarmSoundProfile.ADHAN && adhanVariant == variant,
                        ) {
                            profile = AlarmSoundProfile.ADHAN
                            adhanVariant = variant
                            ringtoneUri = variant.storageValue
                            ringtoneTitle = variant.displayName
                            previewing = variant
                            previewPlayer.play(variant)
                        }
                    }
                    TextButton(
                        onClick = {
                            previewPlayer.stop()
                            previewing = null
                            adhanListOpen = false
                        },
                    ) { Text(appText("Indietro", "رجوع")) }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(appText("Adhan", "الأذان"), fontWeight = FontWeight.ExtraBold, color = OrariForest)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = if (profile == AlarmSoundProfile.ADHAN) OrariSageStrong else Color.Transparent,
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(Icons.Rounded.Mosque, contentDescription = null, tint = OrariForest)
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (profile == AlarmSoundProfile.ADHAN) adhanVariant.displayName else appText("Nessun Adhan selezionato", "لم يتم اختيار أذان"),
                                    color = OrariForest,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    appText("Puoi ascoltarli prima di confermare", "يمكنك الاستماع قبل التأكيد"),
                                    color = OrariForest.copy(alpha = 0.66f),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            previewPlayer.stop()
                            previewing = null
                            adhanListOpen = true
                        },
                        modifier = Modifier.fillMaxWidth().testTag("prayer-sound-choose-adhan"),
                    ) {
                        Icon(Icons.Rounded.Mosque, contentDescription = null)
                        Text(appText("  Scegli Adhan", "  اختر الأذان"), fontWeight = FontWeight.Bold)
                    }

                    SoundListRow(
                        title = appText("Suoneria telefono", "نغمة الهاتف"),
                        icon = Icons.Rounded.Notifications,
                        selected = profile == AlarmSoundProfile.SYSTEM_DEFAULT,
                    ) {
                        previewPlayer.stop()
                        previewing = null
                        profile = AlarmSoundProfile.SYSTEM_DEFAULT
                    }
                    if (profile == AlarmSoundProfile.SYSTEM_DEFAULT) {
                        TextButton(
                            onClick = {
                                previewPlayer.stop()
                                ringtoneLauncher.launch(AlarmRingtonePicker.createIntent(ringtoneUri))
                            },
                        ) { Text(ringtoneTitle ?: appText("Scegli suoneria", "اختر نغمة")) }
                    }
                    SoundListRow(
                        title = appText("Silenzioso", "صامت"),
                        icon = Icons.Rounded.VolumeOff,
                        selected = profile == AlarmSoundProfile.SILENT,
                    ) {
                        previewPlayer.stop()
                        previewing = null
                        profile = AlarmSoundProfile.SILENT
                        ringtoneUri = null
                        ringtoneTitle = null
                    }
                }
            }
        },
        confirmButton = {
            if (!adhanListOpen) {
                Button(
                    onClick = {
                        previewPlayer.stop()
                        previewing = null
                        val storedUri = if (profile == AlarmSoundProfile.ADHAN) adhanVariant.storageValue else ringtoneUri
                        val storedTitle = if (profile == AlarmSoundProfile.ADHAN) adhanVariant.displayName else ringtoneTitle
                        onSave(profile, storedUri, storedTitle)
                    },
                ) { Text(appText("Conferma", "تأكيد")) }
            }
        },
        dismissButton = {
            if (!adhanListOpen) {
                TextButton(
                    onClick = {
                        previewPlayer.stop()
                        previewing = null
                        onDismiss()
                    },
                ) { Text(appText("Chiudi", "إغلاق")) }
            }
        },
    )
}'''
p = p[:start] + replacement + p[end:]
for token in (
    "prayer-sound-choose-adhan",
    "prayer-sound-adhan-list",
    "AdhanPreviewPlayer",
    "AudioAttributes.USAGE_MEDIA",
    "previewPlayer.play(variant)",
    "onDispose { previewPlayer.stop() }",
    "R.raw.adhan_cc0",
    "R.raw.adhan_beautiful_cc0",
    "R.raw.adhan_short_cc0",
    "R.raw.adhan_extended_cc_by_sa",
    "R.raw.adhan_compact_pd",
    "R.raw.adhan_alternative_cc_by_sa",
):
    if token not in p:
        raise SystemExit(f"missing Adhan contract token: {token}")
path.write_text(p, encoding="utf-8")
