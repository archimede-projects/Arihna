from pathlib import Path
import hashlib
import re

def read(path):
    return Path(path).read_text()

def write(path, text):
    Path(path).write_text(text)

def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected 1 occurrence, found {count}")
    return text.replace(old, new, 1)

color_path = "app/src/main/java/com/archimedeprojects/arihna/core/ui/theme/Color.kt"
text = read(color_path)
if "ArihnaDawnTop" in text:
    raise SystemExit("shared dawn tokens already present")
text = text.rstrip() + '''

/**
 * Shared "Alba dorata" visual tokens for every top-level Arihna destination.
 * Feature screens may vary hierarchy, but not product identity.
 */
val ArihnaDawnTop = Color(0xFFFFF7E6)
val ArihnaDawnMiddle = Color(0xFFF8F0DC)
val ArihnaDawnBottom = Color(0xFFE7ECD7)
val ArihnaCream = Color(0xFFFFFCF4)
val ArihnaSage = Color(0xFFE6EAD7)
val ArihnaSageStrong = Color(0xFFD7DFC2)
val ArihnaForest = Color(0xFF173C30)
val ArihnaDawnGold = Color(0xFFB68A25)
val ArihnaMutedText = Color(0xFF6B786F)
val ArihnaWarmOutline = Color(0xFFD6C993)
'''
write(color_path, text)

theme_path = "app/src/main/java/com/archimedeprojects/arihna/core/ui/theme/Theme.kt"
write(theme_path, '''package com.archimedeprojects.arihna.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ArihnaAlbaColors = lightColorScheme(
    primary = ArihnaGreen,
    onPrimary = ArihnaCream,
    secondary = ArihnaDawnGold,
    onSecondary = ArihnaForest,
    background = ArihnaDawnTop,
    onBackground = ArihnaForest,
    surface = ArihnaCream,
    onSurface = ArihnaForest,
    surfaceVariant = ArihnaSage,
    onSurfaceVariant = ArihnaMutedText,
    outline = ArihnaWarmOutline,
)

@Composable
@Suppress("UNUSED_PARAMETER")
fun ArihnaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ArihnaAlbaColors,
        typography = ArihnaTypography,
        shapes = ArihnaShapes,
        content = content,
    )
}
''')

home_path = "app/src/main/java/com/archimedeprojects/arihna/feature/home/HomePrayerScheduleScreen.kt"
text = read(home_path)
insert = '''import com.archimedeprojects.arihna.core.ui.theme.ArihnaCream
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnBottom
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnGold
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnMiddle
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnTop
import com.archimedeprojects.arihna.core.ui.theme.ArihnaForest
import com.archimedeprojects.arihna.core.ui.theme.ArihnaGreen
import com.archimedeprojects.arihna.core.ui.theme.ArihnaMutedText
import com.archimedeprojects.arihna.core.ui.theme.ArihnaSageStrong
import com.archimedeprojects.arihna.core.ui.theme.ArihnaWarmOutline
'''
text = replace_once(text, 'import com.archimedeprojects.arihna.feature.prayerschedule.domain.PrayerName\n',
                    insert + 'import com.archimedeprojects.arihna.feature.prayerschedule.domain.PrayerName\n', "home imports")
old = '''private val HomeBackgroundTop = Color(0xFFFFF7E6)
private val HomeBackgroundMiddle = Color(0xFFF8F0DC)
private val HomeBackgroundBottom = Color(0xFFE7ECD7)
private val HomeSurface = Color(0xFFFFF9ED)
private val HomeSurfaceRaised = Color(0xFFFFFCF4)
private val HomeHero = Color(0xFF1D5A43)
private val HomeHeroDeep = Color(0xFF0F3D2E)
private val HomeText = Color(0xFF183E31)
private val HomeMuted = Color(0xFF6B786F)
private val HomeHeroText = Color(0xFFFFF9EC)
private val HomeAccent = Color(0xFFB68A25)
private val HomeAccentSoft = Color(0xFFF0D78B)
private val HomeOutline = Color(0xFFD6C993)
'''
new = '''private val HomeBackgroundTop = ArihnaDawnTop
private val HomeBackgroundMiddle = ArihnaDawnMiddle
private val HomeBackgroundBottom = ArihnaDawnBottom
private val HomeSurface = ArihnaCream
private val HomeSurfaceRaised = ArihnaCream
private val HomeHero = ArihnaGreen
private val HomeHeroDeep = ArihnaForest
private val HomeText = ArihnaForest
private val HomeMuted = ArihnaMutedText
private val HomeHeroText = ArihnaCream
private val HomeAccent = ArihnaDawnGold
private val HomeAccentSoft = ArihnaSageStrong
private val HomeOutline = ArihnaWarmOutline
'''
text = replace_once(text, old, new, "home palette")
write(home_path, text)

