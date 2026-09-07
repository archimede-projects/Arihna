from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    p = Path(path)
    text = p.read_text()
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{path}: expected replacement target once, found {count}: {old[:100]!r}")
    p.write_text(text.replace(old, new, 1))


def add_import_after(path: str, anchor: str, import_line: str) -> None:
    p = Path(path)
    text = p.read_text()
    if import_line in text:
        return
    count = text.count(anchor)
    if count != 1:
        raise SystemExit(f"{path}: import anchor expected once, found {count}: {anchor!r}")
    p.write_text(text.replace(anchor, anchor + import_line, 1))


def write(path: str, content: str) -> None:
    p = Path(path)
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(content)


write(
    "app/src/main/java/com/archimedeprojects/arihna/core/i18n/AppLanguage.kt",
    '''package com.archimedeprojects.arihna.core.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

enum class AppLanguage(val storageValue: String) {
    ITALIAN("it"),
    ARABIC("ar"),
    ;

    companion object {
        fun fromStorage(value: String?): AppLanguage =
            entries.firstOrNull { it.storageValue == value } ?: ITALIAN
    }
}

class AppLanguageStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "arihna_language",
        Context.MODE_PRIVATE,
    )

    fun read(): AppLanguage = AppLanguage.fromStorage(preferences.getString(KEY_LANGUAGE, null))

    fun set(language: AppLanguage) {
        preferences.edit().putString(KEY_LANGUAGE, language.storageValue).apply()
    }

    private companion object {
        const val KEY_LANGUAGE = "language"
    }
}

val LocalArihnaLanguage = staticCompositionLocalOf { AppLanguage.ITALIAN }

@Composable
fun ArihnaLanguageProvider(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalArihnaLanguage provides language,
        LocalLayoutDirection provides if (language == AppLanguage.ARABIC) {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        },
        content = content,
    )
}

@Composable
fun arihnaString(italian: String, arabic: String): String =
    if (LocalArihnaLanguage.current == AppLanguage.ARABIC) arabic else italian

fun AppLanguage.locale(): Locale = when (this) {
    AppLanguage.ITALIAN -> Locale.ITALIAN
    AppLanguage.ARABIC -> Locale.forLanguageTag("ar")
}
''',
)

write(
    "app/src/main/java/com/archimedeprojects/arihna/core/calendar/HijriDateFormatter.kt",
    '''package com.archimedeprojects.arihna.core.calendar

import com.archimedeprojects.arihna.core.i18n.AppLanguage
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

object HijriDateFormatter {
    private val italianMonths = listOf(
        "Muharram",
        "Safar",
        "Rabi al-awwal",
        "Rabi al-thani",
        "Jumada al-awwal",
        "Jumada al-thani",
        "Rajab",
        "Sha'ban",
        "Ramadan",
        "Shawwal",
        "Dhu al-Qi'dah",
        "Dhu al-Hijjah",
    )
    private val arabicMonths = listOf(
        "محرم",
        "صفر",
        "ربيع الأول",
        "ربيع الآخر",
        "جمادى الأولى",
        "جمادى الآخرة",
        "رجب",
        "شعبان",
        "رمضان",
        "شوال",
        "ذو القعدة",
        "ذو الحجة",
    )

    fun format(date: LocalDate, language: AppLanguage): String {
        val hijri = HijrahDate.from(date)
        val day = hijri.get(ChronoField.DAY_OF_MONTH)
        val month = hijri.get(ChronoField.MONTH_OF_YEAR)
        val year = hijri.get(ChronoField.YEAR_OF_ERA)
        return if (language == AppLanguage.ARABIC) {
            "${toArabicIndic(day)} ${arabicMonths[month - 1]} ${toArabicIndic(year)} هـ"
        } else {
            "$day ${italianMonths[month - 1]} $year AH"
        }
    }

    private fun toArabicIndic(value: Int): String = value.toString().map { digit ->
        if (digit in '0'..'9') ('٠'.code + (digit - '0')).toChar() else digit
    }.joinToString("")
}
''',
)

write(
    "app/src/main/java/com/archimedeprojects/arihna/feature/quran/QuranData.kt",
    '''package com.archimedeprojects.arihna.feature.quran

data class QuranBoundary(val surah: Int, val ayah: Int)

data class HizbMarker(val hizb: Int, val quarter: Int)

data class QuranVerse(
    val surah: Int,
    val ayah: Int,
    val text: String,
    val juzStart: Int? = null,
    val hizbMarker: HizbMarker? = null,
)

data class QuranCorpus(
    val verses: List<QuranVerse>,
    val juzStarts: List<QuranBoundary>,
    val hizbQuarterStarts: List<QuranBoundary>,
) {
    companion object {
        fun parse(text: String, metadata: String): QuranCorpus {
            val juzStarts = parseBoundarySection(metadata, "Juz").filter { it.surah <= 114 }.take(30)
            val hizbStarts = parseBoundarySection(metadata, "HizbQaurter").filter { it.surah <= 114 }.take(240)
            val juzMap = juzStarts.mapIndexed { index, boundary -> boundary to index + 1 }.toMap()
            val hizbMap = hizbStarts.mapIndexed { index, boundary ->
                val zeroBased = index
                boundary to HizbMarker(
                    hizb = zeroBased / 4 + 1,
                    quarter = zeroBased % 4,
                )
            }.toMap()

            val verses = text.lineSequence()
                .filter { it.isNotBlank() }
                .map { line ->
                    val parts = line.split('|', limit = 3)
                    require(parts.size == 3) { "Invalid Tanzil Quran line" }
                    val boundary = QuranBoundary(parts[0].toInt(), parts[1].toInt())
                    QuranVerse(
                        surah = boundary.surah,
                        ayah = boundary.ayah,
                        text = parts[2],
                        juzStart = juzMap[boundary],
                        hizbMarker = hizbMap[boundary],
                    )
                }
                .toList()

            return QuranCorpus(
                verses = verses,
                juzStarts = juzStarts,
                hizbQuarterStarts = hizbStarts,
            )
        }

        private fun parseBoundarySection(metadata: String, section: String): List<QuranBoundary> {
            val heading = "QuranData.$section = ["
            val start = metadata.indexOf(heading)
            require(start >= 0) { "Missing Quran metadata section $section" }
            val end = metadata.indexOf("];", start)
            require(end > start) { "Unterminated Quran metadata section $section" }
            val body = metadata.substring(start, end)
            val pair = Regex("""\\[(\\d+),\\s*(\\d+)]""")
            return pair.findAll(body).map { match ->
                QuranBoundary(
                    surah = match.groupValues[1].toInt(),
                    ayah = match.groupValues[2].toInt(),
                )
            }.toList()
        }
    }
}
''',
)

