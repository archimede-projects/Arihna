package com.archimedeprojects.arihna.feature.prayers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archimedeprojects.arihna.core.i18n.appText
import com.archimedeprojects.arihna.core.prayer.model.AsrMethod
import com.archimedeprojects.arihna.core.prayer.model.HighLatitudeRule
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationMethod
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationSettings
import com.archimedeprojects.arihna.core.ui.theme.ArihnaCream
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnBottom
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnGold
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnTop
import com.archimedeprojects.arihna.core.ui.theme.ArihnaForest
import com.archimedeprojects.arihna.core.ui.theme.ArihnaSage
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.PrayerAdjustmentTarget
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.PrayerScheduleViewModel

@Composable
fun PrayerSettingsRoute(
    contentPadding: PaddingValues,
    viewModel: PrayerScheduleViewModel,
    onBack: () -> Unit,
) {
    val settings by viewModel.prayerSettings.collectAsState()
    PrayerSettingsScreen(
        contentPadding = contentPadding,
        settings = settings,
        onBack = onBack,
        onMethod = viewModel::setCalculationMethod,
        onAsrMethod = viewModel::setAsrMethod,
        onHighLatitudeRule = viewModel::setHighLatitudeRule,
        onAdjust = viewModel::adjustPrayer,
        onReset = viewModel::resetPrayerSettings,
    )
}

@Composable
internal fun PrayerSettingsScreen(
    contentPadding: PaddingValues,
    settings: PrayerCalculationSettings,
    onBack: () -> Unit,
    onMethod: (PrayerCalculationMethod) -> Unit,
    onAsrMethod: (AsrMethod) -> Unit,
    onHighLatitudeRule: (HighLatitudeRule) -> Unit,
    onAdjust: (PrayerAdjustmentTarget, Int) -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ArihnaDawnTop, ArihnaDawnBottom)))
            .padding(
                start = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                end = 16.dp,
                bottom = contentPadding.calculateBottomPadding() + 8.dp,
            )
            .verticalScroll(rememberScrollState())
            .testTag("prayer-settings-screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("prayer-settings-back")) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = appText("Indietro", "رجوع"))
            }
            Text(
                appText("Impostazioni orari", "إعدادات المواقيت"),
                color = ArihnaForest,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }

        ChoiceCard(
            title = appText("Metodo di calcolo", "طريقة الحساب"),
            selected = calculationMethodLabel(settings.method),
            values = PrayerCalculationMethod.entries,
            label = { calculationMethodLabel(it) },
            tag = "prayer-settings-method",
            onSelected = onMethod,
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = ArihnaCream),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(appText("Metodo Asr", "طريقة العصر"), color = ArihnaForest, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AsrMethod.entries.forEach { method ->
                        OutlinedButton(
                            onClick = { onAsrMethod(method) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("prayer-settings-asr-${method.name.lowercase()}"),
                        ) {
                            Text(if (method == AsrMethod.STANDARD) appText("Standard", "قياسي") else appText("Hanafi", "حنفي"))
                        }
                    }
                }
            }
        }

        ChoiceCard(
            title = appText("Alte latitudini", "خطوط العرض العليا"),
            selected = highLatitudeLabel(settings.highLatitudeRule),
            values = HighLatitudeRule.entries,
            label = { highLatitudeLabel(it) },
            tag = "prayer-settings-high-latitude",
            onSelected = onHighLatitudeRule,
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = ArihnaCream),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    appText("Correzioni manuali (minuti)", "التعديلات اليدوية (دقائق)"),
                    color = ArihnaForest,
                    fontWeight = FontWeight.Bold,
                )
                AdjustmentRow("Fajr", PrayerAdjustmentTarget.FAJR, settings.adjustments.fajrMinutes, onAdjust)
                AdjustmentRow(appText("Alba", "الشروق"), PrayerAdjustmentTarget.SUNRISE, settings.adjustments.sunriseMinutes, onAdjust)
                AdjustmentRow("Dhuhr", PrayerAdjustmentTarget.DHUHR, settings.adjustments.dhuhrMinutes, onAdjust)
                AdjustmentRow("Asr", PrayerAdjustmentTarget.ASR, settings.adjustments.asrMinutes, onAdjust)
                AdjustmentRow("Maghrib", PrayerAdjustmentTarget.MAGHRIB, settings.adjustments.maghribMinutes, onAdjust)
                AdjustmentRow("Isha", PrayerAdjustmentTarget.ISHA, settings.adjustments.ishaMinutes, onAdjust)
            }
        }

        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.fillMaxWidth().testTag("prayer-settings-reset"),
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Text(appText("  Ripristina predefiniti", "  استعادة الافتراضي"))
        }
    }
}

@Composable
private fun <T> ChoiceCard(
    title: String,
    selected: String,
    values: List<T>,
    label: @Composable (T) -> String,
    tag: String,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = ArihnaCream),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = ArihnaForest, fontWeight = FontWeight.Bold)
            Box {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ArihnaSage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true }
                        .testTag(tag),
                ) {
                    Text(selected, modifier = Modifier.padding(12.dp), color = ArihnaForest)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    values.forEach { value ->
                        DropdownMenuItem(
                            text = { Text(label(value)) },
                            onClick = {
                                expanded = false
                                onSelected(value)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustmentRow(
    label: String,
    target: PrayerAdjustmentTarget,
    value: Int,
    onAdjust: (PrayerAdjustmentTarget, Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, modifier = Modifier.weight(1f), color = ArihnaForest, fontWeight = FontWeight.SemiBold)
        OutlinedButton(
            onClick = { onAdjust(target, -1) },
            modifier = Modifier.testTag("prayer-setting-adjust-${target.name.lowercase()}-minus"),
        ) { Text("−") }
        Text(
            if (value > 0) "+$value" else value.toString(),
            color = ArihnaDawnGold,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.testTag("prayer-setting-adjust-${target.name.lowercase()}-value"),
        )
        OutlinedButton(
            onClick = { onAdjust(target, 1) },
            modifier = Modifier.testTag("prayer-setting-adjust-${target.name.lowercase()}-plus"),
        ) { Text("+") }
    }
}

@Composable
private fun calculationMethodLabel(method: PrayerCalculationMethod): String = when (method) {
    PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE -> "Muslim World League"
    PrayerCalculationMethod.UMM_AL_QURA -> "Umm al-Qura"
    PrayerCalculationMethod.ISNA -> "ISNA"
    PrayerCalculationMethod.EGYPTIAN -> appText("Autorità egiziana", "الهيئة المصرية")
    PrayerCalculationMethod.KARACHI -> "Karachi"
    PrayerCalculationMethod.DUBAI -> "Dubai"
    PrayerCalculationMethod.KUWAIT -> "Kuwait"
    PrayerCalculationMethod.QATAR -> "Qatar"
    PrayerCalculationMethod.MOONSIGHTING_COMMITTEE -> "Moonsighting Committee"
    PrayerCalculationMethod.SINGAPORE -> "Singapore"
    PrayerCalculationMethod.TURKEY -> appText("Turchia", "تركيا")
}

@Composable
private fun highLatitudeLabel(rule: HighLatitudeRule): String = when (rule) {
    HighLatitudeRule.AUTOMATIC -> appText("Automatico", "تلقائي")
    HighLatitudeRule.MIDDLE_OF_THE_NIGHT -> appText("Metà della notte", "منتصف الليل")
    HighLatitudeRule.SEVENTH_OF_THE_NIGHT -> appText("Un settimo della notte", "سُبع الليل")
    HighLatitudeRule.TWILIGHT_ANGLE -> appText("Angolo del crepuscolo", "زاوية الشفق")
}