orari_path = "app/src/main/java/com/archimedeprojects/arihna/feature/prayers/PrayerTimesPlaceholderScreen.kt"
text = read(orari_path)
insert = '''import com.archimedeprojects.arihna.core.ui.theme.ArihnaCream
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnGold
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnTop
import com.archimedeprojects.arihna.core.ui.theme.ArihnaForest
import com.archimedeprojects.arihna.core.ui.theme.ArihnaGreen
import com.archimedeprojects.arihna.core.ui.theme.ArihnaSage
import com.archimedeprojects.arihna.core.ui.theme.ArihnaSageStrong
'''
text = replace_once(text, 'import com.archimedeprojects.arihna.feature.alarms.AlarmsViewModel\n',
                    insert + 'import com.archimedeprojects.arihna.feature.alarms.AlarmsViewModel\n', "orari imports")
old = '''private val OrariIvory = Color(0xFFFFF8EA)
private val OrariCream = Color(0xFFFFFCF4)
private val OrariSage = Color(0xFFE6EAD7)
private val OrariSageStrong = Color(0xFFD7DFC2)
private val OrariForest = Color(0xFF173C30)
private val OrariGreen = Color(0xFF0F5132)
private val OrariGold = Color(0xFFC79B3B)
'''
new = '''private val OrariIvory = ArihnaDawnTop
private val OrariCream = ArihnaCream
private val OrariSage = ArihnaSage
private val OrariSageStrong = ArihnaSageStrong
private val OrariForest = ArihnaForest
private val OrariGreen = ArihnaGreen
private val OrariGold = ArihnaDawnGold
'''
text = replace_once(text, old, new, "orari palette")
write(orari_path, text)

settings_path = "app/src/main/java/com/archimedeprojects/arihna/feature/settings/LocationSettingsScreen.kt"
text = read(settings_path)
for line in [
    'import androidx.compose.material3.Slider\n',
    'import androidx.compose.material3.SliderDefaults\n',
    'import kotlin.math.roundToInt\n',
]:
    text = text.replace(line, '')
insert = '''import com.archimedeprojects.arihna.core.ui.theme.ArihnaCream
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnBottom
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnGold
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnTop
import com.archimedeprojects.arihna.core.ui.theme.ArihnaForest
import com.archimedeprojects.arihna.core.ui.theme.ArihnaMutedText
import com.archimedeprojects.arihna.core.ui.theme.ArihnaSage
import com.archimedeprojects.arihna.core.ui.theme.ArihnaWarmOutline
'''
text = replace_once(text, 'import com.archimedeprojects.arihna.feature.alarms.AlarmsViewModel\n',
                    insert + 'import com.archimedeprojects.arihna.feature.alarms.AlarmsViewModel\n', "settings imports")
old = '''private val SettingsBackgroundTop = Color(0xFF030B08)
private val SettingsBackgroundBottom = Color(0xFF071610)
private val SettingsSurface = Color(0xFF0C1A15)
private val SettingsSurfaceRaised = Color(0xFF11251D)
private val SettingsText = Color(0xFFF7F2E7)
private val SettingsMuted = Color(0xFFA8B4AC)
private val SettingsAccent = Color(0xFFD8B95A)
private val SettingsDanger = Color(0xFFFF9188)
private val SettingsOutline = Color(0xFF29483B)
'''
new = '''private val SettingsBackgroundTop = ArihnaDawnTop
private val SettingsBackgroundBottom = ArihnaDawnBottom
private val SettingsSurface = ArihnaCream
private val SettingsSurfaceRaised = ArihnaSage.copy(alpha = 0.62f)
private val SettingsText = ArihnaForest
private val SettingsMuted = ArihnaMutedText
private val SettingsAccent = ArihnaDawnGold
private val SettingsDanger = Color(0xFF9A3C34)
private val SettingsOutline = ArihnaWarmOutline
'''
text = replace_once(text, old, new, "settings palette")
start = text.index('@Composable\nprivate fun AlarmVolumeSetting(')
end = text.index('\n@Composable\nprivate fun AlarmDiagnosticCard(', start)
stepper = '''@Composable
private fun AlarmVolumeSetting(
    state: AlarmSettingsPresentation,
    onAlarmVolumeChange: (Int) -> Unit,
) {
    val volume = state.alarmVolumeState
    val current = volume.current.coerceIn(volume.min, volume.max)
    val canDecrease = current > volume.min
    val canIncrease = current < volume.max
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("settings-alarm-volume"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Alarm,
                    contentDescription = null,
                    tint = SettingsAccent,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    "Volume sveglia",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = SettingsText,
                )
            }
            Text(
                "${volume.percent}%",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SettingsAccent,
                modifier = Modifier.testTag("settings-alarm-volume-value"),
            )
        }
        Row(
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
        Text(
            "Volume globale delle sveglie del telefono",
            style = MaterialTheme.typography.bodySmall,
            color = SettingsMuted,
        )
        state.alarmVolumeMessage?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = SettingsDanger)
        }
    }
}
'''
text = text[:start] + stepper + text[end:]
write(settings_path, text)