write(
    "app/src/main/java/com/archimedeprojects/arihna/feature/quran/QuranPlaceholderScreen.kt",
    '''package com.archimedeprojects.arihna.feature.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archimedeprojects.arihna.core.i18n.AppLanguage
import com.archimedeprojects.arihna.core.i18n.LocalArihnaLanguage
import com.archimedeprojects.arihna.core.i18n.arihnaString
import com.archimedeprojects.arihna.core.ui.theme.ArihnaCream
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnBottom
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnGold
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnTop
import com.archimedeprojects.arihna.core.ui.theme.ArihnaForest
import com.archimedeprojects.arihna.core.ui.theme.ArihnaSage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class QuranReaderMode { EASY, HAFS_UTHMANI }

@Composable
fun QuranPlaceholderScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    val language = LocalArihnaLanguage.current
    var mode by remember { mutableStateOf(QuranReaderMode.EASY) }
    var corpus by remember { mutableStateOf<QuranCorpus?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(context) {
        runCatching {
            withContext(Dispatchers.IO) {
                val text = context.assets.open("quran/quran-uthmani.txt").bufferedReader().use { it.readText() }
                val metadata = context.assets.open("quran/quran-data.js").bufferedReader().use { it.readText() }
                QuranCorpus.parse(text, metadata)
            }
        }.onSuccess { corpus = it }.onFailure { error = it.message ?: "Quran data unavailable" }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ArihnaDawnTop, ArihnaDawnBottom)))
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("quran-reader-screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = arihnaString("Corano", "القرآن الكريم"),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = ArihnaForest,
        )
        Text(
            text = arihnaString(
                "Testo Uthmani verificato · lettura offline",
                "نص عثماني موثّق · قراءة دون اتصال",
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = ArihnaForest.copy(alpha = 0.72f),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == QuranReaderMode.EASY,
                onClick = { mode = QuranReaderMode.EASY },
                label = { Text(arihnaString("Facile da leggere", "قراءة سهلة")) },
                modifier = Modifier.testTag("quran-mode-easy"),
            )
            FilterChip(
                selected = mode == QuranReaderMode.HAFS_UTHMANI,
                onClick = { mode = QuranReaderMode.HAFS_UTHMANI },
                label = { Text(arihnaString("Ḥafṣ / Uthmani", "حفص / عثماني")) },
                modifier = Modifier.testTag("quran-mode-hafs"),
            )
        }

        when {
            error != null -> Text(
                text = arihnaString("Corano non disponibile: $error", "تعذر تحميل القرآن: $error"),
                color = MaterialTheme.colorScheme.error,
            )
            corpus == null -> Column(
                modifier = Modifier.fillMaxWidth().padding(top = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(color = ArihnaDawnGold)
            }
            else -> QuranVerseList(corpus = requireNotNull(corpus), mode = mode, language = language)
        }
    }
}

@Composable
private fun QuranVerseList(corpus: QuranCorpus, mode: QuranReaderMode, language: AppLanguage) {
    val uriHandler = LocalUriHandler.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("quran-verses"),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "source") {
            Card(
                colors = CardDefaults.cardColors(containerColor = ArihnaSage.copy(alpha = 0.62f)),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        arihnaString(
                            "Tanzil Project · Quran Text Uthmani v1.1 · CC BY 3.0",
                            "مشروع تنزيل · النص العثماني 1.1 · CC BY 3.0",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = ArihnaForest,
                    )
                    Text(
                        arihnaString(
                            "Il testo coranico è incluso senza modifiche.",
                            "النص القرآني مضمّن دون أي تعديل.",
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = ArihnaForest.copy(alpha = 0.72f),
                    )
                    TextButton(onClick = { uriHandler.openUri("https://tanzil.net") }) {
                        Text("tanzil.net")
                    }
                }
            }
        }

        items(corpus.verses, key = { "${it.surah}:${it.ayah}" }) { verse ->
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                if (verse.ayah == 1) {
                    Text(
                        text = if (language == AppLanguage.ARABIC) {
                            "سورة ${toArabicIndic(verse.surah)}"
                        } else {
                            "Sura ${verse.surah} · سورة ${toArabicIndic(verse.surah)}"
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 3.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.ExtraBold,
                        color = ArihnaDawnGold,
                        fontSize = 19.sp,
                    )
                }
                if (mode == QuranReaderMode.HAFS_UTHMANI && (verse.juzStart != null || verse.hizbMarker != null)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        verse.juzStart?.let { juz ->
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = { Text(arihnaString("Juz $juz", "الجزء ${toArabicIndic(juz)}")) },
                                colors = AssistChipDefaults.assistChipColors(
                                    disabledContainerColor = ArihnaDawnGold.copy(alpha = 0.16f),
                                    disabledLabelColor = ArihnaForest,
                                ),
                                modifier = Modifier.testTag("quran-juz-$juz"),
                            )
                        }
                        verse.hizbMarker?.let { marker ->
                            AssistChip(
                                onClick = {},
                                enabled = false,
                                label = { Text(hizbLabel(marker, language)) },
                                colors = AssistChipDefaults.assistChipColors(
                                    disabledContainerColor = ArihnaSage,
                                    disabledLabelColor = ArihnaForest,
                                ),
                            )
                        }
                    }
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = ArihnaCream),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(
                        text = if (mode == QuranReaderMode.HAFS_UTHMANI) {
                            "${verse.text}  ﴿${toArabicIndic(verse.ayah)}﴾"
                        } else {
                            verse.text
                        },
                        modifier = Modifier.fillMaxWidth().padding(
                            horizontal = if (mode == QuranReaderMode.EASY) 18.dp else 15.dp,
                            vertical = if (mode == QuranReaderMode.EASY) 16.dp else 12.dp,
                        ),
                        textAlign = TextAlign.End,
                        color = ArihnaForest,
                        fontSize = if (mode == QuranReaderMode.EASY) 28.sp else 24.sp,
                        lineHeight = if (mode == QuranReaderMode.EASY) 46.sp else 39.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

private fun hizbLabel(marker: HizbMarker, language: AppLanguage): String {
    val suffix = when (marker.quarter) {
        0 -> ""
        1 -> " · ¼"
        2 -> " · ½"
        else -> " · ¾"
    }
    return if (language == AppLanguage.ARABIC) {
        "الحزب ${toArabicIndic(marker.hizb)}$suffix"
    } else {
        "Hizb ${marker.hizb}$suffix"
    }
}

private fun toArabicIndic(value: Int): String = value.toString().map { digit ->
    if (digit in '0'..'9') ('٠'.code + (digit - '0')).toChar() else digit
}.joinToString("")
''',
)

