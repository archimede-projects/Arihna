from pathlib import Path
import re

ROOT = Path('.')

def read(path):
    return (ROOT / path).read_text(encoding='utf-8')

def write(path, text):
    p = ROOT / path
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding='utf-8')

def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected one target, found {count}')
    return text.replace(old, new, 1)

def regex_once(text, pattern, replacement, label):
    updated, count = re.subn(pattern, replacement, text, count=1, flags=re.S)
    if count != 1:
        raise SystemExit(f'{label}: expected one regex target, found {count}')
    return updated

# -----------------------------------------------------------------------------
# Shared in-app language controller. Preference is intentionally app-owned and
# dependency-free. Compose layout direction follows the selected language.
# -----------------------------------------------------------------------------
write('app/src/main/java/com/archimedeprojects/arihna/core/i18n/AppLanguage.kt', r'''package com.archimedeprojects.arihna.core.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val storageValue: String) {
    ITALIAN("it"),
    ARABIC("ar");

    companion object {
        fun fromStorage(value: String?): AppLanguage =
            entries.firstOrNull { it.storageValue == value } ?: ITALIAN
    }
}

@Stable
class AppLanguageController(context: Context) {
    private val preferences = context.getSharedPreferences("arihna_ui_preferences", Context.MODE_PRIVATE)

    var language by mutableStateOf(AppLanguage.fromStorage(preferences.getString(KEY_LANGUAGE, null)))
        private set

    fun setLanguage(value: AppLanguage) {
        if (value == language) return
        preferences.edit().putString(KEY_LANGUAGE, value.storageValue).apply()
        language = value
    }

    companion object {
        private const val KEY_LANGUAGE = "app_language"
    }
}

val LocalAppLanguageController = staticCompositionLocalOf<AppLanguageController> {
    error("AppLanguageController not provided")
}

@Composable
fun appText(italian: String, arabic: String): String =
    if (LocalAppLanguageController.current.language == AppLanguage.ARABIC) arabic else italian

@Composable
fun isArabicLanguage(): Boolean =
    LocalAppLanguageController.current.language == AppLanguage.ARABIC
''')

# -----------------------------------------------------------------------------
# Hijri date formatting uses java.time HijrahChronology/HijrahDate, not a
# hand-authored conversion table.
# -----------------------------------------------------------------------------
write('app/src/main/java/com/archimedeprojects/arihna/core/calendar/HijriDateFormatter.kt', r'''package com.archimedeprojects.arihna.core.calendar

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

object HijriDateFormatter {
    private val italianMonths = listOf(
        "Muharram", "Safar", "Rabiʿ al-awwal", "Rabiʿ al-thani",
        "Jumada al-awwal", "Jumada al-thani", "Rajab", "Shaʿban",
        "Ramadan", "Shawwal", "Dhu al-Qiʿdah", "Dhu al-Hijjah",
    )
    private val arabicMonths = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة",
    )

    fun format(date: LocalDate, arabic: Boolean): String {
        val hijri = HijrahDate.from(date)
        val day = hijri.get(ChronoField.DAY_OF_MONTH)
        val month = hijri.get(ChronoField.MONTH_OF_YEAR)
        val year = hijri.get(ChronoField.YEAR_OF_ERA)
        val monthName = (if (arabic) arabicMonths else italianMonths)[month - 1]
        return if (arabic) "$day $monthName $year هـ" else "$day $monthName $year AH"
    }
}
''')