alarms_path = "app/src/main/java/com/archimedeprojects/arihna/feature/alarms/AlarmsPlaceholderScreen.kt"
text = read(alarms_path)
text = text.replace('import androidx.compose.foundation.isSystemInDarkTheme\n', '')
insert = '''import com.archimedeprojects.arihna.core.ui.theme.ArihnaCream
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnBottom
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnTop
import com.archimedeprojects.arihna.core.ui.theme.ArihnaSage
'''
text = replace_once(text, 'import com.archimedeprojects.arihna.core.ui.theme.ArihnaGold\n',
                    'import com.archimedeprojects.arihna.core.ui.theme.ArihnaGold\n' + insert, "alarms imports")
old = '''    val customRules = state.rules.filter { it.definition is AlarmDefinition.Custom }
    val dark = isSystemInDarkTheme()
    val listBackground = if (dark) Color(0xFF111914) else Color(0xFFF2F0E9)
'''
new = '''    val customRules = state.rules.filter { it.definition is AlarmDefinition.Custom }
    val listBackground = ArihnaCream
'''
text = replace_once(text, old, new, "alarms list palette")
old = '''                    listOf(
                        MaterialTheme.colorScheme.background,
                        if (dark) Color(0xFF0B2117) else Color(0xFFF5F1E5),
                    ),
'''
new = '''                    listOf(
                        ArihnaDawnTop,
                        ArihnaDawnBottom,
                    ),
'''
text = replace_once(text, old, new, "alarms gradient")
pat = re.compile(r'''    val dark = isSystemInDarkTheme\(\)
    val editorBackground = if \(dark\) Color\(0xFF0B2117\) else Color\(0xFFF5F1E5\)
    val panelColor = if \(dark\) Color\(0xFF111914\) else Color\(0xFFF2F0E9\)
    val secondaryPanelColor = if \(dark\) Color\(0xFF182019\) else Color\(0xFFE7E6DF\)
''')
text, n = pat.subn('''    val editorBackground = ArihnaDawnTop
    val panelColor = ArihnaCream
    val secondaryPanelColor = ArihnaSage
''', text, count=1)
if n != 1:
    raise SystemExit(f"alarms editor palette: expected 1, found {n}")
if "isSystemInDarkTheme" in text:
    raise SystemExit("alarms still contains isSystemInDarkTheme")
write(alarms_path, text)

qibla_path = "app/src/main/java/com/archimedeprojects/arihna/feature/qibla/QiblaPlaceholderScreen.kt"
text = read(qibla_path)
insert = '''import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnBottom
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnTop
'''
text = replace_once(text, 'import com.archimedeprojects.arihna.core.ui.theme.ArihnaGold\n',
                    insert + 'import com.archimedeprojects.arihna.core.ui.theme.ArihnaGold\n', "qibla imports")
old = 'modifier = Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 16.dp, vertical = 8.dp).testTag("qibla-single-viewport"),'
new = '''modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ArihnaDawnTop, ArihnaDawnBottom)))
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("qibla-single-viewport"),'''
text = replace_once(text, old, new, "qibla root")
write(qibla_path, text)

quran_path = "app/src/main/java/com/archimedeprojects/arihna/feature/quran/QuranPlaceholderScreen.kt"
write(quran_path, '''package com.archimedeprojects.arihna.feature.quran

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnBottom
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnTop

@Composable
fun QuranPlaceholderScreen(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ArihnaDawnTop, ArihnaDawnBottom)))
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "Corano — placeholder", style = MaterialTheme.typography.titleLarge)
    }
}
''')