write(
    "app/src/test/java/com/archimedeprojects/arihna/feature/quran/QuranBundledDataTest.kt",
    '''package com.archimedeprojects.arihna.feature.quran

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuranBundledDataTest {
    @Test
    fun bundledTanzilCorpusHasExpectedCanonicalStructure() {
        val text = asset("quran-uthmani.txt").readText()
        val metadata = asset("quran-data.js").readText()
        val corpus = QuranCorpus.parse(text, metadata)

        assertEquals(6_236, corpus.verses.size)
        assertEquals((1..114).toSet(), corpus.verses.map { it.surah }.toSet())
        assertEquals(QuranBoundary(1, 1), corpus.juzStarts.first())
        assertEquals(QuranBoundary(2, 142), corpus.juzStarts[1])
        assertEquals(30, corpus.juzStarts.size)
        assertEquals(240, corpus.hizbQuarterStarts.size)
        assertEquals(1, corpus.verses.first { it.surah == 1 && it.ayah == 1 }.juzStart)
        assertEquals(2, corpus.verses.first { it.surah == 2 && it.ayah == 142 }.juzStart)
        assertNotNull(corpus.verses.first { it.surah == 2 && it.ayah == 26 }.hizbMarker)
        assertTrue(corpus.verses.all { it.text.isNotBlank() })
    }

    private fun asset(name: String): File = sequenceOf(
        File("app/src/main/assets/quran/$name"),
        File("src/main/assets/quran/$name"),
    ).firstOrNull(File::isFile) ?: error("Missing Quran asset $name")
}
''',
)

write(
    "app/src/androidTest/java/com/archimedeprojects/arihna/core/i18n/AppLanguageAndroidTest.kt",
    '''package com.archimedeprojects.arihna.core.i18n

import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class AppLanguageAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun arabicProviderUsesRtlAndLocalizedCopy() {
        var observed: LayoutDirection? = null
        composeRule.setContent {
            ArihnaLanguageProvider(AppLanguage.ARABIC) {
                observed = LocalLayoutDirection.current
                Text(arihnaString("Italiano", "العربية"))
            }
        }
        composeRule.onNodeWithText("العربية").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(LayoutDirection.Rtl, observed) }
    }

    @Test
    fun languageStorePersistsArabicAndItalian() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = AppLanguageStore(context)
        val previous = store.read()
        try {
            store.set(AppLanguage.ARABIC)
            assertEquals(AppLanguage.ARABIC, AppLanguageStore(context).read())
            store.set(AppLanguage.ITALIAN)
            assertEquals(AppLanguage.ITALIAN, AppLanguageStore(context).read())
        } finally {
            store.set(previous)
        }
    }
}
''',
)

write(
    "docs/quran/TANZIL_SOURCE.md",
    '''# Quran text source

Arihna bundles the Quran text from the **Tanzil Project, Quran Text Uthmani v1.1**.

- Upstream repository: `acfatah/tanzil`
- Pinned upstream commit: `052b515f3a24dfacbe4cafc3b89f0681a447f462`
- Text: `data/quran-uthmani.txt`
- Metadata: `data/quran-data.js`
- License: Creative Commons Attribution 3.0, as distributed by the pinned upstream repository.
- Source website: https://tanzil.net

The Quran text asset is bundled **verbatim and unmodified**. Arihna's two reading modes change only typography and metadata presentation; they do not alter Quran text.
''',
)

