package com.archimedeprojects.arihna.feature.home

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

internal data class DailyInspiration(
    val kind: String,
    val text: String,
    val reference: String,
)

internal val curatedDailyInspirations = listOf(
    DailyInspiration("CORANO", "Con la difficoltà viene il sollievo.", "Corano 94:5–6"),
    DailyInspiration("CORANO", "Nel ricordo di Allah i cuori trovano quiete.", "Corano 13:28"),
    DailyInspiration("CORANO", "Non disperate della misericordia di Allah.", "Corano 39:53"),
    DailyInspiration(
        "CORANO",
        "Allah non impone a nessuna anima un peso superiore alle sue capacità.",
        "Corano 2:286",
    ),
    DailyInspiration(
        "HADITH",
        "Le opere più amate da Allah sono quelle compiute con costanza, anche se piccole.",
        "Sahih al-Bukhari 6464",
    ),
    DailyInspiration("HADITH", "Una buona parola è carità.", "Sahih al-Bukhari 2989"),
)

internal fun dailyInspirationFor(date: LocalDate): DailyInspiration {
    val index = Math.floorMod(date.toEpochDay(), curatedDailyInspirations.size.toLong()).toInt()
    return curatedDailyInspirations[index]
}

internal fun DailyInspiration.shareText(): String =
    "“$text”\n$reference\n\nCondiviso da Arihna"

@Composable
internal fun DailyInspirationCard(localDate: LocalDate) {
    val inspiration = remember(localDate) { dailyInspirationFor(localDate) }
    var showDetail by remember(localDate) { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        onClick = { showDetail = true },
        modifier = Modifier.fillMaxWidth().testTag("home-inspiration"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF11251D)),
        border = BorderStroke(1.dp, Color(0xFFD8B95A).copy(alpha = 0.38f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 17.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "ISPIRAZIONE DEL GIORNO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD8B95A),
                )
                Text(
                    inspiration.kind,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA8B4AC),
                )
            }
            Text(
                "“${inspiration.text}”",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF7F2E7),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    inspiration.reference,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA8B4AC),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Share,
                        contentDescription = null,
                        tint = Color(0xFFD8B95A),
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        "Tocca per leggere e condividere",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD8B95A),
                    )
                }
            }
        }
    }

    if (showDetail) {
        AlertDialog(
            onDismissRequest = { showDetail = false },
            modifier = Modifier.testTag("home-inspiration-detail"),
            containerColor = Color(0xFF10241C),
            titleContentColor = Color(0xFFD8B95A),
            textContentColor = Color(0xFFF7F2E7),
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Ispirazione del giorno", fontWeight = FontWeight.Bold)
                    Text(
                        inspiration.kind,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA8B4AC),
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "“${inspiration.text}”",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        inspiration.reference,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD8B95A),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { shareInspiration(context, inspiration) }) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Condividi", modifier = Modifier.padding(start = 6.dp))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDetail = false }) { Text("Chiudi") }
            },
        )
    }
}

private fun shareInspiration(context: Context, inspiration: DailyInspiration) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, inspiration.shareText())
    }
    context.startActivity(Intent.createChooser(sendIntent, "Condividi ispirazione"))
}