models_path = "app/src/main/java/com/archimedeprojects/arihna/feature/alarms/domain/AlarmModels.kt"
text = read(models_path)
old = '''    CLASSIC("classic", "Adhan classico"),
    BEAUTIFUL("beautiful", "Adhan armonioso"),
    SHORT("short", "Adhan breve"),
'''
new = '''    CLASSIC("classic", "Adhan classico"),
    BEAUTIFUL("beautiful", "Adhan armonioso"),
    SHORT("short", "Adhan breve"),
    EXTENDED("extended", "Adhan disteso"),
    COMPACT("compact", "Adhan compatto"),
    ALTERNATIVE("alternative", "Adhan alternativo"),
'''
text = replace_once(text, old, new, "Adhan enum")
write(models_path, text)

service_path = "app/src/main/java/com/archimedeprojects/arihna/feature/alarms/platform/AlarmRingingService.kt"
text = read(service_path)
old = '''            AdhanVariant.CLASSIC -> R.raw.adhan_cc0
            AdhanVariant.BEAUTIFUL -> R.raw.adhan_beautiful_cc0
            AdhanVariant.SHORT -> R.raw.adhan_short_cc0
'''
new = '''            AdhanVariant.CLASSIC -> R.raw.adhan_cc0
            AdhanVariant.BEAUTIFUL -> R.raw.adhan_beautiful_cc0
            AdhanVariant.SHORT -> R.raw.adhan_short_cc0
            AdhanVariant.EXTENDED -> R.raw.adhan_extended_cc0
            AdhanVariant.COMPACT -> R.raw.adhan_compact_pd
            AdhanVariant.ALTERNATIVE -> R.raw.adhan_alternative_cc_by_sa
'''
text = replace_once(text, old, new, "Adhan playback mapping")
write(service_path, text)

adhan_test_path = "app/src/test/java/com/archimedeprojects/arihna/feature/alarms/domain/AdhanVariantTest.kt"
write(adhan_test_path, '''package com.archimedeprojects.arihna.feature.alarms.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AdhanVariantTest {
    @Test
    fun catalogueContainsSixStableDistinctVariants() {
        val values = AdhanVariant.entries.map { it.storageValue }
        assertEquals(6, values.size)
        assertEquals(values.size, values.toSet().size)
        assertNotEquals(AdhanVariant.CLASSIC.storageValue, AdhanVariant.BEAUTIFUL.storageValue)
    }

    @Test
    fun establishedStorageIdsStayMigrationSafe() {
        assertEquals("arihna://adhan/classic", AdhanVariant.CLASSIC.storageValue)
        assertEquals("arihna://adhan/beautiful", AdhanVariant.BEAUTIFUL.storageValue)
        assertEquals("arihna://adhan/short", AdhanVariant.SHORT.storageValue)
    }

    @Test
    fun legacyAndUnknownValuesFallBackToClassic() {
        assertEquals(AdhanVariant.CLASSIC, AdhanVariant.fromStorage(null))
        assertEquals(AdhanVariant.CLASSIC, AdhanVariant.fromStorage("content://legacy"))
    }

    @Test
    fun storedVariantRoundTrips() {
        AdhanVariant.entries.forEach { variant ->
            assertEquals(variant, AdhanVariant.fromStorage(variant.storageValue))
        }
    }
}
''')

settings_test_path = "app/src/androidTest/java/com/archimedeprojects/arihna/feature/settings/LocationSettingsScreenAndroidTest.kt"
text = read(settings_test_path)
text = replace_once(text, 'import androidx.compose.ui.test.assertIsDisplayed\n',
                    'import androidx.compose.ui.test.assertDoesNotExist\nimport androidx.compose.ui.test.assertIsDisplayed\n',
                    "settings test assertion import")
text = replace_once(text, 'import com.archimedeprojects.arihna.core.ui.theme.ArihnaTheme\n',
                    'import com.archimedeprojects.arihna.core.ui.theme.ArihnaTheme\nimport com.archimedeprojects.arihna.feature.alarms.platform.AlarmVolumeState\n',
                    "settings test AlarmVolumeState import")
marker = '    private fun setScreen(state: () -> LocationSettingsUiState) {\n'
new_test = '''    @Test
    fun alarmVolumeUsesAdjacentStepButtonsAndNoSlider() {
        var requestedVolume: Int? = null
        composeRule.setContent {
            ArihnaTheme {
                LocationSettingsScreen(
                    contentPadding = PaddingValues(0.dp),
                    uiState = LocationSettingsUiState(),
                    onUseDevice = {},
                    onDismissRationale = {},
                    onConfirmRationale = {},
                    onSearchQueryChanged = {},
                    onSelectCity = {},
                    onOpenAppSettings = {},
                    onOpenLocationSettings = {},
                    alarmSettings = AlarmSettingsPresentation(
                        alarmVolumeState = AlarmVolumeState(current = 4, min = 0, max = 7),
                    ),
                    onAlarmVolumeChange = { requestedVolume = it },
                )
            }
        }

        composeRule.onNodeWithTag("settings-alarm-volume-slider").assertDoesNotExist()
        composeRule.onNodeWithTag("settings-alarm-volume-value").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-alarm-volume-decrease").performClick()
        composeRule.runOnIdle { assertEquals(3, requestedVolume) }
        composeRule.onNodeWithTag("settings-alarm-volume-increase").performClick()
        composeRule.runOnIdle { assertEquals(5, requestedVolume) }
    }

'''
text = replace_once(text, marker, new_test + marker, "settings stepper test")
write(settings_test_path, text)