# ArihnaApp: persisted language state and RTL provider.
path = "app/src/main/java/com/archimedeprojects/arihna/app/ArihnaApp.kt"
add_import_after(path, "import androidx.compose.runtime.getValue\n", "import androidx.compose.runtime.mutableStateOf\n")
add_import_after(path, "import androidx.compose.runtime.remember\n", "import androidx.compose.runtime.setValue\n")
add_import_after(path, "import com.archimedeprojects.arihna.core.prayer.calculation.AdhanPrayerTimeCalculator\n", "import com.archimedeprojects.arihna.core.i18n.AppLanguageStore\nimport com.archimedeprojects.arihna.core.i18n.ArihnaLanguageProvider\n")
replace_once(
    path,
    '''    ArihnaTheme {
        ArihnaNavHost(
            activity = activity,
            locationSettingsViewModel = locationViewModel,
            prayerScheduleViewModel = prayerScheduleViewModel,
            alarmsViewModel = alarmsViewModel,
            exactAlarmAccessIntentFactory = appContainer.exactAlarmAccessIntentFactory,
            alarmFullScreenAccess = appContainer.alarmFullScreenAccess,
            alarmDiagnosticTestScheduler = appContainer.alarmDiagnosticTestScheduler,
            qiblaRepository = qiblaRepository,
            locationEnvironment = appContainer.locationEnvironment,
            locationPermissionStateResolver = appContainer.locationPermissionStateResolver,
        )
        StartupCapabilityGate(
            activity = activity,
            locationViewModel = locationViewModel,
            locationEnvironment = appContainer.locationEnvironment,
            locationPermissionStateResolver = appContainer.locationPermissionStateResolver,
            alarmsViewModel = alarmsViewModel,
            alarmPlatformScheduler = appContainer.alarmPlatformScheduler,
            alarmNotificationPermissionReader = appContainer.alarmNotificationPermissionReader,
            exactAlarmAccessIntentFactory = appContainer.exactAlarmAccessIntentFactory,
            alarmFullScreenAccess = appContainer.alarmFullScreenAccess,
        )
    }
''',
    '''    val languageStore = remember(activity.applicationContext) { AppLanguageStore(activity.applicationContext) }
    var appLanguage by remember(languageStore) { mutableStateOf(languageStore.read()) }

    ArihnaTheme {
        ArihnaLanguageProvider(appLanguage) {
            ArihnaNavHost(
                activity = activity,
                locationSettingsViewModel = locationViewModel,
                prayerScheduleViewModel = prayerScheduleViewModel,
                alarmsViewModel = alarmsViewModel,
                exactAlarmAccessIntentFactory = appContainer.exactAlarmAccessIntentFactory,
                alarmFullScreenAccess = appContainer.alarmFullScreenAccess,
                alarmDiagnosticTestScheduler = appContainer.alarmDiagnosticTestScheduler,
                qiblaRepository = qiblaRepository,
                locationEnvironment = appContainer.locationEnvironment,
                locationPermissionStateResolver = appContainer.locationPermissionStateResolver,
                appLanguage = appLanguage,
                onAppLanguageChange = { language ->
                    languageStore.set(language)
                    appLanguage = language
                },
            )
            StartupCapabilityGate(
                activity = activity,
                locationViewModel = locationViewModel,
                locationEnvironment = appContainer.locationEnvironment,
                locationPermissionStateResolver = appContainer.locationPermissionStateResolver,
                alarmsViewModel = alarmsViewModel,
                alarmPlatformScheduler = appContainer.alarmPlatformScheduler,
                alarmNotificationPermissionReader = appContainer.alarmNotificationPermissionReader,
                exactAlarmAccessIntentFactory = appContainer.exactAlarmAccessIntentFactory,
                alarmFullScreenAccess = appContainer.alarmFullScreenAccess,
            )
        }
    }
''',
)

# Navigation: localized chrome, Quran quick route, language selector callback.
path = "app/src/main/java/com/archimedeprojects/arihna/app/ArihnaNavHost.kt"
add_import_after(path, "import com.archimedeprojects.arihna.core.location.platform.AndroidLocationPermissionStateResolver\n", "import com.archimedeprojects.arihna.core.i18n.AppLanguage\n")
replace_once(
    path,
    '''private enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Home("home", "Home", Icons.Rounded.Home),
    Prayers("prayers", "Orari", Icons.Rounded.Schedule),
    Qibla("qibla", "Qibla", Icons.Rounded.Explore),
    Quran("quran", "Corano", Icons.Rounded.MenuBook),
    Alarms("alarms", "Sveglie", Icons.Rounded.Alarm),
    Settings("settings", "Impostazioni", Icons.Rounded.Settings),
}
''',
    '''private enum class Destination(
    val route: String,
    val label: String,
    val arabicLabel: String,
    val icon: ImageVector,
) {
    Home("home", "Home", "الرئيسية", Icons.Rounded.Home),
    Prayers("prayers", "Orari", "مواقيت الصلاة", Icons.Rounded.Schedule),
    Qibla("qibla", "Qibla", "القبلة", Icons.Rounded.Explore),
    Quran("quran", "Corano", "القرآن", Icons.Rounded.MenuBook),
    Alarms("alarms", "Sveglie", "المنبّهات", Icons.Rounded.Alarm),
    Settings("settings", "Impostazioni", "الإعدادات", Icons.Rounded.Settings),
}
''',
)
replace_once(
    path,
    '''    locationEnvironment: AndroidLocationEnvironment,
    locationPermissionStateResolver: AndroidLocationPermissionStateResolver,
) {''',
    '''    locationEnvironment: AndroidLocationEnvironment,
    locationPermissionStateResolver: AndroidLocationPermissionStateResolver,
    appLanguage: AppLanguage,
    onAppLanguageChange: (AppLanguage) -> Unit,
) {''',
)
replace_once(path, "contentDescription = destination.label,", "contentDescription = if (appLanguage == AppLanguage.ARABIC) destination.arabicLabel else destination.label,")
replace_once(
    path,
    '''                    onOpenAlarms = {
                        navController.navigateTopLevel(Destination.Alarms.route, Destination.Home.route)
                    },
                    onRefreshLocation = {''',
    '''                    onOpenAlarms = {
                        navController.navigateTopLevel(Destination.Alarms.route, Destination.Home.route)
                    },
                    onOpenQuran = {
                        navController.navigateTopLevel(Destination.Quran.route, Destination.Home.route)
                    },
                    onRefreshLocation = {''',
)
replace_once(
    path,
    '''                    alarmDiagnosticTestScheduler = alarmDiagnosticTestScheduler,
                )''',
    '''                    alarmDiagnosticTestScheduler = alarmDiagnosticTestScheduler,
                    appLanguage = appLanguage,
                    onAppLanguageChange = onAppLanguageChange,
                )''',
)

