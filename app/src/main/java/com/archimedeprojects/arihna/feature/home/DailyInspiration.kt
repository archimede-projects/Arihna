package com.archimedeprojects.arihna.feature.home

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.archimedeprojects.arihna.core.i18n.appText
import com.archimedeprojects.arihna.core.ui.theme.ArihnaCream
import com.archimedeprojects.arihna.core.ui.theme.ArihnaDawnGold
import com.archimedeprojects.arihna.core.ui.theme.ArihnaForest
import com.archimedeprojects.arihna.core.ui.theme.ArihnaGreen
import com.archimedeprojects.arihna.core.ui.theme.ArihnaInk
import com.archimedeprojects.arihna.core.ui.theme.ArihnaMutedText
import com.archimedeprojects.arihna.core.ui.theme.ArihnaSage
import com.archimedeprojects.arihna.core.ui.theme.ArihnaWarmOutline
import java.time.LocalDate

internal data class DailyInspiration(
    val kind: String,
    val text: String,
    val reference: String,
    val translationItalian: String = "",
)

/**
 * Only directly sourced Quran / sahih hadith text is presented as Quran or hadith.
 * Free-form encouragement lives in the separate DailyAction model below and is not
 * attributed to scripture.
 */
internal val curatedDailyInspirations = listOf(
    DailyInspiration(
        kind = "القرآن الكريم",
        text = "فَإِنَّ مَعَ الْعُسْرِ يُسْرًا ۝ إِنَّ مَعَ الْعُسْرِ يُسْرًا",
        reference = "Corano 94:5–6",
        translationItalian = "Con la difficoltà viene il sollievo.",
    ),
    DailyInspiration(
        kind = "القرآن الكريم",
        text = "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ",
        reference = "Corano 13:28",
        translationItalian = "Nel ricordo di Allah i cuori trovano quiete.",
    ),
    DailyInspiration(
        kind = "القرآن الكريم",
        text = "لَا تَقْنَطُوا مِنْ رَحْمَةِ اللَّهِ",
        reference = "Corano 39:53",
        translationItalian = "Non disperate della misericordia di Allah.",
    ),
    DailyInspiration(
        kind = "القرآن الكريم",
        text = "لَا يُكَلِّفُ اللَّهُ نَفْسًا إِلَّا وُسْعَهَا",
        reference = "Corano 2:286",
        translationItalian = "Allah non impone a nessuna anima un peso superiore alle sue capacità.",
    ),
    DailyInspiration(
        kind = "حديث صحيح",
        text = "أَحَبُّ الْأَعْمَالِ إِلَى اللَّهِ أَدْوَمُهَا وَإِنْ قَلَّ",
        reference = "Sahih al-Bukhari 6464",
        translationItalian = "Le opere più amate da Allah sono quelle compiute con costanza, anche se piccole.",
    ),
    DailyInspiration(
        kind = "حديث صحيح",
        text = "وَالْكَلِمَةُ الطَّيِّبَةُ صَدَقَةٌ",
        reference = "Sahih al-Bukhari 2989",
        translationItalian = "Una buona parola è carità.",
    ),
)

internal data class DailyAction(
    val arabic: String,
    val italian: String,
)

internal val curatedDailyActions = listOf(
    DailyAction(
        "اتَّصِلْ بِوَالِدَيْكَ أَوْ بِمَنْ تَشْتَاقُ إِلَيْهِ، وَاسْأَلْهُ عَنْ حَالِهِ بِقَلْبٍ حَاضِرٍ.",
        "Chiama i tuoi genitori o una persona cara e chiedile sinceramente come sta.",
    ),
    DailyAction(
        "تَصَدَّقْ الْيَوْمَ بِشَيْءٍ يَسِيرٍ، وَلَوْ كَانَ خُفْيَةً لَا يَعْلَمُ بِهِ أَحَدٌ.",
        "Fai oggi una piccola elemosina, possibilmente in modo discreto.",
    ),
    DailyAction(
        "ابْتَسِمْ فِي وَجْهِ ثَلَاثَةِ أَشْخَاصٍ، وَابْدَأْهُمْ بِالسَّلَامِ وَكَلِمَةٍ طَيِّبَةٍ.",
        "Sorridi a tre persone, salutale per primo e regala loro una parola gentile.",
    ),
    DailyAction(
        "اخْتَرْ شَخْصًا ضَايَقَكَ وَادْعُ لَهُ بِالْخَيْرِ، ثُمَّ اتركْ مَا فِي قَلْبِكَ مِنْ غِلٍّ.",
        "Pensa a qualcuno che ti ha ferito, prega per il suo bene e prova a lasciare andare il rancore.",
    ),
    DailyAction(
        "خَصِّصْ خَمْسَ دَقَائِقَ لِلْقُرْآنِ بِهُدُوءٍ، ثُمَّ اخْتَرْ آيَةً تَعْمَلُ بِمَعْنَاهَا الْيَوْمَ.",
        "Dedica cinque minuti tranquilli al Corano e scegli un versetto da trasformare in azione oggi.",
    ),
    DailyAction(
        "سَاعِدْ إِنْسَانًا فِي أَمْرٍ صَغِيرٍ مِنْ غَيْرِ أَنْ تَنْتَظِرَ مِنْهُ شُكْرًا.",
        "Aiuta qualcuno in una piccola cosa senza aspettarti ringraziamenti.",
    ),
    DailyAction(
        "اشْكُرْ شَخْصًا كَانَ لَهُ أَثَرٌ طَيِّبٌ فِي حَيَاتِكَ، وَقُلْ لَهُ ذَلِكَ بوضوح.",
        "Ringrazia una persona che ha avuto un effetto positivo nella tua vita e diglielo chiaramente.",
    ),
    DailyAction(
        "ادْعُ الْيَوْمَ لِشَخْصٍ آخَرَ بِدَعْوَةٍ جَمِيلَةٍ فِي ظَهْرِ الْغَيْبِ.",
        "Fai oggi una bella duʿā per un'altra persona, senza che lo sappia.",
    ),
)