audio_dir = Path("app/src/main/res/raw")
new_audio = {
    "adhan_extended_cc0.ogg": {
        "source": "https://commons.wikimedia.org/wiki/File:Muslim_calling_to_prayer.ogg",
        "original": "Muslim calling to prayer.ogg",
        "author": "Aishatu98",
        "license": "CC0 1.0 Universal",
    },
    "adhan_compact_pd.ogg": {
        "source": "https://commons.wikimedia.org/wiki/File:Oraci%C3%B3n_Al-Azzan.ogg",
        "original": "Oración Al-Azzan.ogg",
        "author": "B9",
        "license": "Public domain dedication by copyright holder",
    },
    "adhan_alternative_cc_by_sa.ogg": {
        "source": "https://commons.wikimedia.org/wiki/File:Call_to_prayer.ogg",
        "original": "Call to prayer.ogg",
        "author": "Isaacayodele32",
        "license": "CC BY-SA 4.0",
    },
}
for filename in new_audio:
    p = audio_dir / filename
    if not p.is_file() or p.stat().st_size == 0:
        raise SystemExit(f"missing downloaded audio: {filename}")
    new_audio[filename]["sha"] = hashlib.sha256(p.read_bytes()).hexdigest()
    new_audio[filename]["bytes"] = p.stat().st_size

docs = '''# Arihna bundled Adhan audio provenance

All selectable Adhan recordings below are bundled locally in the APK. Product labels are intentionally generic; Arihna does not infer a mosque, city, reciter identity or stylistic school unless the source itself establishes it.

- adhan_cc0.ogg — existing Arihna frozen/approved asset. SHA-256: 3b350bc210d657727578ed977e32323c06b63c2088c3ebcea9fc9afb7d487702.
- adhan_beautiful_cc0.ogg — Wikimedia Commons, Beautiful adhan.ogg, Adam-synagda, own work, CC0 1.0 Universal. https://commons.wikimedia.org/wiki/File:Beautiful_adhan.ogg — SHA-256: 35fe06b08fe80505c550c33fed8a783fa9901ddc81ac884958b4be048f5b2a79.
- adhan_short_cc0.ogg — Wikimedia Commons, Adhan.ogg, Aishatu98, own work, CC0 1.0 Universal. https://commons.wikimedia.org/wiki/File:Adhan.ogg — SHA-256: faeb03e4338554fb8b54dba98ed4949648615b58e991bbd560a89f4837dde413.
'''
for filename, meta in new_audio.items():
    docs += f'- {filename} — Wikimedia Commons, {meta["original"]}, {meta["author"]}, own work, {meta["license"]}. {meta["source"]} — SHA-256: {meta["sha"]}; bytes: {meta["bytes"]}.\\n'
write("docs/audio/ADHAN_SOURCES.md", docs)

write("docs/audio/THIRD_PARTY_AUDIO_LICENSES.md", '''# Third-party bundled audio license notice

`adhan_alternative_cc_by_sa.ogg` is an unmodified copy of **Call to prayer.ogg** by Wikimedia Commons user **Isaacayodele32**, licensed under **Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)**.

Source: https://commons.wikimedia.org/wiki/File:Call_to_prayer.ogg  
License: https://creativecommons.org/licenses/by-sa/4.0/

No endorsement by the author is implied. Arihna does not modify this recording before bundling it.
''')

for path in [home_path, orari_path, qibla_path, quran_path, alarms_path, settings_path]:
    if "ArihnaDawn" not in read(path) and path != alarms_path:
        raise SystemExit(f"shared visual tokens missing from {path}")
if "settings-alarm-volume-slider" in read(settings_path):
    raise SystemExit("slider test tag remains in Settings production")
if "Slider(" in read(settings_path):
    raise SystemExit("Slider remains in Settings production")
if len(re.findall(r'AdhanVariant\.[A-Z_]+ -> R\.raw\.', read(service_path))) < 6:
    raise SystemExit("not all Adhan variants map to raw playback")