# Settings: language switch and elegant Material3 alarm-volume slider.
path = "app/src/main/java/com/archimedeprojects/arihna/feature/settings/LocationSettingsScreen.kt"
add_import_after(path, "import androidx.compose.material3.OutlinedTextFieldDefaults\n", "import androidx.compose.material3.Slider\nimport androidx.compose.material3.SliderDefaults\n")
add_import_after(path, "import com.archimedeprojects.arihna.core.location.platform.AndroidLocationPermissionStateResolver\n", "import com.archimedeprojects.arihna.core.i18n.AppLanguage\nimport com.archimedeprojects.arihna.core.i18n.arihnaString\n")
add_import_after(path, "import com.archimedeprojects.arihna.feature.alarms.platform.ExactAlarmAccessIntentFactory\n", "import kotlin.math.roundToInt\n")
replace_once(
    path,
    '''    alarmFullScreenAccess: AlarmFullScreenAccess,
    alarmDiagnosticTestScheduler: AlarmDiagnosticTestScheduler,
) {''',
    '''    alarmFullScreenAccess: AlarmFullScreenAccess,
    alarmDiagnosticTestScheduler: AlarmDiagnosticTestScheduler,
    appLanguage: AppLanguage,
    onAppLanguageChange: (AppLanguage) -> Unit,
) {''',
)
replace_once(
    path,
    '''        onCancelDiagnostic = {
            alarmDiagnosticTestScheduler.cancel()
            diagnosticMessage = "Test in attesa annullato"
        },
    )''',
    '''        onCancelDiagnostic = {
            alarmDiagnosticTestScheduler.cancel()
            diagnosticMessage = "Test in attesa annullato"
        },
        appLanguage = appLanguage,
        onAppLanguageChange = onAppLanguageChange,
    )''',
)
replace_once(
    path,
    '''    onTestAdhan: () -> Unit = {},
    onCancelDiagnostic: () -> Unit = {},
) {''',
    '''    onTestAdhan: () -> Unit = {},
    onCancelDiagnostic: () -> Unit = {},
    appLanguage: AppLanguage = AppLanguage.ITALIAN,
    onAppLanguageChange: (AppLanguage) -> Unit = {},
) {''',
)
replace_once(
    path,
    '''                Text(
                    text = "Impostazioni",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = SettingsText,
                )
            }
        }

        SettingsSectionTitle("Posizione", "settings-section-location")''',
    '''                Text(
                    text = arihnaString("Impostazioni", "الإعدادات"),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = SettingsText,
                )
            }
            LanguageSelector(appLanguage = appLanguage, onLanguageChange = onAppLanguageChange)
        }

        SettingsSectionTitle(arihnaString("Posizione", "الموقع"), "settings-section-location")''',
)
replace_once(path, 'SettingsSectionTitle("Sveglia", "settings-section-alarms")', 'SettingsSectionTitle(arihnaString("Sveglia", "المنبّه"), "settings-section-alarms")')
replace_once(path, 'SettingsSectionTitle("Test rapidi", "settings-section-tests")', 'SettingsSectionTitle(arihnaString("Test rapidi", "اختبارات سريعة"), "settings-section-tests")')
insert_anchor = '''@Composable
private fun LocationControlCard('''
language_selector = '''@Composable
private fun LanguageSelector(
    appLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        TextButton(
            onClick = { onLanguageChange(AppLanguage.ITALIAN) },
            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp),
            colors = ButtonDefaults.textButtonColors(
                containerColor = if (appLanguage == AppLanguage.ITALIAN) SettingsAccent.copy(alpha = 0.16f) else Color.Transparent,
                contentColor = SettingsText,
            ),
            modifier = Modifier.testTag("settings-language-italian"),
        ) {
            Text("IT", fontWeight = FontWeight.Bold)
        }
        TextButton(
            onClick = { onLanguageChange(AppLanguage.ARABIC) },
            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp),
            colors = ButtonDefaults.textButtonColors(
                containerColor = if (appLanguage == AppLanguage.ARABIC) SettingsAccent.copy(alpha = 0.16f) else Color.Transparent,
                contentColor = SettingsText,
            ),
            modifier = Modifier.testTag("settings-language-arabic"),
        ) {
            Text("العربية", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LocationControlCard('''