# -----------------------------------------------------------------------------
# Pure Quran corpus parser. It keeps Tanzil line text unchanged after the second
# pipe and reads only immutable Juz/Hizb boundary metadata.
# -----------------------------------------------------------------------------
write('app/src/main/java/com/archimedeprojects/arihna/feature/quran/QuranCorpus.kt', r'''package com.archimedeprojects.arihna.feature.quran

import android.content.Context

data class QuranAyah(
    val surah: Int,
    val ayah: Int,
    val text: String,
)

data class QuranBoundary(
    val number: Int,
    val surah: Int,
    val ayah: Int,
)

data class QuranCorpus(
    val ayahs: List<QuranAyah>,
    val juzBoundaries: List<QuranBoundary>,
    val hizbBoundaries: List<QuranBoundary>,
) {
    val surahNumbers: List<Int> = ayahs.map { it.surah }.distinct()

    fun ayahsForSurah(surah: Int): List<QuranAyah> = ayahs.filter { it.surah == surah }

    fun juzAt(surah: Int, ayah: Int): Int? =
        juzBoundaries.firstOrNull { it.surah == surah && it.ayah == ayah }?.number

    fun hizbAt(surah: Int, ayah: Int): Int? =
        hizbBoundaries.firstOrNull { it.surah == surah && it.ayah == ayah }?.number

    companion object {
        private val boundaryRegex = Regex(
            "\\\"(\\d+)\\\"\\s*:\\s*\\{\\s*\\\"surahNum\\\"\\s*:\\s*(\\d+)\\s*,\\s*\\\"ayahNum\\\"\\s*:\\s*(\\d+)",
        )

        fun parse(quranText: String, juzJson: String, hizbJson: String): QuranCorpus {
            val ayahs = quranText.lineSequence().mapNotNull { line ->
                if (line.isBlank() || line.startsWith('#')) return@mapNotNull null
                val parts = line.split('|', limit = 3)
                if (parts.size != 3) return@mapNotNull null
                val surah = parts[0].toIntOrNull() ?: return@mapNotNull null
                val ayah = parts[1].toIntOrNull() ?: return@mapNotNull null
                QuranAyah(surah = surah, ayah = ayah, text = parts[2])
            }.toList()
            return QuranCorpus(
                ayahs = ayahs,
                juzBoundaries = parseBoundaries(juzJson),
                hizbBoundaries = parseBoundaries(hizbJson),
            )
        }

        private fun parseBoundaries(json: String): List<QuranBoundary> =
            boundaryRegex.findAll(json).map { match ->
                QuranBoundary(
                    number = match.groupValues[1].toInt(),
                    surah = match.groupValues[2].toInt(),
                    ayah = match.groupValues[3].toInt(),
                )
            }.toList()

        fun load(context: Context): QuranCorpus {
            fun asset(name: String): String = context.assets.open("quran/$name")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            return parse(
                quranText = asset("quran-uthmani.txt"),
                juzJson = asset("juz-info.json"),
                hizbJson = asset("hizb-info.json"),
            )
        }
    }
}
''')

