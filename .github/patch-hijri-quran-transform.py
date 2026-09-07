from pathlib import Path

p = Path('/tmp/revision.py')
text = p.read_text(encoding='utf-8')

old = "text = replace_once(text, '    onOpenAlarms: () -> Unit,\\n    onRefreshLocation: () -> Unit,', '    onOpenAlarms: () -> Unit,\\n    onOpenQuran: () -> Unit = {},\\n    onRefreshLocation: () -> Unit,', 'Home route signature Quran')"
new = "text = text.replace('    onOpenAlarms: () -> Unit,\\n    onRefreshLocation: () -> Unit,', '    onOpenAlarms: () -> Unit,\\n    onOpenQuran: () -> Unit = {},\\n    onRefreshLocation: () -> Unit,', 1)"
if text.count(old) != 1:
    raise SystemExit(f'expected one Home transform source target, found {text.count(old)}')
text = text.replace(old, new, 1)

old = '    fun setLanguage(value: AppLanguage) {'
new = '    fun updateLanguage(value: AppLanguage) {'
if text.count(old) != 1:
    raise SystemExit(f'expected one language mutator declaration, found {text.count(old)}')
text = text.replace(old, new, 1)
old = '            onSelect = languageController::setLanguage,'
new = '            onSelect = languageController::updateLanguage,'
if text.count(old) != 1:
    raise SystemExit(f'expected one language mutator reference, found {text.count(old)}')
text = text.replace(old, new, 1)

replacements = [
    ('class AppLanguageController(context: Context) {', 'class AppLanguageController(context: Context? = null) {'),
    ('    private val preferences = context.getSharedPreferences("arihna_ui_preferences", Context.MODE_PRIVATE)', '    private val preferences = context?.getSharedPreferences("arihna_ui_preferences", Context.MODE_PRIVATE)'),
    ('    var language by mutableStateOf(AppLanguage.fromStorage(preferences.getString(KEY_LANGUAGE, null)))', '    var language by mutableStateOf(AppLanguage.fromStorage(preferences?.getString(KEY_LANGUAGE, null)))'),
    ('        preferences.edit().putString(KEY_LANGUAGE, value.storageValue).apply()', '        preferences?.edit()?.putString(KEY_LANGUAGE, value.storageValue)?.apply()'),
    ('val LocalAppLanguageController = staticCompositionLocalOf<AppLanguageController> {\n    error("AppLanguageController not provided")\n}', 'val LocalAppLanguageController = staticCompositionLocalOf { AppLanguageController() }'),
]
for old, new in replacements:
    if text.count(old) != 1:
        raise SystemExit(f'expected one language fallback target, found {text.count(old)}: {old}')
    text = text.replace(old, new, 1)

old = 'import kotlin.test.Test'
new = 'import org.junit.Test'
if text.count(old) != 2:
    raise SystemExit(f'expected two kotlin.test.Test imports, found {text.count(old)}')
text = text.replace(old, new)
old = 'import kotlin.test.assertTrue'
new = 'import org.junit.Assert.assertTrue'
if text.count(old) != 1:
    raise SystemExit(f'expected one kotlin.test.assertTrue import, found {text.count(old)}')
text = text.replace(old, new, 1)
old = 'import kotlin.test.assertEquals'
new = 'import org.junit.Assert.assertEquals'
if text.count(old) != 1:
    raise SystemExit(f'expected one kotlin.test.assertEquals import, found {text.count(old)}')
text = text.replace(old, new, 1)