replace_once(path, insert_anchor, language_selector)
replace_once(
    path,
    '''    val canDecrease = current > volume.min
    val canIncrease = current < volume.max
    Column(''',
    '''    val rangeSize = (volume.max - volume.min).coerceAtLeast(1)
    Column(''',
)
replace_once(path, '"Volume sveglia",', 'arihnaString("Volume sveglia", "مستوى صوت المنبّه"),')
old_controls = '''        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onAlarmVolumeChange(current - 1) },
                enabled = canDecrease,
                modifier = Modifier
                    .background(SettingsSurfaceRaised, RoundedCornerShape(14.dp))
                    .testTag("settings-alarm-volume-decrease"),
            ) {
                Text(
                    "−",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (canDecrease) SettingsText else SettingsMuted.copy(alpha = 0.45f),
                )
            }
            Text(
                "$current / ${volume.max}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = SettingsMuted,
            )
            IconButton(
                onClick = { onAlarmVolumeChange(current + 1) },
                enabled = canIncrease,
                modifier = Modifier
                    .background(SettingsSurfaceRaised, RoundedCornerShape(14.dp))
                    .testTag("settings-alarm-volume-increase"),
            ) {
                Text(
                    "+",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (canIncrease) SettingsText else SettingsMuted.copy(alpha = 0.45f),
                )
            }
        }
'''
new_controls = '''        Slider(
            value = current.toFloat(),
            onValueChange = { requested ->
                onAlarmVolumeChange(requested.roundToInt().coerceIn(volume.min, volume.max))
            },
            valueRange = volume.min.toFloat()..(volume.min + rangeSize).toFloat(),
            steps = (rangeSize - 1).coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = SettingsAccent,
                activeTrackColor = SettingsAccent,
                inactiveTrackColor = SettingsSurfaceRaised,
                activeTickColor = SettingsSurface,
                inactiveTickColor = SettingsMuted.copy(alpha = 0.28f),
            ),
            modifier = Modifier.fillMaxWidth().testTag("settings-alarm-volume-slider"),
        )
'''
replace_once(path, old_controls, new_controls)
replace_once(
    path,
    '''        Text(
            "Volume globale delle sveglie del telefono",''',
    '''        Text(
            arihnaString("Volume globale delle sveglie del telefono", "مستوى الصوت العام لمنبّهات الهاتف"),''',
)