# -----------------------------------------------------------------------------
# Replace Quran placeholder with a real offline reader.
# -----------------------------------------------------------------------------
write('app/src/main/java/com/archimedeprojects/arihna/feature/quran/QuranPlaceholderScreen.kt', r'''package com.archimedeprojects.arihna.feature.quran

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archimedeprojects.arihna.core.i18n.appText
import com.archimedeprojects.arihna.core.ui.theme.ArihnaCream
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnBottom
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnGold
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnTop
import com.archimedeprojects.arihna.core.ui.theme.ArihnaForest
import com.archimedeprojects.arihna.core.ui.theme.ArihnaSage
import com.archimedeprojects.arihna.core.ui.theme.ArihnaWarmOutline

enum class QuranReadingMode { EASY, HAFS_UTHMANI }

@Composable
fun QuranPlaceholderScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    val corpus = remember(context) { QuranCorpus.load(context.applicationContext) }
    var mode by remember { mutableStateOf(QuranReadingMode.EASY) }
    var selectedSurah by remember { mutableIntStateOf(1) }
    var surahMenu by remember { mutableStateOf(false) }
    val visibleAyahs = remember(corpus, selectedSurah) { corpus.ayahsForSurah(selectedSurah) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ArihnaDawnTop, ArihnaDawnBottom)))
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("quran-reader"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            appText("Corano", "القرآن الكريم"),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = ArihnaForest,
        )
        Text(
            appText(
                "Testo Uthmani verificato · lettura offline",
                "نص عثماني موثّق · قراءة دون اتصال",
            ),
            style = MaterialTheme.typography.bodySmall,
            color = ArihnaForest.copy(alpha = 0.68f),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = mode == QuranReadingMode.EASY,
                onClick = { mode = QuranReadingMode.EASY },
                label = { Text(appText("Facile da leggere", "قراءة سهلة")) },
                modifier = Modifier.testTag("quran-mode-easy"),
            )
            FilterChip(
                selected = mode == QuranReadingMode.HAFS_UTHMANI,
                onClick = { mode = QuranReadingMode.HAFS_UTHMANI },
                label = { Text(appText("Ḥafṣ / Uthmani", "حفص / عثماني")) },
                modifier = Modifier.testTag("quran-mode-hafs"),
            )
        }

        Box {
            OutlinedButton(
                onClick = { surahMenu = true },
                border = BorderStroke(1.dp, ArihnaWarmOutline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ArihnaForest),
                modifier = Modifier.testTag("quran-surah-selector"),
            ) {
                Text(appText("Sura $selectedSurah", "سورة $selectedSurah"), fontWeight = FontWeight.Bold)
            }
            DropdownMenu(expanded = surahMenu, onDismissRequest = { surahMenu = false }) {
                corpus.surahNumbers.forEach { surah ->
                    DropdownMenuItem(
                        text = { Text(appText("Sura $surah", "سورة $surah")) },
                        onClick = {
                            selectedSurah = surah
                            surahMenu = false
                        },
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("quran-ayah-list"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(visibleAyahs, key = { "${it.surah}:${it.ayah}" }) { ayah ->
                QuranAyahCard(ayah = ayah, corpus = corpus, mode = mode)
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ArihnaSage.copy(alpha = 0.58f)),
                    border = BorderStroke(1.dp, ArihnaWarmOutline),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quran-attribution"),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            appText("Fonte del testo", "مصدر النص"),
                            fontWeight = FontWeight.Bold,
                            color = ArihnaForest,
                        )
                        Text(
                            "Tanzil Quran Text (Uthmani, Version 1.1) · CC BY 3.0 · tanzil.net",
                            style = MaterialTheme.typography.bodySmall,
                            color = ArihnaForest,
                        )
                        Text(
                            appText(
                                "Il testo coranico è distribuito verbatim e non modificato.",
                                "النص القرآني موزّع كما هو دون تعديل.",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = ArihnaForest.copy(alpha = 0.72f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuranAyahCard(ayah: QuranAyah, corpus: QuranCorpus, mode: QuranReadingMode) {
    val juz = if (mode == QuranReadingMode.HAFS_UTHMANI) corpus.juzAt(ayah.surah, ayah.ayah) else null
    val hizb = if (mode == QuranReadingMode.HAFS_UTHMANI) corpus.hizbAt(ayah.surah, ayah.ayah) else null
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ArihnaCream),
        border = BorderStroke(1.dp, ArihnaWarmOutline),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = if (mode == QuranReadingMode.EASY) 18.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (juz != null || hizb != null) {
                val marker = buildList {
                    juz?.let { add(appText("Juz $it", "الجزء $it")) }
                    hizb?.let { add(appText("Hizb $it", "الحزب $it")) }
                }.joinToString("  •  ")
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
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quran-ayah-${ayah.surah}-${ayah.ayah}"),
                textAlign = TextAlign.End,
                fontSize = if (mode == QuranReadingMode.EASY) 30.sp else 26.sp,
                lineHeight = if (mode == QuranReadingMode.EASY) 48.sp else 42.sp,
                color = ArihnaForest,
                fontWeight = FontWeight.Medium,
            )
            if (mode == QuranReadingMode.HAFS_UTHMANI) {
                Text(
                    text = "﴿${toArabicIndic(ayah.ayah)}﴾",
                    color = ArihnaDawnGold,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.End),
                )
            }
        }
    }
}

private fun toArabicIndic(value: Int): String = value.toString().map { c ->
    if (c in '0'..'9') ('٠'.code + (c - '0')).toChar() else c
}.joinToString("")
''')

# -----------------------------------------------------------------------------
# App root: provide persisted language and RTL/LTR direction.
# -----------------------------------------------------------------------------
p = 'app/src/main/java/com/archimedeprojects/arihna/app/ArihnaApp.kt'
text = read(p)
text = replace_once(text, 'import androidx.compose.runtime.Composable\n', 'import androidx.compose.runtime.Composable\nimport androidx.compose.runtime.CompositionLocalProvider\n', 'ArihnaApp composition import')
text = replace_once(text, 'import androidx.lifecycle.Lifecycle\n', 'import androidx.compose.ui.platform.LocalLayoutDirection\nimport androidx.compose.ui.unit.LayoutDirection\nimport androidx.lifecycle.Lifecycle\n', 'ArihnaApp direction imports')
text = replace_once(text, 'import com.archimedeprojects.arihna.core.prayer.calculation.AdhanPrayerTimeCalculator\n', 'import com.archimedeprojects.arihna.core.i18n.AppLanguage\nimport com.archimedeprojects.arihna.core.i18n.AppLanguageController\nimport com.archimedeprojects.arihna.core.i18n.LocalAppLanguageController\nimport com.archimedeprojects.arihna.core.prayer.calculation.AdhanPrayerTimeCalculator\n', 'ArihnaApp i18n imports')
text = replace_once(text, '    ArihnaTheme {\n        ArihnaNavHost(', '    val languageController = remember(activity.applicationContext) {\n        AppLanguageController(activity.applicationContext)\n    }\n\n    ArihnaTheme {\n        CompositionLocalProvider(\n            LocalAppLanguageController provides languageController,\n            LocalLayoutDirection provides if (languageController.language == AppLanguage.ARABIC) {\n                LayoutDirection.Rtl\n            } else {\n                LayoutDirection.Ltr\n            },\n        ) {\n            ArihnaNavHost(', 'ArihnaApp provider open')
end_marker = '\n    }\n}\n'
idx = text.rfind(end_marker)
if idx < 0:
    raise SystemExit('ArihnaApp final braces not found')