internal fun dailyInspirationFor(date: LocalDate): DailyInspiration {
    val index = Math.floorMod(date.toEpochDay(), curatedDailyInspirations.size.toLong()).toInt()
    return curatedDailyInspirations[index]
}

internal fun dailyActionFor(date: LocalDate): DailyAction {
    val index = Math.floorMod(date.toEpochDay() + 3L, curatedDailyActions.size.toLong()).toInt()
    return curatedDailyActions[index]
}

internal fun DailyInspiration.shareText(): String =
    "“$text”\n$reference\n\n${translationItalian.takeIf { it.isNotBlank() }.orEmpty()}\n\nCondiviso da Arihna"

@Composable
internal fun DailyInspirationCard(localDate: LocalDate) {
    val inspiration = remember(localDate) { dailyInspirationFor(localDate) }
    var showDetail by remember(localDate) { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        onClick = { showDetail = true },
        modifier = Modifier.fillMaxWidth().testTag("home-inspiration"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ArihnaCream),
        border = BorderStroke(1.dp, ArihnaDawnGold.copy(alpha = 0.48f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 17.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    appText("ISPIRAZIONE DEL GIORNO", "نُورُ الْيَوْمِ"),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = ArihnaDawnGold,
                )
                Text(
                    inspiration.kind,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = ArihnaForest,
                )
            }
            Text(
                inspiration.text,
                modifier = Modifier.fillMaxWidth().testTag("home-inspiration-arabic"),
                textAlign = TextAlign.End,
                fontSize = 24.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.SemiBold,
                color = ArihnaInk,
            )
            Text(
                inspiration.translationItalian,
                style = MaterialTheme.typography.bodySmall,
                color = ArihnaMutedText,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    inspiration.reference,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = ArihnaGreen,
                    modifier = Modifier.testTag("home-inspiration-source"),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Share,
                        contentDescription = null,
                        tint = ArihnaDawnGold,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        appText("Leggi e condividi", "اقرأ وشارك"),
                        style = MaterialTheme.typography.labelSmall,
                        color = ArihnaDawnGold,
                    )
                }
            }
        }
    }

    if (showDetail) {
        AlertDialog(
            onDismissRequest = { showDetail = false },
            modifier = Modifier.testTag("home-inspiration-detail"),
            containerColor = ArihnaCream,
            titleContentColor = ArihnaForest,
            textContentColor = ArihnaInk,
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(appText("Ispirazione del giorno", "نُورُ الْيَوْمِ"), fontWeight = FontWeight.Bold)
                    Text(
                        inspiration.kind,
                        style = MaterialTheme.typography.labelSmall,
                        color = ArihnaDawnGold,
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        inspiration.text,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        fontSize = 26.sp,
                        lineHeight = 42.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(inspiration.translationItalian, color = ArihnaMutedText)
                    Text(
                        inspiration.reference,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ArihnaGreen,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { shareInspiration(context, inspiration) }) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(appText("Condividi", "شارك"), modifier = Modifier.padding(start = 6.dp))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDetail = false }) { Text(appText("Chiudi", "إغلاق")) }
            },
        )
    }
}

@Composable
internal fun DailyActionCard(localDate: LocalDate) {
    val action = remember(localDate) { dailyActionFor(localDate) }
    Card(
        modifier = Modifier.fillMaxWidth().testTag("home-daily-action"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ArihnaSage.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, ArihnaGreen.copy(alpha = 0.22f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = ArihnaDawnGold,
                modifier = Modifier.size(25.dp),
            )
            Spacer(Modifier.width(11.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    appText("AZIONE DI OGGI", "عَمَلُ الْيَوْمِ"),
                    color = ArihnaForest,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    action.arabic,
                    modifier = Modifier.fillMaxWidth().testTag("home-daily-action-arabic"),
                    color = ArihnaInk,
                    fontSize = 18.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                )
                Text(
                    action.italian,
                    color = ArihnaMutedText,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    appText("Piccolo passo, giornata più luminosa.", "خُطْوَةٌ صَغِيرَةٌ لِيَوْمٍ أَجْمَلَ"),
                    color = ArihnaGreen,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

private fun shareInspiration(context: Context, inspiration: DailyInspiration) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, inspiration.shareText())
    }
    context.startActivity(Intent.createChooser(sendIntent, "Condividi ispirazione"))
}