# Home: Hijri date, calendar dialog, Quran quick action.
path = "app/src/main/java/com/archimedeprojects/arihna/feature/home/HomePrayerScheduleScreen.kt"
add_import_after(path, "import androidx.compose.foundation.background\n", "import androidx.compose.foundation.clickable\n")
add_import_after(path, "import androidx.compose.material.icons.rounded.LocationOn\n", "import androidx.compose.material.icons.rounded.MenuBook\n")
add_import_after(path, "import androidx.compose.material3.CircularProgressIndicator\n", "import androidx.compose.material3.DatePicker\nimport androidx.compose.material3.DatePickerDialog\nimport androidx.compose.material3.ExperimentalMaterial3Api\n")
add_import_after(path, "import androidx.compose.material3.OutlinedButton\n", "import androidx.compose.material3.TextButton\nimport androidx.compose.material3.rememberDatePickerState\n")
add_import_after(path, "import androidx.compose.runtime.getValue\n", "import androidx.compose.runtime.mutableStateOf\nimport androidx.compose.runtime.remember\nimport androidx.compose.runtime.setValue\n")
add_import_after(path, "import com.archimedeprojects.arihna.core.ui.theme.ArihnaCream\n", "import com.archimedeprojects.arihna.core.calendar.HijriDateFormatter\nimport com.archimedeprojects.arihna.core.i18n.AppLanguage\nimport com.archimedeprojects.arihna.core.i18n.LocalArihnaLanguage\nimport com.archimedeprojects.arihna.core.i18n.arihnaString\nimport com.archimedeprojects.arihna.core.i18n.locale\n")
add_import_after(path, "import java.time.ZoneId\n", "import java.time.ZoneOffset\n")
replace_once(
    path,
    '''    onOpenAlarms: () -> Unit,
    onRefreshLocation: () -> Unit,
) {''',
    '''    onOpenAlarms: () -> Unit,
    onOpenQuran: () -> Unit,
    onRefreshLocation: () -> Unit,
) {''',
)
replace_once(
    path,
    '''        onOpenAlarms = onOpenAlarms,
        onRefreshLocation = onRefreshLocation,
    )''',
    '''        onOpenAlarms = onOpenAlarms,
        onOpenQuran = onOpenQuran,
        onRefreshLocation = onRefreshLocation,
    )''',
)
replace_once(
    path,
    '''    onOpenAlarms: () -> Unit = {},
    onRefreshLocation: () -> Unit = {},
) {''',
    '''    onOpenAlarms: () -> Unit = {},
    onOpenQuran: () -> Unit = {},
    onRefreshLocation: () -> Unit = {},
) {''',
)
replace_once(
    path,
    '''                onOpenAlarms = onOpenAlarms,
                onRefreshLocation = onRefreshLocation,
            )''',
    '''                onOpenAlarms = onOpenAlarms,
                onOpenQuran = onOpenQuran,
                onRefreshLocation = onRefreshLocation,
            )''',
)
replace_once(
    path,
    '''    onOpenAlarms: () -> Unit,
    onRefreshLocation: () -> Unit,
) {
    val zoneId = state.today.zoneId''',
    '''    onOpenAlarms: () -> Unit,
    onOpenQuran: () -> Unit,
    onRefreshLocation: () -> Unit,
) {
    val zoneId = state.today.zoneId''',
)
replace_once(
    path,
    '''        onOpenAlarms = onOpenAlarms,
        onOpenLocationSettings = onOpenLocationSettings,
    )''',
    '''        onOpenAlarms = onOpenAlarms,
        onOpenQuran = onOpenQuran,
    )''',
)
# Keep settings parameter in ReadyContent for no-location compatibility, but quick actions no longer uses it.
replace_once(
    path,
    '''private fun HomeHeader(state: PrayerScheduleUiState.Ready, onRefreshLocation: () -> Unit) {
    Row(''',
    '''private fun HomeHeader(state: PrayerScheduleUiState.Ready, onRefreshLocation: () -> Unit) {
    val language = LocalArihnaLanguage.current
    var calendarVisible by remember(state.localDate) { mutableStateOf(false) }
    Row(''',
)
replace_once(
    path,
    '''            Text(
                text = formatDate(state.localDate),
                style = MaterialTheme.typography.bodySmall,
                color = HomeMuted,
                modifier = Modifier.testTag("home-current-date"),
            )''',
    '''            Column(
                modifier = Modifier
                    .clickable { calendarVisible = true }
                    .testTag("home-date-calendar-action"),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = formatDate(state.localDate, language),
                    style = MaterialTheme.typography.bodySmall,
                    color = HomeMuted,
                    modifier = Modifier.testTag("home-current-date"),
                )
                Text(
                    text = "${arihnaString("Hijri", "هجري")} · ${HijriDateFormatter.format(state.localDate, language)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = HomeAccent,
                    modifier = Modifier.testTag("home-hijri-date"),
                )
            }''',
)
replace_once(
    path,
    '''        }
    }
}

@Composable
private fun NextPrayerHero''',
    '''        }
    }
    if (calendarVisible) {
        HomeCalendarDialog(initialDate = state.localDate, onDismiss = { calendarVisible = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeCalendarDialog(initialDate: LocalDate, onDismiss: () -> Unit) {
    val language = LocalArihnaLanguage.current
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    val selectedDate = pickerState.selectedDateMillis?.let { millis ->
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
    } ?: initialDate

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(arihnaString("Chiudi", "إغلاق"))
            }
        },
        modifier = Modifier.testTag("home-calendar-dialog"),
    ) {
        Column {
            DatePicker(state = pickerState)
            Text(
                text = "${arihnaString("Data Hijri", "التاريخ الهجري")}: ${HijriDateFormatter.format(selectedDate, language)}",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp).testTag("home-calendar-hijri"),
                color = HomeText,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun NextPrayerHero''',
)
replace_once(path, 'text = "PROSSIMA PREGHIERA",', 'text = arihnaString("PROSSIMA PREGHIERA", "الصلاة القادمة"),')
replace_once(path, 'text = "OGGI",', 'text = arihnaString("OGGI", "اليوم"),')
replace_once(path, 'text = "SETTIMANA",', 'text = arihnaString("SETTIMANA", "الأسبوع"),')
replace_once(
    path,
    '''private fun QuickActions(
    onOpenQibla: () -> Unit,
    onOpenAlarms: () -> Unit,
    onOpenLocationSettings: () -> Unit,
) {''',
    '''private fun QuickActions(
    onOpenQibla: () -> Unit,
    onOpenAlarms: () -> Unit,
    onOpenQuran: () -> Unit,
) {''',
)
replace_once(
    path,
    '''        QuickActionButton("Qibla", Icons.Rounded.Explore, onOpenQibla, Modifier.weight(1f))
        QuickActionButton("Sveglie", Icons.Rounded.Alarm, onOpenAlarms, Modifier.weight(1f))
        QuickActionButton("Posizione", Icons.Rounded.LocationOn, onOpenLocationSettings, Modifier.weight(1f))''',
    '''        QuickActionButton(arihnaString("Qibla", "القبلة"), Icons.Rounded.Explore, onOpenQibla, Modifier.weight(1f))
        QuickActionButton(arihnaString("Sveglie", "المنبّهات"), Icons.Rounded.Alarm, onOpenAlarms, Modifier.weight(1f))
        QuickActionButton(arihnaString("Corano", "القرآن"), Icons.Rounded.MenuBook, onOpenQuran, Modifier.weight(1f))''',
)
replace_once(
    path,
    '''private fun formatDate(date: LocalDate): String = DATE_FORMATTER.format(date)''',
    '''private fun formatDate(date: LocalDate, language: AppLanguage): String =
    DateTimeFormatter.ofPattern("EEEE d MMMM", language.locale()).format(date)''',
)
replace_once(path, '\nprivate val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALIAN)', '')