text = text[:idx] + '\n        }' + text[idx:]
write(p, text)

# -----------------------------------------------------------------------------
# Navigation: localized accessibility labels and Home Quran shortcut callback.
# -----------------------------------------------------------------------------
p = 'app/src/main/java/com/archimedeprojects/arihna/app/ArihnaNavHost.kt'
text = read(p)
text = replace_once(text, 'import com.archimedeprojects.arihna.core.location.model.LocationPermissionState\n', 'import com.archimedeprojects.arihna.core.i18n.appText\nimport com.archimedeprojects.arihna.core.location.model.LocationPermissionState\n', 'Nav i18n import')
text = replace_once(text, '                                contentDescription = destination.label,', '                                contentDescription = destination.localizedLabel(),', 'Nav localized content description')
text = replace_once(text, '                    onOpenAlarms = {\n                        navController.navigateTopLevel(Destination.Alarms.route, Destination.Home.route)\n                    },\n                    onRefreshLocation = {', '                    onOpenAlarms = {\n                        navController.navigateTopLevel(Destination.Alarms.route, Destination.Home.route)\n                    },\n                    onOpenQuran = {\n                        navController.navigateTopLevel(Destination.Quran.route, Destination.Home.route)\n                    },\n                    onRefreshLocation = {', 'Nav Home Quran callback')
insert = r'''
@Composable
private fun Destination.localizedLabel(): String = when (this) {
    Destination.Home -> appText("Home", "الرئيسية")
    Destination.Prayers -> appText("Orari", "مواقيت الصلاة")
    Destination.Qibla -> appText("Qibla", "القبلة")
    Destination.Quran -> appText("Corano", "القرآن")
    Destination.Alarms -> appText("Sveglie", "المنبهات")
    Destination.Settings -> appText("Impostazioni", "الإعدادات")
}

'''
needle = '@Composable\nfun ArihnaNavHost('
text = replace_once(text, needle, insert + needle, 'Nav localized label helper')
write(p, text)