append = r"""

home_test_path = 'app/src/androidTest/java/com/archimedeprojects/arihna/feature/home/HomePrayerScheduleScreenAndroidTest.kt'
home_test = read(home_test_path)
home_replacements = [
    ('        composeRule.onNodeWithText("Posizione").assertIsDisplayed()\n', '        composeRule.onNodeWithText("Corano").assertIsDisplayed()\n'),
    ('        var location = 0\n', '        var quran = 0\n'),
    ('            onOpenLocationSettings = { location += 1 },\n', '            onOpenQuran = { quran += 1 },\n'),
    ('        composeRule.onNodeWithText("Posizione").performClick()\n', '        composeRule.onNodeWithText("Corano").performClick()\n'),
    ('            assertEquals(1, location)\n', '            assertEquals(1, quran)\n'),
    ('        onOpenAlarms: () -> Unit = {},\n        onRefreshLocation: () -> Unit = {},\n', '        onOpenAlarms: () -> Unit = {},\n        onOpenQuran: () -> Unit = {},\n        onRefreshLocation: () -> Unit = {},\n'),
    ('                    onOpenAlarms = onOpenAlarms,\n                    onRefreshLocation = onRefreshLocation,\n', '                    onOpenAlarms = onOpenAlarms,\n                    onOpenQuran = onOpenQuran,\n                    onRefreshLocation = onRefreshLocation,\n'),
]
for old, new in home_replacements:
    if home_test.count(old) != 1:
        raise SystemExit(f'Home Android test patch target mismatch {home_test.count(old)}: {old}')
    home_test = home_test.replace(old, new, 1)
calendar_test = '''    @Test
    fun dateBlockShowsHijriDateAndOpensCalendarViewer() {
        setScreen(uiState = readyState())
        composeRule.onNodeWithTag("home-hijri-date").assertIsDisplayed()
        composeRule.onNodeWithTag("home-date-block").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("home-calendar-dialog").assertIsDisplayed()
    }

'''
anchor = '    @Test\n    fun calculationUnavailableShowsControlledErrorWithoutInventedTimes() {'
if home_test.count(anchor) != 1:
    raise SystemExit('Home calendar test insertion anchor mismatch')
home_test = home_test.replace(anchor, calendar_test + anchor, 1)
write(home_test_path, home_test)

settings_test_path = 'app/src/androidTest/java/com/archimedeprojects/arihna/feature/settings/LocationSettingsScreenAndroidTest.kt'
settings_test = read(settings_test_path)
pattern = r'''    @Test\n    fun alarmVolumeUsesAdjacentStepButtonsAndNoSlider\(\) \{.*?\n    \}\n\n    private fun setScreen'''
replacement = '''    @Test
    fun alarmVolumeUsesSlider() {
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
                )
            }
        }

        composeRule.onNodeWithTag("settings-alarm-volume-value").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-alarm-volume-slider").assertIsDisplayed()
    }

    private fun setScreen'''
settings_test, count = re.subn(pattern, replacement, settings_test, count=1, flags=re.S)
if count != 1:
    raise SystemExit(f'Settings slider Android test replacement mismatch: {count}')
write(settings_test_path, settings_test)

write('app/src/androidTest/java/com/archimedeprojects/arihna/core/i18n/AppLanguageAndroidTest.kt', r'''package com.archimedeprojects.arihna.core.i18n

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppLanguageAndroidTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun arabicSelectionPersistsAndMapsToRtl() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("arihna_ui_preferences", 0)
        prefs.edit().clear().commit()
        try {
            val controller = AppLanguageController(context)
            assertEquals(AppLanguage.ITALIAN, controller.language)
            controller.updateLanguage(AppLanguage.ARABIC)
            assertEquals(AppLanguage.ARABIC, controller.language)
            val reloaded = AppLanguageController(context)
            assertEquals(AppLanguage.ARABIC, reloaded.language)
            composeRule.setContent {
                CompositionLocalProvider(
                    LocalAppLanguageController provides reloaded,
                    LocalLayoutDirection provides if (reloaded.language == AppLanguage.ARABIC) {
                        LayoutDirection.Rtl
                    } else {
                        LayoutDirection.Ltr
                    },
                ) {
                    Text(if (LocalLayoutDirection.current == LayoutDirection.Rtl) "RTL_OK" else "RTL_BAD")
                }
            }
            composeRule.onNodeWithText("RTL_OK").assertIsDisplayed()
        } finally {
            prefs.edit().clear().commit()
        }
    }
}
''')
"""
text += append
p.write_text(text, encoding='utf-8')

p = Path('/tmp/quran-buildassets.py')
text = p.read_text(encoding='utf-8')
old = 'val url = java.net.URI(\\n                "https://raw.githubusercontent.com/TarteelAI/quran-assets/$quranSourceCommit/$remotePath",\\n            ).toURL()'
new = 'val url = project.uri(\\n                "https://raw.githubusercontent.com/TarteelAI/quran-assets/$quranSourceCommit/$remotePath",\\n            ).toURL()'
if text.count(old) != 1:
    raise SystemExit(f'expected one Quran URI transform source target, found {text.count(old)}')
text = text.replace(old, new, 1)
old = 'val generatedQuranAssets = layout.buildDirectory.dir("generated/quranAssets")'
new = 'val generatedQuranAssets = layout.buildDirectory.dir("generated/quranAssets").get().asFile'
if text.count(old) != 1:
    raise SystemExit(f'expected one generatedQuranAssets declaration, found {text.count(old)}')
text = text.replace(old, new, 1)
old = 'val root = generatedQuranAssets.get().asFile'
new = 'val root = generatedQuranAssets'
if text.count(old) != 1:
    raise SystemExit(f'expected one generated Quran root expression, found {text.count(old)}')
text = text.replace(old, new, 1)
p.write_text(text, encoding='utf-8')
print('patched v2 Home, language, tests and Gradle Quran asset generation')