# Home Android tests: Quran quick action and calendar/Hijri behavior.
path = "app/src/androidTest/java/com/archimedeprojects/arihna/feature/home/HomePrayerScheduleScreenAndroidTest.kt"
replace_once(path, 'composeRule.onNodeWithText("Posizione").assertIsDisplayed()', 'composeRule.onNodeWithText("Corano").assertIsDisplayed()')
replace_once(
    path,
    '''        var qibla = 0
        var alarms = 0
        var location = 0
        setScreen(
            uiState = readyState(),
            onOpenQibla = { qibla += 1 },
            onOpenAlarms = { alarms += 1 },
            onOpenLocationSettings = { location += 1 },
        )

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Qibla"))
        composeRule.onNodeWithText("Qibla").performClick()
        composeRule.onNodeWithText("Sveglie").performClick()
        composeRule.onNodeWithText("Posizione").performClick()
        composeRule.runOnIdle {
            assertEquals(1, qibla)
            assertEquals(1, alarms)
            assertEquals(1, location)
        }''',
    '''        var qibla = 0
        var alarms = 0
        var quran = 0
        setScreen(
            uiState = readyState(),
            onOpenQibla = { qibla += 1 },
            onOpenAlarms = { alarms += 1 },
            onOpenQuran = { quran += 1 },
        )

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Qibla"))
        composeRule.onNodeWithText("Qibla").performClick()
        composeRule.onNodeWithText("Sveglie").performClick()
        composeRule.onNodeWithText("Corano").performClick()
        composeRule.runOnIdle {
            assertEquals(1, qibla)
            assertEquals(1, alarms)
            assertEquals(1, quran)
        }''',
)
replace_once(
    path,
    '''    @Test
    fun calculationUnavailableShowsControlledErrorWithoutInventedTimes() {''',
    '''    @Test
    fun currentDateShowsHijriAndOpensCalendarViewer() {
        setScreen(uiState = readyState())

        composeRule.onNodeWithTag("home-hijri-date").assertIsDisplayed()
        composeRule.onNodeWithTag("home-date-calendar-action").performClick()
        composeRule.onNodeWithTag("home-calendar-dialog").assertIsDisplayed()
        composeRule.onNodeWithTag("home-calendar-hijri").assertIsDisplayed()
    }

    @Test
    fun calculationUnavailableShowsControlledErrorWithoutInventedTimes() {''',
)
replace_once(
    path,
    '''        onOpenAlarms: () -> Unit = {},
        onRefreshLocation: () -> Unit = {},
    ) {''',
    '''        onOpenAlarms: () -> Unit = {},
        onOpenQuran: () -> Unit = {},
        onRefreshLocation: () -> Unit = {},
    ) {''',
)
replace_once(
    path,
    '''                    onOpenAlarms = onOpenAlarms,
                    onRefreshLocation = onRefreshLocation,
                )''',
    '''                    onOpenAlarms = onOpenAlarms,
                    onOpenQuran = onOpenQuran,
                    onRefreshLocation = onRefreshLocation,
                )''',
)

# Settings Android test: slider is present.
path = "app/src/androidTest/java/com/archimedeprojects/arihna/feature/settings/AlarmSettingsOverlayVolumeAndroidTest.kt"
add_import_after(path, "import androidx.compose.ui.test.onNodeWithText\n", "import androidx.compose.ui.test.onNodeWithTag\n")
replace_once(
    path,
    '''        composeRule.onNodeWithText("53%").assertIsDisplayed()
        composeRule.onNodeWithText("Volume globale delle sveglie del telefono").assertIsDisplayed()''',
    '''        composeRule.onNodeWithText("53%").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-alarm-volume-slider").assertIsDisplayed()
        composeRule.onNodeWithText("Volume globale delle sveglie del telefono").assertIsDisplayed()''',
)

# Primary Arabic top-level chrome on Orari, Qibla and Alarms.
path = "app/src/main/java/com/archimedeprojects/arihna/feature/prayers/PrayerTimesPlaceholderScreen.kt"
add_import_after(path, "import com.archimedeprojects.arihna.core.ui.theme.ArihnaCream\n", "import com.archimedeprojects.arihna.core.i18n.arihnaString\n")
replace_once(path, '                    "Orari",', '                    arihnaString("Orari", "مواقيت الصلاة"),')
replace_once(path, '                    "Orari di preghiera e promemoria",', '                    arihnaString("Orari di preghiera e promemoria", "مواقيت الصلاة والتذكير"),')
replace_once(path, '                            "Oggi • ${scheduleState.location.displayName}",', '                            "${arihnaString("Oggi", "اليوم")} • ${scheduleState.location.displayName}",')

path = "app/src/main/java/com/archimedeprojects/arihna/feature/qibla/QiblaPlaceholderScreen.kt"
add_import_after(path, "import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnBottom\n", "import com.archimedeprojects.arihna.core.i18n.arihnaString\n")
replace_once(path, '        Text("Qibla", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)', '        Text(arihnaString("Qibla", "القبلة"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)')
replace_once(path, '        Text("Direzione verso la Kaaba · nord vero", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)', '        Text(arihnaString("Direzione verso la Kaaba · nord vero", "الاتجاه نحو الكعبة · الشمال الحقيقي"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)')
replace_once(path, '    Button(onClick = onOpenLocationSettings) { Text("Configura posizione") }', '    Button(onClick = onOpenLocationSettings) { Text(arihnaString("Configura posizione", "إعداد الموقع")) }')

path = "app/src/main/java/com/archimedeprojects/arihna/feature/alarms/AlarmsPlaceholderScreen.kt"
add_import_after(path, "import com.archimedeprojects.arihna.core.ui.theme.ArihnaGold\n", "import com.archimedeprojects.arihna.core.i18n.arihnaString\n")
replace_once(path, '            "Sveglie",', '            arihnaString("Sveglie", "المنبّهات"),')
replace_once(path, '                Text("Nuova", fontWeight = FontWeight.ExtraBold)', '                Text(arihnaString("Nuova", "جديد"), fontWeight = FontWeight.ExtraBold)')
replace_once(path, '                            "Nessuna sveglia",', '                            arihnaString("Nessuna sveglia", "لا توجد منبّهات"),')
replace_once(path, '                            "Crea la prima con Nuova sveglia.",', '                            arihnaString("Crea la prima con Nuova sveglia.", "أنشئ أول منبّه بالضغط على جديد."),')

# Remove obsolete Settings +/- imports only if they became unused because IconButton is still used by location search, so keep it.

print("Arihna Arabic/Hijri/Quran/slider revision applied")