# -----------------------------------------------------------------------------
# Settings: language selector + elegant stepped Material slider.
# -----------------------------------------------------------------------------
p = 'app/src/main/java/com/archimedeprojects/arihna/feature/settings/LocationSettingsScreen.kt'
text = read(p)
text = replace_once(text, 'import androidx.compose.material3.OutlinedTextFieldDefaults\n', 'import androidx.compose.material3.OutlinedTextFieldDefaults\nimport androidx.compose.material3.Slider\nimport androidx.compose.material3.SliderDefaults\n', 'Settings slider imports')
text = replace_once(text, 'import com.archimedeprojects.arihna.core.location.model.CitySearchResult\n', 'import com.archimedeprojects.arihna.core.i18n.AppLanguage\nimport com.archimedeprojects.arihna.core.i18n.LocalAppLanguageController\nimport com.archimedeprojects.arihna.core.i18n.appText\nimport com.archimedeprojects.arihna.core.location.model.CitySearchResult\n', 'Settings i18n imports')
text = replace_once(text, 'import com.archimedeprojects.arihna.feature.alarms.platform.ExactAlarmAccessIntentFactory\n', 'import com.archimedeprojects.arihna.feature.alarms.platform.ExactAlarmAccessIntentFactory\nimport kotlin.math.roundToInt\n', 'Settings round import')
text = replace_once(text, '    val presentation = uiState.resolutionState.toPresentation()\n    val ready = uiState.resolutionState is LocationResolutionState.Ready\n', '    val presentation = uiState.resolutionState.toPresentation()\n    val ready = uiState.resolutionState is LocationResolutionState.Ready\n    val languageController = LocalAppLanguageController.current\n', 'Settings language controller')
text = replace_once(text, '                    text = "Impostazioni",', '                    text = appText("Impostazioni", "الإعدادات"),', 'Settings title')
text = replace_once(text, '        SettingsSectionTitle("Posizione", "settings-section-location")', '        SettingsSectionTitle(appText("Lingua", "اللغة"), "settings-section-language")\n        LanguageSettingsCard(\n            selected = languageController.language,\n            onSelect = languageController::setLanguage,\n        )\n\n        SettingsSectionTitle(appText("Posizione", "الموقع"), "settings-section-location")', 'Settings language section')
text = replace_once(text, '        SettingsSectionTitle("Sveglia", "settings-section-alarms")', '        SettingsSectionTitle(appText("Sveglia", "المنبه"), "settings-section-alarms")', 'Settings alarm section')
text = replace_once(text, '        SettingsSectionTitle("Test rapidi", "settings-section-tests")', '        SettingsSectionTitle(appText("Test rapidi", "اختبارات سريعة"), "settings-section-tests")', 'Settings tests section')
lang_card = r'''
@Composable
private fun LanguageSettingsCard(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings-language-card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SettingsSurface),
        border = BorderStroke(1.dp, SettingsOutline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TextButton(
                onClick = { onSelect(AppLanguage.ITALIAN) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("settings-language-italian"),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (selected == AppLanguage.ITALIAN) SettingsAccent.copy(alpha = 0.18f) else Color.Transparent,
                    contentColor = SettingsText,
                ),
            ) { Text("Italiano", fontWeight = FontWeight.Bold) }
            TextButton(
                onClick = { onSelect(AppLanguage.ARABIC) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("settings-language-arabic"),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (selected == AppLanguage.ARABIC) SettingsAccent.copy(alpha = 0.18f) else Color.Transparent,
                    contentColor = SettingsText,
                ),
            ) { Text("العربية", fontWeight = FontWeight.Bold) }
        }
    }
}

'''
text = replace_once(text, '@Composable\nprivate fun AlarmVolumeCard(', lang_card + '@Composable\nprivate fun AlarmVolumeCard(', 'Settings language card insertion')
slider_func = r'''@Composable
private fun AlarmVolumeSetting(
    state: AlarmSettingsPresentation,
    onAlarmVolumeChange: (Int) -> Unit,
) {
    val volume = state.alarmVolumeState
    val current = volume.current.coerceIn(volume.min, volume.max)
    val span = (volume.max - volume.min).coerceAtLeast(1)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("settings-alarm-volume"),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Rounded.Alarm,
                contentDescription = null,
                tint = SettingsAccent,
                modifier = Modifier.size(18.dp),
            )
            Text(
                appText("Volume sveglia", "مستوى صوت المنبه"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = SettingsText,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${volume.percent}%",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = SettingsAccent,
                modifier = Modifier.testTag("settings-alarm-volume-value"),
            )
        }
        Slider(
            value = current.toFloat(),
            onValueChange = { requested ->
                onAlarmVolumeChange(requested.roundToInt().coerceIn(volume.min, volume.max))
            },
            valueRange = volume.min.toFloat()..volume.max.toFloat(),
            steps = (span - 1).coerceAtLeast(0),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings-alarm-volume-slider"),
            colors = SliderDefaults.colors(
                thumbColor = SettingsAccent,
                activeTrackColor = SettingsAccent,
                inactiveTrackColor = SettingsSurfaceRaised,
                activeTickColor = SettingsText.copy(alpha = 0.45f),
                inactiveTickColor = SettingsMuted.copy(alpha = 0.24f),
            ),
        )
        Text(
            appText(
                "Volume globale delle sveglie del telefono",
                "هذا يغيّر مستوى صوت المنبهات في الهاتف بالكامل",
            ),
            style = MaterialTheme.typography.bodySmall,
            color = SettingsMuted,
        )
        state.alarmVolumeMessage?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = SettingsDanger)
        }
    }
}

@Composable
private fun AlarmDiagnosticCard'''
text = regex_once(text, r'@Composable\nprivate fun AlarmVolumeSetting\(.*?\n@Composable\nprivate fun AlarmDiagnosticCard', slider_func, 'Settings slider function')
write(p, text)

