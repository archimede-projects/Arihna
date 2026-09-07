package com.archimedeprojects.arihna.feature.quran

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
