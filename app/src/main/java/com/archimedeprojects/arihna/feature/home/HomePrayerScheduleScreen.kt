package com.archimedeprojects.arihna.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.archimedeprojects.arihna.core.prayer.model.PrayerCalculationMethod
import com.archimedeprojects.arihna.feature.prayerschedule.domain.PrayerName
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.PrayerScheduleLocationSourceUi
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.PrayerScheduleUiState
import com.archimedeprojects.arihna.feature.prayerschedule.presentation.PrayerScheduleViewModel
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomePrayerScheduleRoute(
    contentPadding: PaddingValues,
    viewModel: PrayerScheduleViewModel,
    onOpenLocationSettings: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    HomePrayerScheduleScreen(
        contentPadding = contentPadding,
        uiState = uiState,
        onOpenLocationSettings = onOpenLocationSettings,
    )
}

@Composable
fun HomePrayerScheduleScreen(
    contentPadding: PaddingValues,
    uiState: PrayerScheduleUiState,
    onOpenLocationSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Orari di preghiera",
            style = MaterialTheme.typography.headlineSmall,
        )

        when (uiState) {
            PrayerScheduleUiState.Loading -> LoadingContent()
            is PrayerScheduleUiState.NoLocation -> NoLocationContent(
                state = uiState,
                onOpenLocationSettings = onOpenLocationSettings,
            )
            is PrayerScheduleUiState.CalculationUnavailable -> CalculationUnavailableContent(uiState)
            is PrayerScheduleUiState.Ready -> ReadyContent(uiState)
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator()
        Text(
            text = "Calcolo degli orari in corso…",
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun NoLocationContent(
    state: PrayerScheduleUiState.NoLocation,
    onOpenLocationSettings: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = state.message,
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onOpenLocationSettings) {
            Text("Configura posizione")
        }
    }
}

@Composable
private fun CalculationUnavailableContent(state: PrayerScheduleUiState.CalculationUnavailable) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Orari non disponibili",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = state.message,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = "Nessun orario viene mostrato finché il calcolo non torna disponibile.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ReadyContent(state: PrayerScheduleUiState.Ready) {
    val zoneId = state.today.zoneId

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (state.location.source == PrayerScheduleLocationSourceUi.DEVICE) {
            Text(
                text = "Posizione dispositivo",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = state.location.displayName,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Text(
                text = state.location.displayName,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "Posizione manuale",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Prossima preghiera",
                style = MaterialTheme.typography.labelLarge,
            )
            val nextPrayer = state.nextPrayer
            if (nextPrayer == null) {
                Text(
                    text = "Nessuna prossima preghiera disponibile.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Text(
                    text = "${prayerLabel(nextPrayer.prayer)} · ${formatTime(nextPrayer.time, zoneId)}",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "Tra ${formatCountdown(nextPrayer.remaining)}",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }

    Text(
        text = "Metodo: ${methodLabel(state.settings.method)}",
        style = MaterialTheme.typography.bodySmall,
    )

    Text(
        text = "Oggi",
        style = MaterialTheme.typography.titleMedium,
    )

    PrayerTimeRow("Fajr", state.today.times.fajr, zoneId)
    PrayerTimeRow("Alba", state.today.times.sunrise, zoneId)
    PrayerTimeRow("Dhuhr", state.today.times.dhuhr, zoneId)
    PrayerTimeRow("Asr", state.today.times.asr, zoneId)
    PrayerTimeRow("Maghrib", state.today.times.maghrib, zoneId)
    PrayerTimeRow("Isha", state.today.times.isha, zoneId)
}

@Composable
private fun PrayerTimeRow(
    label: String,
    time: Instant,
    zoneId: ZoneId,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(formatTime(time, zoneId), style = MaterialTheme.typography.bodyLarge)
        }
        HorizontalDivider()
    }
}

private fun prayerLabel(prayer: PrayerName): String = when (prayer) {
    PrayerName.FAJR -> "Fajr"
    PrayerName.DHUHR -> "Dhuhr"
    PrayerName.ASR -> "Asr"
    PrayerName.MAGHRIB -> "Maghrib"
    PrayerName.ISHA -> "Isha"
}

private fun methodLabel(method: PrayerCalculationMethod): String = when (method) {
    PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE -> "Muslim World League (MWL)"
    PrayerCalculationMethod.UMM_AL_QURA -> "Umm al-Qura"
    PrayerCalculationMethod.ISNA -> "ISNA"
    PrayerCalculationMethod.EGYPTIAN -> "Egyptian General Authority"
    PrayerCalculationMethod.KARACHI -> "University of Islamic Sciences, Karachi"
    PrayerCalculationMethod.DUBAI -> "Dubai"
    PrayerCalculationMethod.KUWAIT -> "Kuwait"
    PrayerCalculationMethod.QATAR -> "Qatar"
    PrayerCalculationMethod.MOONSIGHTING_COMMITTEE -> "Moonsighting Committee"
    PrayerCalculationMethod.SINGAPORE -> "Singapore"
    PrayerCalculationMethod.TURKEY -> "Turkey"
}

private fun formatTime(time: Instant, zoneId: ZoneId): String =
    TIME_FORMATTER.withZone(zoneId).format(time)

private fun formatCountdown(duration: Duration): String {
    val totalSeconds = duration.seconds.coerceAtLeast(0L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
}

private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ITALIAN)