# -----------------------------------------------------------------------------
# Home: Hijri date + clickable calendar + Quran quick action.
# -----------------------------------------------------------------------------
p = 'app/src/main/java/com/archimedeprojects/arihna/feature/home/HomePrayerScheduleScreen.kt'
text = read(p)
text = replace_once(text, 'import androidx.compose.foundation.background\n', 'import androidx.compose.foundation.background\nimport androidx.compose.foundation.clickable\n', 'Home clickable import')
text = replace_once(text, 'import androidx.compose.material.icons.rounded.MyLocation\n', 'import androidx.compose.material.icons.rounded.MyLocation\nimport androidx.compose.material.icons.rounded.MenuBook\n', 'Home Quran icon import')
text = replace_once(text, 'import androidx.compose.material3.ButtonDefaults\n', 'import androidx.compose.material3.ButtonDefaults\nimport androidx.compose.material3.DatePicker\nimport androidx.compose.material3.DatePickerDialog\nimport androidx.compose.material3.ExperimentalMaterial3Api\n', 'Home date picker imports 1')
text = replace_once(text, 'import androidx.compose.material3.Text\n', 'import androidx.compose.material3.Text\nimport androidx.compose.material3.TextButton\nimport androidx.compose.material3.rememberDatePickerState\n', 'Home date picker imports 2')
text = replace_once(text, 'import androidx.compose.runtime.getValue\n', 'import androidx.compose.runtime.getValue\nimport androidx.compose.runtime.mutableStateOf\nimport androidx.compose.runtime.remember\nimport androidx.compose.runtime.setValue\n', 'Home state imports')
text = replace_once(text, 'import com.archimedeprojects.arihna.core.ui.theme.ArihnaCream\n', 'import com.archimedeprojects.arihna.core.calendar.HijriDateFormatter\nimport com.archimedeprojects.arihna.core.i18n.appText\nimport com.archimedeprojects.arihna.core.i18n.isArabicLanguage\nimport com.archimedeprojects.arihna.core.ui.theme.ArihnaCream\n', 'Home helper imports')
text = replace_once(text, 'import java.time.ZoneId\n', 'import java.time.ZoneId\nimport java.time.ZoneOffset\n', 'Home zone offset import')
text = replace_once(text, '    onOpenAlarms: () -> Unit,\n    onRefreshLocation: () -> Unit,', '    onOpenAlarms: () -> Unit,\n    onOpenQuran: () -> Unit = {},\n    onRefreshLocation: () -> Unit,', 'Home route signature Quran')
text = replace_once(text, '        onOpenAlarms = onOpenAlarms,\n        onRefreshLocation = onRefreshLocation,', '        onOpenAlarms = onOpenAlarms,\n        onOpenQuran = onOpenQuran,\n        onRefreshLocation = onRefreshLocation,', 'Home route call Quran')
text = replace_once(text, '    onOpenAlarms: () -> Unit = {},\n    onRefreshLocation: () -> Unit = {},', '    onOpenAlarms: () -> Unit = {},\n    onOpenQuran: () -> Unit = {},\n    onRefreshLocation: () -> Unit = {},', 'Home screen signature Quran')
text = replace_once(text, '                onOpenAlarms = onOpenAlarms,\n                onRefreshLocation = onRefreshLocation,', '                onOpenAlarms = onOpenAlarms,\n                onOpenQuran = onOpenQuran,\n                onRefreshLocation = onRefreshLocation,', 'Home ready call Quran')
text = replace_once(text, '    onOpenAlarms: () -> Unit,\n    onRefreshLocation: () -> Unit,\n) {', '    onOpenAlarms: () -> Unit,\n    onOpenQuran: () -> Unit,\n    onRefreshLocation: () -> Unit,\n) {', 'ReadyContent signature Quran')
text = replace_once(text, '        onOpenAlarms = onOpenAlarms,\n        onOpenLocationSettings = onOpenLocationSettings,\n    )', '        onOpenAlarms = onOpenAlarms,\n        onOpenQuran = onOpenQuran,\n    )', 'QuickActions call Quran')
header = r'''@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeHeader(state: PrayerScheduleUiState.Ready, onRefreshLocation: () -> Unit) {
    val arabic = isArabicLanguage()
    var calendarOpen by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "ARIHNA",
                color = HomeAccent,
                fontWeight = FontWeight.Black,
                fontSize = 21.sp,
                letterSpacing = 3.sp,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = HomeMuted,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = state.location.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = HomeText,
                    modifier = Modifier.testTag("home-location"),
                )
            }
            Column(
                modifier = Modifier
                    .clickable { calendarOpen = true }
                    .testTag("home-date-block"),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = formatDate(state.localDate, arabic),
                    style = MaterialTheme.typography.bodySmall,
                    color = HomeMuted,
                    modifier = Modifier.testTag("home-current-date"),
                )
                Text(
                    text = HijriDateFormatter.format(state.localDate, arabic),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = HomeAccent,
                    modifier = Modifier.testTag("home-hijri-date"),
                )
            }
        }
        IconButton(
            onClick = onRefreshLocation,
            modifier = Modifier.testTag("home-refresh-location"),
        ) {
            Icon(
                imageVector = Icons.Rounded.MyLocation,
                contentDescription = appText("Aggiorna posizione", "تحديث الموقع"),
                tint = HomeAccent,
            )
        }
    }

    if (calendarOpen) {
        val selectedDate = datePickerState.selectedDateMillis
            ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
            ?: state.localDate
        DatePickerDialog(
            onDismissRequest = { calendarOpen = false },
            confirmButton = {
                TextButton(onClick = { calendarOpen = false }) {
                    Text(appText("Chiudi", "إغلاق"))
                }
            },
        ) {
            Column(modifier = Modifier.testTag("home-calendar-dialog")) {
                Text(
                    text = HijriDateFormatter.format(selectedDate, arabic),
                    color = HomeAccent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                )
                DatePicker(state = datePickerState)
            }
        }
    }
}

@Composable
private fun NextPrayerHero'''
text = regex_once(text, r'@Composable\nprivate fun HomeHeader\(.*?\n@Composable\nprivate fun NextPrayerHero', header, 'Home header calendar')
text = replace_once(text, 'text = "PROSSIMA PREGHIERA",', 'text = appText("PROSSIMA PREGHIERA", "الصلاة القادمة"),', 'Home next prayer label')
text = replace_once(text, 'text = "Tra ${formatCountdown(nextPrayer.remaining)}",', 'text = "${appText("Tra", "بعد")} ${formatCountdown(nextPrayer.remaining)}",', 'Home countdown label')
text = replace_once(text, '            text = "OGGI",', '            text = appText("OGGI", "اليوم"),', 'Home today heading')
text = replace_once(text, '            PrayerStripTile("Alba", state.today.times.sunrise, zoneId, false, Modifier.weight(1f))', '            PrayerStripTile(appText("Alba", "الشروق"), state.today.times.sunrise, zoneId, false, Modifier.weight(1f))', 'Home sunrise translation')
text = replace_once(text, '            text = "SETTIMANA",', '            text = appText("SETTIMANA", "الأسبوع"),', 'Home week heading')
text = replace_once(text, 'private fun WeekStrip(localDate: LocalDate) {\n    val monday', 'private fun WeekStrip(localDate: LocalDate) {\n    val arabic = isArabicLanguage()\n    val monday', 'Home week Arabic state')
text = replace_once(text, 'text = dayInitial(day.dayOfWeek),', 'text = dayInitial(day.dayOfWeek, arabic),', 'Home day initial call')
quick = r'''@Composable
private fun QuickActions(
    onOpenQibla: () -> Unit,
    onOpenAlarms: () -> Unit,
    onOpenQuran: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home-quick-actions"),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        QuickActionButton(appText("Qibla", "القبلة"), Icons.Rounded.Explore, onOpenQibla, Modifier.weight(1f))
        QuickActionButton(appText("Sveglie", "المنبهات"), Icons.Rounded.Alarm, onOpenAlarms, Modifier.weight(1f))
        QuickActionButton(appText("Corano", "القرآن"), Icons.Rounded.MenuBook, onOpenQuran, Modifier.weight(1f))
    }
}

@Composable
private fun QuickActionButton'''
text = regex_once(text, r'@Composable\nprivate fun QuickActions\(.*?\n@Composable\nprivate fun QuickActionButton', quick, 'Home quick actions Quran')
day_fn = r'''private fun dayInitial(day: DayOfWeek, arabic: Boolean): String = if (arabic) {
    when (day) {
        DayOfWeek.MONDAY -> "ن"
        DayOfWeek.TUESDAY -> "ث"
        DayOfWeek.WEDNESDAY -> "ر"
        DayOfWeek.THURSDAY -> "خ"
        DayOfWeek.FRIDAY -> "ج"
        DayOfWeek.SATURDAY -> "س"
        DayOfWeek.SUNDAY -> "ح"
    }
} else {
    when (day) {
        DayOfWeek.MONDAY -> "L"
        DayOfWeek.TUESDAY -> "M"
        DayOfWeek.WEDNESDAY -> "M"
        DayOfWeek.THURSDAY -> "G"
        DayOfWeek.FRIDAY -> "V"
        DayOfWeek.SATURDAY -> "S"
        DayOfWeek.SUNDAY -> "D"
    }
}

private fun formatDate(date: LocalDate, arabic: Boolean): String =
    DateTimeFormatter.ofPattern("EEEE d MMMM", if (arabic) Locale("ar") else Locale.ITALIAN).format(date)

private fun formatTime'''
text = regex_once(text, r'private fun dayInitial\(day: DayOfWeek\): String = when \(day\) \{.*?private fun formatTime', day_fn, 'Home day/date format')
text = text.replace('\nprivate val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.ITALIAN)', '')
write(p, text)

# -----------------------------------------------------------------------------
# Primary top-level chrome translations (deeper existing content is untouched).
# -----------------------------------------------------------------------------
for path, import_anchor, replacements in [
    (
        'app/src/main/java/com/archimedeprojects/arihna/feature/prayers/PrayerTimesPlaceholderScreen.kt',
        'import com.archimedeprojects.arihna.core.ui.theme.ArihnaCream\n',
        [
            ('                    "Orari",', '                    appText("Orari", "مواقيت الصلاة"),'),
            ('                    "Orari di preghiera e promemoria",', '                    appText("Orari di preghiera e promemoria", "مواقيت الصلاة والتذكيرات"),'),
            ('                            "Oggi • ${scheduleState.location.displayName}",', '                            "${appText("Oggi", "اليوم")} • ${scheduleState.location.displayName}",'),
        ],
    ),
    (
        'app/src/main/java/com/archimedeprojects/arihna/feature/qibla/QiblaPlaceholderScreen.kt',
        'import com.archimedeprojects.arihna.core.location.model.LocationFreshness\n',
        [
            ('        Text("Qibla", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)', '        Text(appText("Qibla", "القبلة"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)'),
            ('        Text("Direzione verso la Kaaba · nord vero", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)', '        Text(appText("Direzione verso la Kaaba · nord vero", "اتجاه الكعبة · الشمال الحقيقي"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)'),
        ],
    ),
    (
        'app/src/main/java/com/archimedeprojects/arihna/feature/alarms/AlarmsPlaceholderScreen.kt',
        'import com.archimedeprojects.arihna.core.ui.theme.ArihnaGold\n',
        [
            ('            "Sveglie",', '            appText("Sveglie", "المنبهات"),'),
            ('                Text("Nuova", fontWeight = FontWeight.ExtraBold)', '                Text(appText("Nuova", "جديد"), fontWeight = FontWeight.ExtraBold)'),
        ],
    ),
]:
    t = read(path)
    t = replace_once(t, import_anchor, 'import com.archimedeprojects.arihna.core.i18n.appText\n' + import_anchor, path + ' i18n import')
    for old, new in replacements:
        t = replace_once(t, old, new, path + ' translation')
    write(path, t)

# -----------------------------------------------------------------------------
# Focused unit tests for pure date and Quran parsing contracts.
# -----------------------------------------------------------------------------
write('app/src/test/java/com/archimedeprojects/arihna/core/calendar/HijriDateFormatterTest.kt', r'''package com.archimedeprojects.arihna.core.calendar

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertTrue

class HijriDateFormatterTest {
    @Test
    fun formatsSameDateInItalianAndArabicWithoutHardcodedCivilLookup() {
        val date = LocalDate.of(2026, 9, 7)
        val italian = HijriDateFormatter.format(date, arabic = false)
        val arabic = HijriDateFormatter.format(date, arabic = true)
        assertTrue(italian.endsWith("AH"))
        assertTrue(arabic.endsWith("هـ"))
        assertTrue(italian.isNotBlank())
        assertTrue(arabic.isNotBlank())
    }
}
''')
write('app/src/test/java/com/archimedeprojects/arihna/feature/quran/QuranCorpusTest.kt', r'''package com.archimedeprojects.arihna.feature.quran

import kotlin.test.Test
import kotlin.test.assertEquals

class QuranCorpusTest {
    @Test
    fun parsesVerbatimTextAndJuzHizbBoundaries() {
        val corpus = QuranCorpus.parse(
            quranText = "1|1|نص أول\n2|142|نص ثان\n",
            juzJson = """{"1":{"surahNum":1,"ayahNum":1},"2":{"surahNum":2,"ayahNum":142}}""",
            hizbJson = """{"1":{"surahNum":1,"ayahNum":1},"3":{"surahNum":2,"ayahNum":142}}""",
        )
        assertEquals("نص أول", corpus.ayahs.first().text)
        assertEquals(2, corpus.juzAt(2, 142))
        assertEquals(3, corpus.hizbAt(2, 142))
    }
}
''')

print('Arihna Hijri/Quran/Arabic/slider revision applied')
